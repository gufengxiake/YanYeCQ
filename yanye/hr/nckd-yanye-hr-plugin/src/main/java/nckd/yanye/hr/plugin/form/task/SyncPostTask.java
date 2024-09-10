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
 * @description 岗位信息同步-定时任务
 */
public class SyncPostTask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        this.syncPosition();
//        this.syncPersonnelPosition();
    }

    /**
     * 组织发展云-组织管理-岗位维护-岗位信息维护 数据同步到 基础服务-企业建模-人员管理-岗位
     */
    private void syncPosition() {
        QFilter filter = new QFilter("iscurrentversion", QCP.equals, "1")
                .and("datastatus", QCP.not_in, "-1,0")
                .and("initstatus", QCP.equals, "2")
                .and("createmode", QCP.not_equals, "3");
        DynamicObject[] homsPositions = BusinessDataServiceHelper.load("homs_position", "id,number,name,enable,isleader,adminorg,parent,description", filter.toArray());
        for (DynamicObject position : homsPositions) {
            QFilter qFilter = new QFilter("id", QCP.equals, position.getLong("id"));
            DynamicObject bosPosition = BusinessDataServiceHelper.loadSingle("bos_position", qFilter.toArray());

            DynamicObject bosAdminorg = BusinessDataServiceHelper.loadSingle(position.getDynamicObject("adminorg").getPkValue(), "bos_adminorg");
            DynamicObject object = bosAdminorg.getDynamicObjectCollection("structure").stream().filter(d ->
                    "01".equals(d.getDynamicObject("view").getString("number"))
            ).findFirst().orElse(null);
            if (bosPosition == null) {
                // 新增
                DynamicObject bos_position = BusinessDataServiceHelper.newDynamicObject("bos_position");
                bos_position.set("id", position.get("id"));//主键
                bos_position.set("number", position.get("number"));//岗位编码
                bos_position.set("name", position.get("name"));//岗位名称
                bos_position.set("enable", position.get("enable"));//使用状态
                bos_position.set("ismainposition", position.get("isleader"));//主负责岗
                bos_position.set("dpt", position.get("adminorg"));//部门
                bos_position.set("orgstructure", object);//组织结构
                bos_position.set("superiorposition", position.get("parent"));//上级岗位
                bos_position.set("remarks", position.get("description"));//备注
                bos_position.set("status", 'C');//数据状态
                SaveServiceHelper.save(new DynamicObject[]{bos_position});
            } else {
                // 更新
                bosPosition.set("number", position.get("number"));//岗位编码
                bosPosition.set("name", position.get("name"));//岗位名称
                bosPosition.set("enable", position.get("enable"));//使用状态
                bosPosition.set("ismainposition", position.get("isleader"));//主负责岗
                bosPosition.set("dpt", position.get("adminorg"));//部门
                bosPosition.set("orgstructure", object);//组织结构
                bosPosition.set("superiorposition", position.get("parent"));//上级岗位
                bosPosition.set("remarks", position.get("description"));//备注
                bosPosition.set("status", 'C');//数据状态
                SaveServiceHelper.update(bosPosition);
            }
        }

        // 删除
        DynamicObject[] bosPositions = BusinessDataServiceHelper.load("bos_position", "id,number,name", null);
        for (DynamicObject bosPosition : bosPositions) {
            QFilter qFilter = new QFilter("id", QCP.equals, bosPosition.getLong("id"));
            boolean exists = QueryServiceHelper.exists("homs_position", qFilter.toArray());
            if (!exists) {
                DeleteServiceHelper.delete(bosPosition.getDataEntityType(), new Object[]{bosPosition.getPkValue()});
            }
        }
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
            // 任职类型
            DynamicObject postype = empposorgrel.getDynamicObject("postype");
            // 岗位
            DynamicObject positionvid = empposorgrel.getDynamicObject("positionvid");
            //获取岗位信息维护的是否主负责岗
            DynamicObject position = BusinessDataServiceHelper.loadSingle("homs_position", new QFilter[]{new QFilter("number", QCP.equals, positionvid.getString("number"))});

            // 人员管理查询对应的人员
            QFilter qFilter = new QFilter("number", QCP.equals, empposorgrel.getDynamicObject("person").getString("number"));
            DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("bos_user", new QFilter[]{qFilter});
            if (loadSingle != null) {
                boolean exists = false;
                DynamicObjectCollection entryentity = loadSingle.getDynamicObjectCollection("entryentity");
                // 判断对应部门的记录是否存在，如果存在直接更新数据，不存在则插入一条记录
                for (DynamicObject dynamicObject : entryentity) {
                    if (dynamicObject.getDynamicObject("dpt") != null && adminorgvid.getString("number").equals(dynamicObject.getDynamicObject("dpt").getString("number"))) {
                        dynamicObject.set("position", positionvid.getString("name"));//职位
                        dynamicObject.set("isincharge", "1".equals(position.getString("isleader")) ? 1 : 0);//负责人
                        dynamicObject.set("ispartjob", "1020_S".equals(postype.getString("number")) ? 1 : 0);//兼职
                        dynamicObject.set("post", positionvid);//岗位

                        SaveServiceHelper.update(dynamicObject);
                        exists = true;
                    }
                }

                if (!exists) {
                    DynamicObject dynamicObject = entryentity.addNew();

                    DynamicObject bosAdminorg = BusinessDataServiceHelper.loadSingle("bos_adminorg",new QFilter[]{new QFilter("number",QCP.equals,adminorgvid.getString("number"))});
                    DynamicObject object = bosAdminorg.getDynamicObjectCollection("structure").stream().filter(d ->
                            "01".equals(d.getDynamicObject("view").getString("number"))
                    ).findFirst().orElse(null);

                    dynamicObject.set("dpt", adminorgvid);//部门
                    dynamicObject.set("orgstructure", object);//组织结构
                    dynamicObject.set("position", positionvid.getString("name"));//职位
                    dynamicObject.set("isincharge", "1".equals(position.getString("isleader")) ? 1 : 0);//负责人
                    dynamicObject.set("ispartjob", "1020_S".equals(postype.getString("number")) ? 1 : 0);//兼职
                    dynamicObject.set("post", positionvid);//岗位

                    SaveServiceHelper.save(new DynamicObject[]{loadSingle});
                }
            }
        }
    }
}
