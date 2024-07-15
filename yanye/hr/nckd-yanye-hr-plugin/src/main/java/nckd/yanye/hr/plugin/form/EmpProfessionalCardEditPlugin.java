package nckd.yanye.hr.plugin.form;/*
 *@title EmpProfessionalCardEditPlugin
 *@description
 *@author jyx
 *@version 1.0
 *@create 2024/4/11 16:55
 */

import kd.bos.bill.OperationStatus;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;

import java.util.EventObject;

public class EmpProfessionalCardEditPlugin extends AbstractFormDrawEdit {

    public EmpProfessionalCardEditPlugin() {

    }
    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            //
            this.setValueFromDb(formShowParameter, "nckd_professionalser", (String)null);
        }

        this.getModel().setDataChanged(false);
    }

    public void registerListener(EventObject eventObject) {
        super.registerListener(eventObject);
        this.addClickListeners(new String[]{"btnsave"});
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control)evt.getSource();
        String key = source.getKey();
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (key.equals("btnsave")) {
            if (status.equals(OperationStatus.EDIT)) {
                this.updateAttachData("nckd_professionalser", this.getView(), true, (String)null);
            } else if (status.equals(OperationStatus.ADDNEW)) {
                this.addAttachData("nckd_professionalser", this.getView(), this.getModel().getDataEntity(), true);
            }
        }

    }


}

