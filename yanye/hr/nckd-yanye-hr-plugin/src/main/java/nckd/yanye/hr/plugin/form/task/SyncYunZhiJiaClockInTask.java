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
 * 云之家打卡同步任务
 * 调度计划编码：SyncYunZhiJiaClockInTask
 *
 * @author liuxiao
 * @since 2024-08-19
 */
public class SyncYunZhiJiaClockInTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(SyncYunZhiJiaClockInTask.class);

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        addYunZhiJiaClockIn();
    }

    public static void addYunZhiJiaClockIn() {
        // 所有人员昨天到今天的打卡流水
        JSONArray yunZhiJiaClockInList = ClockInApiUtil.getYunZhiJiaClockInList();
        // 新增原始卡记录集合
        ArrayList<DynamicObject> signCardList = new ArrayList<>();
        // 新增失败的员工
        HashMap<String, String> failedUserMap = new HashMap<>();

        // 时区
        DynamicObject timezone = BusinessDataServiceHelper.loadSingle(
                "inte_timezone",
                "id,name,number",
                new QFilter[]{new QFilter("number", QCP.equals, "Asia/Shanghai")}
        );
        // 打卡设备
        DynamicObject device = BusinessDataServiceHelper.loadSingle(
                "wtpm_punchcardequip",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "默认云之家打卡")}
        );
        // 打卡来源
        DynamicObject source = BusinessDataServiceHelper.loadSingle(
                "wtbd_signsource",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "云之家")}
        );

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

        // 预先加载所有云之家用户信息
        DynamicObject[] allUsers = BusinessDataServiceHelper.load(
                "bos_user",
                "id,number,useropenid",
                new QFilter[]{new QFilter("useropenid", QCP.not_equals, "").or(new QFilter("useropenid", QCP.not_equals, null))}
        );
        HashMap<String, DynamicObject> userMap = new HashMap<>();
        for (DynamicObject user : allUsers) {
            userMap.put(user.getString("useropenid"), user);
        }

        // 遍历打卡流水
        for (Object o : yunZhiJiaClockInList) {
            JSONObject clockInfo = (JSONObject) o;

            // 此条打卡数据的对应用户id
            String userId = clockInfo.getString("openId");
            DynamicObject user = userMap.get(userId);

            if (user == null) {
                log.error("未找到对应的用户信息: " + userId);
                continue;
            }

            // 该员工是否有对应考勤档案
            DynamicObject attFile = attFileMap.get(user.getString("number"));
            if (attFile == null) {
                failedUserMap.put(user.getString("number"), user.getString("name"));
                log.error("未找到该员工工号的考勤档案: " + user.getString("name"));
                continue;
            }

            // 查原始卡记录，该条打卡记录是否存在
            if (signCardSet.contains("YZJ" + clockInfo.getString("id"))) {
                continue;
            }

            // 新增原始卡记录
            DynamicObject signCard = BusinessDataServiceHelper.newDynamicObject("wtpd_signcard");

            // 考勤卡号
            signCard.set("attcard", "YZJ" + clockInfo.getString("id"));

            // 考勤档案
            signCard.set("attfilebo", attFileMap.get(clockInfo.getString("workNum")));

            // 考勤档案版本
            signCard.set("attfile", attFileMap.get(clockInfo.getString("workNum")));

            // 考勤管理组织
            signCard.set("org", attFileMap.get(clockInfo.getString("workNum")).get("org"));

            // 打卡时间
            signCard.set("signpoint", clockInfo.getString("clockInTime"));

            // 打卡地点
            signCard.set("nckd_position", clockInfo.getString("positionName"));

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
        log.info("[云之家打卡记录同步]同步完成，本次新增数量：{}" +
                        ";本次新增失败的员工有：{}",
                length, failedUserMap);
    }
}
