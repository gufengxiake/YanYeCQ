package nckd.yanye.hr.plugin.workflow;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
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
        // 1）获取单据的业务fid
        String businessKey = execution.getBusinessKey();
        logger.error("cch-标记2：" + businessKey);

        // 2）生成工号
        // 2.1) 入职单据基础资料 构建一个实例对象
        HRBaseServiceHelper baseServiceHelper = new HRBaseServiceHelper("hom_onbrdbillbase"); // 入职单据基础资料
        DynamicObject baseDy = baseServiceHelper.generateEmptyDynamicObject();
        // 查询入职单数据 H20240828011050：2026895654906770432L
        DynamicObject ruzhidata = BusinessDataServiceHelper.loadSingle(businessKey,"hom_onbrdbillbase");
        // laborreltype 基础资料，用工关系类型:1010  劳动合同 雇佣员工
        baseDy.set("laborreltype", ruzhidata.get("laborreltype"));

        // 2.2) 工号编码规则 构建一个实例对象
        HRBaseServiceHelper serviceHelper = new HRBaseServiceHelper("hom_employeeno");
        DynamicObject dy = serviceHelper.generateEmptyDynamicObject();
        dy.set("onbrdbill", baseDy); // 工号编码规则 的 入职单 字段： onbrdbill，把 入职单据基础资料实例 放入规则中

        // 2.3) 调编码规则生成工号
        List<String> numberList = new ArrayList(1);
        RuleCodeUtils.codeRuleHandler(dy, numberList, 1, 1);
        String newNumber = "";
        if (!numberList.isEmpty()) {
            logger.error("cch-标记2-2：");
            newNumber = (String)numberList.get(0);
        }
        logger.error("cch-标记3：" + newNumber);

        // 3）入职办理单更新工号
        ruzhidata.set("employeeno",newNumber);

        ArrayList<DynamicObject> dyList = new ArrayList<>();
        dyList.add(ruzhidata);

        SaveServiceHelper.save(dyList.toArray(new DynamicObject[dyList.size()]));
        logger.error("cch-标记4：保存完成");
    }
}
