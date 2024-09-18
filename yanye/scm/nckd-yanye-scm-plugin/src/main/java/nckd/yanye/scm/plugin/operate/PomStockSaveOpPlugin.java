package nckd.yanye.scm.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.util.List;

/**
 * 供应链-组件清单
 * 表单标识：nckd_pom_mftstock_ext
 * author：xiaoxiaopeng
 * date：2024-09-18
 */

public class PomStockSaveOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("baseqty");
        fieldKeys.add("qty");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            BigDecimal baseqty = dataEntity.getBigDecimal("baseqty");
            if (baseqty.compareTo(BigDecimal.ZERO) == 0) {
                dataEntity.set("baseqty",dataEntity.getBigDecimal("qty"));
                SaveServiceHelper.update(dataEntity);
            }
        }
    }
}
