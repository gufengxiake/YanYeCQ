package nckd.yanye.scm.common.utils;

import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * http请求工具类
 * author：xiaoxiaopeng
 * date：2024-09-24
 */
public class HttpRequestUtils {

    private final static Logger logger = LoggerFactory.getLogger(HttpRequestUtils.class);

    /**
     * post请求
     * @param url         url地址
     * @param jsonParam     参数
     * @return
     */

    public static JSONObject httpPost(String url,JSONObject jsonParam,String token){

        //post请求返回结果
        DefaultHttpClient httpClient = new DefaultHttpClient();
        JSONObject jsonResult = null;
        HttpPost method = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(4000).setConnectTimeout(5000).build();//设置请求和传输超时时间
        method.setConfig(requestConfig);
        if (token != null) {
            method.setHeader("Authorization","Bearer\n" + token);
            if (null != jsonParam) {
                //解决中文乱码问题
                StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                method.setEntity(entity);

            }
        }else {
            method.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            List<NameValuePair> params = new ArrayList<>();
            Map<String, Object> innerMap = jsonParam.getInnerMap();
            for (Map.Entry<String, Object> entry : innerMap.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }
            try {
                method.setEntity(new UrlEncodedFormEntity(params,"utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            HttpResponse result = httpClient.execute(method);
            url = URLDecoder.decode(url, "UTF-8");
            /**请求发送成功，并得到响应**/
            if (result.getStatusLine().getStatusCode() == 200) {
                String str = "";
                try {
                    /**读取服务器返回过来的json字符串数据**/
                    str = EntityUtils.toString(result.getEntity());
                    /**把json字符串转换成json对象**/
                    jsonResult = JSONObject.parseObject(str);
                } catch (Exception e) {
                    logger.error("post请求提交失败:" + url, e);
                }
            }
        } catch (IOException e) {
            logger.error("post请求提交失败:" + url, e);
        }
        return jsonResult;
    }


    public static void setGeneralLog(Map<String,Object> map){
        DynamicObject generalLog = BusinessDataServiceHelper.newDynamicObject("nckd_general_log");
        generalLog.set("number", map.get("number"));
        generalLog.set("name", map.get("name"));
        generalLog.set("creator", map.get("creator"));
        generalLog.set("nckd_system", map.get("nckd_system"));
        generalLog.set("nckd_interfaceurl", map.get("nckd_interfaceurl"));
        generalLog.set("createtime", map.get("createtime"));
        generalLog.set("nckd_parameter", map.get("nckd_parameter"));
        generalLog.set("nckd_returnparameter", map.get("nckd_returnparameter"));
        generalLog.set("status", "C");
        generalLog.set("enable", "1");

        OperationServiceHelper.executeOperate("save", map.get("number").toString(), new DynamicObject[]{generalLog}, OperateOption.create());
    }
}
