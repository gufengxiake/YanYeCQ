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
 * 订单流程查询表取数插件
 * 表单标识：nckd_orderprocessquery
 * author:zhangzhilong
 * date:2024/08/26
 */
public class OrderProcessQueryReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //限定单据为已审核
        //默认过滤单据状态不为暂存和已提交的
        QFilter qFilter = new QFilter("billstatus", QCP.not_equals2, "A")
                .and("billstatus", QCP.not_equals2, "B");
        qFilters.add(qFilter);
        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        DateTime outdateStart = null, outdateEnd = null;
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_saleorgid_q":
                    if(filterItem.getValue() != null){
                        Long  bizOrg =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("saleorgid",QCP.equals,bizOrg));
                    }
                    break;
                // 查询条件销售部门,标识如不一致,请修改
                case "nckd_departmentid_q":
                    if(filterItem.getValue() != null){
                        Long departmentid =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("departmentid",QCP.equals,departmentid));
                    }

                    break;
                // 查询条件客户名称,标识如不一致,请修改
                case "nckd_customerid_q":
                    if(filterItem.getValue() != null){
                        Long customer =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("customerid",QCP.equals,customer));
                    }

                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "orderdate_start":
                    if(filterItem.getDate() != null){
                        DateTime orderdateStart =  DateUtil.beginOfDay(filterItem.getDate());
                        qFilters.add(new QFilter("orderdate",QCP.large_equals,orderdateStart));
                    }
                    break;
                case "orderdate_end":
                    if(filterItem.getDate() != null){
                        DateTime orderdateEnd =  DateUtil.endOfDay(filterItem.getDate());
                        qFilters.add(new QFilter("orderdate",QCP.less_equals,orderdateEnd));
                    }
                    break;
                // 查询出库日期,标识如不一致,请修改
                case "outdate_start":
                    outdateStart = (filterItem.getDate() == null) ? null : DateUtil.beginOfDay(filterItem.getDate());
                    break;
                //
                case "outdate_end":
                    outdateEnd = (filterItem.getDate() == null) ? null : DateUtil.endOfDay(filterItem.getDate());
                    break;
            }
        }
        //取要货订单商品销售组织，
        String sFields = "saleorgid as nckd_saleorgid , " +
                //销售部门，
                "departmentid as nckd_departmentid , " +
                //订货客户，
                "customerid as nckd_customerid ," +
                //订单日期，
                "orderdate as nckd_orderdate , " +
                //订单号,
                "billno as nckd_billno," +
                //商品明显主键
                "itementry.id as orderdetailid";
        DataSet saleOrder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", sFields,
                qFilters.toArray(new QFilter[0])
                , null);

        saleOrder = this.linkOtherBills(saleOrder);

        if (saleOrder.isEmpty()) {
            return saleOrder;
        }
        //根据出库起始日期条件过滤
        if (outdateStart != null) {
            saleOrder = saleOrder.filter("nckd_outdate >= to_date('" + outdateStart + "','yyyy-MM-dd hh:mm:ss')");
        }
        //根据出库截止日期条件过滤
        if (outdateEnd != null) {
            saleOrder = saleOrder.filter("nckd_outdate <= to_date('" + outdateEnd + "','yyyy-MM-dd hh:mm:ss')");
        }
        return saleOrder.orderBy(new String[]{"nckd_saleorgid","nckd_orderdate","nckd_billno"});
    }


    //    关联其他单据
    public DataSet linkOtherBills(DataSet ds) {
        List<Long> orderdetailid = DataSetToList.getOneToList(ds, "orderdetailid");
        if (orderdetailid.isEmpty()){
            return ds;
        }

        //查询销售出库业务日期，
        String saleOutFields = "biztime as nckd_outdate , " +
                //单据号，
                "billno as nckd_outbillno , " +
                //数量，
                "billentry.qty as nckd_qtyout ," +
                //数量-已退库数量得出实发，
                " billentry.qty - billentry.returnqty as nckd_qty , " +
                //审核日期，
                "auditdate as nckd_outauditdate , " +
                //库管组，
                "operatorgroup as nckd_operatorgroup , " +
                //核心单据行id,
                "billentry.mainbillentryid as mainbillentryid , " +
                //销售出库id
                "id as saleoutid ";
        QFilter saleOutFilter = new QFilter("billentry.mainbillentryid", QCP.in, orderdetailid.toArray(new Long[0]));
        //限定单据为已审核
        saleOutFilter.and("billstatus", QCP.equals, "C");
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "im_saloutbill", saleOutFields, new QFilter[]{saleOutFilter}, null)
                .groupBy(new String[]{"nckd_outdate", "nckd_outbillno", "nckd_outauditdate", "nckd_operatorgroup", "mainbillentryid", "saleoutid"})
                .sum("nckd_qtyout", "nckd_qtyout").sum("nckd_qty", "nckd_qty").finish();

        //查询财务应付单
        QFilter arFilter = new QFilter("entry.corebillentryid", QCP.in, orderdetailid.toArray(new Long[0]));
        //限定单据为已审核
        arFilter.and("billstatus", QCP.equals, "C");
        DataSet finarBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "ar_finarbill",
                        //查询财务应收单来源单据id，
                        "entry.e_srcid as e_srcid , " +
                                //核心单据行id，
                                "entry.corebillentryid as ar_corebillentryid, " +
                                //单据编号，
                                "billno as nckd_arbillno ," +
                                //表头金额,
                                "amount as nckd_amountar , " +
