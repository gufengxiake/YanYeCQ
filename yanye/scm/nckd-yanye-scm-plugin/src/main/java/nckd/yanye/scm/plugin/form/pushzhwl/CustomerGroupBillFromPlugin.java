package nckd.yanye.scm.plugin.form.pushzhwl;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.servicehelper.operation.OperationServiceHelper;

/**
 * 主数据-物料分类表单插件（推送智慧物流）
 * 表单标识：bd_customergroup
 * author：xiaoxiaopeng
 * date：2024-09-25
 */
public class CustomerGroupBillFromPlugin extends AbstractBillPlugIn {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        String key = e.getOperateKey();
        if ("save".equals(key)){
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_customergroup", new DynamicObject[]{getModel().getDataEntity()}, OperateOption.create());
                if (!pushzhwl.isSuccess()){
                    this.getView().showMessage(pushzhwl.getMessage());
                }
            }
        }
    }
}
