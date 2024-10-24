package nckd.yanye.fi.plugin.validator;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.entity.validate.BillStatus;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.fa.business.enums.lease.LeaseContractBizStatus;
import kd.fi.fa.business.enums.lease.LeaseContractRentSettleStatus;
import kd.fi.fa.business.lease.LeaseFutureBizChecker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Module           :财务云-租赁管理-退养人员工资
 * Description      :退养人员工资计息反计息操作校验
 *
 * @author : guozhiwei
 * @date : 2024/8/8
 */


public class SalaryFaRentUnSettleValidator extends AbstractValidator {
    public SalaryFaRentUnSettleValidator() {
    }

    public void validate() {
        ExtendedDataEntity[] dataEntities = this.getDataEntities();
        ExtendedDataEntity[] var2 = dataEntities;
        int var3 = dataEntities.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            ExtendedDataEntity dataEntity = var2[var4];
            DynamicObject leaseContract = dataEntity.getDataEntity();
            Long contractId = (Long)leaseContract.getPkValue();
            String status = leaseContract.getString("status");
            String rentStatus = leaseContract.getString("rentsettlestatus");
            List<QFilter> settlefilterList = new ArrayList();
            settlefilterList.add(new QFilter("leasecontract", "=", contractId));
            DynamicObjectCollection rentSettleList = QueryServiceHelper.query("nckd_fa_lease_rent_settle", "id", (QFilter[])settlefilterList.toArray(new QFilter[0]));
            List<Long> rentSettleIdList = (List)rentSettleList.stream().map((v) -> {
                return v.getLong("id");
            }).collect(Collectors.toList());
            boolean isGenSettleData = status.equals(BillStatus.C.name()) && LeaseContractRentSettleStatus.C.name().equals(rentStatus);
            if (!isGenSettleData) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("单据未审核或未计息。", "FaRentUnSettleValidator_0", "fi-fa-opplugin", new Object[0]));
            }

            String bizStatus = leaseContract.getString("bizstatus");
            if (LeaseContractBizStatus.B.name().equals(bizStatus)) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("已终止的单据不允许反计息。", "FaRentUnSettleValidator_1", "fi-fa-opplugin", new Object[0]));
            }

            List<QFilter> voucherfilterList = new ArrayList();
            QFilter rentSettleFilter = new QFilter("sourcebillid", "in", rentSettleIdList);
            voucherfilterList.add(rentSettleFilter);
            QFilter billTypeFilter = new QFilter("billType", "=", "nckd_fa_lease_rent_settle");
            voucherfilterList.add(billTypeFilter);
            boolean isGenVoucher = QueryServiceHelper.exists("ai_daptracker", (QFilter[])voucherfilterList.toArray(new QFilter[0]));
            if (isGenVoucher) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("单据对应的摊销与计息记录已生成凭证，不能反计息。", "FaRentUnSettleValidator_2", "fi-fa-opplugin", new Object[0]));
            }

            boolean existFutureLeaseChangeBill = LeaseFutureBizChecker.existFutureLeaseChangeBill((Object)null, contractId, (Date)null);
            if (existFutureLeaseChangeBill) {
                this.addErrorMessage(dataEntity, ResManager.loadKDString("存在后续变更单，不允许反计息。", "FaRentUnSettleValidator_3", "fi-fa-opplugin", new Object[0]));
            }
        }

    }
}
