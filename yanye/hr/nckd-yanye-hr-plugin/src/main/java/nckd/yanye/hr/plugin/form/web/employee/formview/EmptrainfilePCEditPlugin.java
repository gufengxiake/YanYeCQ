package nckd.yanye.hr.plugin.form.web.employee.formview;

import kd.bos.bill.OperationStatus;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.AbstractPCFormDrawEdit;

import java.util.EventObject;
import java.util.Map;

/**
 * HR员工自助PC端-培训经历-弹框
 * 动态表单标识：nckd_hspm_emptrainfi_pdg
 * author:chengchaohua
 * date:2024-08-19
 */
public class EmptrainfilePCEditPlugin extends AbstractPCFormDrawEdit {

    public EmptrainfilePCEditPlugin() {

    }

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
    }

    protected Map<String, String> getPageParams() {
        Map<String, String> pageParams = super.getPageParams();
        pageParams.put(PAGE_ID, "hrpi_emptrainfile");
        return pageParams;
    }
}
