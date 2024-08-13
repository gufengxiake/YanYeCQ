package nckd.yanye.fi.plugin.operate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.business.enums.lease.LeaseContractRentSettleStatus;
import kd.fi.fa.business.utils.FaMutexRequireUtil;
import kd.fi.fa.po.GenInterestDetailParamPo;
import nckd.yanye.fi.plugin.operate.SalaryRetirRentSettleGenerator;
import nckd.yanye.fi.plugin.validator.SalaryRetirSettleValidator;


/**
 *
 * @author guozhiwei
 * @date 2024-08-09 10:18
 * @description 退休人员工资计息
 */


public class SalaryRetirSettleOpPlugin extends AbstractOperationServicePlugIn {

    public SalaryRetirSettleOpPlugin() {
    }

    public void onPreparePropertys(PreparePropertysEventArgs e) {
        e.getFieldKeys().add("status");
        e.getFieldKeys().add("isexempt");
        e.getFieldKeys().add("payplanentryentity");
        e.getFieldKeys().add("payruleentryentity");
        e.getFieldKeys().add("rule_payitem");
        e.getFieldKeys().add("bizstatus");
        e.getFieldKeys().add("leaseliab");
        e.getFieldKeys().add("rentsettlestatus");
    }

    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        //  校验器替换
        e.addValidator(new SalaryRetirSettleValidator());
    }

    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        boolean var16 = false;

        try {
            var16 = true;
            List<Long> leaseContractIds = (List)Stream.of(e.getDataEntities()).map((v) -> {
                return v.getLong("id");
            }).collect(Collectors.toList());
            List<GenInterestDetailParamPo> paramPos = new ArrayList(leaseContractIds.size());
            leaseContractIds.forEach((v) -> {
                paramPos.add(new GenInterestDetailParamPo(v));
            });
//            RentSettleGenerator generator = new RentSettleGenerator(paramPos);
            // 替换成退养人员单据
            SalaryRetirRentSettleGenerator generator = new SalaryRetirRentSettleGenerator(paramPos);
            generator.generate();
            DynamicObject[] var5 = e.getDataEntities();
            int var6 = var5.length;
            int var7 = 0;

            while(true) {
                if (var7 >= var6) {
                    SaveServiceHelper.save(e.getDataEntities());
                    var16 = false;
                    break;
                }

                DynamicObject dataEntity = var5[var7];
                dataEntity.set("rentsettlestatus", LeaseContractRentSettleStatus.C.name());
                ++var7;
            }
        } finally {
            if (var16) {
                HashSet ids = new HashSet(8);
                DynamicObject[] var11 = e.getDataEntities();
                int var12 = var11.length;

                for(int var13 = 0; var13 < var12; ++var13) {
                    DynamicObject dataEntity = var11[var13];
                    ids.add(dataEntity.getLong("id"));
                }

                FaMutexRequireUtil.batchRelease("nckd_fa_salary_retir", ids, "leaseContractRentSettle", "generateRentSettle");
            }
        }

        Set<Long> ids = new HashSet(8);
        DynamicObject[] var19 = e.getDataEntities();
        int var20 = var19.length;

        for(int var21 = 0; var21 < var20; ++var21) {
            DynamicObject dataEntity = var19[var21];
            ids.add(dataEntity.getLong("id"));
        }

        FaMutexRequireUtil.batchRelease("nckd_fa_salary_retir", ids, "leaseContractRentSettle", "generateRentSettle");
    }
}

