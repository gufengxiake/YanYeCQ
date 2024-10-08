package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 盐类产品内部销售对账表-报表取数插件
 * 表单标识：nckd_ylcpnbxsdz_rpt
 * author:zhangzhilong
 * date:2024/08/27
 */
public class YanYeSaleDZReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //默认发货通知单为审核态并且源头是采购订单
        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C").
                and("billentry.mainbillentity", QCP.equals, "pm_purorderbill");
        qFilters.add(qFilter);
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        Long  nckdPurorg = null;
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_saleorgid_q":
                    if(filterItem.getValue() != null){
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("org",QCP.equals,pkValue));
                    }
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "salebizdate_start":
                    if(filterItem.getDate() != null){
                        DateTime salebizdateStart = DateUtil.beginOfDay(filterItem.getDate());
                        qFilters.add(new QFilter("bizdate",QCP.large_equals,salebizdateStart));
                    }
                    break;
                case "salebizdate_end":
                    if(filterItem.getDate() != null){
                        DateTime salebizdateEnd = DateUtil.endOfDay(filterItem.getDate());
                        qFilters.add(new QFilter("bizdate",QCP.less_equals,salebizdateEnd));
                    }
                    break;
                // 查询条件收货组织,标识如不一致,请修改
                case "nckd_purorg_q":
                    nckdPurorg = filterItem.getValue() == null ? null : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
                //查询条件为物料
                case "nckd_material_q":
                    if(filterItem.getValue() != null){
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("billentry.material.masterid",QCP.equals,pkValue));
                    }
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
//                        物料
                        "billentry.material.masterid as nckd_material," +
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
                qFilters.toArray(new QFilter[0]), null);
        delivernotice = this.linkEleWeighing(delivernotice);
        delivernotice = this.linkPurReceiveBill(delivernotice);


//        根据收货组织过滤
        if (nckdPurorg != null) {
            delivernotice = delivernotice.filter("nckd_purorg = " + nckdPurorg);
        }

        return delivernotice.orderBy(delivernotice.getRowMeta().getFieldNames());
    }

    //关联采购收货单
    public DataSet linkPurReceiveBill(DataSet ds) {
        List<Long> mainbillentryidToList = DataSetToList.getMainbillentryidToList(ds);
        if (mainbillentryidToList.isEmpty()) {
            return ds;
        }

        //根据发货单核心单据行id查询来源于采购订单的采购收货单
        QFilter purFilter = new QFilter("billentry.srcbillentryid", QCP.in, mainbillentryidToList.toArray(new Long[0]));
        purFilter.and("billstatus",QCP.equals,"C");
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
        //获取数据源中的发货通知单的分录行id
        List<Long> billentryid = DataSetToList.getOneToList(ds, "billentryid");
        if (billentryid.isEmpty()){
            return ds;
        }


        //根据发货单单据行id查询来源于发货单的电子磅单
        QFilter eleFilter = new QFilter("entryentity.nckd_srcbillentryid", QCP.in, billentryid.toArray(new Long[0]));
        eleFilter.and("billstatus",QCP.equals,"C");
        DataSet nckdEleweighing = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_eleweighing",
                "entryentity.nckd_srcbillentryid as nckd_srcbillentryid," +
                        //分录id
                        "entryentity.id as eleweighingentryid",
                new QFilter[]{eleFilter}, null);

        //获取电子磅单中的分录行id
        List<Long> eleweighingentryid = DataSetToList.getOneToList(nckdEleweighing, "eleweighingentryid");
        QFilter outFilter = new QFilter("billentry.srcbillentryid", QCP.in, eleweighingentryid.toArray(new Long[0]));
        outFilter.and("billstatus",QCP.equals,"C");
        //根据来源行id查询销售出库单
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill",
                "billentry.srcbillentryid as outsrcbillentryid," +
                        //发货数量
                        "billentry.qty as nckd_saleqty,"+
                        //销售出库单编号
                        "billno as nckd_saleoutbillno",
                new QFilter[]{outFilter}, null);
        //电子磅单关联销售出库单
        nckdEleweighing = nckdEleweighing.leftJoin(imSaloutbill).on("eleweighingentryid", "outsrcbillentryid")
                .select(new String[]{"nckd_srcbillentryid", "nckd_saleqty", "nckd_saleoutbillno"}).finish();

        //发货通知单关联电子磅单
        ds = ds.leftJoin(nckdEleweighing).on("billentryid", "nckd_srcbillentryid")
                .select(ds.getRowMeta().getFieldNames(), nckdEleweighing.getRowMeta().getFieldNames()).finish();
        return ds;
    }


}