package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;

import java.util.EventObject;

/*
人员通用过滤插件，显示商务伙伴人员
author:wgq
date:2024/08/20
 */
public class UserF7SelectPlugIn extends AbstractBillPlugIn implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit deliveryEdit = this.getView().getControl("nckd_deliveryman");
        if(deliveryEdit!=null){
            deliveryEdit.addBeforeF7SelectListener(this);
        }
    }
    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name = evt.getProperty().getName();
         if(name.equalsIgnoreCase("nckd_deliveryman")){
            evt.getFormShowParameter().setCustomParam("externalUserType","all");
        }

    }
}
