package nckd.yanye.scm.common.utils;

import nckd.base.common.utils.capp.CappConfig;

/**
 * Module           :
 * Description      : 5G工厂接口配置类
 *
 * @author : yaosijie
 * @date : 2024/8/27
 */
public class FactoryUtil {

    /**
     * 5G工厂接口token，固定值: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsb2dpbl90b2tlbiIsImV4
     * cFRpbWUiOjMzMDIyMTM1MTg1NzUsImlzcyI6ImN5YnN0YXIiLCJsb2dpbkRhdGUiOjE3MjQzNzY3MTg
     * 1NzUsInJlcXVlc3RJcCI6IiIsInVzZXJOYW1lIjoi6YeR6J225LqR5pif54CaLemHkeidtui9r-S7ti
     * IsInVzZXJJZCI6MjksInVzZXJDb2RlIjoi6YeR6J226L2v5Lu2IiwidGVuYW50TmFtZSI6IiIsImFwcE
     * lkIjoxMDAwNCwidGVuYW50SWQiOiIiLCJjbGllbnRGbGFnIjoiIiwidXNlclR5cGUiOiJCdXNpbmVzc
     * 1NlcnZpY2UiLCJpYXQiOjE3MjQzNzY3MTg1Nzd9.xiTewvVO8GiA9ba9r6GAr8Odq9vI7iHxt_oqTYgPsqo
     */
    public static final String TOKEN = CappConfig.getConfigValue("5G_token");
    /**
     * 5G工厂接口（外网）
     */
    public static final String INTRANETFACTORYUURL = CappConfig.getConfigValue("5G_externalurl");

    /**
     * 5G工厂接口（内网）
     */
    public static final String EXTERNALNETFACTORYUURL = CappConfig.getConfigValue("5G_intraneturl");
}
