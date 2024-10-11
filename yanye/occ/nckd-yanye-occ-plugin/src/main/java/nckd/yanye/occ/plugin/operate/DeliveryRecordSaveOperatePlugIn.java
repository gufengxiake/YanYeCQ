package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.operation.OperationServiceHelper;

/*
 * 发货记录保存服务插件，自动签收
 * 表单标识：nckd_ocbsoc_delivery_ext
 * author:吴国强 2024-07-20
 */

public class DeliveryRecordSaveOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_autosign");
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if(deliverRecords != null){
            for(DynamicObject dataObject:deliverRecords){
                boolean autoSign=dataObject.getBoolean("nckd_autosign");
                if(autoSign){
                     OperationServiceHelper.executeOperate("sign","ocbsoc_delivery_record",new DynamicObject[]{dataObject});
                }
            }
        }
    }
}
