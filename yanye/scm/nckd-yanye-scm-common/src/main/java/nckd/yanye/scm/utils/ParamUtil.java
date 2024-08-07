package nckd.yanye.scm.utils;

import kd.bos.entity.AppInfo;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.param.AppCustomParam;
import kd.bos.entity.param.AppParam;
import kd.bos.entity.param.CustomParam;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;

import java.util.HashSet;
import java.util.Map;

/**
 * 系统参数服务
 */
public class ParamUtil {


    /**
     * 从缓存中加载应用参数
     *
     * @param appNumber 应用标识
     * @param paramPro  参数key
     * @return 参数值
     */
    public static Object getSysCtrlParameter(String appNumber, String paramPro) {
        AppParam appParam = new AppParam();
        AppInfo appInfo = AppMetadataCache.getAppInfo(appNumber);
        String appId = appInfo.getId();
        appParam.setAppId(appId);
        appParam.setOrgId(OrgUnitServiceHelper.getRootOrgId());
        Map<String, Object> map = SystemParamServiceHelper.loadAppParameterFromCache(appParam);
        return map == null ? null : map.get(paramPro);
    }


    /**
     * 获取应用参数-自定义参数
     *
     * @param appNumber 应用标识
     * @param paramPro  参数key
     * @return 参数值
     */
    public static String getAppCustomParameter(String appNumber, String paramPro) {
        AppCustomParam appParam = new AppCustomParam();
        AppInfo appInfo = AppMetadataCache.getAppInfo(appNumber);
        String appId = appInfo.getId();
        appParam.setAppId(appId);
        HashSet<String> set = new HashSet<String>();
        set.add(paramPro);
        appParam.setSearchKeySet(set);
        Map<String, String> map = SystemParamServiceHelper.loadAppCustomParameterFromCache(appParam);
        return map == null ? null : map.get(paramPro);
    }

    /**
     * 获取系统参数-自定义参数
     *
     * @param paramPro 参数key
     * @return 参数值
     */
    public static String getSysCustomParameter(String paramPro) {
        CustomParam customParam = new CustomParam();
        HashSet<String> set = new HashSet<String>();
        set.add(paramPro);
        customParam.setSearchKeySet(set);
        Map<String, String> map = SystemParamServiceHelper.loadCustomParameterFromCache(customParam);
        return map == null ? null : map.get(paramPro);
    }

}
