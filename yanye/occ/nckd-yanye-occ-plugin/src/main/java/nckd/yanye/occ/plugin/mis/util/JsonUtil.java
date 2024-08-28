package nckd.yanye.occ.plugin.mis.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Tan Manguang
 */
public class JsonUtil {

    public static final String NULL = "null";

    private static ObjectMapper getMapper(boolean ignoreUnknown) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, !ignoreUnknown);
        return mapper;
    }

    /**
     * 将结果转成Json串输出
     *
     * @param value 转换对象
     * @return json字符串
     */
    public static String toJsonString(Object value) {
        if (value == null) {
            return NULL;
        }

        try {
            return getMapper(false).writeValueAsString(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  "";
    }

    /**
     * 将字符串转换成泛型对象，比如List对象（可以用接口类型，对于List会使用ArrayList实例化，对于Map会使用LinkedHashMap实例化），
     * json串需要标准格式（即属性名及字符串值要以双引号包含）
     * 如：:
     * jsonStringToObject("[{\"age\":1,\"erpName\":\"aa\"},{\"age\":2,\"erpName\":\"12\"}]",
     * new TypeReference＜List＜Person＞＞(){});
     * 或
     * jsonStringToObject("[{\"age\":1,\"erpName\":\"aa\"},{\"age\":2,\"erpName\":\"12\"}]",
     * new TypeReference＜List＜Map＜?,?＞＞＞(){});
     *
     * @param json
     * @param type
     * @return T
     */
    public static <T> T jsonStringToObject(String json, TypeReference<T> type) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return getMapper(false).readValue(json, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
