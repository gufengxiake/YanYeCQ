package nckd.yanye.scm.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.exception.KDBizException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.user.UserServiceHelper;
import nckd.base.common.utils.log.LogUtil;
import nckd.yanye.scm.common.ZcPlatformConst;

import java.io.File;
import java.util.HashMap;

/**
 * 招采平台接口工具类
 *
 * @author liuxiao
 */
public class ZcPlatformApiUtil {
    private static final Log log = LogFactory.getLog(ZcPlatformApiUtil.class);

    /**
     * 获取 Access token
     *
     * @return Access_token
     */
    public static String getZcAccessToken(ZcPlatformConst zcPlatformConst) {
        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/oauth/token");

        httpRequest.setMethod(Method.GET);
        httpRequest.form("client_id", zcPlatformConst.getClientId());
        httpRequest.form("client_secret", zcPlatformConst.getClientSecret());
        httpRequest.form("grant_type", ZcPlatformConst.GRANT_TYPE);

        HttpResponse execute = httpRequest.execute();

        String token = null;
        // 输出响应内容
        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );

        if ((boolean) responseObj.get("success")) {
            token = (String) ((JSONObject) responseObj.get("data")).get("token");
        } else {
            throw new KDBizException("获取招采平台 Access_token失败!" + responseObj.getString("message"));
        }

