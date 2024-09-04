package nckd.yanye.occ.plugin.mis.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import nckd.yanye.occ.plugin.mis.api.MisApiHttpVo;
import nckd.yanye.occ.plugin.mis.api.MisApiResponseVo;
import nckd.yanye.occ.plugin.mis.enums.TransRequestEnum;
import nckd.yanye.occ.plugin.mis.util.CCBMisSdkUtils;
import nckd.yanye.occ.plugin.mis.util.JsonUtil;
import nckd.yanye.occ.plugin.mis.util.KeyUtilsBean;
import nckd.yanye.occ.plugin.mis.util.RequestService;

/**
 * @author: Tan Manguang
 * @date: 2022/4/12
 */
public class AU011SDK {
    private static final Log logger = LogFactory.getLog(AU011SDK.class);

    public static MisApiResponseVo au011(String authCode, String merchantCode, String terminalId, String url, String apiVer) {
        logger.info("交易码：" + TransRequestEnum.AU011);
        //构建请求参数
        MisApiHttpVo misApiHttpVo = new MisApiHttpVo(TransRequestEnum.AU011, merchantCode, terminalId, "", apiVer);

        KeyUtilsBean misBankKey = CCBMisSdkUtils.getKeyUtilsBean();
        misApiHttpVo.setPublicKey(misBankKey.getPublicKey());
        misApiHttpVo.setAuthCode(authCode);

        //加密参数
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
