package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.dataentity.utils.ArrayUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.ComboProp;
import kd.bos.entity.property.TextProp;
import kd.bos.entity.property.UserProp;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
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
        //维护类型
        if ("nckd_maintenancetype".equals(fieldKey)) {
            String maintenanceType = this.getModel().getValue("nckd_maintenancetype").toString();
            TextEdit maintenanceTypeEdit = this.getControl("nckd_addsupplier");
            TextProp maintenanceTypeProp = (TextProp) maintenanceTypeEdit.getProperty();
            if ("save".equals(maintenanceType)) {
                maintenanceTypeEdit.setMustInput(true);
                maintenanceTypeProp.setMustInput(true);
            }else {
                maintenanceTypeEdit.setMustInput(false);
                maintenanceTypeProp.setMustInput(false);
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
            this.getModel().setValue("nckd_postalcode",supplier.get("duns"),index);
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
            this.getModel().setValue("nckd_postalcode",supplier.get("duns"),newIndex);
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
            this.getModel().setValue("nckd_postalcode",supplier.get("duns"),index);
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

}
