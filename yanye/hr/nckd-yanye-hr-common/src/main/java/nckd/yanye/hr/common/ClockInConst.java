package nckd.yanye.hr.common;


import nckd.base.common.utils.capp.CappConfig;

/**
 * 钉钉云之家平台常量
 *
 * @author liuxiao
 */
public class ClockInConst {

    /**
     * 云之家-团队id
     */
    public static final String YZJ_EID = CappConfig.getConfigValue("yzj_eid");
    /**
     * 云之家-密钥
     */
    public static final String YZJ_SECRET = CappConfig.getConfigValue("yzj_secret");
    /**
     * 钉钉-应用的唯一标识key
     */
    public static final String DD_APPKEY = CappConfig.getConfigValue("dd_appkey");
    /**
     * 钉钉-应用的密钥
     */
    public static final String DD_APPSECRET = CappConfig.getConfigValue("dd_appsecret");
}
