package nckd.yanye.scm.report.inoutrecords;

import java.util.stream.Collectors;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

/**
 * @author husheng
 * @date 2024-08-29 9:37
 * @description 出入库流水账（nckd_inoutrecords）报表查询插件
 */
public class InoutrecordsPlugin extends AbstractReportListDataPlugin {
    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        // 查询条件
        FilterInfo filter = reportQueryParam.getFilter();

        // 采购入库单
        DataSet imPurinbillDataSet = this.getImPurinbillDataSet(filter);
        // 其他入库单
        DataSet imOtherinbillDataSet = this.getImOtherinbillDataSet(filter);
        // 生产入库单
        DataSet imProductinbillDataSet = this.getimProductinbillDataSet(filter);
        // 完工入库单
        DataSet imMdcMftmanuinDataSet = this.getImMdcMftmanuinDataSet(filter);
        // 完工退库单
        DataSet imMdcMftreturnbillDataSet = this.getImMdcMftreturnbillDataSet(filter);
        // 领料出库单
        DataSet imMaterialreqouDataSet = this.getImMaterialreqoutbillDataSet(filter);
        // 其他出库单
        DataSet imOtheroutbilllDataSet = this.getImOtheroutbilllDataSet(filter);
        // 销售出库单
        DataSet imSaloutbillDataSet = this.getImSaloutbillDataSet(filter);
        // 生产领料单
        DataSet imMdcMftproorderDataSet = this.getImMdcMftproorder(filter);
        // 生产退料单
        DataSet imMdcMftreturnorder = this.getImMdcMftreturnorder(filter);
        // 生产补料单
        DataSet imMdcMftfeedorder = this.getImMdcMftfeedorder(filter);

        DataSet dataSet = imPurinbillDataSet.union(imOtherinbillDataSet, imProductinbillDataSet, imMdcMftmanuinDataSet,
                imMdcMftreturnbillDataSet, imMaterialreqouDataSet, imOtheroutbilllDataSet, imSaloutbillDataSet,
                imMdcMftproorderDataSet, imMdcMftreturnorder, imMdcMftfeedorder)
                .orderBy(new String[]{"nckd_bookdate desc", "nckd_billno"});

