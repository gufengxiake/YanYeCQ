package nckd.yanye.tmc.report;

import java.util.*;
import java.util.stream.Collectors;

import kd.bos.algo.*;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.tmc.fbp.common.helper.TmcOrgDataHelper;

/**
 * @author husheng
 * @date 2024-09-19 13:40
 * @description
 */
public class TransactionSummaryPlugin extends AbstractReportListDataPlugin {
    private Map<String, Object> paramMap = null;
    //所有结果的组织id
    private Set<Long> allOrgIds = new HashSet<>(16);
    //符合要求的所有公司管理树节点的id
    private Set<Long> allOrgTreeIds = new HashSet<>(16);

    //汇总字段
    private static final String[] sumFields = new String[]{"nckd_amount1"};


    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) {
        // 查询条件
        FilterInfo filter = reportQueryParam.getFilter();
        FilterItemInfo filterOrgview = filter.getFilterItem("nckd_filter_orgview");
        Long orgViewId = ((DynamicObject) filterOrgview.getValue()).getLong("id");
        DataSet orgDateSet = TmcOrgDataHelper.getOrgDateSet(orgViewId);

        QFilter qFilter = this.buildCdmReceivablebillFilter(filter);
        DataSet cdmReceivablebill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cdm_receivablebill", "company", qFilter.toArray(), null)
                .groupBy(new String[]{"company"}).finish();

        DataSet dataSet = cdmReceivablebill.join(orgDateSet).on("company", "org").select(orgDateSet.getRowMeta().getFieldNames()).finish();
        if (dataSet != null) {
            dataSet.copy().forEach(c -> allOrgTreeIds.add(c.getLong("rowid")));

            this.queryOrgTreeNodeIds(orgDateSet, allOrgTreeIds);
            dataSet = orgDateSet.filter("rowid in allOrgTreeIds", Collections.singletonMap("allOrgTreeIds", allOrgTreeIds));

            // ”6+9“银票
            qFilter.and("draftbilltype.number",QCP.in,new String[]{"101","102"})
                    .and("accepterbebank.nckd_bankcredit_type",QCP.equals,"A");
            DataSet nckdamount1 = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cdm_receivablebill", "company,amount", qFilter.toArray(), null)
                    .groupBy(new String[]{"company"}).sum("amount").finish().select("company","amount nckd_amount1");

            dataSet = dataSet.leftJoin(nckdamount1).on("org","company").select(dataSet.getRowMeta().getFieldNames(),new String[]{"nckd_amount1"}).finish();

            //向上汇总
            dataSet = this.getSumDataSetByLevel(dataSet,sumFields);
        }
        return dataSet;
    }

    /**
     * 递归查找符合要求的节点id
     *
     * @param treeDS
     * @param ids
     * @return
     */
    private void queryOrgTreeNodeIds(DataSet treeDS, Set<Long> ids) {
        DataSet tempDS = treeDS.copy().filter("rowid in ids", Collections.singletonMap("ids", ids));
        if (!tempDS.copy().hasNext()) {
            return;
        } else {
            Set<Long> rowIds = new HashSet<>(16);
            tempDS.copy().forEach(c -> rowIds.add(c.getLong("pid")));
            allOrgTreeIds.addAll(rowIds);
            queryOrgTreeNodeIds(treeDS, rowIds);
        }
    }

    /**
     *添加合计到父级节点
     * @param orgTreeDS
     * @param sumFields
     * @return
     */
    private DataSet getSumDataSetByLevel(DataSet orgTreeDS,String [] sumFields){

        DataSet levelDs = orgTreeDS.copy().groupBy(new String[]{"level"}).finish();
        Set<Integer> levels = new HashSet();
        levelDs.forEach((o) -> {
            if (o.getInteger("level") != null) {
                levels.add(o.getInteger("level"));
            }
        });
        List<Integer> levelList = levels.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (Integer level:levelList) {
            String filter = "level=" + level;
            List<String> sumFieldList = Arrays.asList(sumFields);
            String sumFieldSts = this.getSumFields(sumFieldList, false,"");
            GroupbyDataSet groupbyDataSet = orgTreeDS.copy().filter(filter).select(sumFieldSts.split(",")).groupBy(new String[]{"pid"});
            for (String field:sumFields) {
                groupbyDataSet.sum("p" + field);
            }
            DataSet deltailSm = groupbyDataSet.finish();
            String joinFieldSts = this.getSumFields(sumFieldList, true, "");
            String selectFields = "orgname,orgid,"+joinFieldSts;
            orgTreeDS = orgTreeDS.leftJoin(deltailSm).on("rowid", "pid").select(selectFields.split(",")).finish();
        }

        return orgTreeDS;
    }

    private String getSumFields(List<String> sumFields, boolean isJoin, String customFields) {
        StringBuilder fieldStrs = new StringBuilder();
        String str = "pid,";
        if (isJoin) {
            str = "rowid, pid, isgroupnode, level,";
        }

        fieldStrs.append(str);

        String expStr;
        for(Iterator var6 = sumFields.iterator(); var6.hasNext(); fieldStrs.append(expStr).append(",")) {
            String field = (String)var6.next();
            expStr = field + " as p" + field;
            if (isJoin) {
                expStr = field + "+ p" + field + " as " + field;
            }
        }

        if (isJoin) {
            return fieldStrs.append(customFields).toString();
        } else {
            return fieldStrs.substring(0, fieldStrs.length());
        }
    }

    /**
     * 构建票据过滤条件
     *
     * @param filter
     * @return
     */
    private QFilter buildCdmReceivablebillFilter(FilterInfo filter) {
        // 开始日期
        FilterItemInfo startdate = filter.getFilterItem("nckd_startdate");
        // 结束日期
        FilterItemInfo enddate = filter.getFilterItem("nckd_enddate");
        // 资金组织
        FilterItemInfo mulcalorg = filter.getFilterItem("nckd_mulcalorg");

        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C");

        // 开始日期
        if (startdate.getValue() != null) {
            qFilter.and(new QFilter("bizdate", QCP.large_equals, startdate.getDate()));
        }
        // 结束日期
        if (enddate.getValue() != null) {
            qFilter.and(new QFilter("bizdate", QCP.less_equals, enddate.getDate()));
        }
        // 资金组织
        if (mulcalorg.getValue() != null) {
            qFilter = qFilter.and(new QFilter("company", QCP.in, ((DynamicObjectCollection) mulcalorg.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        return qFilter;
    }
}
