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
import java.util.HashSet;
import java.util.Map;

/**
 * 钉钉打卡同步任务
 * 调度计划编码：SyncDingDingClockInTask
 *
 * @author liuxiao
 * @since 2024-08-19
 */
public class SyncDingDingClockInTask extends AbstractTask {
    private static final Log log = LogFactory.getLog(SyncDingDingClockInTask.class);

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        addDingDingClockIn();
    }

    public static void addDingDingClockIn() {
        // 所有人员昨天到今天的打卡流水
        JSONArray yunZhiJiaClockInList = ClockInApiUtil.getDingDingClockInList();
        // 新增原始卡记录集合
        ArrayList<DynamicObject> signCardList = new ArrayList<>();
        // 新增失败的员工
        HashMap<String, String> failedUserMap = new HashMap<>();

        // 打卡设备
        DynamicObject device = BusinessDataServiceHelper.load(
                "wtpm_punchcardequip",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "默认钉钉打卡")}
        )[0];
        // 打卡来源
        DynamicObject source = BusinessDataServiceHelper.load(
                "wtbd_signsource",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "钉钉")}
        )[0];
        // 时区
        DynamicObject timezone = BusinessDataServiceHelper.load(
                "inte_timezone",
                "id,name,number",
                new QFilter[]{new QFilter("number", QCP.equals, "Asia/Shanghai")}
        )[0];

        // 预先加载所有钉钉用户信息
        DynamicObject[] allUsers = BusinessDataServiceHelper.load(
                "bos_user",
                "id,number,nckd_dingdingid",
                null
        );
        HashMap<String, DynamicObject> userMap = new HashMap<>();
        for (DynamicObject user : allUsers) {
            userMap.put(user.getString("nckd_dingdingid"), user);
        }

        // 预先加载所有考勤档案信息
        DynamicObject[] allAttFiles = BusinessDataServiceHelper.load(
                "wtp_attfilebase",
                "id,personnum,org",
                null
        );
        HashMap<String, DynamicObject> attFileMap = new HashMap<>();
        for (DynamicObject attFile : allAttFiles) {
            attFileMap.put(attFile.getString("personnum"), attFile);
        }

        // 预先加载所有原始卡记录信息
        DynamicObject[] signcards = BusinessDataServiceHelper.load(
                "wtpd_signcard",
                "attcard",
                null
        );
        HashSet<String> signCardSet = new HashSet<>();
        for (DynamicObject signcard : signcards) {
            signCardSet.add(signcard.getString("attcard"));
        }

        // 打卡数据新增
        for (Object o : yunZhiJiaClockInList) {
            JSONObject clockInfo = (JSONObject) o;
            String userId = clockInfo.getString("userId");
            DynamicObject user = userMap.get(userId);

            if (user == null) {
                log.error("未找到对应的用户信息: " + userId);
                continue;
            }

            DynamicObject attFile = attFileMap.get(user.getString("number"));
            if (attFile == null) {
                failedUserMap.put(user.getString("number"), user.getString("name"));
                log.error("未找到该员工工号的考勤档案: " + user.getString("name"));
                continue;
            }

            // 查原始卡记录，该条打卡记录是否存在
            if (signCardSet.contains("DD" + clockInfo.getString("id"))) {
                continue;
            }

            // 新增原始卡记录
            DynamicObject signCard = BusinessDataServiceHelper.newDynamicObject("wtpd_signcard");

            // 考勤卡号
            signCard.set("attcard", "DD" + clockInfo.getString("id"));

            // 考勤档案
            signCard.set("attfilebo", attFileMap.get(user.getString("number")));

            // 考勤档案版本
            signCard.set("attfile", attFileMap.get(user.getString("number")));

            // 考勤管理组织
            signCard.set("org", attFileMap.get(user.getString("number")).get("org"));

            // 打卡时间
            signCard.set("signpoint", clockInfo.getString("userCheckTime"));

            // 打卡来源
            signCard.set("source", source);

            // 打卡设备
            signCard.set("device", device);

            // 时区
            signCard.set("timezone", timezone);

            // 状态-有效
//            signCard.set("status", "1");

            signCardList.add(signCard);
        }
        Object[] save = SaveServiceHelper.save(signCardList.toArray(new DynamicObject[0]));
        int length = save.length;
        log.info("[钉钉打卡记录同步]同步完成，本次新增数量：{}" +
                        ";本次新增失败的员工有：{}",
                length, failedUserMap);
    }
}
