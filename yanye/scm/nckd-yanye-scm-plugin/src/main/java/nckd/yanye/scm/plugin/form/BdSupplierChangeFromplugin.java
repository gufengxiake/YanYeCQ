package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.dataentity.utils.ArrayUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.ComboProp;
import kd.bos.entity.property.TextProp;
import kd.bos.entity.property.UserProp;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.ComboEdit;
import kd.bos.form.field.TextEdit;
import kd.bos.form.field.UserEdit;
import kd.bos.form.operate.FormOperate;
import kd.bos.imageplatform.axis.IScanWebServiceImplServiceStub;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.List;

/**
 * 主数据-供应商信息维护单表单插件
 * 表单标识：nckd_bd_supplier_change
 * author：xiaoxiaopeng
 * date：2024-08-22
 */
public class BdSupplierChangeFromplugin extends AbstractBillPlugIn {

    //调整信息单据体字段
    private String[] ENTRYFIELD = {"nckd_changetype", "nckd_changeafter",
            "nckd_suppliermodify", "nckd_spname", "nckd_group", "nckd_societycreditcode", "nckd_artificialperson",
            "nckd_regcapital", "nckd_basedatafield", "nckd_linkman", "nckd_phone", "nckd_address", "nckd_postalcode",
            "nckd_buyer", "nckd_suppliertype", "nckd_risk", "nckd_bankaccount", "nckd_accountname", "nckd_bank",
            "nckd_acceptingaccount", "nckd_acceptingbank", "nckd_licensenumber", "nckd_transporttype", "nckd_rate",
            "nckd_currency","nckd_cooperatestatus"};

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //监听调整信息分录工具栏
        this.addItemClickListeners("advcontoolbarap");
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        IDataModel model = this.getModel();
        DynamicObjectCollection entry = model.getEntryEntity("nckd_entry");

        //根据申请人带出申请人部门
        DynamicObject creator = (DynamicObject) model.getValue("creator");
        creator = BusinessDataServiceHelper.loadSingle(creator.getPkValue(), "bos_user");
        DynamicObject deptEntry = creator.getDynamicObjectCollection("entryentity").get(0);
        model.setValue("nckd_dept", deptEntry.getDynamicObject("dpt").getPkValue());

