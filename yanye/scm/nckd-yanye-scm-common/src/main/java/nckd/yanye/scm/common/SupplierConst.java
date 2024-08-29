package nckd.yanye.scm.common;

/**
 * Module           : 基础服务云--基础资料模块--供应商单据
 * Description      : 单据常量类
 * @date            : 2024-08-29
 * @author          : Generator
 * @version         : 1.0
 */
public class SupplierConst {

    public static final String FORMBILLID = "bd_supplier";
    public static final String ID = "id";

    /**
     * Type:String,Name:编码
     */
    public static final String NUMBER = "number";

    /**
     * Type:String,Name:名称
     */
    public static final String NAME = "name";

    /**
     * Type:String,Name:数据状态
     */
    public static final String STATUS = "status";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:创建人
     */
    public static final String CREATOR = "creator";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
     */
    public static final String MODIFIER = "modifier";

    /**
     * Type:String,Name:使用状态
     */
    public static final String ENABLE = "enable";

    /**
     * Type:java.util.Date,Name:创建时间
     */
    public static final String CREATETIME = "createtime";

    /**
     * Type:java.util.Date,Name:修改时间
     */
    public static final String MODIFYTIME = "modifytime";

    /**
     * Type:java.lang.Long,Name:主数据内码
     */
    public static final String MASTERID = "masterid";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:管理组织
     */
    public static final String ORG = "org";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:创建组织
     */
    public static final String CREATEORG = "createorg";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:业务组织
     */
    public static final String USEORG = "useorg";

    /**
     * Type:String,Name:控制策略
     */
    public static final String CTRLSTRATEGY = "ctrlstrategy";

    /**
     * Type:java.lang.Long,Name:原资料id
     */
    public static final String SOURCEDATA = "sourcedata";

    /**
     * Type:java.lang.Integer,Name:位图
     */
    public static final String BITINDEX = "bitindex";

    /**
     * Type:java.lang.Integer,Name:原资料位图
     */
    public static final String SRCINDEX = "srcindex";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:原创建组织
     */
    public static final String SRCCREATEORG = "srccreateorg";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:禁用人
     */
    public static final String DISABLER = "disabler";

    /**
     * Type:java.util.Date,Name:禁用时间
     */
    public static final String DISABLEDATE = "disabledate";

    /**
     * 分录entry_linkman实体标识
     */
    public static final String ENTRYENTITYID_ENTRY_LINKMAN = "entry_linkman";

    /**
     * Type:String,Name:名称
     */
    public static final String ENTRY_LINKMAN_CONTACTPERSON = "contactperson";

    /**
     * Type:kd.bos.dataentity.entity.LocaleDynamicObjectCollection,Name:null
     */
    public static final String ENTRY_LINKMAN_MULTILANGUAGETEXT = "multilanguagetext";

    /**
     * Type:String,Name:性别(已废弃)
     */
    public static final String ENTRY_LINKMAN_GENDER = "gender";

    /**
     * Type:String,Name:职务
     */
    public static final String ENTRY_LINKMAN_CONTACTPERSONPOST = "contactpersonpost";

    /**
     * Type:String,Name:部门
     */
    public static final String ENTRY_LINKMAN_DEPT = "dept";

    /**
     * Type:String,Name:联系电话
     */
    public static final String ENTRY_LINKMAN_PHONE = "phone";

    /**
     * Type:String,Name:传真
     */
    public static final String ENTRY_LINKMAN_FAX = "fax";

    /**
     * Type:String,Name:邮箱
     */
    public static final String ENTRY_LINKMAN_EMAIL = "email";

    /**
     * Type:String,Name:手机(已废弃)
     */
    public static final String ENTRY_LINKMAN_MOBILE = "mobile";

    /**
     * Type:String,Name:邮政编码(已废弃)
     */
    public static final String ENTRY_LINKMAN_POSTALCODE = "postalcode";

    /**
     * Type:String,Name:地址(已废弃)
     */
    public static final String ENTRY_LINKMAN_ADDRESS = "address";

