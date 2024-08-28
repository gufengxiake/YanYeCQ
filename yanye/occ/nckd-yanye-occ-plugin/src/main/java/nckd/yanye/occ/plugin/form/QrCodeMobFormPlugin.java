package nckd.yanye.occ.plugin.form;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ccb.CCBMisSdk;
import kd.bos.bill.AbstractMobBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.QRCode;
import kd.bos.form.events.BeforeClosedEvent;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import nckd.yanye.occ.plugin.mis.api.MisApiResponseVo;
import nckd.yanye.occ.plugin.mis.sdk.QR002SDK;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.EventObject;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module           :全渠道云-B2B订单中心-二维码
 * Description      :生成支付二维码
 *
 * @author : zhujintao
 * @date : 2024/8/20
 */
public class QrCodeMobFormPlugin extends AbstractMobBillPlugIn {
    private static final Log logger = LogFactory.getLog(QrCodeMobFormPlugin.class);

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        Map<String, Object> customParams = formShowParameter.getCustomParams();
        JSONObject transResultJson = JSONUtil.parseObj(customParams.get("transResultJson"));
        QRCode code = this.getView().getControl("nckd_qrcodeap");
        JSONObject transData = transResultJson.getJSONObject("transData");
        code.setUrl(transData.getStr("qrCodeUrl"));
        this.getModel().setValue("nckd_billno", Convert.toStr(customParams.get("billNo")));
        this.getModel().setValue("nckd_orderdate", Convert.toDate(customParams.get("orderdate")));
        com.alibaba.fastjson.JSONObject saleorgid = (com.alibaba.fastjson.JSONObject) customParams.get("saleorgid");
        JSONObject name = JSONUtil.parseObj(saleorgid.getString("name"));
        this.getModel().setValue("nckd_supplier", name.getStr("zh_CN"));
        this.getModel().setValue("nckd_sumunrecamount", Convert.toBigDecimal(customParams.get("sumunrecamount")));
    }

    /**
     * 关闭二维码界面就启动立刻启动查询，获取银行的QR002 - 聚合主扫结果查询
     *
     * @param e
     */
    @Override
    public void beforeClosed(BeforeClosedEvent e) {
        super.beforeClosed(e);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        Map<String, Object> customParams = formShowParameter.getCustomParams();
        //用于查询必不可少的条件
        String orderNo = Convert.toStr(customParams.get("orderNo"));
        com.alibaba.fastjson.JSONObject saleorgid = (com.alibaba.fastjson.JSONObject) customParams.get("saleorgid");
        QFilter payparamconfigFilter = new QFilter("createorg", QCP.equals, saleorgid.get("id"));
        payparamconfigFilter.and("nckd_paybank", QCP.equals, "B").and("status", QCP.equals, "C").and("enable", QCP.equals, "1");
        DynamicObject payparamconfig = BusinessDataServiceHelper.loadSingle("nckd_payparamconfig", "id,number,nckd_paybank,nckd_entryentity.nckd_payparamname,nckd_entryentity.nckd_payparamnbr,nckd_entryentity.nckd_payparamvalue", payparamconfigFilter.toArray());
        if (ObjectUtil.isEmpty(payparamconfig)) {
            JSONObject name = JSONUtil.parseObj(saleorgid.getString("name"));
            logger.info("QrCodeMobFormPlugin" + "没有获取到销售组织" + name.getStr("zh_CN") + "对应的支付参数配置");
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
            for (int i = 0; i < 5; i++) {
                DynamicObject nckdPaylogRecord = BusinessDataServiceHelper.newDynamicObject("nckd_paylogrecord");
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
                //调银行的QR002 - 聚合主扫结果查询
                MisApiResponseVo misApiResponse = QR002SDK.qr002(orderNo, nckdPaylogRecord, key, merchantCode, terminalId, url, apiVer, address, gps);
                //这个表示本次查询的结果成功与否
                if ("00".equals(misApiResponse.getRetCode())) {
                    //解密数据
                    String transResultStr = CCBMisSdk.CCBMisSdk_DataDecrypt(misApiResponse.getData(), "0C4E7AAC9CE14A9FB1E68DF96567D733");
                    logger.info("QrCodeMobFormPlugin " + transResultStr);
                    nckdPaylogRecord.set("nckd_respmsg", transResultStr);
                    OperationResult paylogRecordresult = OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());

                    JSONObject transResultJson = JSONUtil.parseObj(transResultStr);
                    JSONObject transData = transResultJson.getJSONObject("transData");
                    //这个表示要查询的那个订单的交易状态成功与否
                    if ("00".equals(transData.getStr("statusCode"))) {
                        logger.info("QrCodeMobFormPlugin 更新交易流水记录start");
                        /**
                         * 原交易成功	retCode=="00" 且 transData.statusCode=="00"	提示成功
                         * 原交易失败	retCode=="01" 或 (retCode=="00" 且transData.statusCode=="01")	提示失败，建议收银机屏幕打印txnTraceNo字段
                         * 未知	其他	继续轮询
                         */
                        //成功获取交易数据则更新交易流水记录 成功、失败、异常、不支持的交易状态
                        QFilter paytranrecordFilter = new QFilter("nckd_orderno", QCP.equals, orderNo).and("nckd_paystatus", QCP.equals, "D");
                        DynamicObject paytranrecord = BusinessDataServiceHelper.loadSingle("nckd_paytranrecord", "id,nckd_paystatus", paytranrecordFilter.toArray());
                        if (ObjectUtil.isNotEmpty(paytranrecord)) {
                            paytranrecord.set("nckd_paystatus", "A");
                        }
                        SaveServiceHelper.update(paytranrecord);
                        logger.info("QrCodeMobFormPlugin 更新交易流水记录end");
                        logger.info("QrCodeMobFormPlugin ");
                        break;
                    }
                } else {
                    //请求失败
                    logger.info("QrCodeMobFormPlugin " + misApiResponse.getRetErrMsg());
                    nckdPaylogRecord.set("nckd_respmsg", misApiResponse.getRetErrMsg());
                    OperationResult paylogRecordresult = OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());
                }
            }
        }
    }
}