        return token;
    }


    /**
     * 获取用户访问凭证
     *
     * @return token
     */
    public static String getZcUserToken(ZcPlatformConst zcPlatformConst) {
        String token;
        String mobile = (String) UserServiceHelper.getUserInfoByID(UserServiceHelper.getCurrentUserId()).get("phone");

        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/users/access-token");

        httpRequest.setMethod(Method.GET);
        httpRequest.form("platform", "1");
        httpRequest.form("mobile", mobile);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());

        HttpResponse execute = httpRequest.execute();

        // 输出响应内容
        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
        if ((boolean) responseObj.get("success")) {
            JSONObject dataObject = responseObj.getJSONObject("data");
            token = dataObject.getString("accessToken");
        } else {
            throw new KDBizException("获取招采平台用户访问凭证失败!" + responseObj.getString("message"));
        }
        return token;
    }

    /**
     * 获取当前登录员工的招采平台id
     *
     * @return
     */
    public static String getZcUserId(ZcPlatformConst zcPlatformConst) {
        //当前登录用户的手机号
        String mobile = (String) UserServiceHelper.getUserInfoByID(UserServiceHelper.getCurrentUserId()).get("phone");

        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/enterprise/employees/page");

        httpRequest.setMethod(Method.GET);
        httpRequest.form("page", "1");
        httpRequest.form("size", "1");
        httpRequest.form("keyword", mobile);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
        JSONObject data = responseObj.getJSONObject("data");
        JSONArray record = data.getJSONArray("records");
        if (!(boolean) responseObj.get("success")) {
            throw new KDBizException("获取您的招采平台信息失败!" + responseObj.getString("message"));
        }

        if (record.isEmpty()) {
            throw new KDBizException("您未在招采平台注册，请先注册");
        }
        return record.getJSONObject(0).getString("employeeId");
    }

    /**
     * 获取所有供应商
     *
     * @return
     */
    public static JSONArray getAllZcSupplier(ZcPlatformConst zcPlatformConst) {
        JSONArray allSuppliers = new JSONArray();
        String accessToken = getZcAccessToken(zcPlatformConst);

        int page = 1;
        int size = 1000;
        while (true) {
            HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/enterprise/companies/page");
            httpRequest.setMethod(Method.GET);
            httpRequest.form("page", page);
            httpRequest.form("size", size);
            //是否为供应商 0: 否 1: 是
            httpRequest.form("isSupplier", 1);
            httpRequest.header("Authorization", "Bearer " + accessToken);
            httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());

            HttpResponse execute = httpRequest.execute();

            JSONObject responseObj = JSON.parseObject(execute.body());
            // 记录调用日志
            LogUtil.newApiLog(
                    "nckd_pm_purapplybill_ext",
                    "采购申请单",
                    "cggl",
                    httpRequest.getUrl(),
                    httpRequest.form().toString(),
                    responseObj.toString()
            );
            if (!responseObj.getBooleanValue("success")) {
                break;
            }
            JSONArray records = responseObj.getJSONObject("data").getJSONArray("records");
            allSuppliers.addAll(records);
            page++;
            if (records.isEmpty()) {
                break;
            }
            if (page == 99) {
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
    public static Integer getCompanyIdByParam(ZcPlatformConst zcPlatformConst, String companyName, String socialCreditCode) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/enterprise/companies/page");
        httpRequest.setMethod(Method.GET);
        httpRequest.form("page", 1);
        httpRequest.form("size", 10);
        httpRequest.form("companyName", companyName);
        httpRequest.form("socialCreditCode", socialCreditCode);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
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
    public static JSONObject getCompanyDataById(ZcPlatformConst zcPlatformConst, String companyID) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/enterprise/companies/" + companyID);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
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
    public static JSONObject addOrder(ZcPlatformConst zcPlatformConst, JSONObject bodyJson, String bizType) {
        String string = bodyJson.toString();
        String accessToken = getZcAccessToken(zcPlatformConst);

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
        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + addorderUrl);

        httpRequest.setMethod(Method.POST);
        httpRequest.body(string);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.header("x-trade-employee-id", getZcUserId(zcPlatformConst));
        httpRequest.header("identity", "purchaser");

        HttpResponse execute = httpRequest.execute();

        // 输出响应内容
        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                string,
                responseObj.toString()
        );
        return responseObj;
    }

    /**
     * 采购单流标
     *
     * @param orderId
     * @return
     */
    public static JSONObject cancelOrder(ZcPlatformConst zcPlatformConst, HashMap<String, Object> cancelMap, String orderId, String bizType) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        String cancelOrderUrl = "";
        JSONObject cancelJson = new JSONObject();
        switch (bizType) {
            case "XB":
                cancelOrderUrl = ZcPlatformConst.ZC_URL + "/sourcing/purchaser/inquiry-orders/" + orderId + "/close/publish";
                cancelJson = ZcPlatformJsonUtil.getXbCancelJson(cancelMap);
                break;
            case "TP":
                cancelOrderUrl = ZcPlatformConst.ZC_URL + "/sourcing/purchaser/hosp-negotiate-orders/" + orderId + "/notice-close/v2/release";
                cancelJson = ZcPlatformJsonUtil.getTpCancelJson(cancelMap);
                break;
            case "ZB":
                cancelOrderUrl = ZcPlatformConst.ZC_URL + "/sourcing/purchaser/bidding-orders/" + orderId + "/notice-closes/release";
                cancelJson = ZcPlatformJsonUtil.getZbCancelJson(cancelMap);
                break;
            default:
                break;
        }

        HttpRequest httpRequest = HttpRequest.of(cancelOrderUrl);
        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.header("identity", "purchaser");
        httpRequest.header("x-trade-employee-id", getZcUserId(zcPlatformConst));
        httpRequest.body(cancelJson.toString());

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                cancelJson.toString(),
                responseObj.toString()
        );
        return responseObj;
    }

    /**
     * 采购单公告查看
     *
     * @param procurements
     * @param orderId
     * @return
     */
    public static String getViewNoticeUrl(ZcPlatformConst zcPlatformConst, String procurements, String orderId) {
        String userAccessToken = getZcUserToken(zcPlatformConst);
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
                        put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(zcPlatformConst,
                                "江西省盐业集团股份有限公司", "91360000158260136N"
                        ));
                    }
                });
            }
        };

        String config = configJson.toString();
        return (ZcPlatformConst.ZC_PASSPORTURL + "/third-access-v2?accessToken=" + userAccessToken + "&config=" + config);
    }

    /**
     * 上传附件
     *
     * @param name
     * @param url
     * @param attGroupId
     * @return
     */
    public static Integer uploadFile(ZcPlatformConst zcPlatformConst, String name, String url, Integer attGroupId) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/file/attachments/upload");

        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());

        File file = new File(url);

        httpRequest.form("file", file);
        httpRequest.form("name", name);
        httpRequest.form("groupId", attGroupId);

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );

        Integer attachmentId;
        if ((boolean) responseObj.get("success")) {
            JSONObject dataObject = responseObj.getJSONObject("data");
            attachmentId = dataObject.getInteger("attachmentId");
        } else {
            throw new KDBizException(responseObj.getString("message"));
        }

        file.delete();
        return attachmentId;


    }

    /**
     * 新增附件组，返回id
     *
     * @param bizType
     * @param attachmentType
     * @return
     */
    public static Integer addAttachmentGroup(ZcPlatformConst zcPlatformConst, String bizType, String attachmentType) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/file/attachment-groups");
        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        String string = new JSONObject() {
            {
                put("bizType", bizType);
                put("attachmentType", attachmentType);
            }
        }.toString();
        httpRequest.body(string);

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                string,
                responseObj.toString()
        );

        Integer groupId;
        if ((boolean) responseObj.get("success")) {
            JSONObject dataObject = responseObj.getJSONObject("data");
            groupId = dataObject.getInteger("groupId");
        } else {
            throw new KDBizException(responseObj.getString("message"));
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
    public static JSONObject getWinData(ZcPlatformConst zcPlatformConst, Integer purchaseType, String orderId, String winId) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/sourcing/purchase-cloud/orders/" + orderId + "/wins/" + winId);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.form("purchaseType", purchaseType);

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONObject("data");
        } else {
            throw new KDBizException("查询成交通知书失败!" + responseObj.getString("message"));
        }
    }


    /**
     * 查询采购单
     *
     * @param orderId
     * @param purchaseType
     * @return
     */
    public static JSONObject getOrderData(ZcPlatformConst zcPlatformConst, String orderId, Integer purchaseType) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/sourcing/purchase-cloud/orders/" + orderId);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.form("purchaseType", purchaseType);

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONObject("data");
        } else {
            throw new KDBizException("查询采购单失败!" + responseObj.getString("message"));
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
    public static JSONObject getAwardData(ZcPlatformConst zcPlatformConst, Integer purchaseType, String orderId, String awardId) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/sourcing/purchase-cloud/orders/" + orderId + "/win-awards/" + awardId);
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.form("purchaseType", purchaseType);

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONObject("data");
        } else {
            throw new KDBizException("查询成交授标失败!" + responseObj.getString("message"));
        }
    }


    /**
     * 采购单公告查看
     *
     * @param procurements
     * @param orderId
     * @return
     */
    public static String getViewWinNoticeUrl(ZcPlatformConst zcPlatformConst, String procurements, String orderId) {
        String userAccessToken = getZcUserToken(zcPlatformConst);
        String page = null;
        if ("2".equals(procurements)) {
            // 采购方式-询比价
            page = "InquiryOrderWinner";
        } else if ("8".equals(procurements)) {
            // 采购方式-竞争性谈判
            page = "NegotiationWinnerDetail";
        } else if ("1".equals(procurements)) {
            // 采购方式-招投采购
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
                        put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(zcPlatformConst,
                                "江西省盐业集团股份有限公司", "91360000158260136N"
                        ));
                    }
                });
            }
        };

        String config = configJson.toString();
        return (ZcPlatformConst.ZC_PASSPORTURL + "/third-access-v2?accessToken=" + userAccessToken + "&config=" + config);
    }

    /**
     * 查询采购单-品目列表
     *
     * @param purchaseType
     * @param orderId
     * @return
     */
    public static JSONArray getOrderItemsData(ZcPlatformConst zcPlatformConst, Integer purchaseType, String orderId) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/sourcing/purchase-cloud/orders/" + orderId + "/items");
        httpRequest.setMethod(Method.GET);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.form("purchaseType", purchaseType);

        HttpResponse execute = httpRequest.execute();


        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                httpRequest.form().toString(),
                responseObj.toString()
        );
        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONArray("data");
        } else {
            throw new KDBizException("查询品目列表失败!" + responseObj.getString("message"));
        }
    }

    /**
     * 预先生成评审单
     *
     * @return 评审单id reviewId
     */
    public static Integer getPurchaseReviews(ZcPlatformConst zcPlatformConst, String reviewModel) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/sourcing/purchaser/purchase-reviews/advance");
        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.header("x-trade-employee-id", getZcUserId(zcPlatformConst));
        httpRequest.header("identity", "purchaser");

        JSONObject jsonObject = new JSONObject()
