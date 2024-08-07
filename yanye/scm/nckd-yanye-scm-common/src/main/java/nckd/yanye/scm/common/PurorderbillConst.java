package nckd.yanye.scm.common;

/**
 * Module           : 供应链云--采购管理模块--采购订单单据
 * Description      : 单据常量类
 * @date            : 2024-08-07
 * @author          : Generator
 * @version         : 1.0
 */
public class PurorderbillConst {

	public static final String FORMBILLID = "pm_purorderbill";
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
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:采购组织
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
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:分录采购组织(封存)
	 */
	public static final String BILLENTRY_ENTRYPURORG = "entrypurorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_materialpurchaseinfo,Name:物料编码
	 */
	public static final String BILLENTRY_MATERIAL = "material";

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
	 * Type:String,Name:备注
	 */
	public static final String BILLENTRY_ENTRYCOMMENT = "entrycomment";

	/**
	 * Type:java.math.BigDecimal,Name:基本数量
	 */
	public static final String BILLENTRY_BASEQTY = "baseqty";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:基本单位
	 */
	public static final String BILLENTRY_BASEUNIT = "baseunit";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_project,Name:项目
	 */
	public static final String BILLENTRY_PROJECT = "project";

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
	 * Type:java.math.BigDecimal,Name:单价
	 */
	public static final String BILLENTRY_PRICE = "price";

	/**
	 * Type:java.math.BigDecimal,Name:含税单价
	 */
	public static final String BILLENTRY_PRICEANDTAX = "priceandtax";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_taxrate,Name:税率
	 */
	public static final String BILLENTRY_TAXRATEID = "taxrateid";

	/**
	 * Type:java.math.BigDecimal,Name:税率(%)
	 */
	public static final String BILLENTRY_TAXRATE = "taxrate";

	/**
	 * Type:String,Name:折扣方式
	 */
	public static final String BILLENTRY_DISCOUNTTYPE = "discounttype";

	/**
	 * Type:java.math.BigDecimal,Name:单位折扣(率)
	 */
	public static final String BILLENTRY_DISCOUNTRATE = "discountrate";

	/**
	 * Type:java.math.BigDecimal,Name:金额
	 */
	public static final String BILLENTRY_AMOUNT = "amount";

	/**
	 * Type:java.math.BigDecimal,Name:金额(本位币)
	 */
	public static final String BILLENTRY_CURAMOUNT = "curamount";

	/**
	 * Type:java.math.BigDecimal,Name:税额
	 */
	public static final String BILLENTRY_TAXAMOUNT = "taxamount";

	/**
	 * Type:java.math.BigDecimal,Name:税额(本位币)
	 */
	public static final String BILLENTRY_CURTAXAMOUNT = "curtaxamount";

	/**
	 * Type:java.math.BigDecimal,Name:折扣额
	 */
	public static final String BILLENTRY_DISCOUNTAMOUNT = "discountamount";

	/**
	 * Type:java.math.BigDecimal,Name:价税合计
	 */
	public static final String BILLENTRY_AMOUNTANDTAX = "amountandtax";

	/**
	 * Type:java.math.BigDecimal,Name:价税合计(本位币)
	 */
	public static final String BILLENTRY_CURAMOUNTANDTAX = "curamountandtax";

	/**
	 * Type:String,Name:变更方式
	 */
	public static final String BILLENTRY_ENTRYCHANGETYPE = "entrychangetype";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_material,Name:主物料(封存)
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
	 * Type:java.lang.Long,Name:来源单据行ID
	 */
	public static final String BILLENTRY_SRCBILLENTRYID = "srcbillentryid";

	/**
	 * Type:java.lang.Long,Name:来源单据ID
	 */
	public static final String BILLENTRY_SRCBILLID = "srcbillid";

	/**
	 * Type:String,Name:来源单据编号
	 */
	public static final String BILLENTRY_SRCBILLNUMBER = "srcbillnumber";

	/**
	 * Type:java.lang.Long,Name:来源单据分录序号
	 */
	public static final String BILLENTRY_SRCBILLENTRYSEQ = "srcbillentryseq";

	/**
	 * Type:String,Name:核心单据实体
	 */
	public static final String BILLENTRY_MAINBILLENTITY = "mainbillentity";

	/**
	 * Type:String,Name:核心单据编号
	 */
	public static final String BILLENTRY_MAINBILLNUMBER = "mainbillnumber";

