package nckd.yanye.tmc.plugin.operate;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.common.util.DateUtil;
import kd.tmc.fbp.common.util.EmptyUtil;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        boolean isSuccess = true;
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            Date issuedate = dataEntity.getDate("issuedate");
            Date draftbillexpiredate = dataEntity.getDate("draftbillexpiredate");
            if(issuedate!= null && draftbillexpiredate != null){
                int diffMonths = DateUtil.getDiffMonthsByLocalDate(issuedate, draftbillexpiredate, false, true);
                if(diffMonths>6){
                    dataEntity.set("nckd_acceptance_period","B");
//                    this.getModel().setValue("nckd_acceptance_period","B");
                }else{
                    dataEntity.set("nckd_acceptance_period","A");
                }
                SaveServiceHelper.save(new DynamicObject[]{dataEntity});
            }
        }

    }




}
