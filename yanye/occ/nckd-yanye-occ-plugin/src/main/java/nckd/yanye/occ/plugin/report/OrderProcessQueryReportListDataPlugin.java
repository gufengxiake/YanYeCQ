package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.util.StringUtils;
import kd.sdk.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 订单流程查询表取数插件
 * 表单标识：nckd_orderprocessquery
 * author:zzl
 * date:2024/08/26
 */
public class OrderProcessQueryReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        DataSet saleOrder = this.getSaleOrder(reportQueryParam);
        saleOrder = this.linkOtherBills(saleOrder);
        DateTime outdate_start = null;
        DateTime outdate_end = null;
        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询出库日期,标识如不一致,请修改
                case "outdate_start":
                    outdate_start =(filterItem.getDate() == null) ? null : DateUtil.beginOfDay(filterItem.getDate());
                    break;
                //
                case "outdate_end":
                    outdate_end =(filterItem.getDate() == null) ? null : DateUtil.endOfDay(filterItem.getDate());
                    break;
            }
        }
        //根据出库日期条件再过滤
        if(outdate_start != null && outdate_end != null){
            saleOrder = saleOrder.filter("nckd_outdate >= to_date('" +  outdate_start + "','yyyy-MM-dd')" ).
                    filter("nckd_outdate <= to_date('" +  outdate_end + "','yyyy-MM-dd')" );
        }
        return saleOrder;
    }

    //获取要货订单相关信息
    public DataSet getSaleOrder(ReportQueryParam reportQueryParam){
        QFilter qFilter = new QFilter("1", QCP.equals,1);
        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_saleorgid_q":
                    if(!(filterItem.getValue() == null)){
                        Long bizOrg = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("saleorgid", QCP.equals, bizOrg);
                    }
                    break;
                // 查询条件销售部门,标识如不一致,请修改
                case "nckd_departmentid_q":
                    if(! (filterItem.getValue() == null) ){
                        Long bizoperator =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("departmentid", QCP.equals, bizoperator);
                    }
                    break;
                // 查询条件客户名称,标识如不一致,请修改
                case "nckd_customerid_q":
                    if(! (filterItem.getValue() == null) ){
                        Long customer =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("customerid", QCP.equals, customer);
                    }
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "orderdate_start":
                    if(! (filterItem.getDate() == null) ){
                        qFilter = qFilter.and("orderdate", QCP.large_equals,
                                DateUtil.beginOfDay(filterItem.getDate()));
                    }
                    break;
                case "orderdate_end":
                    if(! (filterItem.getDate() == null) ){
                        qFilter = qFilter.and("orderdate", QCP.less_equals,
                                DateUtil.endOfDay(filterItem.getDate()));
                    }
                    break;
            }
        }
        //取要货订单商品销售组织，销售部门，订货客户，订单日期，订单号,交付计划主键
        String sFields = "saleorgid as nckd_saleorgid , departmentid as nckd_departmentid , customerid as nckd_customerid ," +
                "orderdate as nckd_orderdate , billno as nckd_billno,itementry.subentryentity.id as orderdetailid" ;
        DataSet saleOrder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", sFields,
                new QFilter[]{qFilter},null);

        return saleOrder;
    }