        return dataSet;
    }

    /**
     * 采购入库单
     *
     * @param filter
     * @return
     */
    private DataSet getImPurinbillDataSet(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_purinbill", "A");

        // =====================二级=========================
        // 采购入库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "2");
        String imPurinbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,supplier.name nckd_supplier_name,supplier.number nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "billentry.baseqty nckd_baseqty,billentry.unit2nd nckd_unit2nd,billentry.qtyunit2nd nckd_qtyunit2nd,null nckd_unit3nd,null nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imPurinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_purinbill", imPurinbillSql, qFilter2.toArray(), null);

        // 采购入库单关联物料
        DataSet finish3 = this.relevancyMaterial(imPurinbill, filter);

        // 核算成本和采购入库关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        DataSet finish4Copy1 = finish4.copy();
        DataSet finish4Copy2 = finish4.copy();

        // 采购订单
        String pmPurorderbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.qty nckd_qty,billentry.payablepriceqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,supplier.simplename nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet pmPurorderbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "pm_purorderbill", pmPurorderbillSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish5 = this.secondUnionThree(finish4Copy1, pmPurorderbill);

        // 采购收货单
        String imPurreceivebillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.qty nckd_qty,billentry.joinpriceqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,supplier.simplename nckd_suppliermnemoniccode, billentry.id billentry_id";
        DataSet imPurreceivebill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_purreceivebill", imPurreceivebillSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish6 = this.secondUnionThree(finish4Copy2, imPurreceivebill);

        // 采购入库单
        String purorderbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.qty nckd_qty,billentry.joinpriceqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,supplier.simplename nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet purinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_purinbill", purorderbillSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish7 = this.secondUnionThree(finish4, purinbill);

        DataSet dataSet = finish5.union(finish6, finish7).distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }

        return dataSet;
    }

    /**
     * 其他入库单
     *
     * @param filter
     * @return
     */
    private DataSet getImOtherinbillDataSet(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_otherinbill", "A");

        // =====================二级=========================
        // 其他入库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "2");
        String imOtherinbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,supplier.name nckd_supplier_name,supplier.number nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "billentry.baseqty nckd_baseqty,billentry.unit2nd nckd_unit2nd,billentry.qtyunit2nd nckd_qtyunit2nd,null nckd_unit3nd,null nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imOtherinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_otherinbill", imOtherinbillSql, qFilter2.toArray(), null);

        // 其他入库单关联物料
        DataSet finish3 = this.relevancyMaterial(imOtherinbill, filter);

        // 核算成本和其他入库关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        DataSet finish4Copy1 = finish4.copy();

        // 销售出库单
        String imSaloutbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "nckd_carcontract nckd_shippingordernumber,nckd_freighttype nckd_cass,billentry.receiveaddress nckd_shipping_address,billentry.mainbillnumber nckd_order_serial_number,billentry.qty qty, billentry.nckd_signqty nckd_signqty,null nckd_damaged_quantity," +
                "nckd_cartype nckd_direct_mode,nckd_carcustomer.name nckd_carrier,nckd_carcustomer.bizpartner_phone nckd_carrier_phone,nckd_driver.nckd_phonenumber nckd_phone,nckd_vehicle.name nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.joinpriceqty nckd_qty,billentry.qty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,customer.simplename nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", imSaloutbillSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        // 签收单
        DataSet nckdSignaturebill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "nckd_signaturebill", "nckd_sale,entryentity.nckd_cpno1 nckd_wagon_number,entryentity.nckd_sourceentryid nckd_sourceentryid", null, null);

        // 销售出库单签收单关联
        DataSet finish5 = imSaloutbill.leftJoin(nckdSignaturebill).on("nckd_sourcebillno", "nckd_sale").on("billentry_id", "nckd_sourceentryid")
                .select(new String[]{"nckd_sourcebilltypename", "nckd_sourcebillno", "nckd_nummer_reisdocument", "nckd_xm_name",
                                "nckd_shippingordernumber", "nckd_cass", "nckd_shipping_address", "nckd_order_serial_number", "( CASE WHEN qty - nckd_signqty <= 0 THEN 0 ELSE qty - nckd_signqty END ) nckd_loss_quantity", "nckd_damaged_quantity",
                                "nckd_direct_mode", "nckd_carrier", "nckd_carrier_phone", "nckd_phone", "nckd_carnumber",
                                "nckd_comment", "materialmasterid", "nckd_qty", "nckd_payablepriceqty", "nckd_entrycomment",
                                "nckd_creator_name", "nckd_auditor_name", "nckd_h_source", "nckd_customermnemoniccode", "nckd_suppliermnemoniccode", "billentry_id"},
                        new String[]{"nckd_wagon_number"}).finish();

        DataSet finish6 = this.secondUnionThree(finish4Copy1, finish5);

        // 盘点表
        String imInvcountbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.gainqty nckd_qty,billentry.lossqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imInvcountbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_invcountbill", imInvcountbillSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish7 = this.secondUnionThree(finish4, imInvcountbill);

        DataSet dataSet = finish6.union(finish7).distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }

        return dataSet;
    }

    /**
     * 生产入库单
     *
     * @param filter
     * @return
     */
    private DataSet getimProductinbillDataSet(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_productinbill", "A");

        // =====================二级=========================
        // 生产入库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "3");
        String imProductinbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,null nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "billentry.baseqty nckd_baseqty,billentry.unit2nd nckd_unit2nd,billentry.qtyunit2nd nckd_qtyunit2nd,null nckd_unit3nd,null nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imProductinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_productinbill", imProductinbillSql, qFilter2.toArray(), null);

        // 生产入库单关联物料
        DataSet finish3 = this.relevancyMaterial(imProductinbill, filter);

        // 核算成本和生产入库单关联
        DataSet finish4 = finish1.join(finish3).on("billnumber", "nckd_billno").on("nckd_masterid", "materialmasterid").on("bizbillentryid", "billentry_id")
                .select(new String[]{"nckd_org", "nckd_costaccount", "nckd_masterid", "nckd_in_amount", "nckd_out_amount"},
                        new String[]{"nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "nckd_modelnum", "nckd_model",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "nckd_group", "srcbillentity"}).finish();

        finish4 = finish4.addNullField("nckd_sourcebilltypename", "nckd_sourcebillno", "nckd_nummer_reisdocument", "nckd_xm_name",
                "nckd_shippingordernumber", "nckd_cass", "nckd_shipping_address", "nckd_order_serial_number", "nckd_loss_quantity", "nckd_damaged_quantity",
                "nckd_wagon_number", "nckd_direct_mode", "nckd_carrier", "nckd_carrier_phone", "nckd_phone", "nckd_carnumber",
                "nckd_comment", "nckd_qty", "nckd_payablepriceqty", "nckd_entrycomment",
                "nckd_creator_name", "nckd_auditor_name", "nckd_h_source", "nckd_customermnemoniccode", "nckd_suppliermnemoniccode");

        DataSet dataSet = finish4.distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 完工入库单
     *
     * @param filter
     * @return
     */
    private DataSet getImMdcMftmanuinDataSet(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_mdc_mftmanuinbill", "A");

        // =====================二级=========================
        // 完工入库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "3");
        String imPurinbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,null nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "billentry.baseqty nckd_baseqty,billentry.unit2nd nckd_unit2nd,billentry.qtyunit2nd nckd_qtyunit2nd,null nckd_unit3nd,null nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imPurinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftintpl", imPurinbillSql, qFilter2.toArray(), null);

        // 完工入库单关联物料
        DataSet finish3 = this.relevancyMaterial(imPurinbill, filter);

        // 核算成本和完工入库单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        // 生产工单
        String pomMftorderSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,treeentryentity.bdproject.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "remark nckd_comment,treeentryentity.qty nckd_qty,null nckd_payablepriceqty,null nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,treeentryentity.id billentry_id";
        DataSet pomMftorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "pom_mftorder", pomMftorderSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish5 = this.secondUnionThree(finish4, pomMftorder);

        DataSet dataSet = finish5.distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 完工退库单
     *
     * @param filter
     * @return
     */
    private DataSet getImMdcMftreturnbillDataSet(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_mdc_mftreturnbill", "A");

        // =====================二级=========================
        // 完工退库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "3");
        String imMdcMftreturnbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,null nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "billentry.baseqty nckd_baseqty,billentry.unit2nd nckd_unit2nd,billentry.qtyunit2nd nckd_qtyunit2nd,null nckd_unit3nd,null nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imMdcMftreturnbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftreturnbill", imMdcMftreturnbillSql, qFilter2.toArray(), null);

        // 完工退库单关联物料
        DataSet finish3 = this.relevancyMaterial(imMdcMftreturnbill, filter);

        // 核算成本和完工退库单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        // 完工入库单
        String imMdcMftintplSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.qty nckd_qty,null nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imMdcMftintpl = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftintpl", imMdcMftintplSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish5 = this.secondUnionThree(finish4, imMdcMftintpl);

        DataSet dataSet = finish5.distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }


    /**
     * 领料出库单
     *
     * @param filter
     * @return
     */
    private DataSet getImMaterialreqoutbillDataSet(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_materialreqoutbill", "B");

        // =====================二级=========================
        // 领料出库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "3");
        String imMaterialreqoutbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,billentry.isrework nckd_rework,billentry.unit nckd_unit," +
                "null nckd_baseqty,billentry.unit2nd nckd_unit2nd,null nckd_qtyunit2nd,billentry.baseqty nckd_unit3nd,billentry.qtyunit2nd nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imMaterialreqoutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_materialreqoutbill", imMaterialreqoutbillSql, qFilter2.toArray(), null);

        // 领料出库单关联物料
        DataSet finish3 = this.relevancyMaterial(imMaterialreqoutbill, filter);

        // 核算成本和领料出库单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        DataSet finish4Copy1 = finish4.copy();
        DataSet finish4Copy2 = finish4.copy();

        // 领料申请单
        String imMaterialreqbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,null nckd_qty,billentry.qty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imMaterialreqbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_materialreqbill", imMaterialreqbillSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish5 = this.secondUnionThree(finish4Copy1, imMaterialreqbill);

        // 领料出库单
        String imMaterialreqouSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,null nckd_qty,billentry.qty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imMaterialreqou = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_materialreqoutbill", imMaterialreqouSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish6 = this.secondUnionThree(finish4Copy2, imMaterialreqou);

        // 发货通知单
        String smDelivernoticeSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,null nckd_qty,billentry.qty nckd_payablepriceqty,billentry.remark nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,customer.name nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet smDelivernotice = QueryServiceHelper.queryDataSet(this.getClass().getName(), "sm_delivernotice", smDelivernoticeSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish7 = this.secondUnionThree(finish4, smDelivernotice);

        DataSet dataSet = finish5.union(finish6, finish7).distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 其他出库单
     *
     * @param filter
     * @return
     */
    private DataSet getImOtheroutbilllDataSet(FilterInfo filter) {
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_otheroutbill", "B");

        // =====================二级=========================
        // 其他出库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "1");
        String imOtheroutbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,customer.number nckd_bp_number,customer.name nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,null nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "null nckd_baseqty,billentry.unit2nd nckd_unit2nd,null nckd_qtyunit2nd,billentry.baseqty nckd_unit3nd,billentry.qtyunit2nd nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imOtheroutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_otheroutbill", imOtheroutbillSql, qFilter2.toArray(), null);

        // 其他出库单关联物料
        DataSet finish3 = this.relevancyMaterial(imOtheroutbill, filter);

        // 核算成本和其他出库单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        // 上游单据是盘点表或手动新增
        String imInvcountbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.gainqty nckd_qty,billentry.lossqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imInvcountbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_invcountbill", imInvcountbillSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish5 = this.secondUnionThree(finish4, imInvcountbill);

        DataSet dataSet = finish5.distinct();

        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 销售出库单
     *
     * @param filter
     * @return
     */
    private DataSet getImSaloutbillDataSet(FilterInfo filter) {
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_saloutbill", "B");

        // =====================二级=========================
        // 销售出库单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "1");
        String imSaloutbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,bizoperator.operatorname nckd_salesman,customer.number nckd_bp_number,customer.name nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "null nckd_baseqty,billentry.unit2nd nckd_unit2nd,null nckd_qtyunit2nd,billentry.baseqty nckd_unit3nd,billentry.qtyunit2nd nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", imSaloutbillSql, qFilter2.toArray(), null);

        // 销售出库单关联物料
        DataSet finish3 = this.relevancyMaterial(imSaloutbill, filter);

        // 核算成本和销售出库单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        // 销售订单
        String smSalorderSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,null nckd_qty,billentry.qty nckd_payablepriceqty,billentry.remark nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,customer.name nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet smSalorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "sm_salorder", smSalorderSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish5 = this.secondUnionThree(finish4, smSalorder);

        DataSet dataSet = finish5.distinct();

        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 生产领料单
     *
     * @param filter
     * @return
     */
    private DataSet getImMdcMftproorder(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_mdc_mftproorder", "B");

        // =====================二级=========================
        // 生产领料单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "3");
        // 单据实体
        qFilter2.and(new QFilter("billentity", QCP.equals, "im_mdc_mftproorder"));
        String imMdcMftproorderSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "null nckd_baseqty,billentry.unit2nd nckd_unit2nd,null nckd_qtyunit2nd,billentry.baseqty nckd_unit3nd,billentry.qtyunit2nd nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imMdcMftproorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftproorder", imMdcMftproorderSql, qFilter2.toArray(), null);

        // 生产领料单关联物料
        DataSet finish3 = this.relevancyMaterial(imMdcMftproorder, filter);

        // 核算成本和生产领料单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        // 生产组件清单
        String pomMftstockSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,bdproject.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "remarks nckd_comment,stockentry.materielmasterid materialmasterid,null nckd_qty,stockentry.standqty nckd_payablepriceqty,stockentry.childremarks nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,nckd_customer.name nckd_customermnemoniccode,null nckd_suppliermnemoniccode,stockentry.id billentry_id";
        DataSet pomMftstock = QueryServiceHelper.queryDataSet(this.getClass().getName(), "pom_mftstock", pomMftstockSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C")}, null);

        DataSet finish5 = this.secondUnionThree(finish4, pomMftstock);

        DataSet dataSet = finish5.distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 生产退料单
     *
     * @param filter
     * @return
     */
    private DataSet getImMdcMftreturnorder(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_mdc_mftreturnorder", "B");

        // =====================二级=========================
        // 生产退料单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "3");
        // 单据实体
        qFilter2.and(new QFilter("billentity", QCP.equals, "im_mdc_mftreturnorder"));
        String imMdcMftreturnorderSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,billentry.isrework nckd_rework,billentry.unit nckd_unit," +
                "null nckd_baseqty,billentry.unit2nd nckd_unit2nd,null nckd_qtyunit2nd,billentry.baseqty nckd_unit3nd,billentry.qtyunit2nd nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imMdcMftreturnorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftproorder", imMdcMftreturnorderSql, qFilter2.toArray(), null);

        // 生产退料单关联物料
        DataSet finish3 = this.relevancyMaterial(imMdcMftreturnorder, filter);

        // 核算成本和生产退料单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        // 生产领料单/生产补料单
        String imMdcMftproorderSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,null nckd_qty,billentry.baseqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imMdcMftproorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftproorder", imMdcMftproorderSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C").and("billentity", QCP.in, new String[]{"im_mdc_mftproorder", "im_mdc_mftfeedorder"})}, null);

        DataSet finish5 = this.secondUnionThree(finish4, imMdcMftproorder);

        DataSet dataSet = finish5.distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 生产补料单
     *
     * @param filter
     * @return
     */
    private DataSet getImMdcMftfeedorder(FilterInfo filter) {
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        // ======================一级========================
        // 核算成本记录
        DataSet finish1 = this.getCalCcostrecord(filter, "im_mdc_mftfeedorder", "B");

        // =====================二级=========================
        // 生产补料单
        QFilter qFilter2 = this.buildImPurinbillFilter(filter, "3");
        // 单据实体
        qFilter2.and(new QFilter("billentity", QCP.equals, "im_mdc_mftfeedorder"));
        String imMdcMftfeedorderSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,null nckd_supplier_name,null nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,billentry.isrework nckd_rework,billentry.unit nckd_unit," +
                "null nckd_baseqty,billentry.unit2nd nckd_unit2nd,null nckd_qtyunit2nd,billentry.baseqty nckd_unit3nd,billentry.qtyunit2nd nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillentryid srcbillentryid,billentry.srcbillnumber srcbillnumber,id,billentry.id billentry_id";
        DataSet imMdcMftfeedorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftproorder", imMdcMftfeedorderSql, qFilter2.toArray(), null);

        // 生产补料单关联物料
        DataSet finish3 = this.relevancyMaterial(imMdcMftfeedorder, filter);

        // 核算成本和生产补料单关联
        DataSet finish4 = this.firstUnionSecond(finish1, finish3);

        // ==================三级=========================
        // 生产退料单
        String imMdcMftreturnorderSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,billentry.project.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,null nckd_qty,billentry.baseqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,null nckd_suppliermnemoniccode,billentry.id billentry_id";
        DataSet imMdcMftreturnorder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_mdc_mftproorder", imMdcMftreturnorderSql, new QFilter[]{new QFilter("billstatus", QCP.equals, "C").and("billentity", QCP.equals, "im_mdc_mftreturnorder")}, null);

        DataSet finish5 = this.secondUnionThree(finish4, imMdcMftreturnorder);

        DataSet dataSet = finish5.distinct();

        // 客户
        if (mulcustomer.getValue() != null) {
            dataSet = dataSet.where("nckd_bp_name is not null");
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            dataSet = dataSet.where("nckd_supplier_name is not null");
        }

        return dataSet;
    }

    /**
     * 二级关联三级
     *
     * @param secondDataSet
     * @param ThreeDataSet
     * @return
     */
    private DataSet secondUnionThree(DataSet secondDataSet, DataSet ThreeDataSet) {
        DataSet finish = secondDataSet.join(ThreeDataSet).on("srcbillnumber", "nckd_sourcebillno").on("srcbillentryid", "billentry_id")
                .select(new String[]{"nckd_org", "nckd_costaccount", "nckd_masterid", "nckd_in_amount", "nckd_out_amount",
                                "nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "nckd_modelnum", "nckd_model",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "nckd_group", "srcbillentity"},
                        new String[]{"nckd_sourcebilltypename", "nckd_sourcebillno", "nckd_nummer_reisdocument", "nckd_xm_name",
                                "nckd_shippingordernumber", "nckd_cass", "nckd_shipping_address", "nckd_order_serial_number", "nckd_loss_quantity", "nckd_damaged_quantity",
                                "nckd_wagon_number", "nckd_direct_mode", "nckd_carrier", "nckd_carrier_phone", "nckd_phone", "nckd_carnumber",
                                "nckd_comment", "nckd_qty", "nckd_payablepriceqty", "nckd_entrycomment",
                                "nckd_creator_name", "nckd_auditor_name", "nckd_h_source", "nckd_customermnemoniccode", "nckd_suppliermnemoniccode"}).finish();
        return finish;
    }

    /**
     * 一级关联二级
     *
     * @param firstDataSet
     * @param secondDataSet
     * @return
     */
    private DataSet firstUnionSecond(DataSet firstDataSet, DataSet secondDataSet) {
        DataSet finish = firstDataSet.join(secondDataSet).on("billnumber", "nckd_billno").on("nckd_masterid", "materialmasterid").on("bizbillentryid", "billentry_id")
                .select(new String[]{"nckd_org", "nckd_costaccount", "nckd_masterid", "nckd_in_amount", "nckd_out_amount"},
                        new String[]{"nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "nckd_modelnum", "nckd_model",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "srcbillentity", "srcbillentryid", "srcbillnumber",
                                "nckd_group"}).finish();
        return finish;
    }

    /**
     * 获取核算成本记录
     *
     * @param filter 查询条件
     * @param formId 业务对象
     * @param type   类型  A 入库  B 出库
     * @return
     */
    private DataSet getCalCcostrecord(FilterInfo filter, String formId, String type) {
        QFilter qFilter = this.buildCalCostrecordFilter(filter, formId);
        // 核算成本记录
        DataSet calCostrecord = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cal_costrecord", "bookdate,billnumber,calorg nckd_org,costaccount nckd_costaccount,entry.material nckd_masterid,entry.actualcost actualcost," +
                "id,entry.id entry_id,entry.bizbillentryid bizbillentryid", qFilter.toArray(), null);
        // 获取已审核的成本调整单
        DataSet calCostadjustbill = this.getCalCostadjustbill(type);

        // 核算成本记录与成本调整单关联
        DataSet finish = calCostrecord.leftJoin(calCostadjustbill).on("nckd_masterid", "masterid").on("id", "invbillid").on("entry_id", "invbillentryid")
                .select(new String[]{"billnumber", "nckd_org", "nckd_costaccount", "nckd_masterid", "actualcost", "bizbillentryid"},
                        new String[]{"( CASE WHEN adjustamt IS NULL THEN 0 ELSE adjustamt END )adjustamount"}).finish();

        if ("A".equals(type)) {
            finish = finish.select("billnumber", "nckd_org", "nckd_costaccount", "nckd_masterid", "(actualcost + adjustamount) nckd_in_amount", "null nckd_out_amount", "bizbillentryid");
        } else if ("B".equals(type)) {
            finish = finish.select("billnumber", "nckd_org", "nckd_costaccount", "nckd_masterid", "null nckd_in_amount", "(actualcost + adjustamount) nckd_out_amount", "bizbillentryid");
        }

        return finish;
    }

    /**
     * 关联物料
     *
     * @param dataSet 要关联的数据
     * @param filter  查询条件
     * @return
     */
    private DataSet relevancyMaterial(DataSet dataSet, FilterInfo filter) {
        // 物料分类标准页签
        QFilter qFilter3 = this.buildMaterialgroupFilter(filter);
        DataSet bdMaterialgroupdetail = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_materialgroupdetail", "group nckd_group,material,createorg", qFilter3.toArray(), null);

        // 物料
        QFilter qFilter4 = this.buildMaterialFilter(filter);
        DataSet bdMaterial = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_material", "masterid,modelnum nckd_modelnum,nckd_model,createorg", qFilter4.toArray(), null);

        DataSet finish2 = bdMaterial.join(bdMaterialgroupdetail).on("masterid", "material")//.on("createorg", "createorg")
                .select(
                        new String[]{"masterid", "nckd_modelnum", "nckd_model"},
                        new String[]{"nckd_group"}).finish();

        DataSet finish3 = dataSet.join(finish2).on("materialmasterid", "masterid")
                .select(new String[]{"nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "materialmasterid",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "srcbillentity", "srcbillentryid", "srcbillnumber", "id", "billentry_id"},
                        new String[]{"nckd_modelnum", "nckd_model", "nckd_group"}).finish();

        return finish3;
    }

    /**
     * 获取成本调整单
     *
     * @param biztype 核算单类型 A 入库 B 出库
     * @return
     */
    private DataSet getCalCostadjustbill(String biztype) {
        QFilter qFilter1 = new QFilter("biztype", QCP.equals, biztype)
                .and(new QFilter("billstatus", QCP.equals, "C"));
        DataSet calCostadjustbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cal_costadjustbill", "entryentity.material masterid,entryentity.adjustamt adjustamt,entryentity.invbillid invbillid,entryentity.invbillentryid invbillentryid", qFilter1.toArray(), null)
                .groupBy(new String[]{"masterid", "invbillid", "invbillentryid"}).sum("adjustamt").finish();
        return calCostadjustbill;
    }

    /**
     * 构建核算成本记录的过滤条件
     *
     * @param filter 查询条件
     * @param key    业务对象
     * @return
     */
    private QFilter buildCalCostrecordFilter(FilterInfo filter, String key) {
        // 核算组织
        FilterItemInfo mulcalorg = filter.getFilterItem("nckd_mulcalorg");
        // 成本账簿
        FilterItemInfo mulcostaccount = filter.getFilterItem("nckd_mulcostaccount");
        // 单据类型
        FilterItemInfo mulbilltype = filter.getFilterItem("nckd_mulbilltype");
        // 开始日期
        FilterItemInfo startdate = filter.getFilterItem("nckd_startdate");
        // 结束日期
        FilterItemInfo enddate = filter.getFilterItem("nckd_enddate");

        QFilter qFilter = new QFilter("entry.costpricesource", QCP.not_equals, " ")
                .and(new QFilter("bizentityobject", QCP.equals, key));
        // 核算组织
        if (mulcalorg.getValue() != null) {
            qFilter.and(new QFilter("calorg", QCP.in, ((DynamicObjectCollection) mulcalorg.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 成本账簿
        if (mulcostaccount.getValue() != null) {
            qFilter.and(new QFilter("costaccount", QCP.in, ((DynamicObjectCollection) mulcostaccount.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 单据类型
        if (mulbilltype.getValue() != null) {
            qFilter.and(new QFilter("billtype", QCP.in, ((DynamicObjectCollection) mulbilltype.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 开始日期
        if (startdate.getValue() != null) {
            qFilter.and(new QFilter("bookdate", QCP.large_equals, startdate.getDate()));
        }
        // 结束日期
        if (enddate.getValue() != null) {
            qFilter.and(new QFilter("bookdate", QCP.less_equals, enddate.getDate()));
        }

        return qFilter;
    }

    /**
     * 构建采购入库单的过滤条件
     *
     * @param filter 查询条件
     * @param flag   1 只有客户 2 只有供应商 3 客户和供应商都没有
     * @return
     */
    private QFilter buildImPurinbillFilter(FilterInfo filter, String flag) {
        // 库存事务
        FilterItemInfo mulinvscheme = filter.getFilterItem("nckd_mulinvscheme");
        // 仓库
        FilterItemInfo mulwarehouse = filter.getFilterItem("nckd_mulwarehouse");
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");

        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C");

        // 库存事务
        if (mulinvscheme.getValue() != null) {
            qFilter = qFilter.and(new QFilter("invscheme", QCP.in, ((DynamicObjectCollection) mulinvscheme.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 仓库
        if (mulwarehouse.getValue() != null) {
            qFilter = qFilter.and(new QFilter("billentry.warehouse", QCP.in, ((DynamicObjectCollection) mulwarehouse.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 客户
        if (mulcustomer.getValue() != null && "1".equals(flag)) {
            qFilter = qFilter.and(new QFilter("customer", QCP.in, ((DynamicObjectCollection) mulcustomer.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 供应商
        if (mulsupplier.getValue() != null && "2".equals(flag)) {
            qFilter = qFilter.and(new QFilter("supplier", QCP.in, ((DynamicObjectCollection) mulsupplier.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        return qFilter;
    }

    /**
     * 构建物料分类的过滤条件
     *
     * @param filter 查询条件
     * @return
     */
    private QFilter buildMaterialgroupFilter(FilterInfo filter) {
        // 物料分类
        FilterItemInfo mulmaterialgroup = filter.getFilterItem("nckd_mulmaterialgroup");

        QFilter qFilter = QFilter.of("1 = 1", new Object[0]);
        if (mulmaterialgroup.getValue() != null) {
            qFilter.and(new QFilter("group", QCP.in, ((DynamicObjectCollection) mulmaterialgroup.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }

        return qFilter;
    }

    /**
     * 构建物料的过滤条件
     *
     * @param filter 查询条件
     * @return
     */
    private QFilter buildMaterialFilter(FilterInfo filter) {
        // 物料从
        FilterItemInfo mulmaterial = filter.getFilterItem("nckd_mulmaterial");
        // 物料至
        FilterItemInfo materialto = filter.getFilterItem("nckd_materialto");

        QFilter qFilter = QFilter.of("1 = 1", new Object[0]);
        if (mulmaterial.getValue() != null) {
            DynamicObjectCollection mulmaterialValue = (DynamicObjectCollection) mulmaterial.getValue();
            if (mulmaterialValue.size() > 1) {
                qFilter.and(new QFilter("id", QCP.in, mulmaterialValue.stream().map(obj -> obj.getLong("id"))
                        .collect(Collectors.toList())));
            } else {
                qFilter.and(new QFilter("number", QCP.large_equals, mulmaterialValue.get(0).getString("number")));
            }
        }
        if (materialto.getValue() != null) {
            qFilter.and(new QFilter("number", QCP.less_equals, ((DynamicObject) materialto.getValue()).getString("number")));
        }

        return qFilter;
    }
}
