package nckd.yanye.scm.common;

/**
 * Module           : 供应链云--采购管理模块--采购申请单单据
 * Description      : 单据常量类
 * @date            : 2024-07-18
 * @author          : Generator
 * @version         : 1.0
 */
public class PurapplybillConst {

    public static final String FORMBILLID = "pm_purapplybill";
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
     * Type:DynamicObject,sourceEntityId:bos_org,Name:申请组织
     */
    public static final String ORG = "org";

    /**
     * Type:DynamicObject,sourceEntityId:bos_billtype,Name:单据类型
     */
    public static final String BILLTYPE = "billtype";

    /**
     * Type:DynamicObject,sourceEntityId:bd_biztype,Name:业务类型
     */
    public static final String BIZTYPE = "biztype";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:申请部门
     */
    public static final String DEPT = "dept";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:申请人
     */
    public static final String BIZUSER = "bizuser";

    /**
     * Type:String,Name:备注
     */
    public static final String COMMENT = "comment";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:关闭人
     */
    public static final String CLOSER = "closer";

    /**
     * Type:java.util.Date,Name:关闭日期
     */
    public static final String CLOSEDATE = "closedate";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:作废人
     */
    public static final String CANCELER = "canceler";

    /**
     * Type:java.util.Date,Name:作废日期
     */
    public static final String CANCELDATE = "canceldate";

    /**
     * 分录billentry实体标识
     */
    public static final String ENTRYENTITYID_BILLENTRY = "billentry";

    /**
     * Type:DynamicObject,sourceEntityId:bd_materialpurchaseinfo,Name:物料编码
     */
    public static final String BILLENTRY_MATERIAL = "material";

    /**
     * Type:DynamicObject,Name:辅助属性
     */
    public static final String BILLENTRY_AUXPTY = "auxpty";

    /**
     * Type:java.math.BigDecimal,Name:核准数量
     */
    public static final String BILLENTRY_QTY = "qty";

    /**
     * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:计量单位
     */
    public static final String BILLENTRY_UNIT = "unit";

    /**
     * Type:java.math.BigDecimal,Name:基本数量
     */
    public static final String BILLENTRY_BASEQTY = "baseqty";

    /**
     * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:基本单位
     */
    public static final String BILLENTRY_BASEUNIT = "baseunit";

    /**
     * Type:String,Name:备注
     */
    public static final String BILLENTRY_ENTRYCOMMENT = "entrycomment";

    /**
     * Type:String,Name:物料名称
     */
    public static final String BILLENTRY_MATERIALNAME = "materialname";

    /**
     * Type:String,Name:行关闭状态
     */
    public static final String BILLENTRY_ROWCLOSESTATUS = "rowclosestatus";

    /**
     * Type:String,Name:行终止状态
     */
    public static final String BILLENTRY_ROWTERMINATESTATUS = "rowterminatestatus";

    /**
     * Type:DynamicObject,sourceEntityId:bd_material,Name:主物料(封存)
     */
    public static final String BILLENTRY_MATERIALMASTERID = "materialmasterid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_materialversion,Name:物料版本
     */
    public static final String BILLENTRY_MATERIALVERSION = "materialversion";

    /**
     * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:辅助单位
     */
    public static final String BILLENTRY_AUXUNIT = "auxunit";

    /**
     * Type:java.math.BigDecimal,Name:辅助数量
     */
    public static final String BILLENTRY_AUXQTY = "auxqty";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:创建人
     */
    public static final String BILLENTRY_ENTRYCREATOR = "entrycreator";

    /**
     * Type:java.util.Date,Name:创建时间
     */
    public static final String BILLENTRY_ENTRYCREATETIME = "entrycreatetime";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
     */
    public static final String BILLENTRY_ENTRYMODIFIER = "entrymodifier";

    /**
     * Type:java.util.Date,Name:修改时间
     */
    public static final String BILLENTRY_ENTRYMODIFYTIME = "entrymodifytime";

    /**
     * Type:DynamicObject,sourceEntityId:bd_linetype,Name:行类型
     */
    public static final String BILLENTRY_LINETYPE = "linetype";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:需求组织
     */
    public static final String BILLENTRY_ENTRYREQORG = "entryreqorg";

    /**
     * Type:java.util.Date,Name:交货日期
     */
    public static final String BILLENTRY_DELIVERDATE = "deliverdate";

    /**
     * Type:java.lang.Integer,Name:采购提前期
     */
    public static final String BILLENTRY_PURLEADDAY = "purleadday";

