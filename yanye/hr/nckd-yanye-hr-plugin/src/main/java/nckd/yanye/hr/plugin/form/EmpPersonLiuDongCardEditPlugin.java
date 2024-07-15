package nckd.yanye.hr.plugin.form;

import kd.bos.bill.OperationStatus;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;

import java.util.EventObject;

/**
 * 核心人力云-》人员信息-》附表弹框-》流动情况（nckd_hspm_hrpi_liudong_dg）
 * 动态表单插件
 * author:程超华 2024-04-11
 */
public class EmpPersonLiuDongCardEditPlugin extends AbstractFormDrawEdit {

    public EmpPersonLiuDongCardEditPlugin() {

    }

    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            // 流动情况基础页面 nckd_hrpi_liudongqk
            this.setValueFromDb(formShowParameter, "nckd_hrpi_liudongqk", (String)null);
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
                this.updateAttachData("nckd_hrpi_liudongqk", this.getView(), true, (String)null);
            } else if (status.equals(OperationStatus.ADDNEW)) {
                this.addAttachData("nckd_hrpi_liudongqk", this.getView(), this.getModel().getDataEntity(), true);
            }
        }

    }
}
