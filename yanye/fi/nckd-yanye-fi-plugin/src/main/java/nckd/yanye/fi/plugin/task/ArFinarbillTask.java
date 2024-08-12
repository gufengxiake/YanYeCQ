package nckd.yanye.fi.plugin.task;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yaosijie
 * @date 2024-07-16 16:44
 * @description 财务应收单提醒收款定时任务
 */
public class ArFinarbillTask extends AbstractTask {

    private static final String KEY_BILLTYPE =	"ar_finarbill_BT";

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        QFilter qFilter = new QFilter("billtype.number", QCP.equals,KEY_BILLTYPE)
                .and("biztype",QCP.equals,"C");
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("ar_finarbill", "id,billno,planentity.planmaterial,planentity.unplansettleamt", new QFilter[]{qFilter});
        //构造map key：收款计划id，value：财务应收单编号
        Map<Long,String> planentityMap = new HashMap<>();
        List<DynamicObject> list = new ArrayList<>();
        Arrays.stream(dynamicObjects).forEach(t->{
            //获取分录
            List<DynamicObject> objectList = t.getDynamicObjectCollection("planentity").stream()
                    .filter(k->k.getBigDecimal("unplansettleamt").compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());
            list.addAll(objectList);
            for (DynamicObject dynamicObject : objectList){
                planentityMap.put(dynamicObject.getLong("id"),t.getString("billno"));
            }
        });

    }
}
