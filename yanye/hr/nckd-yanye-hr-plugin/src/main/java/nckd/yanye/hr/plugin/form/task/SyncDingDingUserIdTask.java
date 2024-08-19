package nckd.yanye.hr.plugin.form.task;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDException;
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
                new QFilter[]{new QFilter("nckd_dingdingid", QCP.equals, "")}
        );

        // 赋值钉钉id
        for (DynamicObject user : users) {
            // 手机号
            String phone = user.getString("phone");
            user.set("nckd_dingdingid", uerIdMap.get(phone));
        }
        SaveServiceHelper.save(users);
    }
}
