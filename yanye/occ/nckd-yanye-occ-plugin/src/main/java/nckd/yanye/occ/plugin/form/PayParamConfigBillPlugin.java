package nckd.yanye.occ.plugin.form;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.math.Money;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import nckd.yanye.occ.plugin.mis.sdk.AU011SDK;
import nckd.yanye.occ.plugin.mis.util.JsonUtil;
import nckd.yanye.occ.plugin.mis.util.RSA2SignUtils;
import nckd.yanye.occ.plugin.mis.util.RequestService;
import nckd.yanye.occ.plugin.task.UpdateCCBKeyTask;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :全渠道云-支付配置
 * Description      :用于全渠道云支付参数的匹配
 *
 * @author : zhujintao
 * @date : 2024/8/16
 */
public class PayParamConfigBillPlugin extends AbstractBillPlugIn {
    private static final Log logger = LogFactory.getLog(PayParamConfigBillPlugin.class);

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        if ("nckd_paybank".equals(key)) {
            ChangeData[] changeSet = e.getChangeSet();
            ChangeData changeData = changeSet[0];
            Object newValue = changeData.getNewValue();
            //A 农业银行 B 建设银行
            if ("A".equals(newValue)) {
                this.getModel().deleteEntryData("nckd_entryentity");
                //创建农行分录
                createABCEntry();
            }
            if ("B".equals(newValue)) {
                this.getModel().deleteEntryData("nckd_entryentity");
                //创建建行分录
                createCCBEntry();
            }
        }
    }

    /**
     * 创建农行分录
     */
    private void createABCEntry() {
        DynamicObjectCollection nckdEntryentity = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_entryentity");
        DynamicObject dynamicObject1 = nckdEntryentity.addNew();
        dynamicObject1.set("nckd_payparamname", "商户私钥");
        dynamicObject1.set("nckd_payparamnbr", "privateKey");
        DynamicObject dynamicObject2 = nckdEntryentity.addNew();
        dynamicObject2.set("nckd_payparamname", "平台公钥");
        dynamicObject2.set("nckd_payparamnbr", "publicKey");
        DynamicObject dynamicObject3 = nckdEntryentity.addNew();
        dynamicObject3.set("nckd_payparamname", "店铺编号");
        dynamicObject3.set("nckd_payparamnbr", "mch_id");
        DynamicObject dynamicObject4 = nckdEntryentity.addNew();
        dynamicObject4.set("nckd_payparamname", "请求地址");
        dynamicObject4.set("nckd_payparamnbr", "url");
        DynamicObject dynamicObject5 = nckdEntryentity.addNew();
        dynamicObject5.set("nckd_payparamname", "gps");
        dynamicObject5.set("nckd_payparamnbr", "gps");
        DynamicObject dynamicObject6 = nckdEntryentity.addNew();
        dynamicObject6.set("nckd_payparamname", "终端设备号");
        dynamicObject6.set("nckd_payparamnbr", "terminal_id");
        this.getView().updateView("nckd_entryentity");
    }

    /**
     * 创建建行分录
     */
    private void createCCBEntry() {
        DynamicObjectCollection nckdEntryentity = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_entryentity");
        DynamicObject dynamicObject1 = nckdEntryentity.addNew();
        dynamicObject1.set("nckd_payparamname", "交易密钥");
        dynamicObject1.set("nckd_payparamnbr", "key");
        this.getView().setEnable(false, 0, "nckd_payparamvalue");
        DynamicObject dynamicObject2 = nckdEntryentity.addNew();
        dynamicObject2.set("nckd_payparamname", "商户号");
        dynamicObject2.set("nckd_payparamnbr", "merchantCode");
        DynamicObject dynamicObject3 = nckdEntryentity.addNew();
        dynamicObject3.set("nckd_payparamname", "终端号");
        dynamicObject3.set("nckd_payparamnbr", "terminalId");
        DynamicObject dynamicObject4 = nckdEntryentity.addNew();
        dynamicObject4.set("nckd_payparamname", "请求地址");
        dynamicObject4.set("nckd_payparamnbr", "url");
        DynamicObject dynamicObject5 = nckdEntryentity.addNew();
        dynamicObject5.set("nckd_payparamname", "gps");
        dynamicObject5.set("nckd_payparamnbr", "gps");
        DynamicObject dynamicObject6 = nckdEntryentity.addNew();
        dynamicObject6.set("nckd_payparamname", "版本号");
        dynamicObject6.set("nckd_payparamnbr", "apiVer");
        DynamicObject dynamicObject7 = nckdEntryentity.addNew();
        dynamicObject7.set("nckd_payparamname", "地址(可为空)");
        dynamicObject7.set("nckd_payparamnbr", "address");
        DynamicObject dynamicObject8 = nckdEntryentity.addNew();
        dynamicObject8.set("nckd_payparamname", "授权码(仅第一次获取密钥有效)");
        dynamicObject8.set("nckd_payparamnbr", "authCode");
        this.getView().updateView("nckd_entryentity");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        if ("nckd_secretkey".equals(itemKey)) {
            String enable = (String) this.getModel().getValue("enable");
            String status = (String) this.getModel().getValue("status");
            if (!"1".equals(enable) || !"C".equals(status)) {
                this.getView().showErrorNotification("必须单据是已审核且有效时才能触发建行密钥下载功能");
                return;
            }
            DynamicObjectCollection nckdEntryentity = this.getModel().getEntryEntity("nckd_entryentity");
            Map<String, String> map = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
            String authCode = map.get("authCode");
            String merchantCode = map.get("merchantCode");
            String terminalId = map.get("terminalId");
            String url = map.get("url");
            String apiVer = map.get("apiVer");
            //必传条件不能为空
            boolean paramConfigResult = StringUtils.isNotEmpty(authCode) && StringUtils.isNotEmpty(merchantCode) && StringUtils.isNotEmpty(terminalId) && StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiVer);
            if (!paramConfigResult) {
                this.getView().showErrorNotification("请检查支付参数授权码，商户号，终端号，请求地址，版本号是否为空");
                return;
            }
            //调银行获取初始密钥接口
            String key = AU011SDK.au011(authCode, merchantCode, terminalId, url, apiVer);
            if (StringUtils.isNotEmpty(key)) {
                logger.info("PayParamConfigBillPlugin 建行初始密钥 " + key);
                if (StringUtils.isNotEmpty(key)) {
                    DynamicObject dataEntity = this.getModel().getDataEntity();
                    DynamicObjectCollection nckd_entryentity = dataEntity.getDynamicObjectCollection("nckd_entryentity");
                    DynamicObject dy = nckd_entryentity.get(0);
                    dy.set("nckd_payparamvalue", key);
                    SaveServiceHelper.update(dataEntity);
                    this.getView().updateView();
                    //第一次下载后，直接再调更新接口和确认接口进行激活
                    DynamicObject payParamConfig = this.getModel().getDataEntity();
                    UpdateCCBKeyTask.UpdateCCBKey(payParamConfig);
                    logger.info("PayParamConfigBillPlugin 建行密钥下载成功");
                    this.getView().showConfirm("建行密钥下载成功", MessageBoxOptions.OK);
                } else {
                    logger.info("PayParamConfigBillPlugin 建行密钥已下载,无需重复下载");
                    this.getView().showConfirm("建行密钥已下载,无需重复下载", MessageBoxOptions.OK);
                }
            } else {
                //请求失败
                logger.info("PayParamConfigBillPlugin 建行密钥下载失败 ");
                this.getView().showConfirm("建行密钥下载失败 ", MessageBoxOptions.OK);
            }
        }
        //建行更新密钥
        if ("nckd_updateccbkey".equals(itemKey)) {
            UpdateCCBKeyTask updateCCBKeyTask = new UpdateCCBKeyTask();
            RequestContext requestContext = RequestContext.get();
            Map<String, Object> map = new HashMap<>();
            Object number = this.getModel().getValue("number");
            map.put("payParamNumber", number);
            updateCCBKeyTask.execute(requestContext, map);
        }
        //农行调接口退款
        if ("nckd_refund".equals(itemKey)) {
            //弹框 设置支付金额
            FormShowParameter showParameter = new FormShowParameter();
            showParameter.setFormId("nckd_refund");
            showParameter.setCaption("农行退款申请设置订单号和退款金额");
            showParameter.getOpenStyle().setShowType(ShowType.Modal);
            // 设置回调
            showParameter.setCloseCallBack(new CloseCallBack(this, "abc_refund"));
            this.getView().showForm(showParameter);
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        String key = closedCallBackEvent.getActionId();
        Object returnData = closedCallBackEvent.getReturnData();
        if ("abc_refund".equals(key) && ObjectUtil.isNotEmpty(returnData)) {
            DynamicObject dynamicObject = (DynamicObject) returnData;
            DynamicObjectCollection nckdEntryentity = this.getModel().getEntryEntity("nckd_entryentity");
            Map<String, String> nckdEntryentityMap = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
            String privateKey = nckdEntryentityMap.get("privateKey");
            String publicKey = nckdEntryentityMap.get("publicKey");
            String mchId = nckdEntryentityMap.get("mch_id");
            String url = nckdEntryentityMap.get("url");
            String gps = nckdEntryentityMap.get("gps");
            String terminalId = nckdEntryentityMap.get("terminal_id");
            boolean paramConfigResult = StringUtils.isNotEmpty(privateKey) && StringUtils.isNotEmpty(publicKey)
                    && StringUtils.isNotEmpty(mchId) && StringUtils.isNotEmpty(url)
                    && StringUtils.isNotEmpty(gps) && StringUtils.isNotEmpty(terminalId);
            if (!paramConfigResult) {
                logger.info("请检查支付参数商户私钥，平台公钥，店铺编号，请求地址，gps，终端设备号是否为空");
                this.getView().showErrorNotification("请检查支付参数商户私钥，平台公钥，店铺编号，请求地址，gps，终端设备号是否为空");
                throw new KDBizException("请检查支付参数商户私钥，平台公钥，店铺编号，请求地址，gps，终端设备号是否为空");
            }
            DynamicObject nckdPaylogRecord = BusinessDataServiceHelper.newDynamicObject("nckd_paylogrecord");
            //自定义参数 退款必须要
            BigDecimal payAmount = dynamicObject.getBigDecimal("nckd_payamount");
            String orderNo = dynamicObject.getString("nckd_orderno");
            String refundNo = DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(6);
            //自定义参数 退款必须要
            Money money = new Money(payAmount);
            logger.info("PayParamConfigBillPlugin 开始调农行退款申请接口");
            String nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
            SortedMap<Object, Object> parameters = new TreeMap<>();
            parameters.put("mch_id", mchId);
            parameters.put("out_refund_no", refundNo);
            parameters.put("out_trade_no", orderNo);
            parameters.put("refund_fee", money.getCent());
            parameters.put("nonce_str", nonceStr);
            String sign = RSA2SignUtils.createSign(parameters, privateKey);
            parameters.put("sign", sign);
            nckdPaylogRecord.set("nckd_reqmsg", JsonUtil.toJsonString(parameters));
            // 调农行退款申请接口
            cn.hutool.json.JSONObject refund = abcRefund(parameters, url);
            logger.info("PayParamConfigBillPlugin 结束调农行退款申请接口");
            if (!"SUCCESS".equals(refund.getStr("code"))) {
                //请求失败
                logger.info("SetPayAmountMobFormPlugin 农行退款申请接口失败" + refund.getStr("message"));
                this.getView().showErrorNotification("农行退款申请接口失败 " + refund.getStr("message"));
                throw new KDBizException("农行退款申请接口失败 " + refund.getStr("message"));
            }
            this.getView().showSuccessNotification("退款成功");
            String returnSign = refund.getStr("sign");
            refund.remove("sign");
            //构造返回数据
            SortedMap<Object, Object> returnParam = new TreeMap<>();
            refund.forEach(e -> {
                returnParam.put(e.getKey(), e.getValue());
            });
            //验签
            boolean checkSign = RSA2SignUtils.checkSign(returnParam, returnSign, publicKey);
            if (!checkSign) {
                logger.info("SetPayAmountMobFormPlugin 农行退款申请接口签失败");
                this.getView().showErrorNotification("农行退款申请接口签失败");
                throw new KDBizException("农行退款申请接口签失败");
            }
            //生成交易操作记录
            long currUserId = RequestContext.get().getCurrUserId();
            long userDefaultOrgID = UserServiceHelper.getUserDefaultOrgID(currUserId);
            Date date = new Date();
            OperationResult paylogRecordresult = createPayLogRecord(nckdPaylogRecord, currUserId, date, userDefaultOrgID, JsonUtil.toJsonString(returnParam));
        }
    }

    /**
     * 开始调农行退款申请接口
     *
     * @param parameters
     * @param url
     */
    private cn.hutool.json.JSONObject abcRefund(SortedMap<Object, Object> parameters, String url) {
        //发送请求
        String realUrl = url + "/refund";
        String result = new RequestService().sendJsonPost(realUrl, null, JsonUtil.toJsonString(parameters));
        logger.info("调农行申请动态聚合码接口 result：" + result);
        cn.hutool.json.JSONObject json = JSONUtil.parseObj(result);
        return json;
    }

    /**
     * 创建操作日志记录
     *
     * @param nckdPaylogRecord
     * @param currUserId
     * @param date
     * @param userDefaultOrgID
     * @param transResultStr
     * @return
     */
    private static OperationResult createPayLogRecord(DynamicObject nckdPaylogRecord, long currUserId, Date date,
                                                      long userDefaultOrgID, String transResultStr) {
        nckdPaylogRecord.set("creator", currUserId);
        nckdPaylogRecord.set("createtime", date);
        nckdPaylogRecord.set("modifier", currUserId);
        nckdPaylogRecord.set("modifytime", date);
        nckdPaylogRecord.set("billstatus", "C");
        nckdPaylogRecord.set("org", userDefaultOrgID);
        nckdPaylogRecord.set("nckd_respmsg", transResultStr);
        OperationResult paylogRecordresult = OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());
        return paylogRecordresult;
    }
}
