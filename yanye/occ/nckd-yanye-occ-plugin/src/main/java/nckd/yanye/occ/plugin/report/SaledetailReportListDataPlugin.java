package nckd.yanye.occ.plugin.report;

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

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 报表取数插件
 */
public class SaledetailReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        DataSet imSaleDS = this.getSaleOutBill(reportQueryParam);
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


        String selectFields = "bizorg AS nckd_bizorg,bizdept AS nckd_bizdept,bizoperator as nckd_bizoperator,customer as nckd_customer," +
                "billentry.material AS nckd_material,nckd_fptype as nckd_fptype,billentry.baseunit as nckd_baseunit," +
                "billentry.unit2nd as nckd_unit2nd,billentry.qty AS nckd_qty,billentry.priceandtax AS nckd_priceandtax," +
                "billentry.amount as nckd_amount,billentry.taxamount as nckd_taxamount,billentry.taxrateid as nckd_taxrateid,"+
                "billentry.discountamount as nckd_discountamount,billentry.discountrate as nckd_discountrate,billentry.amountandtax as nckd_amountandtax," +
                "billentry.nckd_cbj as nckd_cbj,biztime as nckd_biztime,auditdate as nckd_auditdate,billentry.mainbillnumber as nckd_mainbillnumber," +
                "billentry.warehouse as nckd_warehouse,comment as nckd_comment";

        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", selectFields, new QFilter[]{qFilter},null);

        return im_saloutbill;
    }

    public DataSet linkSaleOrder(DataSet ds){
        return null;
    }
}