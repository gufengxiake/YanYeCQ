package nckd.yanye.fi.plugin.validator;

import java.util.List;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.validate.AbstractValidator;
import kd.fi.fa.business.validator.lease.LeaseContractValidator;

public class SalaryFaGeneratePayPlanValidator extends AbstractValidator {
    public SalaryFaGeneratePayPlanValidator() {
    }

    public void validate() {
        ExtendedDataEntity[] var1 = this.dataEntities;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ExtendedDataEntity entity = var1[var3];
            DynamicObject leaseContract = entity.getDataEntity();
            List<String> errorInfo = SalaryLeaseContractValidator.validateForGeneratePayPlan(leaseContract);
            if (!errorInfo.isEmpty()) {
                errorInfo.forEach((v) -> {
                    this.addErrorMessage(entity, v);
                });
            }
        }

    }
}
