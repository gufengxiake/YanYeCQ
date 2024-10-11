package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.EventObject;

/**
 * 主数据-智慧物流供应商对照表单插件
 * 表单标识：nckd_supplier_control
 * author：xiaoxiaopeng
 * date：2024-10-11
 */
public class SupplierControlBillFromPlugin extends AbstractBillPlugIn {

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        DynamicObject creator = (DynamicObject) getModel().getValue("creator");
        if (creator != null) {
            DynamicObject user = BusinessDataServiceHelper.loadSingle(creator.getPkValue(), "bos_user");
            DynamicObjectCollection entryentity = user.getDynamicObjectCollection("entryentity");
            if (entryentity.size() > 0) {
                getModel().setValue("nckd_org", entryentity.get(0).getDynamicObject("dpt"));
            }
        }
    }
}
