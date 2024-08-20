package nckd.yanye.hr.plugin.form.web.employee.formview;

import kd.bos.bill.OperationStatus;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.AbstractPCFormDrawEdit;

import java.util.EventObject;
import java.util.Map;

/**
 * HR员工自助PC端-奖励信息-弹框
 * author:chengchaohua
 * date:2024-08-16
 */
public class PerrprecordPCEditPlugin  extends AbstractPCFormDrawEdit {

    public PerrprecordPCEditPlugin() {

    }

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
    }

    protected Map<String, String> getPageParams() {
        Map<String, String> pageParams = super.getPageParams();
        pageParams.put(PAGE_ID, "hrpi_perrprecord");
        return pageParams;
    }
}
