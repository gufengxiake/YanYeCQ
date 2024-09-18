package nckd.yanye.scm.plugin.operate;

import kd.bd.assistant.service.BaseDataService;
import kd.bos.bd.service.AssignService;
import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.basedata.BaseDataResponse;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 主数据-客商信息维护单审核操作插件
 * 表单标识：nckd_bd_supplier_change
 * author：xiaoxiaopeng
 * date：2024-08-23
 */

public class BdCustomerChangeAuditOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_maintenancetype");

    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (int i = 0; i < dataEntities.length; i++) {
            DynamicObject date = dataEntities[i];
            date = BusinessDataServiceHelper.loadSingle(date.getPkValue(), "nckd_bd_customer_change");
            DynamicObjectCollection entry = date.getDynamicObjectCollection("nckd_entry");
            String maintenanceType = date.getString("nckd_maintenancetype");
            switch (maintenanceType) {
                //新增
                case "save":
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        DynamicObject bdCustomer = BusinessDataServiceHelper.newDynamicObject("bd_customer");
                        String isInvoice = entity.getString("nckd_isinvoice");
                        //获取编码规则
                        CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(bdCustomer.getDataEntityType().getName(), bdCustomer, null);
                        String number = CodeRuleServiceHelper.getNumber(codeRule, bdCustomer);
                        DynamicObject bosOrg = BusinessDataServiceHelper.loadSingle(100000L, "bos_org");

                        bdCustomer.set("number", number);//编码
                        bdCustomer.set("name", entity.get("nckd_addcustomer"));//名称
                        bdCustomer.set("nckd_name1", entity.get("nckd_addcustomer"));//购方名称
                        bdCustomer.set("createorg", bosOrg);//创建组织
                        bdCustomer.set("org", bosOrg);//管理组织
                        bdCustomer.set("createtime", date.get("nckd_date"));//创建日期
                        bdCustomer.set("useorg", date.getDynamicObject("org"));//业务组织
                        bdCustomer.set("status", "C");//单据状态
                        bdCustomer.set("enable", "1");//使用状态
                        bdCustomer.set("ctrlstrategy", "2");//控制策略
                        bdCustomer.set("creator", date.getDynamicObject("creator"));//创建人
                        bdCustomer.set("admindivision", entity.get("nckd_province"));//区域划分
                        bdCustomer.set("nckd_customertype", entity.getDynamicObject("nckd_customerrange"));//客户范围
                        bdCustomer.set("societycreditcode", entity.get("nckd_societycreditcode"));//统一社会信用代码
                        bdCustomer.set("tx_register_no", entity.get("nckd_societycreditcode"));//纳税人识别号
                        bdCustomer.set("nckd_nashuitax", entity.get("nckd_societycreditcode"));//纳税人识别号
                        //bdCustomer.set("nckd_v", entity.getDynamicObject("nckd_businesstype"));//经营类型
                        bdCustomer.set("nckd_group", entity.getDynamicObject("nckd_businesstype"));//分类
                        bdCustomer.set("nckd_customerxz", entity.getDynamicObject("nckd_quality"));//客户性质
                        bdCustomer.set("bloccustomer", entity.getDynamicObject("nckd_basedatafield"));//归属集团客户名称（实际控制人）
                        bdCustomer.set("salerid", entity.getDynamicObject("nckd_buyer"));//业务员
                        bdCustomer.set("nckd_cooperationstatus", getCooperateStatus(entity.getString("nckd_cooperatestatus")));//合作状态
                        //bdCustomer.set("duns", entity.get("nckd_postalcode"));//邮政编码
                        bdCustomer.set("bizpartner_address", entity.getDynamicObject("nckd_address") == null ? null : entity.getDynamicObject("nckd_address").getString("name"));//联系地址
                        bdCustomer.set("nckd_nashuitype", entity.getDynamicObject("nckd_taxpayertype"));//纳税人类型
                        //bdCustomer.set("", entity.get("nckd_invoicename"));//开票单位名称
                        bdCustomer.set("invoicecategory", entity.getDynamicObject("nckd_invoicetype"));//发票类型
                        bdCustomer.set("nckd_yhzh", entity.getString("nckd_banknumber"));//银行账号
                        bdCustomer.set("nckd_bank", entity.getDynamicObject("nckd_bank"));//开户银行
                        bdCustomer.set("nckd_mail", entity.getString("nckd_enterpriseemail"));//客户企业邮箱
                        bdCustomer.set("nckd_phonenumber", entity.getString("nckd_invoicephone"));//交付手机
                        bdCustomer.set("nckd_isopenpay", isInvoice.equals("1") ? true : false);//是否开票
                        //bdCustomer.set("nckd_telnumber", entity.getString("nckd_phone"));//客户联系人电话
                        DynamicObject address = entity.getDynamicObject("nckd_address");
                        if (address != null) {
                            address = BusinessDataServiceHelper.loadSingle(address.getPkValue(), "cts_address");
                            address.set("name", address.getString("name")+entity.getString("nckd_phone"));
                            SaveServiceHelper.update(address);
                        }
                        bdCustomer.set("nckd_addtel", address);//客户联系人地址
                        bdCustomer.set("nckd_email1", entity.getString("nckd_enterpriseemail"));//邮箱1
                        bdCustomer.set("nckd_email2", entity.getString("nckd_customeremail"));//邮箱2
                        bdCustomer.set("nckd_email3", entity.getString("nckd_myemail"));//邮箱3
                        String enterpriseemail = entity.getString("nckd_enterpriseemail");//客户企业邮箱
                        String customeremail = entity.getString("nckd_customeremail");//客户业务员邮箱
                        String myemail = entity.getString("nckd_myemail");//我方业务员邮箱
                        if (StringUtils.isNotEmpty(enterpriseemail) || StringUtils.isNotEmpty(customeremail) || StringUtils.isNotEmpty(myemail)) {
                            String email = (StringUtils.isEmpty(enterpriseemail) ? "" : (enterpriseemail + ";")) + (StringUtils.isEmpty(customeremail) ? "" : (customeremail + ";")) + (StringUtils.isEmpty(myemail) ? "" : (myemail));
                            bdCustomer.set("nckd_mail", email);//客户联系人地
                        }

                        //联系人分录
                        DynamicObjectCollection entryLinkman = bdCustomer.getDynamicObjectCollection("entry_linkman");
                        DynamicObject linkman = entryLinkman.addNew();
                        linkman.set("contactperson", entity.get("nckd_linkman"));//客户联系人姓名
                        linkman.set("phone", entity.get("nckd_phone"));//联系电话
                        linkman.set("isdefault_linkman", true);//默认
                        linkman.set("postalcode", entity.get("nckd_postalcode"));//邮政编码
                        linkman.set("email", entity.getString("nckd_customeremail"));//客户业务员邮箱

                        //银行分录信息保存
                        DynamicObjectCollection entryBank = bdCustomer.getDynamicObjectCollection("entry_bank");
                        DynamicObject bank = entryBank.addNew();
                        bank.set("bankaccount", entity.get("nckd_bankaccount"));//银行账号
                        bank.set("accountname", entity.get("nckd_accountname"));//账户名称
                        bank.set("isdefault_bank", true);//默认
                        bank.set("bank", entity.getDynamicObject("nckd_bank"));//开户银行
                        bank.set("currency", entity.getDynamicObject("nckd_currency"));//币种

                        //客户分类
                        DynamicObjectCollection groupStandard = bdCustomer.getDynamicObjectCollection("entry_groupstandard");
                        DynamicObject group = groupStandard.addNew();
                        DynamicObject customerGroupStandard = BusinessDataServiceHelper.loadSingle("712984405228187648", "bd_customergroupstandard");
                        group.set("groupid", entity.getDynamicObject("nckd_businesstype"));
                        group.set("standardid", customerGroupStandard);


                        try {
                            OperationResult result = OperationServiceHelper.executeOperate("save", "bd_customer", new DynamicObject[]{bdCustomer}, OperateOption.create());
                            if (date.getDynamicObject("org").getLong("id") != 100000) {
                                Set<Long> dataIdsTemp = new HashSet<>();
                                dataIdsTemp.add(bdCustomer.getLong("id"));
                                Set<Long> orgIds = new HashSet<>();
                                orgIds.add(date.getDynamicObject("org").getLong("id"));
                                BaseDataResponse assign = BaseDataServiceHelper.assign("bd_customer", 100000L, "basedata", dataIdsTemp, orgIds);
                                if (!assign.isSuccess()) {
                                    throw new KDBizException("保存信息到客户失败：" + assign.getErrorMsg());
                                }
                            }
                            if (!result.isSuccess()) {
                                throw new KDBizException("保存信息到客户失败：" + result.getMessage());
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException("保存信息客户失败：" + ex);
                        }
                    }
                    break;
                //修改
                case "update":
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        //变更前后，只保存变更后的数据
                        String changeAfter = entity.getString("nckd_changeafter");
                        if ("1".equals(changeAfter)) {
                            continue;
                        }
                        String isInvoice = entity.getString("nckd_isinvoice");
                        //查出更改前数据
                        DynamicObject bdCustomer = BusinessDataServiceHelper.loadSingle(entry.get(t - 1).getDynamicObject("nckd_customermodify").getPkValue(), "bd_customer");
                        bdCustomer.set("name", entity.get("nckd_spname"));//名称
                        bdCustomer.set("nckd_name1", entity.get("nckd_spname"));//购方名称
                        bdCustomer.set("modifier", date.getDynamicObject("creator"));//修改人
                        bdCustomer.set("modifytime", new Date());//修改时间
                        bdCustomer.set("admindivision", entity.get("nckd_province"));//区域划分
                        bdCustomer.set("nckd_customertype", entity.getDynamicObject("nckd_customerrange"));//客户范围
                        bdCustomer.set("societycreditcode", entity.get("nckd_societycreditcode"));//统一社会信用代码
                        bdCustomer.set("tx_register_no", entity.get("nckd_societycreditcode"));//纳税人识别号
                        bdCustomer.set("nckd_nashuitax", entity.get("nckd_societycreditcode"));//纳税人识别号
                        //bdCustomer.set("nckd_v", entity.getDynamicObject("nckd_businesstype"));//经营类型
                        bdCustomer.set("nckd_group", entity.getDynamicObject("nckd_businesstype"));//分类
                        bdCustomer.set("nckd_customerxz", entity.getDynamicObject("nckd_quality"));//客户性质
                        bdCustomer.set("bloccustomer", entity.getDynamicObject("nckd_basedatafield"));//归属集团客户名称（实际控制人）
                        bdCustomer.set("salerid", entity.getDynamicObject("nckd_buyer"));//业务员
                        bdCustomer.set("nckd_cooperationstatus", getCooperateStatus(entity.getString("nckd_cooperatestatus")));//合作状态
                        bdCustomer.set("duns", entity.get("nckd_postalcode"));//邮政编码
                        bdCustomer.set("bizpartner_address", entity.getDynamicObject("nckd_address") == null ? null : entity.getDynamicObject("nckd_address").getString("name"));//联系地址
                        bdCustomer.set("nckd_nashuitype", entity.getDynamicObject("nckd_taxpayertype"));//纳税人类型
                        //bdCustomer.set("", entity.get("nckd_invoicename"));//开票单位名称
                        bdCustomer.set("invoicecategory", entity.getDynamicObject("nckd_invoicetype"));//发票类型
                        bdCustomer.set("nckd_yhzh", entity.getString("nckd_banknumber"));//银行账号
                        bdCustomer.set("nckd_bank", entity.getDynamicObject("nckd_bank"));//开户银行
                        bdCustomer.set("nckd_mail", entity.getString("nckd_enterpriseemail"));//客户企业邮箱
                        bdCustomer.set("nckd_phonenumber", entity.getString("nckd_invoicephone"));//交付手机
                        bdCustomer.set("nckd_isopenpay", isInvoice.equals("1") ? true : false);//是否开票
                        //bdCustomer.set("nckd_telnumber", entity.getString("nckd_phone"));//客户联系人电话
                        DynamicObject address = entity.getDynamicObject("nckd_address");
                        if (address != null) {
                            address = BusinessDataServiceHelper.loadSingle(address.getPkValue(), "cts_address");
                            address.set("name", address.getString("name")+entity.getString("nckd_phone"));
                            SaveServiceHelper.update(address);
                        }
                        bdCustomer.set("nckd_addtel", address);//客户联系人地址
                        bdCustomer.set("nckd_email1", entity.getString("nckd_enterpriseemail"));//邮箱1
                        bdCustomer.set("nckd_email2", entity.getString("nckd_customeremail"));//邮箱2
                        bdCustomer.set("nckd_email3", entity.getString("nckd_myemail"));//邮箱3

                        String enterpriseemail = entity.getString("nckd_enterpriseemail");//客户企业邮箱
                        String customeremail = entity.getString("nckd_customeremail");//客户业务员邮箱
                        String myemail = entity.getString("nckd_myemail");//我方业务员邮箱
                        if (StringUtils.isNotEmpty(enterpriseemail) || StringUtils.isNotEmpty(customeremail) || StringUtils.isNotEmpty(myemail)) {
                            String email = (StringUtils.isEmpty(enterpriseemail) ? "" : (enterpriseemail + ";")) + (StringUtils.isEmpty(customeremail) ? "" : (customeremail + ";")) + (StringUtils.isEmpty(myemail) ? "" : (myemail));
                            bdCustomer.set("nckd_mail", email);//客户联系人地址
                        }
                        //联系人分录
                        DynamicObjectCollection entryLinkman = bdCustomer.getDynamicObjectCollection("entry_linkman");
                        if (entryLinkman.size() > 0) {
                            DynamicObject linkman = entryLinkman.get(0);
                            linkman.set("contactperson", entity.get("nckd_linkman"));//客户联系人姓名
                            linkman.set("phone", entity.get("nckd_phone"));//联系电话
                            linkman.set("isdefault_linkman", true);//默认
                            linkman.set("postalcode", entity.get("nckd_postalcode"));//邮政编码
                            linkman.set("email", entity.getString("nckd_customeremail"));//客户业务员邮箱
                        }

                        //银行分录信息保存
                        DynamicObjectCollection entryBank = bdCustomer.getDynamicObjectCollection("entry_bank");
                        if (entryBank.size() > 0) {
                            DynamicObject bank = entryBank.get(0);
                            bank.set("bankaccount", entity.get("nckd_bankaccount"));//银行账号
                            bank.set("accountname", entity.get("nckd_accountname"));//账户名称
                            bank.set("isdefault_bank", true);//默认
                            bank.set("bank", entity.getDynamicObject("nckd_bank"));//开户银行
                            bank.set("currency", entity.getDynamicObject("nckd_currency"));//币种
                        }

                        //客户分类
                        DynamicObjectCollection groupStandard = bdCustomer.getDynamicObjectCollection("entry_groupstandard");
                        groupStandard.clear();
                        DynamicObject group = groupStandard.addNew();
                        DynamicObject customerGroupStandard = BusinessDataServiceHelper.loadSingle("712984405228187648", "bd_customergroupstandard");
                        group.set("groupid", entity.getDynamicObject("nckd_businesstype"));
                        group.set("standardid", customerGroupStandard);

                        if (!entity.get("nckd_spname").equals(bdCustomer.getString("name"))) {
                            DynamicObjectCollection nameVersionCol = bdCustomer.getDynamicObjectCollection("name$version");
                            if (nameVersionCol.size() > 0) {
                                DynamicObject col = nameVersionCol.addNew();
                                col.set("name$version$name", entity.get("nckd_spname"));
                                col.set("name$version$startdate", new Date());
                                col.set("name$version$enddate", new Date("2099/1/1 00:00:00"));
                                col.set("name$version$enable", "1");
                                col.set("name$version$creator", date.getDynamicObject("creator"));
                                col.set("name$version$createtime", new Date());
                                DynamicObject oldCol = nameVersionCol.get(nameVersionCol.size() - 2);
                                oldCol.set("name$version$enddate", new Date());
                            } else {
                                DynamicObject col = nameVersionCol.addNew();
                                col.set("name$version$name", entity.get("nckd_spname"));
                                col.set("name$version$startdate", new Date());
                                col.set("name$version$enddate", new Date("2099/1/1 00:00:00"));
                                col.set("name$version$enable", "1");
                                col.set("name$version$creator", date.getDynamicObject("creator"));
                                col.set("name$version$createtime", new Date());
                            }
                        }

                        try {
                            OperationResult result = OperationServiceHelper.executeOperate("save", "bd_customer", new DynamicObject[]{bdCustomer}, OperateOption.create());
                            if (!result.isSuccess()) {
                                throw new KDBizException("更新信息到客户失败：" + result.getMessage());
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException("保存信息到客户失败：" + ex);
                        }
                    }
            }

        }

    }

    private String getCooperateStatus(String result) {
        if (StringUtils.isEmpty(result)) {
            return null;
        }
        switch (result) {
            case "4":
            case "5":
                result = "A";
                break;
            case "2":
            case "3":
                result = "B";
                break;
            case "1":
                result = "C";
                break;
        }
        return result;
    }
}
