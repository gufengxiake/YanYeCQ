package nckd.yanye.scm.plugin.operate;

import java.util.Arrays;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;

/**
 * @author husheng
 * @date 2024-07-18 16:43
 * @description  暂估应付单保存插件
 */
public class ApBusbillExtOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        DynamicObject[] dataEntities = e.getDataEntities();

        Arrays.stream(dataEntities).forEach(i -> {
            String billno = (String) i.get("billno");
            DynamicObject asstact = i.getDynamicObject("asstact");
            DynamicObjectCollection entry = i.getDynamicObjectCollection("entry");
            for (DynamicObject dynamicObject : entry) {
                dynamicObject.set("nckd_billnumber", billno + "_" + dynamicObject.get("seq"));
                dynamicObject.set("nckd_supplier", asstact);
            }
        });
    }
}