//    关联其他单据
    public DataSet linkOtherBills(DataSet ds){
        DataSet copy = ds.copy();
        List<Long> orderdetailid = new ArrayList<>();
        while (copy.hasNext()) {
            Row next = copy.next();
            if (next.getLong("orderdetailid") != null) {
                orderdetailid.add(next.getLong("orderdetailid"));
            }
        }
        if(orderdetailid.isEmpty())
            return ds;

        //查询销售出库业务日期，单据号，数量，数量-已退库数量得出实发，审核日期，库管组，核心单据行id,销售出库id
        String saleOutFields = "biztime as nckd_outdate , billno as nckd_outbillno , billentry.qty as nckd_qtyout ," +
                " billentry.qty - billentry.returnqty as nckd_qty , auditdate as nckd_outauditdate , " +
                "operatorgroup as nckd_operatorgroup , billentry.mainbillentryid as mainbillentryid , id as saleoutid ";
        QFilter saleOutFilter = new QFilter("billentry.mainbillentryid" ,QCP.in , orderdetailid.toArray(new Long[0]));
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", saleOutFields, new QFilter[]{saleOutFilter},null)
                .groupBy(new String[]{"nckd_outdate","nckd_outbillno","nckd_outauditdate","nckd_operatorgroup","mainbillentryid","saleoutid"})
                .sum("nckd_qtyout","nckd_qtyout").sum( "nckd_qty","nckd_qty").finish();

        //查询财务应收单来源单据id，核心单据行id，单据编号，表头金额,财务应收id
        QFilter arFilter = new QFilter("entry.corebillentryid" , QCP.in , orderdetailid.toArray(new Long[0]));
        DataSet finarBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "ar_finarbill", "entry.e_srcid as e_srcid , entry.corebillentryid as ar_corebillentryid, " +
                                "billno as nckd_arbillno ,amount as nckd_amountar , id as finarid ",new QFilter[]{arFilter},null)
                .groupBy(new String[]{"e_srcid","ar_corebillentryid","nckd_arbillno","finarid"}).sum("nckd_amountar","nckd_amountar").finish();

        //查询开票申请单核心单据行id，审核日期，发票号，数量
        QFilter aimFilter = new QFilter("sim_original_bill_item.corebillentryid" , QCP.in , orderdetailid.toArray(new Long[0]));
        DataSet originalBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sim_original_bill", "sim_original_bill_item.corebillentryid as sim_corebillentryid, auditdate as nckd_invauditdate ," +
                        "invoiceno as nckd_invbillno ,sim_original_bill_item.num as nckd_qtyfinv ",new QFilter[]{aimFilter},null);
//                .groupBy(new String[]{"sim_corebillentryid","nckd_invauditdate","nckd_invbillno"}).sum("nckd_qtyfinv","nckd_qtyfinv").finish();

        //查询收款单来源单据id，核心单据行id，单据编号，表头金额,财务应收id
        QFilter casFilter = new QFilter("entry.e_corebillentryid" , QCP.in , orderdetailid.toArray(new Long[0]));
        DataSet recBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "cas_recbill", "entry.e_corebillentryid as e_corebillentryid ," +
                                "billno as nckd_recbillno , actrecamt as nckd_amountrec",new QFilter[]{casFilter},null)
                .groupBy(new String[]{"e_corebillentryid","nckd_recbillno"}).sum("nckd_amountrec","nckd_amountrec").finish();


        ArrayList<String> selectFields = new ArrayList<>();

        //销售出库关联财务应收 返回字段 销售出库日期，销售出库单据号，销售出库应发数量，销售出库实发数量，销售出库签字日期，销售出库库存组，销售出库核心单据行id
        //财务应收单据号，财务应收金额
        Collections.addAll(selectFields, new String[]{"nckd_outdate","nckd_outbillno","nckd_qtyout","nckd_qty","nckd_outauditdate",
                "nckd_operatorgroup","mainbillentryid","nckd_arbillno","nckd_amountar"});
        im_saloutbill = im_saloutbill.leftJoin(finarBill).on("mainbillentryid","ar_corebillentryid")
                .on("saleoutid","e_srcid").select(selectFields.toArray(new String[0])).finish();


        //销售出库关联开票申请，增加开票申请审核日期，开票申请发票号，开票申请数量
        Collections.addAll(selectFields,new String[]{"nckd_invauditdate","nckd_invbillno","nckd_qtyfinv",});
        im_saloutbill = im_saloutbill.leftJoin(originalBill).on("mainbillentryid","sim_corebillentryid")
                .select(selectFields.toArray(new String[0])).finish();

        //关联上要货订单销售组织，销售部门，客户，订单日期，订单号，交付计划行id
        Collections.addAll(selectFields, new String[]{"nckd_saleorgid","nckd_departmentid","nckd_customerid","nckd_orderdate","nckd_billno","orderdetailid"});
        ds = ds.leftJoin(im_saloutbill).on("orderdetailid","mainbillentryid").select(selectFields.toArray(new String[0])).finish();

        //关联收款单单据号，收款金额
        selectFields.add("nckd_recbillno");
        selectFields.add("nckd_amountrec");
        ds = ds.leftJoin(recBill).on("orderdetailid","e_corebillentryid").select(selectFields.toArray(new String[0])).finish();

        return ds;
    }
}