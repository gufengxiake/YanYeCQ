package nckd.yanye.hr.plugin.form.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.hr.common.utils.ClockInApiUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉打卡同步任务
 *
 * @author liuxiao
 */
public class SyncDingDingClockInTask extends AbstractTask {
    private static final Log log = LogFactory.getLog(SyncDingDingClockInTask.class);

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        // 所有人员昨天到今天的打卡流水
        JSONArray yunZhiJiaClockInList = ClockInApiUtil.getDingDingClockInList();
        // 新增原始卡记录集合
        ArrayList<DynamicObject> signCardList = new ArrayList<>();
        // 新增失败的员工
        HashMap<String, String> failedUserMap = new HashMap<>();

        for (Object o : yunZhiJiaClockInList) {
            JSONObject clockInfo = (JSONObject) o;

            // 查原始卡记录，该条打卡记录是否存在
            DynamicObject[] users = BusinessDataServiceHelper.load(
                    "wtpd_signcard",
                    "attcard",
                    new QFilter[]{new QFilter("attcard", QCP.equals, "DD" + clockInfo.getString("id"))}
            );

            if (users.length != 0) {
                continue;
            }

            // 新增原始卡记录
            DynamicObject signCard = BusinessDataServiceHelper.newDynamicObject("wtpd_signcard");

            // 考勤卡号
            signCard.set("attcard", "DD" + clockInfo.getString("id"));

            // 根据钉钉id查询工号
            DynamicObject user = BusinessDataServiceHelper.load(
                    "bos_org",
                    "id,number",
                    new QFilter[]{new QFilter("nckd_dingdingid", QCP.equals, clockInfo.getString("userId"))}
            )[0];


            // 考勤档案
            DynamicObject[] load = BusinessDataServiceHelper.load(
                    "wtp_attfilebase",
                    "id,personnum,org,affiliateadminorg,adminorg",
                    new QFilter[]{new QFilter("personnum", QCP.equals, user.getString("number"))}
            );

            if (load.length == 0) {
                failedUserMap.put(user.getString("number"), user.getString("name"));
                log.error("未找到该员工工号的考勤档案:" + user.getString("name"));
                continue;
            }
            DynamicObject attfile = load[0];
            signCard.set("attfilebo", attfile);

            // 考勤档案版本
            signCard.set("attfile", attfile);

            // 考勤管理组织
            signCard.set("org", attfile.get("org"));

            // 打卡时间
            signCard.set("signpoint", clockInfo.getString("userCheckTime"));

            // 打卡来源
            signCard.set("source", BusinessDataServiceHelper.load(
                    "wtbd_signsource",
                    "id,name",
                    new QFilter[]{new QFilter("name", QCP.equals, "钉钉")}
            )[0]);

            // 打卡设备
            signCard.set("device", BusinessDataServiceHelper.load(
                    "wtpm_punchcardequip",
                    "id,name",
                    new QFilter[]{new QFilter("name", QCP.equals, "默认钉钉打卡")}
            )[0]);

            // 时区
            signCard.set("timezone", BusinessDataServiceHelper.load(
                    "inte_timezone",
                    "id,name,number",
                    new QFilter[]{new QFilter("number", QCP.equals, "Asia/Shanghai")}
            )[0]);


            // 状态-有效
//            signCard.set("status", "1");

            signCardList.add(signCard);
        }
        Object[] save = SaveServiceHelper.save(signCardList.toArray(new DynamicObject[0]));
        int length = save.length;
        log.info("新增钉钉打卡记录完成，本次新增数量：{}" +
                        ";本次新增失败的员工有：{}",
                length, failedUserMap);
    }
}
