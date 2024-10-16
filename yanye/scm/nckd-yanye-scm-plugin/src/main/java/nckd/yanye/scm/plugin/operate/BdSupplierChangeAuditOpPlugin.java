package nckd.yanye.scm.plugin.operate;

import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.IOperateInfo;
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
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 主数据-客商信息维护单审核操作插件
 * 表单标识：nckd_bd_supplier_change
 * author：xiaoxiaopeng
 * date：2024-08-23
 */

public class BdSupplierChangeAuditOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_maintenancetype");
        fieldKeys.add("nckd_merchanttype");

    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (int i = 0; i < dataEntities.length; i++) {
            DynamicObject date = dataEntities[i];
            date = BusinessDataServiceHelper.loadSingle(date.getPkValue(), "nckd_bd_supplier_change");
            DynamicObjectCollection entry = date.getDynamicObjectCollection("nckd_entry");
            String maintenanceType = date.getString("nckd_maintenancetype");
            DynamicObject bosOrg = BusinessDataServiceHelper.loadSingle("bos_org","id,number",new QFilter[]{new QFilter("number",QCP.equals,"1")});
            switch (maintenanceType) {
                //新增
                case "save":
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        DynamicObject bdSupplier = BusinessDataServiceHelper.newDynamicObject("bd_supplier");
                        //获取编码规则
                        CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(bdSupplier.getDataEntityType().getName(), bdSupplier, null);
                        String number = CodeRuleServiceHelper.getNumber(codeRule, bdSupplier);

                        bdSupplier.set("number", number);//编码
                        bdSupplier.set("name", entity.get("nckd_addsupplier"));//名称
                        bdSupplier.set("createorg", bosOrg);//创建组织
                        bdSupplier.set("org", bosOrg);//管理组织
                        bdSupplier.set("createtime", date.get("nckd_date"));//创建日期
                        bdSupplier.set("useorg", date.getDynamicObject("org"));//业务组织
                        bdSupplier.set("status", "C");//单据状态
                        bdSupplier.set("enable", "1");//使用状态
                        bdSupplier.set("ctrlstrategy", "5");//控制策略
                        bdSupplier.set("creator", date.getDynamicObject("creator"));//创建人
                        bdSupplier.set("group", entity.getDynamicObject("nckd_group"));//供应商分组
                        bdSupplier.set("societycreditcode", entity.get("nckd_societycreditcode"));//统一社会信用代码
                        bdSupplier.set("artificialperson", entity.get("nckd_artificialperson"));//法定代表人
                        bdSupplier.set("regcapital", entity.get("nckd_regcapital"));//注册资本
                        bdSupplier.set("blocsupplier", entity.getDynamicObject("nckd_basedatafield"));//归属集团供应商名称（实际控制人）
                        bdSupplier.set("linkman", entity.get("nckd_linkman"));//联系人
                        bdSupplier.set("bizpartner_phone", entity.get("nckd_phone"));//联系电话
                        bdSupplier.set("bizpartner_address", entity.get("nckd_address"));//联系地址
                        //bdSupplier.set("duns", entity.get("nckd_postalcode"));//邮政编码
                        //bdSupplier.set("purchaserid", entity.getDynamicObject("nckd_buyer"));//负责采购员
                        bdSupplier.set("nckd_unittype", entity.get("nckd_risk"));//风险属性
                        bdSupplier.set("nckd_suppliertype", entity.get("nckd_suppliertype"));//供应商类型
                        bdSupplier.set("nckd_cooperatestatus", entity.get("nckd_cooperatestatus"));//合作状态
                        if ("carrier".equals(date.get("nckd_merchanttype"))) {
                            bdSupplier.set("nckd_iscys", true);//是否承运商
                            bdSupplier.set("nckd_licensenumber", entity.get("nckd_licensenumber"));//运输许可证编号
                            bdSupplier.set("nckd_transporttype", entity.get("nckd_transporttype"));//运输方式
                            bdSupplier.set("nckd_rate", entity.getDynamicObject("nckd_rate"));//承运商税率
                        }
                        //银行分录信息保存
                        DynamicObjectCollection entryBank = bdSupplier.getDynamicObjectCollection("entry_bank");
                        DynamicObject bank = entryBank.addNew();
                        bank.set("bankaccount", entity.get("nckd_bankaccount"));//银行账号
                        bank.set("accountname", entity.get("nckd_accountname"));//账户名称
                        bank.set("isdefault_bank", true);//默认
                        bank.set("bank", entity.getDynamicObject("nckd_bank"));//开户银行
                        bank.set("currency", entity.getDynamicObject("nckd_currency"));//币种
                        bank.set("nckd_acceptingbank", entity.getDynamicObject("nckd_acceptingbank"));//承兑银行

                        //供应商分类
                        DynamicObjectCollection groupStandard = bdSupplier.getDynamicObjectCollection("entry_groupstandard");
                        DynamicObject group = groupStandard.addNew();
                        DynamicObject bdSupplierGroupStandard = BusinessDataServiceHelper.loadSingle("716529547008326656", "bd_suppliergroupstandard");
                        group.set("groupid", entity.getDynamicObject("nckd_group"));
                        group.set("standardid", bdSupplierGroupStandard);

                        //联系人分录
                        DynamicObjectCollection entryLinkman = bdSupplier.getDynamicObjectCollection("entry_linkman");
                        DynamicObject linkman = entryLinkman.addNew();
                        linkman.set("contactperson", entity.get("nckd_linkman"));
                        linkman.set("phone", entity.get("nckd_phone"));
                        linkman.set("postalcode", entity.get("nckd_postalcode"));
                        linkman.set("isdefault_linkman", true);//默认

                        try {
                            OperationResult result = OperationServiceHelper.executeOperate("save", "bd_supplier", new DynamicObject[]{bdSupplier}, OperateOption.create());
                            if (!result.isSuccess()) {
                                StringBuilder stringBuilder = new StringBuilder();
                                List<IOperateInfo> allErrorOrValidateInfo = result.getAllErrorOrValidateInfo();
                                for (IOperateInfo iOperateInfo : allErrorOrValidateInfo) {
                                    stringBuilder.append(iOperateInfo.getMessage());
                                }
                                throw new KDBizException("保存信息到供应商失败：" + stringBuilder);
                            }
                            DynamicObject bd_address = BusinessDataServiceHelper.newDynamicObject("bd_address");
                            bd_address.set("number", number);
                            bd_address.set("name", entity.getString("nckd_addsupplier") + "地址");
                            bd_address.set("supplier", bdSupplier);
                            bd_address.set("detailaddress", entity.get("nckd_address"));
                            bd_address.set("phone", entity.get("nckd_phone"));
                            bd_address.set("zipcode", entity.get("nckd_postalcode"));
                            bd_address.set("supplierid", bdSupplier.getPkValue());
                            bd_address.set("issupplieradd", true);
                            bd_address.set("creator", date.getDynamicObject("creator"));
                            bd_address.set("createtime", new Date());
                            bd_address.set("createorg", bosOrg);
                            OperationServiceHelper.executeOperate("save", "bd_address", new DynamicObject[]{bd_address}, OperateOption.create());

                            //地址信息
                            DynamicObjectCollection entryAddress = bdSupplier.getDynamicObjectCollection("entry_address");
                            DynamicObject address = entryAddress.addNew();
                            address.set("addnumber", number);
                            address.set("addname", entity.getString("nckd_addsupplier") + "地址");
                            address.set("addphone", entity.get("nckd_phone"));
                            address.set("addpostalcode", entity.get("nckd_postalcode"));
                            address.set("addfulladdress", entity.get("nckd_address"));
                            address.set("default", true);
                            address.set("supplieraddress", bd_address);
                            DynamicObject linkman_e = bdSupplier.getDynamicObjectCollection("entry_linkman").get(0);
                            linkman_e.set("associatedaddress", bd_address);
                            SaveServiceHelper.update(bdSupplier);
                        } catch (Exception ex) {
                            throw new RuntimeException("保存信息到供应商失败：" + ex);
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
                        //查出更改前数据
                        DynamicObject bdSupplier = BusinessDataServiceHelper.loadSingle(entry.get(t - 1).getDynamicObject("nckd_suppliermodify").getPkValue(), "bd_supplier");
                        bdSupplier.set("name", entity.get("nckd_spname"));//名称
                        bdSupplier.set("modifier", date.getDynamicObject("creator"));//修改人
                        bdSupplier.set("modifytime", new Date());//修改时间
                        bdSupplier.set("group", entity.getDynamicObject("nckd_group"));//供应商分组
                        bdSupplier.set("societycreditcode", entity.get("nckd_societycreditcode"));//统一社会信用代码
                        bdSupplier.set("artificialperson", entity.get("nckd_artificialperson"));//法定代表人
                        bdSupplier.set("regcapital", entity.get("nckd_regcapital"));//注册资本
                        bdSupplier.set("blocsupplier", entity.getDynamicObject("nckd_basedatafield"));//归属集团供应商名称（实际控制人）
                        bdSupplier.set("linkman", entity.get("nckd_linkman"));//联系人
                        bdSupplier.set("bizpartner_phone", entity.get("nckd_phone"));//联系电话
                        bdSupplier.set("bizpartner_address", entity.get("nckd_address"));//联系地址
                        //bdSupplier.set("duns", entity.get("nckd_postalcode"));//邮政编码
                        //bdSupplier.set("purchaserid", entity.getDynamicObject("nckd_buyer"));//负责采购员
                        bdSupplier.set("nckd_unittype", entity.get("nckd_risk"));//风险属性
                        bdSupplier.set("nckd_suppliertype", entity.get("nckd_suppliertype"));//供应商类型
                        bdSupplier.set("nckd_cooperatestatus", entity.get("nckd_cooperatestatus"));//合作状态
                        if ("carrier".equals(date.get("nckd_merchanttype"))) {
                            bdSupplier.set("nckd_iscys", true);//是否承运商
                            bdSupplier.set("nckd_licensenumber", entity.get("nckd_licensenumber"));//运输许可证编号
                            bdSupplier.set("nckd_transporttype", entity.get("nckd_transporttype"));//运输方式
                            bdSupplier.set("nckd_rate", entity.getDynamicObject("nckd_rate"));//承运商税率
                        }
                        //银行分录信息保存
                        DynamicObjectCollection entryBank = bdSupplier.getDynamicObjectCollection("entry_bank");
                        DynamicObject bank = null;
                        if (entryBank.size() > 0){
                            bank = entryBank.get(0);
                        }else {
                            bank = entryBank.addNew();
                        }

                        bank.set("bankaccount", entity.get("nckd_bankaccount"));//银行账号
                        bank.set("accountname", entity.get("nckd_accountname"));//账户名称
                        bank.set("isdefault_bank", true);//默认
                        bank.set("bank", entity.getDynamicObject("nckd_bank"));//开户银行
                        bank.set("currency", entity.getDynamicObject("nckd_currency"));//币种
                        bank.set("nckd_acceptingbank", entity.getDynamicObject("nckd_acceptingbank"));//承兑银行

                        //供应商分类
                        DynamicObjectCollection groupStandard = bdSupplier.getDynamicObjectCollection("entry_groupstandard");
                        groupStandard.clear();
                        DynamicObject group = groupStandard.addNew();
                        DynamicObject bdSupplierGroupStandard = BusinessDataServiceHelper.loadSingle("716529547008326656", "bd_suppliergroupstandard");
                        group.set("groupid", entity.getDynamicObject("nckd_group"));
                        group.set("standardid", bdSupplierGroupStandard);

                        //联系人分录
                        DynamicObjectCollection entryLinkman = bdSupplier.getDynamicObjectCollection("entry_linkman");
                        entryLinkman.clear();
                        DynamicObject linkman = entryLinkman.addNew();
                        linkman.set("contactperson", entity.get("nckd_linkman"));
                        linkman.set("phone", entity.get("nckd_phone"));
                        linkman.set("postalcode", entity.get("nckd_postalcode"));
                        linkman.set("isdefault_linkman", true);//默认

                        if (!entity.get("nckd_spname").equals(bdSupplier.getString("name"))) {
                            DynamicObjectCollection nameVersionCol = bdSupplier.getDynamicObjectCollection("name$version");
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
                            OperationResult result = OperationServiceHelper.executeOperate("save", "bd_supplier", new DynamicObject[]{bdSupplier}, OperateOption.create());
                            if (!result.isSuccess()) {
                                StringBuilder stringBuilder = new StringBuilder();
                                List<IOperateInfo> allErrorOrValidateInfo = result.getAllErrorOrValidateInfo();
                                for (IOperateInfo iOperateInfo : allErrorOrValidateInfo) {
                                    stringBuilder.append(iOperateInfo.getMessage());
                                }
                                throw new KDBizException("更新信息到供应商失败：" + stringBuilder);
                            }
                            DynamicObject address = BusinessDataServiceHelper.loadSingle("bd_address","id,number,name,detailaddress,phone,zipcode,modifier,modifytime,supplierid",new QFilter[]{new QFilter("supplierid",QCP.equals,bdSupplier.getPkValue().toString())});
                            if (address != null) {
                                address.set("detailaddress", entity.get("nckd_address"));
                                address.set("number", bdSupplier.get("number"));
                                address.set("name", entity.getString("nckd_spname") + "地址");
                                address.set("phone", entity.get("nckd_phone"));
                                address.set("zipcode", entity.get("nckd_postalcode"));
                                address.set("modifier", date.getDynamicObject("creator"));
                                address.set("modifytime", new Date());
                                SaveServiceHelper.update(address);

                                DynamicObjectCollection entryAddress = bdSupplier.getDynamicObjectCollection("entry_address");
                                entryAddress.clear();
                                DynamicObject address_entry = entryAddress.addNew();
                                address_entry.set("addnumber", address.get("number"));
                                address_entry.set("addname", address.get("name"));
                                address_entry.set("addphone", address.get("phone"));
                                address_entry.set("addpostalcode", address.get("zipcode"));
                                address_entry.set("addfulladdress", address.get("detailaddress"));
                                address_entry.set("default", true);
                                address_entry.set("supplieraddress", address);
                                DynamicObject linkman_e = bdSupplier.getDynamicObjectCollection("entry_linkman").get(0);
                                linkman_e.set("associatedaddress", address);
                                SaveServiceHelper.update(bdSupplier);
                            }else {
                                DynamicObject bd_address = BusinessDataServiceHelper.newDynamicObject("bd_address");
                                bd_address.set("number", bdSupplier.get("number"));
                                bd_address.set("name", entity.getString("nckd_spname") + "地址");
                                bd_address.set("supplier", bdSupplier);
                                bd_address.set("detailaddress", entity.get("nckd_address"));
                                bd_address.set("phone", entity.get("nckd_phone"));
                                bd_address.set("zipcode", entity.get("nckd_postalcode"));
                                bd_address.set("supplierid", bdSupplier.getPkValue());
                                bd_address.set("issupplieradd", true);
                                bd_address.set("creator", date.getDynamicObject("creator"));
                                bd_address.set("createtime", new Date());
                                bd_address.set("createorg", bosOrg);
                                OperationServiceHelper.executeOperate("save", "bd_address", new DynamicObject[]{bd_address}, OperateOption.create());

                                //地址信息
                                DynamicObjectCollection entryAddress = bdSupplier.getDynamicObjectCollection("entry_address");
                                DynamicObject address_a = entryAddress.addNew();
                                address_a.set("addnumber", bdSupplier.get("number"));
                                address_a.set("addname", entity.getString("nckd_addsupplier") + "地址");
                                address_a.set("addphone", entity.get("nckd_phone"));
                                address_a.set("addpostalcode", entity.get("nckd_postalcode"));
                                address_a.set("addfulladdress", entity.get("nckd_address"));
                                address_a.set("default", true);
                                address_a.set("supplieraddress", bd_address);
                                DynamicObject linkman_e = bdSupplier.getDynamicObjectCollection("entry_linkman").get(0);
                                linkman_e.set("associatedaddress", bd_address);
                                SaveServiceHelper.update(bdSupplier);
                            }

                        } catch (Exception ex) {
                            throw new RuntimeException("保存信息到供应商失败：" + ex);
                        }
                    }
            }

        }

    }
}
