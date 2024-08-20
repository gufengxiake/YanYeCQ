package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.formview;

import kd.bos.bill.OperationStatus;
import kd.bos.form.control.Control;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobileFormDrawEdit;

import java.util.EventObject;
import java.util.Map;

/**
 * HR员工自助移动端-奖励信息-弹框
 * 移动动态表单标识：nckd_hspm_perrprecord_mdg
 * author:chengchaohua
 * date:2024-08-17
 */
public class PerrprecordMobEditPlugin extends AbstractMobileFormDrawEdit {

    public PerrprecordMobEditPlugin() {}

    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        String status = (String)this.getView().getFormShowParameter().getCustomParam("cus_status");
        if ("cus_edit".equals(status)) {
            this.getView().setStatus(OperationStatus.EDIT);
        } else {
            if ("cus_addnew".equals(status)) {
                this.getView().setStatus(OperationStatus.ADDNEW);
                return;
            }

            this.getView().setStatus(OperationStatus.VIEW);
        }

        this.setValueFromDb(this.getView().getFormShowParameter(), "hrpi_perrprecord", (String)null);
        this.getModel().setDataChanged(false);
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control)evt.getSource();
        if ("btn_edit".equals(source.getKey())) {
            this.getView().setStatus(OperationStatus.EDIT);
            this.getView().getFormShowParameter().setCustomParam("cus_status", "cus_edit");
            this.getView().invokeOperation("refresh");
        } else if ("btn_save".equals(source.getKey())) {
            String customParam = (String)this.getView().getFormShowParameter().getCustomParam("pkid");
            Long pkid = Long.parseLong(customParam == null ? "0" : customParam);
            Map<String, Object> resultMap = null;
            if (pkid != null && pkid != 0L) {
                resultMap = this.updateAttachData("hrpi_perrprecord", this.getView(), false, (String)null);
            } else {
                resultMap = this.addAttachData("0", "hrpi_perrprecord", this.getView(), this.getModel().getDataEntity(), false);
            }

            this.closeView(this.getView(), resultMap, this.getView().getParentView());
        }

    }
}
