package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.EventObject;

/*
要货订单表单插件
 */
public class OcbSaleOrderBillPlugIn extends AbstractBillPlugIn {
        @Override
    public void propertyChanged(PropertyChangedArgs e) {

        String propName = e.getProperty().getName();
        //业务员
        if (propName.equals("salerid")) {
            DynamicObject saler= (DynamicObject) e.getChangeSet()[0].getNewValue();
            if(saler!=null){
                String number=saler.getString("number");
                this.getModel().setItemValueByNumber("nckd_salerid",number);
            }
            else {
                this.getModel().setValue("nckd_salerid",null);
            }
        }
    }

}
