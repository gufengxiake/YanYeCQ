package nckd.yanye.occ.plugin.report;

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
import kd.sdk.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 销售情况明细表取数插件
 * 表单标识：nckd_saledetail_rpt
 * author:zzl
 * date:2024/08/22
 */
public class SaledetailReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

//    销售组织，部门，业务员，客户，物料
//    发票类型，基本单位，辅单位，数量，含税单价，无税金额，税额
//    税率，折扣额，折扣率，总金额，结算成本，单据日期
//    审核日期，源头单据号，仓库，备注，核心单据行id,核心单据实体标识
//    省市区，详细地址，电话，收货人,退货标识
    private final String [] selectFields = {"nckd_bizorg","nckd_bizdept","nckd_bizoperator","nckd_customer","nckd_material",
            "nckd_fptype","nckd_baseunit","nckd_unit2nd","nckd_qty","nckd_priceandtax","nckd_amount","nckd_taxamount",
            "nckd_taxrateid","nckd_discountamount","nckd_discountrate","nckd_amountandtax","nckd_cbj","nckd_biztime",
            "nckd_auditdate","nckd_mainbillnumber","nckd_warehouse","nckd_comment","nckd_mainbillentryid","nckd_mainbillentity",
            "nckd_entryaddressid","nckd_entrydetailaddress","nckd_entrytelephone","nckd_entrycontactname","nckd_thsl"};

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        DataSet imSaleDS = this.getSaleOutBill(reportQueryParam);
        imSaleDS = this.linkSaleOrder(imSaleDS);
        return imSaleDS;
    }

    //获取销售出库单
    public DataSet getSaleOutBill(ReportQueryParam reportQueryParam){

        QFilter qFilter = new QFilter("1", QCP.equals,1);
        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件库存组织,标识如不一致,请修改
                case "nckd_bizorg_q":
                    if(!(filterItem.getValue() == null)){
                        Long bizOrg = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("bizorg", QCP.equals, bizOrg);
                    }
                    break;
                // 查询条件业务员,标识如不一致,请修改
                case "nckd_bizoperator_q":
                    if(! (filterItem.getValue() == null) ){
                        Long bizoperator =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("bizoperator", QCP.equals, bizoperator);
                    }
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "start":
                    if(! (filterItem.getDate() == null) ){
                        qFilter = qFilter.and("biztime", QCP.large_equals,
                                DateUtil.parse(new SimpleDateFormat("yyyy-MM-dd").format(filterItem.getDate()),"yyyy-MM-dd"));
                    }
                    break;
                case "end":
                    if(! (filterItem.getDate() == null) ){
                        qFilter = qFilter.and("biztime", QCP.less_equals,
                                DateUtil.parse(new SimpleDateFormat("yyyy-MM-dd").format(filterItem.getDate()),"yyyy-MM-dd"));
                    }
                    break;

                // 查询条件物料,标识如不一致,请修改
                case "nckd_customer_q":
                    if(! (filterItem.getValue() == null) ){
                        Long customer =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("customer", QCP.equals, customer);
                    }
                    break;
                case "nckd_bizdept_q":
                    if(! (filterItem.getValue() == null) ){
                        Long bizdept =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("bizdept", QCP.equals, bizdept);
                    }
                    break;
                case "nckd_warehouse_q":
                    if(! (filterItem.getValue() == null) ){
                        Long nckd_warehouse_q =  (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("billentry.warehouse", QCP.equals, nckd_warehouse_q);
                    }
                    break;
            }
        }


        String sFields = "bizorg AS nckd_bizorg,bizdept AS nckd_bizdept,bizoperator as nckd_bizoperator,customer as nckd_customer," +
                "billentry.material AS nckd_material,nckd_fptype as nckd_fptype,billentry.baseunit as nckd_baseunit," +
                "billentry.unit2nd as nckd_unit2nd,billentry.qty AS nckd_qty,billentry.priceandtax AS nckd_priceandtax," +
                "billentry.amount as nckd_amount,billentry.taxamount as nckd_taxamount,billentry.taxrateid as nckd_taxrateid,"+
                "billentry.discountamount as nckd_discountamount,billentry.discountrate as nckd_discountrate,billentry.amountandtax as nckd_amountandtax," +
                "billentry.nckd_cbj as nckd_cbj,biztime as nckd_biztime,auditdate as nckd_auditdate,billentry.mainbillnumber as nckd_mainbillnumber," +
                "billentry.warehouse as nckd_warehouse,comment as nckd_comment,billentry.mainbillid as mainbillid ," +
                "billentry.mainbillentryid as nckd_mainbillentryid , billentry.mainbillentity as nckd_mainbillentity";


        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, new QFilter[]{qFilter},null);

        return im_saloutbill;
    }

    //连接要货订单
    public DataSet linkSaleOrder(DataSet ds){
        DataSet copy = ds.copy();
        List<Long> mainbillentryid = new ArrayList<>();
        while (copy.hasNext()) {
            Row next = copy.next();
            if (next.getLong("nckd_mainbillentryid") != null) {
                mainbillentryid.add(next.getLong("nckd_mainbillentryid"));
            }
        }
        if(mainbillentryid.isEmpty())
            return ds;

        //取要货订单商品明细主键，省市区，详细地址，电话，收货人
        String sFields = "itementry.id as FEntryID,itementry.entryaddressid as nckd_entryaddressid ,itementry.entrydetailaddress as nckd_entrydetailaddress," +
                "itementry.entrytelephone as nckd_entrytelephone,itementry.entrycontactname as nckd_entrycontactname," +
                "itementry.joinreturnbaseqty as nckd_thsl";
        QFilter qFilter = new QFilter("itementry.id" ,QCP.in , mainbillentryid.toArray(new Long[0]));
        DataSet saleOrder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", sFields,
                new QFilter[]{qFilter},null);
        ds = ds.leftJoin(saleOrder).on("nckd_mainbillentryid","FEntryID")
                .select(selectFields)
                .finish();

        return ds;
    }

}