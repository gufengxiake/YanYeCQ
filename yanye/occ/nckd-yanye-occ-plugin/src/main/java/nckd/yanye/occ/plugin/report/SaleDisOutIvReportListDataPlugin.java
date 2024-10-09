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
 * 发货出库开票对应表查询-报表取数插件
 * 表单标识：nckd_saledisoutinv_rpt
 * author:zhangzhilong
 * date:2024/09/06
 */
public class SaleDisOutIvReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        //限定源头为销售订单或是采购订单要货订单并且上游必须为电子磅单的销售出库单
        QFilter mainFilter = new QFilter("billentry.mainbillentity", QCP.in, new String[]{"sm_salorder", "pm_purorderbill","ocbsoc_saleorder"});
        mainFilter.and("billentry.srcbillentity", QCP.equals, "nckd_eleweighing");
        qFilters.add(mainFilter);
        //限定单据为已审核
        qFilters.add(new QFilter("billstatus", QCP.equals, "C"));
        //数据过滤
        FilterInfo filter = reportQueryParam.getFilter();
        //过滤组织
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if(nckdOrgQ != null){
            Long pkValue = (Long) nckdOrgQ.getPkValue();
            qFilters.add(new QFilter("bizorg", QCP.equals, pkValue));
        }
        //公司
        String sFields = "bizorg AS out_bizorg," +
                //公司名称
                "bizorg.name AS out_bizorgname," +
                //出库单号
                "billno as out_billno," +
                //出库日期
                "biztime as out_biztime," +
                //客户
                "customer as out_customer," +
                //客户名称
                "customer.name as out_customername," +
                //收货单位
                "customer.name as out_shdw," +
                //发货仓库
                "billentry.warehouse.name as out_warehouse," +
                //物料编码
                "billentry.material.masterid.number as out_material," +
                //物料名称
                "billentry.material.masterid.name as out_materialname," +
                //规格
                "billentry.material.masterid.modelnum as out_materialmodelnum," +
                //型号
                "billentry.material.masterid.nckd_model as out_materialnckd_model," +
                //出库数量
                "billentry.qty as out_qty," +
                //税率
                "billentry.taxrateid.taxrate as out_taxrateid," +
                //原币含税单价
                "billentry.priceandtax as out_ybpriceandtax," +
                //原币税额
                "billentry.taxamount as out_taxamount," +
                //原币价税合计
                "billentry.amountandtax as out_amountandtax," +
                //汇率
                "exchangerate as out_exchangerate," +
                //本币含税单价
                "billentry.priceandtax as out_bbpriceandtax," +
                //本币税额
                "billentry.curtaxamount as out_curtaxamount," +
                //本币价税合计
                "billentry.curamountandtax as out_curamountandtax," +
                //运输方式
                "nckd_cartype as out_nckd_cartype," +
                //运费结算方式
                "nckd_freighttype as out_nckd_freighttype," +
                //收货地址
                "billentry.receiveaddress as out_receiveaddress," +
                //销售部门
                "bizdept.name as out_bizdept," +
                //业务员
                "bizoperator.operatorname as out_bizoperator," +
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

        //过滤订单日期
        if(filter.getDate("orderdate_start") != null && filter.getDate("orderdate_end") != null){
            DateTime start = DateUtil.beginOfDay(filter.getDate("orderdate_start"));
            DateTime end = DateUtil.endOfDay(filter.getDate("orderdate_end"));
            imSaloutbill = imSaloutbill.filter("order_bizdate >= to_date('" + start + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("order_bizdate <= to_date('" + end + "','yyyy-MM-dd hh:mm:ss')");
        }
        //过滤发货日期
        if(filter.getDate("dispdate_start") != null && filter.getDate("dispdate_end") != null){
            DateTime start = DateUtil.beginOfDay(filter.getDate("dispdate_start"));
            DateTime end = DateUtil.endOfDay(filter.getDate("dispdate_end"));
            imSaloutbill = imSaloutbill.filter("ele_date >= to_date('" + start + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("ele_date <= to_date('" + end + "','yyyy-MM-dd hh:mm:ss')");
        }
        //过滤客户
        DynamicObject nckdCustomerQ = filter.getDynamicObject("nckd_customer_q");
        if(nckdCustomerQ != null){
            imSaloutbill = imSaloutbill.filter("out_customer = "+nckdCustomerQ.getPkValue());
        }

//        im_saloutbill = im_saloutbill.filter("order_billno = 'XSDD-240829-000002'");

        //关联下游单据
        imSaloutbill = this.linkLowBills(imSaloutbill);

        return imSaloutbill.orderBy(new String[]{"out_bizorg","out_biztime"});
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
                //发货日期
                "nckd_date as ele_date," +
                        //磅单号
                        "nckd_carsysno as ele_carsysno," +
                        //车号
                        "nckd_vehicle.nckd_drivingno as ele_vehicle," +
                        //来源单据行id
                        "entryentity.nckd_srcbillentryid as ele_srcbillentryid," +
                        //单据体id
                        "entryentity.id as ele_entryentityid ",
                new QFilter[]{new QFilter("entryentity.id" ,QCP.in,outSrcbillentryid.toArray(new Long[0]))}, null);

        //获取电子磅单来源单据行id
        List<Long> eleSrcbillentryid = DataSetToList.getOneToList(nckdEleweighing,"ele_srcbillentryid");
        //根据电子磅单来源信息查询上游发货通知单
        DataSet smDelivernotice = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sm_delivernotice",
                //发货单号
                "billno as del_billno," +
                        //发货数量
                        "billentry.qty as del_qty," +
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
        QFilter signFilter = new QFilter("entryentity.nckd_sourceentryid", QCP.in, outBillentryid.toArray(new Long[0]));
        //限定单据为已审核
        signFilter.and("billstatus", QCP.equals, "C");
        //签收单通过来源销售出库单行id
        DataSet nckdSignaturebill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_signaturebill",
                //车皮号
                "entryentity.nckd_cpno1 as sign_cpno1," +
                        //报关单号
                        "nckd_customsno as sign_customsno," +
                        //来源单据体id
                        "entryentity.nckd_sourceentryid as sign_sourceentryid ",
                new QFilter[]{signFilter}, null);

        ds = ds.leftJoin(nckdSignaturebill).on("out_billentryid","sign_sourceentryid").select(ds.getRowMeta().getFieldNames(),nckdSignaturebill.getRowMeta().getFieldNames()).finish();


        QFilter arFilter = new QFilter("entry.e_srcentryid", QCP.in, outBillentryid.toArray(new Long[0]));
        //限定单据为已审核
        arFilter.and("billstatus", QCP.equals, "C");
        //财务应收单通过来源销售出库单行id
        DataSet arFinarbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ar_finarbill",
                //应收单编号
                "billno as fin_billno," +
                        //年份
                        "bizdate as fin_year," +
                        //应收单凭证编号
