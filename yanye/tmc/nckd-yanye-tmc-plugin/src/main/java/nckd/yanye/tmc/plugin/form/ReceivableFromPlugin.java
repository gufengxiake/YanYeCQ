package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.operate.FormOperate;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.EventObject;

/**
 * Module           :财务云-资金-收票登记
 * Description      :表单插件
 *
 * @author : xiaoxiaopeng
 * @date : 2024/8/27
 */

public class ReceivableFromPlugin extends AbstractBillPlugIn {


    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        String payeetype = this.getModel().getValue("payeetype").toString();
        if (name.equals("payeetype")){
            if (!payeetype.equals("bd_customer")){
                this.getModel().setValue("nckd_client",null);
            }
        }
        //交票人全称带出客户
        if (name.equals("deliver")) {
            if (payeetype.equals("bd_customer")){
                DynamicObject deliver = (DynamicObject) this.getModel().getValue("deliver");
                this.getModel().setValue("nckd_client",deliver.getPkValue());
            }
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        Object value = this.getModel().getValue("billstatus");
        if (!"C".equals(value)){
            this.getView().setEnable(false,"nckd_client");
        }
    }
}
