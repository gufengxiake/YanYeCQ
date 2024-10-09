package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
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
 * 销售出库报表查询-报表取数插件
 * 表单标识：nckd_saleoutreport_rpt
 * author:zhangzhilong
 * date:2024/09/07
 */
public class SaleoutReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        //限定源头为销售订单或是采购订单要货订单并且上游必须为电子磅单的销售出库单
        QFilter mainFilter = new QFilter("billentry.mainbillentity", QCP.in, new String[]{"sm_salorder", "pm_purorderbill","ocbsoc_saleorder"});
        mainFilter.and("billentry.srcbillentity", QCP.equals, "nckd_eleweighing");
        //限定单据为已审核
        mainFilter.and("billstatus", QCP.equals, "C");
        qFilters.add(mainFilter);
        //数据过滤
        FilterInfo filter = reportQueryParam.getFilter();
        //过滤组织
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if(nckdOrgQ != null){
            Long pkValue = (Long) nckdOrgQ.getPkValue();
            qFilters.add(new QFilter("bizorg",QCP.equals,pkValue));
        }
        //过滤出库日期
        if(filter.getDate("outdate_start") != null && filter.getDate("outdate_end") != null){
            DateTime start = DateUtil.beginOfDay(filter.getDate("outdate_start"));
            DateTime end = DateUtil.endOfDay(filter.getDate("outdate_end"));
            qFilters.add(new QFilter("biztime",QCP.large_equals,start).and("biztime",QCP.less_equals,end));
        }
                //公司
        String sFields = "bizorg AS out_bizorg," +
                //公司名称
                "bizorg.name AS out_bizorgname," +
                //仓库名称
                "billentry.warehouse.name as out_warehouse," +
                //出库单号
                "billno as out_billno," +
                //出库日期
                "biztime as out_biztime," +
                //客户
                "customer as out_customer," +
                //客户名称
                "customer.name as out_customername," +
                //开票单位
                "nckd_name1 as out_name1," +
                //收货单位
                "customer.name as out_shdw," +
                //运费结算方式
                "nckd_freighttype as out_nckd_freighttype," +
                //收货地址
                "billentry.receiveaddress as out_receiveaddress," +
                //运输方式
                "nckd_cartype as out_nckd_cartype," +
                //承运人
                "nckd_carcustomer.name as out_carcustomer ," +
                //承运人电话
                "nckd_carcustomer.bizpartner_phone as out_nckd_carcustomer_phone ," +
                //备注
                "billentry.entrycomment as out_entrycomment," +
                //物料编码
                "billentry.material.masterid.number as out_material," +
                //物料名称
                "billentry.material.masterid.name as out_materialname," +
                //规格
                "billentry.material.masterid.modelnum as out_materialmodelnum," +
                //型号
                "billentry.material.masterid.nckd_model as out_materialnckd_model," +
                //实出数量
                "billentry.qty as out_qty," +
                //实收数量
                "billentry.nckd_signqty as out_nckd_signqty," +
                //含税单价
                "billentry.priceandtax as out_priceandtax," +
                //折扣额
                "billentry.discountamount as out_discountamount," +
                //税率
                "billentry.taxrateid.taxrate as out_taxrateid," +
                //税额
                "billentry.taxamount as out_taxamount," +
                //无税金额
                "billentry.amount as out_amount," +
                //价税合计
                "billentry.amountandtax as out_amountandtax," +
                //业务员
                "bizoperator as out_bizoperator," +
                //来源单据行id
                "billentry.srcbillentryid as out_srcbillentryid," +
                //单据行id
                "billentry.id as out_billentryid";
        //查询销售出库单
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]), null);

        if (imSaloutbill.isEmpty()) {
            return imSaloutbill;
        }
        //关联上游单据
        imSaloutbill = this.linkUpBills(imSaloutbill);

        //过滤销售订单号
        String nckdOrderbillnoQ = filter.getString("nckd_orderbillno_q");
        if(!Objects.equals(nckdOrderbillnoQ, "")){
            imSaloutbill = imSaloutbill.filter("order_billno like '%"+nckdOrderbillnoQ+"%'");
        }
        //过滤客户
        DynamicObject nckdCustomerQ = filter.getDynamicObject("nckd_customer_q");
        if(nckdCustomerQ != null){
            imSaloutbill = imSaloutbill.filter("out_customer = "+nckdCustomerQ.getPkValue());
        }

