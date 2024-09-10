package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.ValueMapItem;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 行政区域销售情况-报表取数插件
 * 表单标识：nckd_salesinarea_rpt
 * author:zhangzhilong
 * date:2024/09/09
 */
public class SalesInAreaReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {
    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        //限定源头为要货订单的销售出库单
        QFilter mainFilter = new QFilter("billentry.mainbillentity", QCP.equals, "ocbsoc_saleorder");
        //限定为华康及其子公司
        mainFilter.and("bizorg.name",QCP.like,"%华康%");
        qFilters.add(mainFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        //根据选择的查询条件进行组织过滤
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if (nckdOrgQ != null) {
            QFilter orgFilter = new QFilter("bizorg", QCP.equals, nckdOrgQ.getPkValue());
            qFilters.add(orgFilter);
        }
        //公司
        String outFields = "bizorg AS out_bizorg," +
                //公司名称
                "bizorg.name AS out_bizorgname," +
                //客户编码
                "customer.number as out_customer," +
                //客户名称
                "customer.name as out_customername," +
                //销售部门
                "bizdept.name as out_bizdeptname," +
                //业务员
                "bizoperator.operatorname as out_bizoperator," +
                //仓库名称
                "billentry.warehouse.name as out_warehouse," +
                //发货地址
                "billentry.receiveaddress as out_receiveaddress," +
                //核心单据行id
                "billentry.mainbillentryid as out_mainbillentryid";
        //查询销售出库单
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", outFields, qFilters.toArray(new QFilter[0]), null);

        //获取销售出库单的核心单据行id
        List<Long> outMainbillentryid = DataSetToList.getOneToList(im_saloutbill, "out_mainbillentryid");
        //电话
        String orderFields = "itementry.entrytelephone as order_entrytelephone," +
                //联系人
                "itementry.entrycontactname as order_entrycontactname," +
                //订货渠道
                "orderchannelid as order_orderchannelid," +
                //表体id
                "itementry.id as order_entryid";
        //查询要货订单
        DataSet ocbsoc_saleorder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", orderFields,new QFilter[]{new QFilter("itementry.id",QCP.in,outMainbillentryid.toArray(new Long[0]))}, null);

        //获取渠道主键
        List<Long> orderOrderchannelid = DataSetToList.getOneToList(ocbsoc_saleorder, "order_orderchannelid");
        //查询渠道档案 获取 主键，详细地址，省市区字段
        DataSet ocdbd_channel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel", "id,address,area",new QFilter[]{new QFilter("id",QCP.in,orderOrderchannelid.toArray(new Long[0]))}, null);
        //要货订单关联渠道档案
        ocbsoc_saleorder = ocbsoc_saleorder.leftJoin(ocdbd_channel).on("order_orderchannelid","id").select(ocbsoc_saleorder.getRowMeta().getFieldNames(),ocdbd_channel.getRowMeta().getFieldNames()).finish();

        //关联要货订单
        im_saloutbill = im_saloutbill.leftJoin(ocbsoc_saleorder).on("out_mainbillentryid","order_entryid").select(im_saloutbill.getRowMeta().getFieldNames(),ocbsoc_saleorder.getRowMeta().getFieldNames()).finish();

        return im_saloutbill.orderBy(new String[]{"out_bizorg"});
    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("out_bizorgname",ReportColumn.TYPE_TEXT,"公司名称"));
        columns.add(createReportColumn("out_customer",ReportColumn.TYPE_TEXT,"客户编码"));
        columns.add(createReportColumn("out_customername",ReportColumn.TYPE_TEXT,"客户名称"));
        columns.add(createReportColumn("out_bizdeptname",ReportColumn.TYPE_TEXT,"销售部门"));
        columns.add(createReportColumn("out_bizoperator",ReportColumn.TYPE_TEXT,"业务员"));
        columns.add(createReportColumn("out_warehouse",ReportColumn.TYPE_TEXT,"仓库名称"));
        columns.add(createReportColumn("out_receiveaddress",ReportColumn.TYPE_TEXT,"发货地址"));
        columns.add(createReportColumn("order_entrytelephone",ReportColumn.TYPE_TEXT,"电话"));
        columns.add(createReportColumn("order_entrycontactname",ReportColumn.TYPE_TEXT,"联系人"));
        columns.add(createReportColumn("xiaobao",ReportColumn.TYPE_TEXT,"小包"));
        columns.add(createReportColumn("address",ReportColumn.TYPE_TEXT,"通信地址"));
        //获取行政区划的长名称
        ReportColumn ocdbd_channel = ReportColumn.createBaseDataColumn("area", "bd_admindivision");
        ocdbd_channel.setCaption(new LocaleString("实际县区"));
        ocdbd_channel.setDisplayProp("fullname");
        columns.add(ocdbd_channel);
        return columns;
    }

    public ReportColumn createReportColumn(String fileKey, String fileType, String name) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fileKey);
        column.setFieldType(fileType);
        column.setCaption(new LocaleString(name));
        if (Objects.equals(fileType, ReportColumn.TYPE_DECIMAL)) {
            column.setScale(2);
        }
        return column;
    }
}