package nckd.yanye.tmc.plugin.form;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;

import java.util.EventObject;

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
        String payeetype = this.getModel().getValue("payeetype").toString();
        if (name.equals("payeetype")) {
            if (!payeetype.equals("bd_supplier")){
                this.getModel().setValue("nckd_vendor",null);
            }
        }
        //收票人全称带出供应商
        if (name.equals("receiver")) {
            if (payeetype.equals("bd_supplier")){
                DynamicObject receiver = (DynamicObject) this.getModel().getValue("receiver");
                this.getModel().setValue("nckd_vendor",receiver.getPkValue());
            }
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        Object value = this.getModel().getValue("billstatus");
        if (!"C".equals(value)){
            this.getView().setEnable(false,"nckd_vendor");
        }
    }
}