	/**
	 * Type:java.lang.Long,Name:核心单据ID
	 */
	public static final String BILLENTRY_MAINBILLID = "mainbillid";

	/**
	 * Type:java.lang.Long,Name:核心单据行ID
	 */
	public static final String BILLENTRY_MAINBILLENTRYID = "mainbillentryid";

	/**
	 * Type:java.lang.Long,Name:核心单据分录序号
	 */
	public static final String BILLENTRY_MAINBILLENTRYSEQ = "mainbillentryseq";

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
	 * Type:DynamicObject,sourceEntityId:bd_wbs,Name:WBS
	 */
	public static final String BILLENTRY_WBS = "wbs";

	/**
	 * Type:java.util.Date,Name:交货日期
	 */
	public static final String BILLENTRY_DELIVERDATE = "deliverdate";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:收货组织
	 */
	public static final String BILLENTRY_ENTRYRECORG = "entryrecorg";

	/**
	 * Type:String,Name:交货地址
	 */
	public static final String BILLENTRY_DELIVERADDRESS = "deliveraddress";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_warehouse,Name:收货仓库
	 */
	public static final String BILLENTRY_WAREHOUSE = "warehouse";

	/**
	 * Type:boolean,Name:赠品
	 */
	public static final String BILLENTRY_ISPRESENT = "ispresent";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:收货部门
	 */
	public static final String BILLENTRY_ENTRYRECDEPT = "entryrecdept";

	/**
	 * 子分录purbillentry_deliver实体标识
	 */
	public static final String SUBENTRYENTITYID_PURBILLENTRY_DELIVER = "purbillentry_deliver";

	/**
	 * Type:java.util.Date,Name:交货日期
	 */
	public static final String PURBILLENTRY_DELIVER_PLANDELIVERDATE = "plandeliverdate";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_measureunits,Name:计量单位
	 */
	public static final String PURBILLENTRY_DELIVER_PLANUNIT = "planunit";

	/**
	 * Type:java.math.BigDecimal,Name:计划交货数量
	 */
	public static final String PURBILLENTRY_DELIVER_PLANQTY = "planqty";

	/**
	 * Type:String,Name:备注
	 */
	public static final String PURBILLENTRY_DELIVER_PLANCOMMENT = "plancomment";

	/**
	 * Type:java.util.Date,Name:收货日期
	 */
	public static final String PURBILLENTRY_DELIVER_PLANRECEIVEDATE = "planreceivedate";

	/**
	 * Type:java.math.BigDecimal,Name:收货数量
	 */
	public static final String PURBILLENTRY_DELIVER_PLANRECEIVEQTY = "planreceiveqty";

	/**
	 * Type:String,Name:变更方式
	 */
	public static final String PURBILLENTRY_DELIVER_DELENTRYCHANGETYPE = "delentrychangetype";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
	 */
	public static final String PURBILLENTRY_DELIVER_DELENTRYMODIFIER = "delentrymodifier";

	/**
	 * Type:java.util.Date,Name:修改时间
	 */
	public static final String PURBILLENTRY_DELIVER_DELENTRYMODIFYTIME = "delentrymodifytime";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:创建人
	 */
	public static final String PURBILLENTRY_DELIVER_DELENTRYCREATOR = "delentrycreator";

	/**
	 * Type:java.util.Date,Name:创建时间
	 */
	public static final String PURBILLENTRY_DELIVER_DELENTRYCREATETIME = "delentrycreatetime";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:需求组织
	 */
	public static final String BILLENTRY_ENTRYREQORG = "entryreqorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:需求部门
	 */
	public static final String BILLENTRY_ENTRYREQDEPT = "entryreqdept";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:结算组织
	 */
	public static final String BILLENTRY_ENTRYSETTLEORG = "entrysettleorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:结算部门
	 */
	public static final String BILLENTRY_ENTRYSETTLEDEPT = "entrysettledept";

	/**
	 * Type:java.math.BigDecimal,Name:已收货基本数量
	 */
	public static final String BILLENTRY_RECEIVEBASEQTY = "receivebaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:已入库基本数量
	 */
	public static final String BILLENTRY_INVBASEQTY = "invbaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:已退库基本数量
	 */
	public static final String BILLENTRY_RETURNBASEQTY = "returnbaseqty";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_admindivision,Name:交货地点
	 */
	public static final String BILLENTRY_DELIVERLOCATION = "deliverlocation";

