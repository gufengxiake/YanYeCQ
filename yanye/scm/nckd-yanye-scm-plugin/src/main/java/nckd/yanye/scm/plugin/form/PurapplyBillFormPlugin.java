package nckd.yanye.scm.plugin.form;

import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.exception.KDBizException;
import kd.bos.form.ConfirmCallBackListener;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.MessageBoxResult;
import kd.bos.form.control.RichTextEditor;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.PurapplybillConst;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;
import nckd.yanye.scm.common.utils.ZcPlatformJsonUtil;

import java.util.EventObject;
import java.util.Objects;

/**
 * 采购申请单-表单插件
 *
 * @author liuxiao
 */
public class PurapplyBillFormPlugin extends AbstractFormPlugin {
    /**
     * 按钮标识-推送招采平台
     */
    final static String PUSHTOZC = "bar_pushtozc";

    /**
     * 按钮标识-公告查看
     */
    final static String ANNOUNCEMENT = "bar_announcement";

    /**
     * 按钮标识-作废
     */
    final static String CANCELORDER = "bar_cancelorder";


    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        FormOperate formOperate = (FormOperate) args.getSource();
        String operateKey = formOperate.getOperateKey();
        //保存之前将富文本的数据保存在大文本中
        if ("save".equals(operateKey) || "submit".equals(operateKey)) {
            RichTextEditor richTextEditor = this.getControl(PurapplybillConst.NCKD_NOTICECONTENT);
            String text = richTextEditor.getText();
            this.getModel().setValue(PurapplybillConst.NCKD_BIGNOTICECONTENT, text);
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        //富文本数据回显
        String largeText = (String) this.getModel().getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT);
        if (largeText == null || largeText.trim().isEmpty()) {
            return;
        }
        RichTextEditor richTextEditor = this.getControl(PurapplybillConst.NCKD_NOTICECONTENT);
        richTextEditor.setText(largeText);
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 侦听主菜单按钮点击事件
        this.addItemClickListeners("tbmain");
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        IDataModel model = this.getModel();
        switch (evt.getItemKey()) {
            // 推送招采平台
            case PUSHTOZC:
                evt.setCancel(true);
                ConfirmCallBackListener pushToZcActionListener = new ConfirmCallBackListener("pushToZcAction", this);
                this.getView().showConfirm("您确认将此采购单推送至招采平台吗？", MessageBoxOptions.YesNo, pushToZcActionListener);
                break;
            // 公告查看
            case ANNOUNCEMENT:
                viewNotice(model);
                break;
            case "制作标书":
                // todo 询比采购线上评审文件制作
                break;
            // 作废
            case CANCELORDER:
                evt.setCancel(true);
                ConfirmCallBackListener cancelOrderListener = new ConfirmCallBackListener("cancelOrderAction", this);
                this.getView().showConfirm("您确认将此采购单作废吗？", MessageBoxOptions.YesNo, cancelOrderListener);
                break;
            default:
                break;
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        //判断回调参数id
        String callBackId = messageBoxClosedEvent.getCallBackId();

        if ("pushToZcAction".equals(callBackId)) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                pushToZc(this.getModel());
            }
        }

        if ("cancelOrderAction".equals(callBackId)) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                cancelOrder(this.getModel());
            }
        }
    }

    private void pushToZc(IDataModel model) {
        if (!Objects.equals(model.getValue(PurapplybillConst.BILLSTATUS), "C")) {
            throw new KDBizException("采购申请单未审核!");
        }

        if (((boolean) model.getValue(PurapplybillConst.NCKD_PUSHED) == true)) {
            throw new KDBizException("该采购申请单已经推送至招采平台!");
        }

        if (((boolean) model.getValue(PurapplybillConst.NCKD_WHETHERPUSH) == false) ||
                model.getValue(PurapplybillConst.NCKD_PROCUREMENTS) == "annualcontract") {
            throw new KDBizException("该采购申请单未勾选“是否推送招采平台”!");
        }

        // 初始化结果json
        JSONObject resultJson = null;

        // 获取采购方式
        String procurements = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        if ("pricecomparison".equals(procurements) || "singlebrand".equals(procurements)) {
            JSONObject xbJson = ZcPlatformJsonUtil.getXbJson(model);
            resultJson = ZcPlatformApiUtil.addOrder(xbJson, "XB");
        } else if ("competitive".equals(procurements)) {
            JSONObject tpJson = ZcPlatformJsonUtil.getTpJson(model);
            resultJson = ZcPlatformApiUtil.addOrder(tpJson, "TP");
        } else if ("singlesupplier".equals(procurements)) {
            JSONObject dyJson = ZcPlatformJsonUtil.getDyJson(model);
            resultJson = ZcPlatformApiUtil.addOrder(dyJson, "ZB");
        } else if ("bidprocurement".equals(procurements)) {
            JSONObject zbJson = ZcPlatformJsonUtil.getZbJson(model);
            resultJson = ZcPlatformApiUtil.addOrder(zbJson, "ZB");
        } else {
            throw new KDBizException("该单据不可推送!");
        }

        //fixme
        if (resultJson.getBooleanValue("success")) {
            this.getModel().setValue(PurapplybillConst.NCKD_PURCHASEID, resultJson.getJSONObject("data").getString("orderId"));
            this.getModel().setValue(PurapplybillConst.NCKD_NOTICEID, resultJson.getJSONObject("data").getString("noticeId"));
            this.getModel().setValue(PurapplybillConst.NCKD_PUSHED, true);
            SaveServiceHelper.saveOperate(this.getView().getEntityId(), new DynamicObject[]{this.getModel().getDataEntity(true)});
            this.getView().showSuccessNotification("公告发布成功!");
        } else {
            this.getView().showErrorNotification("发布失败!" + resultJson.getString("message"));
        }
    }

    private void cancelOrder(IDataModel model) {
        if (((boolean) model.getValue(PurapplybillConst.NCKD_PUSHED) == false)) {
            throw new KDBizException("该采购申请单未推送至招采平台!");
        }
        JSONObject cancelJsonObject;
        // 招采平台id
        String orderId = (String) model.getValue(PurapplybillConst.NCKD_PURCHASEID);
        // 采购方式
        String procurementType = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        if ("pricecomparison".equals(procurementType) || "singlebrand".equals(procurementType)) {
            cancelJsonObject = ZcPlatformApiUtil.cancelOrder(orderId, "XB");
        } else if ("competitive".equals(procurementType)) {
            cancelJsonObject = ZcPlatformApiUtil.cancelOrder(orderId, "TP");
        } else if ("singlesupplier".equals(procurementType) || "bidprocurement".equals(procurementType)) {
            cancelJsonObject = ZcPlatformApiUtil.cancelOrder(orderId, "ZB");
        } else {
            throw new KDBizException("该单据不可作废!");
        }

        if (cancelJsonObject.getBooleanValue("success")) {
            this.getView().showSuccessNotification("作废成功!");
        } else {
            this.getView().showErrorNotification("作废失败!" + cancelJsonObject.getString("message"));
        }
    }

    /**
     * 公告查看-跳转至相应的公告查看界面
     *
     * @param model
     */
    private void viewNotice(IDataModel model) {
        if (((boolean) model.getValue(PurapplybillConst.NCKD_PUSHED) == false)) {
            throw new KDBizException("该采购申请单未推送至招采平台!");
        }
        String procurements = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        String orderId = (String) model.getValue(PurapplybillConst.NCKD_PURCHASEID);
        String url = ZcPlatformApiUtil.viewNotice(procurements, orderId);
        // 跳转页面
        getView().openUrl(url);

    }



}



















