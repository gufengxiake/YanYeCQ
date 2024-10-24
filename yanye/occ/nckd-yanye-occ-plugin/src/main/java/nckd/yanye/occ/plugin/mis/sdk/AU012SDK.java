package nckd.yanye.occ.plugin.mis.sdk;

import com.ccb.CCBMisSdk;
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
public class AU012SDK {
    private static final Log logger = LogFactory.getLog(AU012SDK.class);

    public static String au012(String key, String merchantCode, String terminalId, String url, String apiVer) {
        logger.info("交易码：" + TransRequestEnum.AU012);
        //构建请求参数
        MisApiHttpVo misApiHttpVo = new MisApiHttpVo(TransRequestEnum.AU012, merchantCode, terminalId, "", apiVer);
        KeyUtilsBean misBankKey = CCBMisSdkUtils.getKeyUtilsBean();
        misApiHttpVo.setPublicKey(misBankKey.getPublicKey());
        misApiHttpVo.sign(misBankKey.getPrivateKey(), key);
        //加密参数
        logger.info("misApiHttpVo：" + JsonUtil.toJsonString(misApiHttpVo));

        //发送请求
        String result = new RequestService().sendJsonPost(url, null, JsonUtil.toJsonString(misApiHttpVo));
        logger.info("result：" + result);

        //json转对象
        MisApiResponseVo misApiResponse = JsonUtil.jsonStringToObject(result, new TypeReference<MisApiResponseVo>() {
        });
        if ("00".equals(misApiResponse.getRetCode())) {
            //解密数据
            String newKey = CCBMisSdk.CCBMisSdk_KeyDecrypt(misApiResponse.getKey(), misBankKey.getPrivateKey());
            //解密数据
            logger.info("newKey " + newKey);
            return newKey;
        } else {
            //请求失败
            logger.info("ErrMsg " + misApiResponse.getRetErrMsg());
            return null;
        }
    }
}