        if (entry.size() > 0){
            this.getView().setEnable(false,"nckd_maintenancetype");
            this.getView().setEnable(false,"nckd_merchanttype");
            for (int i = 0; i < entry.size(); i++) {
                DynamicObject entity = entry.get(i);
                if ("1".equals(entity.getString("nckd_changeafter"))) {
                    this.getView().setEnable(false, i, ENTRYFIELD);
                }
            }
        }
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate source = (FormOperate) args.getSource();
        String key = source.getOperateKey();
        if ("submit".equals(key)){
            //verifySubmit(args);
        }
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        String key = evt.getItemKey();
        //监听新增分录按钮
        if (key.equals("tb_new")) {
            //校验表单数据
            Boolean result = verifyHeader();
            if (!result) {
                evt.setCancel(true);
            }
        }
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        DynamicObjectCollection entry = this.getModel().getEntryEntity("nckd_entry");
        String key = evt.getItemKey();
        //监听新增分录按钮
        if (key.equals("tb_new")) {
            setAddMaintenanceType();
        }
        //监听删除分录按钮
        if (key.equals("tb_del")) {
            setDelMaintenanceType();
        }
        //反审核
        if (key.equals("bar_unaudit")) {
            for (int i = 0; i < entry.size(); i++) {
                DynamicObject entity = entry.get(i);
                if ("1".equals(entity.getString("nckd_changeafter"))) {
                    this.getView().setEnable(false, i, ENTRYFIELD);
                }
            }
        }
        //撤销
        if (key.equals("bar_unsubmit")) {
            for (int i = 0; i < entry.size(); i++) {
                DynamicObject entity = entry.get(i);
                if ("1".equals(entity.getString("nckd_changeafter"))) {
                    this.getView().setEnable(false, i, ENTRYFIELD);
                }
            }
        }
    }

    private void setDelMaintenanceType() {
        DynamicObjectCollection entry = this.getModel().getEntryEntity("nckd_entry");
        if (entry.size() > 0 ) {
            return;
        }
        //如果删除了调整分录，且分录行为0，则解锁维护类型
        this.getView().setEnable(true,"nckd_maintenancetype");
        this.getView().setEnable(true,"nckd_merchanttype");
    }

    /**
     * 校验表头数据是否完整
     */
    private Boolean verifyHeader() {
        DynamicObject dataEntity = this.getModel().getDataEntity();
        String maintenanceType = dataEntity.getString("nckd_maintenancetype");
        String merchantType = dataEntity.getString("nckd_merchanttype");
        if (maintenanceType == null) {
            this.getView().showErrorNotification("请先选择维护类型");
            return false;
        }
        if (merchantType == null) {
            this.getView().showErrorNotification("请先选择客商类型");
            return false;
        }
        return true;
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        ChangeData[] changeSet = e.getChangeSet();
        int rowIndex = changeSet[0].getRowIndex();
        DynamicObject entry = this.getModel().getEntryRowEntity("nckd_entry",rowIndex);
        //维护类型
        if ("nckd_maintenancetype".equals(fieldKey)) {
            String maintenanceType = this.getModel().getValue("nckd_maintenancetype").toString();
            Object merchanttype = this.getModel().getValue("nckd_merchanttype");
            
            
            TextEdit maintenanceTypeEdit = this.getControl("nckd_addsupplier");
            TextProp maintenanceTypeProp = (TextProp) maintenanceTypeEdit.getProperty();
            ComboEdit changetypeEdit = this.getControl("nckd_changetype");//变更类型
            ComboProp changetypeProp = (ComboProp) changetypeEdit.getProperty();
            BasedataEdit groupEdit = this.getControl("nckd_group");//供应商分组
            BasedataProp groupProp = (BasedataProp) groupEdit.getProperty();
            TextEdit societycreditcodeEdit = this.getControl("nckd_societycreditcode");//统一社会信用代码
            TextProp societycreditcodeProp = (TextProp) societycreditcodeEdit.getProperty();
            TextEdit artificialpersonEdit = this.getControl("nckd_artificialperson");//法定代表人
            TextProp artificialpersonProp = (TextProp) artificialpersonEdit.getProperty();
            TextEdit regcapitalEdit = this.getControl("nckd_regcapital");//注册资本
            TextProp regcapitalProp = (TextProp) regcapitalEdit.getProperty();
            TextEdit linkmanEdit = this.getControl("nckd_linkman");//联系人
            TextProp linkmanProp = (TextProp) linkmanEdit.getProperty();
            TextEdit phoneEdit = this.getControl("nckd_phone");//联系电话
            TextProp phoneProp = (TextProp) phoneEdit.getProperty();
            TextEdit addressEdit = this.getControl("nckd_address");//联系地址
            TextProp addressProp = (TextProp) addressEdit.getProperty();
            TextEdit postalcodeEdit = this.getControl("nckd_postalcode");//邮政编码
            TextProp postalcodeProp = (TextProp) postalcodeEdit.getProperty();
            ComboEdit riskEdit = this.getControl("nckd_risk");//风险属性
            ComboProp riskProp = (ComboProp) riskEdit.getProperty();
            TextEdit bankaccountEdit = this.getControl("nckd_bankaccount");//银行账号
            TextProp bankaccountProp = (TextProp) bankaccountEdit.getProperty();
            TextEdit accountnameEdit = this.getControl("nckd_accountname");//账户名称
            TextProp accountnameProp = (TextProp) accountnameEdit.getProperty();
            BasedataEdit bankEdit = this.getControl("nckd_bank");//开户银行
            BasedataProp bankProp = (BasedataProp) bankEdit.getProperty();
            TextEdit acceptingaccountEdit = this.getControl("nckd_acceptingaccount");//承兑银行账号
            TextProp acceptingaccountProp = (TextProp) acceptingaccountEdit.getProperty();
            BasedataEdit acceptingbankEdit = this.getControl("nckd_acceptingbank");//承兑银行
            BasedataProp acceptingbankProp = (BasedataProp) acceptingbankEdit.getProperty();
            BasedataEdit currencyEdit = this.getControl("nckd_currency");//币别
            BasedataProp currencyProp = (BasedataProp) currencyEdit.getProperty();
            TextEdit licenseNumberEdit = this.getControl("nckd_licensenumber");
            TextProp licenseNumberProp = (TextProp) licenseNumberEdit.getProperty();
            ComboEdit transportTypeEdit = this.getControl("nckd_transporttype");
            ComboProp transportTypeProp = (ComboProp) transportTypeEdit.getProperty();
            BasedataEdit rateEdit = this.getControl("nckd_rate");
            BasedataProp rateProp = (BasedataProp) rateEdit.getProperty();

            if ("save".equals(maintenanceType)) {
                maintenanceTypeEdit.setMustInput(true);
                maintenanceTypeProp.setMustInput(true);
                changetypeEdit.setMustInput(true);
                changetypeProp.setMustInput(true);
                groupEdit.setMustInput(true);
                groupProp.setMustInput(true);
                societycreditcodeEdit.setMustInput(true);
                societycreditcodeProp.setMustInput(true);
                artificialpersonEdit.setMustInput(true);
                artificialpersonProp.setMustInput(true);
                regcapitalEdit.setMustInput(true);
                regcapitalProp.setMustInput(true);
                linkmanEdit.setMustInput(true);
                linkmanProp.setMustInput(true);
                phoneEdit.setMustInput(true);
                phoneProp.setMustInput(true);
                addressEdit.setMustInput(true);
                addressProp.setMustInput(true);
                postalcodeEdit.setMustInput(true);
                postalcodeProp.setMustInput(true);
                riskEdit.setMustInput(true);
                riskProp.setMustInput(true);
                bankaccountEdit.setMustInput(true);
                bankaccountProp.setMustInput(true);
                accountnameEdit.setMustInput(true);
                accountnameProp.setMustInput(true);
                bankEdit.setMustInput(true);
                bankProp.setMustInput(true);
                acceptingaccountEdit.setMustInput(true);
                acceptingaccountProp.setMustInput(true);
                acceptingbankEdit.setMustInput(true);
                acceptingbankProp.setMustInput(true);
                currencyEdit.setMustInput(true);
                currencyProp.setMustInput(true);
                if ("supplier".equals(merchanttype)){
                    licenseNumberEdit.setMustInput(false);
                    licenseNumberProp.setMustInput(false);
                    transportTypeEdit.setMustInput(false);
                    transportTypeProp.setMustInput(false);
                    rateEdit.setMustInput(false);
                    rateProp.setMustInput(false);
                } else if ("carrier".equals(merchanttype)) {
                    licenseNumberEdit.setMustInput(true);
                    licenseNumberProp.setMustInput(true);
                    transportTypeEdit.setMustInput(true);
                    transportTypeProp.setMustInput(true);
                    rateEdit.setMustInput(true);
                    rateProp.setMustInput(true);
                }
            }else {
                maintenanceTypeEdit.setMustInput(true);
                maintenanceTypeProp.setMustInput(false);

                changetypeEdit.setMustInput(true);
                changetypeProp.setMustInput(false);
                groupEdit.setMustInput(true);
                groupProp.setMustInput(false);
                societycreditcodeEdit.setMustInput(true);
                societycreditcodeProp.setMustInput(false);
                artificialpersonEdit.setMustInput(true);
                artificialpersonProp.setMustInput(false);
                regcapitalEdit.setMustInput(true);
                regcapitalProp.setMustInput(false);
                linkmanEdit.setMustInput(true);
                linkmanProp.setMustInput(false);
                phoneEdit.setMustInput(true);
                phoneProp.setMustInput(false);
                addressEdit.setMustInput(true);
                addressProp.setMustInput(false);
                postalcodeEdit.setMustInput(true);
                postalcodeProp.setMustInput(false);
                riskEdit.setMustInput(true);
                riskProp.setMustInput(false);
                bankaccountEdit.setMustInput(true);
                bankaccountProp.setMustInput(false);
                accountnameEdit.setMustInput(true);
                accountnameProp.setMustInput(false);
                bankEdit.setMustInput(true);
                bankProp.setMustInput(false);
                acceptingaccountEdit.setMustInput(true);
                acceptingaccountProp.setMustInput(false);
                acceptingbankEdit.setMustInput(true);
                acceptingbankProp.setMustInput(false);
                currencyEdit.setMustInput(true);
                currencyProp.setMustInput(false);
                if (merchanttype != null && "supplier".equals(merchanttype.toString())){
                    licenseNumberEdit.setMustInput(false);
                    licenseNumberProp.setMustInput(false);
                    transportTypeEdit.setMustInput(false);
                    transportTypeProp.setMustInput(false);
                    rateEdit.setMustInput(false);
                    rateProp.setMustInput(false);
                } else if (merchanttype != null && "carrier".equals(merchanttype.toString())) {
                    licenseNumberEdit.setMustInput(true);
                    licenseNumberProp.setMustInput(false);
                    transportTypeEdit.setMustInput(true);
                    transportTypeProp.setMustInput(false);
                    rateEdit.setMustInput(true);
                    rateProp.setMustInput(false);
                }
            }
        }
        //客商类型
        if ("nckd_merchanttype".equals(fieldKey)) {
            setMerchantType();
        }
        //需修改供应商
        if ("nckd_suppliermodify".equals(fieldKey)) {
            setSupplierModify(rowIndex);
        }
        //开户银行
        if ("nckd_bank".equals(fieldKey)) {
            Object changeafter = this.getModel().getValue("nckd_changeafter", rowIndex);
            if ("1".equals(changeafter)) {
                DynamicObject supplier = (DynamicObject) this.getModel().getValue("nckd_suppliermodify",rowIndex);
                supplier = BusinessDataServiceHelper.loadSingle(supplier.getPkValue(), "bd_supplier");
                DynamicObjectCollection entryBank = supplier.getDynamicObjectCollection("entry_bank");
                if (entryBank.size() <= 0) {
                    this.getModel().setValue("nckd_bankaccount",null,rowIndex);
                    this.getModel().setValue("nckd_bank",null,rowIndex);
                    this.getModel().setValue("nckd_accountname",null,rowIndex);
                    return;
                }
            }
            DynamicObject bank = (DynamicObject) this.getModel().getValue("nckd_bank",rowIndex);
            if (bank != null) {
                this.getModel().setValue("nckd_bankaccount",bank.getString("number"),rowIndex);
                this.getModel().setValue("nckd_accountname",bank.getString("name"),rowIndex);
            }
        }
        //承兑银行
        if ("nckd_acceptingbank".equals(fieldKey)) {
            Object changeafter = this.getModel().getValue("nckd_changeafter", rowIndex);
            if ("1".equals(changeafter)) {
                DynamicObject supplier = (DynamicObject) this.getModel().getValue("nckd_suppliermodify",rowIndex);
                supplier = BusinessDataServiceHelper.loadSingle(supplier.getPkValue(), "bd_supplier");
                DynamicObjectCollection entryBank = supplier.getDynamicObjectCollection("entry_bank");
                if (entryBank.size() <= 0) {
                    this.getModel().setValue("nckd_acceptingbank",null,rowIndex);
                    this.getModel().setValue("nckd_acceptingaccount",null,rowIndex);
                    return;
                }
            }
            DynamicObject acceptingbank = (DynamicObject) this.getModel().getValue("nckd_acceptingbank",rowIndex);
            if (acceptingbank != null) {
                this.getModel().setValue("nckd_acceptingaccount",acceptingbank.getString("number"),rowIndex);
            }
        }
    }

    /**
     * 根据需修改供应商带出相关字段
     */
    private void setSupplierModify(int rowIndex) {
        int index = this.getModel().getEntryCurrentRowIndex("nckd_entry");
        DynamicObject supplier = (DynamicObject) this.getModel().getValue("nckd_suppliermodify", index);
        if (supplier == null) {
            return;
        }
        supplier = BusinessDataServiceHelper.loadSingle(supplier.getPkValue(), "bd_supplier");
        DynamicObject bankEntry = supplier.getDynamicObjectCollection("entry_bank").size() <= 0 ? null : supplier.getDynamicObjectCollection("entry_bank").get(0);
        Object changeAfter = this.getModel().getValue("nckd_changeafter", rowIndex);
        Object merchantType = this.getModel().getValue("nckd_merchanttype");

        //判断值改变行是否是当前选中行，不是的话直接中断操作
        if (rowIndex == index && changeAfter == null){
            this.getModel().setValue("nckd_changeafter","1",index);
            this.getModel().setValue("nckd_spname",supplier.get("name"),index);
            this.getModel().setValue("nckd_group",supplier.getDynamicObject("group"),index);
            this.getModel().setValue("nckd_societycreditcode",supplier.get("societycreditcode"),index);
            this.getModel().setValue("nckd_artificialperson",supplier.get("artificialperson"),index);
            this.getModel().setValue("nckd_regcapital",supplier.get("regcapital"),index);
            this.getModel().setValue("nckd_basedatafield",supplier,index);
            this.getModel().setValue("nckd_linkman",supplier.get("linkman"),index);
            this.getModel().setValue("nckd_phone",supplier.get("bizpartner_phone"),index);
            this.getModel().setValue("nckd_address",supplier.get("bizpartner_address"),index);
            this.getModel().setValue("nckd_postalcode",supplier.getDynamicObjectCollection("entry_linkman").size() <= 0 ? null : supplier.getDynamicObjectCollection("entry_linkman").get(0).getString("postalcode"),index);
            //this.getModel().setValue("nckd_buyer",supplier.getDynamicObject("purchaserid"),index);
            this.getModel().setValue("nckd_suppliertype","factory",index);
            this.getModel().setValue("nckd_risk",supplier.get("nckd_unittype"),index);
            this.getModel().setValue("nckd_cooperatestatus",supplier.get("nckd_cooperatestatus"),index);
            this.getModel().setValue("nckd_bankaccount",bankEntry == null ? null : bankEntry.get("bankaccount"),index);
            this.getModel().setValue("nckd_currency",bankEntry == null ? null : bankEntry.getDynamicObject("currency"),index);
            this.getModel().setValue("nckd_accountname",bankEntry == null ? null : bankEntry.get("accountname"),index);
            this.getModel().setValue("nckd_bank",bankEntry == null ? null : bankEntry.getDynamicObject("bank"),index);
            this.getModel().setValue("nckd_acceptingaccount",bankEntry == null ? null : bankEntry.getDynamicObject("nckd_acceptingbank").get("number"),index);
            this.getModel().setValue("nckd_acceptingbank",bankEntry == null ? null : bankEntry.getDynamicObject("nckd_acceptingbank"),index);
            if ("carrier".equals(merchantType)) {
                this.getModel().setValue("nckd_licensenumber",supplier.get("nckd_licensenumber"),index);
                this.getModel().setValue("nckd_transporttype",supplier.get("nckd_transporttype"),index);
                this.getModel().setValue("nckd_rate",supplier.getDynamicObject("nckd_rate"),index);
            }
            //锁定当前分录行
            this.getView().setEnable(false, index, ENTRYFIELD);
            //复制当前行可修改
            int newIndex = this.getModel().insertEntryRow("nckd_entry", index + 1);
            this.getModel().setValue("nckd_suppliermodify",supplier,newIndex);
            this.getModel().setValue("nckd_changetype","update",newIndex);
            this.getModel().setValue("nckd_changeafter","2",newIndex);
            this.getModel().setValue("nckd_spname",supplier.get("name"),newIndex);
            this.getModel().setValue("nckd_group",supplier.getDynamicObject("group"),newIndex);
            this.getModel().setValue("nckd_societycreditcode",supplier.get("societycreditcode"),newIndex);
            this.getModel().setValue("nckd_artificialperson",supplier.get("artificialperson"),newIndex);
            this.getModel().setValue("nckd_regcapital",supplier.get("regcapital"),newIndex);
            this.getModel().setValue("nckd_basedatafield",supplier,newIndex);
            this.getModel().setValue("nckd_linkman",supplier.get("linkman"),newIndex);
            this.getModel().setValue("nckd_phone",supplier.get("bizpartner_phone"),newIndex);
            this.getModel().setValue("nckd_address",supplier.get("bizpartner_address"),newIndex);
            this.getModel().setValue("nckd_postalcode",supplier.getDynamicObjectCollection("entry_linkman").size() <= 0 ? null : supplier.getDynamicObjectCollection("entry_linkman").get(0).getString("postalcode"),newIndex);
            //this.getModel().setValue("nckd_buyer",supplier.getDynamicObject("purchaserid"),newIndex);
            this.getModel().setValue("nckd_suppliertype","factory",newIndex);
            this.getModel().setValue("nckd_risk",supplier.get("nckd_unittype"),newIndex);
            this.getModel().setValue("nckd_cooperatestatus",supplier.get("nckd_cooperatestatus"),newIndex);
            this.getModel().setValue("nckd_bankaccount",bankEntry == null ? null : bankEntry.get("bankaccount"),newIndex);
            this.getModel().setValue("nckd_currency",bankEntry == null ? null : bankEntry.getDynamicObject("currency"),newIndex);
            this.getModel().setValue("nckd_accountname",bankEntry == null ? null : bankEntry.get("accountname"),newIndex);
            this.getModel().setValue("nckd_bank",bankEntry == null ? null : bankEntry.getDynamicObject("bank"),newIndex);
            this.getModel().setValue("nckd_acceptingaccount",bankEntry == null ? null : bankEntry.getDynamicObject("nckd_acceptingbank").get("number"),newIndex);
            this.getModel().setValue("nckd_acceptingbank",bankEntry == null ? null : bankEntry.getDynamicObject("nckd_acceptingbank"),newIndex);
            if ("carrier".equals(merchantType)) {
                this.getModel().setValue("nckd_licensenumber",supplier.get("nckd_licensenumber"),newIndex);
                this.getModel().setValue("nckd_transporttype",supplier.get("nckd_transporttype"),newIndex);
                this.getModel().setValue("nckd_rate",supplier.getDynamicObject("nckd_rate"),newIndex);
            }
            return;
        }

        //判断当前选中行是否是可修改行，不是的话无需新增
        if (changeAfter != null && "2".equals(changeAfter)) {
            this.getModel().setValue("nckd_spname",supplier.get("name"),index);
            this.getModel().setValue("nckd_group",supplier.getDynamicObject("group"),index);
            this.getModel().setValue("nckd_societycreditcode",supplier.get("societycreditcode"),index);
            this.getModel().setValue("nckd_artificialperson",supplier.get("artificialperson"),index);
            this.getModel().setValue("nckd_regcapital",supplier.get("regcapital"),index);
            this.getModel().setValue("nckd_basedatafield",supplier,index);
            this.getModel().setValue("nckd_linkman",supplier.get("linkman"),index);
            this.getModel().setValue("nckd_phone",supplier.get("bizpartner_phone"),index);
            this.getModel().setValue("nckd_address",supplier.get("bizpartner_address"),index);
            this.getModel().setValue("nckd_postalcode",supplier.getDynamicObjectCollection("entry_linkman").size() <= 0 ? null : supplier.getDynamicObjectCollection("entry_linkman").get(0).getString("postalcode"),index);
            //this.getModel().setValue("nckd_buyer",supplier.getDynamicObject("purchaserid"),index);
            this.getModel().setValue("nckd_suppliertype","factory",index);
            this.getModel().setValue("nckd_risk",supplier.get("nckd_unittype"),index);
            this.getModel().setValue("nckd_bankaccount",bankEntry == null ? null : bankEntry.get("bankaccount"),index);
            this.getModel().setValue("nckd_accountname",bankEntry == null ? null : bankEntry.get("accountname"),index);
            this.getModel().setValue("nckd_bank",bankEntry == null ? null : bankEntry.getDynamicObject("bank"),index);
            this.getModel().setValue("nckd_acceptingaccount",bankEntry == null ? null : bankEntry.getDynamicObject("nckd_acceptingbank").get("number"),index);
            this.getModel().setValue("nckd_acceptingbank",bankEntry == null ? null : bankEntry.getDynamicObject("nckd_acceptingbank"),index);
            if ("carrier".equals(merchantType)) {
                this.getModel().setValue("nckd_licensenumber",supplier.get("nckd_licensenumber"),index);
                this.getModel().setValue("nckd_transporttype",supplier.get("nckd_transporttype"),index);
                this.getModel().setValue("nckd_rate",supplier.getDynamicObject("nckd_rate"),index);
            }
        }
    }

    private void setMerchantType() {
        Object merchantType = this.getModel().getValue("nckd_merchanttype");
        if (merchantType == null) {
            this.getView().setVisible(true, "nckd_licensenumber");
            this.getView().setVisible(true, "nckd_transporttype");
            this.getView().setVisible(true, "nckd_rate");
            return;
        }
        //UserEdit buyerEdit = this.getControl("nckd_buyer");
        //UserProp buyerProp = (UserProp) buyerEdit.getProperty();
        ComboEdit supplierTypeEdit = this.getControl("nckd_suppliertype");
        ComboProp supplierTypeProp = (ComboProp) supplierTypeEdit.getProperty();
        TextEdit licenseNumberEdit = this.getControl("nckd_licensenumber");
        TextProp licenseNumberProp = (TextProp) licenseNumberEdit.getProperty();
        ComboEdit transportTypeEdit = this.getControl("nckd_transporttype");
        ComboProp transportTypeProp = (ComboProp) transportTypeEdit.getProperty();
        BasedataEdit rateEdit = this.getControl("nckd_rate");
        BasedataProp rateProp = (BasedataProp) rateEdit.getProperty();

        switch (merchantType.toString()) {
            //供应商
            case "supplier":
                this.getView().setVisible(false, "nckd_licensenumber");
                this.getView().setVisible(false, "nckd_transporttype");
                this.getView().setVisible(false, "nckd_rate");

                //设置供应商相关字段必录
                //buyerEdit.setMustInput(true);
                //buyerProp.setMustInput(true);
                supplierTypeEdit.setMustInput(true);
                supplierTypeProp.setMustInput(true);

                //取消承运商相关字段必录
                licenseNumberEdit.setMustInput(false);
                licenseNumberProp.setMustInput(false);
                transportTypeEdit.setMustInput(false);
                transportTypeProp.setMustInput(false);
                rateEdit.setMustInput(false);
                rateProp.setMustInput(false);
                break;
            //承运商
            case "carrier":
                this.getView().setVisible(true, "nckd_licensenumber");
                this.getView().setVisible(true, "nckd_transporttype");
                this.getView().setVisible(true, "nckd_rate");

                Object maintenancetype = this.getModel().getValue("nckd_maintenancetype");
                if (maintenancetype != null && "save".equals(maintenancetype)) {
                    //取消供应商相关字段必录
                    //buyerEdit.setMustInput(false);
                    //buyerProp.setMustInput(false);
                    supplierTypeEdit.setMustInput(false);
                    supplierTypeProp.setMustInput(false);

                    //设置承运商相关字段必录
                    licenseNumberEdit.setMustInput(true);
                    licenseNumberProp.setMustInput(true);
                    transportTypeEdit.setMustInput(true);
                    transportTypeProp.setMustInput(true);
                    rateEdit.setMustInput(true);
                    rateProp.setMustInput(true);
                } else if (maintenancetype != null && "update".equals(maintenancetype)) {
                    //取消供应商相关字段必录
                    //buyerEdit.setMustInput(false);
                    //buyerProp.setMustInput(false);
                    supplierTypeEdit.setMustInput(false);
                    supplierTypeProp.setMustInput(false);

                    //设置承运商相关字段必录
                    licenseNumberEdit.setMustInput(false);
                    licenseNumberProp.setMustInput(false);
                    transportTypeEdit.setMustInput(false);
                    transportTypeProp.setMustInput(false);
                    rateEdit.setMustInput(false);
                    rateProp.setMustInput(false);
                }else {
                    //取消供应商相关字段必录
                    //buyerEdit.setMustInput(false);
                    //buyerProp.setMustInput(false);
                    supplierTypeEdit.setMustInput(false);
                    supplierTypeProp.setMustInput(false);

                    //设置承运商相关字段必录
                    licenseNumberEdit.setMustInput(true);
                    licenseNumberProp.setMustInput(true);
                    transportTypeEdit.setMustInput(true);
                    transportTypeProp.setMustInput(true);
                    rateEdit.setMustInput(true);
                    rateProp.setMustInput(true);
                }
                break;
            //客户
            case "customer":
                break;
        }
    }

    private void setAddMaintenanceType() {
        DynamicObjectCollection entry = this.getModel().getEntryEntity("nckd_entry");
        String maintenanceType = this.getModel().getValue("nckd_maintenancetype").toString();
        switch (maintenanceType) {
            //新增
            case "save":
                //如果新增了调整分录，则锁定维护类型
                this.getView().setEnable(false,"nckd_maintenancetype");
                this.getView().setEnable(false,"nckd_merchanttype");
                for (int i = 0; i < entry.size(); i++) {
                    this.getModel().setValue("nckd_changetype", maintenanceType, i);
                }
            break;
            //修改
            case "update":
                //如果新增了调整分录，则锁定维护类型
                this.getView().setEnable(false,"nckd_maintenancetype");
                this.getView().setEnable(false,"nckd_merchanttype");
                for (int i = 0; i < entry.size(); i++) {
                    this.getModel().setValue("nckd_changetype", maintenanceType, i);
                }
                break;
        }
    }

    /**
     * 单据提交校验
     * @param args
     */
    private void verifySubmit(BeforeDoOperationEventArgs args) {
        ListSelectedRowCollection data = args.getListSelectedData();
        if (data.size() <= 0){
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        //单据提交进行将“社会统一信用代码”字段与供应商及客户档案中该字段进行查找，如有重复，则进行报错，提示与XX供应商或XX客户社会统一信用代码重复；
        for (int i = 0; i < data.size(); i++) {
            //当前只考虑新增单据
            DynamicObject bdSupplierChange = BusinessDataServiceHelper.loadSingle(data.get(i).getPrimaryKeyValue(), "nckd_bd_supplier_change");
            if ("update".equals(bdSupplierChange.getString("nckd_maintenancetype"))){
                break;
            }
            DynamicObjectCollection entry = bdSupplierChange.getDynamicObjectCollection("nckd_entry");
            if (entry.size() <= 0){
                break;
            }
            for (int t = 0; t < entry.size(); t++) {
                DynamicObject entity = entry.get(t);
                //社会统一信用代码
                String societyCreditCode = entity.getString("nckd_societycreditcode");
                if (StringUtils.isBlank(societyCreditCode)){
                    stringBuilder.append("提交单据第" + (i+1) + "中维护客商信息表体第" + (t+1) + "条数据中社会统一信用代码无效");
                    break;
                }
                //先查供应商，供应商无重复再查客户，都无重复则放行，否则中断操作
                DynamicObject bdSupplier = BusinessDataServiceHelper.loadSingle("bd_supplier", "id,societycreditcode",
                        new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                if (bdSupplier != null){
                    stringBuilder.append("提交单据第" + (i+1) + "中维护客商信息表体第" + (t+1) + "条数据中" + bdSupplier.getString("name") + "供应商社会统一信用代码重复");
                    break;
                }
                DynamicObject bdCustomer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,societycreditcode",
                        new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                if (bdCustomer != null){
                    stringBuilder.append("提交单据第" + (i+1) + "中维护客商信息表体第" + (t+1) + "条数据中" + bdCustomer.getString("name") + "客户社会统一信用代码重复");
                    break;
                }
            }
        }
        if (stringBuilder.length() > 0){
            this.getView().showErrorNotification(stringBuilder.toString());
            args.setCancel(true);
        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        DynamicObjectCollection entity = this.getModel().getEntryEntity("nckd_entry");
        String key = e.getOperateKey();
        if ("audit".equals(key)){
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                String nckdMerchanttype = this.getModel().getValue("nckd_merchanttype").toString();
                if ("carrier".equals(nckdMerchanttype)){
                    if (entity.size() > 0){
                        for (DynamicObject entry : entity) {
                            String changeAfter = entry.getString("nckd_changeafter");
                            if (StringUtils.isEmpty(changeAfter)){
                                String addSupplier = entry.getString("nckd_addsupplier");
                                DynamicObject supplier = BusinessDataServiceHelper.loadSingle("bd_supplier","id",new QFilter[]{new QFilter("name",QCP.equals,addSupplier)});
                                OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_supplier", new DynamicObject[]{supplier}, OperateOption.create());
                                if (!pushzhwl.isSuccess()){
                                    this.getView().showMessage(pushzhwl.getMessage());
                                }
                                continue;
                            }
                            if ("2".equals(changeAfter)){
                                DynamicObject supplier = entry.getDynamicObject("nckd_suppliermodify");
                                supplier = BusinessDataServiceHelper.loadSingle(supplier.getPkValue(),"bd_supplier");
                                OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_supplier", new DynamicObject[]{supplier}, OperateOption.create());
                                if (!pushzhwl.isSuccess()){
                                    this.getView().showMessage(pushzhwl.getMessage());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
