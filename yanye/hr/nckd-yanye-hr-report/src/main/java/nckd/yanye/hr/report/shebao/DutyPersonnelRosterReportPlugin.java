package nckd.yanye.hr.report.shebao;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import kd.bos.algo.*;
import kd.bos.algo.util.Tuple2;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.QueryEntityType;
import kd.bos.entity.list.JoinEntity;
import kd.bos.entity.report.*;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.metadata.entity.EntityItem;
import kd.bos.metadata.entity.EntityMetadata;
import kd.bos.orm.query.QFilter;
import kd.bos.util.CollectionUtils;
import kd.hr.hbp.business.servicehelper.HRMServiceHelper;
import kd.hr.hbp.business.servicehelper.HRQueryEntityHelper;
import kd.hr.hbp.common.util.HRDateTimeUtils;
import kd.hr.hbp.common.util.HRStringUtils;
import kd.hr.hspm.business.domian.repository.EmpReportRepository;
import kd.hr.hspm.business.domian.repository.ReportDisplayRepository;
import kd.hr.hspm.formplugin.web.report.func.EmpReportCalculateMapFunction;
import kd.hr.hspm.formplugin.web.report.func.EmpReportShowHisNameCalculateMapFunction;
import kd.hr.hspm.formplugin.web.report.helper.CreateReportColumn;
import kd.hr.hspm.formplugin.web.report.helper.EmpReportHelper;
import kd.sdk.hr.hspm.business.helper.ShowHisVersionEntityHelper;
import kd.sdk.hr.hspm.business.repository.ext.service.EmpReportExtCommon;
import kd.sdk.hr.hspm.common.entity.ShowHisVersionEntity;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Module           :核心人力云 -人员信息-在职员工花名册,报表插件
 * Description      :在职员工花名册报表插件,插件，标识：nckd_hspm_empreport_ext
 *
 * @author guozhiwei
 * @date  2024/9/13 10：40
 * 标识 nckd_hspm_empreport_ext
 *
 */


public class DutyPersonnelRosterReportPlugin extends AbstractReportListDataPlugin {

    private static final Log LOGGER = LogFactory.getLog(DutyPersonnelRosterReportPlugin.class);
    private static final Integer BATCH_COUNT = 1000;
    private Long reportPlanId;
    private final QueryEntityType queryType = (QueryEntityType)EmpReportRepository.generate().getDataEntityType();
    private static final Map<String, List<String>> CALCULATE_COLUMN_MAP = new HashMap(16);
    private static final List<ShowHisVersionEntity> SHOW_HIS_VERSION_ENTITY_LIST = new ArrayList(16);

    public DutyPersonnelRosterReportPlugin() {
    }

    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        super.getColumns(columns);
        List<JoinEntity> joinEntityList = this.queryType.getJoinEntitys();
        Map<String, String> joinEntityAliasMap = (Map)joinEntityList.stream().collect(Collectors.toMap(JoinEntity::getAlias, JoinEntity::getEntityName));
        Map<String, DynamicObject> map = ReportDisplayRepository.getConfigs(this.reportPlanId);
        map.put("id", (DynamicObject) null);
        Map<String, EntityMetadata> metaMap = new HashMap(16);
        Iterator var6 = map.entrySet().iterator();

        while(var6.hasNext()) {
            Map.Entry<String, DynamicObject> entry = (Map.Entry)var6.next();
            String mapKey = (String)entry.getKey();
            DynamicObject mapValue = (DynamicObject)entry.getValue();
            EntityItem<?> entityItem = (EntityItem) EmpReportHelper.getFieldItem(joinEntityAliasMap, (String)entry.getKey(), metaMap).item2;
            ReportColumn column = CreateReportColumn.createColumnByType(entityItem, mapKey, mapValue);
            if (column == null) {
                column = CreateReportColumn.createTextColumn(mapValue, mapKey, mapKey);
            }

            if ("id".equals(mapKey)) {
                column.setHide(true);
            }

            EmpReportExtCommon.addExtColumnByType(column, entityItem, mapKey, mapValue);
            columns.add(columns.size(), column);
        }

