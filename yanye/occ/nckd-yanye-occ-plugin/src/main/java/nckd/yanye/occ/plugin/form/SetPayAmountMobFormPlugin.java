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
import kd.bos.form.*;
import kd.bos.form.control.Control;
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
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
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

            BigDecimal payamount = (BigDecimal) value;
            if (payamount.compareTo(sumunrecamount) > 0) {
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
                getPayQrCode(payamount, saleorgid, orderNo, billNo, sumunrecamount, orderdate);
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
            BigDecimal payamount = (BigDecimal) this.getModel().getValue("nckd_payamount");
            getPayQrCode(payamount, saleorgid, orderNo, billNo, sumunrecamount, orderdate);
            //this.getView().showSuccessNotification("confirm call back success");
        }
    }

    private void getPayQrCode(BigDecimal payamount, JSONObject saleorgid, String orderNo, String billNo, BigDecimal sumunrecamount, Date orderdate) {
        Money money = new Money(payamount);
        // 加上10分钟过期限制
        DateTime dateTime = DateUtil.offsetMinute(DateUtil.date(), 10);
        String effectiveTime = DateUtil.format(dateTime, DatePattern.PURE_DATETIME_FORMAT);
        logger.info("SetPayAmountMobFormPlugin 开始调PY002SDK");
        DynamicObject nckdPaylogRecord = BusinessDataServiceHelper.newDynamicObject("nckd_paylogrecord");
        //TODO 通过销售组织+有效性 进行匹配
        QFilter payparamconfigFilter = new QFilter("createorg", QCP.equals, saleorgid.get("id"));
        payparamconfigFilter.and("nckd_paybank", QCP.equals, "B").and("status", QCP.equals, "C").and("enable", QCP.equals, "1");
        DynamicObject payparamconfig = BusinessDataServiceHelper.loadSingle("nckd_payparamconfig", "id,number,nckd_paybank,nckd_entryentity.nckd_payparamname,nckd_entryentity.nckd_payparamnbr,nckd_entryentity.nckd_payparamvalue", payparamconfigFilter.toArray());
        if (ObjectUtil.isEmpty(payparamconfig)) {
            cn.hutool.json.JSONObject name = JSONUtil.parseObj(saleorgid.getString("name"));
            logger.info("SetPayAmountMobFormPlugin" + "没有获取到销售组织" + name.getStr("zh_CN") + "对应的支付参数配置");
            this.getView().showErrorNotification("没有获取到销售组织" + name.getStr("zh_CN") + "对应的支付参数配置");
            return;
        }
        DynamicObjectCollection nckdEntryentity = payparamconfig.getDynamicObjectCollection("nckd_entryentity");
        Map<String, String> nckdEntryentityMap = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
        String key = nckdEntryentityMap.get("key");
        String merchantCode = nckdEntryentityMap.get("merchantCode");
        String terminalId = nckdEntryentityMap.get("terminalId");
        String url = nckdEntryentityMap.get("url");
        String apiVer = nckdEntryentityMap.get("apiVer");
        String address = nckdEntryentityMap.get("address");
        String gps = nckdEntryentityMap.get("gps");
        if (StringUtils.isNotEmpty(key)
                && StringUtils.isNotEmpty(merchantCode)
                && StringUtils.isNotEmpty(terminalId)
                && StringUtils.isNotEmpty(url)
                && StringUtils.isNotEmpty(apiVer)
                && StringUtils.isNotEmpty(gps)) {
            MisApiResponseVo misApiResponse = PY002SDK.py002(money.toString(), orderNo, effectiveTime, nckdPaylogRecord, key, merchantCode, terminalId, url, apiVer, address, gps);
            logger.info("SetPayAmountMobFormPlugin 结束调PY002SDK");
            if ("00".equals(misApiResponse.getRetCode())) {
                //解密数据
                String transResultStr = CCBMisSdk.CCBMisSdk_DataDecrypt(misApiResponse.getData(), key);
                logger.info("SetPayAmountMobFormPlugin" + transResultStr);
                cn.hutool.json.JSONObject transResultJson = JSONUtil.parseObj(transResultStr);
                if (ObjectUtil.isNotEmpty(transResultJson)) {
                    //生成交易操作记录
                    long currUserId = RequestContext.get().getCurrUserId();
                    long userDefaultOrgID = UserServiceHelper.getUserDefaultOrgID(currUserId);
                    Date date = new Date();
                    nckdPaylogRecord.set("creator", currUserId);
                    nckdPaylogRecord.set("createtime", date);
                    nckdPaylogRecord.set("modifier", currUserId);
                    nckdPaylogRecord.set("modifytime", date);
                    nckdPaylogRecord.set("billstatus", "C");
                    nckdPaylogRecord.set("org", userDefaultOrgID);
                    nckdPaylogRecord.set("nckd_respmsg", transResultStr);

                    OperationResult paylogRecordresult = OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());
                    if (paylogRecordresult.isSuccess()) {
                        //生成交易流水记录
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
                        OperationResult paytranRecordresult = OperationServiceHelper.executeOperate("save", "nckd_paytranrecord", new DynamicObject[]{nckdPaytranRecord}, OperateOption.create());
                        if (paytranRecordresult.isSuccess()) {
                            //弹框
                            MobileFormShowParameter showParameter = new MobileFormShowParameter();
                            showParameter.setFormId("nckd_qrcode");
                            showParameter.setCaption("支付二维码");
                            showParameter.setPosition(MobileFormPosition.Middle);
                            showParameter.getOpenStyle().setShowType(ShowType.Floating);
                            Map<String, Object> qrcode = new HashMap<>();
                            qrcode.put("transResultJson", transResultJson);
                            qrcode.put("sumunrecamount", sumunrecamount);
                            qrcode.put("orderNo", orderNo);
                            qrcode.put("billNo", billNo);
                            qrcode.put("saleorgid", saleorgid);
                            qrcode.put("orderdate", orderdate);
                            showParameter.setCustomParams(qrcode);
                            this.getView().showForm(showParameter);
                        }
                    }
                }
            } else {
                //请求失败
                logger.info("SetPayAmountMobFormPlugin 请求失败" + misApiResponse.getRetErrMsg());
            }
        }
    }
}
