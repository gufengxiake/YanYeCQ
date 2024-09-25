package nckd.yanye.scm.plugin.operate;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 主数据-客商信息维护单提交操作插件
 * 表单标识：nckd_bd_supplier_change
 * author：xiaoxiaopeng
 * date：2024-08-26
 */
public class BdSupplierChangeSubmitOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_maintenancetype");
        fieldKeys.add("nckd_merchanttype");
        fieldKeys.add("nckd_changeafter");
        fieldKeys.add("nckd_merchanttype");

    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    DynamicObject data = dataEntity.getDataEntity();
                    String maintenancetype = data.getString("nckd_maintenancetype");
                    if ("update".equals(maintenancetype)) {
                        DynamicObjectCollection entry = data.getDynamicObjectCollection("nckd_entry");
                        if (entry.size() <= 0){
                            this.addErrorMessage(dataEntity, String.format("单据(%s)的明细信息为空，请先填写要修改信息",data.getString("billno")));
                        }
                        for (int i = 0; i < entry.size(); i++) {
                            DynamicObject entity = entry.get(i);
                            String changeAfter = entity.getString("nckd_changeafter");
                            if ("2".equals(changeAfter)){
                                DynamicObject suppliermodify = entity.getDynamicObject("nckd_suppliermodify");
                                if (suppliermodify == null){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：需修改供应商为空，请先填写供应商分组",data.getString("billno")));
                                }
                                DynamicObject group = entity.getDynamicObject("nckd_group");
                                if (group == null){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：供应商分组为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String spname = entity.getString("nckd_spname");
                                if (StringUtils.isEmpty(spname)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：供应商名称为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String societycreditcode = entity.getString("nckd_societycreditcode");
                                if (StringUtils.isEmpty(societycreditcode)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：统一社会信用代码为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String artificialperson = entity.getString("nckd_artificialperson");
                                if (StringUtils.isEmpty(artificialperson)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：法定代表人为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String regcapital = entity.getString("nckd_regcapital");
                                if (StringUtils.isEmpty(regcapital)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：注册资本为空，请先填写供应商分组",data.getString("billno")));
                                }
                                DynamicObject basedatafield = entity.getDynamicObject("nckd_basedatafield");
                                if (basedatafield == null){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：归属集团供应商名称（实际控制人）为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String linkman = entity.getString("nckd_linkman");
                                if (StringUtils.isEmpty(linkman)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：联系人为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String phone = entity.getString("nckd_phone");
                                if (StringUtils.isEmpty(phone)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：联系电话为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String address = entity.getString("nckd_address");
                                if (StringUtils.isEmpty(address)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：联系地址为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String postalcode = entity.getString("nckd_postalcode");
                                if (StringUtils.isEmpty(postalcode)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：邮政编码为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String risk = entity.getString("nckd_risk");
                                if (StringUtils.isEmpty(risk)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：风险属性为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String bankaccount = entity.getString("nckd_bankaccount");
                                if (StringUtils.isEmpty(bankaccount)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：银行账号为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String accountname = entity.getString("nckd_accountname");
                                if (StringUtils.isEmpty(accountname)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：账户名称为空，请先填写供应商分组",data.getString("billno")));
                                }
                                DynamicObject bank = entity.getDynamicObject("nckd_bank");
                                if (bank == null){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：开户银行为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String acceptingaccount = entity.getString("nckd_acceptingaccount");
                                if (StringUtils.isEmpty(acceptingaccount)){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：承兑银行账号为空，请先填写供应商分组",data.getString("billno")));
                                }
                                DynamicObject acceptingbank = entity.getDynamicObject("nckd_acceptingbank");
                                if (acceptingbank == null){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：承兑银行为空，请先填写供应商分组",data.getString("billno")));
                                }
                                DynamicObject currency = entity.getDynamicObject("nckd_currency");
                                if (currency == null){
                                    this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：币别为空，请先填写供应商分组",data.getString("billno")));
                                }
                                String merchanttype = data.getString("nckd_merchanttype");
                                if ("carrier".equals(merchanttype)){
                                    String licensenumber = entity.getString("nckd_licensenumber");
                                    if (StringUtils.isEmpty(licensenumber)){
                                        this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：运输许可证编号为空，请先填写供应商分组",data.getString("billno")));
                                    }
                                    String transporttype = entity.getString("nckd_transporttype");
                                    if (StringUtils.isEmpty(transporttype)){
                                        this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：运输方式为空，请先填写供应商分组",data.getString("billno")));
                                    }
                                    DynamicObject rate = entity.getDynamicObject("nckd_rate");
                                    if (rate == null){
                                        this.addErrorMessage(dataEntity, String.format("单据(%s)的修改明细信息：承运商税率为空，请先填写供应商分组",data.getString("billno")));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        StringBuilder stringBuilder = new StringBuilder();
        //单据提交进行将“社会统一信用代码”字段与供应商及客户档案中该字段进行查找，如有重复，则进行报错，提示与XX供应商或XX客户社会统一信用代码重复；
        for (int i = 0; i < dataEntities.length; i++) {
            DynamicObject date = dataEntities[i];
            date = BusinessDataServiceHelper.loadSingle(date.getPkValue(), "nckd_bd_supplier_change");
            DynamicObjectCollection entry = date.getDynamicObjectCollection("nckd_entry");
            String maintenanceType = date.getString("nckd_maintenancetype");
            switch (maintenanceType) {
                case "save":
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        //社会统一信用代码
                        String societyCreditCode = entity.getString("nckd_societycreditcode");
                        String addSupplier = entity.getString("nckd_addsupplier");
                        if (StringUtils.isBlank(societyCreditCode)) {
                            stringBuilder.append("提交单据第" + (i + 1) + "中维护客商信息表体第" + (t + 1) + "条数据中社会统一信用代码无效");
                            continue;
                        }
                        //先查供应商，供应商无重复再查客户，都无重复则放行，否则中断操作
                        DynamicObject bdSupplier = BusinessDataServiceHelper.loadSingle("bd_supplier", "id,societycreditcode",
                                new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                        if (bdSupplier != null) {
                            stringBuilder.append("提交单据第" + (i + 1) + "条中维护供应商信息表体第" + (t + 1) + "条数据中名字为" + addSupplier + "的供应商社会统一信用代码重复");
                            continue;
                        }
                        DynamicObject bdCustomer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,name,societycreditcode",
                                new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                        if (bdCustomer != null){
                            if (!addSupplier.equals(bdCustomer.getString("name"))){
                                stringBuilder.append("提交单据第" + (i+1) + "中维护供应商信息表体第" + (t+1) + "条数据中名字为" + addSupplier + "的供应商与名字为" +bdCustomer.getString("name") +  "的客户名字不一致");
                            }
                        }
                    }
                    break;
                case "update":
                    List<Object> pkList = new ArrayList<>();
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        String changeAfter = entity.getString("nckd_changeafter");
                        if ("1".equals(changeAfter)) {
                            DynamicObject supplierModify = entity.getDynamicObject("nckd_suppliermodify");
                            pkList.add(supplierModify.getPkValue());
                        }else {
                            //社会统一信用代码
                            String societyCreditCode = entity.getString("nckd_societycreditcode");
                            DynamicObject bdSupplier = BusinessDataServiceHelper.loadSingle("bd_supplier", "id,societycreditcode",
                                    new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                            if (bdSupplier != null && !pkList.contains(bdSupplier.getPkValue())) {
                                stringBuilder.append("提交单据第" + (i + 1) + "中维护客商信息表体第" + (t + 1) + "条数据中" + bdSupplier.getString("name") + "供应商社会统一信用代码重复");
                            }
                        }
                    }
                    break;
            }

                /*DynamicObject bdCustomer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,societycreditcode",
                        new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                if (bdCustomer != null){
                    stringBuilder.append("提交单据第" + (i+1) + "中维护客商信息表体第" + (t+1) + "条数据中" + bdCustomer.getString("name") + "客户社会统一信用代码重复");
                    break;
                }*/

        }
        if (stringBuilder.length() > 0) {
            e.setCancel(true);
            e.setCancelMessage(stringBuilder.toString());
        }
    }
}