	/**
	 * Type:java.math.BigDecimal,Name:已收货数量
	 */
	public static final String BILLENTRY_RECEIVEQTY = "receiveqty";

	/**
	 * Type:java.math.BigDecimal,Name:已入库数量
	 */
	public static final String BILLENTRY_INVQTY = "invqty";

	/**
	 * Type:java.math.BigDecimal,Name:已退库数量
	 */
	public static final String BILLENTRY_RETURNQTY = "returnqty";

	/**
	 * Type:java.math.BigDecimal,Name:应付数量
	 */
	public static final String BILLENTRY_PAYABLEPRICEQTY = "payablepriceqty";

	/**
	 * Type:String,Name:供应商批号
	 */
	public static final String BILLENTRY_SUPPLIERLOT = "supplierlot";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:付款组织
	 */
	public static final String BILLENTRY_ENTRYPAYORG = "entrypayorg";

	/**
	 * Type:java.math.BigDecimal,Name:关联数量
	 */
	public static final String BILLENTRY_JOINQTY = "joinqty";

	/**
	 * Type:java.math.BigDecimal,Name:关联基本数量
	 */
	public static final String BILLENTRY_JOINBASEQTY = "joinbaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:收货可退数量
	 */
	public static final String BILLENTRY_RECRETQTY = "recretqty";

	/**
	 * Type:java.math.BigDecimal,Name:库存可退数量
	 */
	public static final String BILLENTRY_INVRETQTY = "invretqty";

	/**
	 * Type:java.math.BigDecimal,Name:库存可退基本数量
	 */
	public static final String BILLENTRY_INVRETBASEQTY = "invretbaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:收货可退基本数量
	 */
	public static final String BILLENTRY_RECRETBASEQTY = "recretbaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:关联验收应付数量
	 */
	public static final String BILLENTRY_JOINPAYABLEPRICEQTY = "joinpayablepriceqty";

	/**
	 * Type:String,Name:销售订单编号
	 */
	public static final String BILLENTRY_SALBILLNUMBER = "salbillnumber";

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
	 * Type:boolean,Name:控制收货数量
	 */
	public static final String BILLENTRY_ISCONTROLQTY = "iscontrolqty";

	/**
	 * Type:java.math.BigDecimal,Name:收货超收比率(%)
	 */
	public static final String BILLENTRY_RECEIVERATEUP = "receiverateup";

	/**
	 * Type:java.math.BigDecimal,Name:收货欠收比率(%)
	 */
	public static final String BILLENTRY_RECEIVERATEDOWN = "receiveratedown";

	/**
	 * Type:java.math.BigDecimal,Name:收货下限数量
	 */
	public static final String BILLENTRY_RECEIVEQTYDOWN = "receiveqtydown";

	/**
	 * Type:java.math.BigDecimal,Name:收货上限数量
	 */
	public static final String BILLENTRY_RECEIVEQTYUP = "receiveqtyup";

	/**
	 * Type:java.math.BigDecimal,Name:收货下限基本数量
	 */
	public static final String BILLENTRY_RECEIVEBASEQTYDOWN = "receivebaseqtydown";

	/**
	 * Type:java.math.BigDecimal,Name:收货上限基本数量
	 */
	public static final String BILLENTRY_RECEIVEBASEQTYUP = "receivebaseqtyup";

	/**
	 * Type:String,Name:货主类型
	 */
	public static final String BILLENTRY_OWNERTYPE = "ownertype";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:货主
	 */
	public static final String BILLENTRY_OWNER = "owner";

	/**
	 * Type:DynamicObject,sourceEntityId:er_expenseitemedit,Name:费用项目
	 */
	public static final String BILLENTRY_EXPENSEITEM = "expenseitem";

	/**
	 * Type:java.math.BigDecimal,Name:应付金额
	 */
	public static final String BILLENTRY_PAYABLEAMOUNT = "payableamount";

	/**
	 * Type:boolean,Name:控制时间
	 */
	public static final String BILLENTRY_ISCONTROLDAY = "iscontrolday";

	/**
	 * Type:java.lang.Integer,Name:允许提前天数
	 */
	public static final String BILLENTRY_RECEIVEDAYUP = "receivedayup";

	/**
	 * Type:java.lang.Integer,Name:允许延迟天数
	 */
	public static final String BILLENTRY_RECEIVEDAYDOWN = "receivedaydown";

