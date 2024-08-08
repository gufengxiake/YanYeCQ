package nckd.yanye.hr.plugin.form.task;

import java.util.Map;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.DeleteServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

/**
 * @author husheng
 * @date 2024-08-01 18:06
 * @description 人员档案岗位信息同步-定时任务
 */
public class SyncPersonnelPositionTask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        this.syncPersonnelPosition();
    }

    /**
     * 核心人力云-人员信息-人员档案 同步岗位到 基础服务-企业建模-人员管理-人员
     */
    private void syncPersonnelPosition() {
        // 人员档案任职信息
        QFilter filter = new QFilter("initstatus", QCP.equals, "2")
                .and("iscurrentversion", QCP.equals, "1")
                .and("datastatus", QCP.not_equals, "-1");
        DynamicObject[] empposorgrels = BusinessDataServiceHelper.load("hrpi_empposorgrel", "id,person,adminorgvid,postype,positionvid", filter.toArray());
        for (DynamicObject empposorgrel : empposorgrels) {
            // 部门
            DynamicObject adminorgvid = empposorgrel.getDynamicObject("adminorgvid");
            // 岗位
            DynamicObject positionvid = empposorgrel.getDynamicObject("positionvid");

            // 人员管理查询对应的人员
            QFilter qFilter = new QFilter("number", QCP.equals, empposorgrel.getDynamicObject("person").getString("number"));
            DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("bos_user", new QFilter[]{qFilter});
            if (loadSingle != null) {
                DynamicObjectCollection entryentity = loadSingle.getDynamicObjectCollection("entryentity");
                for (DynamicObject dynamicObject : entryentity) {
                    if (dynamicObject.getDynamicObject("dpt") != null && adminorgvid.getString("number").equals(dynamicObject.getDynamicObject("dpt").getString("number"))) {
                        dynamicObject.set("post", positionvid);//岗位

                        SaveServiceHelper.update(dynamicObject);
                    }
                }
            }
        }
    }
}
