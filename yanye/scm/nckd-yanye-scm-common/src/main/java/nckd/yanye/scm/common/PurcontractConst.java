package nckd.yanye.scm.common;

/**
 * Module           : 供应链云--合同管理模块--采购合同单据
 * Description      : 单据常量类
 * @date            : 2024-07-08
 * @author          : Generator
 * @version         : 1.0
 */
public class PurcontractConst {

	public static final String FORMBILLID = "conm_purcontract";
	public static final String ID = "id";

	/**
	 * Type:String,Name:合同编号
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
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:采购组织
	 */
	public static final String ORG = "org";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_billtype,Name:单据类型
	 */
	public static final String BILLTYPE = "billtype";

	/**
	 * Type:String,Name:合同名称
	 */
	public static final String BILLNAME = "billname";

	/**
	 * Type:String,Name:备注
	 */
	public static final String COMMENT = "comment";

	/**
	 * Type:String,Name:关闭状态
	 */
	public static final String CLOSESTATUS = "closestatus";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:关闭人
	 */
	public static final String CLOSER = "closer";

	/**
	 * Type:java.util.Date,Name:关闭日期
	 */
	public static final String CLOSEDATE = "closedate";

	/**
	 * Type:String,Name:作废状态
	 */
	public static final String CANCELSTATUS = "cancelstatus";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:作废人
	 */
	public static final String CANCELER = "canceler";

	/**
	 * Type:java.util.Date,Name:作废日期
	 */
	public static final String CANCELDATE = "canceldate";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:变更人
	 */
	public static final String CHANGER = "changer";

	/**
	 * Type:java.util.Date,Name:变更日期
	 */
	public static final String CHANGEDATE = "changedate";

	/**
	 * Type:String,Name:变更状态
	 */
	public static final String CHANGESTATUS = "changestatus";

	/**
	 * Type:String,Name:版本号
	 */
	public static final String VERSION = "version";

	/**
	 * Type:String,Name:生效状态
	 */
	public static final String VALIDSTATUS = "validstatus";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:生效人
	 */
	public static final String VALIDER = "valider";

	/**
	 * Type:java.util.Date,Name:生效日期
	 */
	public static final String VALIDDATE = "validdate";

	/**
	 * Type:java.util.Date,Name:冻结日期
	 */
	public static final String FREEZEDATE = "freezedate";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:冻结人
	 */
	public static final String FREEZER = "freezer";

	/**
	 * Type:String,Name:冻结状态
	 */
	public static final String FREEZESTATUS = "freezestatus";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:确认人
	 */
	public static final String CONFIRMER = "confirmer";

	/**
	 * Type:java.util.Date,Name:确认日期
	 */
	public static final String CONFIRMDATE = "confirmdate";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:合同甲方
	 */
	public static final String PARTA = "parta";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_bizpartner,Name:合同乙方
	 */
	public static final String PARTB = "partb";

	/**
	 * Type:DynamicObject,Name:其他方
	 */
	public static final String PARTOTHER = "partother";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_template,Name:合同模板
	 */
	public static final String TEMPLATE = "template";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_type,Name:合同类型
	 */
	public static final String TYPE = "type";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_currency,Name:结算币别
	 */
	public static final String SETTLECURRENCY = "settlecurrency";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_currency,Name:本位币
	 */
	public static final String CURRENCY = "currency";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_exratetable,Name:汇率表
	 */
	public static final String EXRATETABLE = "exratetable";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_settlementtype,Name:结算方式
	 */
	public static final String SETTLETYPE = "settletype";

	/**
	 * Type:boolean,Name:含税
	 */
	public static final String ISTAX = "istax";

	/**
	 * Type:java.math.BigDecimal,Name:税额
	 */
	public static final String TOTALTAXAMOUNT = "totaltaxamount";

	/**
	 * Type:java.math.BigDecimal,Name:金额
	 */
	public static final String TOTALAMOUNT = "totalamount";

	/**
	 * Type:java.math.BigDecimal,Name:价税合计
	 */
	public static final String TOTALALLAMOUNT = "totalallamount";

	/**
	 * Type:boolean,Name:基于清单
	 */
	public static final String ISONLIST = "isonlist";

