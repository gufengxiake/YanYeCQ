package nckd.yanye.fi.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.fi.fa.business.enums.lease.LeaseContractRentSettleStatus;
import kd.fi.fa.business.lease.utils.LeaseUtil;

import java.util.List;

public class SalaryFaAbstractLeaseContractSubmitOp extends AbstractOperationServicePlugIn {
    public SalaryFaAbstractLeaseContractSubmitOp() {
    }

    public void onPreparePropertys(PreparePropertysEventArgs e) {
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.addAll(this.billEntityType.getAllFields().keySet());
        fieldKeys.add("payplanentryentity.seq");
    }

    public void onAddValidators(AddValidatorsEventArgs e) {
        e.addValidator(new SalaryFaLeaseContractSubmitValidator());
    }

    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        DynamicObject[] var2 = e.getDataEntities();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            DynamicObject leaseContract = var2[var4];
            LeaseUtil.generatePayPlan(leaseContract);
            this.calLeaseContractAmount(leaseContract);
            this.setRentSettleStatus(leaseContract);
        }

    }

    protected void calLeaseContractAmount(DynamicObject leaseContract) {
    }

    private void setRentSettleStatus(DynamicObject leaseContract) {
        boolean needRentSettle = LeaseUtil.isNeedRentSettle(leaseContract);
        String rentSettleStatus = needRentSettle ? LeaseContractRentSettleStatus.A.name() : LeaseContractRentSettleStatus.B.name();
        leaseContract.set("rentsettlestatus", rentSettleStatus);
    }
}
