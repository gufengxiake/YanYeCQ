package nckd.yanye.hr.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDBizException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClockInApiUtil {

    /**
     * 云之家团队id：26165950
     */
    private static final String YZJ_EID = "26165950";

    /**
     * 云之家密钥
     */
    private static final String YZJ_SECRET = "apqQLDJPISlrlrZqZntEPc74HJdB8e0S";

    /**
     * 钉钉应用的唯一标识key
     */
    private static final String DD_APPKEY = "";

    /**
     * 钉钉应用的密钥
     */
    private static final String DD_APPSECRET = "";


    private static Log log = LogFactory.getLog(ClockInApiUtil.class);


    /**
     * 云之家-获取accessToken
     *
     * @return 云之家accessToken
     */
    public static String getYunZhiJiaAccessToken() {
        String url = "https://yunzhijia.com/gateway/oauth2/token/getAccessToken";

        JSONObject body = new JSONObject()
                .fluentPut("eid", YZJ_EID)
                .fluentPut("secret", YZJ_SECRET)
                .fluentPut("timestamp", System.currentTimeMillis())
                .fluentPut("scope", "resGroupSecret");

        HttpRequest httpRequest = HttpRequest.of(url);
        httpRequest.setMethod(Method.POST);
        httpRequest.body(body.toString());
        HttpResponse execute = httpRequest.execute();
        JSONObject responseJson = JSON.parseObject(execute.body());

        if (responseJson.getBoolean("success")) {
            return responseJson.getJSONObject("data").getString("accessToken");
        } else {
            throw new KDBizException("获取云之家accessToken失败!" + responseJson.getString("error"));
        }
    }


    /**
     * 云之家-获取打卡列表
     *
     * @return 所有人员昨天到今天的打卡流水
     */
    public static JSONArray getYunZhiJiaClockInList() {
        String url = "https://yunzhijia.com/gateway/smartatt-core/v2/clockIn/listByUpdateTime"
                + "?accessToken=" + getYunZhiJiaAccessToken();

        // 查询时间
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String yesterdayStr = yesterday.format(formatter);
        String todayStr = today.format(formatter);

        // 获取苍穹人员集合
        DynamicObject[] users = BusinessDataServiceHelper.load(
                "bos_user",
                "useropenid",
                new QFilter[]{new QFilter("useropenid", QCP.is_notnull, null)}
        );

        JSONArray usersClockInList = new JSONArray();

        List<String> userIds = new ArrayList<>(100);
        for (DynamicObject user : users) {
            userIds.add(user.getString("useropenid"));
            if (userIds.size() == 100) {
                getClockInlistByUpdateTime(url, yesterdayStr, todayStr, usersClockInList, userIds);
                userIds.clear();
            }
        }

        if (!userIds.isEmpty()) {
            getClockInlistByUpdateTime(url, yesterdayStr, todayStr, usersClockInList, userIds);
        }

        return usersClockInList;
    }

    public static void getClockInlistByUpdateTime(String url, String yesterdayStr, String todayStr, JSONArray usersClockInList, List<String> userIds) {
        JSONObject body = new JSONObject()
                .fluentPut("startDate", yesterdayStr)
                .fluentPut("endDate", todayStr)
                .fluentPut("openIds", userIds);
        HttpRequest httpRequest = HttpRequest.of(url);
        httpRequest.setMethod(Method.POST);
        httpRequest.body(body.toString());
        HttpResponse execute = httpRequest.execute();
        JSONObject responseObj = JSON.parseObject(execute.body());
        if (responseObj.getBoolean("success")) {
            JSONArray data = responseObj.getJSONArray("data");
            if (data != null) {
                usersClockInList.addAll(data);
            }
        } else {
            throw new KDBizException("获取云之家打卡列表失败!" + responseObj.getString("errorMsg"));
        }
    }


    /**
     * 钉钉-获取accessToken
     *
     * @return 钉钉accessToken
     */
    public static String getDingDingAccessToken() {
        String url = "https://oapi.dingtalk.com/gettoken";

        HttpRequest httpRequest = HttpRequest.of(url);
        httpRequest.setMethod(Method.GET);
        httpRequest.form("appkey", DD_APPKEY);
        httpRequest.form("appsecret", DD_APPSECRET);
        HttpResponse execute = httpRequest.execute();
        JSONObject responseObj = JSON.parseObject(execute.body());

        if ("ok".equals(responseObj.getString("errmsg"))) {
            return responseObj.getString("access_token");
        } else {
            throw new KDBizException("获取钉钉accessToken失败!" + responseObj.getString("errmsg"));
        }
    }


    /**
     * 钉钉-获取企业下所有员工信息
     *
     * @return 所有员工id和userid的映射
     */
    public static HashMap<String, String> getDingDingUserList() {
        HashMap<String, String> uerIdMap = new HashMap<>();
        String accessToken = getDingDingAccessToken();
        List<Integer> deptList = getDingDingDeptList(1, accessToken);
        for (Integer deptId : deptList) {
            List<JSONObject> departmentUserDetails = getDepartmentUserDetails(deptId, accessToken);
            for (JSONObject departmentUserDetail : departmentUserDetails) {
                String jobNumber = departmentUserDetail.getString("job_number");
                uerIdMap.put(jobNumber, departmentUserDetail.getString("userid"));
            }
        }

        return uerIdMap;
    }


    /**
     * 获取指定部门的所有员工详情
     */
    public static List<JSONObject> getDepartmentUserDetails(Integer deptId, String accessToken) {
        List<JSONObject> userDetails = new ArrayList<>();
        int cursor = 0;
        boolean hasMore = true;

        while (hasMore) {
            JSONObject response = fetchDepartmentUsers(deptId, accessToken, cursor, 10);
            JSONArray userList = response.getJSONObject("result").getJSONArray("list");
            for (int i = 0; i < userList.size(); i++) {
                userDetails.add(userList.getJSONObject(i));
            }
            cursor = response.getJSONObject("result").getIntValue("next_cursor");
            hasMore = response.getJSONObject("result").getBooleanValue("has_more");
        }

        return userDetails;
    }

    /**
     * 钉钉-获取部门用户详情
     */
    private static JSONObject fetchDepartmentUsers(Integer deptId, String accessToken, int cursor, int size) {
        String url = "https://oapi.dingtalk.com/topapi/v2/user/list";

        HttpRequest httpRequest = HttpRequest.of(url)
                .setMethod(Method.POST)
                .form("access_token", accessToken);
        JSONObject body = new JSONObject()
                // 分页查询的游标，最开始传0，后续传返回参数中的next_cursor值。
                .fluentPut("cursor", cursor)
                // 是否返回访问受限的员工
                .fluentPut("contain_access_limit", false)
                // 分页大小
                .fluentPut("size", size)
                // 部门成员的排序规则，默认不传是按自定义排序（custom）：
//                .fluentPut("order_field", "modify_desc")
                // 通讯录语言，取值
                .fluentPut("language", "zh_CN")
                // 部门ID
                .fluentPut("dept_id", deptId);
        httpRequest.body(body.toString());
        HttpResponse execute = httpRequest.execute();
        JSONObject responseObj = JSON.parseObject(execute.body());
        if ("ok".equals(responseObj.getString("errmsg"))) {
            return responseObj;
        } else {
            throw new RuntimeException("获取部门用户详情失败: " + responseObj.getString("errmsg"));
        }
    }

    /**
     * 钉钉-获取企业所有部门
     */
    public static List<Integer> getDingDingDeptList(Integer deptId, String accessToken) {
        List<Integer> allDepartments = new ArrayList<>();
        fetchDepartments(deptId, accessToken, allDepartments);
        return allDepartments;
    }

    /**
     * 钉钉-递归获取所有部门ID
     */
    private static void fetchDepartments(Integer deptId, String accessToken, List<Integer> allDepartments) {
        JSONArray subDeptList = getDingDingSubDeptList(deptId, accessToken);
        if (subDeptList != null) {
            for (int i = 0; i < subDeptList.size(); i++) {
                JSONObject dept = subDeptList.getJSONObject(i);
                Integer childDeptId = dept.getInteger("dept_id");
                allDepartments.add(childDeptId);
                fetchDepartments(childDeptId, accessToken, allDepartments);
            }
        }
    }

    /**
     * 钉钉-获取子部门ID列表
     */
    public static JSONArray getDingDingSubDeptList(Integer deptId, String accessToken) {
        String url = "https://oapi.dingtalk.com/topapi/v2/department/listsub";

        HttpRequest httpRequest = HttpRequest.of(url);
        httpRequest.setMethod(Method.POST);
        httpRequest.form("access_token", accessToken);
        JSONObject body = new JSONObject()
                .fluentPut("dept_id", deptId);
        httpRequest.body(body.toString());
        HttpResponse execute = httpRequest.execute();
        JSONObject responseObj = JSON.parseObject(execute.body());
        if ("ok".equals(responseObj.getString("errmsg"))) {
            return responseObj.getJSONArray("result");
        } else {
            throw new KDBizException("获取钉钉子部门ID列表失败!" + responseObj.getString("errmsg"));
        }
    }

}
