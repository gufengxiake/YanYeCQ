package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.servicehelper.operation.OperationServiceHelper;

/**
 * 供应链-发货通知单表单插件
 * 表单标识：sm_delivernotice
 * author：xiaoxiaopeng
 * date：2024-09-24
 */
public class DeliverNoticeBillFromPlugin extends AbstractBillPlugIn {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        String key = e.getOperateKey();
        if ("audit".equals(key)) {
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                OperationResult pushzzwl = OperationServiceHelper.executeOperate("pushzzwl", "sm_delivernotice", new DynamicObject[]{this.getModel().getDataEntity()}, OperateOption.create());
                if (pushzzwl == null || !pushzzwl.isSuccess()){
                    this.getView().showMessage("同步智慧物流派车单失败");
                }else {
                    this.getView().showMessage("同步智慧物流派车单成功");
                }
            }
        }
    }
}
