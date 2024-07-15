package nckd.yanye.hr.plugin.form;/*
 *@title EmpCountryCardEditFormPlugin
 *@description  人员档案-国家职称信息的字段新增
 *@author jyx
 *@version 1.0
 *@create 2024/4/10 17:47
 */

import kd.bos.bill.OperationStatus;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;

import java.util.EventObject;

public class EmpCountryCardEditPlugin extends AbstractFormDrawEdit {
    public EmpCountryCardEditPlugin() {

    }
    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
            if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            //
            this.setValueFromDb(formShowParameter, "nckd_hrpi_country", (String)null);
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
                this.updateAttachData("nckd_hrpi_country", this.getView(), true, (String)null);
            } else if (status.equals(OperationStatus.ADDNEW)) {
                this.addAttachData("nckd_hrpi_country", this.getView(), this.getModel().getDataEntity(), true);
            }
        }

    }



}

