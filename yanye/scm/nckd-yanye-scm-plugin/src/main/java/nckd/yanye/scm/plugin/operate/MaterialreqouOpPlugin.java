package nckd.yanye.scm.plugin.operate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.metadata.botp.ConvertRuleReader;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import org.apache.commons.lang.StringUtils;

/**
 * @author husheng
 * @date 2024-07-18 16:43
 * @description  领料出库单审批通过后下推生成需求申请单并提交
 */
public class MaterialreqouOpPlugin extends AbstractOperationServicePlugIn {
//    @Override
//    public void onPreparePropertys(PreparePropertysEventArgs e) {
//        super.onPreparePropertys(e);
//
//        List<String> fieldKeys = e.getFieldKeys();
//        fieldKeys.add("nckd_other_depart");
//    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);

        List<ListSelectedRow> rows = new ArrayList<>();
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dynamicObject : dataEntities) {
            Boolean otherDepart = (Boolean) dynamicObject.get("nckd_other_depart");
            if(otherDepart){
                rows.add(new ListSelectedRow(dynamicObject.getPkValue()));
            }
        }

        if(rows.size() > 0){
            // 创建下推参数
            PushArgs pushArgs = new PushArgs();
            //源单单据标识
            String sourceEntityNumber = "im_materialreqoutbill";
            //目标单单据标识
            String targetEntityNumber = "pm_requirapplybill";
            pushArgs.setSourceEntityNumber(sourceEntityNumber);
            pushArgs.setTargetEntityNumber(targetEntityNumber);
            //不检查目标单新增权限
            pushArgs.setHasRight(true);
            //下推后默认保存
            pushArgs.setAutoSave(true);
            //是否生成单据转换报告
            pushArgs.setBuildConvReport(false);

            // 单据转换规则id
            ConvertRuleReader reader = new ConvertRuleReader();
            List<String> ruleIds = reader.loadRuleIds(sourceEntityNumber, targetEntityNumber, false);
            if(ruleIds.size() > 0){
                pushArgs.setRuleId(ruleIds.get(0));
            }

            //构建选中行数据包
            pushArgs.setSelectedRows(rows);

            // 执行下推操作
            ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
            if(result.isSuccess()){
                // 获取下推目标单id
                Set<Object> targetBillIds = result.getTargetBillIds();
                // 需求申请单提交审批
                OperationResult submit = OperationServiceHelper.executeOperate("submit", "nckd_pm_requirapplybi_ext", targetBillIds.toArray(), OperateOption.create());
            }
        }
    }
}
