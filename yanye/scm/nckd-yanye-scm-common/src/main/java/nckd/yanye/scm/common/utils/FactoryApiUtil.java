package nckd.yanye.scm.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * 5G工厂接口工具类
 *
 * @author yaosijie
 */
public class FactoryApiUtil {

    private static final Log log = LogFactory.getLog(FactoryApiUtil.class);

    /**
     * 5G工厂接口
     * @param stringList
     * @param date
     * @return
     */
    public static JSONObject getFactoryInfo(List<String> stringList, Date date) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("indexNameList",stringList);
        jsonObject.put("dw", DateUtil.format(date,"yyyyMMdd"));
        log.info("调用5G工厂接口参数：{}",jsonObject);
        HttpRequest httpRequest = HttpRequest.of(FactoryUtil.INTRANETFACTORYUURL);
        httpRequest.setMethod(Method.POST);
        httpRequest.header("token", FactoryUtil.TOKEN);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.body(jsonObject.toString());
        HttpResponse execute = httpRequest.execute();
        log.info("调用5G工厂接口返回参数：{}",execute);
        return JSON.parseObject(execute.body());
    }
}
