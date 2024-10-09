package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.List;

/**
 * 客户交易情况表-报表取数插件
 * 表单标识：nckd_customertrade_rpt
 * author:zhangzhilong
 * date:2024/09/02
 */
public class CustomerTradeReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        //默认过滤单据状态不为暂存和已提交的
        QFilter qFilter = new QFilter("billstatus", QCP.not_equals2, "A")
                .and("billstatus", QCP.not_equals2, "B");
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_org_q":
                    if (filterItem.getValue() != null) {
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter.and("saleorgid", QCP.equals, pkValue);
                    }
                    break;
                // 订货渠道
                case "nckd_orderchannelid_q":
                    if (filterItem.getValue() != null) {
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilter.and("orderchannelid", QCP.equals, pkValue);
                    }
                    break;
                // 查询条件订单日期,标识如不一致,请修改
                case "start":
                    if (filterItem.getDate() != null) {
                        qFilter.and("orderdate", QCP.large_equals, DateUtil.beginOfDay(filterItem.getDate()));
                    }
                    break;
                case "end":
                    if (filterItem.getDate() != null) {
                        qFilter.and("orderdate", QCP.less_equals, DateUtil.endOfDay(filterItem.getDate()));
                    }
                    break;
            }
        }
        String sFields =
                //公司
                "saleorgid as nckd_org," +
                        //客户编码
                        "orderchannelid as nckd_orderchannelid," +
                        //省市区
                        "itementry.entryaddressid as nckd_entryaddressid ," +
                        //详细地址
                        "itementry.entrydetailaddress as nckd_entrydetailaddress," +
                        //电话
                        "itementry.entrytelephone as nckd_entrytelephone," +
                        //联系人
                        "itementry.entrycontactname as nckd_entrycontactname," +
                        //数量
                        "itementry.reqbaseqty as nckd_reqbaseqty," +
                        //订单日期
                        "orderdate as nckd_orderdate," +
                        //累计出库基本单位数量-累计退货基本单位数量
                        "itementry.totaloutstockbaseqty - itementry.totalreturnbaseqty as complete," +
                        //金额
                        "sumtaxamount as nckd_sumtaxamount";

        //过滤累计出库基本单位数量-累计退货基本单位数量不大于0的数据
        DataSet orderDS = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "ocbsoc_saleorder", sFields, new QFilter[]{qFilter}, null).filter("complete > 0");

        //汇总数量
        orderDS = orderDS.groupBy(new String[]{"nckd_org", "nckd_orderchannelid", "nckd_entryaddressid", "nckd_entrydetailaddress", "nckd_entrytelephone", "nckd_entrycontactname", "nckd_orderdate","nckd_sumtaxamount"}).sum("nckd_reqbaseqty").finish();

        //根据公司和渠道分组获取最大订单日期
        DataSet maxOrderdate = orderDS.groupBy(new String[]{"nckd_org", "nckd_orderchannelid"}).max("nckd_orderdate", "max_orderdate").finish()
                .select("nckd_org as nckd_org1", "nckd_orderchannelid as nckd_orderchannelid1", "max_orderdate");
        //根据公司和渠道分组获取相关的累计订单数量
        DataSet countOrder = orderDS.groupBy(new String[]{"nckd_org", "nckd_orderchannelid"}).count("nckd_orderdate").finish()
                .select("nckd_org as nckd_org2", "nckd_orderchannelid as nckd_orderchannelid2", "nckd_orderdate as countorder");

        //关联获取最大订单日期
        orderDS = orderDS.leftJoin(maxOrderdate).on("nckd_org", "nckd_org1").on("nckd_orderchannelid", "nckd_orderchannelid1")
                .select(orderDS.getRowMeta().getFieldNames(), new String[]{"max_orderdate"}).finish();

        //关联获取累计订单数量
        orderDS = orderDS.leftJoin(countOrder).on("nckd_org", "nckd_org2").on("nckd_orderchannelid", "nckd_orderchannelid2")
                .select(orderDS.getRowMeta().getFieldNames(), new String[]{"countorder"}).finish();

        return orderDS;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        columns.add(createReportColumn("nckd_entrytelephone", ReportColumn.TYPE_TEXT, "电话"));
        columns.add(createReportColumn("nckd_entrycontactname", ReportColumn.TYPE_TEXT, "联系人"));
        columns.add(createReportColumn("nckd_reqbaseqty", ReportColumn.TYPE_DECIMAL, "数量"));
        columns.add(createReportColumn("nckd_sumtaxamount", ReportColumn.TYPE_DECIMAL, "金额"));
        columns.add(createReportColumn("nckd_pricetax", ReportColumn.TYPE_DECIMAL, "含税单价"));
        columns.add(createReportColumn("notradedays", ReportColumn.TYPE_TEXT, "无交易天数"));
        columns.add(createReportColumn("max_orderdate", ReportColumn.TYPE_DATE, "最后交易日期"));
        columns.add(createReportColumn("countorder", ReportColumn.TYPE_TEXT, "累计订单数"));
        return columns;
    }


    public ReportColumn createReportColumn(String fieldKey, String fieldType, String caption) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fieldKey);
        column.setFieldType(fieldType);
        column.setCaption(new LocaleString(caption));
        if (fieldType.equals(ReportColumn.TYPE_DECIMAL)) {
            column.setScale(2);
            column.setZeroShow(true);
        }

        return column;
    }
}