	/**
	 * Type:boolean,Name:按比例(%)
	 */
	public static final String ISPAYRATE = "ispayrate";

	/**
	 * 分录payentry实体标识
	 */
	public static final String ENTRYENTITYID_PAYENTRY = "payentry";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_assistantdata_detail,Name:款项名称
	 */
	public static final String PAYENTRY_PAYNAME = "payname";

	/**
	 * Type:java.util.Date,Name:计划付款日期
	 */
	public static final String PAYENTRY_PAYDATE = "paydate";

	/**
	 * Type:java.math.BigDecimal,Name:付款比例(%)
	 */
	public static final String PAYENTRY_PAYRATE = "payrate";

	/**
	 * Type:java.math.BigDecimal,Name:付款金额
	 */
	public static final String PAYENTRY_PAYAMOUNT = "payamount";

	/**
	 * Type:boolean,Name:是否预付
	 */
	public static final String PAYENTRY_ISPREPAY = "isprepay";

	/**
	 * Type:java.math.BigDecimal,Name:关联付款金额
	 */
	public static final String PAYENTRY_JOINPAYAMOUNT = "joinpayamount";

	/**
	 * Type:java.math.BigDecimal,Name:已付金额
	 */
	public static final String PAYENTRY_PAIDAMOUNT = "paidamount";

	/**
	 * Type:String,Name:变更方式
	 */
	public static final String PAYENTRY_PAYENTRYCHANGETYPE = "payentrychangetype";

	/**
	 * Type:java.lang.Integer,Name:间隔时间
	 */
	public static final String PAYENTRY_INTERVALTIME = "intervaltime";

	/**
	 * Type:String,Name:时间单位
	 */
	public static final String PAYENTRY_TIMEUNIT = "timeunit";

	/**
	 * Type:String,Name:预付时点
	 */
	public static final String PAYENTRY_PRETIMEPOINT = "pretimepoint";

	/**
	 * 分录termentry实体标识
	 */
	public static final String ENTRYENTITYID_TERMENTRY = "termentry";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_termgroup,Name:分组
	 */
	public static final String TERMENTRY_TERMGROUP = "termgroup";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_term,Name:合同条款
	 */
	public static final String TERMENTRY_TERM = "term";

	/**
	 * Type:String,Name:变更方式
	 */
	public static final String TERMENTRY_TERMENTRYCHANGETYPE = "termentrychangetype";

	/**
	 * Type:String,Name:条款内容
	 */
	public static final String TERMENTRY_TERMCONTENT = "termcontent";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_category,Name:合同种类
	 */
	public static final String CATEGORY = "category";

	/**
	 * Type:String,Name:业务模式
	 */
	public static final String BIZMODE = "bizmode";

	/**
	 * Type:String,Name:合同属性
	 */
	public static final String CONMPROP = "conmprop";

	/**
	 * Type:boolean,Name:是否电子签章
	 */
	public static final String ISELECSIGNATURE = "iselecsignature";

	/**
	 * Type:String,Name:归档状态
	 */
	public static final String FILINGSTATUS = "filingstatus";

	/**
	 * Type:String,Name:评审状态
	 */
	public static final String REVIEWSTATUS = "reviewstatus";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:归档人
	 */
	public static final String FILINGER = "filinger";

	/**
	 * Type:java.util.Date,Name:归档日期
	 */
	public static final String FILINGDATE = "filingdate";

	/**
	 * Type:java.util.Date,Name:评审日期
	 */
	public static final String REVIEWDATE = "reviewdate";

	/**
	 * Type:String,Name:甲方
	 */
	public static final String PARTY1ST = "party1st";

	/**
	 * Type:String,Name:甲方联系人
	 */
	public static final String CONTACTPERSON1ST = "contactperson1st";

	/**
	 * Type:String,Name:甲方电话
	 */
	public static final String PHONE1ST = "phone1st";

	/**
	 * Type:String,Name:乙方
	 */
	public static final String PARTY2ND = "party2nd";

	/**
	 * Type:String,Name:乙方联系人
	 */
	public static final String CONTACTPERSON2ND = "contactperson2nd";

	/**
	 * Type:String,Name:乙方电话
	 */
	public static final String PHONE2ND = "phone2nd";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_contparties,Name:合同主体
	 */
	public static final String CONTPARTIES = "contparties";

