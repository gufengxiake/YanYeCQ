package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
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
    //判断是否需要根据地区进行客户汇总
    private Boolean isSumAreaCustomer = false;

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {


        DataSet imSalOutBill = this.getImSalOutBill(reportQueryParam);

        if(isSumAreaCustomer){
            return this.getSumAreaCustomer(imSalOutBill);
        }
        return imSalOutBill.orderBy(new String[]{"out_bizorg"});

    }
    //获取销售出库相关数据
    public DataSet getImSalOutBill(ReportQueryParam reportQueryParam){
        List<QFilter> qFilters = new ArrayList<>();
        //限定源头为要货订单的销售出库单
        QFilter mainFilter = new QFilter("billentry.mainbillentity", QCP.equals, "ocbsoc_saleorder");
        //限定为华康及其子公司
        mainFilter.and("bizorg.name", QCP.like, "%华康%");
        //限定单据为已审核
        mainFilter.and("billstatus", QCP.equals, "C");
        qFilters.add(mainFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        //根据选择的查询条件进行组织过滤
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if (nckdOrgQ != null) {
            QFilter orgFilter = new QFilter("bizorg", QCP.equals, nckdOrgQ.getPkValue());
            qFilters.add(orgFilter);
        }
        //获取前台返回的查询条件
        isSumAreaCustomer = filter.getBoolean("nckd_check_q");
        //公司
        String outFields = "bizorg AS out_bizorg," +
                //公司名称
                "bizorg.name AS out_bizorgname," +
                //客户编码
                "customer as out_customerid," +
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
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", outFields, qFilters.toArray(new QFilter[0]), null);
        if (imSaloutbill.isEmpty()) {
            return imSaloutbill;
        }

        //获取销售出库单的核心单据行id
        List<Long> outMainbillentryid = DataSetToList.getOneToList(imSaloutbill, "out_mainbillentryid");
        //电话
        String orderFields = "itementry.entrytelephone as order_entrytelephone," +
                //联系人
                "itementry.entrycontactname as order_entrycontactname," +
                //订货渠道
                "orderchannelid as order_orderchannelid," +
                //表体id
                "itementry.id as order_entryid";
        //查询要货订单
        DataSet ocbsocSaleorder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", orderFields, new QFilter[]{new QFilter("itementry.id", QCP.in, outMainbillentryid.toArray(new Long[0]))}, null);

        //获取渠道主键
        List<Long> orderOrderchannelid = DataSetToList.getOneToList(ocbsocSaleorder, "order_orderchannelid");
        //查询渠道档案 获取 主键，详细地址，省市区字段
        DataSet ocdbdChannel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel", "id,address,area", new QFilter[]{new QFilter("id", QCP.in, orderOrderchannelid.toArray(new Long[0]))}, null);
        //要货订单关联渠道档案
        ocbsocSaleorder = ocbsocSaleorder.leftJoin(ocdbdChannel).on("order_orderchannelid", "id").select(ocbsocSaleorder.getRowMeta().getFieldNames(), ocdbdChannel.getRowMeta().getFieldNames()).finish();

        //关联要货订单
        imSaloutbill = imSaloutbill.leftJoin(ocbsocSaleorder).on("out_mainbillentryid", "order_entryid").select(imSaloutbill.getRowMeta().getFieldNames(), ocbsocSaleorder.getRowMeta().getFieldNames()).finish();

        return imSaloutbill;
    }
    //获取汇总数数据
    public DataSet getSumAreaCustomer(DataSet ds){
        //根据订货渠道的省市区汇总客户数量
        DataSet outCustomerid = ds.filter("area <> null and area<>''");
        outCustomerid = outCustomerid.groupBy(new String[]{"area", "out_customerid"}).finish()
                .groupBy(new String[]{"area"}).count("out_customerid").finish()
                .select("area as sumarea", "out_customerid as sumcustomerid");
        return outCustomerid.orderBy(new String[]{"sumarea"});
    }

    @Override
    public List<ReportExportDataResult> exportWithSheet(ReportQueryParam queryParam, Object selectedObj) {
        DataSet imSalOutBill1 = this.getImSalOutBill(queryParam);
        DataSet imSalOutBill2 = this.getImSalOutBill(queryParam);
        //生成多个sheet页签，并填充DataSet
        List<ReportExportDataResult> list = new ArrayList<>();
        list.add(new ReportExportDataResult("sheetA",this.getSumAreaCustomer(imSalOutBill1)));
        list.add(new ReportExportDataResult("null",null));//第二个list会被吞掉不显示，我们可以先给第二个list项置空处理
        list.add(new ReportExportDataResult("sheetB",imSalOutBill2));
        return list;

    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        if (!isSumAreaCustomer) {
            columns.add(createReportColumn("out_bizorgname", ReportColumn.TYPE_TEXT, "公司名称"));
            columns.add(createReportColumn("out_customer", ReportColumn.TYPE_TEXT, "客户编码"));
            columns.add(createReportColumn("out_customername", ReportColumn.TYPE_TEXT, "客户名称"));
            columns.add(createReportColumn("out_bizdeptname", ReportColumn.TYPE_TEXT, "销售部门"));
            columns.add(createReportColumn("out_bizoperator", ReportColumn.TYPE_TEXT, "业务员"));
            columns.add(createReportColumn("out_warehouse", ReportColumn.TYPE_TEXT, "仓库名称"));
            columns.add(createReportColumn("out_receiveaddress", ReportColumn.TYPE_TEXT, "发货地址"));
            columns.add(createReportColumn("order_entrytelephone", ReportColumn.TYPE_TEXT, "电话"));
            columns.add(createReportColumn("order_entrycontactname", ReportColumn.TYPE_TEXT, "联系人"));

//            columns.add(createReportColumn("xiaobao", ReportColumn.TYPE_TEXT, "小包"));
            columns.add(createReportColumn("address", ReportColumn.TYPE_TEXT, "通信地址"));
            //获取行政区划的长名称
            ReportColumn ocdbdChannel = ReportColumn.createBaseDataColumn("area", "bd_admindivision");
            ocdbdChannel.setCaption(new LocaleString("实际县区"));
            ocdbdChannel.setDisplayProp("fullname");
            columns.add(ocdbdChannel);
        } else {
            ReportColumn ocdbdChannel1 = ReportColumn.createBaseDataColumn("sumarea", "bd_admindivision");
            ocdbdChannel1.setCaption(new LocaleString("实际县区"));
//            ocdbd_channel.setHyperlink(true);
            columns.add(ocdbdChannel1);
            columns.add(createReportColumn("sumcustomerid", ReportColumn.TYPE_TEXT, "盐客户数量"));
//            columns.add(createReportColumn("xiaobao", ReportColumn.TYPE_TEXT, "小包"));
        }

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