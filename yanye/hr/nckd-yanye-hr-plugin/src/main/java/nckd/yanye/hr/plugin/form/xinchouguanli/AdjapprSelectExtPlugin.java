package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.IDecAdjApprExtPlugin;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.event.AfterF7PersonSelectEvent;
import kd.sdk.swc.hcdm.service.spi.SalaryStdQueryService;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工定调薪申请单-业务扩展插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 * 业务场景编码：kd.sdk.swc.hcdm.business.extpoint.adjapprbill.IDecAdjApprExtPlugin#onAfterF7PersonSelect
 *
 * @author ：luxiao
 * @since ：Created in 17:28 2024/9/11
 */
public class AdjapprSelectExtPlugin implements IDecAdjApprExtPlugin {

    /**
     * 在添加定调薪人员时，选择定调薪人员后，需要二开对新增的字段赋值
     *
     * @param e
     */
    @Override
    public void onAfterF7PersonSelect(AfterF7PersonSelectEvent e) {
        List<DynamicObject> adjPersonDyObjList = e.getAdjPersonDyObjList();
        for (DynamicObject adjObj : adjPersonDyObjList) {
            DynamicObject adjapprBill = BusinessDataServiceHelper.loadSingle(
                    adjObj.get("adjapprbill"),
                    "hcdm_adjapprbill");

            DynamicObject salaryadjrsn = adjapprBill.getDynamicObject("salaryadjrsn");
            if (salaryadjrsn == null) {
                return;
            }
            // 组织范围：针对晶昊公司和富达公司
            DynamicObject org = adjapprBill.getDynamicObject("org");
            String orgName = org.getString("name");
            String salaryadjrsnName = salaryadjrsn.getString("name");
            if ("年度绩效调薪".equals(salaryadjrsnName) &&
                    ("晶昊本部".equals(orgName) || "江西富达盐化有限公司".equals(orgName))) {
                addAllYearPerson(adjObj);
            }
        }
    }


    private void addAllYearPerson(DynamicObject adjObj) {
        Map<String, List<Map<String, String>>> yearKaoheMap = getYearKaoheMap();

        // 获取所有任职经历Map <员工id, 职级名称>
        DynamicObject[] jobExpArray = BusinessDataServiceHelper.load(
                "hrpi_empposorgrel",
                "person,nckd_zhiji",
                new QFilter[]{
                        // 是否主任职：是
                        new QFilter("isprimary", QCP.equals, "1"),
                        // 开始日期小于今天
                        new QFilter("startdate", QCP.less_than, new Date()),
                        // 结束日期大于今天
                        new QFilter("enddate", QCP.large_than, new Date()),
                        // 业务状态：生效中
                        new QFilter("businessstatus", QCP.equals, "1"),
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );
        Map<String, String> jobExpMap = Arrays.stream(jobExpArray)
                .collect(Collectors.toMap(
                        obj -> obj.getString("person.number"),
                        obj -> obj.getString("nckd_zhiji.name") == null ? "" : obj.getString("nckd_zhiji.name")
                ));

        DynamicObjectCollection entryEntity = adjObj.getDynamicObjectCollection("entryentity");
        //  根据调薪单据日期，判定年份。取此年份上一年的年度绩效考核成绩为考核成绩
        Date effectivedate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(effectivedate);
        // 上一年
        calendar.add(Calendar.YEAR, -1);
        String lastYearDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());

        // 上上年
        calendar.add(Calendar.YEAR, -1);
        String lastTwoYearsDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());