    /**
     * Type:boolean,Name:默认
     */
    public static final String ENTRY_LINKMAN_ISDEFAULT_LINKMAN = "isdefault_linkman";

    /**
     * Type:String,Name:角色
     */
    public static final String ENTRY_LINKMAN_ROLE = "role";

    /**
     * Type:boolean,Name:失效
     */
    public static final String ENTRY_LINKMAN_INVALID = "invalid";

    /**
     * Type:String,Name:姓
     */
    public static final String ENTRY_LINKMAN_FAMILYNAME = "familyname";

    /**
     * Type:String,Name:名
     */
    public static final String ENTRY_LINKMAN_GIVENNAME = "givenname";

    /**
     * Type:String,Name:中间名
     */
    public static final String ENTRY_LINKMAN_MIDDLENAME = "middlename";

    /**
     * Type:String,Name:别名
     */
    public static final String ENTRY_LINKMAN_ALIAS = "alias";

    /**
     * Type:DynamicObject,sourceEntityId:bd_address,Name:关联地址
     */
    public static final String ENTRY_LINKMAN_ASSOCIATEDADDRESS = "associatedaddress";

    /**
     * Type:String,Name:电话区号
     */
    public static final String ENTRY_LINKMAN_PHONECODE = "phonecode";

    /**
     * Type:boolean,Name:删除标识
     */
    public static final String ENTRY_LINKMAN_DELETEIDENTITY = "deleteidentity";

    /**
     * 分录entry_bank实体标识
     */
    public static final String ENTRYENTITYID_ENTRY_BANK = "entry_bank";

    /**
     * Type:String,Name:银行账号
     */
    public static final String ENTRY_BANK_BANKACCOUNT = "bankaccount";

    /**
     * Type:String,Name:账户名称
     */
    public static final String ENTRY_BANK_ACCOUNTNAME = "accountname";

    /**
     * Type:kd.bos.dataentity.entity.LocaleDynamicObjectCollection,Name:null
     */
    public static final String ENTRY_BANK_MULTILANGUAGETEXT = "multilanguagetext";

    /**
     * Type:DynamicObject,sourceEntityId:bd_currency,Name:币种
     */
    public static final String ENTRY_BANK_CURRENCY = "currency";

    /**
     * Type:boolean,Name:默认
     */
    public static final String ENTRY_BANK_ISDEFAULT_BANK = "isdefault_bank";

    /**
     * Type:DynamicObject,sourceEntityId:bd_bebank,Name:开户银行
     */
    public static final String ENTRY_BANK_BANK = "bank";

    /**
     * Type:String,Name:银行账户类型
     */
    public static final String ENTRY_BANK_BANKACCOUNTTYPE = "bankaccounttype";

    /**
     * Type:DynamicObject,sourceEntityId:bd_settlementtype,Name:默认结算方式
     */
    public static final String ENTRY_BANK_SETTLMENT = "settlment";

    /**
     * Type:String,Name:默认手续费承担方
     */
    public static final String ENTRY_BANK_COMMISSIONBEARER = "commissionbearer";

    /**
     * Type:String,Name:默认清算要求参数
     */
    public static final String ENTRY_BANK_LIQUIDATIONPARAM = "liquidationparam";

    /**
     * Type:DynamicObject,sourceEntityId:bd_bebank,Name:默认代理行
     */
    public static final String ENTRY_BANK_AGENTBANK = "agentbank";

    /**
     * Type:String,Name:代理行账号
     */
    public static final String ENTRY_BANK_AGENTBANKACCOUNT = "agentbankaccount";

    /**
     * Type:String,Name:国际银行账户号码
     */
    public static final String ENTRY_BANK_IBAN = "iban";

    /**
     * Type:String,Name:收款方行政区划
     */
    public static final String ENTRY_BANK_PAYEEADMINDIVISION = "payeeadmindivision";