    /**
     * Type:java.util.Date,Name:建议采购日期
     */
    public static final String BILLENTRY_PURDATE = "purdate";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:收货组织
     */
    public static final String BILLENTRY_ENTRYRECORG = "entryrecorg";

    /**
     * Type:DynamicObject,sourceEntityId:bd_operatorgroup,Name:采购组
     */
    public static final String BILLENTRY_ENTRYOPERATORGROUP = "entryoperatorgroup";

    /**
     * Type:DynamicObject,sourceEntityId:bd_operator,Name:采购员
     */
    public static final String BILLENTRY_ENTRYOPERATOR = "entryoperator";

    /**
     * Type:DynamicObject,sourceEntityId:bd_supplier,Name:建议供应商
     */
    public static final String BILLENTRY_SUPPLIER = "supplier";

    /**
     * Type:java.math.BigDecimal,Name:单价
     */
    public static final String BILLENTRY_PRICE = "price";

    /**
     * Type:java.math.BigDecimal,Name:税率(%)
     */
    public static final String BILLENTRY_TAXRATE = "taxrate";

    /**
     * Type:java.math.BigDecimal,Name:含税单价
     */
    public static final String BILLENTRY_PRICEANDTAX = "priceandtax";

    /**
     * Type:java.math.BigDecimal,Name:金额
     */
    public static final String BILLENTRY_AMOUNT = "amount";

    /**
     * Type:java.math.BigDecimal,Name:价税合计
     */
    public static final String BILLENTRY_AMOUNTANDTAX = "amountandtax";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:分录采购组织(封存)
     */
    public static final String BILLENTRY_ENTRYPUROG = "entrypurog";

    /**
     * Type:java.util.Date,Name:需求日期
     */
    public static final String BILLENTRY_REQDATE = "reqdate";

    /**
     * Type:java.math.BigDecimal,Name:数量
     */
    public static final String BILLENTRY_APPLYQTY = "applyqty";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:采购部门
     */
    public static final String BILLENTRY_ENTRYPURDEPT = "entrypurdept";

    /**
     * Type:java.math.BigDecimal,Name:已订货基本数量
     */
    public static final String BILLENTRY_ORDERBASEQTY = "orderbaseqty";

    /**
     * Type:java.math.BigDecimal,Name:已收货基本数量
     */
    public static final String BILLENTRY_RECEIVEBASEQTY = "receivebaseqty";

    /**
     * Type:java.math.BigDecimal,Name:已入库基本数量
     */
    public static final String BILLENTRY_INVBASEQTY = "invbaseqty";

    /**
     * Type:java.math.BigDecimal,Name:已订货数量
     */
    public static final String BILLENTRY_ORDERQTY = "orderqty";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:收货部门
     */
    public static final String BILLENTRY_ENTRYRECDEPT = "entryrecdept";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:需求部门
     */
    public static final String BILLENTRY_ENTRYREQDEPT = "entryreqdept";

    /**
     * Type:java.math.BigDecimal,Name:税额
     */
    public static final String BILLENTRY_TAXAMOUNT = "taxamount";

    /**
     * Type:java.math.BigDecimal,Name:已收货数量
     */
    public static final String BILLENTRY_RECEIVEQTY = "receiveqty";

    /**
     * Type:java.math.BigDecimal,Name:已入库数量
     */
    public static final String BILLENTRY_INVQTY = "invqty";

    /**
     * Type:DynamicObject,sourceEntityId:bd_taxrate,Name:税率
     */
    public static final String BILLENTRY_TAXRATEID = "taxrateid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_project,Name:项目
     */
    public static final String BILLENTRY_PROJECT = "project";

    /**
     * Type:java.math.BigDecimal,Name:关联数量
     */
    public static final String BILLENTRY_JOINQTY = "joinqty";

    /**
     * Type:java.math.BigDecimal,Name:关联基本数量
     */
    public static final String BILLENTRY_JOINBASEQTY = "joinbaseqty";

    /**
     * Type:String,Name:来源单据实体
     */
    public static final String BILLENTRY_SRCBILLENTITY = "srcbillentity";

    /**
     * Type:java.lang.Long,Name:来源单据ID
     */
    public static final String BILLENTRY_SRCBILLID = "srcbillid";

    /**
     * Type:String,Name:来源单据编号
     */
    public static final String BILLENTRY_SRCBILLNUMBER = "srcbillnumber";

