package nckd.yanye.occ.plugin.form;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.math.Money;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.ccb.CCBMisSdk;
import kd.bos.bill.AbstractMobBillPlugIn;
import kd.bos.bill.MobileFormPosition;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.*;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import nckd.yanye.occ.plugin.mis.api.MisApiResponseVo;
import nckd.yanye.occ.plugin.mis.sdk.PY002SDK;
import nckd.yanye.occ.plugin.mis.util.JsonUtil;
import nckd.yanye.occ.plugin.mis.util.RSA2SignUtils;
import nckd.yanye.occ.plugin.mis.util.RequestService;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :全渠道云-B2B订单中心-设置支付金额
 * Description      :用户点击提交的时候调用银行的获取支付二维码接口
 *
 * @author : zhujintao
 * @date : 2024/8/20
 */
public class SetPayAmountMobFormPlugin extends AbstractMobBillPlugIn {
    private static final Log logger = LogFactory.getLog(SetPayAmountMobFormPlugin.class);

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        Map<String, Object> customParams = formShowParameter.getCustomParams();
        this.getModel().setValue("nckd_payamount", Convert.toBigDecimal(customParams.get("sumunrecamount")));
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"btnsubmit"});
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        String btnkey = ((Control) evt.getSource()).getKey();
        if ("btnsubmit".equals(btnkey)) {
            FormShowParameter formShowParameter = this.getView().getFormShowParameter();
            Map<String, Object> customParams = formShowParameter.getCustomParams();
            BigDecimal sumunrecamount = (BigDecimal) customParams.get("sumunrecamount");
            String orderNo = Convert.toStr(customParams.get("orderNo"));
            String billNo = Convert.toStr(customParams.get("billNo"));
            JSONObject saleorgid = (JSONObject) customParams.get("saleorgid");
            Date orderdate = Convert.toDate(customParams.get("orderdate"));
            Object value = this.getModel().getValue("nckd_payamount");
            if (ObjectUtil.isEmpty(value) || ((BigDecimal) value).compareTo(new BigDecimal(0)) == 0) {
                this.getView().showErrorNotification("支付金额不能为空或不能为0");
                return;
            }

            BigDecimal payAmount = (BigDecimal) value;
            if (payAmount.compareTo(sumunrecamount) > 0) {
                this.getView().showErrorNotification("支付金额不能超过待收金额");
                return;
            }
            //点击确认后，进行检查是否有支付流水存在”不支持的交易状态”的状态，如不存在，则唤起支付，将订单信息、金额传递至银行接口，获取银行二维码；
            QFilter paytranrecordFilter = new QFilter("nckd_saleorderno", QCP.equals, billNo);
            paytranrecordFilter.and("nckd_paystatus", QCP.equals, "D");
            DynamicObject[] payTranRecordArr = BusinessDataServiceHelper.load("nckd_paytranrecord", "id", paytranrecordFilter.toArray());
            if (payTranRecordArr.length > 0) {
                ConfirmCallBackListener confirmCallBacks = new ConfirmCallBackListener("getPayQrCode", this);
                // 设置回调提示
                String confirmTip = "已存在结算中的支付流水，是否需要继续支付?";
                this.getView().showConfirm(confirmTip, MessageBoxOptions.OKCancel, ConfirmTypes.Default, confirmCallBacks);
            } else {
                getPayQrCode(payAmount, saleorgid, orderNo, billNo, sumunrecamount, orderdate);
            }
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        // 回调标识正确，并且点击了确认
        if (StringUtils.equals(messageBoxClosedEvent.getCallBackId(), "getPayQrCode") && messageBoxClosedEvent.getResult() == MessageBoxResult.Yes) {
            FormShowParameter formShowParameter = this.getView().getFormShowParameter();
            Map<String, Object> customParams = formShowParameter.getCustomParams();
            BigDecimal sumunrecamount = (BigDecimal) customParams.get("sumunrecamount");
            String orderNo = Convert.toStr(customParams.get("orderNo"));
            String billNo = Convert.toStr(customParams.get("billNo"));
            JSONObject saleorgid = (JSONObject) customParams.get("saleorgid");
            Date orderdate = Convert.toDate(customParams.get("orderdate"));
            BigDecimal payAmount = (BigDecimal) this.getModel().getValue("nckd_payamount");
            getPayQrCode(payAmount, saleorgid, orderNo, billNo, sumunrecamount, orderdate);
        }
    }

    /**
     * 获取 支付二维码
     *
     * @param payAmount      支付金额
     * @param saleorgid      要货订单-销售组织
     * @param orderNo        生成的订单号
     * @param billNo         要货订单-编码
     * @param sumunrecamount 要货订单-待收金额
     * @param orderdate      要货订单-订单日期
     */
    private void getPayQrCode(BigDecimal payAmount, JSONObject saleorgid, String orderNo, String billNo, BigDecimal sumunrecamount, Date orderdate) {
        //通过销售组织+有效性 进行匹配 获得的数据需要判断是建行还是农行
        QFilter payparamconfigFilter = new QFilter("createorg", QCP.equals, saleorgid.get("id"));
        payparamconfigFilter.and("status", QCP.equals, "C").and("enable", QCP.equals, "1");
        DynamicObject payParamConfig = BusinessDataServiceHelper.loadSingle("nckd_payparamconfig", "id,number,nckd_paybank,nckd_entryentity.nckd_payparamname,nckd_entryentity.nckd_payparamnbr,nckd_entryentity.nckd_payparamvalue", payparamconfigFilter.toArray());
        if (ObjectUtil.isEmpty(payParamConfig)) {
            cn.hutool.json.JSONObject name = JSONUtil.parseObj(saleorgid.getString("name"));
            logger.info("SetPayAmountMobFormPlugin " + "没有获取到销售组织 " + name.getStr("zh_CN") + " 对应的支付参数配置");
            this.getView().showErrorNotification("没有获取到销售组织 " + name.getStr("zh_CN") + " 对应的支付参数配置");
            return;
        }
        //此处判断是建行支付参数还是农行的支付参数，走不同的支付分支
        //A 农业银行 B 建设银行
        String nckdPaybank = payParamConfig.getString("nckd_paybank");
        DynamicObjectCollection nckdEntryentity = payParamConfig.getDynamicObjectCollection("nckd_entryentity");
        Map<String, String> nckdEntryentityMap = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
        Money money = new Money(payAmount);
        DynamicObject nckdPaylogRecord = BusinessDataServiceHelper.newDynamicObject("nckd_paylogrecord");
        //A 农业银行
        if ("A".equals(nckdPaybank)) {
            getABCPayCode(payAmount, saleorgid, orderNo, billNo, sumunrecamount, orderdate, nckdEntryentityMap, money, nckdPaylogRecord, nckdPaybank);
        }
        //B 建设银行
        if ("B".equals(nckdPaybank)) {
            getCCBPayCode(payAmount, saleorgid, orderNo, billNo, sumunrecamount, orderdate, nckdEntryentityMap, money, nckdPaylogRecord, nckdPaybank);
        }
    }

    /**
     * 获取建行支付二维码
     *
     * @param payAmount
     * @param saleorgid
     * @param orderNo
     * @param billNo
     * @param sumunrecamount
     * @param orderdate
     * @param nckdEntryentityMap
     * @param money
     * @param nckdPaylogRecord
     * @param nckdPaybank
     */
    private void getCCBPayCode(BigDecimal payAmount, JSONObject saleorgid, String orderNo, String billNo, BigDecimal sumunrecamount, Date orderdate, Map<String, String> nckdEntryentityMap, Money money, DynamicObject nckdPaylogRecord, String nckdPaybank) {
        String key = nckdEntryentityMap.get("key");
        String merchantCode = nckdEntryentityMap.get("merchantCode");
        String terminalId = nckdEntryentityMap.get("terminalId");
        String url = nckdEntryentityMap.get("url");
        String apiVer = nckdEntryentityMap.get("apiVer");
        String address = nckdEntryentityMap.get("address");
        String gps = nckdEntryentityMap.get("gps");
        boolean paramConfigResult = StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(merchantCode)
                && StringUtils.isNotEmpty(terminalId) && StringUtils.isNotEmpty(url)
                && StringUtils.isNotEmpty(apiVer) && StringUtils.isNotEmpty(gps);
        if (!paramConfigResult) {
            logger.info("请检查支付参数交易密钥，商户号，终端号，请求地址，版本号，gps是否为空");
            this.getView().showErrorNotification("请检查支付参数交易密钥，商户号，终端号，请求地址，版本号，gps是否为空");
            throw new KDBizException("请检查支付参数商户私钥，平台公钥，店铺编号，请求地址，gps，终端设备号是否为空");
        }
        // 二维码加上10分钟过期限制
        DateTime dateTime = DateUtil.offsetMinute(DateUtil.date(), 10);
        String effectiveTime = DateUtil.format(dateTime, DatePattern.PURE_DATETIME_FORMAT);
        logger.info("SetPayAmountMobFormPlugin 开始调PY002SDK");
        MisApiResponseVo misApiResponse = PY002SDK.py002(money.toString(), orderNo, effectiveTime, nckdPaylogRecord, key, merchantCode, terminalId, url, apiVer, address, gps);
        logger.info("SetPayAmountMobFormPlugin 结束调PY002SDK");
        if (!"00".equals(misApiResponse.getRetCode())) {
            //请求失败
            logger.info("SetPayAmountMobFormPlugin 获取建行二维码路径失败" + misApiResponse.getRetErrMsg());
            this.getView().showErrorNotification("获取建行二维码路径失败 " + misApiResponse.getRetErrMsg());
            throw new KDBizException("获取建行二维码路径失败 " + misApiResponse.getRetErrMsg());
        }
        //解密数据
        String transResultStr = CCBMisSdk.CCBMisSdk_DataDecrypt(misApiResponse.getData(), key);
        logger.info("SetPayAmountMobFormPlugin 解密数据" + transResultStr);
        cn.hutool.json.JSONObject transResultJson = JSONUtil.parseObj(transResultStr);
        if (ObjectUtil.isNotEmpty(transResultJson)) {
            //生成交易操作记录
            long currUserId = RequestContext.get().getCurrUserId();
            long userDefaultOrgID = UserServiceHelper.getUserDefaultOrgID(currUserId);
            Date date = new Date();
            OperationResult paylogRecordresult = createPayLogRecord(nckdPaylogRecord, currUserId, date, userDefaultOrgID, transResultStr);
            if (paylogRecordresult.isSuccess()) {
                logger.info("SetPayAmountMobFormPlugin 生成交易操作记录成功");
                //生成交易流水记录
                OperationResult paytranRecordresult = createPayTranRecord(payAmount, orderNo, billNo, currUserId, date, userDefaultOrgID, saleorgid);
                if (paytranRecordresult.isSuccess()) {
                    logger.info("SetPayAmountMobFormPlugin 生成交易流水记录成功");
                    //弹框
                    MobileFormShowParameter showParameter = new MobileFormShowParameter();
                    showParameter.setFormId("nckd_qrcode");
                    showParameter.setCaption("支付二维码");
                    showParameter.setPosition(MobileFormPosition.Middle);
                    showParameter.getOpenStyle().setShowType(ShowType.Floating);
                    Map<String, Object> qrcode = new HashMap<>();
                    qrcode.put("codeUrl", transResultJson.getStr("qrCodeUrl"));
                    qrcode.put("sumunrecamount", sumunrecamount);
                    qrcode.put("orderNo", orderNo);
                    qrcode.put("billNo", billNo);
                    qrcode.put("saleorgid", saleorgid);
                    qrcode.put("orderdate", orderdate);
                    qrcode.put("payamount", payAmount);
                    qrcode.put("nckdPaybank", nckdPaybank);
                    showParameter.setCustomParams(qrcode);
                    // 设置回调
                    showParameter.setCloseCallBack(new CloseCallBack(this, "paydone"));
                    this.getView().showForm(showParameter);
                }
            }
        }
    }

    /**
     * 获取农行支付二维码
     *
     * @param payAmount
     * @param saleorgid
     * @param orderNo
     * @param billNo
     * @param sumunrecamount
     * @param orderdate
     * @param nckdEntryentityMap
     * @param money
     * @param nckdPaylogRecord
     * @param nckdPaybank
     */
    private void getABCPayCode(BigDecimal payAmount, JSONObject saleorgid, String orderNo, String billNo, BigDecimal sumunrecamount, Date orderdate, Map<String, String> nckdEntryentityMap, Money money, DynamicObject nckdPaylogRecord, String nckdPaybank) {
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
        logger.info("SetPayAmountMobFormPlugin 开始调农行申请动态聚合码接口");
        String nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
        SortedMap<Object, Object> parameters = new TreeMap<>();
        parameters.put("mch_id", mchId);
        parameters.put("out_trade_no", orderNo);
        parameters.put("total_fee", money.getCent());
        parameters.put("body", "朱锦涛");//TODO==========================================================================
        parameters.put("nonce_str", nonceStr);
        parameters.put("terminal_id", terminalId);
        String sign = RSA2SignUtils.createSign(parameters, privateKey);
        parameters.put("sign", sign);
        nckdPaylogRecord.set("nckd_reqmsg", JsonUtil.toJsonString(parameters));
        // 调农行申请动态聚合码接口
        cn.hutool.json.JSONObject jspay = jspay(parameters, url);
        logger.info("SetPayAmountMobFormPlugin 结束调农行申请动态聚合码接口");
        if (!"SUCCESS".equals(jspay.getStr("code"))) {
            //请求失败
            logger.info("SetPayAmountMobFormPlugin 获取农行二维码路径失败" + jspay.getStr("message"));
            this.getView().showErrorNotification("获取农行二维码路径失败 " + jspay.getStr("message"));
            throw new KDBizException("获取农行二维码路径失败 " + jspay.getStr("message"));
        }
        String returnSign = jspay.getStr("sign");
        jspay.remove("sign");
        //构造返回数据
        SortedMap<Object, Object> returnParam = new TreeMap<>();
        jspay.forEach(e -> {
            returnParam.put(e.getKey(), e.getValue());
        });
        //验签
        boolean checkSign = RSA2SignUtils.checkSign(returnParam, returnSign, publicKey);
        if (!checkSign) {
            logger.info("SetPayAmountMobFormPlugin 获取农行二维码路径验签失败");
            this.getView().showErrorNotification("获取农行二维码路径验签失败");
            throw new KDBizException("获取农行二维码路径验签失败");
        }
        //生成交易操作记录
        long currUserId = RequestContext.get().getCurrUserId();
        long userDefaultOrgID = UserServiceHelper.getUserDefaultOrgID(currUserId);
        Date date = new Date();
        OperationResult paylogRecordresult = createPayLogRecord(nckdPaylogRecord, currUserId, date, userDefaultOrgID, JsonUtil.toJsonString(returnParam));
        if (paylogRecordresult.isSuccess()) {
            logger.info("SetPayAmountMobFormPlugin 生成交易操作记录成功");
            //生成交易流水记录
            OperationResult paytranRecordresult = createPayTranRecord(payAmount, orderNo, billNo, currUserId, date, userDefaultOrgID, saleorgid);
            if (paytranRecordresult.isSuccess()) {
                logger.info("SetPayAmountMobFormPlugin 生成交易流水记录成功");
                //弹框
                MobileFormShowParameter showParameter = new MobileFormShowParameter();
                showParameter.setFormId("nckd_qrcode");
                showParameter.setCaption("支付二维码");
                showParameter.setPosition(MobileFormPosition.Middle);
                showParameter.getOpenStyle().setShowType(ShowType.Floating);
                Map<String, Object> qrcode = new HashMap<>();
                qrcode.put("codeUrl", jspay.getStr("code_url"));
                qrcode.put("sumunrecamount", sumunrecamount);
                qrcode.put("orderNo", orderNo);
                qrcode.put("billNo", billNo);
                qrcode.put("saleorgid", saleorgid);
                qrcode.put("orderdate", orderdate);
                qrcode.put("payamount", payAmount);
                qrcode.put("nckdPaybank", nckdPaybank);
                showParameter.setCustomParams(qrcode);
                // 设置回调
                showParameter.setCloseCallBack(new CloseCallBack(this, "paydone"));
                this.getView().showForm(showParameter);
            }
        }
    }

    /**
     * 开始调农行申请动态聚合码接口
     *
     * @param parameters
     * @param url
     */
    private cn.hutool.json.JSONObject jspay(SortedMap<Object, Object> parameters, String url) {
        //发送请求
        String realUrl = url + "/jspay";
        String result = new RequestService().sendJsonPost(realUrl, null, JsonUtil.toJsonString(parameters));
        logger.info("调农行申请动态聚合码接口 result：" + result);
        cn.hutool.json.JSONObject json = JSONUtil.parseObj(result);
        return json;
    }

    /**
     * 点击支付完成，设置支付金额的地方也完成
     *
     * @param closedCallBackEvent
     */
    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        String key = closedCallBackEvent.getActionId();
        if (StringUtils.equals("paydone", key)) {
            this.getView().close();
        }
    }

    /**
     * 生成交易流水记录
     *
     * @param payamount
     * @param orderNo
     * @param billNo
     * @param currUserId
     * @param date
     * @param userDefaultOrgID
     * @param saleorgid
     * @return
     */
    private static OperationResult createPayTranRecord(BigDecimal payamount, String orderNo, String billNo,
                                                       long currUserId, Date date, long userDefaultOrgID, JSONObject saleorgid) {
        DynamicObject nckdPaytranRecord = BusinessDataServiceHelper.newDynamicObject("nckd_paytranrecord");
        nckdPaytranRecord.set("creator", currUserId);
        nckdPaytranRecord.set("createtime", date);
        nckdPaytranRecord.set("modifier", currUserId);
        nckdPaytranRecord.set("modifytime", date);
        nckdPaytranRecord.set("billstatus", "C");
        nckdPaytranRecord.set("org", userDefaultOrgID);
        nckdPaytranRecord.set("nckd_orderno", orderNo);
        nckdPaytranRecord.set("nckd_saleorderno", billNo);
        nckdPaytranRecord.set("nckd_payamount", payamount);
        nckdPaytranRecord.set("nckd_paystatus", "D");
        nckdPaytranRecord.set("nckd_querycount", 0);
        //nckdPaytranRecord.set("nckd_querydate", DateUtil.offsetMinute(date, 1));
        nckdPaytranRecord.set("nckd_saleorg", saleorgid.get("id"));
        OperationResult paytranRecordresult = OperationServiceHelper.executeOperate("save", "nckd_paytranrecord", new DynamicObject[]{nckdPaytranRecord}, OperateOption.create());
        return paytranRecordresult;
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
