package nckd.yanye.occ.plugin.mobile;

import kd.bos.context.RequestContext;
import kd.bos.form.plugin.AbstractMobFormPlugin;

import java.util.EventObject;

public class MobileChannelReqBillPlugIn extends AbstractMobFormPlugin {
    @Override
    public void afterCreateNewData(EventObject e) {
        Long orgId = RequestContext.get().getOrgId();
        if (orgId != 0) {
            //销售组织
            this.getModel().setItemValueByID("saleorg",orgId,0);
        }
        Object[] ids=new Object[]{768696787199340544L,769522191178951680L};
        this.getModel().setValue("channelfunctions",ids,0);
    }
}
