package nckd.yanye.scm.plugin.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.api.MessageHandler;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.SupplierConst;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * 供应商同步招采平台供应商id-定时任务
 * 调度计划编码：SynchronizeSuppliersTask
 *
 * @author liuxiao
 * @since 2024-08-19
 */
public class SynchronizeSuppliersTask extends AbstractTask {
    @Override
    public MessageHandler getMessageHandle() {
        return super.getMessageHandle();
    }

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        //获取招采平台供应商列表
        JSONArray allSuppliers = ZcPlatformApiUtil.getAllZcSupplier();
        //遍历招采平台供应商列表，对应社会统一代码与id
        HashMap<String, String> supplierMap = new HashMap<>();
        for (int i = 0; i < allSuppliers.size(); i++) {
            JSONObject zcSupplier = allSuppliers.getJSONObject(i);
            String socialCreditCode = zcSupplier.getString("socialCreditCode");
            String companyId = zcSupplier.getString("companyId");
            supplierMap.put(socialCreditCode, companyId);
        }

        //根据社会信用代码查询供应商
        DynamicObject[] load = BusinessDataServiceHelper.load(
                SupplierConst.FORMBILLID,
                SupplierConst.ALLPROPERTY,
                new QFilter[]{new QFilter(SupplierConst.SOCIETYCREDITCODE, QCP.in, new HashSet<>(supplierMap.keySet()))}
        );

        //如果供应商存在，赋值招采平台供应商id
        for (DynamicObject supplierObj : load) {
            String socialCreditCode = supplierObj.getString(SupplierConst.SOCIETYCREDITCODE);
            supplierObj.set(SupplierConst.NCKD_PLATFORMSUPID, supplierMap.get(socialCreditCode));
        }

        SaveServiceHelper.save(load);
    }

    @Override
    public boolean isSupportReSchedule() {
        return super.isSupportReSchedule();
    }

}
