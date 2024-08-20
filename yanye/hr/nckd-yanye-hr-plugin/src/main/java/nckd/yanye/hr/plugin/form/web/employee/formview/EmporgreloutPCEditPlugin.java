package nckd.yanye.hr.plugin.form.web.employee.formview;

import kd.bos.bill.OperationStatus;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.AbstractPCFormDrawEdit;

import java.util.EventObject;
import java.util.Map;

/**
 * HR员工自助PC端-上线前任职经历-弹框
 * 动态表单标识：nckd_hspm_emporgrelo_pdg
 * author:chengchaohua
 * date:2024-08-19
 */
public class EmporgreloutPCEditPlugin extends AbstractPCFormDrawEdit {

    public EmporgreloutPCEditPlugin() {

    }

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
    }

    protected Map<String, String> getPageParams() {
        Map<String, String> pageParams = super.getPageParams();
        pageParams.put(PAGE_ID, "nckd_hrpi_emporgrelout");
        return pageParams;
    }
}
