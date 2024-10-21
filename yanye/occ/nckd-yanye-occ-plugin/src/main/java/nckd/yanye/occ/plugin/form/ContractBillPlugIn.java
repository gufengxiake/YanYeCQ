package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.TextEdit;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.EventObject;

/**
 * 采购/销售合同新增自动携带合同主体
 * 编辑订货客户根据部门不同给甲方乙方赋值
 * author:wgq
 * date:2024/09/24
 */
public class ContractBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        DynamicObject org = ((DynamicObject) this.getModel().getValue("org"));
        if (org != null) {
            Object orgId = org.getPkValue();
            // 构造QFilter  org  业务组织   enable 可用
            QFilter qFilter = new QFilter("org.id", QCP.equals, orgId)
                    .and("enable", QCP.equals, "1");
            //查找合同主体
            DynamicObjectCollection collections = QueryServiceHelper.query("conm_contparties",
                    "id,name,contacts,phone", qFilter.toArray(), "");
            if (!collections.isEmpty()) {
                DynamicObject contpartiesItem = collections.get(0);
                long contpartiesItemId = (long) contpartiesItem.get("id");
                this.getModel().setItemValueByID("contparties", contpartiesItemId);
                if (contpartiesItem != null) {
                    //甲方
                    this.getModel().setValue("party1st", contpartiesItem.getString("name"));
                    //甲方联系人
                    this.getModel().setValue("contactperson1st", contpartiesItem.getString("contacts"));
                    //联系人电话
                    this.getModel().setValue("phone1st", contpartiesItem.get("phone"));
                } else {
                    this.getModel().setValue("party1st", (Object) null);
                    this.getModel().setValue("contactperson1st", (Object) null);
                    this.getModel().setValue("phone1st", (Object) null);
                }
            }

        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        if (StringUtils.equals("customer", fieldKey)){
            if(this.getModel().getValue("customer") == null){
                this.getModel().setValue("party1st",null);
                this.getModel().setValue("party2nd",null);
                return;
            }
            if (this.getModel().getValue("org") == null || this.getModel().getValue("dept") == null ) {
                return;
            }
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            DynamicObject customer = (DynamicObject) this.getModel().getValue("customer");
            DynamicObject dept = (DynamicObject) this.getModel().getValue("dept");
            String deptName = dept.getString("name");
            if (StringUtils.equals("化工事业部", deptName) || StringUtils.equals("盐品事业部", deptName) || StringUtils.equals("海外事业部", deptName)){
                this.getModel().setValue("party1st",org.getString("name"));
                this.getModel().setValue("party2nd",customer.getString("name"));
            }
        }

    }
}
