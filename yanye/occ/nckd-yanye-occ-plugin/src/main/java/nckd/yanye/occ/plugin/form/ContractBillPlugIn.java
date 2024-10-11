package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.EventObject;

/*
采购/销售合同新增自动携带合同主体
author:wgq
date:2024/09/24
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
}
