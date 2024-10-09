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
 * 主数据-客户变更单列表插件（推送智慧物流）
 * 表单标识：bd_customer
 * author：xiaoxiaopeng
 * date：2024-09-25
 */
public class BdCustomerChangeListPlugin extends AbstractListPlugin {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        String key = e.getOperateKey();
        if ("audit".equals(key)) {
            OperationResult operationResult = e.getOperationResult();
            if (operationResult.isSuccess()){
                for (ListSelectedRow row : selectedRows) {
                    DynamicObject customerChange = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "nckd_bd_customer_change");
                    DynamicObjectCollection entity = customerChange.getDynamicObjectCollection("nckd_entry");
                    if (entity.size() > 0){
                        for (DynamicObject entry : entity) {
                            String changeAfter = entry.getString("nckd_changeafter");
                            if (StringUtils.isEmpty(changeAfter)){
                                String addCustomer = entry.getString("nckd_addcustomer");
                                DynamicObject customer = BusinessDataServiceHelper.loadSingle("bd_customer","id",new QFilter[]{new QFilter("name", QCP.equals,addCustomer)});
                                OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_customer", new DynamicObject[]{customer}, OperateOption.create());
                                if (!pushzhwl.isSuccess()){
                                    this.getView().showMessage("客户：" +customer.getString("name") + pushzhwl.getMessage());
                                }
                                continue;
                            }
                            if ("2".equals(changeAfter)){
                                DynamicObject customer = entry.getDynamicObject("nckd_customermodify");
                                customer = BusinessDataServiceHelper.loadSingle(customer.getPkValue(),"bd_customer");
                                OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bd_customer", new DynamicObject[]{customer}, OperateOption.create());
                                if (!pushzhwl.isSuccess()){
                                    this.getView().showMessage("客户：" +customer.getString("name") + pushzhwl.getMessage());
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
