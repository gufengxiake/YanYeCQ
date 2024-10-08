package nckd.yanye.occ.plugin.mobile;

import kd.bos.form.CloseCallBack;
import kd.bos.form.MobileFormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.scmc.im.consts.OP;

import java.util.EventObject;

public class MobileReturnEditBillPlugIn extends AbstractMobFormPlugin {

    public void afterBindData(EventObject e){
        super.afterBindData(e);
        this.getView().setEnable(  true, "flexpanelap5");
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        String key=e.getOperateKey();
        switch (key){
            case OP.OP_SUBMIT://提交
                if("ocdma_saleorder_list".equals(this.getView().getParentView().getEntityId())){
                    MobileFormShowParameter mobileFormShowParameter = new MobileFormShowParameter();
                    mobileFormShowParameter.getOpenStyle().setShowType(ShowType.Floating);
                    mobileFormShowParameter.setFormId("ocbsoc_returnmoblist");
                    mobileFormShowParameter.setCustomParam("supplierid",this.getModel().getValue("supplierid_id").toString());
                    mobileFormShowParameter.setCloseCallBack(new CloseCallBack(this,"ocbsoc_returnmoblist"));
                    getView().showForm(mobileFormShowParameter);
                }
                break;
            default:
                break;
        }
    }
}
