package nckd.yanye.hr.plugin.form;

import kd.bos.bill.OperationStatus;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;

import java.util.EventObject;

/**
 * 核心人力云-》人员信息-》附表弹框-》人员聘用情况（nckd_hspm_hrpi_perpyqk_dg）
 * 动态表单插件
 * author:程超华 2024-04-11
 */
public class EmpPersonPingYongCardEditPlugin extends AbstractFormDrawEdit {

    public EmpPersonPingYongCardEditPlugin() {

    }

    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            // 人员聘用情况基础页面 nckd_hrpi_personpingyqk
            this.setValueFromDb(formShowParameter, "nckd_hrpi_personpingyqk", (String)null);
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
                this.updateAttachData("nckd_hrpi_personpingyqk", this.getView(), true, (String)null);
            } else if (status.equals(OperationStatus.ADDNEW)) {
                this.addAttachData("nckd_hrpi_personpingyqk", this.getView(), this.getModel().getDataEntity(), true);
            }
        }

    }
}