        // 上上上年
        calendar.add(Calendar.YEAR, -1);
        String lastThreeYearsDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());

        for (DynamicObject entry : entryEntity) {
            // 人员信息
            DynamicObject person = adjObj.getDynamicObject("person");
            // 获取此薪档下2档金额和1档金额
            // 本次薪酬标准表
            DynamicObject salarystd = entry.getDynamicObject("salarystd");
            // 上次薪酬标准表
            DynamicObject presalarystd = entry.getDynamicObject("presalarystd");
            if (salarystd == null || presalarystd == null) {
                entry.set("nckd_notesinfo", "未作处理：找不到上次薪酬信息");
                continue;
            }
            long salaryStdId = salarystd.getLong("id");
            long preSalaryStdId = presalarystd.getLong("id");

            // SPI接口2：获取标准表的薪档信息
            Map<Long, List<Map<String, Object>>> thisResultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(salaryStdId));
            Map<Long, List<Map<String, Object>>> preResultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(preSalaryStdId));
            if (thisResultMap == null || thisResultMap.isEmpty()) {
                entry.set("nckd_notesinfo", "未作处理：找不到标准表的薪档信息");
                continue;
            }
            Map<Integer, Long> thisRankMap = thisResultMap.get(salaryStdId).stream().collect(
                    Collectors.toMap(
                            obj -> (Integer) obj.get("rankIndex"),
                            obj -> (Long) obj.get("rankId")
                    ));
            Map<Long, Integer> preRankMap = preResultMap.get(preSalaryStdId).stream().collect(
                    Collectors.toMap(
                            obj -> (Long) obj.get("rankId"),
                            obj -> (Integer) obj.get("rankIndex")
                    ));
            // 本次最高和最低档
            Long thisMaxRankId = thisRankMap.get(thisRankMap.size());
            Long thisMinRankId = thisRankMap.get(1);

            // 该员工职级
            String zhiJiName = jobExpMap.get(person.getString("number"));
            if (StringUtils.isEmpty(zhiJiName)) {
                entry.set("nckd_notesinfo", "未作处理：找不到职级信息");
                continue;
            }

            // 员工年度考核数据
            List<Map<String, String>> yearKaoheResult = yearKaoheMap.get(person.getString("number"));
            if (yearKaoheResult == null || yearKaoheResult.isEmpty()) {
                entry.set("nckd_notesinfo", "未作处理：找不到员工年度考核数据");
                continue;
            }
            // 员工近三年的年度绩效考核成绩
            String lastYearKaoHeResult = yearKaoheResult.stream()
                    .map(map -> map.get(lastYearDateString))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            String lastTwoYearsKaoHeResult = yearKaoheResult.stream()
                    .map(map -> map.get(lastTwoYearsDateString))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            String lastThreeYearsKaoHeResult = yearKaoheResult.stream()
                    .map(map -> map.get(lastThreeYearsDateString))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            // 员工上次薪等
            DynamicObject preGrade = entry.getDynamicObject("pregrade");
            // 员工上次薪档
            DynamicObject preRank = entry.getDynamicObject("prerank");
            Integer preRankIndex = preRankMap.get(preRank.getLong("id"));
            // 本次调档范围
            int changeRank = 0;
            // 员工的薪档逻辑：
            if ("员工".equals(zhiJiName)) {
                switch (lastYearKaoHeResult) {
                    case "优秀":
                        changeRank = 3;
                        break;
                    case "良好":
                        changeRank = 2;
                        break;
                    case "称职":
                        changeRank = 1;
                        break;
                    case "基本称职":
                        changeRank = 0;
                        break;
                    case "不称职":
                        changeRank = -1;
                        break;
                    default:
                        break;
                }
                entry.set("grade", preGrade);
                int thisRankIndex = preRankIndex + changeRank;
                Long thisRankId = thisRankMap.get(thisRankIndex);
                if (thisRankIndex > thisRankMap.size()) {
                    entry.set("rank", thisMaxRankId);
                } else if (thisRankIndex < 1) {
                    entry.set("rank", thisMinRankId);
                } else {
                    entry.set("rank", thisRankId);
                }
            }

            // 中层管理人员：不等于 员工 或 其他
            if (!"其他".equals(zhiJiName) && !"员工".equals(zhiJiName)) {
                switch (lastYearKaoHeResult) {
                    case "优秀":
                        changeRank = 1;
                        break;
                    case "良好":
                        // 连续两年良好
                        if ("良好".equals(lastTwoYearsKaoHeResult)) {
                            changeRank = 1;
                        }
                        break;
                    case "不称职":
                        // 连续三年内累计两次基本称职
                        changeRank = -1;
                        break;
                    case "基本称职":
                        // 连续三年内累计两次基本称职
                        if ("基本称职".equals(lastTwoYearsKaoHeResult) ||
                                "基本称职".equals(lastThreeYearsKaoHeResult)) {
                            changeRank = -1;
                        }
                        break;
                    default:
                        break;
                }
                entry.set("grade", preGrade);
                int thisRankIndex = preRankIndex + changeRank;
                Long thisRankId = thisRankMap.get(thisRankIndex);
                if (thisRankIndex > thisRankMap.size()) {
                    entry.set("rank", thisMaxRankId);
                    // 最低档，次年工资下调10%
                } else if (thisRankIndex <= 1) {
                    entry.set("calctype", 1);
                    entry.set("actualrange", -10.00);
                } else {
                    entry.set("rank", thisRankId);
                }
            }
            // “系统处理备注信息”字段：“上年度绩效考核”&值“，
            entry.set("nckd_notesinfo", "上年度绩效考核：" + lastYearKaoHeResult);
        }
    }

    private Map<String, List<Map<String, String>>> getYearKaoheMap() {
        // 绩效结果
        DynamicObject[] jobExpArray = BusinessDataServiceHelper.load(
                "epa_performanceresult",
                "id,name,number,person,activity,assessleveltext",
                new QFilter[]{
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                        // 非历史绩效
                        new QFilter("number", QCP.not_like, "%HI%"),
                }
        );
        Map<String, List<Map<String, String>>> yearKaoheMap = Arrays.stream(jobExpArray)
                .collect(Collectors.groupingBy(
                        job -> job.getString("person.number"),
                        Collectors.mapping(
                                job -> {
                                    Map<String, String> yearResultMap = new HashMap<>();
                                    yearResultMap.put(
                                            job.getString("activity.periodname"),
                                            job.getString("assessleveltext")
                                    );
                                    return yearResultMap;
                                },
                                Collectors.toList()
                        )
                ));
        return yearKaoheMap;
    }
}
