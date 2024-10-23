package nckd.yanye.scm.plugin.form;


import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import org.apache.commons.lang3.StringUtils;

/**
 * 主数据-供应商变更单列表插件（推送智慧物流）
 * 表单标识：nckd_bd_supplier_change
 * author：xiaoxiaopeng
 * date：2024-09-25
 */
public class BdSupplierChangeListPlugin extends AbstractListPlugin {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        String key = e.getOperateKey();
        if ("audit".equals(key)) {
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                for (ListSelectedRow row : selectedRows) {
                    DynamicObject customerChange = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "nckd_bd_supplier_change");
                    DynamicObjectCollection entity = customerChange.getDynamicObjectCollection("nckd_entry");
                    String merchanttype = customerChange.getString("nckd_merchanttype");
                    if ("carrier".equals(merchanttype)){
                        if (entity.size() > 0){
                            for (DynamicObject entry : entity) {
                                String changeAfter = entry.getString("nckd_changeafter");
                                if (StringUtils.isEmpty(changeAfter)){
                                    String addSupplier = entry.getString("nckd_addsupplier");
                                    DynamicObject supplier = BusinessDataServiceHelper.loadSingle("bd_supplier","id",new QFilter[]{new QFilter("name", QCP.equals,addSupplier)});
                                    OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_supplier", new DynamicObject[]{supplier}, OperateOption.create());
                                    if (!pushzhwl.isSuccess()){
                                        this.getView().showMessage("供应商：" +supplier.getString("name") + pushzhwl.getMessage());
                                    }
                                    continue;
                                }
                                if ("2".equals(changeAfter)){
                                    DynamicObject supplier = entry.getDynamicObject("nckd_suppliermodify");
                                    supplier = BusinessDataServiceHelper.loadSingle(supplier.getPkValue(),"bd_supplier");
                                    OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_supplier", new DynamicObject[]{supplier}, OperateOption.create());
                                    if (!pushzhwl.isSuccess()){
                                        this.getView().showMessage("供应商：" +supplier.getString("name") + pushzhwl.getMessage());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
