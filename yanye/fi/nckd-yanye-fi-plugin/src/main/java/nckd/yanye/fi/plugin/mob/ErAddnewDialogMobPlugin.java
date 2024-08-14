package nckd.yanye.fi.plugin.mob;

import java.util.EventObject;

import kd.bos.bill.MobileBillShowParameter;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.MobileListShowParameter;
import kd.fi.er.model.FormModel;

/**
 * @author husheng
 * @date 2024-08-14 11:01
 * @description  移动端-特殊事项审批插件
 */
public class ErAddnewDialogMobPlugin extends AbstractMobFormPlugin {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addClickListeners("nckd_tssxspd");
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);

        Control control = (Control)evt.getSource();
        String key = control.getKey();
        if(key.equals("nckd_tssxspd")){
            FormModel formModel = new FormModel("nckd_tssxspd_mob", null, "6", false);
            FormShowParameter formShowParameter = formModel.getFormShowParameter();
            formShowParameter.setCustomParam("reimbursetype", "meetting_bill");
            formShowParameter.setCustomParam("bizitem", "meetting_bill");
            this.getView().showForm(formShowParameter);
        }
    }
}