    /**
     * Type:String,Name:收款方详细地址
     */
    public static final String ENTRY_BANK_PAYEEADDRESS = "payeeaddress";

    /**
     * Type:String,Name:收款方联系电话
     */
    public static final String ENTRY_BANK_PAYEEPHONE = "payeephone";

    /**
     * Type:DynamicObject,sourceEntityId:bd_bebank,Name:承兑银行
     */
    public static final String ENTRY_BANK_NCKD_ACCEPTINGBANK = "nckd_acceptingbank";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:负责人
     */
    public static final String PURCHASERID = "purchaserid";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:负责组织
     */
    public static final String PURCHASEDEPTID = "purchasedeptid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_supplier,Name:结算供应商
     */
    public static final String INVOICESUPPLIERID = "invoicesupplierid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_supplier,Name:供货供应商
     */
    public static final String DELIVERSUPPLIERID = "deliversupplierid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_supplier,Name:收款供应商
     */
    public static final String RECEIVINGSUPPLIERID = "receivingsupplierid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_currency,Name:交易币
     */
    public static final String SETTLEMENTCYID = "settlementcyid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_settlementtype,Name:结算方式
     */
    public static final String SETTLEMENTTYPEID = "settlementtypeid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_paycondition,Name:付款条件
     */
    public static final String PAYCOND = "paycond";

    /**
     * Type:boolean,Name:启用采购协同
     */
    public static final String ISSUPPCOLLA = "issuppcolla";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:管理组织
     */
    public static final String ADMINORG = "adminorg";

    /**
     * Type:String,Name:简称
     */
    public static final String SIMPLENAME = "simplename";

    /**
     * Type:String,Name:计税类型
     */
    public static final String TAXTYPE = "taxtype";

    /**
     * Type:String,Name:伙伴类型
     */
    public static final String TYPE = "type";

    /**
     * Type:DynamicObject,sourceEntityId:bd_country,Name:国家/地区
     */
    public static final String COUNTRY = "country";

    /**
     * Type:String,Name:详细地址
     */
    public static final String BIZPARTNER_ADDRESS = "bizpartner_address";

    /**
     * Type:String,Name:联系人
     */
    public static final String LINKMAN = "linkman";

    /**
     * Type:String,Name:联系电话
     */
    public static final String BIZPARTNER_PHONE = "bizpartner_phone";

    /**
     * Type:String,Name:传真
     */
    public static final String BIZPARTNER_FAX = "bizpartner_fax";

    /**
     * Type:String,Name:统一社会信用代码
     */
    public static final String SOCIETYCREDITCODE = "societycreditcode";

    /**
     * Type:String,Name:组织机构代码(已废弃)
     */
    public static final String ORGCODE = "orgcode";

    /**
     * Type:String,Name:工商登记号(已废弃)
     */
    public static final String BIZ_REGISTER_NUM = "biz_register_num";

    /**
     * Type:String,Name:纳税人识别号
     */
    public static final String TX_REGISTER_NO = "tx_register_no";

    /**
     * Type:String,Name:法人代表
     */
    public static final String ARTIFICIALPERSON = "artificialperson";

    /**
     * Type:DynamicObject,sourceEntityId:bos_org,Name:内部业务单元
     */
    public static final String INTERNAL_COMPANY = "internal_company";

    /**
     * Type:String,Name:商务伙伴内码(已废弃)
     */
    public static final String BIZPARTNERID = "bizpartnerid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_suppliergroup,Name:供应商分组
     */
    public static final String GROUP = "group";

    /**
     * Type:String,Name:图片
     */
    public static final String PICTUREFIELD = "picturefield";

    /**
     * Type:DynamicObject,sourceEntityId:bd_bizpartner,Name:商务伙伴
     */
    public static final String BIZPARTNER = "bizpartner";

    /**
     * Type:java.util.Date,Name:商城入驻时间
     */
    public static final String MALLDATE = "malldate";

    /**
     * Type:String,Name:商城入驻状态
     */
    public static final String MALLSTATUS = "mallstatus";

