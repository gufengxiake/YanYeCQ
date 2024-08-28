package nckd.yanye.tmc.plugin.operate;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.common.util.DateUtil;
import java.util.Date;

/**
 * Module           :资金云-票据管理-票据登记-收票登记
 * Description      :收票登记承兑期限计算
 *
 *
 * @author guozhiwei
 * @date  2024/8/15 11:02
 * 标识 nckd_cdm_receivablebi_ext
 *
 *
 */
public class BillReceivableSubmit extends AbstractOperationServicePlugIn {


    public BillReceivableSubmit() {
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        process(e);
    }

    public void process(AfterOperationArgs e) {
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            Date issuedate = dataEntity.getDate("issuedate");
            Date draftbillexpiredate = dataEntity.getDate("draftbillexpiredate");
            if(issuedate!= null && draftbillexpiredate != null){
                int diffMonths = DateUtil.getDiffMonthsByLocalDate(issuedate, draftbillexpiredate, false, true);
                String period = null;
                if(diffMonths>6){
                    period = "B";
                }else{
                    period = "A";
                }
                dataEntity.set("nckd_acceptance_period",period);
                SaveServiceHelper.save(new DynamicObject[]{dataEntity});
            }
        }
    }
}
