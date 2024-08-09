package nckd.yanye.fi.plugin.operate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.servicehelper.MetadataServiceHelper;
import kd.imc.sim.formplugin.issuing.helper.IssueInvoiceControlHelper;

/**
 * @author husheng
 * @date 2024-08-09 10:18
 * @description 开票申请审核时一键开票
 */
public class FinarbillOpPlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);

        List<String> fieldKeys = e.getFieldKeys();
        MainEntityType dt = MetadataServiceHelper.getDataEntityType("sim_original_bill");
        Map<String, IDataEntityProperty> fields = dt.getAllFields();
        fields.forEach((Key, value) -> {
            fieldKeys.add(Key);
        });

        fieldKeys.add("sim_original_bill_item.seq");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);

        DynamicObject[] dynamicObjects = Arrays.stream(e.getDataEntities())
                .filter(dynamicObject -> Objects.equals(dynamicObject.get("nckd_sfzdkp"), true))
                .collect(Collectors.toList()).toArray(new DynamicObject[]{});

        if(dynamicObjects.length > 0){
            // 一键开票
            IssueInvoiceControlHelper.issueInvoice(dynamicObjects, 1, false, false);
        }
    }
}
