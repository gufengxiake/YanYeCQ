package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;
import kd.sdk.swc.hcdm.common.Pair;
import kd.sdk.swc.hcdm.common.stdtab.SalaryCountAmountMatchParam;
import kd.sdk.swc.hcdm.common.stdtab.SalaryCountAmountMatchResult;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountAndSalaryCountQueryResult;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountQueryParam;
import kd.sdk.swc.hcdm.service.spi.SalaryStdQueryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工定调薪申请单-表单插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 *
 * @author ：luxiao
 * @since ：Created in 14:38 2024/9/12
 */
public class AdjapprBillFormPlugin extends AbstractFormPlugin {
    @Override
    public void registerListener(EventObject event) {
        super.registerListener(event);
        this.addItemClickListeners("advcontoolbarap");
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        IFormView view = this.getView();

        ChangeData changeData = e.getChangeSet()[0];
        int changeRowIndex = changeData.getRowIndex();

        // 监听字段：本次调薪信息：薪等
        String propertyName = e.getProperty().getName();
        if (!"dy_grade".equals(propertyName)) {
            return;
        }
        DynamicObject salaryadjrsn = (DynamicObject) model.getValue("salaryadjrsn");
        if (salaryadjrsn == null) {
            return;
        }
        String salaryadjrsnName = salaryadjrsn.getString("name");
        // 需求：调动调薪
        if ("调动调薪".equals(salaryadjrsnName)) {
            autoGetRank(model, changeRowIndex);
        }
    }


    @Override
    public void beforeItemClick(BeforeItemClickEvent e) {
        String itemKey = e.getItemKey();
        // 添加所有年度绩效调薪人员
        if ("advconbaritemap".equals(itemKey)) {
            // 组织范围：针对晶昊公司和富达公司
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            String orgName = org.getString("name");
            DynamicObject salaryadjrsn = (DynamicObject) this.getModel().getValue("salaryadjrsn");
            String salaryadjrsnName = salaryadjrsn.getString("name");

            if ("年度绩效调薪".equals(salaryadjrsnName) &&
                    ("晶昊本部".equals(orgName) || "江西富达盐化有限公司".equals(orgName))) {
                this.getView().getPageCache().put("isYear", "true");
            } else {
                this.getView().getPageCache().put("isYear", "false");
            }
        }
    }


