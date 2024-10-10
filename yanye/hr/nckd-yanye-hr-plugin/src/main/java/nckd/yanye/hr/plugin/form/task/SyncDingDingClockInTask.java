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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
        // 查询时间
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime yesterday = today.minusDays(2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String workDateFrom = yesterday.format(formatter);
        String workDateTo = today.format(formatter);
        JSONArray yunZhiJiaClockInList = ClockInApiUtil.getDingDingClockInList(workDateFrom, workDateTo);
        // 新增原始卡记录集合
        ArrayList<DynamicObject> signCardList = new ArrayList<>();
        // 新增失败的员工
        HashMap<String, String> failedUserMap = new HashMap<>();

        // 打卡设备
        DynamicObject device = BusinessDataServiceHelper.loadSingle(
                "wtpm_punchcardequip",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "默认钉钉打卡")}
        );
        // 打卡来源
        DynamicObject source = BusinessDataServiceHelper.loadSingle(
                "wtbd_signsource",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "钉钉")}
        );
        // 时区
        DynamicObject timezone = BusinessDataServiceHelper.loadSingle(
                "inte_timezone",
                "id,name,number",
                new QFilter[]{new QFilter("number", QCP.equals, "Asia/Shanghai")}
        );

        // 预先加载所有钉钉用户信息
        DynamicObject[] allUsers = BusinessDataServiceHelper.load(
                "bos_user",
                "id,number,nckd_dingdingid",
                new QFilter[]{new QFilter("useropenid", QCP.not_equals, "").or(new QFilter("useropenid", QCP.not_equals, null))}
        );
        HashMap<String, DynamicObject> userMap = new HashMap<>();
        for (DynamicObject user : allUsers) {
            userMap.put(user.getString("nckd_dingdingid"), user);
        }

        // 预先加载所有考勤档案信息
        DynamicObject[] allAttFiles = BusinessDataServiceHelper.load(
                "wtp_attfilebase",
                "id,personnum,org",
                new QFilter[]{
                        // 数据版本状态：生效中
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );
        HashMap<String, DynamicObject> attFileMap = new HashMap<>();
        for (DynamicObject attFile : allAttFiles) {
            attFileMap.put(attFile.getString("personnum"), attFile);
        }

        // 预先加载所有原始卡记录信息的唯一id信息
        DynamicObject[] signcards = BusinessDataServiceHelper.load(
                "wtpd_signcard",
                "attcard,nckd_cardid",
                new QFilter[]{
                        new QFilter("signpoint", QCP.large_equals, new Date().getTime() - 5 * 24 * 60 * 60 * 1000)
                }
        );
        HashSet<String> signCardSet = new HashSet<>();
        for (DynamicObject signcard : signcards) {
            signCardSet.add(signcard.getString("nckd_cardid"));
        }

        // 预加载所有考勤卡号
        DynamicObject[] allCardSchedules = BusinessDataServiceHelper.load(
                "wtp_cardschedule",
                "id,number,attfileid,card,bsed,bsled",
                new QFilter[]{
                        // 开始日期小于今天
                        new QFilter("bsed", QCP.less_than, new Date()),
                        // 结束日期大于今天
                        new QFilter("bsled", QCP.large_than, new Date()),
                        // 数据版本状态：生效中
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );
        Map<Long, String> cardMap = Arrays.stream(allCardSchedules).collect(
                Collectors.toMap(
                        obj -> obj.getLong("attfileid.id"),
                        obj -> obj.getString("card")
                )
        );

        // 预加载考勤人员信息
        DynamicObject[] allAttPersons = BusinessDataServiceHelper.load(
                "wtp_attendperson",
                "id,number,name",
                new QFilter[]{}
        );
        Map<String, DynamicObject> attPersonMap = Arrays.stream(allAttPersons).collect(
                Collectors.toMap(
                        obj -> obj.getString("number"),
                        obj -> obj
                )
        );

        // 打卡数据新增
        for (Object o : yunZhiJiaClockInList) {
            JSONObject clockInfo = (JSONObject) o;
            // 此条打卡数据的对应用户id
            String userId = clockInfo.getString("userId");
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
            if (signCardSet.contains("DD" + clockInfo.getString("id"))) {
                continue;
            }

            // 新增原始卡记录
            DynamicObject signCard = BusinessDataServiceHelper.newDynamicObject("wtpd_signcard");

            // 考勤卡号
            signCard.set("attcard", cardMap.get(attFile.getLong("id")));

            // 唯一id
            signCard.set("nckd_cardid", "DD" + clockInfo.getString("id"));

            // 考勤档案
            signCard.set("attfilebo", attFileMap.get(user.getString("number")));

            // 考勤档案版本
            signCard.set("attfile", attFileMap.get(user.getString("number")));

            // 考勤管理组织
            signCard.set("org", attFileMap.get(user.getString("number")).get("org"));

            // 打卡时间
            Date userCheckTime = clockInfo.getDate("userCheckTime");
            signCard.set("signpoint", userCheckTime);

            // 打卡具体时间-0时区(打卡事件-8小时)
            signCard.set("signpointutc", userCheckTime.getTime() - 8 * 60 * 60 * 1000);

            // 打卡地点
            signCard.set("nckd_position", clockInfo.getString("userAddress"));

            // 打卡来源
            signCard.set("source", source);

            // 打卡设备
            signCard.set("device", device);

            // 时区
            signCard.set("timezone", timezone);

            // 状态-有效
            signCard.set("status", "1");

            // 其他需要的字段
            // 考勤人
            DynamicObject attPerson = attPersonMap.get(user.getString("number"));
            if (attPerson == null) {
                failedUserMap.put(user.getString("number"), user.getString("name"));
                log.error("未找到该员工工号的考勤人员信息: " + user.getString("name"));
                continue;
            }
            signCard.set("attperson", attPerson);

            signCardList.add(signCard);
        }
        Object[] save = SaveServiceHelper.save(signCardList.toArray(new DynamicObject[0]));
        int length = save.length;
        log.info("[钉钉打卡记录同步]同步完成，本次新增数量：{}" +
                        ";本次新增失败的员工有：{}",
                length, failedUserMap);
    }
}
