package nckd.yanye.scm.plugin.task;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import nckd.yanye.scm.plugin.form.NegainventoryOrderListPlugin;

import java.util.Map;

/**
 * Module           :制造云-生产任务管理-生产工单(新)
 * Description      :定时任务通过负库存生成下游单据
 * nckd_negainventoryorder
 *
 * @author : yaosijie
 * @date : 2024/8/27
 */
public class NegainventoryorderTask extends AbstractTask {

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        //查询所有得5G工厂类型得负库存数据
        QFilter qFilter = new QFilter("nckd_datasources", QCP.equals,"2")
                .and("billstatus",QCP.equals,"C");
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("nckd_negainventoryorder", "id,masterid", new QFilter[]{qFilter});
        NegainventoryOrderListPlugin plugin = new NegainventoryOrderListPlugin();
        plugin.handleBussProcessOrder(dynamicObjects,"2");
    }
}