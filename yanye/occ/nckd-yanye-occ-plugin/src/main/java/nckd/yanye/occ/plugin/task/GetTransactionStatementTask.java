package nckd.yanye.occ.plugin.task;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ccb.CCBMisSdk;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.result.IOperateInfo;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.exception.KDException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.metadata.botp.ConvertRuleReader;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.api.MessageHandler;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import nckd.yanye.occ.plugin.mis.api.MisApiResponseVo;
import nckd.yanye.occ.plugin.mis.sdk.QR002SDK;
import nckd.yanye.occ.plugin.mis.util.JsonUtil;
import nckd.yanye.occ.plugin.mis.util.RSA2SignUtils;
import nckd.yanye.occ.plugin.mis.util.RequestService;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :系统服务云-调度中心-调度执行程序
 * Description      :全渠道聚合支付功能，如果客户从要货订单移动端发起支付，建行不会主动返回交易状态，如果客户在支付二维码nckd_qrcode界面没有手动
 * 点击“支付完成”，或者点击了，但是银行尚未处理完这笔交易，则需要定时任务去轮询银行的接口获取交易
 * 如果客户再聚合支付扫码结束没有手动点击“支付完成”或者点击后但银行没有处理完，这个时候需要定时任务循环获取交易状态
 *
 * @author : zhujintao
 * @date : 2024/9/3
 */
public class GetTransactionStatementTask extends AbstractTask {
    private static final Log logger = LogFactory.getLog(GetTransactionStatementTask.class);

