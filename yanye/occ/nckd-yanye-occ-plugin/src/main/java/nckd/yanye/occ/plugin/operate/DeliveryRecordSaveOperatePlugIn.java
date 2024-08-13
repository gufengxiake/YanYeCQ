package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.occ.ocbsoc.opplugin.delivery.DeliveryRecordSavePlugin;

public class DeliveryRecordSaveOperatePlugIn extends DeliveryRecordSavePlugin {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_autosign");
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if(deliverRecords != null && deliverRecords.length > 0){
            for(DynamicObject dataObject:deliverRecords){
                boolean autoSign=dataObject.getBoolean("nckd_autosign");
                if(autoSign){
                     OperationServiceHelper.executeOperate("sign","ocbsoc_delivery_record",new DynamicObject[]{dataObject});
                }
            }
        }
    }
}