//                                财务应收id
                                "id as finarid ", new QFilter[]{arFilter}, null)
                .groupBy(new String[]{"e_srcid", "ar_corebillentryid", "nckd_arbillno", "finarid"}).sum("nckd_amountar", "nckd_amountar").finish();


        //查询开票申请单
        QFilter aimFilter = new QFilter("sim_original_bill_item.corebillentryid", QCP.in, orderdetailid.toArray(new Long[0]));
        DataSet originalBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sim_original_bill",
                //查询开票申请单核心单据行id，
                "sim_original_bill_item.corebillentryid as sim_corebillentryid, " +
//                        审核日期，
                        "auditdate as nckd_invauditdate ," +
//                        发票号
                        "invoiceno as nckd_invbillno ," +
//                        数量
                        "sim_original_bill_item.num as nckd_qtyfinv ", new QFilter[]{aimFilter}, null);
//                .groupBy(new String[]{"sim_corebillentryid","nckd_invauditdate","nckd_invbillno"}).sum("nckd_qtyfinv","nckd_qtyfinv").finish();


        //查询收款单
        QFilter casFilter = new QFilter("entry.e_corebillentryid", QCP.in, orderdetailid.toArray(new Long[0]));
        //限定单据不为暂存或是已提交
        casFilter.and("billstatus", QCP.not_equals2, "A")
                .and("billstatus", QCP.not_equals2, "B");
        DataSet recBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "cas_recbill",
                        //查询收款单核心单据id，
                        "entry.e_corebillentryid as e_corebillentryid ," +
//                                单据编号
                                "billno as nckd_recbillno , " +
//                                表头金额
                                "actrecamt as nckd_amountrec", new QFilter[]{casFilter}, null)
                .groupBy(new String[]{"e_corebillentryid", "nckd_recbillno"}).sum("nckd_amountrec", "nckd_amountrec").finish();

        //销售出库关联财务应收 返回字段 销售出库日期，销售出库单据号，销售出库应发数量，销售出库实发数量，销售出库签字日期，销售出库库存组，销售出库核心单据行id
        //财务应收单据号，财务应收金额
        imSaloutbill = imSaloutbill.leftJoin(finarBill).on("mainbillentryid", "ar_corebillentryid").on("saleoutid", "e_srcid")
                .select(imSaloutbill.getRowMeta().getFieldNames(), finarBill.getRowMeta().getFieldNames()).finish();


        //销售出库关联开票申请，增加开票申请审核日期，开票申请发票号，开票申请数量
        imSaloutbill = imSaloutbill.leftJoin(originalBill).on("mainbillentryid", "sim_corebillentryid")
                .select(imSaloutbill.getRowMeta().getFieldNames(), originalBill.getRowMeta().getFieldNames()).finish();

        //关联上要货订单销售组织，销售部门，客户，订单日期，订单号，交付计划行id
        ds = ds.leftJoin(imSaloutbill).on("orderdetailid", "mainbillentryid")
                .select(ds.getRowMeta().getFieldNames(), imSaloutbill.getRowMeta().getFieldNames()).finish();

        //关联收款单单据号，收款金额
        ds = ds.leftJoin(recBill).on("orderdetailid", "e_corebillentryid")
                .select(ds.getRowMeta().getFieldNames(), recBill.getRowMeta().getFieldNames()).finish();

        imSaloutbill.close();
        finarBill.close();
        originalBill.close();
        recBill.close();

        return ds;
    }
}