    @Override
    public MessageHandler getMessageHandle() {
        return super.getMessageHandle();
    }

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        //获取传入的支付参数配置编码
        Object orderNo = map.get("orderNo");
        if (ObjectUtil.isNotEmpty(orderNo)) {
            logger.info("GetTransactionStatementTask 根据交易订单号 " + orderNo + " 去查询交易流水记录");
            QFilter paytranrecordFilter = new QFilter("nckd_orderno", QCP.equals, orderNo);
            paytranrecordFilter.and("billstatus", QCP.equals, "C").and("nckd_paystatus", QCP.equals, "D");
            DynamicObject payTranRecord = BusinessDataServiceHelper.loadSingle("nckd_paytranrecord", "id,number,billstatus,org,nckd_orderno,nckd_saleorderno,nckd_payamount,nckd_paystatus,nckd_querycount,nckd_querydate,nckd_saleorg", paytranrecordFilter.toArray());

            if (ObjectUtil.isNotEmpty(payTranRecord)) {
                logger.info("GetTransactionStatementTask 获取到对应交易流水记录");
                QFilter payparamconfigFilter = new QFilter("createorg", QCP.equals, payTranRecord.getDynamicObject("nckd_saleorg").getPkValue());
                payparamconfigFilter.and("nckd_paybank", QCP.equals, "B").and("status", QCP.equals, "C").and("enable", QCP.equals, "1");
                DynamicObject payParamConfig = BusinessDataServiceHelper.loadSingle("nckd_payparamconfig", "id,number,nckd_paybank,nckd_entryentity.nckd_payparamname,nckd_entryentity.nckd_payparamnbr,nckd_entryentity.nckd_payparamvalue", payparamconfigFilter.toArray());
                //获取银行交易状态
                getTransactionStatement(payParamConfig, payTranRecord);
            } else {
                logger.info("GetTransactionStatementTask 没有获取到对应交易流水记录");
            }
        } else {
            logger.info("GetTransactionStatementTask 全量获取符合条件的交易流水记录进行银行接口调用start");
            QFilter qFilter = new QFilter("billstatus", QCP.equals, "C");
            qFilter.and("nckd_paystatus", QCP.equals, "D").and("nckd_querycount", QCP.less_than, 20);
            DynamicObject[] payTranRecordArr = BusinessDataServiceHelper.load("nckd_paytranrecord", "id,number,billstatus,org,nckd_orderno,nckd_saleorderno,nckd_payamount,nckd_paystatus,nckd_querycount,nckd_querydate,nckd_saleorg", qFilter.toArray());
            for (DynamicObject dynamicObject : payTranRecordArr) {
                QFilter payparamconfigFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("nckd_saleorg").getPkValue());
                payparamconfigFilter.and("nckd_paybank", QCP.equals, "B").and("status", QCP.equals, "C").and("enable", QCP.equals, "1");
                DynamicObject payParamConfig = BusinessDataServiceHelper.loadSingle("nckd_payparamconfig", "id,number,nckd_paybank,nckd_entryentity.nckd_payparamname,nckd_entryentity.nckd_payparamnbr,nckd_entryentity.nckd_payparamvalue", payparamconfigFilter.toArray());
                //获取银行交易状态
                getTransactionStatement(payParamConfig, dynamicObject);
            }
            logger.info("GetTransactionStatementTask 全量获取符合条件的交易流水记录进行银行接口调用end");
        }
    }

    /**
     * @param payParamConfig
     * @param payTranRecord
     */
    private void getTransactionStatement(DynamicObject payParamConfig, DynamicObject payTranRecord) {
        //此处判断是建行支付参数还是农行的支付参数，走不同的支付分支
        //A 农业银行 B 建设银行
        String nckdPaybank = payParamConfig.getString("nckd_paybank");
        DynamicObjectCollection nckdEntryentity = payParamConfig.getDynamicObjectCollection("nckd_entryentity");
        Map<String, String> nckdEntryentityMap = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
        DynamicObject nckdPaylogRecord = BusinessDataServiceHelper.newDynamicObject("nckd_paylogrecord");
        //A 农业银行 ====================================================================================================
        String orderNo = payTranRecord.getString("nckd_orderno");
        String billNo = payTranRecord.getString("nckd_saleorderno");
        BigDecimal payAmount = payTranRecord.getBigDecimal("nckd_payamount");
        if ("A".equals(nckdPaybank)) {
            beforeClosedWhenPayBankIsABC(nckdEntryentityMap, nckdPaylogRecord, orderNo, billNo, payAmount);
        }
        //B 建设银行 ====================================================================================================
        if ("B".equals(nckdPaybank)) {
            beforeClosedWhenPayBankIsCCB(nckdEntryentityMap, nckdPaylogRecord, orderNo, billNo, payAmount);
        }
    }

    /**
     * 当支付银行是农行时点击了”支付完成“按钮
     *
     * @param nckdEntryentityMap
     * @param nckdPaylogRecord
     * @param orderNo
     * @param billNo
     * @param payAmount
     */
    private void beforeClosedWhenPayBankIsABC(Map<String, String> nckdEntryentityMap, DynamicObject nckdPaylogRecord, String orderNo, String billNo, BigDecimal payAmount) {
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
            logger.info("GetTransactionStatementTask 请检查支付参数商户私钥，平台公钥，店铺编号，请求地址，gps，终端设备号是否为空");
            throw new KDBizException("请检查支付参数商户私钥，平台公钥，店铺编号，请求地址，gps，终端设备号是否为空");
        }
        //生成交易操作记录
        createPayLogRecord(nckdPaylogRecord);
        logger.info("GetTransactionStatementTask 开始调农行查询订单状态接口");
        String nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
        SortedMap<Object, Object> parameters = new TreeMap<>();
        parameters.put("mch_id", mchId);
        parameters.put("out_trade_no", orderNo);
        parameters.put("nonce_str", nonceStr);
        String sign = RSA2SignUtils.createSign(parameters, privateKey);
        parameters.put("sign", sign);
        nckdPaylogRecord.set("nckd_reqmsg", JsonUtil.toJsonString(parameters));
        JSONObject orderQuery = orderQuery(parameters, url);
        logger.info("GetTransactionStatementTask 结束调农行查询订单状态接口");
        //获取交易流水记录
        QFilter paytranrecordFilter = new QFilter("nckd_orderno", QCP.equals, orderNo).and("nckd_paystatus", QCP.equals, "D");
        DynamicObject paytranrecord = BusinessDataServiceHelper.loadSingle("nckd_paytranrecord", "id,nckd_paystatus,nckd_querycount,nckd_querydate", paytranrecordFilter.toArray());
        if (ObjectUtil.isNotEmpty(paytranrecord)) {
            int nckdQuerycount = paytranrecord.getInt("nckd_querycount");
            paytranrecord.set("nckd_querycount", nckdQuerycount + 1);
        }
        if (!"SUCCESS".equals(orderQuery.getStr("code"))) {
            logger.info("GetTransactionStatementTask 获取农行查询订单状态失败 " + orderQuery.getStr("message"));
            //更新支付流水的查询次数
            SaveServiceHelper.update(paytranrecord);
            logger.info("GetTransactionStatementTask 更新支付流水的查询次数");
            nckdPaylogRecord.set("nckd_respmsg", orderQuery.getStr("message"));
            OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());
        }
        if ("SUCCESS".equals(orderQuery.getStr("code"))) {
            String returnSign = orderQuery.getStr("sign");
            orderQuery.remove("sign");
            //构造返回数据
            SortedMap<Object, Object> returnParam = new TreeMap<>();
            orderQuery.forEach(f -> {
                returnParam.put(f.getKey(), f.getValue());
            });
            //验签
            boolean checkSign = RSA2SignUtils.checkSign(returnParam, returnSign, publicKey);
            if (!checkSign) {
                logger.info("GetTransactionStatementTask 获取农行查询订单状态验签失败");
                throw new KDBizException("获取农行查询订单状态验签失败");
            }
            String respmsg = JsonUtil.toJsonString(orderQuery);
            logger.info("GetTransactionStatementTask 获取农行查询订单状态成功 " + respmsg);
            nckdPaylogRecord.set("nckd_respmsg", respmsg);
            OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());

            logger.info("GetTransactionStatementTask 更新交易流水记录start");

            paytranrecord.set("nckd_paystatus", "A");
            //更新支付流水的支付状态为成功
            SaveServiceHelper.update(paytranrecord);
            logger.info("GetTransactionStatementTask 更新交易流水记录end");
            //自动审核收款单
            auditCasRecBill(billNo, payAmount);
        }
    }

    /**
     * 当支付银行是建行时点击了”支付完成“按钮
     *
     * @param nckdEntryentityMap
     * @param nckdPaylogRecord
     * @param orderNo
     * @param billNo
     * @param payAmount
     */
    private void beforeClosedWhenPayBankIsCCB(Map<String, String> nckdEntryentityMap, DynamicObject nckdPaylogRecord, String orderNo, String billNo, BigDecimal payAmount) {
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
            logger.info("GetTransactionStatementTask 请检查支付参数交易密钥，商户号，终端号，请求地址，版本号，gps是否为空");
            throw new KDBizException("请检查支付参数交易密钥，商户号，终端号，请求地址，版本号，gps是否为空");
        }
        //生成交易操作记录
        createPayLogRecord(nckdPaylogRecord);
        //调银行的QR002 - 聚合主扫结果查询
        logger.info("GetTransactionStatementTask 开始调 聚合主扫结果查询接口");
        MisApiResponseVo misApiResponse = QR002SDK.qr002(orderNo, nckdPaylogRecord, key, merchantCode, terminalId, url, apiVer, address, gps);
        logger.info("GetTransactionStatementTask 结束调 聚合主扫结果查询接口");
        //获取交易流水记录
        QFilter paytranrecordFilter = new QFilter("nckd_orderno", QCP.equals, orderNo).and("nckd_paystatus", QCP.equals, "D");
        DynamicObject paytranrecord = BusinessDataServiceHelper.loadSingle("nckd_paytranrecord", "id,nckd_paystatus,nckd_querycount,nckd_querydate", paytranrecordFilter.toArray());
        //每次查询完毕都需要增加查询次数
        if (ObjectUtil.isNotEmpty(paytranrecord)) {
            int nckdQuerycount = paytranrecord.getInt("nckd_querycount");
            paytranrecord.set("nckd_querycount", nckdQuerycount + 1);
        }
        //调银行 聚合主扫结果查询 失败
        if (!"00".equals(misApiResponse.getRetCode())) {
            //这个表示要查询的那个订单的交易状态成功与否
            logger.info("GetTransactionStatementTask 聚合主扫结果查询失败" + misApiResponse.getRetErrMsg());
            //更新支付流水的查询次数
            SaveServiceHelper.update(paytranrecord);
            logger.info("GetTransactionStatementTask 更新支付流水的查询次数");
            //保存交易操作记录
            nckdPaylogRecord.set("nckd_respmsg", misApiResponse.getRetErrMsg());
            OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());
        }
        //调银行 聚合主扫结果查询 成功
        if ("00".equals(misApiResponse.getRetCode())) {
            //解密数据
            String transResultStr = CCBMisSdk.CCBMisSdk_DataDecrypt(misApiResponse.getData(), key);
            logger.info("GetTransactionStatementTask 聚合主扫结果查询成功" + transResultStr);
            nckdPaylogRecord.set("nckd_respmsg", transResultStr);
            OperationServiceHelper.executeOperate("save", "nckd_paylogrecord", new DynamicObject[]{nckdPaylogRecord}, OperateOption.create());

            JSONObject transResultJson = JSONUtil.parseObj(transResultStr);
            JSONObject transData = transResultJson.getJSONObject("transData");

            logger.info("GetTransactionStatementTask 更新交易流水记录start");
            /**
             * 原交易成功	retCode=="00" 且 transData.statusCode=="00"	提示成功
             * 原交易失败	retCode=="01" 或 (retCode=="00" 且transData.statusCode=="01")	提示失败，建议收银机屏幕打印txnTraceNo字段
             * 未知	其他	继续轮询
             */
            //这个表示要查询的那个订单的交易状态成功与否
            if ("00".equals(transData.getStr("statusCode"))) {
                paytranrecord.set("nckd_paystatus", "A");
                //更新支付流水的支付状态为成功
                SaveServiceHelper.update(paytranrecord);
                logger.info("GetTransactionStatementTask 更新交易流水记录end");
                //自动审核收款单
                auditCasRecBill(billNo, payAmount);
            } else {
                logger.info("GetTransactionStatementTask 聚合主扫结果查询的原交易结果" + misApiResponse.getRetErrMsg());
                //更新支付流水的查询次数
                SaveServiceHelper.update(paytranrecord);
                logger.info("GetTransactionStatementTask 更新支付流水的查询次数");
            }
        }
    }

    /**
     * 开始调农行查询订单状态接口
     *
     * @param parameters
     * @param url
     * @return
     */
    private JSONObject orderQuery(SortedMap<Object, Object> parameters, String url) {
        //发送请求
        String realUrl = url + "/orderquery";
        String result = new RequestService().sendJsonPost(realUrl, null, JsonUtil.toJsonString(parameters));
        logger.info("调农行查询订单状态接口 result：" + result);
        cn.hutool.json.JSONObject json = JSONUtil.parseObj(result);
        return json;
    }

    /**
     * 自动审核收款单
     *
     * @param billNo
     * @param payAmount
     */
    private void auditCasRecBill(String billNo, BigDecimal payAmount) {
        //========================================================
        logger.info("GetTransactionStatementTask 下推生成收款单start");
        ConvertOperationResult convertResult = convertCasRecBill(billNo);
        //获取下推目标单id
        if (!convertResult.isSuccess()) {
            logger.info("GetTransactionStatementTask 下推生成收款单失败" + convertResult.getMessage());
            throw new KDBizException("下推生成收款单失败" + convertResult.getMessage());
        }
        logger.info("GetTransactionStatementTask 下推生成收款单成功");
        Set<Object> targetBillIds = convertResult.getTargetBillIds();
        Object[] targetBillIdArr = targetBillIds.stream().toArray();
        DynamicObject casRecbill = BusinessDataServiceHelper.loadSingle(targetBillIdArr[0], "cas_recbill");
        //获取到收款单后需要将客户真正付款的金额和付款单的应收金额相比较，相同不更新，否则要更新

        DynamicObjectCollection entry = casRecbill.getDynamicObjectCollection("entry");
        BigDecimal eReceivableamt = entry.get(0).getBigDecimal("e_receivableamt");
        if (payAmount.compareTo(eReceivableamt) != 0) {
            OperationResult updateCasRecBillResult = updateCasRecBill(payAmount, casRecbill);
            //=========================================
            if (!updateCasRecBillResult.isSuccess()) {
                logger.info("GetTransactionStatementTask 收款单后台更新应收金额失败" + updateCasRecBillResult.getMessage());
                throw new KDBizException("收款单后台更新应收金额失败" + updateCasRecBillResult.getMessage());
            }
            logger.info("GetTransactionStatementTask 收款单后台更新应收金额成功");
        }
        OperationResult casRecbillSubmitResult = OperationServiceHelper.executeOperate("submit", "cas_recbill", new DynamicObject[]{casRecbill}, OperateOption.create());
        if (!casRecbillSubmitResult.isSuccess()) {
            List<IOperateInfo> allErrorOrValidateInfo = casRecbillSubmitResult.getAllErrorOrValidateInfo();
            for (int j = 0; j < allErrorOrValidateInfo.size(); j++) {
                IOperateInfo iOperateInfo = allErrorOrValidateInfo.get(j);
                String message = iOperateInfo.getMessage();
                logger.info("GetTransactionStatementTask 收款单提交失败" + message);
            }
            logger.info("GetTransactionStatementTask 收款单提交失败" + casRecbillSubmitResult.getMessage());
            throw new KDBizException("收款单提交失败" + casRecbillSubmitResult.getMessage());
        }
        logger.info("GetTransactionStatementTask 收款单提交成功");
        OperationResult casRecbillAuditResult = OperationServiceHelper.executeOperate("audit", "cas_recbill", new DynamicObject[]{casRecbill}, OperateOption.create());
        if (!casRecbillAuditResult.isSuccess()) {
            List<IOperateInfo> allErrorOrValidateInfo = casRecbillAuditResult.getAllErrorOrValidateInfo();
            for (int j = 0; j < allErrorOrValidateInfo.size(); j++) {
                IOperateInfo iOperateInfo = allErrorOrValidateInfo.get(j);
                String message = iOperateInfo.getMessage();
                logger.info("GetTransactionStatementTask 收款单审核失败" + message);
            }
            logger.info("GetTransactionStatementTask 收款单审核失败" + casRecbillAuditResult.getMessage());
            throw new KDBizException("收款单提交失败" + casRecbillAuditResult.getMessage());
        }
        logger.info("GetTransactionStatementTask 收款单审核成功");
    }

    /**
     * 生成交易操作记录
     *
     * @param nckdPaylogRecord
     */
    private static void createPayLogRecord(DynamicObject nckdPaylogRecord) {
        long currUserId = RequestContext.get().getCurrUserId();
        long userDefaultOrgID = UserServiceHelper.getUserDefaultOrgID(currUserId);
        Date date = new Date();
        nckdPaylogRecord.set("creator", currUserId);
        nckdPaylogRecord.set("createtime", date);
        nckdPaylogRecord.set("modifier", currUserId);
        nckdPaylogRecord.set("modifytime", date);
        nckdPaylogRecord.set("billstatus", "C");
        nckdPaylogRecord.set("org", userDefaultOrgID);
    }

    /**
     * 如果输入的支付金额小于待付金额，那么需要更新收款处理
     *
     * @param payAmount
     * @param casRecbill
     * @return
     */
    public static OperationResult updateCasRecBill(BigDecimal payAmount, DynamicObject casRecbill) {
        casRecbill.set("actrecamt", payAmount);//收款金额
        casRecbill.set("localamt", payAmount);//折本位币
        casRecbill.set("unsettleamount", payAmount);//未结算金额
        casRecbill.set("unsettleamountbase", payAmount);//未结算金额(本位币)
        casRecbill.set("unmatchamountpay", payAmount);//未匹配付款金额
        casRecbill.set("unmatchamountrec", payAmount);//未匹配收款金额
        DynamicObjectCollection casRecbillColl = casRecbill.getDynamicObjectCollection("entry");
        DynamicObject casRecbillEntry = casRecbillColl.get(0);
        casRecbillEntry.set("e_receivableamt", payAmount);//应收金额
        casRecbillEntry.set("e_receivablelocamt", payAmount);//应收折本币
        casRecbillEntry.set("e_actamt", payAmount);//实收金额
        casRecbillEntry.set("e_localamt", payAmount);//实收折本币
        casRecbillEntry.set("e_unsettledamt", payAmount);//未结算金额
        casRecbillEntry.set("e_unsettledlocalamt", payAmount);//未结算金额折本位币
        casRecbillEntry.set("e_unlockamt", payAmount);//未锁定金额
        OperationResult casRecbillSaveResult = OperationServiceHelper.executeOperate("save", "cas_recbill", new DynamicObject[]{casRecbill}, OperateOption.create());
        return casRecbillSaveResult;
    }

    /**
     * 下推生成收款记录
     *
     * @param billNo
     * @return
     */
    public static ConvertOperationResult convertCasRecBill(String billNo) {
        //（1）获取需要执行下推的源单数据，并封装数据包
        //获取源单id
        QFilter saleorderFilter = new QFilter("billno", QCP.equals, billNo);
        DynamicObject ocbsocSaleorder = QueryServiceHelper.queryOne("ocbsoc_saleorder", "*", saleorderFilter.toArray());
        Object pkid = ocbsocSaleorder.get("id");
        //构建选中行数据包
        List<ListSelectedRow> selectedRows = new ArrayList<>();
        ListSelectedRow selectedRow = new ListSelectedRow(pkid);
        selectedRows.add(selectedRow);
        //（2）获取单据转换规则
        //获取转换规则id
        ConvertRuleReader read = new ConvertRuleReader();
        List<String> loadRuleIds = read.loadRuleIds("ocbsoc_saleorder", "cas_recbill", false);
        //（3）创建下推参数
        // 创建下推参数
        PushArgs pushArgs = new PushArgs();
        // 源单标识，必填
        pushArgs.setSourceEntityNumber("ocbsoc_saleorder");
        // 目标单据标识，必填
        pushArgs.setTargetEntityNumber("cas_recbill");
        // 生成转换结果报告，必填
        pushArgs.setBuildConvReport(true);
        //不检查目标单新增权限,非必填
        pushArgs.setHasRight(true);
        //传入下推使用的转换规则id，不填则使用默认规则
        //pushArgs.setRuleId(loadRuleIds.get(0));
        //下推默认保存，必填
        pushArgs.setAutoSave(true);
        // 设置源单选中的数据包，必填
        pushArgs.setSelectedRows(selectedRows);
        //（4）执行下推操作，并确认是否执行成功

        // 执行下推操作
        ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
        return result;
    }

    @Override
    public boolean isSupportReSchedule() {
        return super.isSupportReSchedule();
    }
}