	/**
	 * Type:String,Name:来源系统单据分录ID
	 */
	public static final String BILLENTRY_SRCSYSBILLENTRYID = "srcsysbillentryid";

	/**
	 * Type:String,Name:来源系统
	 */
	public static final String BILLENTRY_SRCSYSTEM = "srcsystem";

	/**
	 * Type:String,Name:来源系统单据ID
	 */
	public static final String BILLENTRY_SRCSYSBILLID = "srcsysbillid";

	/**
	 * Type:String,Name:来源系统编号
	 */
	public static final String BILLENTRY_SRCSYSBILLNO = "srcsysbillno";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_entityobject,Name:合同实体
	 */
	public static final String BILLENTRY_CONBILLENTITY = "conbillentity";

	/**
	 * Type:String,Name:合同编号
	 */
	public static final String BILLENTRY_CONBILLNUMBER = "conbillnumber";

	/**
	 * Type:String,Name:合同行号
	 */
	public static final String BILLENTRY_CONBILLROWNUM = "conbillrownum";

	/**
	 * Type:String,Name:合同分录序号
	 */
	public static final String BILLENTRY_CONBILLENTRYSEQ = "conbillentryseq";

	/**
	 * Type:java.lang.Long,Name:合同ID
	 */
	public static final String BILLENTRY_CONBILLID = "conbillid";

	/**
	 * Type:java.lang.Long,Name:合同行ID
	 */
	public static final String BILLENTRY_CONBILLENTRYID = "conbillentryid";

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
	 * Type:java.math.BigDecimal,Name:已退验数量
	 */
	public static final String BILLENTRY_RETURNRECEIPTQTY = "returnreceiptqty";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_configuredcode,Name:配置号
	 */
	public static final String BILLENTRY_CONFIGUREDCODE = "configuredcode";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_tracknumber,Name:跟踪号
	 */
	public static final String BILLENTRY_TRACKNUMBER = "tracknumber";

	/**
	 * Type:java.util.Date,Name:承诺日期
	 */
	public static final String BILLENTRY_PROMISEDATE = "promisedate";

	/**
	 * Type:String,Name:协同单据编号
	 */
	public static final String BILLENTRY_SOUBILLNUMBER = "soubillnumber";

	/**
	 * Type:java.lang.Long,Name:协同单据ID
	 */
	public static final String BILLENTRY_SOUBILLID = "soubillid";

	/**
	 * Type:java.lang.Long,Name:协同单据行ID
	 */
	public static final String BILLENTRY_SOUBILLENTRYID = "soubillentryid";

	/**
	 * Type:String,Name:协同单据实体
	 */
	public static final String BILLENTRY_SOUBILLENTITY = "soubillentity";

	/**
	 * Type:java.lang.Long,Name:协同单据分录序号
	 */
	public static final String BILLENTRY_SOUBILLENTRYSEQ = "soubillentryseq";

	/**
	 * Type:java.math.BigDecimal,Name:应付基本数量
	 */
	public static final String BILLENTRY_PAYABLEBASEQTY = "payablebaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:已退验基本数量
	 */
	public static final String BILLENTRY_RETURNRECEIPTBASEQTY = "returnreceiptbaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:关联验收应付基本数量
	 */
	public static final String BILLENTRY_JOINPAYABLEBASEQTY = "joinpayablebaseqty";

	/**
	 * Type:java.math.BigDecimal,Name:已收货通知数量
	 */
	public static final String BILLENTRY_RECEIPTNOTICEQTY = "receiptnoticeqty";

	/**
	 * Type:java.math.BigDecimal,Name:已收货通知基本数量
	 */
	public static final String BILLENTRY_RECEIPTNOTICBASEQTY = "receiptnoticbaseqty";

	/**
	 * Type:boolean,Name:控制上限金额
	 */
	public static final String BILLENTRY_ISCONTROLAMOUNTUP = "iscontrolamountup";

	/**
	 * Type:java.math.BigDecimal,Name:上限金额
	 */
	public static final String BILLENTRY_AMOUNTUP = "amountup";

	/**
	 * Type:java.math.BigDecimal,Name:关联金额
	 */
	public static final String BILLENTRY_JOINAMOUNT = "joinamount";

	/**
	 * Type:java.math.BigDecimal,Name:执行金额
	 */
	public static final String BILLENTRY_PERFORMAMOUNT = "performamount";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_costcenter,Name:成本中心
	 */
	public static final String BILLENTRY_ECOSTCENTER = "ecostcenter";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_adminorg,Name:申请部门
	 */
	public static final String BILLENTRY_NCKD_NCKD_SQBM = "nckd_nckd_sqbm";

