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

/**
 * 招采平台接口工具类
 *
 * @author liuxiao
 */
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
     * 测试环境访问指定页面url
     */
    public static final String PASSPORTURL = "http://passport.yingcaicheng.net";


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
     * 采购单发布
     *
     * @param bodyJson Body 参数
     * @param bizType
     * @return 结果
     */
    public static JSONObject addOrder(JSONObject bodyJson, String bizType) {
        String string = bodyJson.toString();
        String accessToken = getZcAccessToken();

        String addorderUrl = "";
        switch (bizType) {
            case "XB":
                addorderUrl = "/sourcing/purchaser/inquiry-orders/release";
                break;
            case "TP":
                addorderUrl = "/sourcing/purchaser/hosp-negotiate-orders/v2/release";
                break;
            case "ZB":
                addorderUrl = "/sourcing/purchaser/bidding-orders/v2/publish";
                break;
            default:
                break;
        }


        HttpRequest httpRequest = HttpRequest.of(URL + addorderUrl);

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

    /**
     * 上传附件
     *
     * @param
     * @param attGroupId
     * @return
     */
    public static Integer uploadFile(String name, String url, Integer attGroupId) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/file/attachments/upload");

        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);

        File file = new File(url);
        httpRequest.form("file", file);
        httpRequest.form("name", name);
        httpRequest.form("groupId", attGroupId);
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());

        Integer attachmentId;
        if ((boolean) responseObj.get("success")) {
            JSONObject dataObject = responseObj.getJSONObject("data");
            attachmentId = dataObject.getInteger("attachmentId");
        } else {
            throw new KDBizException(String.valueOf(responseObj.get("message")));
        }
        return attachmentId;

    }

    /**
     * 采购单流标
     *
     * @param orderId
     * @return
     */
    public static JSONObject cancelOrder(String orderId, String bizType) {
        String accessToken = getZcAccessToken();

        String cancelOrderUrl = "";
        JSONObject cancelJson = new JSONObject();

        switch (bizType) {
            case "XB":
                cancelOrderUrl = URL + "/sourcing/purchaser/inquiry-orders/" + orderId + "/close/publish";
                // 是否对外网公示 1：是 0：否
                cancelJson.put("closePublicity", 0);
                //流标类型 1：终止 2：重新
                cancelJson.put("closeType", 1);
                // 流标原因
                cancelJson.put("closeReason", 5);
                // 其他原因
                cancelJson.put("otherReason", "其它原因");
                //关闭公告
                cancelJson.put("notice", new JSONObject() {
                    {
                        put("noticeTitle", "测试流标公告标题");
                        put("noticeContent", "测试流标公告内容");
                    }
                });
                break;
            case "TP":
                cancelOrderUrl = URL + "/sourcing/purchaser/hosp-negotiate-orders/" + orderId + "/notice-close/v2/release";
                // 是否对外网公示 1：是 0：否
                cancelJson.put("closePublicity", 0);
                //流标类型 1：终止 2：重新
                cancelJson.put("closeType", 1);
                // 流标原因
                cancelJson.put("closeReason", 5);
                // 其他原因
                cancelJson.put("otherReason", "其它原因");
                //关闭公告
                cancelJson.put("notice", new JSONObject() {
                    {
                        put("title", "测试流标公告标题");
                        put("content", "测试流标公告内容");
                    }
                });
                break;
            case "ZB":
                cancelOrderUrl = "/sourcing/purchaser/bidding-orders/v2/publish";
                break;
            default:
                break;
        }

        HttpRequest httpRequest = HttpRequest.of(cancelOrderUrl);
        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.header("identity", "purchaser");
        httpRequest.header("x-trade-employee-id", getZcUserId());
        httpRequest.body(cancelJson.toString());
        HttpResponse execute = httpRequest.execute();

        return JSON.parseObject(execute.body());
    }

    /**
     * 询比公告查看
     *
     * @param orderId
     */
    public static String viewNotice(String procurements, String orderId) {
        String userAccessToken = getZcUserToken();
        String page = null;
        // 采购方式-询比价，单一品牌
        if ("pricecomparison".equals(procurements) || "singlebrand".equals(procurements)) {
            page = "InquiryDetail";

            // 采购方式-竞争性谈判
        } else if ("competitive".equals(procurements)) {
            page = "NegotiationInfo";

            // 采购方式-单一供应商
        } else if ("singlesupplier".equals(procurements)) {

            // 采购方式-招投采购
        } else if ("bidprocurement".equals(procurements)) {

        } else {
            throw new KDBizException("该单据未选择采购方式!");
        }

        String finalPage = page;
        JSONObject configJson = new JSONObject() {
            {
                put("platform", "purchase");
                put("page", finalPage);
                put("params", new JSONObject() {
                    {
                        put("orderId", orderId);
                    }
                });
                put("query", new JSONObject() {
                    {
                        put("loginCompanyId", 463);
                    }
                });
            }
        };

        String config = configJson.toString();
        return (PASSPORTURL + "/third-access?accessToken=" + userAccessToken + "&config=" + config);
    }

    /**
     * 新增附件组，返回id
     */
    public static Integer addAttachmentGroup(String bizType, String attachmentType) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/file/attachment-groups");
        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.body(new JSONObject() {
            {
                put("bizType", bizType);
                put("attachmentType", attachmentType);
            }
        }.toString());
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());

        Integer groupId;
        if ((boolean) responseObj.get("success")) {
            JSONObject dataObject = responseObj.getJSONObject("data");
            groupId = dataObject.getInteger("groupId");
        } else {
            throw new KDBizException(String.valueOf(responseObj.get("message")));
        }
        return groupId;
    }
}