	/**
	 * Type:java.util.Date,Name:签订日期
	 */
	public static final String BIZTIME = "biztime";

	/**
	 * Type:java.util.Date,Name:起始日期
	 */
	public static final String BIZTIMEBEGIN = "biztimebegin";

	/**
	 * Type:java.util.Date,Name:截止日期
	 */
	public static final String BIZTIMEEND = "biztimeend";

	/**
	 * Type:boolean,Name:明细金额汇总
	 */
	public static final String ISENTRYSUMAMT = "isentrysumamt";

	/**
	 * Type:String,Name:签章状态
	 */
	public static final String SIGNSTATUS = "signstatus";

	/**
	 * Type:java.util.Date,Name:签章日期
	 */
	public static final String SIGNDATE = "signdate";

	/**
	 * Type:String,Name:终止状态
	 */
	public static final String TERMINATESTATUS = "terminatestatus";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:终止人
	 */
	public static final String TERMINATOR = "terminator";

	/**
	 * Type:java.util.Date,Name:终止日期
	 */
	public static final String TERMINATEDATE = "terminatedate";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_bizpartner,Name:第三方
	 */
	public static final String PARTC = "partc";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:签章人
	 */
	public static final String SIGNER = "signer";

	/**
	 * Type:String,Name:单据生成类型
	 */
	public static final String BILLCRETYPE = "billcretype";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_tempfileentry,Name:模板版本
	 */
	public static final String TEMPLATEVERSION = "templateversion";

	/**
	 * Type:String,Name:子版本号
	 */
	public static final String SUBVERSION = "subversion";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
	 */
	public static final String LASTUPDATEUSER = "lastupdateuser";

	/**
	 * Type:java.util.Date,Name:修改时间
	 */
	public static final String LASTUPDATETIME = "lastupdatetime";

	/**
	 * 分录suitentry实体标识
	 */
	public static final String ENTRYENTITYID_SUITENTRY = "suitentry";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:组织编码
	 */
	public static final String SUITENTRY_SUITORG = "suitorg";

	/**
	 * Type:String,Name:变更方式
	 */
	public static final String SUITENTRY_SUITENTRYCHANGETYPE = "suitentrychangetype";

	/**
	 * Type:String,Name:适用组织范围
	 */
	public static final String SUITSCOPE = "suitscope";

	/**
	 * Type:String,Name:确认状态
	 */
	public static final String CONFIRMSTATUS = "confirmstatus";

	/**
	 * 分录billentry实体标识
	 */
	public static final String ENTRYENTITYID_BILLENTRY = "billentry";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:采购组织
	 */
	public static final String BILLENTRY_ENTRYPURORG = "entrypurorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_materialpurchaseinfo,Name:物料编码
	 */
	public static final String BILLENTRY_MATERIAL = "material";

	/**
	 * Type:String,Name:物料名称
	 */
	public static final String BILLENTRY_MATERIALNAME = "materialname";

	/**
	 * Type:DynamicObject,Name:辅助属性
	 */
	public static final String BILLENTRY_AUXPTY = "auxpty";

	/**
	 * Type:java.math.BigDecimal,Name:数量
	 */
	public static final String BILLENTRY_QTY = "qty";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:计量单位
	 */
	public static final String BILLENTRY_UNIT = "unit";

	/**
	 * Type:boolean,Name:赠品
	 */
	public static final String BILLENTRY_ISPRESENT = "ispresent";

	/**
	 * Type:java.math.BigDecimal,Name:单价
	 */
	public static final String BILLENTRY_PRICE = "price";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_taxrate,Name:税率
	 */
	public static final String BILLENTRY_TAXRATEID = "taxrateid";

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
	 * Type:java.math.BigDecimal,Name:税额
	 */
	public static final String BILLENTRY_TAXAMOUNT = "taxamount";

	/**
	 * Type:String,Name:折扣方式
	 */
	public static final String BILLENTRY_DISCOUNTTYPE = "discounttype";

	/**
	 * Type:java.math.BigDecimal,Name:单位折扣(率)
	 */
	public static final String BILLENTRY_DISCOUNTRATE = "discountrate";

