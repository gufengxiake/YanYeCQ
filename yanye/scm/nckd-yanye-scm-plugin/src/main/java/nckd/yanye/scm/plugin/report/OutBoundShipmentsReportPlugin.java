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

import java.util.ArrayList;
import java.util.List;

/**
 * 供应链-出库汇总表报表查询插件
 * 表单标识：nckd_outboundshipments
 * author：xiaoxiaopeng
 * date：2024-09-05
 */
public class OutBoundShipmentsReportPlugin extends AbstractReportListDataPlugin {

    /**
     * 核算成本记录/实际成本
     * 1、核算单类型=出库
     * 2、成本价来源不为空的成本金额相加
     */
    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        List<QFilter> qFilters_m = new ArrayList<>();
        qFilters.add(new QFilter("calbilltype", QCP.equals,"OUT"));
        qFilters.add(new QFilter("entry.costpricesource", QCP.not_equals,null));
        List<FilterItemInfo> filterItems = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filterItems) {
            switch (filterItem.getPropName()){
                //会计期间起始时间
                case "nckd_daterange_startdate":
                    if (filterItem.getValue() != null) {
                        QFilter qFilter = new QFilter("bookdate", QCP.large_equals, filterItem.getDate());
                        qFilters.add(qFilter);
                    }
                    break;
                //会计期间结束时间
                case "nckd_daterange_enddate":
                    if (filterItem.getValue() != null) {
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
                "cal_costrecord", "id,bookdate,storageorgunit nckd_org1,calbilltype,adminorg,entry,entry.stocktype.id,entry.id entrypk,entry.material.number as nckd_materialnum,entry.material as nckd_material, entry.material.id materialId,entry.material.modelnum as nckd_model" +
                        ",entry.assist as nckd_assist,entry.lot as nckd_lot,entry.configuredcode as nckd_configuredcode,entry.tracknumber as nckd_tracknumber, entry.ispresent as nckd_ispresent" +
                        ", entry.isrework as nckd_isrework, entry.baseunit as nckd_baseunit,entry.baseqty as nckd_baseqty, entry.unitactualcost as nckd_unitstandardcost,entry.actualcost as nckd_amount,entry.costpricesource",
                qFilters.toArray(new QFilter[0]), null);

        //关联物料查分组
        DataSet material = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_material", "id material_id,group nckd_classify1", qFilters_m.toArray(new QFilter[0]), null);
        DataSet materialDataset = costrecord.leftJoin(material).on("materialId", "material_id").select(new String[]{"id", "nckd_org1","nckd_materialnum","nckd_material","nckd_model","nckd_lot","nckd_assist","nckd_configuredcode","nckd_tracknumber","nckd_ispresent","nckd_isrework","nckd_baseunit","nckd_unitstandardcost","nckd_baseqty","nckd_amount"},
                new String[]{"material_id", "nckd_classify1"}).finish();

        DataSet resultDataset = materialDataset.groupBy(new String[]{"nckd_classify1","nckd_org1","nckd_materialnum","nckd_material","nckd_model","nckd_lot","nckd_assist","nckd_configuredcode","nckd_tracknumber","nckd_ispresent","nckd_isrework","nckd_baseunit","nckd_unitstandardcost"}).sum("nckd_baseqty").sum("nckd_amount").finish();
        return resultDataset;
    }
}
