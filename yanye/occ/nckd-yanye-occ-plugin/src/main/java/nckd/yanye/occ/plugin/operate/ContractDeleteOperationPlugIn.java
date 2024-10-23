package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

/**
 * 销售/采购合同删除同步清空销售订单上的销售/运输合同号
 * 表单标识: nckd_conm_salcontract_ext
 * @author zhangzhilong
 * @since 2024/10/23
 */
public class ContractDeleteOperationPlugIn extends AbstractOperationServicePlugIn implements Plugin {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("billno");
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] contracts = e.getDataEntities();
        Set<String> billNos = new HashSet<>();
        for (DynamicObject contract : contracts) {
            billNos.add(contract.getString("billno"));
        }
        if (billNos.isEmpty()) {
            return;
        }
        QFilter sFilter = new QFilter("nckd_salecontractno", QCP.in,billNos.toArray(new String[0]));
        DynamicObject[] salorder = BusinessDataServiceHelper.load("sm_salorder", "nckd_salecontractno", new QFilter[]{sFilter});
        if (salorder == null || salorder.length == 0) {
            QFilter tFilter = new QFilter("nckd_trancontractno", QCP.in,billNos.toArray(new String[0]));
            salorder = BusinessDataServiceHelper.load("sm_salorder", "nckd_trancontractno", new QFilter[]{tFilter});
            if (salorder == null || salorder.length == 0) {
                return;
            }
            for (DynamicObject dynamicObject : salorder) {
                dynamicObject.set("nckd_trancontractno", null);
            }
        }else{
            for (DynamicObject dynamicObject : salorder) {
                dynamicObject.set("nckd_salecontractno",null);
            }
        }
        SaveServiceHelper.update(salorder);
    }
}