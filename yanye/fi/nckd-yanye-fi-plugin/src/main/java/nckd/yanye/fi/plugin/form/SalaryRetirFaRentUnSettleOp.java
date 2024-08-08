package nckd.yanye.fi.plugin.form;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.DeleteServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.business.depretask.DepreSplitSumUtils;
import kd.fi.fa.business.enums.lease.LeaseContractRentSettleStatus;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author guozhiwei
 * @date  2024/8/6 18：01
 * @description  退养人员费用反计息操作
 *  标识:nckd_fa_salary_retir
 *
 */

public class SalaryRetirFaRentUnSettleOp extends AbstractOperationServicePlugIn {
    public SalaryRetirFaRentUnSettleOp() {
    }

    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new SalaryFaRentUnSettleValidator());
    }

    public void onPreparePropertys(PreparePropertysEventArgs e) {
        e.getFieldKeys().add("id");
        e.getFieldKeys().add("status");
        e.getFieldKeys().add("bizstatus");
        e.getFieldKeys().add("rentsettlestatus");
    }

    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        List<Long> contractIdList = (List) Stream.of(e.getDataEntities()).map((v) -> {
            return v.getLong("id");
        }).collect(Collectors.toList());
        QFilter rentSettleFilter = new QFilter("leasecontract", "in", contractIdList);
//        LeaseUtil.deleteRentSettle(new QFilter[]{rentSettleFilter}, true);
        deleteRentSettle(new QFilter[]{rentSettleFilter}, true);
        QFilter interestDetailFilter = new QFilter("leasecontract", "in", contractIdList);
        DeleteServiceHelper.delete("fa_interest_detail", new QFilter[]{interestDetailFilter});
        DynamicObject[] var5 = e.getDataEntities();
        int var6 = var5.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            DynamicObject dataEntity = var5[var7];
            dataEntity.set("rentsettlestatus", LeaseContractRentSettleStatus.A.name());
        }

        SaveServiceHelper.save(e.getDataEntities());
    }

    public static void deleteRentSettle(QFilter[] filters, boolean syncIep) {
        DynamicObjectCollection rentSettles = QueryServiceHelper.query("nckd_fa_lease_rent_settle", "id", filters);
        Object[] pks = rentSettles.stream().map((v) -> {
            return v.get("id");
        }).toArray();
        if (syncIep) {
            DepreSplitSumUtils.deleteIntelliWhitelist(pks, "nckd_fa_lease_rent_settle");
        }

        DeleteServiceHelper.delete(EntityMetadataCache.getDataEntityType("nckd_fa_lease_rent_settle"), pks);
    }
}

