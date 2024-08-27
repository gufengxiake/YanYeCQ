package nckd.base.common.utils.capp;

import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.encrypt.Encrypters;
import kd.bos.util.StringUtils;

/**
 * 系统配置参数缓存取值--自定义开发
 * 该类会用到的地方：单据名称：capp配置表  标识：capp_config
 * 用于维护系统配置或接口参数
 * author: chengchaohua
 * date: 2024-08-27
 */
public class CappConfig {
    // 缓存前缀标记
    private static final String CAPP_CONFIG = "capp_config";

    //缓存无值时，取设置的默认值
    public static String getConfigValue(String code, String def) {
        String ret;
        try {
            ret = getConfigValue(code);
            if (StringUtils.isEmpty(ret) && !StringUtils.isEmpty(def)) {
                ret = def;
                CacheBusinessData.set(CAPP_CONFIG, code, def);
            }
        } catch (Exception ignored) {
            return def;
        }
        return ret;
    }

    /**
     * 根据编码从缓存或数据库中获取配置参数值
     * 若缓存中没有,从数据库中获取后放入缓存中,过期时间12小时
     *
     * @return String
     */
    public static String getConfigValue(String code) {
        if (code == null || code.length() == 0) {
            return null;
        }
        String value = CacheBusinessData.get(CAPP_CONFIG, code);
        if (value != null) {
            value = Encrypters.decode(value);
            return value;
        } else {
            value = DB.query(DBRoute.of("sys"), "select fvalue from t_capp_config where fenable = '1' and fcode =  ?", new Object[]{code}, resultSet -> {
                String valuetemp = null;
                while (resultSet.next()) {
                    valuetemp = resultSet.getString(1);
                }
                if (valuetemp == null) {
                    return null;
                }
                CacheBusinessData.set(CAPP_CONFIG, code, valuetemp);
                valuetemp = Encrypters.decode(valuetemp);
                return valuetemp;
            });
            return value;
        }
    }

    /**
     * 根据编码刷新配置参数值缓存
     *
     * @param code 编码
     */
    public static void refreshConfigValueCache(String code) {
        // 清缓存
        removeConfigValueCache(code);
        // 重新赋新值给缓存
        getConfigValue(code);
    }

    /**
     * 清缓存
     *
     * @param code
     */
    public static void removeConfigValueCache(String code) {
        if (code == null || code.length() == 0) {
            return;
        }
        // 清缓存
        CacheBusinessData.remove(CAPP_CONFIG, code);
    }
}
