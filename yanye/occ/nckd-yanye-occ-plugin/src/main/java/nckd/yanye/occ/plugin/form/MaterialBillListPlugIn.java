package nckd.yanye.occ.plugin.form;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;

import java.util.HashSet;

public class MaterialBillListPlugIn extends AbstractListPlugin {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        String key = e.getOperateKey();
        //批量生成物料业务信息
        if ("batchcreatmatinfo".equals(key)) {
            ListSelectedRowCollection selectedRows = this.getSelectedRows();
            if(selectedRows.size()==0){
                this.getView().showErrorNotification("请至少选择一行数据！");
                return;
            }
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                HashSet<Object> matIds = new HashSet<>();
                for (ListSelectedRow row : selectedRows) {
                    matIds.add(row.getPrimaryKeyValue());
                }
                FormShowParameter formShowParameter = new FormShowParameter();
                // 弹窗案例-动态表单 页面标识
                formShowParameter.setFormId("nckd_batchcreatmaterial");//批量生成物料业务信息
                formShowParameter.setCustomParam("matId", matIds);
                // 设置打开类型为模态框（不设置的话指令参数缺失，没办法打开页面）
                formShowParameter.getOpenStyle().setShowType(ShowType.Modal);
                //显示界面
                this.getView().showForm(formShowParameter);
            }
        }
    }

}
