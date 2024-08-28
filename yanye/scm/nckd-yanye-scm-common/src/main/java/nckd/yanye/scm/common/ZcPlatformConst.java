package nckd.yanye.scm.common;

import nckd.base.common.utils.capp.CappConfig;

public class ZcPlatformConst {
    /**
     * 招采平台接口授权类型，固定值: client_credentials
     */
    public static final String GRANT_TYPE = "client_credentials";

    /**
     * 招采平台-应用id
     */
    public static final String ZC_CLIENT_ID = CappConfig.getConfigValue("zc_client_id");

    /**
     * 招采平台-应用密钥
     */
    public static final String ZC_CLIENT_SECRET = CappConfig.getConfigValue("zc_client_secret");

    /**
     * 招采平台-测试环境url
     */
    public static final String ZC_URL = CappConfig.getConfigValue("zc_url");

    /**
     * 招采平台-测试环境访问指定页面url
     */
    public static final String ZC_PASSPORTURL = CappConfig.getConfigValue("zc_passporturl");

    /**
     * 招采平台-回调接口-消息加密密钥
     */
    public static final String ZC_MSG_SECRET = CappConfig.getConfigValue("zc_msg_secret");
}
