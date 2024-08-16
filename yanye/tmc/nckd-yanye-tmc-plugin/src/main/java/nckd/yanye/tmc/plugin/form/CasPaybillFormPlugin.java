package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.RefObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.ConfirmCallBackListener;
import kd.bos.form.ConfirmTypes;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.MessageBoxResult;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Module           :财务云-出纳-付款单
 * Description      :2.在对供应商支付环节对【上级要求风险单位、失信单位、自定义黑名单单位】预警提示，预警信息如下：收款单位为问题单位，是否确认付款。
 *
 * @author : zhujintao
 * @date : 2024/8/7
 */
public class CasPaybillFormPlugin extends AbstractBillPlugIn {
    private static String KEY_BEFORESUBMIT = "beforesubmit";
    private static String KEY_BATCHENDORSE = "batchendorse";
    private static String OPPARAM_AFTERCONFIRM = "afterconfirm";
    private static Map<String, String> unittypeMap;

    static {
        unittypeMap = new HashMap<>();
        unittypeMap.put("B", "上级要求风险单位");
        unittypeMap.put("C", "失信单位");
        unittypeMap.put("D", "自定义黑名单单位");
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate formOperate = (FormOperate) args.getSource();
        if (StringUtils.equals(KEY_BEFORESUBMIT, formOperate.getOperateKey())) {
            DynamicObject casPaybill = this.getModel().getDataEntity();
            long payee = casPaybill.getLong("payee");
            if ("bd_supplier".equals(casPaybill.getString("payeetype"))) {
                DynamicObject supplier = BusinessDataServiceHelper.loadSingle(payee, "bd_supplier");
                String unittype = supplier.getString("nckd_unittype");
                //不为正常单位
                if ("A" != unittype) {
                    RefObject<String> afterConfirm = new RefObject<>();
                    // 自定义操作参数中，没有afterconfirm参数：说明是首次执行付款操作，需要提示用户确认
                    if (!formOperate.getOption().tryGetVariableValue(OPPARAM_AFTERCONFIRM, afterConfirm)) {
                        // 显示确认消息
                        ConfirmCallBackListener confirmCallBacks = new ConfirmCallBackListener(KEY_BEFORESUBMIT, this);
                        //收款单位为失信单位，是否继续付款
                        this.getView().showConfirm("收款单位为" + unittypeMap.get(unittype) + "，是否继续付款?", MessageBoxOptions.YesNo, ConfirmTypes.Default, confirmCallBacks);
                        // 在没有确认之前，先取消本次操作
                        args.setCancel(true);
                    }
                }
            }
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        if (StringUtils.equals(KEY_BEFORESUBMIT, messageBoxClosedEvent.getCallBackId())) {
            // 提交确认
            if (messageBoxClosedEvent.getResult() == MessageBoxResult.Yes) {
                // 确认执行提交操作
                // 构建操作自定义参数，标志为确认后再次执行操作，避免重复显示交互提示
                OperateOption operateOption = OperateOption.create();
                operateOption.setVariableValue(OPPARAM_AFTERCONFIRM, "true");

                // 执行提交操作，并传入自定义操作参数
                this.getView().invokeOperation(KEY_BEFORESUBMIT, operateOption);
            }
        }

    }
}