    /**
     * Type:java.lang.Long,Name:来源单据行ID
     */
    public static final String BILLENTRY_SRCBILLENTRYID = "srcbillentryid";

    /**
     * Type:java.lang.Long,Name:来源单据分录序号
     */
    public static final String BILLENTRY_SRCBILLENTRYSEQ = "srcbillentryseq";

    /**
     * Type:java.lang.Long,Name:委外工单ID
     */
    public static final String BILLENTRY_MFTORDERID = "mftorderid";

    /**
     * Type:String,Name:委外工单编号
     */
    public static final String BILLENTRY_MFTORDERNUMBER = "mftordernumber";

    /**
     * Type:java.lang.Long,Name:委外工单行ID
     */
    public static final String BILLENTRY_MFTORDERENTRYID = "mftorderentryid";

    /**
     * Type:java.lang.Long,Name:委外工单分录序号
     */
    public static final String BILLENTRY_MFTORDERENTRYSEQ = "mftorderentryseq";

    /**
     * Type:String,Name:产品类型
     */
    public static final String BILLENTRY_PRODUCTTYPE = "producttype";

    /**
     * Type:DynamicObject,sourceEntityId:bd_tracknumber,Name:跟踪号
     */
    public static final String BILLENTRY_TRACKNUMBER = "tracknumber";

    /**
     * Type:DynamicObject,sourceEntityId:bd_configuredcode,Name:配置号
     */
    public static final String BILLENTRY_CONFIGUREDCODE = "configuredcode";

    /**
     * Type:java.lang.Long,Name:销售订单ID
     */
    public static final String BILLENTRY_SALBILLID = "salbillid";

    /**
     * Type:java.lang.Long,Name:销售订单行ID
     */
    public static final String BILLENTRY_SALBILLENTRYID = "salbillentryid";

    /**
     * Type:java.lang.Long,Name:销售订单分录序号
     */
    public static final String BILLENTRY_SALBILLENTRYSEQ = "salbillentryseq";

    /**
     * Type:String,Name:销售订单编号
     */
    public static final String BILLENTRY_SALBILLNUMBER = "salbillnumber";

    /**
     * Type:java.math.BigDecimal,Name:可用库存基本数量
     */
    public static final String BILLENTRY_AVINVBASEQTY = "avinvbaseqty";

    /**
     * Type:String,Name:变更方式
     */
    public static final String BILLENTRY_ENTRYCHANGETYPE = "entrychangetype";

    /**
     * Type:String,Name:采购方式
     */
    public static final String BILLENTRY_PURMETHOD = "purmethod";

    /**
     * Type:java.util.Date,Name:展BOM时间
     */
    public static final String BILLENTRY_BOMTIME = "bomtime";

    /**
     * Type:DynamicObject,sourceEntityId:bos_costcenter,Name:成本中心
     */
    public static final String BILLENTRY_ECOSTCENTER = "ecostcenter";

    /**
     * Type:boolean,Name:重新展算订单物料
     */
    public static final String BILLENTRY_ISREDORDERMATE = "isredordermate";

    /**
     * Type:DynamicObject,sourceEntityId:bd_warehouse,Name:收货仓库
     */
    public static final String BILLENTRY_WAREHOUSE = "warehouse";

    /**
     * 子分录billentry_lk实体标识
     */
    public static final String SUBENTRYENTITYID_BILLENTRY_LK = "billentry_lk";

    /**
     * Type:java.lang.Long,Name:源单主实体编码
     */
    public static final String BILLENTRY_LK_BILLENTRY_LK_STABLEID = "billentry_lk_stableid";

    /**
     * Type:java.lang.Long,Name:源单内码
     */
    public static final String BILLENTRY_LK_BILLENTRY_LK_SBILLID = "billentry_lk_sbillid";

    /**
     * Type:java.lang.Long,Name:源单主实体内码
     */
    public static final String BILLENTRY_LK_BILLENTRY_LK_SID = "billentry_lk_sid";

    /**
     * Type:java.math.BigDecimal,Name:基本数量_原始携带值
     */
    public static final String BILLENTRY_LK_BILLENTRY_LK_BASEQTY_OLD = "billentry_lk_baseqty_old";

    /**
     * Type:java.math.BigDecimal,Name:基本数量_确认携带值
     */
    public static final String BILLENTRY_LK_BILLENTRY_LK_BASEQTY = "billentry_lk_baseqty";

