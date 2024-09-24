package nckd.yanye.tmc.report;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import kd.bos.algo.*;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.AppInfo;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.param.AppParam;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;
import kd.tmc.fbp.common.helper.TmcOrgDataHelper;

/**
 * @author husheng
 * @date 2024-09-19 13:40
 * @description 交易汇总表（nckd_transaction_summary）报表查询插件
 */
public class TransactionSummaryPlugin extends AbstractReportListDataPlugin {
    //符合要求的所有公司管理树节点的id
    private Set<Long> allOrgTreeIds = new HashSet<>(16);

    //汇总字段
    private static final String[] sumFields = new String[]{"nckd_amount1", "nckd_amount2", "nckd_amount3", "nckd_amountsum1", "nckd_duecollection",
            "nckd_amount4", "nckd_amount5", "nckd_amount6", "nckd_amountsum2",
            "nckd_amount7", "nckd_amount8", "nckd_amount9", "nckd_amountsum3",
            "nckd_amount10", "nckd_amount11", "nckd_amount12", "nckd_amountsum4",
            "nckd_receivablebalance"};


    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) {
        // 查询条件
        FilterInfo filter = reportQueryParam.getFilter();
        // 资金组织视图
        FilterItemInfo filterOrgview = filter.getFilterItem("nckd_filter_orgview");
        Long orgViewId = ((DynamicObject) filterOrgview.getValue()).getLong("id");
        DataSet orgDateSet = TmcOrgDataHelper.getOrgDateSet(orgViewId);
        // 资金组织
        FilterItemInfo mulcalorg = filter.getFilterItem("nckd_mulcalorg");
        ((DynamicObjectCollection) mulcalorg.getValue()).forEach(c -> allOrgTreeIds.add(c.getLong("id")));

        // 重构树形
        this.queryOrgTreeNodeIds(orgDateSet, allOrgTreeIds);
        DataSet dataSet = orgDateSet.filter("rowid in allOrgTreeIds", Collections.singletonMap("allOrgTreeIds", allOrgTreeIds));

        DynamicObjectCollection dynamicObjectCollection = ORM.create().toPlainDynamicObjectCollection(dataSet.copy());
        List<Long> pids = dynamicObjectCollection.stream().map(t -> t.getLong("pid")).collect(Collectors.toList());
        dynamicObjectCollection.forEach(t -> {
            if ("1".equals(t.getString("isgroupnode")) && !pids.contains(t.getLong("orgid"))) {
                t.set("isgroupnode", "0");
            }
        });
        dataSet = buildDataByObjCollection("algoKey", dataSet.getRowMeta().getFields(), dynamicObjectCollection);

        // 本期间收取
        dataSet = this.getCurrentcollection(dataSet, filter);

        // 到期收款
        QFilter qFilter1 = this.buildCdmReceivablebillFilter(filter, "2").and("draftbillstatus", QCP.equals, "collected");
        DataSet nckdduecollection = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cdm_receivablebill", "company,amount", qFilter1.toArray(), null)
                .groupBy(new String[]{"company"}).sum("amount").finish().select("company", "amount nckd_duecollection");
        dataSet = dataSet.leftJoin(nckdduecollection).on("org", "company").select(dataSet.getRowMeta().getFieldNames(), new String[]{"nckd_duecollection"}).finish();

        // 背书
        dataSet = this.getEndorsement(dataSet, filter);

        // 贴现
        dataSet = this.getDiscount(dataSet, filter);

        // 本期间
        dataSet = this.getCurrentperiod(dataSet, filter);

        // 本期间应收票据余额
        dataSet = dataSet.addField("0", "nckd_receivablebalance");

        DynamicObjectCollection dynamicObjects = ORM.create().toPlainDynamicObjectCollection(dataSet.copy());
        dynamicObjects.forEach(t -> {
            Boolean parameter = (Boolean) getSysCtrlParameter("cdm", "nckd_endorsementtransfer", t.getLong("org"));
            BigDecimal result = t.getBigDecimal("nckd_amountsum4").add(t.getBigDecimal("nckd_amount5")).add(t.getBigDecimal("nckd_amount8"));
            if (parameter) {
                result = result.subtract(t.getBigDecimal("nckd_amount10"));
                t.set("nckd_receivablebalance", result);
            } else {
                t.set("nckd_receivablebalance", result);
            }
        });

        dataSet = buildDataByObjCollection("algoKey", dataSet.getRowMeta().getFields(), dynamicObjects);

        // 向上汇总
        dataSet = this.getSumDataSetByLevel(dataSet, sumFields);

        return dataSet;
    }

    //DynamicObjectCollection 转换为 DataSet 方法
    public DataSet buildDataByObjCollection(String algoKey, Field[] rowFields, DynamicObjectCollection objCollection) {
        DataSetBuilder dataSetBuilder = Algo.create(algoKey + ".emptyFields")
                .createDataSetBuilder(new RowMeta(rowFields));
        for (DynamicObject arObj : objCollection) {
            Object[] rowData = new Object[rowFields.length];
            for (int i = 0; i < rowFields.length; i++) {
                Field field = rowFields[i];
                rowData[i] = arObj.get(field.getName());
            }
            dataSetBuilder.append(rowData);
        }
        return dataSetBuilder.build();
    }

    // 获取系统参数
    public Object getSysCtrlParameter(String appNumber, String paramPro, Long orgId) {
        AppParam appParam = new AppParam();
        AppInfo appInfo = AppMetadataCache.getAppInfo(appNumber);
        String appId = appInfo.getId();
        appParam.setAppId(appId);
        appParam.setOrgId(orgId);
        Map<String, Object> map = SystemParamServiceHelper.loadAppParameterFromCache(appParam);
        return map == null ? null : map.get(paramPro);
    }

    /**
     * 本期间收取
     *
     * @param dataSet
     * @param filter
     * @return
     */
    private DataSet getCurrentcollection(DataSet dataSet, FilterInfo filter) {
        QFilter qFilter = this.buildCdmReceivablebillFilter(filter, "1");

        // ”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter1(qFilter), "nckd_amount1");

        // 非”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter2(qFilter), "nckd_amount2");

        // 商票
        dataSet = this.getAmount(dataSet, this.buildQFilter3(qFilter), "nckd_amount3");

        // 小计
        dataSet = dataSet.addField("nckd_amount1 + nckd_amount2 + nckd_amount3", "nckd_amountsum1");

        return dataSet;
    }

    /**
     * 背书
     *
     * @param dataSet
     * @param filter
     * @return
     */
    private DataSet getEndorsement(DataSet dataSet, FilterInfo filter) {
        QFilter qFilter = this.buildCdmReceivablebillFilter(filter, "2")
                .and("draftbillstatus", QCP.equals, "endorsed");

        // ”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter1(qFilter), "nckd_amount4");

        // 非”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter2(qFilter), "nckd_amount5");

        // 商票
        dataSet = this.getAmount(dataSet, this.buildQFilter3(qFilter), "nckd_amount6");

        // 小计
        dataSet = dataSet.addField("nckd_amount4 + nckd_amount5 + nckd_amount6", "nckd_amountsum2");

        return dataSet;
    }

    /**
     * 贴现
     *
     * @param dataSet
     * @param filter
     * @return
     */
    private DataSet getDiscount(DataSet dataSet, FilterInfo filter) {
        QFilter qFilter = this.buildCdmReceivablebillFilter(filter, "2")
                .and("draftbillstatus", QCP.equals, "discounted");

        // ”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter1(qFilter), "nckd_amount7");

        // 非”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter2(qFilter), "nckd_amount8");

        // 商票
        dataSet = this.getAmount(dataSet, this.buildQFilter3(qFilter), "nckd_amount9");

        // 小计
        dataSet = dataSet.addField("nckd_amount7 + nckd_amount8 + nckd_amount9", "nckd_amountsum3");

        return dataSet;
    }

    /**
     * 本期间
     *
     * @param dataSet
     * @param filter
     * @return
     */
    private DataSet getCurrentperiod(DataSet dataSet, FilterInfo filter) {
        QFilter qFilter = this.buildCdmReceivablebillFilter(filter, "3");

        // ”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter1(qFilter), "nckd_amount10");

        // 非”6+9“银票
        dataSet = this.getAmount(dataSet, this.buildQFilter2(qFilter), "nckd_amount11");

        // 商票
        dataSet = this.getAmount(dataSet, this.buildQFilter3(qFilter), "nckd_amount12");

        // 小计
        dataSet = dataSet.addField("nckd_amount10 + nckd_amount11 + nckd_amount12", "nckd_amountsum4");

        return dataSet;
    }

    /**
     * 构建”6+9“银票过滤条件
     *
     * @param qFilter
     * @return
     */
    private QFilter buildQFilter1(QFilter qFilter) {
        return qFilter.copy().and("draftbilltype.number", QCP.in, new String[]{"101", "102"})
                .and("accepterbebank.nckd_bankcredit_type", QCP.equals, "A");
    }

    /**
     * 构建非”6+9“银票过滤条件
     *
     * @param qFilter
     * @return
     */
    private QFilter buildQFilter2(QFilter qFilter) {
        return qFilter.copy().and("draftbilltype.number", QCP.in, new String[]{"101", "102"})
                .and("accepterbebank.nckd_bankcredit_type", QCP.in, new String[]{"B", "C"});
    }

    /**
     * 构建商票过滤条件
     *
     * @param qFilter
     * @return
     */
    private QFilter buildQFilter3(QFilter qFilter) {
        return qFilter.copy().and("draftbilltype.number", QCP.in, new String[]{"103", "104"});
    }

    /**
     * 获取金额
     *
     * @param dataSet
     * @param qFilter
     * @param s       字段标识
     * @return
     */
    private DataSet getAmount(DataSet dataSet, QFilter qFilter, String s) {
        DataSet nckdamount1 = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cdm_receivablebill", "company,amount", qFilter.toArray(), null)
                .groupBy(new String[]{"company"}).sum("amount").finish().select("company", "amount");

        dataSet = dataSet.leftJoin(nckdamount1).on("org", "company").select(dataSet.getRowMeta().getFieldNames(), new String[]{"amount " + s}).finish();

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
     * 添加合计到父级节点
     *
     * @param orgTreeDS
     * @param sumFields
     * @return
     */
    private DataSet getSumDataSetByLevel(DataSet orgTreeDS, String[] sumFields) {

        DataSet levelDs = orgTreeDS.copy().groupBy(new String[]{"level"}).finish();
        Set<Integer> levels = new HashSet();
        levelDs.forEach((o) -> {
            if (o.getInteger("level") != null) {
                levels.add(o.getInteger("level"));
            }
        });
        List<Integer> levelList = levels.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (Integer level : levelList) {
            String filter = "level=" + level;
            List<String> sumFieldList = Arrays.asList(sumFields);
            String sumFieldSts = this.getSumFields(sumFieldList, false, "");
            GroupbyDataSet groupbyDataSet = orgTreeDS.copy().filter(filter).select(sumFieldSts.split(",")).groupBy(new String[]{"pid"});
            for (String field : sumFields) {
                groupbyDataSet.sum("p" + field);
            }
            DataSet deltailSm = groupbyDataSet.finish();
            String joinFieldSts = this.getSumFields(sumFieldList, true, "");
            String selectFields = "orgname,orgid," + joinFieldSts;
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
        for (Iterator var6 = sumFields.iterator(); var6.hasNext(); fieldStrs.append(expStr).append(",")) {
            String field = (String) var6.next();
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
    private QFilter buildCdmReceivablebillFilter(FilterInfo filter, String type) {
        // 开始日期
        FilterItemInfo startdate = filter.getFilterItem("nckd_startdate");
        // 结束日期
        FilterItemInfo enddate = filter.getFilterItem("nckd_enddate");
        // 资金组织
        FilterItemInfo mulcalorg = filter.getFilterItem("nckd_mulcalorg");

        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C")
                .and(new QFilter("company", QCP.in, ((DynamicObjectCollection) mulcalorg.getValue())
                        .stream()
                        .map(obj -> obj.getLong("id"))
                        .collect(Collectors.toList())));

        if ("1".equals(type)) {
            // 开始日期
            qFilter.and(new QFilter("bizdate", QCP.large_equals, startdate.getDate()));
            // 结束日期
            qFilter.and(new QFilter("bizdate", QCP.less_equals, enddate.getDate()));
        } else if ("2".equals(type)) {
            // 开始日期
            qFilter.and(new QFilter("bizfinishdate", QCP.large_equals, startdate.getDate()));
            // 结束日期
            qFilter.and(new QFilter("bizfinishdate", QCP.less_equals, enddate.getDate()));
        } else if ("3".equals(type)) {
            qFilter.and(new QFilter("bizdate", QCP.large_equals, startdate.getDate())
                    .and("bizdate", QCP.less_equals, enddate.getDate())
                    .and("bizfinishdate", QCP.large_than, enddate.getDate())
                    .or(new QFilter("bizdate", QCP.less_than, startdate.getDate())
                            .or("bizdate", QCP.large_than, enddate.getDate())
                            .and("draftbillstatus", QCP.equals, "registered")));
        }

        return qFilter;
    }
}