	/**
	 * Type:String,Name:车牌/车型
	 */
	public static final String BILLENTRY_NCKD_CPCX = "nckd_cpcx";

	/**
	 * Type:String,Name:用人单位人员名称
	 */
	public static final String BILLENTRY_NCKD_RYMC = "nckd_rymc";

	/**
	 * Type:java.math.BigDecimal,Name:运费单价
	 */
	public static final String BILLENTRY_NCKD_PRICEFIELDYF = "nckd_pricefieldyf";

	/**
	 * Type:java.math.BigDecimal,Name:运费金额
	 */
	public static final String BILLENTRY_NCKD_AMOUNTFIELDYF = "nckd_amountfieldyf";

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
	 * Type:java.math.BigDecimal,Name:基本数量_原始携带值
	 */
	public static final String BILLENTRY_LK_BILLENTRY_LK_BASEQTY_OLD = "billentry_lk_baseqty_old";

	/**
	 * Type:java.math.BigDecimal,Name:基本数量_确认携带值
	 */
	public static final String BILLENTRY_LK_BILLENTRY_LK_BASEQTY = "billentry_lk_baseqty";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:采购部门
	 */
	public static final String DEPT = "dept";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_operatorgroup,Name:采购组
	 */
	public static final String OPERATORGROUP = "operatorgroup";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_operator,Name:采购员
	 */
	public static final String OPERATOR = "operator";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:订货供应商
	 */
	public static final String SUPPLIER = "supplier";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:收款供应商
	 */
	public static final String RECEIVESUPPLIER = "receivesupplier";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:结算供应商
	 */
	public static final String INVOICESUPPLIER = "invoicesupplier";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:供货供应商
	 */
	public static final String PROVIDERSUPPLIER = "providersupplier";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplierlinkman,Name:联系人
	 */
	public static final String LINKMAN = "linkman";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_currency,Name:结算币
	 */
	public static final String SETTLECURRENCY = "settlecurrency";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_settlementtype,Name:结算方式
	 */
	public static final String SETTLETYPE = "settletype";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_exratetable,Name:汇率表
	 */
	public static final String EXRATETABLE = "exratetable";

	/**
	 * Type:java.math.BigDecimal,Name:金额
	 */
	public static final String TOTALAMOUNT = "totalamount";

	/**
	 * Type:java.math.BigDecimal,Name:价税合计
	 */
	public static final String TOTALALLAMOUNT = "totalallamount";

	/**
	 * Type:java.math.BigDecimal,Name:税额
	 */
	public static final String TOTALTAXAMOUNT = "totaltaxamount";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_currency,Name:本位币
	 */
	public static final String CURRENCY = "currency";

	/**
	 * Type:String,Name:关闭状态
	 */
	public static final String CLOSESTATUS = "closestatus";

	/**
	 * Type:String,Name:作废状态
	 */
	public static final String CANCELSTATUS = "cancelstatus";

	/**
	 * Type:String,Name:备注
	 */
	public static final String COMMENT = "comment";

	/**
	 * Type:boolean,Name:含税
	 */
	public static final String ISTAX = "istax";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplierlinkman,Name:供货联系人
	 */
	public static final String PROVIDERLINKMAN = "providerlinkman";

	/**
	 * Type:String,Name:供货联系地址
	 */
	public static final String PROVIDERADDRESS = "provideraddress";

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
	 * Type:java.util.Date,Name:汇率日期
	 */
	public static final String EXRATEDATE = "exratedate";

	/**
	 * Type:java.util.Date,Name:订单日期
	 */
	public static final String BIZTIME = "biztime";

	/**
	 * Type:String,Name:单据生成类型
	 */
	public static final String BILLCRETYPE = "billcretype";

	/**
	 * Type:String,Name:付款方式
	 */
	public static final String PAYMODE = "paymode";

	/**
	 * Type:boolean,Name:录入金额
	 */
	public static final String INPUTAMOUNT = "inputamount";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
	 */
	public static final String LASTUPDATEUSER = "lastupdateuser";

	/**
	 * Type:java.util.Date,Name:修改时间
	 */
	public static final String LASTUPDATETIME = "lastupdatetime";

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
	 * Type:DynamicObject,sourceEntityId:bd_invoicebiztype,Name:发票类别
	 */
	public static final String INVOICEBIZTYPE = "invoicebiztype";

