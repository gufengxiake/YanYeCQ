package nckd.yanye.scm.plugin.form;

import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.*;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.form.control.RichTextEditor;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.util.StringUtils;
import nckd.yanye.scm.common.PurapplybillConst;
import nckd.yanye.scm.common.ZcPlatformConst;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;
import nckd.yanye.scm.common.utils.ZcPlatformJsonUtil;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Objects;

/**
 * 采购申请单-表单插件
 * 单据标识：nckd_pm_purapplybill_ext
 *
 * @author liuxiao
 * @since 2024-08-19
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
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        IFormView view = this.getView();
        // 显隐
        String biddingMethod = visibleSetting(view, model);
        // 发布方式-自动赋值
        if ("1".equals(biddingMethod)) {
            model.setValue(PurapplybillConst.NCKD_PUBLISHINGMETHOD, "1");
        }
        if ("2".equals(biddingMethod)) {
            model.setValue(PurapplybillConst.NCKD_PUBLISHINGMETHOD, "2");
        }
    }

    private String visibleSetting(IFormView view, IDataModel model) {
        // 显隐
        // 公告发布日期
        view.setVisible("timing".equals(model.getValue(PurapplybillConst.NCKD_PUBLISHSET)), PurapplybillConst.NCKD_TIMINGTIME);
        // 按钮
        view.setVisible("bidprocurement".equals(model.getValue(PurapplybillConst.NCKD_PROCUREMENTS)) && "1".equals(model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL))
                        || "1".equals(model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL1))
                        || "1".equals(model.getValue(PurapplybillConst.NCKD_BIDONLINE))
                , "nckd_bidonlinebar");

        view.setVisible("pricecomparison".equals(model.getValue(PurapplybillConst.NCKD_PROCUREMENTS)) && "1".equals(model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL))
                        || "1".equals(model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL1))
                        || "1".equals(model.getValue(PurapplybillConst.NCKD_BIDONLINE))
                , "nckd_bidonlinebar1");
        view.setVisible("competitive".equals(model.getValue(PurapplybillConst.NCKD_PROCUREMENTS)) && "1".equals(model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL))
                        || "1".equals(model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL1))
                        || "1".equals(model.getValue(PurapplybillConst.NCKD_BIDONLINE))
                , "nckd_bidonlinebar2");

        /*
         * 询比价
         */
        // 处置方式
        view.setVisible("5".equals(model.getValue(PurapplybillConst.NCKD_PROJECTTYPE)), PurapplybillConst.NCKD_DISPOSALMETHOD);
        // 控制总价
        view.setVisible("1".equals(model.getValue(PurapplybillConst.NCKD_CONTROLPRICE)), PurapplybillConst.NCKD_TOTALPRICE);
        // 供应商
        view.setVisible("1".equals(model.getValue(PurapplybillConst.NCKD_INQUIRYMETHOD)), PurapplybillConst.NCKD_SUPPLIERS);
        // 公开范围
        view.setVisible("0".equals(model.getValue(PurapplybillConst.NCKD_INQUIRYMETHOD)), PurapplybillConst.NCKD_PUBLICSCOPE);
        // 报名审核
        view.setVisible("1".equals(model.getValue(PurapplybillConst.NCKD_INQUIRYMETHOD)), PurapplybillConst.NCKD_REGISTERAUDIT);

        /*
         * 竞争性谈判
         */
        // 处置方式
        view.setVisible("5".equals(model.getValue(PurapplybillConst.NCKD_PROJECTTYPE1)), PurapplybillConst.NCKD_DISPOSALMETHOD1);
        // 供应商
        view.setVisible("2".equals(model.getValue(PurapplybillConst.NCKD_COMPETITIONMODE)), PurapplybillConst.NCKD_SUPPLIERS1);
        // 公开范围
        view.setVisible("1".equals(model.getValue(PurapplybillConst.NCKD_COMPETITIONMODE)), PurapplybillConst.NCKD_PUBLICSCOPE1);

        /*
         * 招标采购
         */
        // 招标方式
        String biddingMethod = (String) model.getValue(PurapplybillConst.NCKD_BIDDINGMETHOD);
        // 公开范围
        view.setVisible("1".equals(biddingMethod), PurapplybillConst.NCKD_PUBLICSCOPE2);
        // 报名审核
        view.setVisible("2".equals(biddingMethod), PurapplybillConst.NCKD_REGISTERAUDIT2);
        // 供应商
        view.setVisible("2".equals(biddingMethod), PurapplybillConst.NCKD_SUPPLIERS3);
        // 允许联合体报名
        view.setVisible("2".equals(biddingMethod), PurapplybillConst.NCKD_ALLOWJOINT);
        return biddingMethod;
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        FormOperate formOperate = (FormOperate) args.getSource();
        String operateKey = formOperate.getOperateKey();
        //保存之前将富文本的数据保存在大文本中
        if ("save".equals(operateKey) || "submit".equals(operateKey) || "submitandnew".equals(operateKey)) {
            RichTextEditor richTextEditor = this.getControl(PurapplybillConst.NCKD_NOTICECONTENT);
            String text = richTextEditor.getText();
            this.getModel().setValue(PurapplybillConst.NCKD_BIGNOTICECONTENT, text);
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        visibleSetting(this.getView(), this.getModel());
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
        // 按钮点击
        Button button = this.getView().getControl("nckd_bidonlinebar");
        Button button1 = this.getView().getControl("nckd_bidonlinebar1");
        Button button2 = this.getView().getControl("nckd_bidonlinebar2");
        button.addClickListener(this);
        button1.addClickListener(this);
        button2.addClickListener(this);
    }

    @Override
    public void click(EventObject evt) {
        Control source = (Control) evt.getSource();
        String key = source.getKey();

        switch (key) {
            // 作废
            case "nckd_bidonlinebar":
            case "nckd_bidonlinebar1":
            case "nckd_bidonlinebar2":
                makeBidFile(this.getModel());
                break;
            default:
                break;
        }
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
            // 作废
            case CANCELORDER:
                evt.setCancel(true);
                FormShowParameter showParameter = new FormShowParameter();
                showParameter.setFormId("nckd_zccancelorder");
                showParameter.setCaption("请输入流标原因");
                showParameter.setCloseCallBack(new CloseCallBack(this, "cancelOrderListener"));
                showParameter.getOpenStyle().setShowType(ShowType.Modal);
                this.getView().showForm(showParameter);
                break;
            default:
                break;
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        if ("cancelOrderListener".equals(closedCallBackEvent.getActionId())) {
            //子页面数据回调
            HashMap<String, Object> cancelMap = (HashMap<String, Object>) closedCallBackEvent.getReturnData();
            if (cancelMap == null || cancelMap.isEmpty()) {
                return;
            }
            cancelOrder(this.getModel(), cancelMap);
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
    }

    private void pushToZc(IDataModel model) {
        if (!Objects.equals(model.getValue(PurapplybillConst.BILLSTATUS), "C")) {
            throw new KDBizException("采购申请单未审核!");
        }

        if (Objects.equals(model.getValue(PurapplybillConst.NCKD_PUSHED), true)) {
            throw new KDBizException("该采购申请单已经推送至招采平台!");
        }
        if (Objects.equals(model.getValue(PurapplybillConst.NCKD_PROCUREMENTS), "annualcontract")) {
            throw new KDBizException("年度合同采购申请单不允许推送至招采平台!");
        }

        if (Objects.equals(model.getValue(PurapplybillConst.NCKD_WHETHERPUSH), false)) {
            throw new KDBizException("该采购申请单未勾选“是否推送招采平台”!");
        }

        DynamicObject org = (DynamicObject) model.getValue("org");
        Long orgId = org.getLong("id");
        ZcPlatformConst zcPlatformConst = new ZcPlatformConst(orgId);
        if (!zcPlatformConst.isExist()) {
            throw new KDBizException("未找到对应该申请组织的招采平台应用信息,请在基础资料【招采平台参数】中维护");
        }

        // 初始化结果json
        JSONObject resultJson = null;

        // 采购单id
        String orderIdPre = null;
        // 获取采购方式
        String procurements = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        if ("pricecomparison".equals(procurements) || "singlebrand".equals(procurements)) {
            JSONObject xbJson = ZcPlatformJsonUtil.getXbJson(zcPlatformConst, model);
            resultJson = ZcPlatformApiUtil.addOrder(zcPlatformConst, xbJson, "XB");
            orderIdPre = "XB-";
        } else if ("competitive".equals(procurements)) {
            JSONObject tpJson = ZcPlatformJsonUtil.getTpJson(zcPlatformConst, model);
            resultJson = ZcPlatformApiUtil.addOrder(zcPlatformConst, tpJson, "TP");
            orderIdPre = "TP-";
        } else if ("singlesupplier".equals(procurements)) {
            JSONObject dyJson = ZcPlatformJsonUtil.getDyJson(zcPlatformConst, model);
            resultJson = ZcPlatformApiUtil.addOrder(zcPlatformConst, dyJson, "ZB");
            orderIdPre = "ZB-";
        } else if ("bidprocurement".equals(procurements)) {
            JSONObject zbJson = ZcPlatformJsonUtil.getZbJson(zcPlatformConst, model);
            resultJson = ZcPlatformApiUtil.addOrder(zcPlatformConst, zbJson, "ZB");
            orderIdPre = "ZB-";
        } else {
            throw new KDBizException("该单据不可推送!");
        }

        if (resultJson.getBooleanValue("success")) {
            this.getModel().setValue(PurapplybillConst.NCKD_PURCHASEID, orderIdPre + resultJson.getJSONObject("data").getString("orderId"));
            this.getModel().setValue(PurapplybillConst.NCKD_PUSHED, true);
            SaveServiceHelper.save(new DynamicObject[]{this.getModel().getDataEntity(true)});
            this.getView().showSuccessNotification("公告发布成功！");
        } else {
            this.getView().showErrorNotification("公告发布失败！" + resultJson.getString("message"));
        }
    }

    private void cancelOrder(IDataModel model, HashMap<String, Object> cancelMap) {
        if (Objects.equals(model.getValue(PurapplybillConst.NCKD_PUSHED), false)) {
            throw new KDBizException("该采购申请单未推送至招采平台!");
        }
        if (Objects.equals(model.getValue(PurapplybillConst.NCKD_CLOSED), true)) {
            throw new KDBizException("该采购申请单已作废!");
        }

        DynamicObject org = (DynamicObject) model.getValue("org");
        Long orgId = org.getLong("id");
        ZcPlatformConst zcPlatformConst = new ZcPlatformConst(orgId);
        if (!zcPlatformConst.isExist()) {
            throw new KDBizException("未找到对应该申请组织的招采平台应用信息,请在基础资料【招采平台参数】中维护");
        }

        DynamicObjectCollection attachments = (DynamicObjectCollection) cancelMap.get("attachments");
        ArrayList<Integer> closeAttachmentIds = ZcPlatformJsonUtil.getAttIdList(zcPlatformConst, attachments);
        cancelMap.put("closeAttachmentIds", closeAttachmentIds);

        JSONObject cancelJsonObject;
        // 招采平台id
        String orderId = (String) model.getValue(PurapplybillConst.NCKD_PURCHASEID);
        orderId = orderId.substring(3);
        // 采购方式
        String procurementType = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        if ("pricecomparison".equals(procurementType) || "singlebrand".equals(procurementType)) {
            cancelJsonObject = ZcPlatformApiUtil.cancelOrder(zcPlatformConst, cancelMap, orderId, "XB");
        } else if ("competitive".equals(procurementType)) {
            cancelJsonObject = ZcPlatformApiUtil.cancelOrder(zcPlatformConst, cancelMap, orderId, "TP");
        } else if ("singlesupplier".equals(procurementType) || "bidprocurement".equals(procurementType)) {
            cancelJsonObject = ZcPlatformApiUtil.cancelOrder(zcPlatformConst, cancelMap, orderId, "ZB");
        } else {
            throw new KDBizException("该单据不可作废!");
        }

        if (cancelJsonObject.getBooleanValue("success")) {
            StringBuilder sb = new StringBuilder();
            this.getModel().setValue(PurapplybillConst.NCKD_CLOSED, true);
            SaveServiceHelper.save(new DynamicObject[]{this.getModel().getDataEntity(true)});
            sb.append("公告作废成功！");
//            // 下游单据关闭
//            // 采购订单关闭
//            DynamicObject orderObj = BusinessDataServiceHelper.loadSingle(
//                    "pm_purorderbill",
//                    new QFilter[]{new QFilter("nckd_upapplybill", QCP.equals, this.getModel().getValue("billno"))}
//            );
//            if (orderObj != null) {
//                OperationResult result = OperationServiceHelper.executeOperate("bizclose", "pm_purorderbill", new DynamicObject[]{orderObj}, OperateOption.create());
//                boolean success = result.isSuccess();
//                sb.append(success ? "\r\n采购订单关闭成功！" : "\r\n采购订单关闭失败！" + result.getMessage());
//            }
//
//            // 采购合同关闭
//            DynamicObject contractObj = BusinessDataServiceHelper.loadSingle(
//                    "conm_purcontract",
//                    new QFilter[]{new QFilter("nckd_upapplybill", QCP.equals, this.getModel().getValue("billno"))}
//            );
//            if (contractObj != null) {
//                OperationResult result = OperationServiceHelper.executeOperate("bizclose", "conm_purcontract", new DynamicObject[]{contractObj}, OperateOption.create());
//                boolean success = result.isSuccess();
//                sb.append(success ? "\r\n采购合同关闭成功！" : "\r\n采购合同关闭失败！" + result.getMessage());
//            }

            this.getView().showSuccessNotification(String.valueOf(sb));
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
        if (Objects.equals(model.getValue(PurapplybillConst.NCKD_PUSHED), false)) {
            throw new KDBizException("该采购申请单未推送至招采平台!");
        }
        DynamicObject org = (DynamicObject) model.getValue("org");
        Long orgId = org.getLong("id");
        ZcPlatformConst zcPlatformConst = new ZcPlatformConst(orgId);
        if (!zcPlatformConst.isExist()) {
            throw new KDBizException("未找到对应该申请组织的招采平台应用信息,请在基础资料【招采平台参数】中维护");
        }

        String procurements = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        String orderId = (String) model.getValue(PurapplybillConst.NCKD_PURCHASEID);
        orderId = orderId.substring(3);
        String url = ZcPlatformApiUtil.getViewNoticeUrl(zcPlatformConst, procurements, orderId);
        // 跳转页面
        getView().openUrl(url);
    }

    /**
     * 制作标书
     */
    private void makeBidFile(IDataModel model) {
        // 采购方式
        String procurements = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        if (StringUtils.isEmpty(procurements)) {
            throw new KDBizException("请选择采购方式!");
        }

        DynamicObject org = (DynamicObject) model.getValue("org");
        Long orgId = org.getLong("id");
        ZcPlatformConst zcPlatformConst = new ZcPlatformConst(orgId);
        if (zcPlatformConst.isExist()) {
            throw new KDBizException("未找到对应该申请组织的招采平台应用信息,请在基础资料【招采平台参数】中维护");
        }

        if ("bidprocurement".equals(procurements)) {
            Integer reviewId = ZcPlatformApiUtil.getBiddingFiles(zcPlatformConst);
            // 赋值线上评审id
            model.setValue(PurapplybillConst.NCKD_REVIEWID, reviewId);
            String reviewMode = (String) model.getValue(PurapplybillConst.NCKD_REVIEWMETHOD);
            String url = ZcPlatformApiUtil.getOnlineReviewUrl(zcPlatformConst, procurements, reviewId, reviewMode);
            // 跳转页面
            getView().openUrl(url);
        } else {
            String reviewMode = (String) model.getValue(PurapplybillConst.NCKD_REVIEWMETHOD);
            Integer reviewId = ZcPlatformApiUtil.getPurchaseReviews(zcPlatformConst, reviewMode);
            // 赋值线上评审id
            model.setValue(PurapplybillConst.NCKD_REVIEWID, reviewId);
            String url = ZcPlatformApiUtil.getOnlineReviewUrl(zcPlatformConst, procurements, reviewId, reviewMode);
            // 跳转页面
            getView().openUrl(url);
        }
    }
}



















