package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.IOperateInfo;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.exception.KDBizException;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;

import java.util.HashSet;
import java.util.Map;

/**
 * 销售出库单列表拒签
 * 表单标识：nckd_im_saloutbill_ext
 * author:wgq
 * date:2024/09/26
 */
public class SalOutUnsignOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_autosign");//销售组织

    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        String targetEntityNumber = this.billEntityType.getName();
        for (DynamicObject dataEntity : e.getDataEntities()) {
            Object pkId = dataEntity.getPkValue();
            DynamicObject dataAllEntity = BusinessDataServiceHelper.loadSingle(pkId, targetEntityNumber);
            dataAllEntity.set("nckd_autosign", false);
            //SaveServiceHelper.update(dataEntity);
            OperateOption auditOption = OperateOption.create();
            auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
            auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
            //审核
            OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetEntityNumber, new DynamicObject[]{dataAllEntity}, auditOption);
            if (!auditResult.isSuccess()) {
                String detailMessage = auditResult.getMessage();
                // 演示提取保存详细错误
                for (IOperateInfo errInfo : auditResult.getAllErrorOrValidateInfo()) {
                    detailMessage += errInfo.getMessage();
                }

                throw new KDBizException("单据审核失败：" + detailMessage);
            }
            Map<String, HashSet<Long>> targetBills = BFTrackerServiceHelper.findTargetBills(targetEntityNumber, new Long[]{(Long) pkId});
            String botpbill1_EntityNumber = "ocbsoc_delivery_record";//发货记录
            if (targetBills.containsKey(botpbill1_EntityNumber)) {
                HashSet<Long> botpbill1_Ids = targetBills.get(botpbill1_EntityNumber);
                for (Long Id : botpbill1_Ids) {
                    DynamicObject delivery = BusinessDataServiceHelper.loadSingle(Id, botpbill1_EntityNumber);
                    OperationResult unsignResult = OperationServiceHelper.executeOperate("unsign", botpbill1_EntityNumber, new DynamicObject[]{delivery});
                    if (!unsignResult.isSuccess()) {
                        String detailMessage = "";
                        // 演示提取保存详细错误
                        for (IOperateInfo errInfo : unsignResult.getAllErrorOrValidateInfo()) {
                            detailMessage += errInfo.getMessage();
                        }
                        throw new KDBizException("发货记录拒签失败：" + detailMessage);
                    }
                }
            }

        }
    }
}