	/**
	 * 分录purbillentry_pay实体标识
	 */
	public static final String ENTRYENTITYID_PURBILLENTRY_PAY = "purbillentry_pay";

	/**
	 * Type:java.util.Date,Name:付款日期
	 */
	public static final String PURBILLENTRY_PAY_PAYDATE = "paydate";

	/**
	 * Type:java.math.BigDecimal,Name:付款比例(%)
	 */
	public static final String PURBILLENTRY_PAY_PAYRATE = "payrate";

	/**
	 * Type:java.math.BigDecimal,Name:付款金额
	 */
	public static final String PURBILLENTRY_PAY_PAYAMOUNT = "payamount";

	/**
	 * Type:boolean,Name:是否预付
	 */
	public static final String PURBILLENTRY_PAY_ISPREPAY = "isprepay";

	/**
	 * Type:String,Name:预付款单编号
	 */
	public static final String PURBILLENTRY_PAY_PREPAYBILLNO = "prepaybillno";

	/**
	 * Type:java.math.BigDecimal,Name:已付金额
	 */
	public static final String PURBILLENTRY_PAY_PAIDAMOUNT = "paidamount";

	/**
	 * Type:java.math.BigDecimal,Name:已开票金额
	 */
	public static final String PURBILLENTRY_PAY_INVOICEDAMOUNT = "invoicedamount";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_assistantdata_detail,Name:款项名称
	 */
	public static final String PURBILLENTRY_PAY_PAYNAME = "payname";

	/**
	 * Type:String,Name:变更方式
	 */
	public static final String PURBILLENTRY_PAY_PAYENTRYCHANGETYPE = "payentrychangetype";

	/**
	 * Type:java.math.BigDecimal,Name:关联付款金额
	 */
	public static final String PURBILLENTRY_PAY_JOINPAYAMOUNT = "joinpayamount";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:创建人
	 */
	public static final String PURBILLENTRY_PAY_PAYENTRYCREATOR = "payentrycreator";

	/**
	 * Type:java.util.Date,Name:创建时间
	 */
	public static final String PURBILLENTRY_PAY_PAYENTRYCREATETIME = "payentrycreatetime";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
	 */
	public static final String PURBILLENTRY_PAY_PAYENTRYMODIFIER = "payentrymodifier";

	/**
	 * Type:java.util.Date,Name:修改时间
	 */
	public static final String PURBILLENTRY_PAY_PAYENTRYMODIFYTIME = "payentrymodifytime";

	/**
	 * Type:String,Name:预付时点
	 */
	public static final String PURBILLENTRY_PAY_PRETIMEPOINT = "pretimepoint";

	/**
	 * Type:String,Name:付款计划来源
	 */
	public static final String PURBILLENTRY_PAY_PAYSRCBILLENTITY = "paysrcbillentity";

	/**
	 * 子分录purbillentry_pay_lk实体标识
	 */
	public static final String SUBENTRYENTITYID_PURBILLENTRY_PAY_LK = "purbillentry_pay_lk";

	/**
	 * Type:java.lang.Long,Name:源单主实体编码
	 */
	public static final String PURBILLENTRY_PAY_LK_PURBILLENTRY_PAY_LK_STABLEID = "purbillentry_pay_lk_stableid";

	/**
	 * Type:java.lang.Long,Name:源单内码
	 */
	public static final String PURBILLENTRY_PAY_LK_PURBILLENTRY_PAY_LK_SBILLID = "purbillentry_pay_lk_sbillid";

	/**
	 * Type:java.lang.Long,Name:源单主实体内码
	 */
	public static final String PURBILLENTRY_PAY_LK_PURBILLENTRY_PAY_LK_SID = "purbillentry_pay_lk_sid";

	/**
	 * Type:String,Name:联系地址
	 */
	public static final String ADDRESS = "address";

	/**
	 * Type:boolean,Name:按比例(%)
	 */
	public static final String ISPAYRATE = "ispayrate";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_paycondition,Name:付款条件
	 */
	public static final String PAYCONDITION = "paycondition";

	/**
	 * Type:java.math.BigDecimal,Name:已预付金额
	 */
	public static final String PAIDPREALLAMOUNT = "paidpreallamount";

	/**
	 * Type:java.math.BigDecimal,Name:已付金额
	 */
	public static final String PAIDALLAMOUNT = "paidallamount";

