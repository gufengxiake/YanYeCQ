package nckd.yanye.fi.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.fa.business.enums.lease.LeaseContractSourceType;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.opplugin.lease.FaGeneratePayPlanValidator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Module           :财务云-租赁管理-退养人员工资
 * Description      : 退休人员工资生成付款计划组件
 * @author guozhiwei
 * @date  2024/8/7 11:01
 *  标识:nckd_fa_salary_retir
 *
 */
public class SalaryFaGeneratePayPlanOp extends AbstractOperationServicePlugIn {
    public SalaryFaGeneratePayPlanOp() {
    }

    public void onPreparePropertys(PreparePropertysEventArgs e) {
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.addAll(this.billEntityType.getAllFields().keySet());
    }

    public void onAddValidators(AddValidatorsEventArgs e) {
        e.addValidator(new FaGeneratePayPlanValidator());
    }

    public void endOperationTransaction(EndOperationTransactionArgs e) {
        DynamicObject[] dataEntities = e.getDataEntities();
        DynamicObject[] var3 = dataEntities;
        int var4 = dataEntities.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject entity = var3[var5];
            String sourceType = entity.getString("sourcetype");
            if (LeaseContractSourceType.C.name().equals(sourceType)) {
                Object chgoricontractId = entity.get("masterid");
                DynamicObject chgoricontract = null;
                if (chgoricontractId != null) {
                    chgoricontract = BusinessDataServiceHelper.loadSingle(chgoricontractId, EntityMetadataCache.getDataEntityType("fa_lease_contract"));
                }

                LeaseUtil.generatePayPlan4Chg(entity, chgoricontract);
            } else {
                LeaseUtil.generatePayPlan(entity);
            }

            if (LeaseContractSourceType.A.name().equals(sourceType)) {
                entity.set("leaseliab", BigDecimal.ZERO);
                entity.set("leaseliabori", BigDecimal.ZERO);
                entity.set("leaseassets", BigDecimal.ZERO);
                entity.set("assetsaccumdepre", BigDecimal.ZERO);
            }
        }

    }
}
