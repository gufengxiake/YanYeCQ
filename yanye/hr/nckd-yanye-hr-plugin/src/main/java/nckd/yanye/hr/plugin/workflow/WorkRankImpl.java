package nckd.yanye.hr.plugin.workflow;

import cn.hutool.core.convert.Convert;
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
        logger.info("进入：WorkRankImpl职级扩展");
        if(param == null) {
            return null;
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
        String name = "";
        //1、获取工号
        DynamicObjectCollection queryNumber = QueryServiceHelper.query("wtp_attendperson", "number", new QFilter[]{new QFilter("id", QCP.equals, uid)}, null);
        String number = Convert.toStr(queryNumber.get(0).get("number"));
        //2、通过工号获取HR人员信息(hrpi_person)
        DynamicObjectCollection queryPerson = QueryServiceHelper.query("hrpi_person", "id", new QFilter[]{new QFilter("number", QCP.equals, number)}, null);
        Long id = Convert.toLong(queryPerson.get(0).get("id"));
        //3、通过HR人员信息id 获取  任职经历基础页面-过滤条件 hr人员信息id、生效中、主任职、当前版本
        DynamicObjectCollection query = QueryServiceHelper.query("hrpi_empposorgrel", "nckd_zhiji.name as name", new QFilter[]{
                new QFilter("person.id", QCP.equals, id),
                new QFilter("businessstatus", QCP.equals, "1"),
                new QFilter("isprimary", QCP.equals, "1"),
                new QFilter("iscurrentversion", QCP.equals, "1")}, null);
        name = Convert.toStr(query.get(0).get("name"));
        logger.info("返回的职级值："+name);
        return name;
    }
}
