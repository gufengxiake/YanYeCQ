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
 * @description  岗位信息同步-定时任务
 */
public class SyncPostTask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        this.syncPosition();
        this.syncPersonnelPosition();
    }

    /**
     * 组织发展云-组织管理-岗位维护-岗位信息维护 数据同步到 基础服务-企业建模-人员管理-岗位
     */
    private void syncPosition(){
        DynamicObject[] homsPositions = BusinessDataServiceHelper.load("homs_position", "id,number,name,enable,isleader,adminorg,parent,description", null);
        for (DynamicObject position : homsPositions) {
            QFilter qFilter = new QFilter("number", QCP.equals, position.getString("number"));
            DynamicObject bosPosition = BusinessDataServiceHelper.loadSingle("bos_position", qFilter.toArray());
            if (bosPosition == null) {
                // 新增
                DynamicObject bos_position = BusinessDataServiceHelper.newDynamicObject("bos_position");
                bos_position.set("id",position.get("id"));
                bos_position.set("number", position.get("number"));
                bos_position.set("name", position.get("name"));
                bos_position.set("enable", position.get("enable"));
                bos_position.set("ismainposition", position.get("isleader"));
                bos_position.set("dpt", position.get("adminorg"));
                bos_position.set("superiorposition", position.get("parent"));
                bos_position.set("remarks", position.get("description"));
                SaveServiceHelper.save(new DynamicObject[]{bos_position});
            } else {
                // 更新
                bosPosition.set("number", position.get("number"));
                bosPosition.set("name", position.get("name"));
                bosPosition.set("enable", position.get("enable"));
                bosPosition.set("ismainposition", position.get("isleader"));
                bosPosition.set("dpt", position.get("adminorg"));
                bosPosition.set("superiorposition", position.get("parent"));
                bosPosition.set("remarks", position.get("description"));
                SaveServiceHelper.update(bosPosition);
            }
        }

        // 删除
        DynamicObject[] bosPositions = BusinessDataServiceHelper.load("bos_position", "id,number,name", null);
        for (DynamicObject bosPosition : bosPositions) {
//            QFilter qFilter = new QFilter("number", QCP.equals, bosPosition.getString("number"));
            QFilter qFilter = new QFilter("id", QCP.equals, bosPosition.getLong("id"));
            boolean exists = QueryServiceHelper.exists("homs_position", qFilter.toArray());
            if(!exists){
                DeleteServiceHelper.delete(bosPosition.getDataEntityType(),new Object[]{bosPosition.getPkValue()});
            }
        }
    }

    /**
     * 核心人力云-人员信息-人员档案 同步岗位到 基础服务-企业建模-人员管理-人员
     */
    private void syncPersonnelPosition(){
        // 人员档案任职信息
        DynamicObject[] empposorgrels = BusinessDataServiceHelper.load("hrpi_empposorgrel", "id,person,adminorgvid,postype,positionvid", null);
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
            if(loadSingle != null){
                boolean exists = false;
                DynamicObjectCollection entryentity = loadSingle.getDynamicObjectCollection("entryentity");
                // 判断对应部门的记录是否存在，如果存在直接更新数据，不存在则插入一条记录
                for (DynamicObject dynamicObject : entryentity) {
                    if(adminorgvid.getString("number").equals(dynamicObject.getDynamicObject("dpt").getString("number"))){
                        dynamicObject.set("position",positionvid.getString("name"));//职位
                        dynamicObject.set("isincharge","1".equals(position.getString("isleader")) ? 1 : 0);//负责人
                        dynamicObject.set("ispartjob","1020_S".equals(postype.getString("number")) ? 1 : 0);//兼职
                        dynamicObject.set("post",positionvid);//岗位

                        SaveServiceHelper.update(dynamicObject);
                        exists = true;
                    }
                }

                if(!exists){
                    DynamicObject dynamicObject = entryentity.addNew();

                    DynamicObject bosAdminorg = BusinessDataServiceHelper.loadSingle(adminorgvid.getPkValue(), "bos_adminorg");
                    DynamicObjectCollection structure = bosAdminorg.getDynamicObjectCollection("structure");

                    dynamicObject.set("dpt",adminorgvid);//部门
                    dynamicObject.set("orgstructure",structure.get(0).getDynamicObject("vieworg"));//组织结构
                    dynamicObject.set("position",positionvid.getString("name"));//职位
                    dynamicObject.set("isincharge","1".equals(position.getString("isleader")) ? 1 : 0);//负责人
                    dynamicObject.set("ispartjob","1020_S".equals(postype.getString("number")) ? 1 : 0);//兼职
                    dynamicObject.set("post",positionvid);//岗位

                    SaveServiceHelper.save(new DynamicObject[]{loadSingle});
                }
            }
        }
    }
}
