package nckd.yanye.scm.common.utils;
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
    public static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJs" +
            "b2dpbl90b2tlbiIsImV4cFRpbWUiOjMzMDIyMTM1MTg1NzUsImlzcyI6ImN5YnN0YXIiLCJsb2dpbkRhd" +
            "GUiOjE3MjQzNzY3MTg1NzUsInJlcXVlc3RJcCI6IiIsInVzZXJOYW1lIjoi6YeR6J225LqR5pif54CaLem" +
            "Hkeidtui9r-S7tiIsInVzZXJJZCI6MjksInVzZXJDb2RlIjoi6YeR6J226L2v5Lu2IiwidGVuYW50TmFtZ" +
            "SI6IiIsImFwcElkIjoxMDAwNCwidGVuYW50SWQiOiIiLCJjbGllbnRGbGFnIjoiIiwidXNlclR5cGUiOiJ" +
            "CdXNpbmVzc1NlcnZpY2UiLCJpYXQiOjE3MjQzNzY3MTg1Nzd9.xiTewvVO8GiA9ba9r6GAr8Odq9vI7iHxt_oqTYgPsqo";
    /**
     * 5G工厂userCode
     */
    public static final String USERCODE = "金蝶软件";
    /**
     * 5G工厂AppId
     */
    public static final String APPID = "10004";

    /**
     * 5G工厂接口（外网）
     */
    public static final String INTRANETFACTORYUURL = "http://106.225.128.131:65500/mes/api/Index/IndexData/QueryByName";

    /**
     * 5G工厂接口（内网）
     */
    public static final String EXTERNALNETFACTORYUURL = "http://http://10.66.2.207/mes/api/Index/IndexData/QueryByName";
}
