package nckd.base.common.utils.capp;

import kd.bos.cache.CacheFactory;
import kd.bos.cache.DistributeSessionlessCache;


/**
 *  自定义缓存
 * author: chengchaohua
 * date: 2024-08-27
 */
public class CacheBusinessData
{
  private static final String CACHE_REGION = "BUSSINESS_DATA";
  private static final String CACHE_TIMEOUT_KEY = "capp.cache.timeout";
  private static final String DEFAULT_CACHE_TIMEOUT = System.getProperty("capp.cache.timeout", String.valueOf(43200));


  private static DistributeSessionlessCache cache = CacheFactory.getCommonCacheFactory().getDistributeSessionlessCache("BUSSINESS_DATA");


  public static void set(String type, String key, String value, int timeout) {
    cache.put(getCombinedKey(type, key), value, timeout);
  }

  public static void set(String type, String key, String value) {
    set(type, key, value, Integer.parseInt(DEFAULT_CACHE_TIMEOUT));
  }

  public static String get(String type, String key) {
    return (String)cache.get(getCombinedKey(type, key));
  }

  public static void remove(String type, String key) {
    cache.remove(getCombinedKey(type, key));
  }

  private static String getKeyPrefix(String type) {
    return type + "-";
  }

  private static String getCombinedKey(String type, String key) {
    return getKeyPrefix(type) + key + "_" + System.getProperty("clusterName");
  }
}