    /**
     * Type:String,Name:注册资本
     */
    public static final String REGCAPITAL = "regcapital";

    /**
     * Type:String,Name:营业期限
     */
    public static final String BUSINESSTERM = "businessterm";

    /**
     * Type:String,Name:经营范围
     */
    public static final String BUSINESSSCOPE = "businessscope";

    /**
     * Type:java.util.Date,Name:成立日期
     */
    public static final String ESTABLISHDATE = "establishdate";

    /**
     * 分录entry_groupstandard实体标识
     */
    public static final String ENTRYENTITYID_ENTRY_GROUPSTANDARD = "entry_groupstandard";

    /**
     * Type:DynamicObject,sourceEntityId:bd_suppliergroupstandard,Name:分类标准
     */
    public static final String ENTRY_GROUPSTANDARD_STANDARDID = "standardid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_suppliergroup,Name:分类
     */
    public static final String ENTRY_GROUPSTANDARD_GROUPID = "groupid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_supplier,Name:供应商
     */
    public static final String ENTRY_GROUPSTANDARD_SUPPLIERID = "supplierid";

    /**
     * Type:DynamicObject,sourceEntityId:bd_taxrate,Name:默认税率(%)
     */
    public static final String TAXRATE = "taxrate";

    /**
     * Type:DynamicObject,sourceEntityId:bd_currency,Name:付款币种
     */
    public static final String PAYMENTCURRENCY = "paymentcurrency";

    /**
     * Type:boolean,Name:可VMI
     */
    public static final String ENABLEVMI = "enablevmi";

    /**
     * Type:String,Name:业务职能
     */
    public static final String BIZFUNCTION = "bizfunction";

    /**
     * Type:boolean,Name:发票冻结
     */
    public static final String INVOICEHOLD = "invoicehold";

    /**
     * Type:boolean,Name:付款冻结
     */
    public static final String PAYHOLD = "payhold";

    /**
     * Type:boolean,Name:采购冻结
     */
    public static final String PURCHASEHOLD = "purchasehold";

    /**
     * Type:String,Name:行政区划
     */
    public static final String ADMINDIVISION = "admindivision";

    /**
     * Type:String,Name:公司网址
     */
    public static final String URL = "url";

    /**
     * Type:String,Name:电子邮箱
     */
    public static final String POSTAL_CODE = "postal_code";

    /**
     * Type:String,Name:发票类型(已失效)
     */
    public static final String INVOICETYPE = "invoicetype";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:员工
     */
    public static final String EMPLOYEE = "employee";

    /**
     * 分录entry_address实体标识
     */
    public static final String ENTRYENTITYID_ENTRY_ADDRESS = "entry_address";

    /**
     * Type:String,Name:编码
     */
    public static final String ENTRY_ADDRESS_ADDNUMBER = "addnumber";

    /**
     * Type:String,Name:传真
     */
    public static final String ENTRY_ADDRESS_ADDLINKMAN = "addlinkman";

    /**
     * Type:String,Name:电子邮箱
     */
    public static final String ENTRY_ADDRESS_ADDEMAIL = "addemail";

    /**
     * Type:String,Name:时区
     */
    public static final String ENTRY_ADDRESS_ADDTIMEZONE = "addtimezone";

    /**
     * Type:boolean,Name:默认
     */
    public static final String ENTRY_ADDRESS_DEFAULT = "default";

    /**
     * Type:String,Name:地址id
     */
    public static final String ENTRY_ADDRESS_ADDID = "addid";

    /**
     * Type:String,Name:使用状态
     */
    public static final String ENTRY_ADDRESS_ADDSTATUS = "addstatus";

    /**
     * Type:boolean,Name:失效
     */
    public static final String ENTRY_ADDRESS_ADDINVALID = "addinvalid";

    /**
     * Type:String,Name:详细地址
     */
    public static final String ENTRY_ADDRESS_ADDFULLADDRESS = "addfulladdress";