//        im_saloutbill = im_saloutbill.filter("order_billno = 'XSDD-240829-000002'");

        //关联下游单据
        imSaloutbill = this.linkLowBills(imSaloutbill);

        return imSaloutbill.orderBy(new String[]{"out_bizorg","order_bizdate"});
    }

    //关联上游单据单据
    public DataSet linkUpBills(DataSet ds) {
        //获取销售出库来源单据行id
        List<Long> outSrcbillentryid = DataSetToList.getOneToList(ds, "out_srcbillentryid");
        if (outSrcbillentryid.isEmpty()) {
            return ds;
        }
        //根据来源信息查询销售出库上游电子磅单
        DataSet nckdEleweighing = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_eleweighing",
                        //磅单号
                        "nckd_carsysno as ele_carsysno," +
                        //车号
                        "nckd_vehicle.nckd_drivingno as ele_vehicle," +
                        //来源单据行id
                        "entryentity.nckd_srcbillentryid as ele_srcbillentryid," +
                        //单据体id
                        "entryentity.id as ele_entryentityid ",
                new QFilter[]{new QFilter("entryentity.id" , QCP.in,outSrcbillentryid.toArray(new Long[0]))}, null);

        //获取电子磅单来源单据行id
        List<Long> eleSrcbillentryid = DataSetToList.getOneToList(nckdEleweighing,"ele_srcbillentryid");
        //根据电子磅单来源信息查询上游发货通知单
        DataSet smDelivernotice = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sm_delivernotice",
                //发货单号
                "billno as del_billno," +
                        //来源单据行id
                        "billentry.srcbillentryid as del_srcbillentryid," +
                        //单据体id
                        "billentry.id as del_billentryid ",
                new QFilter[]{new QFilter("billentry.id" ,QCP.in,eleSrcbillentryid.toArray(new Long[0]))}, null);
        //电子磅单关联发货申请单
        nckdEleweighing = nckdEleweighing.leftJoin(smDelivernotice).on("ele_srcbillentryid","del_billentryid").select(nckdEleweighing.getRowMeta().getFieldNames(),smDelivernotice.getRowMeta().getFieldNames()).finish();

        //获取发货申请单来源单据行id
        List<Long> delSrcbillentryid = DataSetToList.getOneToList(smDelivernotice, "del_srcbillentryid");
        //根据发货通知单的来源单据行id查询销售订单
        DataSet smSalorder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sm_salorder",
                //订单号
                "billno as order_billno," +
                        //订单日期
                        "bizdate as order_bizdate," +
                        //单据体id
                        "billentry.id as order_billentryid ",
                new QFilter[]{new QFilter("billentry.id" ,QCP.in,delSrcbillentryid.toArray(new Long[0]))}, null);
        //关联销售订单
        nckdEleweighing = nckdEleweighing.leftJoin(smSalorder).on("del_srcbillentryid","order_billentryid").select(nckdEleweighing.getRowMeta().getFieldNames(),smSalorder.getRowMeta().getFieldNames()).finish();

        //销售出库单关联电子磅单
        ds = ds.leftJoin(nckdEleweighing).on("out_srcbillentryid","ele_entryentityid").select(ds.getRowMeta().getFieldNames(),nckdEleweighing.getRowMeta().getFieldNames()).finish();

        nckdEleweighing.close();
        smDelivernotice.close();
        smSalorder.close();
        return ds;
    }

    //关联下游单据单据
    public DataSet linkLowBills(DataSet ds){
        //获取销售出库单表体id
        List<Long> outBillentryid = DataSetToList.getOneToList(ds, "out_billentryid");
        if (outBillentryid.isEmpty()){
            return ds;
        }
        //签收单通过来源销售出库单行id
        DataSet nckdSignaturebill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_signaturebill",
                //车皮号
                "entryentity.nckd_cpno1 as sign_cpno1," +
                        //来源单据体id
                        "entryentity.nckd_sourceentryid as sign_sourceentryid ",
                new QFilter[]{new QFilter("entryentity.nckd_sourceentryid" ,QCP.in,outBillentryid.toArray(new Long[0]))}, null);

        ds = ds.leftJoin(nckdSignaturebill).on("out_billentryid","sign_sourceentryid").select(ds.getRowMeta().getFieldNames(),nckdSignaturebill.getRowMeta().getFieldNames()).finish();


        //财务应收单通过来源销售出库单行id
        DataSet arFinarbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ar_finarbill",
                        //财务应收单表体id
                        "entry.id as fin_entryid," +
                        //来源单据体id
                        "entry.e_srcentryid as fin_srcentryid",
                new QFilter[]{new QFilter("entry.e_srcentryid" ,QCP.in,outBillentryid.toArray(new Long[0]))}, null);

        //获取财务应收单表体id
        List<Long> finentryid = DataSetToList.getOneToList(arFinarbill, "fin_entryid");
        //根据财务应收单找下游开票申请单
        DataSet simOriginalBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sim_original_bill",
                        //已开票数量
                        "sim_original_bill_item.issuednum as sim_issuednum," +
                        //来源单据体id
                        "sim_original_bill_item.srcentryid as sim_srcentryid",
                new QFilter[]{new QFilter("sim_original_bill_item.srcentryid" ,QCP.in,finentryid.toArray(new Long[0]))}, null);

        //财务应收关联开票申请
        arFinarbill = arFinarbill.leftJoin(simOriginalBill).on("fin_entryid","sim_srcentryid").select(arFinarbill.getRowMeta().getFieldNames(),simOriginalBill.getRowMeta().getFieldNames()).finish();

        //销售出库关联财务应收单
        ds = ds.leftJoin(arFinarbill).on("out_billentryid","fin_srcentryid").select(ds.getRowMeta().getFieldNames(),arFinarbill.getRowMeta().getFieldNames()).finish();
        return ds;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("out_bizorgname", ReportColumn.TYPE_TEXT, "公司名称"));
        columns.add(createReportColumn("out_warehouse", ReportColumn.TYPE_TEXT, "仓库名称"));

        columns.add(createReportColumn("order_bizdate", ReportColumn.TYPE_DATE, "业务日期"));
        columns.add(createReportColumn("order_billno", ReportColumn.TYPE_TEXT, "销售订单号"));
        columns.add(createReportColumn("del_billno", ReportColumn.TYPE_TEXT, "发货单号"));


        columns.add(createReportColumn("out_billno", ReportColumn.TYPE_TEXT, "出库单号"));
        columns.add(createReportColumn("out_biztime", ReportColumn.TYPE_DATE, "出库日期"));
        columns.add(createReportColumn("out_customername", ReportColumn.TYPE_TEXT, "客户名称"));
        columns.add(createReportColumn("out_name1", ReportColumn.TYPE_TEXT, "开票单位"));
        columns.add(createReportColumn("out_shdw", ReportColumn.TYPE_TEXT, "收货单位"));
        columns.add(createReportColumn("out_nckd_freighttype", ReportColumn.TYPE_COMBO, "运费结算方式"));
        columns.add(createReportColumn("out_receiveaddress", ReportColumn.TYPE_TEXT, "收货地址"));
        columns.add(createReportColumn("out_nckd_cartype", ReportColumn.TYPE_COMBO, "运输方式"));
        columns.add(createReportColumn("out_carcustomer", ReportColumn.TYPE_TEXT, "承运人"));
        columns.add(createReportColumn("out_nckd_carcustomer_phone", ReportColumn.TYPE_TEXT, "承运人电话"));

        columns.add(createReportColumn("ele_carsysno", ReportColumn.TYPE_TEXT, "磅单号"));
        columns.add(createReportColumn("ele_vehicle", ReportColumn.TYPE_TEXT, "车号"));
        columns.add(createReportColumn("sign_cpno1", ReportColumn.TYPE_TEXT, "车皮号"));


        columns.add(createReportColumn("out_entrycomment", ReportColumn.TYPE_TEXT, "备注"));
        columns.add(createReportColumn("out_material", ReportColumn.TYPE_TEXT, "物料编码"));
        columns.add(createReportColumn("out_materialname", ReportColumn.TYPE_TEXT, "物料名称"));
        columns.add(createReportColumn("out_materialmodelnum", ReportColumn.TYPE_TEXT, "规格"));
        columns.add(createReportColumn("out_materialnckd_model", ReportColumn.TYPE_TEXT, "型号"));

        columns.add(createReportColumn("out_qty", ReportColumn.TYPE_DECIMAL, "实出数量"));
        columns.add(createReportColumn("out_tusun", ReportColumn.TYPE_DECIMAL, "累计途损数量"));
        columns.add(createReportColumn("out_nckd_signqty", ReportColumn.TYPE_DECIMAL, "实收数量"));
        columns.add(createReportColumn("sim_issuednum", ReportColumn.TYPE_DECIMAL, "已开票数量"));
        columns.add(createReportColumn("sim_nosuednum", ReportColumn.TYPE_DECIMAL, "未开票数量"));

        columns.add(createReportColumn("out_priceandtax", ReportColumn.TYPE_DECIMAL, "含税单价"));
        columns.add(createReportColumn("out_discountamount", ReportColumn.TYPE_DECIMAL, "折扣额"));
        columns.add(createReportColumn("out_taxrateid", ReportColumn.TYPE_DECIMAL, "税率"));
        columns.add(createReportColumn("out_taxamount", ReportColumn.TYPE_DECIMAL, "税额"));
        columns.add(createReportColumn("out_amount", ReportColumn.TYPE_DECIMAL, "无税金额"));
        columns.add(createReportColumn("out_amountandtax", ReportColumn.TYPE_DECIMAL, "价税合计"));
