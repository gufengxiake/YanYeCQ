package nckd.yanye.scm.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 供应链-入库汇总表报表查询插件
 * 表单标识：nckd_storagesummary
 * author：xiaoxiaopeng
 * date：2024-09-04
 */
public class StorageSummaryReportPlugin extends AbstractReportListDataPlugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        List<QFilter> qFilters_m = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        /**
         * 2、核算成本记录不统计的数据：
         * 1）暂估冲回的单
         * 2）标准直接调拨单
         * 3）核算单类型=出库
         */
        qFilters.add(new QFilter("calbilltype", QCP.equals,"IN"));
        qFilters.add(new QFilter("entry.costpricesource", QCP.not_equals,null));
        List<FilterItemInfo> filterItems = reportQueryParam.getFilter().getFilterItems();
        Date startdate = null;
        Date enddate = null;
        for (FilterItemInfo filterItem : filterItems) {
            switch (filterItem.getPropName()){
                //会计期间起始时间
                case "nckd_daterange_startdate":
                    if (filterItem.getValue() != null) {
                        startdate = filterItem.getDate();
                        QFilter qFilter = new QFilter("bookdate", QCP.large_equals, filterItem.getDate());
                        qFilters.add(qFilter);
                    }
                    break;
                //会计期间结束时间
                case "nckd_daterange_enddate":
                    if (filterItem.getValue() != null) {
                        enddate = filterItem.getDate();
                        QFilter qFilter = new QFilter("bookdate", QCP.less_equals, filterItem.getDate());
                        qFilters.add(qFilter);
                    }
                    break;
                    //库存组织
                case  "nckd_org":
                    if (filterItem.getValue() != null) {
                        DynamicObjectCollection pk = (DynamicObjectCollection) filterItem.getValue();
                        if (pk.size()>0) {
                            ArrayList<Object> ids = new ArrayList<>();
                            for (DynamicObject p : pk) {
                                ids.add(p.getPkValue());
                            }
                            qFilters.add(new QFilter("storageorgunit", QCP.in, ids));
                        }
                    }
                    break;
                    //部门
                case "nckd_dept":
                    if (filterItem.getValue() != null) {
                        DynamicObjectCollection pk = (DynamicObjectCollection) filterItem.getValue();
                        if (pk.size()>0) {
                            ArrayList<Object> ids = new ArrayList<>();
                            for (DynamicObject p : pk) {
                                ids.add(p.getPkValue());
                            }
                            qFilters.add(new QFilter("adminorg", QCP.in, ids));
                        }
                    }
                    break;
                    //存货分类
                case "nckd_stocktype":
                    if (filterItem.getValue() != null) {
                        DynamicObjectCollection stockPks = (DynamicObjectCollection) filterItem.getValue();
                        if (stockPks.size()>0) {
                            ArrayList<Object> ids = new ArrayList<>();
                            for (DynamicObject p : stockPks) {
                                ids.add(p.getPkValue());
                            }
                            qFilters.add(new QFilter("entry.stocktype.id", QCP.in, ids));
                        }
                    }
                    break;
                    //存货
                case "nckd_stockt":
                    if (filterItem.getValue() != null) {
                        DynamicObjectCollection stockPks = (DynamicObjectCollection) filterItem.getValue();
                        if (stockPks.size()>0) {
                            ArrayList<Object> ids = new ArrayList<>();
                            for (DynamicObject p : stockPks) {
                                ids.add(p.getPkValue());
                            }
                            qFilters.add(new QFilter("entry.material.id", QCP.in, ids));
                        }
                    }
                    break;
                case "nckd_classify":
                    if (filterItem.getValue() != null) {
                        DynamicObjectCollection classify = (DynamicObjectCollection) filterItem.getValue();
                        if (classify.size()>0) {
                            ArrayList<Object> ids = new ArrayList<>();
                            for (DynamicObject p : classify) {
                                ids.add(p.getPkValue());
                            }
                            qFilters_m.add(new QFilter("group.id", QCP.in, ids));
                        }
                    }
                    break;
            }
        }

        //核算成本记录
        DataSet costrecord = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "cal_costrecord", "id,bookdate,storageorgunit nckd_org1,storageorgunit,calbilltype,adminorg,entry,entry.stocktype.id,entry.id entrypk,entry.material.number as nckd_materialnum,entry.material nckd_material, entry.material.id materialId,entry.material.modelnum as nckd_model" +
                        ",entry.assist as nckd_assist,entry.lot nckd_lot,entry.configuredcode as nckd_configuredcode,entry.tracknumber as nckd_tracknumber, entry.ispresent as nckd_ispresent" +
                        ", entry.isrework as nckd_isrework, entry.baseunit as nckd_baseunit,entry.baseqty as nckd_baseqty, entry.unitactualcost as nckd_unitstandardcost,entry.actualcost nckd_amount,entry.costpricesource",
                qFilters.toArray(new QFilter[0]), null);
        costrecord = costrecord.groupBy(new String[]{"id","nckd_org1","storageorgunit","entry","entrypk","nckd_materialnum", "materialId","nckd_material", "nckd_model", "nckd_assist", "nckd_lot","nckd_configuredcode","nckd_tracknumber","nckd_ispresent","nckd_isrework","nckd_baseunit","nckd_unitstandardcost","nckd_amount"}).sum("nckd_amount").sum("nckd_baseqty").finish();

        //关联物料查分组
        DataSet material = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_material", "id material_id,group nckd_classify1", qFilters_m.toArray(new QFilter[0]), null);
        DataSet materialDataset = costrecord.leftJoin(material).on("materialId", "material_id").select(new String[]{"id", "nckd_org1", "materialId","storageorgunit", "entry", "entrypk", "nckd_materialnum", "nckd_material", "nckd_model", "nckd_assist", "nckd_lot", "nckd_configuredcode", "nckd_tracknumber", "nckd_ispresent", "nckd_isrework", "nckd_baseunit", "nckd_unitstandardcost", "nckd_amount", "nckd_baseqty"},
                new String[]{"material_id", "nckd_classify1"}).finish();

        QFilter calqFilter = new QFilter("biztype", QCP.equals, "A")
                .and(new QFilter("billstatus", QCP.equals, "C"));
        DataSet calCostadjustbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cal_costadjustbill", "entryentity.material masterid,entryentity.adjustamt adjustamt,entryentity.invbillid invbillid,entryentity.invbillentryid invbillentryid", calqFilter.toArray(), null)
                .groupBy(new String[]{"masterid", "invbillid", "invbillentryid"}).sum("adjustamt").finish();


        // 核算成本记录与成本调整单关联
        DataSet resultDataset = materialDataset.leftJoin(calCostadjustbill).on("nckd_material", "masterid").on("id", "invbillid").on("entrypk", "invbillentryid")
                .select(new String[]{"id","nckd_classify1","entry","nckd_org1","entrypk","nckd_materialnum", "nckd_material", "nckd_model", "nckd_assist", "nckd_lot","nckd_configuredcode","nckd_tracknumber","nckd_ispresent","nckd_isrework","nckd_baseunit","nckd_baseqty","nckd_unitstandardcost","nckd_amount"},
                        new String[]{"( CASE WHEN adjustamt IS NULL THEN 0 ELSE adjustamt END )nckd_differencamount"}).finish();
        resultDataset = resultDataset.select("nckd_classify1","nckd_org1","entry", "nckd_materialnum", "nckd_material", "nckd_model", "nckd_assist", "nckd_lot","nckd_configuredcode","nckd_tracknumber","nckd_ispresent","nckd_isrework","nckd_baseunit","nckd_baseqty","nckd_unitstandardcost","(nckd_differencamount + nckd_amount)nckd_amount","nckd_differencamount");
        resultDataset = resultDataset.groupBy(new String[]{"nckd_classify1","nckd_org1","nckd_materialnum", "nckd_material", "nckd_model", "nckd_assist", "nckd_lot","nckd_configuredcode","nckd_tracknumber","nckd_ispresent","nckd_isrework","nckd_baseunit","nckd_unitstandardcost"}).sum("nckd_baseqty").sum("nckd_amount").sum("nckd_differencamount").finish();

        return resultDataset;
    }
}
