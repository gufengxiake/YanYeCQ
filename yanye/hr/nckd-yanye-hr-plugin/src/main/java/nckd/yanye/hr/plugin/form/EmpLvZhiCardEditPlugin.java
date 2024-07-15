package nckd.yanye.hr.plugin.form;

import kd.bos.bill.OperationStatus;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;

import java.util.EventObject;

/**
 * 核心人力云-》人员信息-》附表弹框-》履职信息（nckd_hspm_hrpi_lvzhi_dg）
 * 动态表单插件
 * author：刘少波
 */
public class EmpLvZhiCardEditPlugin extends AbstractFormDrawEdit {
    public EmpLvZhiCardEditPlugin() {

    }

    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            // 离职信息基础页面 nckd_hrpi_lvzhiinform
            this.setValueFromDb(formShowParameter, "nckd_hrpi_lvzhiinform", (String) null);
        }
        this.getModel().setDataChanged(false);
    }

    public void registerListener(EventObject eventObject) {
        super.registerListener(eventObject);
        this.addClickListeners(new String[]{"btnsave"});
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control) evt.getSource();
        String key = source.getKey();
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (key.equals("btnsave")) {
            if (status.equals(OperationStatus.EDIT)) {
                this.updateAttachData("nckd_hrpi_lvzhiinform", this.getView(), true, (String) null);
            } else if (status.equals(OperationStatus.ADDNEW)) {
                this.addAttachData("nckd_hrpi_lvzhiinform", this.getView(), this.getModel().getDataEntity(), true);
            }
        }

    }
}