    /**
     * Type:DynamicObject,sourceEntityId:bd_address,Name:供应商地址
     */
    public static final String ENTRY_ADDRESS_SUPPLIERADDRESS = "supplieraddress";

    /**
     * Type:String,Name:名称
     */
    public static final String ENTRY_ADDRESS_ADDNAME = "addname";

    /**
     * Type:String,Name:联系电话
     */
    public static final String ENTRY_ADDRESS_ADDPHONE = "addphone";

    /**
     * Type:String,Name:行政区划
     */
    public static final String ENTRY_ADDRESS_ADDADMINDIVISION = "addadmindivision";

    /**
     * Type:String,Name:供应商地址用途
     */
    public static final String ENTRY_ADDRESS_ADDSUPPLIERADDRSSPURPOSE = "addsupplieraddrsspurpose";

    /**
     * Type:String,Name:邮政编码
     */
    public static final String ENTRY_ADDRESS_ADDPOSTALCODE = "addpostalcode";

    /**
     * Type:String,Name:电话区号
     */
    public static final String ENTRY_ADDRESS_ADDPHONECODE = "addphonecode";

    /**
     * 分录entry_site实体标识
     */
    public static final String ENTRYENTITYID_ENTRY_SITE = "entry_site";

    /**
     * Type:String,Name:编码
     */
    public static final String ENTRY_SITE_SITENUMBER = "sitenumber";

    /**
     * Type:String,Name:使用状态
     */
    public static final String ENTRY_SITE_SITESTATUS = "sitestatus";

    /**
     * Type:String,Name:业务职能
     */
    public static final String ENTRY_SITE_SITEBIZFUNCTION = "sitebizfunction";

    /**
     * Type:String,Name:地点id
     */
    public static final String ENTRY_SITE_SITEID = "siteid";

    /**
     * Type:String,Name:创建组织
     */
    public static final String ENTRY_SITE_SITECREATEORG = "sitecreateorg";

    /**
     * Type:String,Name:业务组织
     */
    public static final String ENTRY_SITE_SITEUSEORG = "siteuseorg";

    /**
     * Type:String,Name:名称
     */
    public static final String ENTRY_SITE_SITENAME = "sitename";

    /**
     * Type:String,Name:联系地址
     */
    public static final String ENTRY_SITE_SITEADDRESS = "siteaddress";

    /**
     * Type:String,Name:支付周期
     */
    public static final String PAYMENTUNIT = "paymentunit";

    /**
     * Type:String,Name:发票匹配规则
     */
    public static final String MATCHINGRULE = "matchingrule";

    /**
     * Type:DynamicObject,sourceEntityId:bos_assistantdata_detail,Name:收票地址
     */
    public static final String INVOICEADDRESS = "invoiceaddress";

    /**
     * Type:String,Name:身份证号
     */
    public static final String IDNO = "idno";

    /**
     * Type:DynamicObject,sourceEntityId:bd_supplier,Name:集团供应商
     */
    public static final String BLOCSUPPLIER = "blocsupplier";

    /**
     * Type:String,Name:简拼
     */
    public static final String SIMPLEPINYIN = "simplepinyin";

    /**
     * Type:DynamicObject,sourceEntityId:bd_address,Name:开票地址
     */
    public static final String BILLADDRESS = "billaddress";

    /**
     * Type:DynamicObject,sourceEntityId:bd_supplierstatus,Name:供应商状态
     */
    public static final String SUPPLIER_STATUS = "supplier_status";

    /**
     * Type:DynamicObject,sourceEntityId:bd_invoicetype,Name:发票类型
     */
    public static final String INVOICECATEGORY = "invoicecategory";

    /**
     * Type:java.util.Date,Name:失效日期（废弃）
     */
    public static final String INVALIDDATE = "invaliddate";

    /**
     * Type:boolean,Name:简易供应商标识
     */
    public static final String SIMPLESUPPLIER = "simplesupplier";

    /**
     * Type:DynamicObject,sourceEntityId:bd_country,Name:税务注册地
     */
    public static final String TAXREGISTPLACE = "taxregistplace";

