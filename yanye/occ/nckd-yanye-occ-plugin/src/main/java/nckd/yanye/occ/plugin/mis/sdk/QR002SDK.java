package nckd.yanye.occ.plugin.mis.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import nckd.yanye.occ.plugin.mis.api.MisApiHttpVo;
import nckd.yanye.occ.plugin.mis.api.MisApiResponseVo;
import nckd.yanye.occ.plugin.mis.api.TransData;
import nckd.yanye.occ.plugin.mis.api.TransRequest;
import nckd.yanye.occ.plugin.mis.enums.TransRequestEnum;
import nckd.yanye.occ.plugin.mis.util.JsonUtil;
import nckd.yanye.occ.plugin.mis.util.RequestService;

/**
 * 3.7.QR002 - 聚合主扫结果查询
 * orderNo	String	Y	原支付交易订单号。
 * 原交易成功	retCode=="00" 且 transData.statusCode=="00"	提示成功
 * 原交易失败	retCode=="01" 或 (retCode=="00" 且transData.statusCode=="01")	提示失败，建议收银机屏幕打印traceNo和txnTraceNo字段
 * 未知	其他	继续轮询，建议轮询20次，3秒一次
 *
 * @author: Tan Manguang
 * @date: 2022/3/24
 */
public class QR002SDK {
    private static final Log logger = LogFactory.getLog(QR002SDK.class);

    public static MisApiResponseVo qr002(String orderNo, DynamicObject nckdPaylogRecord, String key, String merchantCode, String terminalId, String url, String apiVer, String address, String gps) {
        logger.info("交易码：" + TransRequestEnum.QR002);
        //构建请求参数
        MisApiHttpVo misApiHttpVo = new MisApiHttpVo(TransRequestEnum.QR002, merchantCode, terminalId, "", apiVer);
        TransRequest transResult = new TransRequest(address, gps);
        TransData transData = new TransData();
        transData.setOrderNo(orderNo);
        transResult.setTransData(transData);
        nckdPaylogRecord.set("nckd_reqmsg", JsonUtil.toJsonString(transResult));
        logger.info("transResult：" + JsonUtil.toJsonString(transResult));

        //加密参数
        misApiHttpVo.encodeData(JsonUtil.toJsonString(transResult), key);
        logger.info("misApiHttpVo：" + JsonUtil.toJsonString(misApiHttpVo));

        //发送请求
        String result = new RequestService().sendJsonPost(url, null, JsonUtil.toJsonString(misApiHttpVo));
        logger.info("result：" + result);

        //json转对象
        MisApiResponseVo misApiResponse = JsonUtil.jsonStringToObject(result, new TypeReference<MisApiResponseVo>() {
        });
        return misApiResponse;
    }
}

