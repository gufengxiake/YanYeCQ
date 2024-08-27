package nckd.yanye.hr.common.utils.capp;

 import kd.bos.db.DB;
 import kd.bos.db.DBRoute;
 import kd.bos.encrypt.Encrypters;
 import kd.bos.util.StringUtils;


public class CappConfig
 {
   private static final String CAPP_CONFIG = "capp_config";

   public static String getConfigValue(String code, String def) {
     String ret = getConfigValue(code);
     if (StringUtils.isEmpty(ret)) {
       ret = def;
     }
     return ret;
   }


   public static String getConfigValue(String code) {
     if (code == null || code.length() == 0) {
       return null;
     }
     String value = CacheBusinessData.get("capp_config", code);
     if (value != null) {
       value = Encrypters.decode(value);
       return value;
     }

     return (String)DB.query(DBRoute.of("sys"), "select fvalue from t_capp_config where fenable = 1 and fcode = ?",
             new Object[] { code },
             resultSet -> {
              String valuetemp = null;
              for (valuetemp = null; resultSet.next(); valuetemp = resultSet.getString(1));
              if (valuetemp == null) {
                 return null;
              }
             CacheBusinessData.set("capp_config", code, valuetemp);
             return Encrypters.decode(valuetemp);
         });
   }


   public static void refreshConfigValueCache(String code) {
     removeConfigValueCache(code);

     getConfigValue(code);
   }


   public static void removeConfigValueCache(String code) {
     if (code == null || code.length() == 0) {
       return;
     }

     CacheBusinessData.remove("capp_config", code);
   }
 }