    /**
     * Type:java.math.BigDecimal,Name:数量_原始携带值
     */
    public static final String BILLENTRY_LK_BILLENTRY_LK_APPLYQTY_OLD = "billentry_lk_applyqty_old";

    /**
     * Type:java.math.BigDecimal,Name:数量_确认携带值
     */
    public static final String BILLENTRY_LK_BILLENTRY_LK_APPLYQTY = "billentry_lk_applyqty";

    /**
     * Type:String,Name:关闭状态
     */
    public static final String CLOSESTATUS = "closestatus";

    /**
     * Type:String,Name:作废状态
     */
    public static final String CANCELSTATUS = "cancelstatus";

    /**
     * Type:java.util.Date,Name:申请日期
     */
    public static final String BIZTIME = "biztime";

    /**
     * Type:String,Name:单据生成类型
     */
    public static final String BILLCRETYPE = "billcretype";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
     */
    public static final String LASTUPDATEUSER = "lastupdateuser";

    /**
     * Type:java.util.Date,Name:修改时间
     */
    public static final String LASTUPDATETIME = "lastupdatetime";

    /**
     * Type:String,Name:计量单位来源
     */
    public static final String UNITSRCTYPE = "unitsrctype";

    /**
     * Type:java.math.BigDecimal,Name:价税合计
     */
    public static final String TOTALALLAMOUNT = "totalallamount";

    /**
     * Type:DynamicObject,sourceEntityId:bd_currency,Name:币别
     */
    public static final String CURRENCY = "currency";

    /**
     * Type:boolean,Name:含税
     */
    public static final String ISTAX = "istax";

    /**
     * Type:String,Name:询价状态
     */
    public static final String INQUIRYSTATUS = "inquirystatus";

    /**
     * Type:String,Name:竞价状态
     */
    public static final String BIDSTATUS = "bidstatus";

    /**
     * Type:String,Name:变更状态
     */
    public static final String CHANGESTATUS = "changestatus";

    /**
     * Type:java.util.Date,Name:变更日期
     */
    public static final String CHANGEDATE = "changedate";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:变更人
     */
    public static final String CHANGER = "changer";

    /**
     * Type:String,Name:子版本号
     */
    public static final String SUBVERSION = "subversion";

    /**
     * Type:String,Name:版本号
     */
    public static final String VERSION = "version";

    /**
     * Type:String,Name:采购方式
     */
    public static final String NCKD_PROCUREMENTS = "nckd_procurements";

    /**
     * Type:boolean,Name:已推送招采平台
     */
    public static final String NCKD_PUSHED = "nckd_pushed";

    /**
     * Type:boolean,Name:是否推送招采平台
     */
    public static final String NCKD_WHETHERPUSH = "nckd_whetherpush";

    /**
     * Type:String,Name:询比单名称
     */
    public static final String NCKD_INQUIRYLISTNAME = "nckd_inquirylistname";

    /**
     * Type:String,Name:项目类型
     */
    public static final String NCKD_PROJECTTYPE = "nckd_projecttype";

    /**
     * Type:String,Name:报价方式
     */
    public static final String NCKD_QUOTATION = "nckd_quotation";

    /**
     * Type:String,Name:详细地址
     */
    public static final String NCKD_DETAILEDADDR = "nckd_detailedaddr";

    /**
     * Type:String,Name:发布媒体
     */
    public static final String NCKD_PUBLISHMEDIA = "nckd_publishmedia";

    /**
     * Type:String,Name:报价单是否在线签章
     */
    public static final String NCKD_QUOTATIONSIGN = "nckd_quotationsign";

    /**
     * Type:String,Name:报价附件是否在线签章
     */
    public static final String NCKD_QUOTATIONATTSIGN = "nckd_quotationattsign";

    /**
     * Type:java.util.Date,Name:报名截止时间
     */
    public static final String NCKD_DEADLINE = "nckd_deadline";

    /**
     * Type:String,Name:采购类型
     */
    public static final String NCKD_PURCHASETYPE = "nckd_purchasetype";

    /**
     * Type:String,Name:报价含税
     */
    public static final String NCKD_INCLUDESTAX = "nckd_includestax";

    /**
     * Type:String,Name:允许对部分品目报价
     */
    public static final String NCKD_ALLOWFORPARTIAL = "nckd_allowforpartial";

    /**
     * Type:DynamicObject,sourceEntityId:bd_currency,Name:报价币别
     */
    public static final String NCKD_QUOTATIONCURRENCY = "nckd_quotationcurrency";

