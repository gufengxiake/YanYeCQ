package nckd.yanye.scm.plugin.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.exception.KDBizException;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.io.File;

public class ZcPlatformApiUtil {
    /**
     * 授权类型，固定值: client_credentials
     */
    private static final String GRANT_TYPE = "client_credentials";

    /**
     * 应用id
     */
    private static final String ZC_CLIENT_ID = "jczh_1981169731";

    /**
     * 密钥
     */
    private static final String ZC_CLIENT_SECRET = "09bZJdpjoy3CdaietioBr1bgJvHfw1w2RAn7XAqDduI";

    /**
     * 测试环境url
     */
    private static final String URL = "http://open-api.yingcaicheng.net";


    /**
     * 获取 Access token
     *
     * @return Access_token
     */
    public static String getZcAccessToken() {
        HttpRequest httpRequest = HttpRequest.of(URL + "/oauth/token");

        httpRequest.setMethod(Method.GET);
        httpRequest.form("client_id", ZC_CLIENT_ID);
        httpRequest.form("client_secret", ZC_CLIENT_SECRET);
        httpRequest.form("grant_type", GRANT_TYPE);
        HttpResponse execute = httpRequest.execute();

        String token = null;
        // 输出响应内容
        JSONObject responseObj = JSON.parseObject(execute.body());
        if ((boolean) responseObj.get("success")) {
            token = (String) ((JSONObject) responseObj.get("data")).get("token");
        } else {
            throw new KDBizException(String.valueOf(responseObj.get("message")));
        }

        return token;
    }


    /**
     * 获取用户访问凭证
     *
     * @return token
     */
    public static String getZcUserToken() {
        String token;
        String mobile = (String) UserServiceHelper.getUserInfoByID(UserServiceHelper.getCurrentUserId()).get("phone");

        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/users/access-token");

        httpRequest.setMethod(Method.GET);
        httpRequest.form("platform", "1");
        httpRequest.form("mobile", mobile);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        HttpResponse execute = httpRequest.execute();

        // 输出响应内容
        JSONObject responseObj = JSON.parseObject(execute.body());
        if ((boolean) responseObj.get("success")) {
            JSONObject dataObject = responseObj.getJSONObject("data");
            token = dataObject.getString("accessToken");
        } else {
            throw new KDBizException(String.valueOf(responseObj.get("message")));
        }
        return token;
    }

    /**
     * 获取员工id
     *
     * @return
     */
    public static String getZcUserId() {
        //当前登录用户的手机号
        String mobile = (String) UserServiceHelper.getUserInfoByID(UserServiceHelper.getCurrentUserId()).get("phone");

        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/enterprise/employees/page");

        httpRequest.setMethod(Method.GET);
        httpRequest.form("page", "1");
        httpRequest.form("size", "1");
        httpRequest.form("keyword", mobile);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        JSONObject data = responseObj.getJSONObject("data");
        JSONArray record = data.getJSONArray("records");
        if (record.isEmpty()) {
            throw new KDBizException("您未在招采平台注册!");
        }
        return record.getJSONObject(0).getString("employeeId");
    }

    /**
     * 获取所有供应商
     *
     * @return
     */
    public static JSONArray getZcSupplier() {
        JSONArray allSuppliers = new JSONArray();
        String accessToken = getZcAccessToken();

        int page = 1;
        int size = 1000;
        while (true) {
            HttpRequest httpRequest = HttpRequest.of(URL + "/enterprise/companies/page");
            httpRequest.setMethod(Method.GET);
            httpRequest.form("page", page);
            httpRequest.form("size", size);
            //是否为供应商 0: 否 1: 是
            httpRequest.form("isSupplier", 1);
            httpRequest.header("Authorization", "Bearer " + accessToken);
            httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
            HttpResponse execute = httpRequest.execute();

            JSONObject responseObj = JSON.parseObject(execute.body());
            if (!responseObj.getBooleanValue("success")) {
                break;
            }
            JSONArray records = responseObj.getJSONObject("data").getJSONArray("records");
            allSuppliers.addAll(records);
            page++;
            if (page == 99) {
                break;
            }
            if (records.isEmpty()) {
                break;
            }
        }
        return allSuppliers;
    }


    /**
     * 询比单发布
     *
     * @param xbJson 询比单json
     * @return 结果
     */
    public static JSONObject addXBD(JSONObject xbJson) {
        String string = xbJson.toString();
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/sourcing/purchaser/inquiry-orders/release");

        httpRequest.setMethod(Method.POST);
        httpRequest.body(string);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.header("x-trade-employee-id", getZcUserId());
        httpRequest.header("identity", "purchaser");
        HttpResponse execute = httpRequest.execute();

        // 输出响应内容
        return JSON.parseObject(execute.body());
    }


    public static void test(String xbJson) {
        HttpRequest httpRequest = HttpRequest.of("http://passport.yingcaicheng.net/third-access?accessToken=${accessToken}&config=${config}");

        JSONObject config = new JSONObject() {
            {
                put("platform", "purchase");
                put("page", "InquiryOnlineReviewSetting");
                put("query", new JSONObject() {
                    {
                        // 登录公司id
                        put("loginCompanyId", 0);
                        // 评审单id，必填
                        put("reviewId", 0);
                        // 评审模式，必填
                        put("reviewModel", 0);
                        // 原评审单id，选填，澄清时使用
                        put("copyReviewId", 0);
                        // 第三方跳转标识
                        put("from", "ThirdAccess");
                    }
                });
            }
        };

    }


    /**
     * 上传附件
     *
     * @param
     * @return
     */
    public static JSONObject uploadFile(String name, String url) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/file/attachments/upload");

        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.form("file", new File(url));
        httpRequest.form("name", name);
        HttpResponse execute = httpRequest.execute();

        return JSON.parseObject(execute.body());
    }

    /**
     * 询比单流标
     *
     * @param
     * @return
     */
    public static JSONObject cancelXBD(String orderId, String noticeId) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/sourcing/purchaser/inquiry-orders/" + orderId + "/close/publish");
        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.header("identity", "purchaser");
        httpRequest.header("x-trade-employee-id", getZcUserId());

        JSONObject cancelJson = new JSONObject() {
            {
                // 是否公示 1：是 0：否
                put("closePublicity", 0);
                //流标类型 1：终止询比 2：重新询比
                put("closeType", 1);
                // 流标原因
                put("closeReason", 5);
                // 其他原因
                put("otherReason", "其它原因");
                //关闭公告
                put("notice", new JSONObject() {
                    {
                        put("noticeTitle", "流标公告");
                    }
                });
            }
        };
        httpRequest.body(cancelJson.toString());

        HttpResponse execute = httpRequest.execute();

        return JSON.parseObject(execute.body());
    }
}














