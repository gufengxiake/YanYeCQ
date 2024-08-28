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

import java.util.ArrayList;
import java.util.List;

/**
 * 外贸管理报表-报表取数插件
 * 表单标识：nckd_ftmanage_rpt
 * author:zzl
 * date:2024/08/28
 */
public class FtManageReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        QFilter qFilter = new QFilter("1", QCP.equals, 1);
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件收货单位,标识如不一致,请修改
                case "nckd_customer_q":
                    if (!(filterItem.getValue() == null)) {
                        Long nckd_customer_q = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("nckd_customer", QCP.equals, nckd_customer_q);
                    }
                    break;
                // 查询条件发货日期,标识如不一致,请修改
                case "nckd_fhdate_q_start":
                    if (!(filterItem.getDate() == null)) {
                        qFilter = qFilter.and("detailentry.nckd_fhdate", QCP.large_equals, DateUtil.beginOfDay(filterItem.getDate()));
                    }
                    break;
                case "nckd_fhdate_q_end":
                    if (!(filterItem.getDate() == null)) {
                        qFilter = qFilter.and("detailentry.nckd_fhdate", QCP.less_equals, DateUtil.endOfDay(filterItem.getDate()));
                    }
                    break;
            }
        }

        String sFields =
                //客户编码
                "nckd_customer as nckd_customer ," +
//                        存货编码
                        "detailentry.material as nckd_material ," +
//                        发货日期
                        "detailentry.nckd_fhdate as nckd_fhdate ," +
//                        发货单号
                        "detailentry.nckd_fhbillno as nckd_fhbillno ," +
//                        发货数量
                        "detailentry.quantity as nckd_quantity," +
//                        途损数
                        "detailentry.nckd_damageqty as nckd_damageqty," +
//                        客户签收数量
                        "detailentry.nckd_receiveqty as nckd_receiveqty," +
//                        运价
                        "detailentry.pricetax as nckd_pricetax," +
//                        价税合计
                        "detailentry.e_pricetaxtotal as nckd_e_pricetaxtotal," +
//                        运费结算方式
                        "nckd_freighttype as nckd_freighttype," +
//                        磅单号
                        "detailentry.nckd_poundnumber as nckd_poundnumber," +
//                        车号
                        "detailentry.nckd_licensenumber as nckd_licensenumber," +
//                        船柜号
                        "detailentry.nckd_shipnumber as nckd_shipnumber," +
//                        报关单号
                        "detailentry.nckd_declarationnumber as nckd_declarationnumber," +
//                        运输合同号
                        "detailentry.nckd_carriagenumber as nckd_carriagenumber," +
//                        核心单据行id
                        "detailentry.corebillentryid as corebillentryid";
        DataSet finapBill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "ap_finapbill", sFields, new QFilter[]{qFilter}, null);
        finapBill = this.linkSignAtureBill(finapBill);
        return finapBill.orderBy(finapBill.getRowMeta().getFieldNames());
    }

    //获取签收单  nckd_signaturebill
    public DataSet linkSignAtureBill(DataSet ds) {
        DataSet copy = ds.copy();
        List<Long> corebillentryid = new ArrayList<>();
        while (copy.hasNext()) {
            Row next = copy.next();
            if (next.getLong("corebillentryid") != null) {
                corebillentryid.add(next.getLong("corebillentryid"));
            }
        }
        if (corebillentryid.isEmpty()) return ds;

        QFilter signFilter = new QFilter("entryentity.nckd_mainentrybill", QCP.in, corebillentryid.toArray(new Long[0]));

        DataSet nckd_signaturebill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "nckd_signaturebill",
//                       目的地
                        "nckd_harbor as nckd_harbor," +
//                        核心单据行id
                        "entryentity.nckd_mainentrybill as nckd_mainentrybill," +
//                        来源单据号
                        "entryentity.nckd_srcbillnumber as nckd_srcbillnumber",
                new QFilter[]{signFilter}, null);
        ds = ds.leftJoin(nckd_signaturebill).on("corebillentryid","nckd_mainentrybill").on("nckd_fhbillno","nckd_srcbillnumber")
                .select(ds.getRowMeta().getFieldNames(),nckd_signaturebill.getRowMeta().getFieldNames()).finish();
        return ds;
    }
}