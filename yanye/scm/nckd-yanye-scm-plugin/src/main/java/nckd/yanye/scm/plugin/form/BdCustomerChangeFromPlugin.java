package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.TextProp;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.EventObject;


/**
 * 主数据-客户信息维护单表单插件
 * 表单标识：nckd_bd_customer_change
 * author：xiaoxiaopeng
 * date：2024-08-29
 */
public class BdCustomerChangeFromPlugin extends AbstractBillPlugIn {

    //调整信息单据体字段
    private String[] ENTRYFIELD = {"nckd_changetype", "nckd_changeafter",
            "nckd_addcustomer","nckd_customermodify" ,"nckd_spname","nckd_customerrange",  "nckd_province", "nckd_city",
            "nckd_district", "nckd_businesstype", "nckd_quality", "nckd_societycreditcode", "nckd_basedatafield", "nckd_buyer",
            "nckd_cooperatestatus", "nckd_linkman", "nckd_phone", "nckd_postalcode", "nckd_address", "nckd_bankaccount",
            "nckd_accountname", "nckd_bank", "nckd_currency", "nckd_taxpayertype", "nckd_invoicename","nckd_banknumber",
            "nckd_invoicetype","nckd_addressorphone","nckd_invoicephone","nckd_enterpriseemail","nckd_customeremail","nckd_myemail"};


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
            for (int i = 0; i < entry.size(); i++) {
                DynamicObject entity = entry.get(i);
                if ("1".equals(entity.getString("nckd_changeafter"))) {
                    this.getView().setEnable(false, i, ENTRYFIELD);
                }
            }
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

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        ChangeData[] changeSet = e.getChangeSet();
        int rowIndex = changeSet[0].getRowIndex();
        //维护类型
        if ("nckd_maintenancetype".equals(fieldKey)) {
            String maintenanceType = this.getModel().getValue("nckd_maintenancetype").toString();
            TextEdit maintenanceTypeEdit = this.getControl("nckd_addcustomer");
            TextProp maintenanceTypeProp = (TextProp) maintenanceTypeEdit.getProperty();
            if ("save".equals(maintenanceType)) {
                maintenanceTypeEdit.setMustInput(true);
                maintenanceTypeProp.setMustInput(true);
            }else {
                maintenanceTypeEdit.setMustInput(false);
                maintenanceTypeProp.setMustInput(false);
            }
        }
        //需修改供应商
        if ("nckd_customermodify".equals(fieldKey)) {
            setSupplierModify(rowIndex);
        }
    }

    /**
     * 校验表头数据是否完整
     */
    private Boolean verifyHeader() {
        DynamicObject dataEntity = this.getModel().getDataEntity();
        String maintenanceType = dataEntity.getString("nckd_maintenancetype");
        if (maintenanceType == null) {
            this.getView().showErrorNotification("请先选择维护类型");
            return false;
        }
        return true;
    }

    private void setAddMaintenanceType() {
        DynamicObjectCollection entry = this.getModel().getEntryEntity("nckd_entry");
        String maintenanceType = this.getModel().getValue("nckd_maintenancetype").toString();
        switch (maintenanceType) {
            //新增
            case "save":
                //如果新增了调整分录，则锁定维护类型
                this.getView().setEnable(false,"nckd_maintenancetype");
                for (int i = 0; i < entry.size(); i++) {
                    this.getModel().setValue("nckd_changetype", maintenanceType, i);
                }
                break;
            //修改
            case "update":
                //如果新增了调整分录，则锁定维护类型
                this.getView().setEnable(false,"nckd_maintenancetype");
                for (int i = 0; i < entry.size(); i++) {
                    this.getModel().setValue("nckd_changetype", maintenanceType, i);
                }
                break;
        }
    }

    private void setDelMaintenanceType() {
        DynamicObjectCollection entry = this.getModel().getEntryEntity("nckd_entry");
        if (entry.size() > 0 ) {
            return;
        }
        //如果删除了调整分录，且分录行为0，则解锁维护类型
        this.getView().setEnable(true,"nckd_maintenancetype");
    }


    /**
     * 根据需修改供应商带出相关字段
     */
    private void setSupplierModify(int rowIndex) {
        int index = this.getModel().getEntryCurrentRowIndex("nckd_entry");
        DynamicObject customer = (DynamicObject) this.getModel().getValue("nckd_customermodify", index);
        if (customer == null) {
            return;
        }
        customer = BusinessDataServiceHelper.loadSingle(customer.getPkValue(), "bd_customer");
        DynamicObject bankEntry = customer.getDynamicObjectCollection("entry_bank").size() <= 0 ? null : customer.getDynamicObjectCollection("entry_bank").get(0);
        DynamicObject linkmanEntry = customer.getDynamicObjectCollection("entry_linkman").size() <= 0 ? null : customer.getDynamicObjectCollection("entry_linkman").get(0);
        Object changeAfter = this.getModel().getValue("nckd_changeafter", rowIndex);

        //判断值改变行是否是当前选中行，不是的话直接中断操作
        if (rowIndex == index && changeAfter == null){
            this.getModel().setValue("nckd_changeafter","1",index);
            this.getModel().setValue("nckd_spname",customer.get("name"),index);
            this.getModel().setValue("nckd_customerrange", customer.getDynamicObject("nckd_customertype"),index);//客户范围
            this.getModel().setValue("nckd_province",customer.get("admindivision"),index);
            this.getModel().setValue("nckd_businesstype",customer.get("nckd_v"),index);
            this.getModel().setValue("nckd_quality",customer.get("nckd_customerxz"),index);
            this.getModel().setValue("nckd_societycreditcode",customer.get("societycreditcode"),index);
            this.getModel().setValue("nckd_basedatafield",customer,index);
            this.getModel().setValue("nckd_buyer",customer.get("salerid"),index);
            this.getModel().setValue("nckd_cooperatestatus",getCooperateStatus(customer.getString("nckd_cooperationstatus")),index);
            this.getModel().setValue("nckd_linkman",linkmanEntry == null ? null : linkmanEntry.get("contactperson"),index);
            this.getModel().setValue("nckd_phone",linkmanEntry == null ? null : linkmanEntry.get("phone"),index);
            this.getModel().setValue("nckd_postalcode",customer.get("duns"),index);
            this.getModel().setValue("nckd_address",customer.get("bizpartner_address"),index);
            this.getModel().setValue("nckd_bankaccount",bankEntry == null ? null : bankEntry.get("bankaccount"),index);
            this.getModel().setValue("nckd_accountname",bankEntry == null ? null : bankEntry.get("accountname"),index);
            this.getModel().setValue("nckd_bank",bankEntry == null ? null : bankEntry.getDynamicObject("bank"),index);
            this.getModel().setValue("nckd_currency",bankEntry == null ? null : bankEntry.getDynamicObject("currency"),index);
            this.getModel().setValue("nckd_taxpayertype",customer.getDynamicObject("nckd_nashuitype"),index);
            //this.getModel().setValue("nckd_invoicename",,index);
            this.getModel().setValue("nckd_invoicetype",customer.getDynamicObject("invoicecategory") == null ? null : customer.getDynamicObject("invoicecategory").get("number"),index);
            //this.getModel().setValue("nckd_addressorphone",,index);
            this.getModel().setValue("nckd_banknumber",customer.get("nckd_yhzh"),index);
            this.getModel().setValue("nckd_invoicephone",customer.get("nckd_telnumber"),index);
            this.getModel().setValue("nckd_enterpriseemail",customer.get("postal_code"),index);
            this.getModel().setValue("nckd_customeremail",customer.get("nckd_mail"),index);
            //this.getModel().setValue("nckd_myemail",customer.get("nckd_mail"),index);

            //锁定当前分录行
            this.getView().setEnable(false, index, ENTRYFIELD);
            //复制当前行可修改
            int newIndex = this.getModel().insertEntryRow("nckd_entry", index + 1);
            this.getModel().setValue("nckd_changeafter","2",newIndex);
            this.getModel().setValue("nckd_changetype","update",newIndex);
            this.getModel().setValue("nckd_customermodify",customer,newIndex);
            this.getModel().setValue("nckd_spname",customer.get("name"),newIndex);
            this.getModel().setValue("nckd_customerrange", customer.getDynamicObject("nckd_customertype"),newIndex);//客户范围
            this.getModel().setValue("nckd_province",customer.get("admindivision"),newIndex);
            this.getModel().setValue("nckd_businesstype",customer.get("nckd_v"),newIndex);
            this.getModel().setValue("nckd_quality",customer.get("nckd_customerxz"),newIndex);
            this.getModel().setValue("nckd_societycreditcode",customer.get("societycreditcode"),newIndex);
            this.getModel().setValue("nckd_basedatafield",customer,newIndex);
            this.getModel().setValue("nckd_buyer",customer.get("salerid"),newIndex);
            this.getModel().setValue("nckd_cooperatestatus",getCooperateStatus(customer.getString("nckd_cooperationstatus")),newIndex);
            this.getModel().setValue("nckd_linkman",linkmanEntry == null ? null : linkmanEntry.get("contactperson"),newIndex);
            this.getModel().setValue("nckd_phone",linkmanEntry == null ? null : linkmanEntry.get("phone"),newIndex);
            this.getModel().setValue("nckd_postalcode",customer.get("duns"),newIndex);
            this.getModel().setValue("nckd_address",customer.get("bizpartner_address"),newIndex);
            this.getModel().setValue("nckd_bankaccount",bankEntry == null ? null : bankEntry.get("bankaccount"),newIndex);
            this.getModel().setValue("nckd_accountname",bankEntry == null ? null : bankEntry.get("accountname"),newIndex);
            this.getModel().setValue("nckd_bank",bankEntry == null ? null : bankEntry.getDynamicObject("bank"),newIndex);
            this.getModel().setValue("nckd_currency",bankEntry == null ? null : bankEntry.getDynamicObject("currency"),newIndex);
            this.getModel().setValue("nckd_taxpayertype",customer.getDynamicObject("nckd_nashuitype"),newIndex);
            //this.getModel().setValue("nckd_invoicename",,index);
            this.getModel().setValue("nckd_invoicetype",customer.getDynamicObject("invoicecategory") == null ? null : customer.getDynamicObject("invoicecategory").get("number"),newIndex);
            //this.getModel().setValue("nckd_addressorphone",,index);
            this.getModel().setValue("nckd_banknumber",customer.get("nckd_yhzh"),newIndex);
            this.getModel().setValue("nckd_invoicephone",customer.get("nckd_telnumber"),newIndex);
            this.getModel().setValue("nckd_enterpriseemail",customer.get("postal_code"),newIndex);
            this.getModel().setValue("nckd_customeremail",customer.get("nckd_mail"),newIndex);
            //this.getModel().setValue("nckd_myemail",customer.get("nckd_mail"),index);
            return;
        }

        //判断当前选中行是否是可修改行，不是的话无需新增
        if (changeAfter != null && "2".equals(changeAfter)) {
            this.getModel().setValue("nckd_spname",customer.get("name"),index);
            this.getModel().setValue("nckd_customerrange", customer.getDynamicObject("nckd_customertype"),index);//客户范围
            this.getModel().setValue("nckd_province",customer.get("admindivision"),index);
            this.getModel().setValue("nckd_businesstype",customer.get("nckd_v"),index);
            this.getModel().setValue("nckd_quality",customer.get("nckd_customerxz"),index);
            this.getModel().setValue("nckd_societycreditcode",customer.get("societycreditcode"),index);
            this.getModel().setValue("nckd_basedatafield",customer,index);
            this.getModel().setValue("nckd_buyer",customer.get("salerid"),index);
            this.getModel().setValue("nckd_cooperatestatus",getCooperateStatus(customer.getString("nckd_cooperationstatus")),index);
            this.getModel().setValue("nckd_linkman",linkmanEntry == null ? null : linkmanEntry.get("contactperson"),index);
            this.getModel().setValue("nckd_phone",linkmanEntry == null ? null : linkmanEntry.get("phone"),index);
            this.getModel().setValue("nckd_postalcode",customer.get("duns"),index);
            this.getModel().setValue("nckd_address",customer.get("bizpartner_address"),index);
            this.getModel().setValue("nckd_bankaccount",bankEntry == null ? null : bankEntry.get("bankaccount"),index);
            this.getModel().setValue("nckd_accountname",bankEntry == null ? null : bankEntry.get("accountname"),index);
            this.getModel().setValue("nckd_bank",bankEntry == null ? null : bankEntry.getDynamicObject("bank"),index);
            this.getModel().setValue("nckd_currency",bankEntry == null ? null : bankEntry.getDynamicObject("currency"),index);
            this.getModel().setValue("nckd_taxpayertype",customer.getDynamicObject("nckd_nashuitype"),index);
            //this.getModel().setValue("nckd_invoicename",,index);
            this.getModel().setValue("nckd_invoicetype",customer.getDynamicObject("invoicecategory") == null ? null : customer.getDynamicObject("invoicecategory").get("number"),index);
            //this.getModel().setValue("nckd_addressorphone",,index);
            this.getModel().setValue("nckd_banknumber",customer.get("nckd_yhzh"),index);
            this.getModel().setValue("nckd_invoicephone",customer.get("nckd_telnumber"),index);
            this.getModel().setValue("nckd_enterpriseemail",customer.get("postal_code"),index);
            this.getModel().setValue("nckd_customeremail",customer.get("nckd_mail"),index);
            //this.getModel().setValue("nckd_myemail",customer.get("nckd_mail"),index);
        }
    }

    private String getCooperateStatus(String result){
        if (StringUtils.isEmpty(result)){
            return null;
        }
        switch (result){
            case "A":
                result = "5";
                break;
            case "B":
                result = "2";
                break;
            case "C":
                result = "1";
                break;
        }
        return result;
    }

}