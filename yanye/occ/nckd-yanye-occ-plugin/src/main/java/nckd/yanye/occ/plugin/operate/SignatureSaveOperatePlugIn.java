package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.botp.runtime.BFRow;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/*
签收单保存反写签收日期到销售出库单
 */
public class SignatureSaveOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_signdate");//签收日期
    }
    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        String targetEntityNumber = this.billEntityType.getName();
        if(deliverRecords != null){
            for(DynamicObject dataObject:deliverRecords){
                Object signdate=dataObject.getDate("nckd_signdate");
                if(signdate!=null){
                    Object billId=dataObject.getPkValue();
                    Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber,new Long[]{(Long) billId});
                    if (sourceBillIds.containsKey("im_saloutbill")) {
                        HashSet<Long> botpbill1_Ids = sourceBillIds.get("im_saloutbill");
                        for (long id:botpbill1_Ids){
                            DynamicObject date= BusinessDataServiceHelper.loadSingle(id,"im_saloutbill");
                            date.set("nckd_signdate",signdate);
                            SaveServiceHelper.save(new DynamicObject[]{date});
                        }
                    }
                }
            }
        }
    }
}
