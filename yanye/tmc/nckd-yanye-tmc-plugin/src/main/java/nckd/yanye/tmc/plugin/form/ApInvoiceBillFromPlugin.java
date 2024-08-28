package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;

/**
 * 资金-收票单表单插件
 * 表单标识：ap_invoice
 * author：xiaoxiaopeng
 * date：2024-08-27
 */
public class ApInvoiceBillFromPlugin extends AbstractBillPlugIn {

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        if ("e_pricetaxtotal".equals(name)) {
            Object amount = this.getModel().getValue("amount");
            this.getModel().setValue("nckd_remainderamount",amount);
        }
    }
}