//        columns.add(createReportColumn("out_bizoperator", ReportColumn.TYPE_TEXT, "业务员"));
        ReportColumn baseDataColumn = ReportColumn.createBaseDataColumn("out_bizoperator", "bd_operator");
        baseDataColumn.setCaption(new LocaleString("业务员"));
        columns.add(baseDataColumn);
        columns.add(createReportColumn("out_bizoperatorandphone", ReportColumn.TYPE_TEXT, "业务员以及电话"));

        return columns;
    }

    public ReportColumn createReportColumn(String fileKey, String fileType, String name) {
        if (Objects.equals(fileType, ReportColumn.TYPE_COMBO)) {
            ComboReportColumn column = new ComboReportColumn();
            column.setFieldKey(fileKey);
            column.setFieldType(fileType);
            column.setCaption(new LocaleString(name));
            List<ValueMapItem> comboItems = new ArrayList<>();
            if (Objects.equals(fileKey, "out_nckd_cartype")) {
                comboItems.add(new ValueMapItem("", "A", new LocaleString("船运")));
                comboItems.add(new ValueMapItem("", "B", new LocaleString("汽运")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("火车")));
            } else {
                comboItems.add(new ValueMapItem("", "A", new LocaleString("出厂")));
                comboItems.add(new ValueMapItem("", "B", new LocaleString("一票")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("出口CNF")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("出口FOB")));
            }
            column.setComboItems(comboItems);
            return column;
        }
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