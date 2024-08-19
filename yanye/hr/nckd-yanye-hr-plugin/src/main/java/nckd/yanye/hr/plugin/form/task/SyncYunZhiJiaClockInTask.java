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
        DynamicObject timezone = BusinessDataServiceHelper.load(
                "inte_timezone",
                "id,name,number",
                new QFilter[]{new QFilter("number", QCP.equals, "Asia/Shanghai")}
        )[0];
        // 打卡设备
        DynamicObject device = BusinessDataServiceHelper.load(
                "wtpm_punchcardequip",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "默认云之家打卡")}
        )[0];
        // 打卡来源
        DynamicObject source = BusinessDataServiceHelper.load(
                "wtbd_signsource",
                "id,name",
                new QFilter[]{new QFilter("name", QCP.equals, "云之家")}
        )[0];

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

        // 遍历打卡流水
        for (Object o : yunZhiJiaClockInList) {
            JSONObject obj = (JSONObject) o;

            // 查原始卡记录，该条打卡记录是否存在
            if (signCardSet.contains("YZJ" + obj.getString("id"))) {
                continue;
            }

            // 新增原始卡记录
            DynamicObject signCard = BusinessDataServiceHelper.newDynamicObject("wtpd_signcard");

            // 考勤卡号
            signCard.set("attcard", "YZJ" + obj.getString("id"));

            // 考勤档案
            signCard.set("attfilebo", attFileMap.get(obj.getString("workNum")));

            // 考勤档案版本
            signCard.set("attfile", attFileMap.get(obj.getString("workNum")));

            // 考勤管理组织
            signCard.set("org", attFileMap.get(obj.getString("workNum")).get("org"));

            // 打卡时间
            signCard.set("signpoint", obj.getString("clockInTime"));

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
