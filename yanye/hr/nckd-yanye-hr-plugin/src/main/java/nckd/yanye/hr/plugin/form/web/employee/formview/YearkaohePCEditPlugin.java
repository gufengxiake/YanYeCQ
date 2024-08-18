package nckd.yanye.hr.plugin.form.web.employee.formview;

import kd.bos.bill.OperationStatus;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.AbstractPCFormDrawEdit;

import java.util.EventObject;
import java.util.Map;

/**
 * HR员工自助PC端-年度考核-弹框
 * 动态表单标识：nckd_hspm_yearkaohe_pdg
 * author:chengchaohua
 * date:2024-08-16
 */
public class YearkaohePCEditPlugin extends AbstractPCFormDrawEdit {

    public YearkaohePCEditPlugin() {

    }

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
    }

    protected Map<String, String> getPageParams() {
        Map<String, String> pageParams = super.getPageParams();
        pageParams.put(PAGE_ID, "nckd_hrpi_yearkaohe");
        return pageParams;
    }
}
