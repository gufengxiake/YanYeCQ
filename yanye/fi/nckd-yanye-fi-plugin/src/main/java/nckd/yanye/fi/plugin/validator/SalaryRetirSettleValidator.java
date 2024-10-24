package nckd.yanye.fi.plugin.validator;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.entity.validate.BillStatus;
import kd.fi.fa.business.enums.lease.LeaseContractBizStatus;
import kd.fi.fa.business.enums.lease.LeaseContractRentSettleStatus;
import kd.fi.fa.business.utils.FaMutexRequireUtil;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Module           :财务云-租赁管理-退养人员工资
 * @author guozhiwei
 * @date  2024/8/5 15:18
 * @description  退养人员生成摊销与计息校验
 *  标识:nckd_fa_salary_retir
 *
 */

public class SalaryRetirSettleValidator extends AbstractValidator {
    public SalaryRetirSettleValidator() {
    }

    public void validate() {
        ExtendedDataEntity[] dataEntities = this.getDataEntities();
        Set<Long> ids = new HashSet(dataEntities.length);
        ExtendedDataEntity[] var3 = dataEntities;
        int var4 = dataEntities.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            ExtendedDataEntity dataEntity = var3[var5];
            ids.add((Long)dataEntity.getBillPkId());
        }

        Set<Long> mutexIds = FaMutexRequireUtil.requireMutexBatch("nckd_fa_salary_retir", ids, "leaseContractRentSettle", "generateRentSettle");
        ids.removeAll(mutexIds);
        Set<Long> releaseIds = new HashSet(dataEntities.length);
        ExtendedDataEntity[] var17 = dataEntities;
        int var18 = dataEntities.length;

        for(int var7 = 0; var7 < var18; ++var7) {
            ExtendedDataEntity dataEntity = var17[var7];
            DynamicObject leaseContract = dataEntity.getDataEntity();
            long id = leaseContract.getLong("id");
            if (ids.contains(id)) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("当前单据正在计息，请稍后再试。", "FaRentSettleValidator_0", "fi-fa-opplugin", new Object[0]));
            }

            String status = leaseContract.getString("status");
            if (!status.equals(BillStatus.C.name())) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("单据审核状态为未审核，操作失败。", "FaRentSettleValidator_1", "fi-fa-opplugin", new Object[0]));
                releaseIds.add(id);
            }

            String bizStatus = leaseContract.getString("bizstatus");
            if (LeaseContractBizStatus.B.name().equals(bizStatus)) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("已终止的单据不能计息。", "FaRentSettleValidator_2", "fi-fa-opplugin", new Object[0]));
                releaseIds.add(id);
            }

            String rentStatus = leaseContract.getString("rentsettlestatus");
            if (LeaseContractRentSettleStatus.B.name().equals(rentStatus)) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("单据无需计息。", "FaRentSettleValidator_3", "fi-fa-opplugin", new Object[0]));
                releaseIds.add(id);
            } else if (LeaseContractRentSettleStatus.C.name().equals(rentStatus)) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("单据已生成摊销与计息数据，不能重复生成。", "FaRentSettleValidator_4", "fi-fa-opplugin", new Object[0]));
                releaseIds.add(id);
            }

            if (!releaseIds.isEmpty()) {
                FaMutexRequireUtil.batchRelease("nckd_fa_salary_retir", releaseIds, "leaseContractRentSettle", "generateRentSettle");
            }
        }

    }
}
