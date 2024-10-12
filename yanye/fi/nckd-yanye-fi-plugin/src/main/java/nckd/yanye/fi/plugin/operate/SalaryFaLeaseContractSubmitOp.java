package nckd.yanye.fi.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.opplugin.lease.FaLeaseContractExemptPropertyValidator;

/**
 * Module           :财务云-租赁管理-退养人员工资
 * Description      :退养人员工资提交生成付款计划
 *  nckd_fa_salary_retir
 * @author : guozhiwei
 * @date : 2024/8/7
 */

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
