package nckd.yanye.fi.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.validate.AbstractValidator;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SalaryFaLeaseContractSubmitValidator extends AbstractValidator {
    public SalaryFaLeaseContractSubmitValidator() {
    }

    public Set<String> preparePropertys() {
        Set<String> propSet = super.preparePropertys();
        propSet.add("freeleasestartdate");
        propSet.add("isdeductible");
        propSet.add("deductinputtax");
        propSet.add("prepayrent");
        propSet.add("deinputtaxforpre");
        propSet.add("isexempt");
        propSet.add("initcost");
        propSet.add("payplanentryentity");
        propSet.add("rentnotax");
        propSet.add("tax");
        propSet.add("rent");
        return propSet;
    }

    public void validate() {
        ExtendedDataEntity[] dataEntities = this.dataEntities;
        ExtendedDataEntity[] var2 = dataEntities;
        int var3 = dataEntities.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            ExtendedDataEntity dataEntity = var2[var4];
            DynamicObject leaseContract = dataEntity.getDataEntity();
            List<String> errorInfoList = SalaryLeaseContractValidator.validateContractForSubmit(leaseContract);
            Iterator var8 = errorInfoList.iterator();

            while(var8.hasNext()) {
                String errorInfo = (String)var8.next();
                this.addErrorMessage(dataEntity, errorInfo);
            }
        }

    }
}