    /**
     * Type:String,Name:是否设置控制总价
     */
    public static final String NCKD_CONTROLPRICE = "nckd_controlprice";

    /**
     * Type:java.math.BigDecimal,Name:控制总价
     */
    public static final String NCKD_TOTALPRICE = "nckd_totalprice";

    /**
     * Type:String,Name:是否必须上传报价附件
     */
    public static final String NCKD_MUSTUPATT = "nckd_mustupatt";

    /**
     * Type:String,Name:询比方式
     */
    public static final String NCKD_INQUIRYMETHOD = "nckd_inquirymethod";

    /**
     * Type:String,Name:比价方式
     */
    public static final String NCKD_COMPARMETHOD = "nckd_comparmethod";

    /**
     * Type:String,Name:公开范围
     */
    public static final String NCKD_PUBLICSCOPE = "nckd_publicscope";

    /**
     * Type:String,Name:报价时段查看供应商参与名单
     */
    public static final String NCKD_VIEWLIST = "nckd_viewlist";

    /**
     * Type:String,Name:报名审核
     */
    public static final String NCKD_REGISTERAUDIT = "nckd_registeraudit";

    /**
     * Type:String,Name:评审办法
     */
    public static final String NCKD_REVIEWMETHOD = "nckd_reviewmethod";

    /**
     * Type:String,Name:是否需要线上评审
     */
    public static final String NCKD_WHETHERREVIEWOL = "nckd_whetherreviewol";

    /**
     * Type:DynamicObject,Name:内部文件
     */
    public static final String NCKD_INTERNALDOCUMENTS = "nckd_internaldocuments";

    /**
     * Type:DynamicObject,Name:询比文件
     */
    public static final String NCKD_INQUIRYDOCUMENT = "nckd_inquirydocument";

    /**
     * Type:String,Name:备注说明
     */
    public static final String NCKD_REMARKS = "nckd_remarks";

    /**
     * Type:String,Name:公告标题
     */
    public static final String NCKD_ANNOUNCEMENTTITLE = "nckd_announcementtitle";

    /**
     * Type:String,Name:发布设置
     */
    public static final String NCKD_PUBLISHSET = "nckd_publishset";

    /**
     * Type:java.util.Date,Name:发布日期
     */
    public static final String NCKD_TIMINGTIME = "nckd_timingtime";

    /**
     * Type:String,Name:处置方式
     */
    public static final String NCKD_DISPOSALMETHOD = "nckd_disposalmethod";

    /**
     * Type:DynamicObject,Name:供应商
     */
    public static final String NCKD_SUPPLIERS = "nckd_suppliers";

    /**
     * Type:String,Name:邀请函标题
     */
    public static final String NCKD_INVITATIONTITLE = "nckd_invitationtitle";

    /**
     * Type:String,Name:谈判采购名称
     */
    public static final String NCKD_NEGOTIATEDNAME = "nckd_negotiatedname";

    /**
     * Type:String,Name:谈判采购编号
     */
    public static final String NCKD_NEGOTIATEDNUM = "nckd_negotiatednum";

    /**
     * Type:java.math.BigDecimal,Name:谈判采购预算（万元）
     */
    public static final String NCKD_NEGOTIATEDBUDGET = "nckd_negotiatedbudget";

    /**
     * Type:java.util.Date,Name:报名开始时间
     */
    public static final String NCKD_REGSTARTTIME = "nckd_regstarttime";

    /**
     * Type:java.util.Date,Name:提交响应文件截止时间
     */
    public static final String NCKD_SUBDEADTIME = "nckd_subdeadtime";

    /**
     * Type:java.util.Date,Name:报名结束时间
     */
    public static final String NCKD_REGENDTIME = "nckd_regendtime";

    /**
     * Type:String,Name:项目类型
     */
    public static final String NCKD_PROJECTTYPE1 = "nckd_projecttype1";

    /**
     * Type:String,Name:处置方式
     */
    public static final String NCKD_DISPOSALMETHOD1 = "nckd_disposalmethod1";

    /**
     * Type:String,Name:报价方式
     */
    public static final String NCKD_QUOTATION1 = "nckd_quotation1";

    /**
     * Type:String,Name:发布媒体
     */
    public static final String NCKD_PUBLISHMEDIA1 = "nckd_publishmedia1";

    /**
     * Type:String,Name:谈判方式
     */
    public static final String NCKD_NEGOTIATIONMODE = "nckd_negotiationmode";