    /**
     * Type:String,Name:税号
     */
    public static final String TAXNO = "taxno";

    /**
     * 分录entry_tax实体标识
     */
    public static final String ENTRYENTITYID_ENTRY_TAX = "entry_tax";

    /**
     * Type:DynamicObject,sourceEntityId:bd_taxaptitudes,Name:税务资质
     */
    public static final String ENTRY_TAX_TAXCERTIFICATE = "taxcertificate";

    /**
     * Type:java.util.Date,Name:生效日期
     */
    public static final String ENTRY_TAX_EFFECTIVEDATE = "effectivedate";

    /**
     * Type:java.util.Date,Name:失效日期
     */
    public static final String ENTRY_TAX_EXPIRYDATE = "expirydate";

    /**
     * Type:String,Name:邓白氏编码
     */
    public static final String DUNS = "duns";

    /**
     * Type:DynamicObject,sourceEntityId:bd_currency,Name:注册资本币种
     */
    public static final String CUREGCAPITAL = "curegcapital";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:审核人
     */
    public static final String APPROVERID = "approverid";

    /**
     * Type:java.util.Date,Name:审核日期
     */
    public static final String APPROVEDATE = "approvedate";

    /**
     * Type:String,Name:招采平台供应商id
     */
    public static final String NCKD_PLATFORMSUPID = "nckd_platformsupid";

    /**
     * Type:boolean,Name:是否承运商
     */
    public static final String NCKD_ISCYS = "nckd_iscys";

    /**
     * Type:String,Name:单位类型
     */
    public static final String NCKD_UNITTYPE = "nckd_unittype";

    /**
     * Type:String,Name:运输许可证编号
     */
    public static final String NCKD_LICENSENUMBER = "nckd_licensenumber";

    /**
     * Type:String,Name:运输方式
     */
    public static final String NCKD_TRANSPORTTYPE = "nckd_transporttype";

    /**
     * Type:DynamicObject,sourceEntityId:bd_taxrate,Name:承运商税率
     */
    public static final String NCKD_RATE = "nckd_rate";

    /**
     * Type:String,Name:供应商类型
     */
    public static final String NCKD_SUPPLIERTYPE = "nckd_suppliertype";

    /**
     * Type:String,Name:合作状态
     */
    public static final String NCKD_COOPERATESTATUS = "nckd_cooperatestatus";

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
     * 分录name$version实体标识
     */
    public static final String ENTRYENTITYID_NAME$VERSION = "name$version";

    /**
     * Type:String,Name:名称
     */
    public static final String NAME$VERSION_NAME$VERSION$NAME = "name$version$name";

    /**
     * Type:kd.bos.dataentity.entity.LocaleDynamicObjectCollection,Name:null
     */
    public static final String NAME$VERSION_MULTILANGUAGETEXT = "multilanguagetext";

    /**
     * Type:java.util.Date,Name:生效日期
     */
    public static final String NAME$VERSION_NAME$VERSION$STARTDATE = "name$version$startdate";

    /**
     * Type:java.util.Date,Name:失效日期
     */
    public static final String NAME$VERSION_NAME$VERSION$ENDDATE = "name$version$enddate";

    /**
     * Type:boolean,Name:是否生效
     */
    public static final String NAME$VERSION_NAME$VERSION$ENABLE = "name$version$enable";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:修改人
     */
    public static final String NAME$VERSION_NAME$VERSION$MODIFIER = "name$version$modifier";

    /**
     * Type:java.util.Date,Name:修改时间
     */
    public static final String NAME$VERSION_NAME$VERSION$MODIFYTIME = "name$version$modifytime";

    /**
     * Type:DynamicObject,sourceEntityId:bos_user,Name:创建人
     */
    public static final String NAME$VERSION_NAME$VERSION$CREATOR = "name$version$creator";

    /**
     * Type:java.util.Date,Name:创建时间
     */
    public static final String NAME$VERSION_NAME$VERSION$CREATETIME = "name$version$createtime";

