package nckd.yanye.hr.plugin.workflow;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.workflow.api.AgentExecution;
import kd.bos.workflow.engine.rule.ext.IExtExpressionParse;

/**
 * 工作流-考勤人详情-职级扩展引用
 * @author wenxin 2024-09-26
 */
public class WorkRankImpl implements IExtExpressionParse {
    private static final Log logger = LogFactory.getLog(WorkRankImpl.class);

    @Override
    public Object parseExpression(AgentExecution agentExecution, Object param) {
        if(param == null) {
            return null;
        }
        //传入考勤人的id
        Long id = (Long)param;
        logger.info("传入的值："+id);
        //查询<考勤人详情>数据
        String name = "";
        DynamicObjectCollection query = QueryServiceHelper.query("nckd_wtp_attendperdet_ext", "empposorgrelhr.name as names", new QFilter[]{new QFilter("attendperson.id", QCP.equals, id)}, null);
        if(ObjectUtil.isNotNull(query)){
            try {
                logger.info("查询的对象："+ JSON.toJSONString(query));
            }catch (Exception e){

            }
            name = query.get(0).getString("names");
        }
        logger.info("返回的值："+name);
        return name;
    }
}