    /**
     * Type:String,Name:详细地址
     */
    public static final String NCKD_DETAILEDADDR1 = "nckd_detailedaddr1";

    /**
     * Type:String,Name:竞争方式
     */
    public static final String NCKD_COMPETITIONMODE = "nckd_competitionmode";

    /**
     * Type:DynamicObject,Name:供应商
     */
    public static final String NCKD_SUPPLIERS1 = "nckd_suppliers1";

    /**
     * Type:String,Name:公开范围
     */
    public static final String NCKD_PUBLICSCOPE1 = "nckd_publicscope1";

    /**
     * Type:String,Name:报价时段查看供应商参与名单
     */
    public static final String NCKD_VIEWLIST1 = "nckd_viewlist1";

    /**
     * Type:String,Name:报名审核
     */
    public static final String NCKD_REGISTERAUDIT1 = "nckd_registeraudit1";

    /**
     * Type:String,Name:评审办法
     */
    public static final String NCKD_REVIEWMETHOD1 = "nckd_reviewmethod1";

    /**
     * Type:String,Name:是否需要线上评审
     */
    public static final String NCKD_WHETHERREVIEWOL1 = "nckd_whetherreviewol1";

    /**
     * Type:DynamicObject,Name:内部文件
     */
    public static final String NCKD_INTERNALDOCUMENTS1 = "nckd_internaldocuments1";

    /**
     * Type:DynamicObject,Name:谈判文件
     */
    public static final String NCKD_NEGOTIATINGDOCUMENTS = "nckd_negotiatingdocuments";

    /**
     * Type:java.math.BigDecimal,Name:标书费（元）
     */
    public static final String NCKD_TENDERFEE = "nckd_tenderfee";

    /**
     * Type:java.math.BigDecimal,Name:平台服务费（元）
     */
    public static final String NCKD_SERVICEFEE = "nckd_servicefee";

    /**
     * Type:String,Name:项目名称
     */
    public static final String NCKD_PROJECTNAME = "nckd_projectname";

    /**
     * Type:java.math.BigDecimal,Name:项目金额（万元）
     */
    public static final String NCKD_PROJECTAMOUNT = "nckd_projectamount";

    /**
     * Type:String,Name:项目类型
     */
    public static final String NCKD_PROJECTTYPE2 = "nckd_projecttype2";

    /**
     * Type:String,Name:报价方式
     */
    public static final String NCKD_QUOTATION2 = "nckd_quotation2";

    /**
     * Type:String,Name:详细地址
     */
    public static final String NCKD_DETAILEDADDR2 = "nckd_detailedaddr2";

    /**
     * Type:DynamicObject,Name:采购范围
     */
    public static final String NCKD_PROCUREMENTSCOPEATT = "nckd_procurementscopeatt";

    /**
     * Type:DynamicObject,Name:内部文件
     */
    public static final String NCKD_INTERNALDOCUMENTS2 = "nckd_internaldocuments2";

    /**
     * Type:DynamicObject,Name:供应商
     */
    public static final String NCKD_SUPPLIERS2 = "nckd_suppliers2";

    /**
     * Type:DynamicObject,Name:项目文件
     */
    public static final String NCKD_PROJECTFILES = "nckd_projectfiles";

    /**
     * Type:String,Name:内容标题
     */
    public static final String NCKD_CONTENTTITLE = "nckd_contenttitle";

    /**
     * Type:String,Name:详细内容
     */
    public static final String NCKD_DETAILS = "nckd_details";

    /**
     * Type:String,Name:招标名称
     */
    public static final String NCKD_TENDERNAME = "nckd_tendername";

    /**
     * Type:java.math.BigDecimal,Name:招标估算（万元）
     */
    public static final String NCKD_TENDERESTIMATE = "nckd_tenderestimate";

    /**
     * Type:String,Name:中标方式
     */
    public static final String NCKD_BIDMETHOD = "nckd_bidmethod";

    /**
     * Type:String,Name:报价方式
     */
    public static final String NCKD_QUOTATION3 = "nckd_quotation3";

    /**
     * Type:java.util.Date,Name:报名开始时间
     */
    public static final String NCKD_REGSTARTTIME1 = "nckd_regstarttime1";

    /**
     * Type:java.util.Date,Name:报名结束时间
     */
    public static final String NCKD_REGENDTIME1 = "nckd_regendtime1";

    /**
     * Type:java.util.Date,Name:开标时间
     */
    public static final String NCKD_BIDOPENTIME = "nckd_bidopentime";

