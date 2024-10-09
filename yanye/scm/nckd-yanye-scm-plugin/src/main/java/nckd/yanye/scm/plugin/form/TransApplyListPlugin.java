package nckd.yanye.scm.plugin.form;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;

/**
 * 供应链-调拨申请单列表插件
 * 表单标识：im_transapply
 * author：xiaoxiaopeng
 * date：2024-09-24
 */
public class TransApplyListPlugin extends AbstractListPlugin {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        String key = e.getOperateKey();
        if ("audit".equals(key)) {
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                for (ListSelectedRow row : selectedRows) {
                    DynamicObject delivernotice = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "im_transapply");
                    OperationResult pushzzwl = OperationServiceHelper.executeOperate("pushzzwl", "im_transapply", new DynamicObject[]{delivernotice}, OperateOption.create());
                    if (pushzzwl == null || !pushzzwl.isSuccess()){
                        this.getView().showMessage("单据：" +delivernotice.getString("billno") + "同步智慧物流派车单失败");
                    }else {
                        this.getView().showMessage("同步智慧物流派车单成功");
                    }
                }
            }
        }
    }
}
