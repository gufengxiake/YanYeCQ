package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 销售管理报表-报表取数插件
 * 表单标识：nckd_salemanage_rpt
 * author:zhangzhilong
 * date:2024/08/28
 */
public class FtManageReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //限定组织为晶昊本部和江西富达盐化有限公司的销售出库单
        QFilter  orgfilter= new QFilter("bizorg.number", QCP.in,new String[]{"11901","121"});
        qFilters.add(orgfilter);
        //限定单据为已审核
        QFilter filter = new QFilter("billstatus", QCP.equals, "C");
        qFilters.add(filter);
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                case "nckd_org_q":
                    if (filterItem.getValue() != null) {
                        Long nckd_org_q = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        QFilter qFilter = new QFilter("bizorg", QCP.equals, nckd_org_q);
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件收货单位,标识如不一致,请修改
                case "nckd_customer_q":
                    if (filterItem.getValue() != null) {
                        Long nckd_customer_q = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        QFilter qFilter = new QFilter("customer", QCP.equals, nckd_customer_q);
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件发货日期,标识如不一致,请修改
                case "nckd_fhdate_q_start":
                    if (filterItem.getDate() != null) {
                        DateTime start = DateUtil.beginOfDay(filterItem.getDate());
                        QFilter qFilter = new QFilter("biztime", QCP.large_equals, start);
                        qFilters.add(qFilter);
                    }
                    break;
                case "nckd_fhdate_q_end":
                    if (filterItem.getDate() != null) {
                        DateTime end =  DateUtil.endOfDay(filterItem.getDate());
                        QFilter qFilter = new QFilter("biztime", QCP.less_equals, end);
                        qFilters.add(qFilter);
                    }
                    break;
            }
        }

        //公司
        String sFields ="bizorg.name as nckd_bizorg," +
                         //客户编码
                         "customer as nckd_customer ," +
//                        存货编码
                        "billentry.material as nckd_material ," +
//                       发货日期
                        "biztime as nckd_fhdate," +
//                        发货单号
                        "billno as nckd_fhbillno ," +
//                        发货数量
                        "billentry.qty as nckd_quantity," +
//                        销售单价
                        "billentry.price as nckd_price," +
//                        销售金额
                        "billentry.amount as nckd_amount," +
//                        税额
                        "billentry.taxamount as nckd_taxamount," +
//                        运价
                        "billentry.nckd_pricefieldyf1 as nckd_pricefieldyf1," +
//                         运费结算方式
                         "nckd_freighttype as nckd_freighttype," +
//                      销售出库表体id
                        "billentry.id as saleoutbodyid," +
//                        核心单据行id
                        "billentry.mainbillentryid as mainbillentryid";
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]) , null);
        im_saloutbill = this.linkSignAtureBill(im_saloutbill);

        return im_saloutbill.orderBy(new String[]{"nckd_bizorg","nckd_fhdate"});
    }

    //获取签收单  nckd_signaturebill
    public DataSet linkSignAtureBill(DataSet ds) {
        List<Long> mainbillentryidToList = DataSetToList.getMainbillentryidToList(ds);
        if (mainbillentryidToList.isEmpty()) return ds;

        //关联销售订单
        QFilter orderFilter = new QFilter("billentry.id", QCP.in, mainbillentryidToList.toArray(new Long[0]));
        DataSet sm_salorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "sm_salorder"
                //      销售合同号
                ,"nckd_salecontractno as nckd_salecontractno," +
                //        运输合同号
                        "nckd_trancontractno as nckd_trancontractno," +
//                        销售订单表体id
                        "billentry.id as orderbodyid",
                new QFilter[]{orderFilter}, null);
        ds = ds.leftJoin(sm_salorder).on("mainbillentryid","orderbodyid").select(ds.getRowMeta().getFieldNames(),sm_salorder.getRowMeta().getFieldNames()).finish();

//        关联签收单
        QFilter signFilter = new QFilter("entryentity.nckd_mainentrybill", QCP.in, mainbillentryidToList.toArray(new Long[0]));
        //限定单据为已审核
        signFilter.and("billstatus", QCP.equals, "C");
        DataSet nckd_signaturebill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "nckd_signaturebill",
//                发货日期
//                "nckd_signdate as nckd_fhdate," +
//                       出库数量-签收数量 = 途损数量
                        "entryentity.nckd_outstockqty - entryentity.nckd_signqty as nckd_damageqty," +
//                        签收数量
                        "entryentity.nckd_signqty as nckd_signqty,"+
//                        磅单号
                        "entryentity.nckd_eleno as nckd_eleno," +
//                        车号
                        "entryentity.nckd_cpno1 as nckd_cpno1," +
//                        船柜号
                        "entryentity.nckd_ship as nckd_ship," +
//                        报关单号
                        "nckd_customsno as nckd_customsno," +
//                       目的地
                        "nckd_harbor as nckd_harbor," +
//                        来源单据行id
                        "entryentity.nckd_sourceentryid as nckd_sourceentryid",
                new QFilter[]{signFilter}, null);
        ds = ds.leftJoin(nckd_signaturebill).on("saleoutbodyid","nckd_sourceentryid")
                .select(ds.getRowMeta().getFieldNames(),nckd_signaturebill.getRowMeta().getFieldNames()).finish();

        //关联开票申请单
        QFilter aimFilter = new QFilter("sim_original_bill_item.corebillentryid" , QCP.in , mainbillentryidToList.toArray(new Long[0]));
        DataSet originalBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sim_original_bill",
                //查询开票申请单核心单据行id，
                "sim_original_bill_item.corebillentryid as sim_corebillentryid, " +
//                        开票日期，
                        "billdate as nckd_invbilldate ," +
//                        发票号
                        "invoiceno as nckd_invbillno ," +
//                        开票数量
                        "sim_original_bill_item.num as nckd_qtyfinv ",new QFilter[]{aimFilter},null);
        ds = ds.leftJoin(originalBill).on("mainbillentryid","sim_corebillentryid")
                .select(ds.getRowMeta().getFieldNames(),originalBill.getRowMeta().getFieldNames()).finish();
        return ds;
    }
}