    /**
     * Type:String,Name:项目类型
     */
    public static final String NCKD_PROJECTTYPE3 = "nckd_projecttype3";

    /**
     * Type:String,Name:详细地址
     */
    public static final String NCKD_DETAILEDADDR3 = "nckd_detailedaddr3";

    /**
     * Type:String,Name:招标方式
     */
    public static final String NCKD_BIDDINGMETHOD = "nckd_biddingmethod";

    /**
     * Type:String,Name:发布方式
     */
    public static final String NCKD_PUBLISHINGMETHOD = "nckd_publishingmethod";

    /**
     * Type:String,Name:公开范围
     */
    public static final String NCKD_PUBLICSCOPE2 = "nckd_publicscope2";

    /**
     * Type:String,Name:报价时段查看供应商参与名单
     */
    public static final String NCKD_VIEWLIST2 = "nckd_viewlist2";

    /**
     * Type:String,Name:报名审核
     */
    public static final String NCKD_REGISTERAUDIT2 = "nckd_registeraudit2";

    /**
     * Type:String,Name:允许联合体报名
     */
    public static final String NCKD_ALLOWJOINT = "nckd_allowjoint";

    /**
     * Type:String,Name:在线开评标
     */
    public static final String NCKD_BIDONLINE = "nckd_bidonline";

    /**
     * Type:java.math.BigDecimal,Name:标书费（元）
     */
    public static final String NCKD_TENDERFEE1 = "nckd_tenderfee1";

    /**
     * Type:DynamicObject,Name:上传文件
     */
    public static final String NCKD_UPLOADFILE = "nckd_uploadfile";

    /**
     * Type:DynamicObject,Name:其他附件
     */
    public static final String NCKD_OTHERANNEXES = "nckd_otherannexes";

    /**
     * Type:java.math.BigDecimal,Name:保证金金额（元）
     */
    public static final String NCKD_MARGINAMOUNT = "nckd_marginamount";

    /**
     * Type:DynamicObject,sourceEntityId:bd_admindivision,Name:谈判地点
     */
    public static final String NCKD_NEGOTIATIONADDR = "nckd_negotiationaddr";

    /**
     * Type:DynamicObject,sourceEntityId:bd_admindivision,Name:项目地点
     */
    public static final String NCKD_PROJECTADDR = "nckd_projectaddr";

    /**
     * Type:DynamicObject,sourceEntityId:bd_admindivision,Name:招标地点
     */
    public static final String NCKD_TENDERADDR = "nckd_tenderaddr";

    /**
     * Type:String,Name:已推送采购单对应id
     */
    public static final String NCKD_PURCHASEID = "nckd_purchaseid";

    /**
     * Type:String,Name:公告内容
     */
    public static final String NCKD_BIGNOTICECONTENT = "nckd_bignoticecontent";

    /**
     * Type:String,Name:收货地址
     */
    public static final String NCKD_ADDRESS = "nckd_address";

    /**
     * Type:String,Name:已推送对应公告id
     */
    public static final String NCKD_NOTICEID = "nckd_noticeid";

    /**
     * 分录billhead_lk实体标识
     */
    public static final String ENTRYENTITYID_BILLHEAD_LK = "billhead_lk";

    /**
     * Type:java.lang.Long,Name:源单主实体编码
     */
    public static final String BILLHEAD_LK_BILLHEAD_LK_STABLEID = "billhead_lk_stableid";

    /**
     * Type:java.lang.Long,Name:源单内码
     */
    public static final String BILLHEAD_LK_BILLHEAD_LK_SBILLID = "billhead_lk_sbillid";

    /**
     * Type:java.lang.Long,Name:源单主实体内码
     */
    public static final String BILLHEAD_LK_BILLHEAD_LK_SID = "billhead_lk_sid";


    public static final String NCKD_NOTICECONTENT = "nckd_noticecontent";
    public static final String NCKD_DEPOSITENDTIME = "nckd_depositendtime";
    public static final String NCKD_INTERNALATTACHMENTS = "nckd_internalattachments";




