package nckd.yanye.scm.plugin.report;

import java.util.stream.Collectors;

import kd.bos.algo.DataSet;
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
        DataSet imPurinbillDataSet = this.getImPurinbillDataSet(reportQueryParam);

        return imPurinbillDataSet;
    }

    /**
     * 采购入库单
     *
     * @return
     */
    private DataSet getImPurinbillDataSet(ReportQueryParam reportQueryParam) {
        // 查询条件
        FilterInfo filter = reportQueryParam.getFilter();
        // 核算组织
        FilterItemInfo mulcalorg = filter.getFilterItem("nckd_mulcalorg");
        // 成本账簿
        FilterItemInfo mulcostaccount = filter.getFilterItem("nckd_mulcostaccount");
        // 库存事务
        FilterItemInfo mulinvscheme = filter.getFilterItem("nckd_mulinvscheme");
        // 仓库
        FilterItemInfo mulwarehouse = filter.getFilterItem("nckd_mulwarehouse");
        // 客户
        FilterItemInfo mulcustomer = filter.getFilterItem("nckd_mulcustomer");
        // 供应商
        FilterItemInfo mulsupplier = filter.getFilterItem("nckd_mulsupplier");
        // 物料分类
        FilterItemInfo mulmaterialgroup = filter.getFilterItem("nckd_mulmaterialgroup");
        // 物料从
        FilterItemInfo mulmaterial = filter.getFilterItem("nckd_mulmaterial");
        // 物料至
        FilterItemInfo materialto = filter.getFilterItem("nckd_materialto");

        // ======================一级========================
        // 核算成本记录
        QFilter qFilter = new QFilter("entry.costpricesource", QCP.is_notnull, null)
                .and(new QFilter("bizentityobject", QCP.equals, "im_purinbill"));
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
        DataSet calCostrecord = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cal_costrecord", "bookdate,billnumber,calorg nckd_org,costaccount nckd_costaccount,entry.material nckd_masterid,entry.actualcost in_amount,null out_amount", qFilter.toArray(), null);

        // 获取核算单类型是入库且已审核的成本调整单
        QFilter qFilter1 = new QFilter("biztype", QCP.equals, "A")
                .and(new QFilter("billstatus", QCP.equals, "C"));
        DataSet calCostadjustbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cal_costadjustbill", "entryentity.material masterid,entryentity.adjustamt adjustamt", qFilter1.toArray(), null);

        DataSet finish1 = calCostrecord.leftJoin(calCostadjustbill).on("nckd_masterid", "masterid").select(
                new String[]{"billnumber", "nckd_org", "nckd_costaccount", "nckd_masterid", "in_amount", "out_amount"},
                new String[]{"( CASE WHEN adjustamt IS NULL THEN 0 ELSE adjustamt END )adjustamount"}).finish();
        finish1 = finish1.select("billnumber", "nckd_org", "nckd_costaccount", "nckd_masterid", "(in_amount + adjustamount) nckd_in_amounts", "out_amount nckd_out_amount");

        // =====================二级=========================
        QFilter qFilter2 = new QFilter("billstatus", QCP.equals, "C");
        // 库存事务
        if (mulinvscheme.getValue() != null) {
            qFilter2 = qFilter2.and(new QFilter("invscheme", QCP.in, ((DynamicObjectCollection) mulinvscheme.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 仓库
        if (mulwarehouse.getValue() != null) {
            qFilter2 = qFilter2.and(new QFilter("billentry.warehouse", QCP.in, ((DynamicObjectCollection) mulwarehouse.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 供应商
        if (mulsupplier.getValue() != null) {
            qFilter2 = qFilter2.and(new QFilter("supplier", QCP.in, ((DynamicObjectCollection) mulsupplier.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        // 采购入库单
        String imPurinbillSql = "biztime nckd_biztime,bookdate nckd_bookdate,auditdate nckd_auditdate,billtype.name nckd_billtype_name,billno nckd_billno,invscheme.name nckd_invscheme_name," +
                "billentry.warehouse nckd_warehouse,operator.operatorname nckd_operatorname,dept.name nckd_deptname,null nckd_salesman,null nckd_bp_number,null nckd_bp_name,supplier.name nckd_supplier_name,supplier.number nckd_supplier_number," +
                "billentry.seq nckd_seq,billentry.materialmasterid materialmasterid," +
                "billentry.auxpty nckd_auxpty,billentry.lotnumber nckd_lotnumber,billentry.configuredcode.number nckd_configuredcodenumber,billentry.tracknumber.number nckd_tracknumber_number,billentry.ispresent nckd_ispresent,null nckd_rework,billentry.unit nckd_unit," +
                "billentry.baseqty nckd_baseqty,billentry.unit2nd nckd_unit2nd,billentry.qtyunit2nd nckd_qtyunit2nd,null nckd_unit3nd,null nckd_qtyunit3nd,billentry.price nckd_price," +
                "billentry.srcbillentity srcbillentity,billentry.srcbillnumber srcbillnumber";
        DataSet imPurinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_purinbill", imPurinbillSql, qFilter2.toArray(), null);

        // 客户
        if (mulcustomer.getValue() != null) {
            imPurinbill = imPurinbill.where("nckd_bp_name is not null");
        }

        // 物料
        DataSet bdMaterial = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_material", "masterid,modelnum nckd_modelnum,nckd_model,createorg", null, null);
        // 物料分类标准页签
        QFilter qFilter3 = QFilter.of("1 = 1", new Object[0]);
        if (mulmaterialgroup.getValue() != null) {
            qFilter3.and(new QFilter("group", QCP.in, ((DynamicObjectCollection) mulmaterialgroup.getValue())
                    .stream()
                    .map(obj -> obj.getLong("id"))
                    .collect(Collectors.toList())));
        }
        DataSet bdMaterialgroupdetail = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_materialgroupdetail", "group nckd_group,material,createorg", qFilter3.toArray(), null);


        DataSet finish2 = bdMaterial.join(bdMaterialgroupdetail).on("masterid", "material")//.on("createorg", "createorg")
                .select(
                        new String[]{"masterid", "nckd_modelnum", "nckd_model"},
                        new String[]{"nckd_group"}).finish();

        DataSet finish3 = imPurinbill.join(finish2).on("materialmasterid", "masterid")
                .select(new String[]{"nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "materialmasterid",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "srcbillentity", "srcbillnumber"},
                        new String[]{"nckd_modelnum", "nckd_model", "nckd_group"}).finish();

        // 核算成本和采购入库关联
        DataSet finish4 = finish1.leftJoin(finish3).on("billnumber", "nckd_billno").on("nckd_masterid", "materialmasterid")
                .select(new String[]{"nckd_org", "nckd_costaccount", "nckd_masterid", "nckd_in_amounts", "nckd_out_amount"},
                        new String[]{"nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "nckd_modelnum", "nckd_model",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "srcbillentity", "srcbillnumber",
                                "nckd_group"}).finish();

        // ==================三级=========================
        DataSet finish4Copy1 = finish4.copy();
        DataSet finish4Copy2 = finish4.copy();

        // 采购订单
        String pmPurorderbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,nckd_xm.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.qty nckd_qty,billentry.payablepriceqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,supplier.simplename nckd_suppliermnemoniccode";
        DataSet pmPurorderbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "pm_purorderbill", pmPurorderbillSql, null, null);

        DataSet finish5 = finish4Copy1.leftJoin(pmPurorderbill).on("srcbillnumber", "nckd_sourcebillno").on("nckd_masterid", "materialmasterid")
                .select(new String[]{"nckd_org", "nckd_costaccount", "nckd_masterid", "nckd_in_amounts", "nckd_out_amount",
                                "nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "nckd_modelnum", "nckd_model",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "nckd_group", "srcbillentity"},
                        new String[]{"nckd_sourcebilltypename", "nckd_sourcebillno", "nckd_nummer_reisdocument", "nckd_xm_name",
                                "nckd_shippingordernumber", "nckd_cass", "nckd_shipping_address", "nckd_order_serial_number", "nckd_loss_quantity", "nckd_damaged_quantity",
                                "nckd_wagon_number", "nckd_direct_mode", "nckd_carrier", "nckd_carrier_phone", "nckd_phone", "nckd_carnumber",
                                "nckd_comment", "nckd_qty", "nckd_payablepriceqty", "nckd_entrycomment",
                                "nckd_creator_name", "nckd_auditor_name", "nckd_h_source", "nckd_customermnemoniccode", "nckd_suppliermnemoniccode"}).finish()
                .where("srcbillentity = 'pm_purorderbill'");

        // 采购收货单
        String imPurreceivebillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,nckd_xm.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.qty nckd_qty,billentry.joinpriceqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,supplier.simplename nckd_suppliermnemoniccode";
        DataSet imPurreceivebill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_purreceivebill", imPurreceivebillSql, null, null);

        DataSet finish6 = finish4Copy2.leftJoin(imPurreceivebill).on("srcbillnumber", "nckd_sourcebillno").on("nckd_masterid", "materialmasterid")
                .select(new String[]{"nckd_org", "nckd_costaccount", "nckd_masterid", "nckd_in_amounts", "nckd_out_amount",
                                "nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "nckd_modelnum", "nckd_model",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "nckd_group", "srcbillentity"},
                        new String[]{"nckd_sourcebilltypename", "nckd_sourcebillno", "nckd_nummer_reisdocument", "nckd_xm_name",
                                "nckd_shippingordernumber", "nckd_cass", "nckd_shipping_address", "nckd_order_serial_number", "nckd_loss_quantity", "nckd_damaged_quantity",
                                "nckd_wagon_number", "nckd_direct_mode", "nckd_carrier", "nckd_carrier_phone", "nckd_phone", "nckd_carnumber",
                                "nckd_comment", "nckd_qty", "nckd_payablepriceqty", "nckd_entrycomment",
                                "nckd_creator_name", "nckd_auditor_name", "nckd_h_source", "nckd_customermnemoniccode", "nckd_suppliermnemoniccode"}).finish()
                .where("srcbillentity = 'im_purreceivebill'");

        // 采购入库单
        String purorderbillSql = "billtype.name nckd_sourcebilltypename,billno nckd_sourcebillno,null nckd_nummer_reisdocument,nckd_xm.name nckd_xm_name," +
                "null nckd_shippingordernumber,null nckd_cass,null nckd_shipping_address,null nckd_order_serial_number,null nckd_loss_quantity,null nckd_damaged_quantity," +
                "null nckd_wagon_number, null nckd_direct_mode,null nckd_carrier,null nckd_carrier_phone,null nckd_phone,null nckd_carnumber," +
                "comment nckd_comment,billentry.materialmasterid materialmasterid,billentry.qty nckd_qty,billentry.joinpriceqty nckd_payablepriceqty,billentry.entrycomment nckd_entrycomment," +
                "creator.name nckd_creator_name,auditor.name nckd_auditor_name,null nckd_h_source,null nckd_customermnemoniccode,supplier.simplename nckd_suppliermnemoniccode";
        DataSet purinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_purinbill", purorderbillSql, null, null);
        DataSet finish7 = finish4.leftJoin(purinbill).on("srcbillnumber", "nckd_sourcebillno").on("nckd_masterid", "materialmasterid")
                .select(new String[]{"nckd_org", "nckd_costaccount", "nckd_masterid", "nckd_in_amounts", "nckd_out_amount",
                                "nckd_biztime", "nckd_bookdate", "nckd_auditdate", "nckd_billtype_name", "nckd_billno", "nckd_invscheme_name",
                                "nckd_warehouse", "nckd_operatorname", "nckd_deptname", "nckd_salesman", "nckd_bp_number", "nckd_bp_name", "nckd_supplier_name", "nckd_supplier_number",
                                "nckd_seq", "nckd_modelnum", "nckd_model",
                                "nckd_auxpty", "nckd_lotnumber", "nckd_configuredcodenumber", "nckd_tracknumber_number", "nckd_ispresent", "nckd_rework", "nckd_unit",
                                "nckd_baseqty", "nckd_unit2nd", "nckd_qtyunit2nd", "nckd_unit3nd", "nckd_qtyunit3nd", "nckd_price", "nckd_group", "srcbillentity"},
                        new String[]{"nckd_sourcebilltypename", "nckd_sourcebillno", "nckd_nummer_reisdocument", "nckd_xm_name",
                                "nckd_shippingordernumber", "nckd_cass", "nckd_shipping_address", "nckd_order_serial_number", "nckd_loss_quantity", "nckd_damaged_quantity",
                                "nckd_wagon_number", "nckd_direct_mode", "nckd_carrier", "nckd_carrier_phone", "nckd_phone", "nckd_carnumber",
                                "nckd_comment", "nckd_qty", "nckd_payablepriceqty", "nckd_entrycomment",
                                "nckd_creator_name", "nckd_auditor_name", "nckd_h_source", "nckd_customermnemoniccode", "nckd_suppliermnemoniccode"}).finish()
                .where("srcbillentity = 'im_purinbill'");

        return finish5.union(finish6).union(finish7).distinct();
    }
}
