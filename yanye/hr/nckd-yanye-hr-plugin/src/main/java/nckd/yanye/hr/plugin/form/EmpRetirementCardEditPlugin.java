package nckd.yanye.hr.plugin.form;/*
 *@title EmpRetirementCardEditPlugin
 *@description
 *@author jyx
 *@version 1.0
 *@create 2024/4/11 16:53
 */

import kd.bos.bill.OperationStatus;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;

import java.util.EventObject;

public class EmpRetirementCardEditPlugin extends AbstractFormDrawEdit {
    public EmpRetirementCardEditPlugin() {

    }
    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            //
            this.setValueFromDb(formShowParameter, "nckd_retirementstatus", (String)null);
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
                this.updateAttachData("nckd_retirementstatus", this.getView(), true, (String)null);
            } else if (status.equals(OperationStatus.ADDNEW)) {
                this.addAttachData("nckd_retirementstatus", this.getView(), this.getModel().getDataEntity(), true);
            }
        }

    }


}