	/**
	 * Type:String,Name:确认状态
	 */
	public static final String CONFIRMSTATUS = "confirmstatus";

	/**
	 * Type:String,Name:物流状态
	 */
	public static final String LOGISTICSSTATUS = "logisticsstatus";

	/**
	 * Type:String,Name:付款状态
	 */
	public static final String PAYSTATUS = "paystatus";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:结算组织(废弃)
	 */
	public static final String SETTLEORG = "settleorg";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_address,Name:供货联系地址F7
	 */
	public static final String PROVIDERADDRESSF7 = "provideraddressf7";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_address,Name:联系地址F7
	 */
	public static final String ADDRESSF7 = "addressf7";

	/**
	 * Type:boolean,Name:是否虚单
	 */
	public static final String ISVIRTUALBILL = "isvirtualbill";

	/**
	 * Type:String,Name:异步状态
	 */
	public static final String ASYNCSTATUS = "asyncstatus";

	/**
	 * Type:DynamicObject,sourceEntityId:ism_settlerelations,Name:交易路径
	 */
	public static final String TRANSACTEPATH = "transactepath";

	/**
	 * Type:boolean,Name:供应商直运
	 */
	public static final String SUPPLYTRANS = "supplytrans";

	/**
	 * Type:String,Name:子版本号
	 */
	public static final String SUBVERSION = "subversion";

	/**
	 * Type:DynamicObject,sourceEntityId:pm_purpricelist,Name:价目表
	 */
	public static final String PRICELIST = "pricelist";

	/**
	 * Type:boolean,Name:按履约支付
	 */
	public static final String ISALLOWOVERPAY = "isallowoverpay";

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
	 * Type:java.lang.Integer,Name:附件数
	 */
	public static final String NCKD_ATTACHMENTCOUNTFIELD = "nckd_attachmentcountfield";

	/**
	 * Type:String,Name:上游信息接收单
	 */
	public static final String NCKD_UPINFORECEIVEBILL = "nckd_upinforeceivebill";

	/**
	 * Type:DynamicObject,sourceEntityId:nckd_address,Name:物流路线
	 */
	public static final String NCKD_ADDRESS = "nckd_address";

	/**
	 * Type:String,Name:运费承担方
	 */
	public static final String NCKD_YUNFEI = "nckd_yunfei";

	/**
	 * Type:String,Name:承运方类型
	 */
	public static final String NCKD_YUNFEITY = "nckd_yunfeity";

	/**
	 * Type:DynamicObject,sourceEntityId:bos_org,Name:承运组织
	 */
	public static final String NCKD_ORGYF = "nckd_orgyf";

	/**
	 * Type:DynamicObject,sourceEntityId:bd_supplier,Name:承运商
	 */
	public static final String NCKD_CYS = "nckd_cys";

	/**
	 * Type:DynamicObject,sourceEntityId:conm_purcontractf7,Name:采购合同号
	 */
	public static final String NCKD_CGHTH = "nckd_cghth";

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

