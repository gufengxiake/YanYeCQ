package nckd.yanye.scm.plugin.operate;

import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

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
            date = BusinessDataServiceHelper.loadSingle(date.getPkValue(),"nckd_bd_customer_change");
            DynamicObjectCollection entry = date.getDynamicObjectCollection("nckd_entry");
            String maintenanceType = date.getString("nckd_maintenancetype");
            switch (maintenanceType) {
                //新增
                case "save":
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        DynamicObject bdCustomer = BusinessDataServiceHelper.newDynamicObject("bd_customer");
                        //获取编码规则
                        CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(bdCustomer.getDataEntityType().getName(), bdCustomer, null);
                        String number = CodeRuleServiceHelper.getNumber(codeRule, bdCustomer);

                        bdCustomer.set("number", number);//编码
                        bdCustomer.set("name", entity.get("nckd_spname"));//名称
                        bdCustomer.set("createorg", date.getDynamicObject("org"));//创建组织
                        bdCustomer.set("org", date.getDynamicObject("org"));//管理组织
                        bdCustomer.set("createtime", date.get("nckd_date"));//创建日期
                        bdCustomer.set("useorg", date.getDynamicObject("org"));//业务组织
                        bdCustomer.set("status", "C");//单据状态
                        bdCustomer.set("enable", "1");//使用状态
                        bdCustomer.set("ctrlstrategy", "5");//控制策略
                        bdCustomer.set("creator", date.getDynamicObject("creator"));//创建人
                        bdCustomer.set("admindivision", entity.get("nckd_province"));//区域划分
                        bdCustomer.set("nckd_customertype", entity.getDynamicObject("nckd_customerrange"));//客户范围
                        bdCustomer.set("societycreditcode", entity.get("nckd_societycreditcode"));//统一社会信用代码
                        bdCustomer.set("nckd_v", entity.getDynamicObject("nckd_businesstype"));//经营类型
                        bdCustomer.set("nckd_customerxz", entity.getDynamicObject("nckd_quality"));//客户性质
                        bdCustomer.set("bloccustomer", entity.getDynamicObject("nckd_basedatafield"));//归属集团客户名称（实际控制人）
                        bdCustomer.set("salerid", entity.getDynamicObject("nckd_buyer"));//业务员
                        bdCustomer.set("nckd_cooperationstatus", getCooperateStatus(entity.getString("nckd_cooperatestatus")));//合作状态
                        bdCustomer.set("duns", entity.get("nckd_postalcode"));//邮政编码
                        bdCustomer.set("bizpartner_address", entity.get("nckd_address"));//联系地址
                        bdCustomer.set("nckd_nashuitype", entity.getDynamicObject("nckd_taxpayertype"));//纳税人类型
                        //bdCustomer.set("", entity.get("nckd_invoicename"));//开票单位名称
                        bdCustomer.set("invoicecategory", entity.getString("nckd_invoicetype") == null ? null : BusinessDataServiceHelper.loadSingle("bd_invoicetype","id,number",new QFilter[]{new QFilter("number", QCP.equals,entity.getString("nckd_invoicetype"))}));//发票类型
                        bdCustomer.set("nckd_yhzh",entity.getString("nckd_banknumber"));//银行账号
                        bdCustomer.set("nckd_telnumber",entity.getString("nckd_invoicephone"));//客户收票手机号码
                        bdCustomer.set("nckd_mail",entity.getString("nckd_enterpriseemail"));//客户企业邮箱

                        //联系人分录
                        DynamicObjectCollection entryLinkman = bdCustomer.getDynamicObjectCollection("entry_linkman");
                        DynamicObject linkman = entryLinkman.addNew();
                        linkman.set("contactperson", entity.get("nckd_linkman"));//客户联系人姓名
                        linkman.set("phone", entity.get("nckd_phone"));//联系电话
                        linkman.set("isdefault_linkman", true);//默认
                        linkman.set("email",entity.getString("nckd_customeremail"));//客户业务员邮箱

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
                        DynamicObject customerGroup = BusinessDataServiceHelper.loadSingle("bd_customergroup", "id", new QFilter[]{new QFilter("number", QCP.equals, entity.getDynamicObject("nckd_businesstype").get("number"))});
                        DynamicObject customerGroupStandard = BusinessDataServiceHelper.loadSingle("712984405228187648", "bd_customergroupstandard");
                        group.set("groupid",customerGroup);
                        group.set("standardid",customerGroupStandard);


                        try {
                            OperationResult result = OperationServiceHelper.executeOperate("save", "bd_customer", new DynamicObject[]{bdCustomer}, OperateOption.create());
                            if (!result.isSuccess()){
                                throw new KDBizException("保存信息到客户失败：" + result.getMessage());
                            }
                        }catch (Exception ex){
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
                        if ("1".equals(changeAfter)){
                            continue;
                        }
                        //查出更改前数据
                        DynamicObject bdCustomer = BusinessDataServiceHelper.loadSingle(entry.get(t - 1).getDynamicObject("nckd_customermodify").getPkValue(), "bd_customer");
                        bdCustomer.set("name", entity.get("nckd_spname"));//名称
                        bdCustomer.set("modifier", date.getDynamicObject("creator"));//修改人
                        bdCustomer.set("modifytime", new Date());//修改时间
                        bdCustomer.set("admindivision", entity.get("nckd_province"));//区域划分
                        bdCustomer.set("nckd_customertype", entity.getDynamicObject("nckd_customerrange"));//客户范围
                        bdCustomer.set("societycreditcode", entity.get("nckd_societycreditcode"));//统一社会信用代码
                        bdCustomer.set("nckd_v", entity.getDynamicObject("nckd_businesstype"));//经营类型
                        bdCustomer.set("nckd_customerxz", entity.getDynamicObject("nckd_quality"));//客户性质
                        bdCustomer.set("bloccustomer", entity.getDynamicObject("nckd_basedatafield"));//归属集团客户名称（实际控制人）
                        bdCustomer.set("salerid", entity.getDynamicObject("nckd_buyer"));//业务员
                        bdCustomer.set("nckd_cooperationstatus", getCooperateStatus(entity.getString("nckd_cooperatestatus")));//合作状态
                        bdCustomer.set("duns", entity.get("nckd_postalcode"));//邮政编码
                        bdCustomer.set("bizpartner_address", entity.get("nckd_address"));//联系地址
                        bdCustomer.set("nckd_nashuitype", entity.getDynamicObject("nckd_taxpayertype"));//纳税人类型
                        //bdCustomer.set("", entity.get("nckd_invoicename"));//开票单位名称
                        bdCustomer.set("invoicecategory", entity.getString("nckd_invoicetype") == null ? null : BusinessDataServiceHelper.loadSingle("bd_invoicetype","id,number",new QFilter[]{new QFilter("number", QCP.equals,entity.getString("nckd_invoicetype"))}));//发票类型
                        bdCustomer.set("nckd_yhzh",entity.getString("nckd_banknumber"));//银行账号
                        bdCustomer.set("nckd_telnumber",entity.getString("nckd_invoicephone"));//客户收票手机号码
                        bdCustomer.set("nckd_mail",entity.getString("nckd_enterpriseemail"));//客户企业邮箱

                        //联系人分录
                        DynamicObjectCollection entryLinkman = bdCustomer.getDynamicObjectCollection("entry_linkman");
                        if (entryLinkman.size() > 0){
                            DynamicObject linkman = entryLinkman.get(0);
                            linkman.set("contactperson", entity.get("nckd_linkman"));//客户联系人姓名
                            linkman.set("phone", entity.get("nckd_phone"));//联系电话
                            linkman.set("isdefault_linkman", true);//默认
                            linkman.set("email",entity.getString("nckd_customeremail"));//客户业务员邮箱
                        }

                        //银行分录信息保存
                        DynamicObjectCollection entryBank = bdCustomer.getDynamicObjectCollection("entry_bank");
                        if (entryBank.size() > 0){
                            DynamicObject bank = entryBank.get(0);
                            bank.set("bankaccount", entity.get("nckd_bankaccount"));//银行账号
                            bank.set("accountname", entity.get("nckd_accountname"));//账户名称
                            bank.set("isdefault_bank", true);//默认
                            bank.set("bank", entity.getDynamicObject("nckd_bank"));//开户银行
                            bank.set("currency", entity.getDynamicObject("nckd_currency"));//币种
                        }

                        //客户分类
                        DynamicObjectCollection groupStandard = bdCustomer.getDynamicObjectCollection("entry_groupstandard");
                        if (groupStandard.size() > 0){
                            DynamicObject group = groupStandard.get(0);
                            DynamicObject customerGroup = BusinessDataServiceHelper.loadSingle("bd_customergroup", "id", new QFilter[]{new QFilter("number", QCP.equals, entity.getDynamicObject("nckd_businesstype").get("number"))});
                            group.set("groupid",customerGroup);
                        }

                        if (!entity.get("nckd_spname").equals(bdCustomer.getString("name"))){
                            DynamicObjectCollection nameVersionCol = bdCustomer.getDynamicObjectCollection("name$version");
                            if (nameVersionCol.size() > 0){
                                DynamicObject col = nameVersionCol.addNew();
                                col.set("name$version$name", entity.get("nckd_spname"));
                                col.set("name$version$startdate", new Date());
                                col.set("name$version$enddate", new Date("2099/1/1 00:00:00"));
                                col.set("name$version$enable", "1");
                                col.set("name$version$creator", date.getDynamicObject("creator"));
                                col.set("name$version$createtime", new Date());
                                DynamicObject oldCol = nameVersionCol.get(nameVersionCol.size() - 2);
                                oldCol.set("name$version$enddate", new Date());
                            }else {
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
                            if (!result.isSuccess()){
                                throw new KDBizException("更新信息到供应商失败：" + result.getMessage());
                            }
                        }catch (Exception ex){
                            throw new RuntimeException("保存信息到供应商失败：" + ex);
                        }
                    }
            }

        }

    }

    private String getCooperateStatus(String result){
        if (StringUtils.isEmpty(result)){
            return null;
        }
        switch (result){
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