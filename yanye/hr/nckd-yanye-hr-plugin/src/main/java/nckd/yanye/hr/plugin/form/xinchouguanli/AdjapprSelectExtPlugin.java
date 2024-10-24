package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.IDecAdjApprExtPlugin;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.event.AfterF7PersonSelectEvent;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountAndSalaryCountQueryResult;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountQueryParam;
import kd.sdk.swc.hcdm.service.spi.SalaryStdQueryService;
import kd.swc.hcdm.formplugin.adjapprbill.DecAdjApprFormUtils;
import nckd.base.common.utils.capp.CappConfig;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
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
            // 组织范围：针对晶昊公司
            DynamicObject org = adjapprBill.getDynamicObject("org");
            String orgNumber = org.getString("number");
            String orgNumberString = CappConfig.getConfigValue("jinghao_orgnumber", "119.01");
            String[] checkOrgNumbers = orgNumberString.split(",");

            String salaryadjrsnName = salaryadjrsn.getString("name");
            // 生效日期
            Date effectivedate = adjapprBill.getDate("effectivedate");
            if ("年度绩效调薪".equals(salaryadjrsnName) &&
                    Arrays.asList(checkOrgNumbers).contains(orgNumber)) {
                addAllYearPerson(adjObj, effectivedate);
            }
        }
    }


    private void addAllYearPerson(DynamicObject adjObj, Date effectivedate) {
        Map<String, List<Map<String, String>>> yearKaoheMap = getYearKaoheMap();

        Map<String, String> jobExpMap = getJobExpMap();

        DynamicObjectCollection entryEntity = adjObj.getDynamicObjectCollection("entryentity");
        //  根据调薪单默认生效日期，判定年份。取此年份上一年的年度绩效考核成绩为考核成绩
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
            // 对象里没有薪等索引，直接用number里的数字代替
            int preGradeIndex = preGrade.getInt("gradeindex");
            // 由于上次和本次的薪酬标准表会不同，把上次的薪等转换为本次的对应薪等id
            Long preGradeId = AdjapprBillFormPlugin.conversionGradeId(salaryStdId, preGradeIndex);

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
                entry.set("grade", preGradeId);
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
            String downMsg = "";
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
                        if ("基本称职".equals(lastTwoYearsKaoHeResult) || "基本称职".equals(lastThreeYearsKaoHeResult)) {
                            changeRank = -1;
                        }
                        break;
                    default:
                        break;
                }

                // 赋值薪等
                entry.set("grade", preGradeId);
                int thisRankIndex = preRankIndex + changeRank;
                Long thisRankId = thisRankMap.get(thisRankIndex);
                // 超过最高档，暂按最高档处理
                if (thisRankIndex > thisRankMap.size()) {
                    entry.set("rank", thisMaxRankId);
                    downMsg = "调整后超过最高档，按最高档处理";
                    // 最低档，次年工资下调10%
                } else if (thisRankIndex <= 1) {
                    downMsg = "调整后低于最低档，工资下调: 10%";
                    entry.set("calctype", 1);
                    entry.set("actualrange", -10.00);
                } else {
                    entry.set("rank", thisRankId);
                }
            }
            // 赋值薪等薪档完毕，开始赋值其他需要的字段
            test(entry);

            // “系统处理备注信息”字段：“上年度绩效考核”&值“，
            entry.set("nckd_notesinfo", "上年度绩效考核：" + lastYearKaoHeResult + downMsg);
        }
    }

    @NotNull
    public static Map<String, String> getJobExpMap() {
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
        return jobExpMap;
    }

    private void test(DynamicObject entry) {
        // 根据薪等薪档获取金额
        List<StdAmountQueryParam> queryParams = new ArrayList<>();
        queryParams.add(
                AdjapprBillFormPlugin.getStdAmountQueryParam(
                        entry.getDynamicObject("salarystd").getLong("id"),
                        entry.getDynamicObject("standarditem").getLong("id"),
                        entry.getLong("grade"),
                        entry.getLong("rank"),
                        "this"
                )
        );
        // 查询
        List<StdAmountAndSalaryCountQueryResult> stdAmountAndSalaryCountQueryResults =
                SalaryStdQueryService.get().queryAmountAndSalaryCount(queryParams);
        BigDecimal amount = stdAmountAndSalaryCountQueryResults.stream()
                .filter(result -> "this".equals(result.getUnionId()))
                .findFirst()
                .map(StdAmountAndSalaryCountQueryResult::getAmount)
                .orElse(BigDecimal.ZERO);

        // 上一次金额
        BigDecimal preSalary = entry.getBigDecimal("presalary");
        // 赋值实际调幅（%）
        entry.set("actualrange", amount.subtract(preSalary)
                .divide(preSalary, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
        );
        // 赋值实际调幅（金额）
        entry.set("actualamount", amount.subtract(preSalary));
        // 赋值金额
        entry.set("amount", amount);
        // 赋值薪酬比例（CR）薪酬比率=金额/(薪酬标准表相应薪等中位值*系数)*100
//        entry.set("salarypercent", 0);
        // 赋值薪酬渗透率（PR）薪酬渗透率=(金额-(薪酬标准表相应薪等区间最小值*系数))/(薪酬标准表相应薪等区间最大值*系数-薪酬标准表相应薪等区间最小值*系数)*100
//        entry.set("salaryseeprate", 0);
    }

    static Map<String, List<Map<String, String>>> getYearKaoheMap() {
        // 绩效结果
        DynamicObject[] jobExpArray = BusinessDataServiceHelper.load(
                "epa_performanceresult",
                "id,person,activity,assessleveltext",
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
