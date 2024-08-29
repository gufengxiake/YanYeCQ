package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.util.EventObject;

/*
要货订单表单插件
表单标识：nckd_ocbsoc_saleorder_ext
author:wgq
date:2024/08/20
 */
public class OcbSaleOrderBillPlugIn extends AbstractBillPlugIn {
//    @Override
//    public void propertyChanged(PropertyChangedArgs e) {
//
//        String propName = e.getProperty().getName();
//        //业务员
//        if (propName.equals("salerid")) {
//            DynamicObject saler = (DynamicObject) e.getChangeSet()[0].getNewValue();
//            if (saler != null) {
//                String number = saler.getString("number");
//                this.getModel().setItemValueByNumber("nckd_salerid", number);
//            } else {
//                this.getModel().setValue("nckd_salerid", null);
//            }
//        }
//    }

    @Override
    public void afterCreateNewData(EventObject e) {
        DynamicObject user= UserServiceHelper.getCurrentUser("id,number,name");
        if(user!=null){
            String number=user.getString("number");
            // 构造QFilter  operatornumber业务员   opergrptype 业务组类型=销售组
            QFilter qFilter = new QFilter("operatornumber", QCP.equals, number)
                    .and("opergrptype", QCP.equals, "XSZ");
            //查找业务员
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_operator",
                    "id", qFilter.toArray(), "");
            if(!collections.isEmpty()){
                DynamicObject operatorItem = collections.get(0);
                String operatorId = operatorItem.getString("id");
                this.getModel().setItemValueByID("nckd_salerid",operatorId);
            }
        }
    }

}
