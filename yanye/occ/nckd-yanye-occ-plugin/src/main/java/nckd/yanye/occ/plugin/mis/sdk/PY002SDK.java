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
 * 3.6.PY002 - 聚合支付主扫
 * amt	String	Y	金额，最长13位(含小数点)。单位：元。
 * orderNo	String	Y	订单号。
 * effectiveTime	String	N	有效期,订单超时时间：银行系统时间>该值时拒绝交易；若送空字符串则不判断超时。格式YYYYMMDDHHMMSS如：20120214143005
 * address	String	N	地理位置中文，例如：广东省广州市天河区
 * gps	String	Y	GPS地理位置信息，格式为（纬度,经度），例如：21.129362,121.566955。
 * 若无法实时动态获取，请联系商户经理提供固定值作为固定参数上送。
 * transData	List	Y	业务交易的输入参数分组，详见后续具体交易的说明
 *
 * @author: Tan Manguang
 * @date: 2022/3/24
 */
public class PY002SDK {
    private static final Log logger = LogFactory.getLog(PY002SDK.class);

    public static MisApiResponseVo py002(String amt, String orderNo, String effectiveTime, DynamicObject nckdPaylogRecord, String key, String merchantCode, String terminalId, String url, String apiVer, String address, String gps) {
        logger.info("交易码：" + TransRequestEnum.PY002);

        //构建请求参数
        MisApiHttpVo misApiHttpVo = new MisApiHttpVo(TransRequestEnum.PY002, merchantCode, terminalId, "", apiVer);
        TransRequest transResult = new TransRequest(address, gps);
        TransData transData = new TransData();
        transData.setAmt(amt);
        transData.setOrderNo(orderNo);
        transData.setEffectiveTime(effectiveTime);
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

