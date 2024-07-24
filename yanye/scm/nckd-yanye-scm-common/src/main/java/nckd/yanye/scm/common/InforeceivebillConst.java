package nckd.yanye.scm.common;

/**
 * Module           : 供应链云--采购管理模块--信息接收单单据
 * Description      : 单据常量类
 *
 * @author : Generator
 * @version : 1.0
 * @date : 2024-07-24
 */
public class InforeceivebillConst {

    public static final String FORMBILLID = "nckd_pm_inforeceivebill";
    public static final String ID = "id";

    /**
     * Type:String,Name:单据编号
     */
    public static final String BILLNO = "billno";

    /**
     * Type:String,Name:单据状态
     */
    public static final String BILLSTATUS = "billstatus";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:创建人
     */
    public static final String CREATOR = "creator";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
     */
    public static final String MODIFIER = "modifier";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:审核人
     */
    public static final String AUDITOR = "auditor";

    /**
     * Type:java.util.Date,Name:审核日期
     */
    public static final String AUDITDATE = "auditdate";

    /**
     * Type:java.util.Date,Name:修改时间
     */
    public static final String MODIFYTIME = "modifytime";

    /**
     * Type:java.util.Date,Name:创建时间
     */
    public static final String CREATETIME = "createtime";

    /**
     * 分录entryentity实体标识
     */
    public static final String ENTRYENTITYID_ENTRYENTITY = "entryentity";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
     */
    public static final String ENTRYENTITY_MODIFIERFIELD = "modifierfield";

    /**
     * Type:java.util.Date,Name:修改时间
     */
    public static final String ENTRYENTITY_MODIFYDATEFIELD = "modifydatefield";

    /**
     * Type:DynamicObject,sourceEntityId:bd_materialpurchaseinfo,Name:物料编码
     */
    public static final String ENTRYENTITY_NCKD_MATERIAL = "nckd_material";

    /**
     * Type:String,Name:物料名称
     */
    public static final String ENTRYENTITY_NCKD_MATERIALNAME = "nckd_materialname";

    /**
     * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:计量单位
     */
    public static final String ENTRYENTITY_NCKD_UNIT = "nckd_unit";

    /**
     * Type:java.math.BigDecimal,Name:数量
     */
    public static final String ENTRYENTITY_NCKD_APPLYQTY = "nckd_applyqty";

    /**
     * Type:java.math.BigDecimal,Name:单价
     */
    public static final String ENTRYENTITY_NCKD_PRICE = "nckd_price";

    /**
     * Type:java.math.BigDecimal,Name:含税单价
     */
    public static final String ENTRYENTITY_NCKD_PRICEANDTAX = "nckd_priceandtax";

    /**
     * Type:java.math.BigDecimal,Name:税率(%)
     */
    public static final String ENTRYENTITY_NCKD_TAXRATE = "nckd_taxrate";

    /**
     * Type:java.math.BigDecimal,Name:金额
     */
    public static final String ENTRYENTITY_NCKD_AMOUNT = "nckd_amount";

    /**
     * Type:java.math.BigDecimal,Name:税额
     */
    public static final String ENTRYENTITY_NCKD_TAXAMOUNT = "nckd_taxamount";

    /**
     * Type:java.math.BigDecimal,Name:价税合计
     */
    public static final String ENTRYENTITY_NCKD_AMOUNTANDTAX = "nckd_amountandtax";

    /**
     * Type:String,Name:采购申请单单号
     */
    public static final String NCKD_PURAPPLYBILLNO = "nckd_purapplybillno";

    /**
     * Type:String,Name:采购类型
     */
    public static final String NCKD_PURCHASETYPE = "nckd_purchasetype";

    /**
     * Type:java.math.BigDecimal,Name:招采成交价税合计
     */
    public static final String NCKD_TOTALPRICE = "nckd_totalprice";

    /**
     * Type:boolean,Name:采购订单生成状态
     */
    public static final String NCKD_GENERATIONSTATUS = "nckd_generationstatus";

    /**
     * Type:String,Name:供应商/订单生成失败原因
     */
    public static final String NCKD_FAILINFO = "nckd_failinfo";

    /**
     * Type:DynamicObject,sourceEntityId:bd_currency,Name:币别
     */
    public static final String NCKD_CURRENCY = "nckd_currency";

    /**
     * 分录nckd_winentryentity实体标识
     */
    public static final String ENTRYENTITYID_NCKD_WINENTRYENTITY = "nckd_winentryentity";

    /**
     * Type:String,Name:供应商id
     */
    public static final String NCKD_WINENTRYENTITY_NCKD_SUPPLIERID = "nckd_supplierid";

    /**
     * Type:String,Name:供应商名称
     */
    public static final String NCKD_WINENTRYENTITY_NCKD_SUPPLIERNAME = "nckd_suppliername";

    /**
     * Type:String,Name:社会统一信用代码
     */
    public static final String NCKD_WINENTRYENTITY_NCKD_USCC = "nckd_uscc";

    /**
     * Type:java.math.BigDecimal,Name:中标价
     */
    public static final String NCKD_WINENTRYENTITY_NCKD_BIDPRICE = "nckd_bidprice";
    public static final String NCKD_PROCUREMENTS = "nckd_procurements";


    public static final String ALLPROPERTY = "id,billno,billstatus,creator,modifier,auditor,auditdate,modifytime,createtime,entryentity.id,entryentity.modifierfield,entryentity.modifydatefield,entryentity.nckd_material,entryentity.nckd_materialname,entryentity.nckd_unit,entryentity.nckd_applyqty,entryentity.nckd_price,entryentity.nckd_priceandtax,entryentity.nckd_taxrate,entryentity.nckd_amount,entryentity.nckd_taxamount,entryentity.nckd_amountandtax,nckd_purapplybillno,nckd_purchasetype,nckd_totalprice,nckd_generationstatus,nckd_failinfo,nckd_currency,nckd_winentryentity.id,nckd_winentryentity.nckd_supplierid,nckd_winentryentity.nckd_suppliername,nckd_winentryentity.nckd_uscc,nckd_winentryentity.nckd_bidprice";

}