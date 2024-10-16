package nckd.yanye.scm.common.utils;

import nckd.base.common.utils.capp.CappConfig;

/**
 * 智慧物流接口配置类
 * author：xiaoxiaopeng
 * date：2024-10-12
 */
public class ZhWlUtil {

    /**
     * 智慧物流获取token用户名
     */
    public static String USERNAME = CappConfig.getConfigValue("zhwl_UserName");

    /**
     * 智慧物流获取token密码
     */
    public static String PASSWORD = CappConfig.getConfigValue("zhwl_Password");

    /**
     * 智慧物流获取token参数
     */
    public static String GRANTTYPE = CappConfig.getConfigValue("zhwl_grant_type");

    /**
     * 智慧物流url地址
     */
    public static String URL = CappConfig.getConfigValue("zhwl_url");
}
