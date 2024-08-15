package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;

public class SalOrderSubmitOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("r_needrecadvance");//是否预收
        e.getFieldKeys().add("nckd_amountfieldys");//预收金额
        e.getFieldKeys().add("recplanentry.r_unremainamount");//未关联收款金额
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        for (DynamicObject dataObject : deliverRecords) {
            DynamicObjectCollection recplanentry = dataObject.getDynamicObjectCollection("recplanentry");
            BigDecimal count = BigDecimal.ZERO;
            for (DynamicObject recplanRow : recplanentry) {
                Boolean ys=recplanRow.getBoolean("r_needrecadvance");
                if(ys){
                    BigDecimal r_remainamount = recplanRow.getBigDecimal("r_unremainamount");
                    count = count.add(r_remainamount);
                }
            }
            dataObject.set("nckd_amountfieldys", count);
            SaveServiceHelper.save(new DynamicObject[]{dataObject});
        }
    }
}
