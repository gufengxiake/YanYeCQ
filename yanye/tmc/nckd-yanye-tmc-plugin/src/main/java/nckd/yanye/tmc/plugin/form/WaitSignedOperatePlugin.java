package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.sdk.plugin.Plugin;
import nckd.yanye.tmc.plugin.task.BankAccountTask;

import static kd.bos.devportal.script.plugin.ScriptType.OperationPlugin;

/**
 * Module           :资金云-电子票据-票据管理-待签收票据处理
 * Description      :获取待签收票据操作插件
 *
 *
 * @author guozhiwei
 * @date  2024/8/27 16:08
 * 标识 nckd_cdm_electronic_s_ext
 *
 *
 */


public class WaitSignedOperatePlugin extends AbstractOperationServicePlugIn {

    private static Log logger = LogFactory.getLog(WaitSignedOperatePlugin.class);

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
//        logger.info("DataEntities:{}", e.getDataEntities());
        e.getOperationKey();
        logger.info("OperationKey:{}", e.getOperationKey());
        e.getSelectedRows();
        logger.info("SelectedRows:{}", e.getSelectedRows());
    }


}
