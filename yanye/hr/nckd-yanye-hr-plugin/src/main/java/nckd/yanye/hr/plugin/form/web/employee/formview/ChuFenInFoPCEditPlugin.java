package nckd.yanye.hr.plugin.form.web.employee.formview;

import kd.bos.bill.OperationStatus;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.AbstractPCFormDrawEdit;

import java.util.EventObject;
import java.util.Map;

/**
 * HR员工自助PC端-处分信息-弹框
 * 动态表单标识：nckd_hspm_chufeninfo_pdg
 * author:chengchaohua
 * date:2024-10-11
 */
public class ChuFenInFoPCEditPlugin extends AbstractPCFormDrawEdit {

    public ChuFenInFoPCEditPlugin() {}

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
    }

    protected Map<String, String> getPageParams() {
        Map<String, String> pageParams = super.getPageParams();
        pageParams.put(PAGE_ID, "nckd_hrpi_chufeninfo");
        return pageParams;
    }
}
