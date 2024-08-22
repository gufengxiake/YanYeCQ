package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.HashSet;
import java.util.Map;


/**
 * 签收单删除清空销售出库单签收日期
 * 表单标识：nckd_signaturebill
 * author:吴国强 2024-07-12
 */
public class SignatureDeleteOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_signdate");//签收日期
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        String targetEntityNumber = this.billEntityType.getName();
        if (deliverRecords != null) {
            for (DynamicObject dataObject : deliverRecords) {
                Object billId = dataObject.getPkValue();
                Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, new Long[]{(Long) billId});
                if (sourceBillIds.containsKey("im_saloutbill")) {
                    HashSet<Long> botpbill1_Ids = sourceBillIds.get("im_saloutbill");
                    for (long id : botpbill1_Ids) {
                        DynamicObject date = BusinessDataServiceHelper.loadSingle(id, "im_saloutbill");
                        date.set("nckd_signdate", null);
                        SaveServiceHelper.save(new DynamicObject[]{date});
                    }
                }

            }
        }
    }
}
