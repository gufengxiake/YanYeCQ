package nckd.yanye.tmc.plugin.form;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;

/**
 * Module           :财务云-资金-开票登记
 * Description      :表单插件
 *
 * @author : xiaoxiaopeng
 * @date : 2024/8/27
 */
public class PayableBillFromPlugin extends AbstractBillPlugIn {


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        //收票人全称带出供应商
        if (name.equals("receiver")) {
            DynamicObject receiver = (DynamicObject) this.getModel().getValue("receiver");
            this.getModel().setValue("nckd_vendor",receiver.getPkValue());
        }
    }
}
