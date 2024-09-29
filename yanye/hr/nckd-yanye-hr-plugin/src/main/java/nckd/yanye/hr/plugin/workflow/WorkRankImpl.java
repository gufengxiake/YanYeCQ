package nckd.yanye.hr.plugin.workflow;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import kd.bos.dataentity.entity.DynamicObject;
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
        logger.info("进入：WorkRankImpl");
        try {
            logger.info("进入：WorkRankImpl param"+ JSON.toJSONString(param));
        }catch (Exception e){

        }
        //传入考勤人的id
        Long uid = 0L;
        if(param instanceof Long){
            uid = (Long)param;
        }else if(param instanceof DynamicObject){
            DynamicObject dynObj = (DynamicObject) param;
            uid = dynObj.getLong("id");
        }else if(param instanceof String && !param.toString().contains(",")){
            uid = Long.parseLong(param.toString());
        }
        logger.info("传入的值："+uid);
        //查询<考勤人详情>数据
        String name = "";
        DynamicObjectCollection query = QueryServiceHelper.query("nckd_wtp_attendperdet_ext", "empposorgrelhr.name as names", new QFilter[]{new QFilter("attendperson.id", QCP.equals, uid)}, null);
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
