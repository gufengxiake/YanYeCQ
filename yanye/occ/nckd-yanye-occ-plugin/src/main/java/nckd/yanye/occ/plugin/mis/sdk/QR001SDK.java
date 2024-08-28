package nckd.yanye.occ.plugin.mis.sdk;

import com.ccb.CCBMisSdk;
import com.fasterxml.jackson.core.type.TypeReference;
import nckd.yanye.occ.plugin.mis.Config;
import nckd.yanye.occ.plugin.mis.api.MisApiHttpVo;
import nckd.yanye.occ.plugin.mis.api.MisApiResponseVo;
import nckd.yanye.occ.plugin.mis.api.TransData;
import nckd.yanye.occ.plugin.mis.api.TransRequest;
import nckd.yanye.occ.plugin.mis.enums.TransRequestEnum;
import nckd.yanye.occ.plugin.mis.util.JsonUtil;
import nckd.yanye.occ.plugin.mis.util.RequestService;

/**
 * @author: Tan Manguang
 * @date: 2022/3/24
 */
public class QR001SDK {
    /*public static void main(String[] args) {
        System.out.println("交易码：" + TransRequestEnum.QR001);
        //构建请求参数
        String key = Config.key;
        MisApiHttpVo misApiHttpVo = new MisApiHttpVo(TransRequestEnum.QR001, Config.merchantCode, Config.terminalId, Config.termSN,"");
        TransRequest transResult = new TransRequest(Config.address, Config.gps);
        TransData transData = new TransData();
        transData.setOrderNo(Config.orderNo);
        transResult.setTransData(transData);
        System.out.println("transResult：" + JsonUtil.toJsonString(transResult));

        //加密参数
        misApiHttpVo.encodeData(JsonUtil.toJsonString(transResult), key);
        System.out.println("misApiHttpVo：" + JsonUtil.toJsonString(misApiHttpVo));

        //发送请求
        String result = new RequestService().sendJsonPost(Config.url, null, JsonUtil.toJsonString(misApiHttpVo));
        System.out.println("result：" + result);

        //json转对象
        MisApiResponseVo misApiResponse = JsonUtil.jsonStringToObject(result, new TypeReference<MisApiResponseVo>() {
        });
        if ("00".equals(misApiResponse.getRetCode())) {
            //解密数据
            String transResultStr = CCBMisSdk.CCBMisSdk_DataDecrypt(misApiResponse.getData(), key);
            System.out.println(transResultStr);
        } else {
            //请求失败
            System.out.println(misApiResponse.getRetErrMsg());
        }

    }*/
}

