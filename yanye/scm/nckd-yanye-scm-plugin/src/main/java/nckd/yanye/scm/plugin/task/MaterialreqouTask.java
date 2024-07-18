package nckd.yanye.scm.plugin.task;

import java.time.LocalDate;
import java.util.*;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.botp.ConvertRuleElement;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.exception.KDException;
import kd.bos.metadata.botp.ConvertRuleReader;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

/**
 * @author husheng
 * @date 2024-07-16 16:44
 * @description 下推生成使用评价单定时任务
 */
public class MaterialreqouTask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        // 创建下推参数
        PushArgs pushArgs = new PushArgs();
        //源单单据标识
        String sourceEntityNumber = "im_materialreqoutbill";
        //目标单单据标识
        String targetEntityNumber = "nckd_im_use_evaluate_bill";
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
        List<ListSelectedRow> rows = new ArrayList<>();
        QFilter qFilter1 = new QFilter("billentry.nckd_evaluate_flag", QCP.equals, "1");
        QFilter qFilter2 = new QFilter("billentry.nckd_evaluate_date", QCP.equals, LocalDate.now());
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("im_materialreqoutbill", "billentry.id", new QFilter[]{qFilter1, qFilter2});
        for (DynamicObject dynamicObject : dynamicObjects) {
            rows.add(new ListSelectedRow(dynamicObject.get("id")));

            dynamicObject.set("nckd_already_evaluate",1);
        }
        pushArgs.setSelectedRows(rows);

        // 执行下推操作
        ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
        if(result.isSuccess()){
            SaveServiceHelper.save(dynamicObjects);
        }
    }
}
