package nckd.yanye.scm.common.utils;

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
     * 获取当前登录员工的招采平台id
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
     * 根据公司名称或者社会信用代码查询公司id
     *
     * @param companyName      公司名称
     * @param socialCreditCode 社会信用代码
     * @return
     */
    public static Integer getCompanyIdByParam(String companyName, String socialCreditCode) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/enterprise/companies/page");
        httpRequest.setMethod(Method.GET);
        httpRequest.form("page", 1);
        httpRequest.form("size", 10);
        httpRequest.form("companyName", companyName);
        httpRequest.form("socialCreditCode", socialCreditCode);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        if (!responseObj.getBooleanValue("success")) {
            throw new KDBizException("查询公司信息失败!" + responseObj.getString("message"));
        }

        JSONArray records = responseObj.getJSONObject("data").getJSONArray("records");
        // 第一个公司信息
        JSONObject companyInfo = (JSONObject) records.get(0);
        return companyInfo.getInteger("companyId");
    }

    /**
     * 根据公司id查询公司信息
     *
     * @param companyID
     * @return
     */
    public static JSONObject getCompanyDataById(String companyID) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/enterprise/companies/" + companyID);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        if (!responseObj.getBooleanValue("success")) {
            throw new KDBizException("查询公司信息失败!" + responseObj.getString("message"));
        }

        return responseObj.getJSONObject("data");
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
            case "DY":
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
                cancelJson = ZcPlatformJsonUtil.getXbCancelJson();
                break;
            case "TP":
                cancelOrderUrl = URL + "/sourcing/purchaser/hosp-negotiate-orders/" + orderId + "/notice-close/v2/release";
                cancelJson = ZcPlatformJsonUtil.getTpCancelJson();
                break;
            case "ZB":
                cancelOrderUrl = URL + "/sourcing/purchaser/bidding-orders/" + orderId + "/notice-closes/release";
                cancelJson = ZcPlatformJsonUtil.getZbCancelJson();
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
     * 采购单公告查看
     *
     * @param procurements
     * @param orderId
     * @return
     */
    public static String viewNotice(String procurements, String orderId) {
        String userAccessToken = getZcUserToken();
        String page = null;
        if ("pricecomparison".equals(procurements) || "singlebrand".equals(procurements)) {
            // 采购方式-询比价，单一品牌
            page = "InquiryDetail";
        } else if ("competitive".equals(procurements)) {
            // 采购方式-竞争性谈判
            page = "NegotiationInfo";
        } else if ("singlesupplier".equals(procurements) || "bidprocurement".equals(procurements)) {
            // 采购方式-单一供应商，招投采购
            page = "BiddingInfo";
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
                        put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(
                                "江西省盐业集团股份有限公司", "91360000158260136N"
                        ));
                    }
                });
            }
        };

        String config = configJson.toString();
        return (PASSPORTURL + "/third-access-v2?accessToken=" + userAccessToken + "&config=" + config);
    }

    /**
     * 上传附件
     *
     * @param name
     * @param url
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
     * 新增附件组，返回id
     *
     * @param bizType
     * @param attachmentType
     * @return
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


    /**
     * 查询成交通知书
     *
     * @param purchaseType 采购类型
     * @param orderId      采购单id
     * @param winId        成交id
     * @return 成交data
     */
    public static JSONObject getWinData(Integer purchaseType, String orderId, String winId) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/sourcing/purchase-cloud/orders/" + orderId + "/wins/" + winId);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.form("purchaseType", purchaseType);

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());

        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONObject("data");
        } else {
            throw new KDBizException("查询成交通知书失败!");
        }
    }


    /**
     * 查询采购单
     *
     * @param orderId
     * @param purchaseType
     * @return
     */
    public static JSONObject getOrderData(String orderId, Integer purchaseType) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/sourcing/purchase-cloud/orders/" + orderId);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.form("purchaseType", purchaseType);
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());

        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONObject("data");
        } else {
            throw new KDBizException("查询采购单失败!");
        }
    }


    /**
     * 查询成交授标
     *
     * @param purchaseType 采购类型
     * @param orderId      采购单id
     * @param awardId      授标id
     * @return 授标data
     */
    public static JSONObject getAwardData(Integer purchaseType, String orderId, String awardId) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/sourcing/purchase-cloud/orders/" + orderId + "/win-awards/" + awardId);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.form("purchaseType", purchaseType);
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());

        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONObject("data");
        } else {
            throw new KDBizException("查询成交授标失败!");
        }
    }


    /**
     * 采购单公告查看
     *
     * @param procurements
     * @param orderId
     * @return
     */
    public static String viewWinNotice(String procurements, Integer orderId) {
        String userAccessToken = getZcUserToken();
        String page = null;
        if ("pricecomparison".equals(procurements) || "singlebrand".equals(procurements)) {
            // 采购方式-询比价，单一品牌
            page = "InquiryOrderWinner";
        } else if ("competitive".equals(procurements)) {
            // 采购方式-竞争性谈判
            page = "NegotiationWinnerDetail";
        } else if ("singlesupplier".equals(procurements) || "bidprocurement".equals(procurements)) {
            // 采购方式-单一供应商，招投采购
            page = "BiddingWinnerDetail";
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
                        put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(
                                "江西省盐业集团股份有限公司", "91360000158260136N"
                        ));
                    }
                });
            }
        };

        String config = configJson.toString();
        return (PASSPORTURL + "/third-access-v2?accessToken=" + userAccessToken + "&config=" + config);
    }

    /**
     * 查询采购单-品目列表
     *
     * @param purchaseType
     * @param orderId
     * @return
     */
    public static JSONArray getOrderItemsData(Integer purchaseType, String orderId) {
        String accessToken = getZcAccessToken();

        HttpRequest httpRequest = HttpRequest.of(URL + "/sourcing/purchase-cloud/orders/" + orderId + "/items");
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", ZC_CLIENT_ID);
        httpRequest.form("purchaseType", purchaseType);
        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());

        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONArray("data");
        } else {
            throw new KDBizException("查询品目列表失败!");
        }
    }

    /**
     * 线上评审文件制作
     *
     * @param procurements 采购方式
     * @param reviewId     评审单id
     * @param reviewMode   评审模式
     * @return url
     */
    public static String getOnlineReview(String procurements, String reviewId, String reviewMode) {

        String userAccessToken = getZcUserToken();
        String page = null;
        if ("pricecomparison".equals(procurements) || "singlebrand".equals(procurements)) {
            // 采购方式-询比价，单一品牌
            page = "InquiryOnlineReviewSetting";
        } else if ("competitive".equals(procurements)) {
            // 采购方式-竞争性谈判
            page = "NegotiationOnlineReviewSetting";
        } else if ("singlesupplier".equals(procurements) || "bidprocurement".equals(procurements)) {
            // 采购方式-单一供应商，招投采购
            page = "KpbBidFileForm";
        } else {
            throw new KDBizException("该单据未选择采购方式!");
        }

        String finalPage = page;
        JSONObject configJson = new JSONObject() {
            {
                put("platform", "purchase");
                put("page", finalPage);
                put("query", new JSONObject() {
                    {
                        // 登录公司id，选填
                        put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(
                                "江西省盐业集团股份有限公司", "91360000158260136N"
                        ));
                        // 评审单id，必填
                        put("reviewId", reviewId);
                        // 评审模式，必填
                        put("reviewMode", reviewMode);
                        // 第三方跳转标识
                        put("from", "ThirdAccess");
                    }
                });
            }
        };

        if ("singlesupplier".equals(procurements) || "bidprocurement".equals(procurements)) {
            configJson = new JSONObject() {
                {
                    put("platform", "purchase");
                    put("page", "KpbBidFileForm");
                    put("params", new JSONObject() {
                        {
                            // 电子标书id，必填
                            put("kpbFileId", reviewId);
                        }
                    });
                }
            };
            configJson.put("query", new JSONObject() {
                {
                    // 登录公司id，选填
                    put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(
                            "江西省盐业集团股份有限公司", "91360000158260136N"
                    ));
                }
            });
        }

        String config = configJson.toString();
        return (PASSPORTURL + "/third-access-v2?accessToken=" + userAccessToken + "&config=" + config);
    }

}
















