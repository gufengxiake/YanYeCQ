package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 盐类产品内部销售对账表-报表取数插件
 * 表单标识：nckd_ylcpnbxsdz_rpt
 * author:zzl
 * date:2024/08/27
 */
public class YanYeSaleDZReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        //默认发货通知单为审核态并且源头是采购订单
        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C").and("billentry.mainbillentity", QCP.equals, "pm_purorderbill");

        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        Long nckd_purorg = null;
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_saleorgid_q":
                    if(!(filterItem.getValue() == null)){
                        Long bizOrg = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter = qFilter.and("org", QCP.equals, bizOrg);
                    }
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "salebizdate_start":
                    if(! (filterItem.getDate() == null) ){
                        qFilter = qFilter.and("bizdate", QCP.large_equals,
                                DateUtil.beginOfDay(filterItem.getDate()));
                    }
                    break;
                case "salebizdate_end":
                    if(! (filterItem.getDate() == null) ){
                        qFilter = qFilter.and("bizdate", QCP.less_equals,
                                DateUtil.endOfDay(filterItem.getDate()));
                    }
                    break;
                // 查询条件收货组织,标识如不一致,请修改
                case "nckd_purorg_q":
                    nckd_purorg =  filterItem.getValue() == null ? null : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
            }
        }

        String sFields =
                //发货组织
                "org as nckd_saleorgid ," +
//                        发货日期
                        "bizdate as nckd_salebizdate ," +
//                        发货单号
                        "billno as nckd_salebillno ," +
//                        车辆号
                        "nckd_vehicle as nckd_vehicle ," +
//                        司机
                        "nckd_driver as nckd_driver," +
//                        单位
                        "billentry.unit as nckd_unit," +
//                        销售单价
                        "billentry.priceandtax as nckd_priceandtax," +
//                        销售金额
                        "billentry.amountandtax as nckd_amountandtax," +
//                        表体id
                        "billentry.id as billentryid," +
//                        核心单据行id
                        "billentry.mainbillentryid as mainbillentryid";
        DataSet delivernotice = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sm_delivernotice", sFields,
                new QFilter[]{qFilter}, null);
        delivernotice = this.linkEleWeighing(delivernotice);
        delivernotice = this.linkPurReceiveBill(delivernotice);
//        根据收货日组织过滤
        if(nckd_purorg != null){
            delivernotice = delivernotice.filter("nckd_purorg = "+ nckd_purorg );
        }

        return delivernotice.orderBy(delivernotice.getRowMeta().getFieldNames());
    }

    //关联采购收货单
    public DataSet linkPurReceiveBill(DataSet ds) {
        DataSet copy = ds.copy();
        List<Long> mainbillentryid = new ArrayList<>();
        while (copy.hasNext()) {
            Row next = copy.next();
            if (next.getLong("mainbillentryid") != null
                    && next.getLong("mainbillentryid")!= 0) {
                mainbillentryid.add(next.getLong("mainbillentryid"));
            }
        }
        if (mainbillentryid.isEmpty())
            return ds;

        //根据发货单核心单据行id查询来源于采购订单的采购发货单
        QFilter purFilter = new QFilter("billentry.srcbillentryid", QCP.in, mainbillentryid.toArray(new Long[0]));
        DataSet purReceiveBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_purreceivebill",
                "billentry.srcbillentryid as srcbillentryid," +//来源行id
//                        收货组织
                        "bizorg as nckd_purorg," +
//                        收货单号
                        "billno as nckd_purbillno," +
//                        收货日期
                        "biztime as nckd_purbizdate," +
//                        收货数量
                        "billentry.qty as nckd_purqty"
                , new QFilter[]{purFilter}, null);




        ds = ds.leftJoin(purReceiveBill).on("mainbillentryid", "srcbillentryid")
                .select(ds.getRowMeta().getFieldNames(), purReceiveBill.getRowMeta().getFieldNames()).finish();
        return ds;

    }

    //关联电子磅单
    public DataSet linkEleWeighing(DataSet ds) {
        DataSet copy = ds.copy();
        List<Long> billentryid = new ArrayList<>();
        while (copy.hasNext()) {
            Row next = copy.next();
            if (next.getLong("billentryid") != null) {
                billentryid.add(next.getLong("billentryid"));
            }
        }
        if (billentryid.isEmpty())
            return ds;

        //根据发货单单据行id查询来源于发货单的电子磅单
        QFilter eleFilter = new QFilter("entryentity.nckd_srcbillentryid", QCP.in, billentryid.toArray(new Long[0]));
        DataSet nckd_eleweighing = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_eleweighing",
                "entryentity.nckd_srcbillentryid as nckd_srcbillentryid," +
                        //                        发货数量
                        "entryentity.nckd_qty as nckd_saleqty",
                new QFilter[]{eleFilter}, null);

        ds = ds.leftJoin(nckd_eleweighing).on("billentryid","nckd_srcbillentryid")
                .select(ds.getRowMeta().getFieldNames(), nckd_eleweighing.getRowMeta().getFieldNames()).finish();
        return ds;
    }


}