	/**
	 * Type:java.math.BigDecimal,Name:折扣额
	 */
	public static final String BILLENTRY_DISCOUNTAMOUNT = "discountamount";

	/**
	 * Type:java.math.BigDecimal,Name:价税合计
	 */
	public static final String BILLENTRY_AMOUNTANDTAX = "amountandtax";

	/**
	 * Type:java.math.BigDecimal,Name:金额(本位币)
	 */
	public static final String BILLENTRY_CURAMOUNT = "curamount";

	/**
	 * Type:java.math.BigDecimal,Name:税额(本位币)
	 */
	public static final String BILLENTRY_CURTAXAMOUNT = "curtaxamount";

	/**
	 * Type:java.math.BigDecimal,Name:价税合计(本位币)
	 */
	public static final String BILLENTRY_CURAMOUNTANDTAX = "curamountandtax";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_project,Name:项目
	 */
	public static final String BILLENTRY_PROJECT = "project";

	/**
	 * Type:String,Name:行关闭状态
	 */
	public static final String BILLENTRY_ROWCLOSESTATUS = "rowclosestatus";

	/**
	 * Type:String,Name:行终止状态
	 */
	public static final String BILLENTRY_ROWTERMINATESTATUS = "rowterminatestatus";

	/**
	 * Type:String,Name:备注
	 */
	public static final String BILLENTRY_ENTRYCOMMENT = "entrycomment";

	/**
	 * Type:String,Name:变更方式
	 */
	public static final String BILLENTRY_BILLENTRYCHANGETYPE = "billentrychangetype";

	/**
	 * Type:java.math.BigDecimal,Name:基本数量
	 */
	public static final String BILLENTRY_BASEQTY = "baseqty";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:基本单位
	 */
	public static final String BILLENTRY_BASEUNIT = "baseunit";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_material,Name:主物料(废弃)1
	 */
	public static final String BILLENTRY_MATERIALMASTERID = "materialmasterid";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_materialversion,Name:物料版本
	 */
	public static final String BILLENTRY_MATERIALVERSION = "materialversion";

	/**
	 * Type:java.math.BigDecimal,Name:辅助数量
	 */
	public static final String BILLENTRY_AUXQTY = "auxqty";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:辅助单位
	 */
	public static final String BILLENTRY_AUXUNIT = "auxunit";

	/**
	 * Type:String,Name:来源单据实体
	 */
	public static final String BILLENTRY_SRCBILLENTITY = "srcbillentity";

	/**
	 * Type:java.lang.Long,Name:来源单据ID
	 */
	public static final String BILLENTRY_SRCBILLID = "srcbillid";

	/**
	 * Type:java.lang.Long,Name:来源单据行ID
	 */
	public static final String BILLENTRY_SRCBILLENTRYID = "srcbillentryid";

	/**
	 * Type:java.lang.Long,Name:来源单据分录序号
	 */
	public static final String BILLENTRY_SRCBILLENTRYSEQ = "srcbillentryseq";

	/**
	 * Type:String,Name:来源单据编号
	 */
	public static final String BILLENTRY_SRCBILLNUMBER = "srcbillnumber";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:结算组织
	 */
	public static final String BILLENTRY_ENTRYSETTLEORG = "entrysettleorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:需求组织
	 */
	public static final String BILLENTRY_ENTRYREQORG = "entryreqorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:收货组织
	 */
	public static final String BILLENTRY_ENTRYINVORG = "entryinvorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_warehouse,Name:收货仓库
	 */
	public static final String BILLENTRY_WAREHOUSE = "warehouse";

	/**
	 * Type:java.util.Date,Name:交货日期
	 */
	public static final String BILLENTRY_DELIVERDATE = "deliverdate";

	/**
	 * Type:String,Name:交货地址
	 */
	public static final String BILLENTRY_DELIVERADDRESS = "deliveraddress";

	/**
	 * Type:java.math.BigDecimal,Name:已订货数量
	 */
	public static final String BILLENTRY_ORDERQTY = "orderqty";

	/**
	 * Type:java.math.BigDecimal,Name:已订货基本数量
	 */
	public static final String BILLENTRY_ORDERBASEQTY = "orderbaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:关联订货数量
	 */
	public static final String BILLENTRY_JOINORDERQTY = "joinorderqty";