//                        "" +
                        //财务应收单表体id
                        "entry.id as fin_entryid," +
                        //来源单据体id
                        "entry.e_srcentryid as fin_srcentryid",
                new QFilter[]{arFilter}, null);

        //获取财务应收单表体id
        List<Long> finentryid = DataSetToList.getOneToList(arFinarbill, "fin_entryid");
        //根据财务应收单找下游开票申请单
        DataSet simOriginalBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sim_original_bill",
                //发票号
                "invoiceno as sim_invoiceno," +
                        //发票日期
                        "issuetime as sim_billdate," +
                        //凭证号
                        "deductions.evidenceno as sim_evidenceno," +
                        //开票单位
                        "buyername as sim_buyername," +
                        //开票数量
                        "sim_original_bill_item.issuednum as sim_num," +
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
        columns.add(createReportColumn("out_bizorgname", ReportColumn.TYPE_TEXT, "公司"));
        columns.add(createReportColumn("order_billno", ReportColumn.TYPE_TEXT, "订单号"));
        columns.add(createReportColumn("order_bizdate", ReportColumn.TYPE_DATE, "订单日期"));
        columns.add(createReportColumn("del_billno", ReportColumn.TYPE_TEXT, "发货单号"));
        columns.add(createReportColumn("ele_date", ReportColumn.TYPE_DATE, "发货日期"));

        columns.add(createReportColumn("out_billno", ReportColumn.TYPE_TEXT, "出库单号"));
        columns.add(createReportColumn("out_biztime", ReportColumn.TYPE_DATE, "出库日期"));
        columns.add(createReportColumn("sim_invoiceno", ReportColumn.TYPE_TEXT, "发票号"));
        columns.add(createReportColumn("sim_billdate", ReportColumn.TYPE_DATE, "发票日期"));

        columns.add(createReportColumn("out_customername", ReportColumn.TYPE_TEXT, "客户"));
        columns.add(createReportColumn("out_shdw", ReportColumn.TYPE_TEXT, "收货单位"));
        columns.add(createReportColumn("sim_buyername", ReportColumn.TYPE_TEXT, "开票单位"));

        columns.add(createReportColumn("out_warehouse", ReportColumn.TYPE_TEXT, "发货仓库"));

        columns.add(createReportColumn("out_material", ReportColumn.TYPE_TEXT, "物料编码"));
        columns.add(createReportColumn("out_materialname", ReportColumn.TYPE_TEXT, "物料名称"));
        columns.add(createReportColumn("out_materialmodelnum", ReportColumn.TYPE_TEXT, "规格"));
        columns.add(createReportColumn("out_materialnckd_model", ReportColumn.TYPE_TEXT, "型号"));

        columns.add(createReportColumn("del_qty", ReportColumn.TYPE_DECIMAL, "发货数量"));
        columns.add(createReportColumn("out_qty", ReportColumn.TYPE_DECIMAL, "出库数量"));
        columns.add(createReportColumn("sim_num", ReportColumn.TYPE_DECIMAL, "开票数量"));

        columns.add(createReportColumn("out_taxrateid", ReportColumn.TYPE_DECIMAL, "税率"));
        columns.add(createReportColumn("out_ybpriceandtax", ReportColumn.TYPE_DECIMAL, "原币含税单价"));
        columns.add(createReportColumn("out_taxamount", ReportColumn.TYPE_DECIMAL, "原币税额"));
        columns.add(createReportColumn("out_amountandtax", ReportColumn.TYPE_DECIMAL, "原币价税合计"));
        columns.add(createReportColumn("out_exchangerate", ReportColumn.TYPE_DECIMAL, "汇率"));
        columns.add(createReportColumn("out_bbpriceandtax", ReportColumn.TYPE_DECIMAL, "本币含税单价"));
        columns.add(createReportColumn("out_curtaxamount", ReportColumn.TYPE_DECIMAL, "本币税额"));
        columns.add(createReportColumn("out_curamountandtax", ReportColumn.TYPE_DECIMAL, "本币价税合计"));

        columns.add(createReportColumn("ele_carsysno", ReportColumn.TYPE_TEXT, "磅单号"));
        columns.add(createReportColumn("ele_vehicle", ReportColumn.TYPE_TEXT, "车号"));
        columns.add(createReportColumn("sign_cpno1", ReportColumn.TYPE_TEXT, "车皮号"));

        columns.add(createReportColumn("out_nckd_cartype", ReportColumn.TYPE_COMBO, "运输方式"));
        columns.add(createReportColumn("out_nckd_freighttype", ReportColumn.TYPE_COMBO, "运费结算方式"));
        columns.add(createReportColumn("out_receiveaddress", ReportColumn.TYPE_TEXT, "收货地址"));
        columns.add(createReportColumn("out_bizdept", ReportColumn.TYPE_TEXT, "销售部门"));
        columns.add(createReportColumn("out_bizoperator", ReportColumn.TYPE_TEXT, "业务员"));

        columns.add(createReportColumn("fin_billno", ReportColumn.TYPE_TEXT, "应收单编号"));
        columns.add(createReportColumn("fin_year", ReportColumn.TYPE_TEXT, "会计年度"));
        columns.add(createReportColumn("fin_month", ReportColumn.TYPE_TEXT, "会计期间"));
        columns.add(createReportColumn("sign_customsno", ReportColumn.TYPE_TEXT, "报关单号"));
        columns.add(createReportColumn("sim_evidenceno", ReportColumn.TYPE_TEXT, "应收单凭证编号"));

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