    public static final String ALLPROPERTY = "id,number,name,status,creator,modifier,enable,createtime,modifytime,masterid,org,createorg,useorg,ctrlstrategy,sourcedata,bitindex,srcindex,srccreateorg,disabler,disabledate,entry_linkman.id,entry_linkman.contactperson,entry_linkman.multilanguagetext,entry_linkman.gender,entry_linkman.contactpersonpost,entry_linkman.dept,entry_linkman.phone,entry_linkman.fax,entry_linkman.email,entry_linkman.mobile,entry_linkman.postalcode,entry_linkman.address,entry_linkman.isdefault_linkman,entry_linkman.role,entry_linkman.invalid,entry_linkman.familyname,entry_linkman.givenname,entry_linkman.middlename,entry_linkman.alias,entry_linkman.associatedaddress,entry_linkman.phonecode,entry_linkman.deleteidentity,entry_bank.id,entry_bank.bankaccount,entry_bank.accountname,entry_bank.multilanguagetext,entry_bank.currency,entry_bank.isdefault_bank,entry_bank.bank,entry_bank.bankaccounttype,entry_bank.settlment,entry_bank.commissionbearer,entry_bank.liquidationparam,entry_bank.agentbank,entry_bank.agentbankaccount,entry_bank.iban,entry_bank.payeeadmindivision,entry_bank.payeeaddress,entry_bank.payeephone,entry_bank.nckd_acceptingbank,purchaserid,purchasedeptid,invoicesupplierid,deliversupplierid,receivingsupplierid,settlementcyid,settlementtypeid,paycond,issuppcolla,adminorg,simplename,taxtype,type,country,bizpartner_address,linkman,bizpartner_phone,bizpartner_fax,societycreditcode,orgcode,biz_register_num,tx_register_no,artificialperson,internal_company,bizpartnerid,group,picturefield,bizpartner,malldate,mallstatus,regcapital,businessterm,businessscope,establishdate,entry_groupstandard.id,entry_groupstandard.standardid,entry_groupstandard.groupid,entry_groupstandard.supplierid,taxrate,paymentcurrency,enablevmi,bizfunction,invoicehold,payhold,purchasehold,admindivision,url,postal_code,invoicetype,employee,entry_address.id,entry_address.addnumber,entry_address.addlinkman,entry_address.addemail,entry_address.addtimezone,entry_address.default,entry_address.addid,entry_address.addstatus,entry_address.addinvalid,entry_address.addfulladdress,entry_address.supplieraddress,entry_address.addname,entry_address.addphone,entry_address.addadmindivision,entry_address.addsupplieraddrsspurpose,entry_address.addpostalcode,entry_address.addphonecode,entry_site.id,entry_site.sitenumber,entry_site.sitestatus,entry_site.sitebizfunction,entry_site.siteid,entry_site.sitecreateorg,entry_site.siteuseorg,entry_site.sitename,entry_site.siteaddress,paymentunit,matchingrule,invoiceaddress,idno,blocsupplier,simplepinyin,billaddress,supplier_status,invoicecategory,invaliddate,simplesupplier,taxregistplace,taxno,entry_tax.id,entry_tax.taxcertificate,entry_tax.effectivedate,entry_tax.expirydate,duns,curegcapital,approverid,approvedate,nckd_platformsupid,nckd_iscys,nckd_unittype,nckd_licensenumber,nckd_transporttype,nckd_rate,nckd_suppliertype,nckd_cooperatestatus,billhead_lk.id,billhead_lk.billhead_lk_stableid,billhead_lk.billhead_lk_sbillid,billhead_lk.billhead_lk_sid,name$version.id,name$version.name$version$name,name$version.multilanguagetext,name$version.name$version$startdate,name$version.name$version$enddate,name$version.name$version$enable,name$version.name$version$modifier,name$version.name$version$modifytime,name$version.name$version$creator,name$version.name$version$createtime";

}