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
import nckd.yanye.hr.common.ClockInConst;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClockInApiUtil {

    private static Log log = LogFactory.getLog(ClockInApiUtil.class);


    /**
     * 云之家-获取accessToken
     *
     * @return 云之家accessToken
     */
    public static String getYunZhiJiaAccessToken() {
        String url = "https://yunzhijia.com/gateway/oauth2/token/getAccessToken";

        JSONObject body = new JSONObject()
                .fluentPut("eid", ClockInConst.YZJ_EID)
                .fluentPut("secret", ClockInConst.YZJ_SECRET)
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
        LocalDate yesterday = LocalDate.now().minusDays(2);
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
                getYunZhiJiaClockInlist(url, yesterdayStr, todayStr, usersClockInList, userIds);
                userIds.clear();
            }
        }

        if (!userIds.isEmpty()) {
            getYunZhiJiaClockInlist(url, yesterdayStr, todayStr, usersClockInList, userIds);
        }

        return usersClockInList;
    }

    public static void getYunZhiJiaClockInlist(String url, String yesterdayStr, String todayStr, JSONArray usersClockInList, List<String> userIds) {
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
        httpRequest.form("appkey", ClockInConst.DD_APPKEY);
        httpRequest.form("appsecret", ClockInConst.DD_APPSECRET);
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
     * @return 所有手机号和userid的映射
     */
    public static HashMap<String, String> getDingDingUserList() {
        HashMap<String, String> uerIdMap = new HashMap<>();
        String accessToken = getDingDingAccessToken();
        List<Integer> deptList = getDingDingDeptList(1, accessToken);
        for (Integer deptId : deptList) {
            List<JSONObject> departmentUserDetails = getDepartmentUserDetails(deptId, accessToken);
            for (JSONObject departmentUserDetail : departmentUserDetails) {
                String jobNumber = departmentUserDetail.getString("mobile");
                uerIdMap.put(jobNumber, departmentUserDetail.getString("userid"));
            }
        }

        return uerIdMap;
    }


    /**
     * 钉钉-获取指定部门的所有员工详情
     *
     * @param deptId      部门id
     * @param accessToken accessToken
     * @return 该部门下员工详情
     */
    public static List<JSONObject> getDepartmentUserDetails(Integer deptId, String accessToken) {
        List<JSONObject> userDetails = new ArrayList<>();
        int cursor = 0;
        boolean hasMore = true;

        while (hasMore) {
            JSONObject response = fetchDepartmentUsers(deptId, accessToken, cursor, 100);
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
     *
     * @param deptId
     * @param accessToken
     * @param cursor
     * @param size
     * @return
     */
    private static JSONObject fetchDepartmentUsers(Integer deptId, String accessToken, int cursor, int size) {
        String url = "https://oapi.dingtalk.com/topapi/v2/user/list";
        url = url + "?access_token=" + accessToken;

        HttpRequest httpRequest = HttpRequest.of(url)
                .setMethod(Method.POST);
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
     *
     * @param deptId
     * @param accessToken
     * @return
     */
    public static List<Integer> getDingDingDeptList(Integer deptId, String accessToken) {
        List<Integer> allDepartments = new ArrayList<>();
        fetchDepartments(deptId, accessToken, allDepartments);
        return allDepartments;
    }

    /**
     * 钉钉-递归获取所有部门ID
     *
     * @param deptId
     * @param accessToken
     * @param allDepartments
     */
    private static void fetchDepartments(Integer deptId, String accessToken, List<Integer> allDepartments) {
        JSONArray subDeptList = getDingDingSubDeptList(deptId, accessToken);
        if (subDeptList != null) {
            for (Object o : subDeptList) {
                Integer childDeptId = (Integer) o;
                allDepartments.add(childDeptId);
                fetchDepartments(childDeptId, accessToken, allDepartments);
            }
        }
    }

    /**
     * 钉钉-获取子部门ID列表
     *
     * @param deptId
     * @param accessToken
     * @return
     */
    public static JSONArray getDingDingSubDeptList(Integer deptId, String accessToken) {
        String url = "https://oapi.dingtalk.com/topapi/v2/department/listsubid";
        url = url + "?access_token=" + accessToken;

        HttpRequest httpRequest = HttpRequest.of(url);
        httpRequest.setMethod(Method.POST);
        JSONObject body = new JSONObject()
                .fluentPut("dept_id", deptId);
        httpRequest.body(body.toString());
        HttpResponse execute = httpRequest.execute();
        JSONObject responseObj = JSON.parseObject(execute.body());
        if ("ok".equals(responseObj.getString("errmsg"))) {
            return responseObj.getJSONObject("result").getJSONArray("dept_id_list");
        } else {
            throw new KDBizException("获取钉钉子部门ID列表失败!" + responseObj.getString("errmsg"));
        }
    }

    /**
     * 钉钉-获取打卡结果
     */
    public static JSONArray getDingDingClockInList() {
        String url = "https://oapi.dingtalk.com/attendance/list?access_token=" + ClockInApiUtil.getDingDingAccessToken();

        // 查询时间
        LocalDate yesterday = LocalDate.now().minusDays(2);
        LocalDate today = LocalDate.now();
        // 使用LocalDateTime代替LocalDate
        LocalDateTime yesterdayDateTime = yesterday.atStartOfDay();
        LocalDateTime todayDateTime = today.atStartOfDay();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String yesterdayStr = yesterdayDateTime.format(formatter);
        String todayStr = todayDateTime.format(formatter);

        // 获取苍穹人员集合
        DynamicObject[] users = BusinessDataServiceHelper.load(
                "bos_user",
                "id,name,nckd_dingdingid",
                new QFilter[]{new QFilter("nckd_dingdingid", QCP.not_equals, "")}
        );

        JSONArray usersClockInList = new JSONArray();

        ArrayList<String> userIds = new ArrayList<>(50);
        for (DynamicObject user : users) {
            userIds.add(user.getString("nckd_dingdingid"));
            if (userIds.size() == 50) {
                int offset = 0;
                boolean hasMore = true;

                while (hasMore) {
                    JSONObject responseObj = getDingDingClockInlist(userIds, offset, url, yesterdayStr, todayStr);
                    usersClockInList.addAll(responseObj.getJSONArray("recordresult"));
                    offset = offset + 50;
                    hasMore = responseObj.getBoolean("hasMore");
                }
                userIds.clear();
            }
        }

        if (!userIds.isEmpty()) {
            int offset = 0;
            boolean hasMore = true;

            while (hasMore) {
                JSONObject responseObj = getDingDingClockInlist(userIds, offset, url, yesterdayStr, todayStr);
                usersClockInList.addAll(responseObj.getJSONArray("recordresult"));
                offset = offset + 50;
                hasMore = responseObj.getBoolean("hasMore");
            }
            userIds.clear();
        }

        return usersClockInList;
    }

    private static JSONObject getDingDingClockInlist(ArrayList<String> userIds, Integer offset, String url, String yesterdayStr, String todayStr) {
        JSONObject body = new JSONObject()
                // 查询考勤打卡记录的起始工作日。
                // 格式为yyyy-MM-dd HH:mm:ss，HH:mm:ss可以使用00:00:00，将返回此日期从0点到24点的结果。
                .fluentPut("workDateFrom", yesterdayStr)
                // 查询考勤打卡记录的结束工作日。
                // 格式为“yyyy-MM-dd HH:mm:ss”，HH:mm:ss可以使用00:00:00，将返回此日期从0点到24点的结果。
                .fluentPut("workDateTo", todayStr)
                // 员工在企业内的userId列表，最大值50。
                .fluentPut("userIdList", userIds)
                // 表示获取考勤数据的起始点。
                // 第一次传0，如果还有多余数据，下次获取传的offset值为之前的offset+limit，0、1、2...依次递增。
                .fluentPut("offset", offset)
                .fluentPut("limit", 50);

        HttpRequest httpRequest = HttpRequest.of(url);
        httpRequest.setMethod(Method.POST);
        httpRequest.body(body.toString());
        HttpResponse execute = httpRequest.execute();
        JSONObject responseObj = JSON.parseObject(execute.body());

        if ("ok".equals(responseObj.getString("errmsg"))) {
            return responseObj;
        } else {
            throw new KDBizException("获取钉钉打卡列表失败!" + responseObj.getString("errmsg"));
        }
    }

}
