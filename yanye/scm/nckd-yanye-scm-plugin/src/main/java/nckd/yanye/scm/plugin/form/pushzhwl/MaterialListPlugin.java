package nckd.yanye.scm.plugin.form.pushzhwl;


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
 * 主数据-物料列表插件（推送智慧物流）
 * 表单标识：bd_material
 * author：xiaoxiaopeng
 * date：2024-09-25
 */
public class MaterialListPlugin extends AbstractListPlugin {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        String key = e.getOperateKey();
        if ("audit".equals(key)) {
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                for (ListSelectedRow row : selectedRows) {
                    DynamicObject material = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "bd_material");
                    OperationResult pushzzwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_material", new DynamicObject[]{material}, OperateOption.create());
                    if (!pushzzwl.isSuccess()){
                        this.getView().showMessage("物料：" +material.getString("number") + pushzzwl.getMessage());
                    }
                }
            }
        }
    }
}