	/**
	 * Type:java.math.BigDecimal,Name:关联订货基本数量
	 */
	public static final String BILLENTRY_JOINORDERBASEQTY = "joinorderbaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:关联应付数量
	 */
	public static final String BILLENTRY_JOINPAYABLEPRICEQTY = "joinpayablepriceqty";

	/**
	 * Type:java.math.BigDecimal,Name:应付数量
	 */
	public static final String BILLENTRY_PAYABLEPRICEQTY = "payablepriceqty";

	/**
	 * Type:java.math.BigDecimal,Name:应付金额
	 */
	public static final String BILLENTRY_PAYABLEAMOUNT = "payableamount";

	/**
	 * Type:java.math.BigDecimal,Name:关联应付基本数量
	 */
	public static final String BILLENTRY_JOINPAYABLEBASEQTY = "joinpayablebaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:应付基本数量
	 */
	public static final String BILLENTRY_PAYABLEBASEQTY = "payablebaseqty";

	/**
	 * Type:String,Name:行号
	 */
	public static final String BILLENTRY_LINENO = "lineno";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_costcenter,Name:成本中心
	 */
	public static final String BILLENTRY_ECOSTCENTER = "ecostcenter";

	/**
	 * Type:boolean,Name:含税单价来自招采平台推送
	 */
	public static final String BILLENTRY_NCKD_TOPUSH = "nckd_topush";

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
	 * Type:java.math.BigDecimal,Name:关联订货数量_原始携带值
	 */
	public static final String BILLENTRY_LK_BILLENTRY_LK_JOINORDERQTY_OLD = "billentry_lk_joinorderqty_old";

	/**
	 * Type:java.math.BigDecimal,Name:关联订货数量_确认携带值
	 */
	public static final String BILLENTRY_LK_BILLENTRY_LK_JOINORDERQTY = "billentry_lk_joinorderqty";

	/**
	 * Type:java.math.BigDecimal,Name:关联订货基本数量_原始携带值
	 */
	public static final String BILLENTRY_LK_BILLENTRY_LK_JOINORDERBASEQTY_OLD = "billentry_lk_joinorderbaseqty_old";

	/**
	 * Type:java.math.BigDecimal,Name:关联订货基本数量_确认携带值
	 */
	public static final String BILLENTRY_LK_BILLENTRY_LK_JOINORDERBASEQTY = "billentry_lk_joinorderbaseqty";

	/**
	 * Type:java.util.Date,Name:汇率日期
	 */
	public static final String EXRATEDATE = "exratedate";

	/**
	 * Type:String,Name:来源合同编号
	 */
	public static final String FRAMENUM = "framenum";

	/**
	 * Type:String,Name:来源合同版本
	 */
	public static final String FRAMEVERSION = "frameversion";

	/**
	 * Type:String,Name:来源合同名称
	 */
	public static final String FRAMENAME = "framename";

	/**
	 * Type:java.math.BigDecimal,Name:汇率
	 */
	public static final String EXCHANGERATE = "exchangerate";

	/**
	 * Type:String,Name:换算方式
	 */
	public static final String EXCHANGETYPE = "exchangetype";

	/**
	 * Type:String,Name:计量单位来源
	 */
	public static final String UNITSRCTYPE = "unitsrctype";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_operator,Name:采购员
	 */
	public static final String OPERATOR = "operator";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:采购部门
	 */
	public static final String DEPT = "dept";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_operatorgroup,Name:采购组
	 */
	public static final String OPERATORGROUP = "operatorgroup";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:订货供应商
	 */
	public static final String SUPPLIER = "supplier";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:供货供应商
	 */
	public static final String PROVIDERSUPPLIER = "providersupplier";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplierlinkman,Name:供货联系人
	 */
	public static final String PROVIDERLINKMAN = "providerlinkman";

	/**
	 * Type:String,Name:供货联系地址
	 */
	public static final String PROVIDERADDRESS = "provideraddress";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:结算供应商
	 */
	public static final String INVOICESUPPLIER = "invoicesupplier";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:收款供应商
	 */
	public static final String RECEIVESUPPLIER = "receivesupplier";