	public static final String ALLPROPERTY = "id,billno,billstatus,creator,modifier,auditor,auditdate,modifytime,createtime,org,billtype,biztype,closer,closedate,canceler,canceldate,billentry.id,billentry.entrypurorg,billentry.material,billentry.auxpty,billentry.qty,billentry.unit,billentry.entrycomment,billentry.baseqty,billentry.baseunit,billentry.project,billentry.materialname,billentry.rowclosestatus,billentry.rowterminatestatus,billentry.price,billentry.priceandtax,billentry.taxrateid,billentry.taxrate,billentry.discounttype,billentry.discountrate,billentry.amount,billentry.curamount,billentry.taxamount,billentry.curtaxamount,billentry.discountamount,billentry.amountandtax,billentry.curamountandtax,billentry.entrychangetype,billentry.materialmasterid,billentry.materialversion,billentry.auxqty,billentry.auxunit,billentry.srcbillentity,billentry.srcbillentryid,billentry.srcbillid,billentry.srcbillnumber,billentry.srcbillentryseq,billentry.mainbillentity,billentry.mainbillnumber,billentry.mainbillid,billentry.mainbillentryid,billentry.mainbillentryseq,billentry.entrycreator,billentry.entrycreatetime,billentry.entrymodifier,billentry.entrymodifytime,billentry.linetype,billentry.wbs,billentry.deliverdate,billentry.entryrecorg,billentry.deliveraddress,billentry.warehouse,billentry.ispresent,billentry.entryrecdept,billentry.entryreqorg,billentry.entryreqdept,billentry.entrysettleorg,billentry.entrysettledept,billentry.receivebaseqty,billentry.invbaseqty,billentry.returnbaseqty,billentry.deliverlocation,billentry.receiveqty,billentry.invqty,billentry.returnqty,billentry.payablepriceqty,billentry.supplierlot,billentry.entrypayorg,billentry.joinqty,billentry.joinbaseqty,billentry.recretqty,billentry.invretqty,billentry.invretbaseqty,billentry.recretbaseqty,billentry.joinpayablepriceqty,billentry.salbillnumber,billentry.salbillid,billentry.salbillentryid,billentry.salbillentryseq,billentry.iscontrolqty,billentry.receiverateup,billentry.receiveratedown,billentry.receiveqtydown,billentry.receiveqtyup,billentry.receivebaseqtydown,billentry.receivebaseqtyup,billentry.ownertype,billentry.owner,billentry.expenseitem,billentry.payableamount,billentry.iscontrolday,billentry.receivedayup,billentry.receivedaydown,billentry.srcsysbillentryid,billentry.srcsystem,billentry.srcsysbillid,billentry.srcsysbillno,billentry.conbillentity,billentry.conbillnumber,billentry.conbillrownum,billentry.conbillentryseq,billentry.conbillid,billentry.conbillentryid,billentry.mftorderid,billentry.mftordernumber,billentry.mftorderentryid,billentry.mftorderentryseq,billentry.producttype,billentry.returnreceiptqty,billentry.configuredcode,billentry.tracknumber,billentry.promisedate,billentry.soubillnumber,billentry.soubillid,billentry.soubillentryid,billentry.soubillentity,billentry.soubillentryseq,billentry.payablebaseqty,billentry.returnreceiptbaseqty,billentry.joinpayablebaseqty,billentry.receiptnoticeqty,billentry.receiptnoticbaseqty,billentry.iscontrolamountup,billentry.amountup,billentry.joinamount,billentry.performamount,billentry.ecostcenter,billentry.nckd_nckd_sqbm,billentry.nckd_cpcx,billentry.nckd_rymc,billentry.nckd_pricefieldyf,billentry.nckd_amountfieldyf,billentry.nckd_topush,dept,operatorgroup,operator,supplier,receivesupplier,invoicesupplier,providersupplier,linkman,settlecurrency,settletype,exratetable,totalamount,totalallamount,totaltaxamount,currency,closestatus,cancelstatus,comment,istax,providerlinkman,provideraddress,changer,changedate,changestatus,version,exratedate,biztime,billcretype,paymode,inputamount,lastupdateuser,lastupdatetime,exchangerate,exchangetype,unitsrctype,invoicebiztype,purbillentry_pay.id,purbillentry_pay.paydate,purbillentry_pay.payrate,purbillentry_pay.payamount,purbillentry_pay.isprepay,purbillentry_pay.prepaybillno,purbillentry_pay.paidamount,purbillentry_pay.invoicedamount,purbillentry_pay.payname,purbillentry_pay.payentrychangetype,purbillentry_pay.joinpayamount,purbillentry_pay.payentrycreator,purbillentry_pay.payentrycreatetime,purbillentry_pay.payentrymodifier,purbillentry_pay.payentrymodifytime,purbillentry_pay.pretimepoint,purbillentry_pay.paysrcbillentity,address,ispayrate,paycondition,paidpreallamount,paidallamount,confirmstatus,logisticsstatus,paystatus,settleorg,provideraddressf7,addressf7,isvirtualbill,asyncstatus,transactepath,supplytrans,subversion,pricelist,isallowoverpay,nckd_totalprice,nckd_pricecompresult,nckd_priceinfo.id,nckd_priceinfo.nckd_modifierfield,nckd_priceinfo.nckd_modifydatefield,nckd_priceinfo.nckd_suppliername,nckd_priceinfo.nckd_quotation,nckd_attachmentcountfield,nckd_upinforeceivebill,nckd_address,nckd_yunfei,nckd_yunfeity,nckd_orgyf,nckd_cys,nckd_cghth,billhead_lk.id,billhead_lk.billhead_lk_stableid,billhead_lk.billhead_lk_sbillid,billhead_lk.billhead_lk_sid";

    public static final String NCKD_UPAPPLYBILL = "nckd_upapplybill";
}