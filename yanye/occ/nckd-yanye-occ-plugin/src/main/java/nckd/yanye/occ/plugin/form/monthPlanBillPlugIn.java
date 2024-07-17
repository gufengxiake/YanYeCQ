package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.form.FormShowParameter;

import java.util.EventObject;

public class monthPlanBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        // 获取当前页面的FormShowParameter对象
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        // 获取自定义参数
        Object orgId = formShowParameter.getCustomParam("orgId");
        // 把参数值赋值到页面文本字段上
        this.getModel().setValue("kdec_textfield", orgId);
    }
}