	/**
	 * Type:java.math.BigDecimal,Name:已预付金额
	 */
	public static final String PAIDPREALLAMOUNT = "paidpreallamount";

	/**
	 * Type:java.math.BigDecimal,Name:已付金额
	 */
	public static final String PAIDALLAMOUNT = "paidallamount";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_address,Name:供货联系地址F7
	 */
	public static final String PROVIDERADDRESSF7 = "provideraddressf7";

	/**
	 * Type:java.math.BigDecimal,Name:已订货金额
	 */
	public static final String ORDERALLAMOUNT = "orderallamount";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_invoicebiztype,Name:发票类别
	 */
	public static final String INVOICEBIZTYPE = "invoicebiztype";

	/**
	 * 分录attachmententry实体标识
	 */
	public static final String ENTRYENTITYID_ATTACHMENTENTRY = "attachmententry";

	/**
	 * Type:DynamicObject,Name:附件
	 */
	public static final String ATTACHMENTENTRY_ATTACHMENT = "attachment";

	/**
	 * Type:String,Name:附件名称
	 */
	public static final String ATTACHMENTENTRY_ATTACHNAME = "attachname";

	/**
	 * Type:String,Name:附件来源
	 */
	public static final String ATTACHMENTENTRY_ATTACHSOURCE = "attachsource";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:上传人
	 */
	public static final String ATTACHMENTENTRY_UPLOADER = "uploader";

	/**
	 * Type:java.util.Date,Name:上传时间
	 */
	public static final String ATTACHMENTENTRY_UPLOADTIME = "uploadtime";

	/**
	 * Type:java.lang.Long,Name:行标记
	 */
	public static final String ATTACHMENTENTRY_MAPPINGID = "mappingid";

	/**
	 * Type:String,Name:备注
	 */
	public static final String ATTACHMENTENTRY_ATTACHCOMMENT = "attachcomment";

	/**
	 * Type:boolean,Name:磋商记录
	 */
	public static final String ATTACHMENTENTRY_ISCONSULTRECORD = "isconsultrecord";

	/**
	 * Type:boolean,Name:续签合同
	 */
	public static final String ISRENEW = "isrenew";

	/**
	 * Type:boolean,Name:启用协同磋商
	 */
	public static final String ISCOLLACONSULT = "iscollaconsult";

	/**
	 * Type:java.lang.Integer,Name:附件数
	 */
	public static final String ATTCHMENTCOUNT = "attchmentcount";

	/**
	 * Type:boolean,Name:允许超额付款
	 */
	public static final String ISALLOWOVERPAY = "isallowoverpay";

	/**
	 * Type:java.lang.Long,Name:合同台账来源id
	 */
	public static final String LEDGERID = "ledgerid";

	/**
	 * Type:java.math.BigDecimal,Name:已收合同履约金
	 */
	public static final String NCKD_YSHTLLYJ = "nckd_yshtllyj";

	/**
	 * Type:java.math.BigDecimal,Name:合同履约金
	 */
	public static final String NCKD_AMOUNTFIELD = "nckd_amountfield";

	/**
	 * Type:java.math.BigDecimal,Name:招采成交价税合计
	 */
	public static final String NCKD_TOTALPRICE = "nckd_totalprice";

	/**
	 * Type:String,Name:比质比价结果
	 */
	public static final String NCKD_PRICECOMPRESULT = "nckd_pricecompresult";

	/**
	 * 分录nckd_priceinfo实体标识
	 */
	public static final String ENTRYENTITYID_NCKD_PRICEINFO = "nckd_priceinfo";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
	 */
	public static final String NCKD_PRICEINFO_NCKD_MODIFIERFIELD = "nckd_modifierfield";

	/**
	 * Type:java.util.Date,Name:修改时间
	 */
	public static final String NCKD_PRICEINFO_NCKD_MODIFYDATEFIELD = "nckd_modifydatefield";

	/**
	 * Type:String,Name:供应商名称
	 */
	public static final String NCKD_PRICEINFO_NCKD_SUPPLIERNAME = "nckd_suppliername";

	/**
	 * Type:java.math.BigDecimal,Name:报价（元）
	 */
	public static final String NCKD_PRICEINFO_NCKD_QUOTATION = "nckd_quotation";

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