    private void addAllYearPerson() {
        // 获取所有年度绩效Map <员工id, <考核年度, 考核结果>>
        DynamicObject[] yearKaoheArray = BusinessDataServiceHelper.load(
                "nckd_hspm_yearkaohe",
                "person,nckd_kaoheyear,nckd_kaoheresult,nckd_pingjiaorg,nckd_wcjreason",
                new QFilter[]{
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );
        Map<Long, Map<String, String>> yearKaoheMap = Arrays.stream(yearKaoheArray)
                .collect(Collectors.groupingBy(
                        obj -> obj.getLong("person.id"),
                        Collectors.toMap(
                                obj -> obj.getString("nckd_kaoheyear"),
                                obj -> obj.getString("nckd_kaoheresult.name") == null ? "" : obj.getString("nckd_kaoheresult.name")
                        )
                ));

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
        Map<Long, String> jobExpMap = Arrays.stream(jobExpArray)
                .collect(Collectors.toMap(
                        obj -> obj.getLong("person.id"),
                        obj -> obj.getString("nckd_zhiji.name") == null ? "" : obj.getString("nckd_zhiji.name")
                ));


        IDataModel model = this.getModel();
        IFormView view = this.getView();
//            // 清空调薪明细
//            int delete = DeleteServiceHelper.delete(
//                    "hcdm_adjapprperson",
//                    new QFilter[]{
//                            new QFilter("adjapprbill", QCP.equals, model.getDataEntity().getLong("id"))
//                    }
//            );
//            this.getView().updateView();

        // fixme 假设已批量添加成功
        DynamicObjectCollection entryEntity = model.getEntryEntity("adjapprdetailentry");
        //  根据调薪单据日期，判定年份。取此年份上一年的年度绩效考核成绩为考核成绩
        Date effectivedate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(effectivedate);
        // 上一年
        calendar.add(Calendar.YEAR, -1);
        String lastYearDateString = new SimpleDateFormat("yyyy").format(calendar.getTime());

        // 上上年
        calendar.add(Calendar.YEAR, -1);
        String lastTwoYearsDateString = new SimpleDateFormat("yyyy").format(calendar.getTime());

        // 上上上年
        calendar.add(Calendar.YEAR, -1);
        String lastThreeYearsDateString = new SimpleDateFormat("yyyy").format(calendar.getTime());
        for (DynamicObject entry : entryEntity) {
            int thisRowIndex = entry.getInt("seq") - 1;
            DynamicObject person = entry.getDynamicObject("dy_person");

            // 获取此薪档下2档金额和1档金额
            // 本次薪酬标准表
            long salaryStdId = entry.getDynamicObject("dy_salarystd").getLong("id");
            // 上次薪酬标准表
            long preSalaryStdId = entry.getDynamicObject("dy_presalarystd").getLong("id");

            // SPI接口2：获取标准表的薪档信息
            Map<Long, List<Map<String, Object>>> thisResultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(salaryStdId));
            Map<Long, List<Map<String, Object>>> preResultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(preSalaryStdId));
            if (thisResultMap == null || thisResultMap.isEmpty()) {
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
            String zhiJiName = jobExpMap.get(person.getLong("id"));
            if (StringUtils.isEmpty(zhiJiName)) {
                continue;
            }

            // 员工年度考核数据
            Map<String, String> yearKaoheResult = yearKaoheMap.get(person.getLong("id"));
            if (yearKaoheResult == null || yearKaoheResult.isEmpty()) {
                continue;
            }
            // 员工上一年的年度绩效考核成绩
            String lastYearKaoHeResult = yearKaoheResult.get(lastYearDateString);
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
                model.setValue("dy_grade", preGrade, thisRowIndex);
                int thisRankIndex = preRankIndex + changeRank;
                Long thisRankId = thisRankMap.get(thisRankIndex);
                if (thisRankIndex > thisRankMap.size()) {
                    model.setValue("dy_rank", thisMaxRankId, thisRowIndex);
                } else if (thisRankIndex < 1) {
                    model.setValue("dy_rank", thisMinRankId, thisRowIndex);
                } else {
                    model.setValue("dy_rank", thisRankId, thisRowIndex);
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
                        if ("良好".equals(yearKaoheResult.get(lastTwoYearsDateString))) {
                            changeRank = 1;
                        }
                        break;
                    case "不称职":
                        // 连续三年内累计两次基本称职
                        changeRank = -1;
                        break;
                    case "基本称职":
                        // 连续三年内累计两次基本称职
                        if ("基本称职".equals(yearKaoheResult.get(lastThreeYearsDateString)) ||
                                "基本称职".equals(yearKaoheResult.get(lastTwoYearsDateString))) {
                            changeRank = -1;
                        }
                        break;
                    default:
                        break;
                }
                model.setValue("dy_grade", preGrade, thisRowIndex);
                int thisRankIndex = preRankIndex + changeRank;
                Long thisRankId = thisRankMap.get(thisRankIndex);
                if (thisRankIndex > thisRankMap.size()) {
                    model.setValue("dy_rank", thisMaxRankId, thisRowIndex);
                    // 最低档，次年工资下调10%
                } else if (thisRankIndex <= 1) {
                    model.setValue("dy_calctype", 1, thisRowIndex);
                    model.setValue("dy_actualrange", -10.00, thisRowIndex);
                } else {
                    model.setValue("dy_rank", thisRankId, thisRowIndex);
                }
            }


            // “系统处理备注信息”字段：“上年度绩效考核”&值“，
            model.setValue("dy_nckd_notesinfo", "上年度绩效考核：" + lastYearKaoHeResult, thisRowIndex);
        }
    }


    /**
     * 调动调薪：自动生成薪档
     *
     * @param model
     * @param changeRowIndex
     */
    private void autoGetRank(IDataModel model, int changeRowIndex) {
        // 调薪明细信息
        DynamicObjectCollection entryentity = model.getEntryEntity("adjapprdetailentry");
        for (DynamicObject entry : entryentity) {
            int thisRowIndex = entry.getInt("seq") - 1;
            if (changeRowIndex != thisRowIndex) {
                continue;
            }
            // 本次调薪信息：薪等
            DynamicObject grade = entry.getDynamicObject("dy_grade");
            // 上次薪酬信息：薪等
            DynamicObject pregrade = entry.getDynamicObject("dy_pregrade");
            if (pregrade == null || grade == null) {
                continue;
            }

            /*
             * 薪等相同，则薪档为原薪档。
             */
            int gradeIndex = grade.getInt("gradeindex");
            int preGradeIndex = pregrade.getInt("gradeindex");
            if (gradeIndex == preGradeIndex) {
                DynamicObject preRank = entry.getDynamicObject("dy_prerank");
                if (preRank == null) {
                    continue;
                }
                model.setValue("dy_rank", preRank, thisRowIndex);
                String notesInfo = "薪档差金额为:" + "薪等不变" + "，调整后金额:" + "薪等不变";
                model.setValue("dy_nckd_notesinfo", notesInfo, thisRowIndex);
                continue;
            }

            // 获取此薪档下2档金额和1档金额
            // 本次薪酬标准表
            DynamicObject salarystd = entry.getDynamicObject("dy_salarystd");
            long salaryStdId = salarystd.getLong("id");
            // 上次薪酬标准表
            DynamicObject preSalarystd = entry.getDynamicObject("dy_presalarystd");
            long preSalaryStdId = preSalarystd.getLong("id");

            // SPI接口2：获取标准表的薪档信息
            Map<Long, List<Map<String, Object>>> resultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(salaryStdId));
            if (resultMap == null || resultMap.isEmpty()) {
                continue;
            }
            List<Map<String, Object>> rankMap = resultMap.get(salaryStdId);

            // 本次1档id和2档id，所有档
            Long rankId1 = rankMap.stream()
                    .filter(map -> (int) map.get("rankIndex") == 1)
                    .map(map -> (Long) map.get("rankId"))
                    .findFirst()
                    .orElse(null);

            Long rankId2 = rankMap.stream()
                    .filter(map -> (int) map.get("rankIndex") == 2)
                    .map(map -> (Long) map.get("rankId"))
                    .findFirst()
                    .orElse(null);

            List<Long> rankList = rankMap.stream()
                    .map(map -> (Long) map.get("rankId"))
                    .collect(Collectors.toList());


            // 上次1档id和2档id，所有档
            Map<Long, List<Map<String, Object>>> preResultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(preSalaryStdId));
            if (preResultMap == null || preResultMap.isEmpty()) {
                continue;
            }
            List<Map<String, Object>> preRankMap = preResultMap.get(preSalaryStdId);

            Long preRankId1 = preRankMap.stream()
                    .filter(map -> (int) map.get("rankIndex") == 1)
                    .map(map -> (Long) map.get("rankId"))
                    .findFirst()
                    .orElse(null);

            Long preRankId2 = preRankMap.stream()
                    .filter(map -> (int) map.get("rankIndex") == 2)
                    .map(map -> (Long) map.get("rankId"))
                    .findFirst()
                    .orElse(null);

            List<Long> preRankList = preRankMap.stream()
                    .map(map -> (Long) map.get("rankId"))
                    .collect(Collectors.toList());

            // 定调薪项目id
            long itemId = entry.getDynamicObject("dy_standarditem").getLong("id");
            // 本次薪等id
            long gradeId = grade.getLong("id");
            // 上次薪等id
            long preGradeId = pregrade.getLong("id");


            // 获取金额
            List<StdAmountQueryParam> queryParams = new ArrayList<>();
            queryParams.add(
                    getStdAmountQueryParam(
                            salaryStdId, itemId, gradeId, rankId1, "this1"
                    )
            );
            queryParams.add(
                    getStdAmountQueryParam(
                            salaryStdId, itemId, gradeId, rankId2, "this2"
                    )
            );
            queryParams.add(
                    getStdAmountQueryParam(
                            preSalaryStdId, itemId, preGradeId, preRankId1, "pre1"
                    )
            );
            queryParams.add(
                    getStdAmountQueryParam(
                            preSalaryStdId, itemId, preGradeId, preRankId2, "pre2"
                    )
            );
            // 查询
            List<StdAmountAndSalaryCountQueryResult> stdAmountAndSalaryCountQueryResults =
                    SalaryStdQueryService.get().queryAmountAndSalaryCount(queryParams);

            BigDecimal this2_this1_amount = stdAmountAndSalaryCountQueryResults.stream()
                    .filter(result -> "this2".equals(result.getUnionId()))
                    .findFirst()
                    .map(this2 -> stdAmountAndSalaryCountQueryResults.stream()
                            .filter(result -> "this1".equals(result.getUnionId()))
                            .findFirst()
                            .map(this1 -> this2.getAmount().subtract(this1.getAmount()))
                            .orElse(BigDecimal.ZERO))
                    .orElse(BigDecimal.ZERO);

            BigDecimal pre2_pre1_amount = stdAmountAndSalaryCountQueryResults.stream()
                    .filter(result -> "pre2".equals(result.getUnionId()))
                    .findFirst()
                    .map(pre2 -> stdAmountAndSalaryCountQueryResults.stream()
                            .filter(result -> "pre1".equals(result.getUnionId()))
                            .findFirst()
                            .map(pre1 -> pre2.getAmount().subtract(pre1.getAmount()))
                            .orElse(BigDecimal.ZERO))
                    .orElse(BigDecimal.ZERO);
            // 最终的rankId
            Long finalRankId = null;
            // 调整后金额
            BigDecimal finalAmount = null;
            // 薪档差金额
            BigDecimal rankDiffAmount = null;
            // 晋升：取调动后岗位的薪等，取（此薪等下2档金额-1档金额）*2+原金额，得出调整后金额，
            // 再按照就高原则匹配到对应的薪档，系统会根据薪档得到本次调薪后的金额
            // 原金额
            BigDecimal preAmount = entry.getBigDecimal("dy_presalary");
            if (gradeIndex > preGradeIndex) {
                // 薪档差金额
                rankDiffAmount = this2_this1_amount;
                // 调整后金额
                finalAmount = this2_this1_amount.multiply(new BigDecimal("2")).add(preAmount)
                        .setScale(2, RoundingMode.HALF_UP);
                String unionId = "up";
                String matchStrategy = "1";
                String isMatchGradeRank = "1";
                List<SalaryCountAmountMatchResult> salaryCountAmountMatchResults =
                        getSalaryCountAmountMatchResults(
                                salaryStdId, itemId, gradeId, finalAmount, rankList, unionId, matchStrategy, isMatchGradeRank
                        );

                finalRankId = salaryCountAmountMatchResults.stream()
                        .filter(result -> "up".equals(result.getUnionId()))
                        .findFirst().map(SalaryCountAmountMatchResult::getGradeId).orElse(null);

                if (finalRankId == null) {
                    Map<Long, List<Pair<Long, Long>>> positionInfo = salaryCountAmountMatchResults.stream().filter(r -> "up".equals(r.getUnionId()))
                            .findFirst().map(SalaryCountAmountMatchResult::getPositionInfo).orElse(null);
                    Pair<Long, Long> longLongPair = positionInfo.get(gradeId).get(0);
                    finalRankId = positionInfoCheck(longLongPair);
                }
            }

            // 下降：取（原薪等下2档金额-1档金额）*（-2）+原金额，得出调整后金额，
            // 再按照就低原则匹配到对应的薪档，系统会根据薪档得到本次调薪后的金额。
            if (gradeIndex < preGradeIndex) {
                // 薪档差金额
                rankDiffAmount = pre2_pre1_amount;
                // 调整后金额
                finalAmount = pre2_pre1_amount.multiply(new BigDecimal("-2")).add(preAmount)
                        .setScale(2, RoundingMode.HALF_UP);
                // 查询
                List<SalaryCountAmountMatchResult> salaryCountAmountMatchResults =
                        getSalaryCountAmountMatchResults(
                                preSalaryStdId, itemId, preGradeId, finalAmount, preRankList, "down", "3", "1"
                        );

                finalRankId = salaryCountAmountMatchResults.stream()
                        .filter(result -> "down".equals(result.getUnionId()))
                        .findFirst().map(SalaryCountAmountMatchResult::getGradeId).orElse(null);

                if (finalRankId == null) {
                    Map<Long, List<Pair<Long, Long>>> positionInfo = salaryCountAmountMatchResults.stream().filter(r -> "down".equals(r.getUnionId()))
                            .findFirst().map(SalaryCountAmountMatchResult::getPositionInfo).orElse(null);
                    Pair<Long, Long> longLongPair = positionInfo.get(gradeId).get(0);
                    finalRankId = positionInfoCheck(longLongPair);
                }
            }
            // 设置最终的薪档
            model.setValue("dy_rank", finalRankId, thisRowIndex);
            // 设置“系统处理备注信息”字段：“薪档差为”&金额“，调整后金额”&金额
            String notesInfo = "薪档差金额为:" + rankDiffAmount + "，调整后金额:" + finalAmount;
            model.setValue("dy_nckd_notesinfo", notesInfo, thisRowIndex);
        }
    }


    private List<SalaryCountAmountMatchResult> getSalaryCountAmountMatchResults(long salaryStdId, long itemId, long gradeId, BigDecimal finalAmount, List<Long> rankList, String unionId, String matchStrategy, String isMatchGradeRank) {
        // 查询
        SalaryCountAmountMatchParam salaryCountAmountMatchParam = new SalaryCountAmountMatchParam();
        // 标准表id
        salaryCountAmountMatchParam.setStdTableId(salaryStdId);
        // 定调薪项目id
        salaryCountAmountMatchParam.setItemId(itemId);
        // 薪等id：本次薪等
        salaryCountAmountMatchParam.setGradeId(gradeId);
        // 标准金额
        salaryCountAmountMatchParam.setAmount(finalAmount);
        // 给定薪等薪档范围
        HashMap<Long, List<Long>> gradeRankInfoMap = new HashMap<>();
        gradeRankInfoMap.put(gradeId, rankList);
        salaryCountAmountMatchParam.setGradeRankInfo(gradeRankInfoMap);
        // unionId
        salaryCountAmountMatchParam.setUnionId(unionId);
        // 匹配策略:就高
        salaryCountAmountMatchParam.setMatchStrategy(matchStrategy);
        // 是否显示薪等薪档
        salaryCountAmountMatchParam.setisMatchGradeRank(isMatchGradeRank);

        return SalaryStdQueryService.get().matchTableRangeBySalaryCountOrAmount(
                Collections.singletonList(salaryCountAmountMatchParam)
        );
    }

    private Long positionInfoCheck(Pair<Long, Long> positionInfo) {
        Long minRank = positionInfo.getKey();
        Long maxRank = positionInfo.getValue();
        // 当最低等级和最高等级相同，或者其中一个为0时，返回非0的等级
        if (minRank.equals(maxRank) || minRank.equals(0L) || maxRank.equals(0L)) {
            return minRank.equals(0L) ? maxRank : minRank;
        }
        return null;
    }


    private StdAmountQueryParam getStdAmountQueryParam(Long salaryStdId, Long itemId, Long gradeId, Long rankId1, String unionId) {
        // 构建查询参数
        StdAmountQueryParam queryParam = new StdAmountQueryParam();
        // 对应薪酬标准表id
        queryParam.setStdTabId(salaryStdId);
        // 定调薪项目id
        queryParam.setItemId(itemId);
        // 薪等id
        queryParam.setGradeId(gradeId);
        // 薪档id
        queryParam.setRankId(rankId1);
        // UnionId 代表这组参数的唯一标识，通过该参数将入参和返回值对应起来，必传
        queryParam.setUnionId(unionId);
        return queryParam;
    }
}
