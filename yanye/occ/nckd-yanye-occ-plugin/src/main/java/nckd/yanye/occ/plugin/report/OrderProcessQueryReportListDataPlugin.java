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
//        DataSet saleOrder = this.getSaleOrder(reportQueryParam);
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        DateTime outdate_start = null, outdate_end = null;
        DateTime orderdate_start = null, orderdate_end = null;
        Long bizOrg = null, departmentid = null, customer = null;
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_saleorgid_q":
                    bizOrg = (filterItem.getValue() == null) ? null : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
                // 查询条件销售部门,标识如不一致,请修改
                case "nckd_departmentid_q":
                    departmentid = (filterItem.getValue() == null) ? null : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
                // 查询条件客户名称,标识如不一致,请修改
                case "nckd_customerid_q":
                    customer = (filterItem.getValue() == null) ? null : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "orderdate_start":
                    orderdate_start = (filterItem.getDate() == null) ? null : DateUtil.beginOfDay(filterItem.getDate());
                    break;
                case "orderdate_end":
                    orderdate_end = (filterItem.getDate() == null) ? null : DateUtil.endOfDay(filterItem.getDate());
                    break;
                // 查询出库日期,标识如不一致,请修改
                case "outdate_start":
                    outdate_start = (filterItem.getDate() == null) ? null : DateUtil.beginOfDay(filterItem.getDate());
                    break;
                //
                case "outdate_end":
                    outdate_end = (filterItem.getDate() == null) ? null : DateUtil.endOfDay(filterItem.getDate());
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
//                qFilters.toArray(new QFilter[0])
                new QFilter[0]
                , null);

        saleOrder = this.linkOtherBills(saleOrder);

        //根据组织过滤
        if (bizOrg != null) {
            saleOrder = saleOrder.filter("nckd_saleorgid = " + bizOrg);
        }
        //根据部门过滤
        if (departmentid != null) {
            saleOrder = saleOrder.filter("nckd_departmentid = " + departmentid);
        }
        //根据客户过滤
        if (customer != null) {
            saleOrder = saleOrder.filter("nckd_customerid = " + customer);
        }
        //根据订单起始日期条件过滤
        if (orderdate_start != null) {
            saleOrder = saleOrder.filter("nckd_orderdate >= to_date('" + orderdate_start + "','yyyy-MM-dd hh:mm:ss')");
        }
        //根据订单截止日期条件过滤
        if (orderdate_end != null) {
            saleOrder = saleOrder.filter("nckd_orderdate <= to_date('" + orderdate_end + "','yyyy-MM-dd hh:mm:ss')");
        }
        //根据出库起始日期条件过滤
        if (outdate_start != null) {
            saleOrder = saleOrder.filter("nckd_outdate >= to_date('" + outdate_start + "','yyyy-MM-dd hh:mm:ss')");
        }
        //根据出库截止日期条件过滤
        if (outdate_end != null) {
            saleOrder = saleOrder.filter("nckd_outdate <= to_date('" + outdate_end + "','yyyy-MM-dd hh:mm:ss')");
        }
        return saleOrder.orderBy(saleOrder.getRowMeta().getFieldNames());
    }

    //获取要货订单相关信息
    public DataSet getSaleOrder(ReportQueryParam reportQueryParam) {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_saleorgid_q":
                    if (!(filterItem.getValue() == null)) {
                        Long bizOrg = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        QFilter qFilter = new QFilter("saleorgid", QCP.equals, bizOrg);
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件销售部门,标识如不一致,请修改
                case "nckd_departmentid_q":
                    if (!(filterItem.getValue() == null)) {
                        Long bizoperator = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        QFilter qFilter = new QFilter("departmentid", QCP.equals, bizoperator);
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件客户名称,标识如不一致,请修改
                case "nckd_customerid_q":
                    if (!(filterItem.getValue() == null)) {
                        Long customer = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        QFilter qFilter = new QFilter("customerid", QCP.equals, customer);
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "orderdate_start":
                    if (!(filterItem.getDate() == null)) {
                        QFilter qFilter = new QFilter("orderdate", QCP.large_equals, DateUtil.beginOfDay(filterItem.getDate()));
                        qFilters.add(qFilter);
                    }
                    break;
                case "orderdate_end":
                    if (!(filterItem.getDate() == null)) {
                        QFilter qFilter = new QFilter("orderdate", QCP.less_equals, DateUtil.endOfDay(filterItem.getDate()));
                        qFilters.add(qFilter);
                    }
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
                //交付计划主键
                "itementry.subentryentity.id as orderdetailid";
        DataSet saleOrder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", sFields,
                qFilters.toArray(new QFilter[0]), null);

        return saleOrder;
    }

    //    关联其他单据
    public DataSet linkOtherBills(DataSet ds) {
        List<Long> orderdetailid = DataSetToList.getOneToList(ds, "orderdetailid");
        if (orderdetailid.isEmpty())
            return ds;

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
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "im_saloutbill", saleOutFields, new QFilter[]{saleOutFilter}, null)
                .groupBy(new String[]{"nckd_outdate", "nckd_outbillno", "nckd_outauditdate", "nckd_operatorgroup", "mainbillentryid", "saleoutid"})
                .sum("nckd_qtyout", "nckd_qtyout").sum("nckd_qty", "nckd_qty").finish();

        //查询财务应付单
        QFilter arFilter = new QFilter("entry.corebillentryid", QCP.in, orderdetailid.toArray(new Long[0]));
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
        im_saloutbill = im_saloutbill.leftJoin(finarBill).on("mainbillentryid", "ar_corebillentryid").on("saleoutid", "e_srcid")
                .select(im_saloutbill.getRowMeta().getFieldNames(), finarBill.getRowMeta().getFieldNames()).finish();


        //销售出库关联开票申请，增加开票申请审核日期，开票申请发票号，开票申请数量
        im_saloutbill = im_saloutbill.leftJoin(originalBill).on("mainbillentryid", "sim_corebillentryid")
                .select(im_saloutbill.getRowMeta().getFieldNames(), originalBill.getRowMeta().getFieldNames()).finish();

        //关联上要货订单销售组织，销售部门，客户，订单日期，订单号，交付计划行id
        ds = ds.leftJoin(im_saloutbill).on("orderdetailid", "mainbillentryid")
                .select(ds.getRowMeta().getFieldNames(), im_saloutbill.getRowMeta().getFieldNames()).finish();

        //关联收款单单据号，收款金额
        ds = ds.leftJoin(recBill).on("orderdetailid", "e_corebillentryid")
                .select(ds.getRowMeta().getFieldNames(), recBill.getRowMeta().getFieldNames()).finish();

        im_saloutbill.close();
        finarBill.close();
        originalBill.close();
        recBill.close();

        return ds;
    }
}