	/**
	 * 上游信息接收单
	 */
	public static final String NCKD_UPINFORECEIVEBILL = "nckd_upinforeceivebill";

	public static final String ALLPROPERTY = "id,billno,billstatus,creator,modifier,auditor,auditdate,modifytime,createtime,org,billtype,billname,comment,closestatus,closer,closedate,cancelstatus,canceler,canceldate,changer,changedate,changestatus,version,validstatus,valider,validdate,freezedate,freezer,freezestatus,confirmer,confirmdate,parta,partb,partother,template,type,settlecurrency,currency,exratetable,settletype,istax,totaltaxamount,totalamount,totalallamount,isonlist,ispayrate,payentry.id,payentry.payname,payentry.paydate,payentry.payrate,payentry.payamount,payentry.isprepay,payentry.joinpayamount,payentry.paidamount,payentry.payentrychangetype,payentry.intervaltime,payentry.timeunit,payentry.pretimepoint,termentry.id,termentry.termgroup,termentry.term,termentry.termentrychangetype,termentry.termcontent,category,bizmode,conmprop,iselecsignature,filingstatus,reviewstatus,filinger,filingdate,reviewdate,party1st,contactperson1st,phone1st,party2nd,contactperson2nd,phone2nd,contparties,biztime,biztimebegin,biztimeend,isentrysumamt,signstatus,signdate,terminatestatus,terminator,terminatedate,partc,signer,billcretype,templateversion,subversion,lastupdateuser,lastupdatetime,suitentry.id,suitentry.suitorg,suitentry.suitentrychangetype,suitscope,confirmstatus,billentry.id,billentry.entrypurorg,billentry.material,billentry.materialname,billentry.auxpty,billentry.qty,billentry.unit,billentry.ispresent,billentry.price,billentry.taxrateid,billentry.taxrate,billentry.priceandtax,billentry.amount,billentry.taxamount,billentry.discounttype,billentry.discountrate,billentry.discountamount,billentry.amountandtax,billentry.curamount,billentry.curtaxamount,billentry.curamountandtax,billentry.project,billentry.rowclosestatus,billentry.rowterminatestatus,billentry.entrycomment,billentry.billentrychangetype,billentry.baseqty,billentry.baseunit,billentry.materialmasterid,billentry.materialversion,billentry.auxqty,billentry.auxunit,billentry.srcbillentity,billentry.srcbillid,billentry.srcbillentryid,billentry.srcbillentryseq,billentry.srcbillnumber,billentry.entrysettleorg,billentry.entryreqorg,billentry.entryinvorg,billentry.warehouse,billentry.deliverdate,billentry.deliveraddress,billentry.orderqty,billentry.orderbaseqty,billentry.joinorderqty,billentry.joinorderbaseqty,billentry.joinpayablepriceqty,billentry.payablepriceqty,billentry.payableamount,billentry.joinpayablebaseqty,billentry.payablebaseqty,billentry.lineno,billentry.ecostcenter,billentry.nckd_topush,exratedate,framenum,frameversion,framename,exchangerate,exchangetype,unitsrctype,operator,dept,operatorgroup,supplier,providersupplier,providerlinkman,provideraddress,invoicesupplier,receivesupplier,paidpreallamount,paidallamount,provideraddressf7,orderallamount,invoicebiztype,attachmententry.id,attachmententry.attachment,attachmententry.attachname,attachmententry.attachsource,attachmententry.uploader,attachmententry.uploadtime,attachmententry.mappingid,attachmententry.attachcomment,attachmententry.isconsultrecord,isrenew,iscollaconsult,attchmentcount,isallowoverpay,ledgerid,nckd_yshtllyj,nckd_amountfield,nckd_totalprice,nckd_pricecompresult,nckd_priceinfo.id,nckd_priceinfo.nckd_modifierfield,nckd_priceinfo.nckd_modifydatefield,nckd_priceinfo.nckd_suppliername,nckd_priceinfo.nckd_quotation,billhead_lk.id,billhead_lk.billhead_lk_stableid,billhead_lk.billhead_lk_sbillid,billhead_lk.billhead_lk_sid,nckd_upinforeceivebill";

}