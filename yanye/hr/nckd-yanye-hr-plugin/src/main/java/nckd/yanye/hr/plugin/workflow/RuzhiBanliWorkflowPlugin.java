package nckd.yanye.hr.plugin.workflow;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.workflow.api.AgentExecution;
import kd.bos.workflow.engine.extitf.IWorkflowPlugin;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.hr.hom.business.application.utils.RuleCodeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流：江盐-正式员工入职流程  单据：入职办理单
 * 需求：入职办理完成审批时，自动生成员工工号
 * author: chengchaohua
 * date: 2024-09-03
 */
public class RuzhiBanliWorkflowPlugin implements IWorkflowPlugin {
    private static final Log logger = LogFactory.getLog(RuzhiBanliWorkflowPlugin.class);

    // 正向流转触发
    @Override
    public void notify(AgentExecution execution) {
        logger.error("cch-标记1");
        IWorkflowPlugin.super.notify(execution);
        // 1）获取单据的业务id
        String businessKey = execution.getBusinessKey();
        logger.error("cch-标记2：" + businessKey);

        // 2）生成工号
        HRBaseServiceHelper serviceHelper = new HRBaseServiceHelper("hom_employeeno");
        DynamicObject dy = serviceHelper.generateEmptyDynamicObject();
        List<String> numberList = new ArrayList(1);
        RuleCodeUtils.codeRuleHandler(dy, numberList, 1, 1);
        String newNumber = "";
        if (!numberList.isEmpty()) {
            logger.error("cch-标记2-2：");
            newNumber = (String)numberList.get(0);
        }
        logger.error("cch-标记3：" + newNumber);
        // 3）入职办理单更新工号
        DynamicObject obj = new DynamicObject();
        obj.set("id",businessKey);
        obj.set("employeeno",newNumber);

        HRBaseServiceHelper hrBaseServiceHelper = new HRBaseServiceHelper("hom_onbrdinfo");
        hrBaseServiceHelper.updateDataOne(obj);
        logger.error("cch-标记4：");
    }
}
