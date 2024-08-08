package nckd.yanye.fi.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.opplugin.lease.FaLeaseContractExemptPropertyValidator;

public class SalaryFaLeaseContractSubmitOp extends SalaryFaAbstractLeaseContractSubmitOp {

    public SalaryFaLeaseContractSubmitOp() {
    }

    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new FaLeaseContractExemptPropertyValidator());
    }

    protected void calLeaseContractAmount(DynamicObject leaseContract) {
        LeaseUtil.calLeaseContractAmount4Submit(leaseContract);
    }



}
