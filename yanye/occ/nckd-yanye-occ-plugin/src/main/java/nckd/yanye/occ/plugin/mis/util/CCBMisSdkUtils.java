package nckd.yanye.occ.plugin.mis.util;

import com.ccb.CCBMisSdk;
import com.fasterxml.jackson.core.type.TypeReference;

public class CCBMisSdkUtils {

    public static KeyUtilsBean getKeyUtilsBean() {
        String json = CCBMisSdk.CCBMisSdk_PPKeyGen();
        return JsonUtil.jsonStringToObject(json, new TypeReference<KeyUtilsBean>() {
        });
    }
}
