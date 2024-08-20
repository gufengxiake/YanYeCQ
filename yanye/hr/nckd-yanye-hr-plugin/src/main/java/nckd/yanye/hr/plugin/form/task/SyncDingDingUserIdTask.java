package nckd.yanye.hr.plugin.form.task;

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

import java.util.HashMap;
import java.util.Map;


/**
 * 同步钉钉人员id信息
 * 调度计划编码：SyncDingDingUserIdTask
 *
 * @author liuxiao
 * @since 2024-08-19
 */
public class SyncDingDingUserIdTask extends AbstractTask {
    private static final Log log = LogFactory.getLog(SyncDingDingUserIdTask.class);

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        addDingDingUserId();
    }

    public static void addDingDingUserId() {
        // 获取所有员工手机号和钉钉userid的映射
        HashMap<String, String> uerIdMap = ClockInApiUtil.getDingDingUserList();

        // 获取用户钉钉OpenID为空的员工
        DynamicObject[] users = BusinessDataServiceHelper.load(
                "bos_user",
                "id,number,nckd_dingdingid,phone",
                new QFilter[]{new QFilter("nckd_dingdingid", QCP.equals, "").or(new QFilter("nckd_dingdingid", QCP.equals, null))}

        );
        // 新增失败的员工map
        HashMap<String, String> failedUserMap = new HashMap<>();

        int length = 0;

        // 赋值钉钉id
        for (DynamicObject user : users) {
            // 手机号
            String phone = user.getString("phone");
            String userId = uerIdMap.get(phone);
            if (userId == null) {
                failedUserMap.put(user.getString("number"), user.getString("name"));
                continue;
            }
            user.set("nckd_dingdingid", userId);
            length++;
        }
        SaveServiceHelper.save(users);

        log.info("[钉钉人员id同步]同步完成，本次新增数量：{}" +
                        ";本次新增失败的员工有：{}",
                length, failedUserMap);
    }
}
