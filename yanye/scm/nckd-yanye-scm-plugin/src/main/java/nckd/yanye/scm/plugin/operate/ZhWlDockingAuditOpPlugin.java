package nckd.yanye.scm.plugin.operate;


import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import nckd.yanye.scm.plugin.operate.pushzhwl.ZhWlDockingPushOpPlugin;

import java.util.List;

/**
 * 供应链-智慧物流对接单审核按钮（推送智慧物流）
 * 表单标识：nckd_zhwl_docking
 * author：xiaoxiaopeng
 * date：2024-10-22
 */
public class ZhWlDockingAuditOpPlugin extends AbstractOperationServicePlugIn {
    private static Log logger = LogFactory.getLog(ZhWlDockingAuditOpPlugin.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_entryentity");
        fieldKeys.add("createtime");
        fieldKeys.add("nckd_customer");
        fieldKeys.add("nckd_comment");
        fieldKeys.add("nckd_qty");
        fieldKeys.add("nckd_pricefield");
        fieldKeys.add("nckd_unit");
        fieldKeys.add("nckd_materiel");
        fieldKeys.add("nckd_customer");
        fieldKeys.add("nckd_srcbillnumber");
        fieldKeys.add("nckd_comment");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            OperationResult pushzhwl = OperationServiceHelper.executeOperate("pushzhwl", "bos_user", new DynamicObject[]{dataEntity}, OperateOption.create());
            if (pushzhwl == null){
                logger.error("推送智慧物流失败",pushzhwl);
            } else if (!pushzhwl.isSuccess()) {
                logger.error("推送智慧物流失败",pushzhwl.getMessage());
            }
        }
    }
}
