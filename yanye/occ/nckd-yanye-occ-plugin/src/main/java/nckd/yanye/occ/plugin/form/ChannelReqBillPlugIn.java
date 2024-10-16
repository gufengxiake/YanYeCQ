package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;

import java.util.EventObject;

public class ChannelReqBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        Long orgId = RequestContext.get().getOrgId();
        if (orgId != 0) {
            //销售组织
           this.getModel().setItemValueByID("saleorg",orgId,0);
        }
        Object[] ids=new Object[]{768696787199340544L,769522191178951680L};
        //渠道职能
        this.getModel().setValue("channelfunctions",ids,0);
    }
}