        return columns;
    }

    public DataSet queryBatchBy(ReportQueryParam queryParam) {
        LOGGER.info("EmpReportListPlugin.queryBatchBy---start");
        long startTime = System.currentTimeMillis();
        int size = 0;

        DataSet dataSet;
        try {
            Map<String, Object> basedataIdMap = (Map)queryParam.getCustomParam().get("empCommonfilterBasedataId");
            FilterInfo filterInfo = queryParam.getFilter();
            this.reportPlanId = filterInfo.getLong("report");
            List<Object> ids = HRQueryEntityHelper.getInstance().queryAllPkByKSql(this.queryType, new QFilter[]{EmpReportRepository.handleFilter(filterInfo, basedataIdMap)}, ReportDisplayRepository.getSort(this.reportPlanId), EmpReportRepository.reletionMapFilter(filterInfo));
            if (ids == null) {
                ids = new ArrayList(16);
            }

            dataSet = this.buildBatchDataSet((List)ids);
            IReportBatchQueryInfo byBatchInfo = queryParam.byBatchInfo();
            byBatchInfo.setCountPerBatch(BATCH_COUNT);
            size = ((List)ids).size();
            byBatchInfo.setMaxRowCountCached(size);
        } finally {
            LOGGER.info(MessageFormat.format("EmpReportListPlugin.queryBatchBy---end maxSize={0},batchSize={1},executeTime={2}", size, BATCH_COUNT, System.currentTimeMillis() - startTime));
        }

        return dataSet;
    }

    public DataSet query(ReportQueryParam reportQueryParam, Object selectedObj) {
        IReportBatchQueryInfo byBatchInfo = reportQueryParam.byBatchInfo();
        List<Row> currentBatchRows = byBatchInfo.getCurrentBatchRows();
        if (currentBatchRows != null && !currentBatchRows.isEmpty()) {
            LOGGER.info("EmpReportListPlugin.query---begin query");
            long startTime = System.currentTimeMillis();
            Set<Long> matIdsOfCurrentBatch = new HashSet();

            Object var11;
            try {
                FilterInfo filterInfo = reportQueryParam.getFilter();
                this.reportPlanId = filterInfo.getLong("report");
                Set<String> queryFieldSet = this.getQueryFields();
                if (!CollectionUtils.isEmpty(queryFieldSet)) {
                    String order  = "hrpi_empposorgrel.adminorg.sortcode asc,hrpi_empposorgrel.adminorg.number asc,hrpi_empposorgrel.position.number,hrpi_empposorgrel.nckd_personindex asc";
                    String sort = ReportDisplayRepository.getSort(this.reportPlanId);
                    order = order + "," + sort;
                    matIdsOfCurrentBatch = this.getQueryIdList(currentBatchRows);
                    DataSet dataSet = HRQueryEntityHelper.getInstance().getQueryDataSet(this.queryType, String.join(",", (CharSequence[])queryFieldSet.toArray(new String[0])), new QFilter[]{new QFilter("id", "in", matIdsOfCurrentBatch)}, order, EmpReportRepository.reletionMapFilter(filterInfo));
                    dataSet = this.calculate(dataSet, filterInfo, queryFieldSet);
                    return dataSet;
                }

                var11 = null;
            } finally {
                LOGGER.info(MessageFormat.format("EmpReportListPlugin.query---end query by batch,current size={0},executeTime={1} ms.", ((Set)matIdsOfCurrentBatch).size(), System.currentTimeMillis() - startTime));
            }

            return (DataSet)var11;
        } else {
            LOGGER.info("EmpReportListPlugin.query,not found batch rows");
            return Algo.create(this.getClass().getName()).createDataSetBuilder(new RowMeta(new Field[0])).build();
        }
    }

    private DataSet calculate(DataSet dataSet, FilterInfo filterInfo, Set<String> queryFieldSet) {
        dataSet = this.addCalculate(dataSet, filterInfo, queryFieldSet);
        dataSet = this.addShowHisNameCalculate(dataSet, filterInfo, queryFieldSet);
        return EmpReportExtCommon.addExtCalculate(dataSet, filterInfo, queryFieldSet);
    }

    private DataSet addCalculate(DataSet dataSet, FilterInfo filterInfo, Set<String> queryFieldSet) {
        if (dataSet == null) {
            LOGGER.info("EmpReportListPlugin.addCalculate dataset is null");
            return dataSet;
        } else {
            if (this.hasCalculateField(queryFieldSet)) {
                Date queryDate = EmpReportRepository.getQueryDate(filterInfo);
                Map<String, Object> paramMap = new HashMap(16);
                if (queryFieldSet.contains("hrpi_empjobrel.jobclass.name")) {
                    List<Long> jobBoidList = new ArrayList(10);
                    DataSet copyDataSet = dataSet.copy();
                    Iterator var8 = copyDataSet.iterator();

                    while(var8.hasNext()) {
                        Row row = (Row)var8.next();
                        Long jobBoid = row.getLong("hrpi_empjobrel.job.boid");
                        if (jobBoid != null && jobBoid != 0L) {
                            jobBoidList.add(jobBoid);
                        }
                    }

                    if (!jobBoidList.isEmpty()) {
                        Map<Long, String> jobClassLongNameMap = (Map)HRMServiceHelper.invokeBizService("hrmp", "hbjm", "IHBJMJobClassService", "getJobClassLongNameByJobAndBsed", new Object[]{jobBoidList, queryDate});
                        paramMap.put("hrpi_empjobrel.jobclass.name", jobClassLongNameMap);
                    }
                }

                if (queryFieldSet.contains("hrpi_pernontsprop.age")) {
                    paramMap.put("hrpi_pernontsprop.age", queryDate);
                }

                dataSet = dataSet.map(new EmpReportCalculateMapFunction(dataSet.getRowMeta(), paramMap));
            }

            return dataSet;
        }
    }

    private DataSet addShowHisNameCalculate(DataSet dataSet, FilterInfo filterInfo, Set<String> queryFieldSet) {
        if (dataSet == null) {
            LOGGER.info("EmpReportListPlugin.addShowHisNameCalculate,dataset is null");
            return dataSet;
        } else {
            if (this.hasShowHisNameField(queryFieldSet) && this.isQueryHisDate(filterInfo)) {
                LOGGER.info("EmpReportListPlugin.addShowHisNameCalculate,queryFieldSet:{}", queryFieldSet);
                DataSet copyDataSet = dataSet.copy();
                ShowHisVersionEntityHelper showHisVersionEntityHelper = ShowHisVersionEntityHelper.getInstance();
                Map<String, List<Long>> hisVersionIdListMap = showHisVersionEntityHelper.getShowHisVersionIdListMap(copyDataSet, queryFieldSet, SHOW_HIS_VERSION_ENTITY_LIST);
                Map<String, Tuple2<String, Map<Long, Object>>> hisVersionDysMap = showHisVersionEntityHelper.queryHisVersionDysMap(hisVersionIdListMap, SHOW_HIS_VERSION_ENTITY_LIST, this.getDataStatusAndBsedFilter(filterInfo));
                dataSet = dataSet.map(new EmpReportShowHisNameCalculateMapFunction(dataSet.getRowMeta(), hisVersionDysMap));
            }

            return dataSet;
        }
    }

    private QFilter getDataStatusAndBsedFilter(FilterInfo filterInfo) {
        QFilter dataStatusFilter = new QFilter("datastatus", "in", Lists.newArrayList(new String[]{"0", "1", "2"}));
        dataStatusFilter.and(new QFilter("iscurrentversion", "=", "0"));
        Date dateParam = EmpReportRepository.getQueryDate(filterInfo);
        dataStatusFilter.and(new QFilter("bsed", "<=", dateParam));
        dataStatusFilter.and(new QFilter("bsled", ">=", dateParam));
        dataStatusFilter.and(new QFilter("initstatus", "=", "2"));
        return dataStatusFilter;
    }

    private boolean hasCalculateField(Set<String> queryFieldSet) {
        Iterator var2 = CALCULATE_COLUMN_MAP.keySet().iterator();

        String calculateColumn;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            calculateColumn = (String)var2.next();
        } while(!queryFieldSet.contains(calculateColumn));

        return true;
    }

    private boolean hasShowHisNameField(Set<String> queryFieldSet) {
        Iterator var2 = SHOW_HIS_VERSION_ENTITY_LIST.iterator();

        ShowHisVersionEntity showHisVersionEntity;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            showHisVersionEntity = (ShowHisVersionEntity)var2.next();
        } while(!queryFieldSet.contains(showHisVersionEntity.getListFieldName()));

        return true;
    }

    private boolean isQueryHisDate(FilterInfo filterInfo) {
        Date dateParam = EmpReportRepository.getQueryDate(filterInfo);
        Date today = HRDateTimeUtils.dateFormatDate(new Date());
        return dateParam.compareTo(today) < 0;
    }

    public Set<Long> getQueryIdList(List<Row> currentBatchRows) {
        Set<Long> matIdsOfCurrentBatch = Sets.newHashSetWithExpectedSize(BATCH_COUNT);
        Iterator var3 = currentBatchRows.iterator();

        while(var3.hasNext()) {
            Row currentBatchRow = (Row)var3.next();
            Long matId = currentBatchRow.getLong(0);
            matIdsOfCurrentBatch.add(matId);
        }

        return matIdsOfCurrentBatch;
    }

    private Set<String> getQueryFields() {
        Map<String, String> map = ReportDisplayRepository.getField(this.reportPlanId);
        Set<String> queryFieldSet = Sets.newLinkedHashSetWithExpectedSize(map.size());
        if (map.size() < 1) {
            return queryFieldSet;
        } else {
            queryFieldSet.add("id");
            queryFieldSet.addAll(map.keySet());
            this.addCalculateField(queryFieldSet);
            this.addHisNameCalculateField(queryFieldSet);
            EmpReportExtCommon.addExtQueryFields(queryFieldSet);
            return queryFieldSet;
        }
    }

    private void addCalculateField(Set<String> queryFieldSet) {
        Iterator var2 = CALCULATE_COLUMN_MAP.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, List<String>> entry = (Map.Entry)var2.next();
            if (queryFieldSet.contains(entry.getKey())) {
                queryFieldSet.addAll((Collection)entry.getValue());
            }
        }

    }

    private void addHisNameCalculateField(Set<String> queryFieldSet) {
        Iterator var2 = SHOW_HIS_VERSION_ENTITY_LIST.iterator();

        while(var2.hasNext()) {
            ShowHisVersionEntity showHisVersionEntity = (ShowHisVersionEntity)var2.next();
            if (queryFieldSet.contains(showHisVersionEntity.getListFieldName())) {
                queryFieldSet.add(showHisVersionEntity.getListBoidName());
                if (HRStringUtils.isNotEmpty(showHisVersionEntity.getParentEntityName())) {
                    queryFieldSet.add(showHisVersionEntity.getListParentIdName());
                }
            }
        }

    }

    private DataSet buildBatchDataSet(List<Object> ids) {
        RowMeta rowMeta = new RowMeta(new Field[]{new Field("id", DataType.LongType)});
        DataSetBuilder dataSetBuilder = Algo.create(this.getClass().getName()).createDataSetBuilder(rowMeta);
        Iterator var4 = ids.iterator();

        while(var4.hasNext()) {
            Object id = var4.next();
            dataSetBuilder.append(new Object[]{id});
        }

        return dataSetBuilder.build();
    }

    static {
        CALCULATE_COLUMN_MAP.put("hrpi_pernontsprop.age", Collections.singletonList("hrpi_pernontsprop.birthday"));
        CALCULATE_COLUMN_MAP.put("hrpi_empjobrel.jobclass.name", Collections.singletonList("hrpi_empjobrel.job.boid"));
        CALCULATE_COLUMN_MAP.put("sort", Collections.singletonList("sort"));
        SHOW_HIS_VERSION_ENTITY_LIST.addAll(Lists.newArrayList(new ShowHisVersionEntity[]{new ShowHisVersionEntity("haos_adminorghr", "hrpi_empposorgrel.adminorg.boid", "hrpi_empposorgrel.adminorg.name"), new ShowHisVersionEntity("hbpm_positionhr", "hrpi_empposorgrel.position.boid", "hrpi_empposorgrel.position.name"), new ShowHisVersionEntity("hbpm_stposition", "hrpi_empposorgrel.stdposition.boid", "hrpi_empposorgrel.stdposition.name"), new ShowHisVersionEntity("haos_adminorghr", "hrpi_empposorgrel.company.boid", "hrpi_empposorgrel.company.name"), new ShowHisVersionEntity("hbjm_jobhr", "hrpi_empjobrel.job.boid", "hrpi_empjobrel.job.name"), new ShowHisVersionEntity("hbjm_joblevelhr", "hrpi_empjobrel.joblevel.entryboid", "hrpi_empjobrel.joblevel.name", "hbjm_joblevelscmhr", "hrpi_empjobrel.joblevel.joblevelscm", "entryboid", "joblevelscm"), new ShowHisVersionEntity("hbjm_jobgradehr", "hrpi_empjobrel.jobgrade.entryboid", "hrpi_empjobrel.jobgrade.name", "hbjm_jobgradescmhr", "hrpi_empjobrel.jobgrade.jobgradescm", "entryboid", "jobgradescm")}));
    }
}