//                //评审时间
//                .fluentPut("reviewTime", )
//                // 评审耗时
//                .fluentPut("consumeHour", )
//                //评审地址
//                .fluentPut("reviewAddress", )
//                // 其它备注说明
//                .fluentPut("otherRemark", )
                // 评审模式
                .fluentPut("reviewModel", reviewModel);
//                // 由专家打分
//                .fluentPut("isReviewerScore", 1);

        httpRequest.body(jsonObject.toString());

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                jsonObject.toString(),
                responseObj.toString()
        );

        if (responseObj.getBooleanValue("success")) {
            return responseObj.getJSONObject("data").getInteger("reviewId");
        } else {
            throw new KDBizException("预先生成评审单失败!" + responseObj.getString("message"));
        }
    }

    /**
     * 预先生成标书
     *
     * @return 标书id
     */
    public static Integer getBiddingFiles(ZcPlatformConst zcPlatformConst) {
        String accessToken = getZcAccessToken(zcPlatformConst);

        HttpRequest httpRequest = HttpRequest.of(ZcPlatformConst.ZC_URL + "/sourcing/purchaser/kpb/bidding-files/advance");
        httpRequest.setMethod(Method.POST);
        httpRequest.header("Authorization", "Bearer " + accessToken);
        httpRequest.header("X-Open-App-Id", zcPlatformConst.getClientId());
        httpRequest.header("x-trade-employee-id", getZcUserId(zcPlatformConst));
        httpRequest.header("identity", "purchaser");

        JSONObject jsonObject = new JSONObject()
                //电子标书名称
                .fluentPut("fileName", "电子标书");
//                // 开标地址-国家
//                .fluentPut("openBidCountry", )
//                // 开标地址-省
//                .fluentPut("openBidProvince", )
//                // 开标地址-市
//                .fluentPut("openBidCity", )
//                // 开标地址-区县
//                .fluentPut("openBidArea", )
//                // 开标地址-详细地址
//                .fluentPut("openBidAddress", 1);

        httpRequest.body(jsonObject.toString());

        HttpResponse execute = httpRequest.execute();

        JSONObject responseObj = JSON.parseObject(execute.body());
        // 记录调用日志
        LogUtil.newApiLog(
                "nckd_pm_purapplybill_ext",
                "采购申请单",
                "cggl",
                httpRequest.getUrl(),
                jsonObject.toString(),
                responseObj.toString()
        );

        if (responseObj.getBooleanValue("success")) {
            return responseObj.getInteger("data");
        } else {
            throw new KDBizException("预先生成标书失败!" + responseObj.getString("message"));
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
    public static String getOnlineReviewUrl(ZcPlatformConst zcPlatformConst, String procurements, Integer reviewId, String reviewMode) {

        String userAccessToken = getZcUserToken(zcPlatformConst);
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
                        put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(zcPlatformConst,
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
                    put("loginCompanyId", ZcPlatformApiUtil.getCompanyIdByParam(zcPlatformConst,
                            "江西省盐业集团股份有限公司", "91360000158260136N"
                    ));
                }
            });
        }

        String config = configJson.toString();
        return (ZcPlatformConst.ZC_PASSPORTURL + "/third-access-v2?accessToken=" + userAccessToken + "&config=" + config);
    }

}