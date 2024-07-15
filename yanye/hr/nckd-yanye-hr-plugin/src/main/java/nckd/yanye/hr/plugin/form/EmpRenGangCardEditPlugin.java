package nckd.yanye.hr.plugin.form;


import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;
import kd.bos.bill.OperationStatus;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;

import java.util.EventObject;

/**
 * 核心人力云-》人员信息-》附表弹框-》任岗信息（nckd_hspm_hrpi_rengang_dg）
 * 动态表单插件
 * author：刘少波
 */
public class EmpRenGangCardEditPlugin extends AbstractFormDrawEdit {
    public EmpRenGangCardEditPlugin() {
    }
    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            // 任职信息基础页面 nckd_hrpi_workinform
            this.setValueFromDb(formShowParameter, "nckd_hrpi_workinform", (String)null);
        }

        this.getModel().setDataChanged(false);
    }

    @Override
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
                this.updateAttachData("nckd_hrpi_workinform", this.getView(), true, (String)null);
            } else if (status.equals(OperationStatus.ADDNEW)) {
                this.addAttachData("nckd_hrpi_workinform", this.getView(), this.getModel().getDataEntity(), true);
            }
        }

    }
}