    public static final String ALLPROPERTY = "id,billno,billstatus,creator,modifier,auditor,auditdate,modifytime,createtime,org,billtype,biztype,dept,bizuser,comment,closer,closedate,canceler,canceldate,billentry.id,billentry.material,billentry.auxpty,billentry.qty,billentry.unit,billentry.baseqty,billentry.baseunit,billentry.entrycomment,billentry.materialname,billentry.rowclosestatus,billentry.rowterminatestatus,billentry.materialmasterid,billentry.materialversion,billentry.auxunit,billentry.auxqty,billentry.entrycreator,billentry.entrycreatetime,billentry.entrymodifier,billentry.entrymodifytime,billentry.linetype,billentry.entryreqorg,billentry.deliverdate,billentry.purleadday,billentry.purdate,billentry.entryrecorg,billentry.entryoperatorgroup,billentry.entryoperator,billentry.supplier,billentry.price,billentry.taxrate,billentry.priceandtax,billentry.amount,billentry.amountandtax,billentry.entrypurog,billentry.reqdate,billentry.applyqty,billentry.entrypurdept,billentry.orderbaseqty,billentry.receivebaseqty,billentry.invbaseqty,billentry.orderqty,billentry.entryrecdept,billentry.entryreqdept,billentry.taxamount,billentry.receiveqty,billentry.invqty,billentry.taxrateid,billentry.project,billentry.joinqty,billentry.joinbaseqty,billentry.srcbillentity,billentry.srcbillid,billentry.srcbillnumber,billentry.srcbillentryid,billentry.srcbillentryseq,billentry.mftorderid,billentry.mftordernumber,billentry.mftorderentryid,billentry.mftorderentryseq,billentry.producttype,billentry.tracknumber,billentry.configuredcode,billentry.salbillid,billentry.salbillentryid,billentry.salbillentryseq,billentry.salbillnumber,billentry.avinvbaseqty,billentry.entrychangetype,billentry.purmethod,billentry.bomtime,billentry.ecostcenter,billentry.isredordermate,billentry.warehouse,closestatus,cancelstatus,biztime,billcretype,lastupdateuser,lastupdatetime,unitsrctype,totalallamount,currency,istax,inquirystatus,bidstatus,changestatus,changedate,changer,subversion,version,nckd_procurements,nckd_pushed,nckd_whetherpush,nckd_inquirylistname,nckd_projecttype,nckd_quotation,nckd_detailedaddr,nckd_publishmedia,nckd_quotationsign,nckd_quotationattsign,nckd_deadline,nckd_purchasetype,nckd_includestax,nckd_allowforpartial,nckd_quotationcurrency,nckd_controlprice,nckd_totalprice,nckd_mustupatt,nckd_inquirymethod,nckd_comparmethod,nckd_publicscope,nckd_viewlist,nckd_registeraudit,nckd_reviewmethod,nckd_whetherreviewol,nckd_internaldocuments,nckd_inquirydocument,nckd_remarks,nckd_announcementtitle,nckd_publishset,nckd_timingtime,nckd_disposalmethod,nckd_suppliers,nckd_invitationtitle,nckd_negotiatedname,nckd_negotiatednum,nckd_negotiatedbudget,nckd_regstarttime,nckd_subdeadtime,nckd_regendtime,nckd_projecttype1,nckd_disposalmethod1,nckd_quotation1,nckd_publishmedia1,nckd_negotiationmode,nckd_detailedaddr1,nckd_competitionmode,nckd_suppliers1,nckd_publicscope1,nckd_viewlist1,nckd_registeraudit1,nckd_reviewmethod1,nckd_whetherreviewol1,nckd_internaldocuments1,nckd_negotiatingdocuments,nckd_tenderfee,nckd_servicefee,nckd_projectname,nckd_projectamount,nckd_projecttype2,nckd_quotation2,nckd_detailedaddr2,nckd_procurementscopeatt,nckd_internaldocuments2,nckd_suppliers2,nckd_projectfiles,nckd_contenttitle,nckd_details,nckd_tendername,nckd_tenderestimate,nckd_bidmethod,nckd_quotation3,nckd_regstarttime1,nckd_regendtime1,nckd_bidopentime,nckd_projecttype3,nckd_detailedaddr3,nckd_biddingmethod,nckd_publishingmethod,nckd_publicscope2,nckd_viewlist2,nckd_registeraudit2,nckd_allowjoint,nckd_bidonline,nckd_tenderfee1,nckd_uploadfile,nckd_otherannexes,nckd_marginamount,nckd_negotiationaddr,nckd_projectaddr,nckd_tenderaddr,nckd_purchaseid,nckd_bignoticecontent,nckd_address,nckd_noticeid,billhead_lk.id,billhead_lk.billhead_lk_stableid,billhead_lk.billhead_lk_sbillid,billhead_lk.billhead_lk_sid,nckd_noticecontent";

}