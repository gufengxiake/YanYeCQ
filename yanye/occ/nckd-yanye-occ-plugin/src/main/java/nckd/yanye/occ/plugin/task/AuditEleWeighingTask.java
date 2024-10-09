package nckd.yanye.occ.plugin.task;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Module           :系统服务云-调度中心-调度执行程序
 * Description      :定时审核电子磅单
 * @author : wgq
 * @date : 2024/10/8
 */
public class AuditEleWeighingTask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        Set idList = new HashSet();
        //查找已提交的电子磅单
        QFilter qFilter = new QFilter("billstatus", QCP.equals, "B");
        DynamicObjectCollection collections = QueryServiceHelper.query("nckd_eleweighing","id", qFilter.toArray(), "");
        if(!collections.isEmpty()){
            for (DynamicObject saleData : collections) {
                Object id = saleData.get("id");
                idList.add(id);
            }
        }
        if (!idList.isEmpty()) {
            DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("nckd_eleweighing");
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(idList.toArray(), newDynamicObject.getDynamicObjectType());
            OperateOption auditOption = OperateOption.create();
            auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
            auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
            //批量审核电子磅单
            OperationServiceHelper.executeOperate("audit","nckd_eleweighing",dynamicObjects,auditOption);
        }
    }
}
