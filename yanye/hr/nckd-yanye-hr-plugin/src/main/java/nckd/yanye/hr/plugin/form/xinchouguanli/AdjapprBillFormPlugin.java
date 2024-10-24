package nckd.yanye.hr.plugin.form.xinchouguanli;

import com.alibaba.fastjson.JSON;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.ValueMapItem;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.ComboProp;
import kd.bos.exception.KDBizException;
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
import kd.swc.hcdm.business.adjapprbill.GradeAndRankNameHelper;
import kd.swc.hcdm.business.helper.DynamicObjectValueHelper;
import kd.swc.hcdm.common.entity.adjapprbill.AmountStdRangeEntity;
import kd.swc.hcdm.common.entity.adjapprbill.GradeAndRankNameEntity;
import kd.swc.hcdm.formplugin.adjapprbill.DecAdjApprFormUtils;
import nckd.base.common.utils.capp.CappConfig;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 员工定调薪申请单-表单插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 *
 * @author ：luxiao
 * @since ：Created in 14:38 2024/9/12
 */
public class AdjapprBillFormPlugin extends AbstractFormPlugin {
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        DynamicObjectCollection entryentity = this.getModel().getEntryEntity("adjapprdetailentry");
        // 调动调薪：锁定薪档
        DynamicObject salaryadjrsn = (DynamicObject) this.getModel().getValue("salaryadjrsn");
        if (salaryadjrsn == null) {
            return;
        }
        String salaryadjrsnName = salaryadjrsn.getString("name");
        // 定调薪范围-调动调薪  组织范围-针对晶昊公司
        DynamicObject org = (DynamicObject) this.getModel().getValue("org");
        String orgNumber = org.getString("number");
        String orgNumberString = CappConfig.getConfigValue("jinghao_orgnumber", "119.01");
        String[] checkOrgNumbers = orgNumberString.split(",");
        if ("调动调薪".equals(salaryadjrsnName) &&
                Arrays.asList(checkOrgNumbers).contains(orgNumber)) {
            for (int rowIndex = 0; rowIndex < entryentity.size(); rowIndex++) {
                DecAdjApprFormUtils.setEnable(this.getView(), Boolean.FALSE, rowIndex, "dy_rank");
            }
        }
    }

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

        DynamicObject salaryadjrsn = (DynamicObject) model.getValue("salaryadjrsn");
        if (salaryadjrsn == null) {
            return;
        }
        // 组织范围：针对晶昊公司
        DynamicObject org = (DynamicObject) this.getModel().getValue("org");
        String orgName = org.getString("name");
        String orgNumber = org.getString("number");
        String orgNumberString = CappConfig.getConfigValue("jinghao_orgnumber", "119.01");
        String[] checkOrgNumbers = orgNumberString.split(",");
        String salaryadjrsnName = salaryadjrsn.getString("name");

        if ("dy_grade".equals(propertyName)) {
            if ("调动调薪".equals(salaryadjrsnName) &&
                    Arrays.asList(checkOrgNumbers).contains(orgNumber)) {
                autoGetRank(model, changeRowIndex);
            }
        }
    }


    @Override
    public void beforeItemClick(BeforeItemClickEvent e) {
        String itemKey = e.getItemKey();
        // 添加所有年度绩效调薪人员
        if ("advconbaritemap".equals(itemKey)) {
            // 组织范围：针对晶昊公司
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            if (org == null) {
                throw new KDBizException("请先填写基本信息中的薪酬管理组织");
            }
            String orgNumber = org.getString("number");
            String orgNumberString = CappConfig.getConfigValue("jinghao_orgnumber", "119.01");
            String[] checkOrgNumbers = orgNumberString.split(",");
            DynamicObject salaryadjrsn = (DynamicObject) this.getModel().getValue("salaryadjrsn");
            if (salaryadjrsn == null) {
                throw new KDBizException("请先填写基本信息中的定调薪类型");
            }

            String salaryadjrsnName = salaryadjrsn.getString("name");

            if ("年度绩效调薪".equals(salaryadjrsnName) &&
                    Arrays.asList(checkOrgNumbers).contains(orgNumber)) {
                // 默认生效日期
                Date defaultEffectiveDate = (Date) this.getModel().getValue("effectivedate");
                if (defaultEffectiveDate == null) {
                    throw new KDBizException("请先填写基本信息中的默认生效日期");
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = sdf.format(defaultEffectiveDate);
                this.getView().getPageCache().put("isYear", dateString);
            } else {
                this.getView().getPageCache().put("isYear", "false");
            }
        }

        // 一键匹配年度调薪薪档
        if ("nckd_onematch".equals(itemKey)) {
            this.getView().showLoading(new LocaleString("请稍后"));
            // 组织范围：针对晶昊公司
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            String orgNumber = org.getString("number");
            String orgNumberString = CappConfig.getConfigValue("jinghao_orgnumber", "119.01");
            String[] checkOrgNumbers = orgNumberString.split(",");
            DynamicObject salaryadjrsn = (DynamicObject) this.getModel().getValue("salaryadjrsn");
            String salaryadjrsnName = salaryadjrsn.getString("name");

            if ("年度绩效调薪".equals(salaryadjrsnName) &&
                    Arrays.asList(checkOrgNumbers).contains(orgNumber)) {
                // 赋值
                try {
                    addAllYearPerson();
                } catch (Exception ex) {
                    this.getView().showLoading(new LocaleString("请稍后"), 1);
                    throw ex;
                }
            }
            this.getView().showLoading(new LocaleString("请稍后"), 1);
        }

        // 一键匹配调动调薪薪档
        if ("nckd_onematch2".equals(itemKey)) {
            this.getView().showLoading(new LocaleString("请稍后"));
            // 组织范围：针对晶昊公司
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            String orgNumber = org.getString("number");
            String orgNumberString = CappConfig.getConfigValue("jinghao_orgnumber", "119.01");
            String[] checkOrgNumbers = orgNumberString.split(",");
            DynamicObject salaryadjrsn = (DynamicObject) this.getModel().getValue("salaryadjrsn");
            String salaryadjrsnName = salaryadjrsn.getString("name");

            if ("调动调薪".equals(salaryadjrsnName) &&
                    Arrays.asList(checkOrgNumbers).contains(orgNumber)) {
                // 赋值
                try {
                    addAllReloadPerson();
                } catch (Exception ex) {
                    this.getView().showLoading(new LocaleString("请稍后"), 1);
                    throw ex;
                }
            }
            this.getView().showLoading(new LocaleString("请稍后"), 1);
        }

    }

    private void addAllReloadPerson() {
        DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("adjapprdetailentry");
        int[] rowIndexes = IntStream.range(0, entryEntity.size()).toArray();
        setAmountStdRangeIfCalTypeIsGradeRank(rowIndexes, this.getView());
    }

    public static void setAmountStdRangeIfCalTypeIsGradeRank(int[] rowIndexes, IFormView billView) {
        IDataModel billModel = billView.getModel();
        Map<Long, Map<Long, Boolean>> itemUseRankFlag = DecAdjApprFormUtils.getItemUseRankFlag(rowIndexes, billModel);
        Map<Integer, Boolean> itemUseRankMap = DecAdjApprFormUtils.checkItemUseRank(rowIndexes, billModel, itemUseRankFlag);
        List<String> personRangeList = new ArrayList(1);
        int[] var6 = rowIndexes;
        int var7 = rowIndexes.length;

        int var8;
        int rowIndex;
        for (var8 = 0; var8 < var7; ++var8) {
            rowIndex = var6[var8];
            DynamicObject billEntryRow = billModel.getEntryRowEntity("adjapprdetailentry", rowIndex);
            String personRange = (String) DynamicObjectValueHelper.dyObjGetValueIfExist(billEntryRow, "dy_salargrel", (String) null);
            if (org.apache.commons.lang.StringUtils.isNotEmpty(personRange)) {
                personRangeList.add(personRange);
            }
        }

        if (CollectionUtils.isNotEmpty(personRangeList)) {
            GradeAndRankNameEntity gradeAndRankNameEntity = GradeAndRankNameHelper.getGradeAndRankNameFromMatchGradeRankRel(personRangeList);
            int[] var17 = rowIndexes;
            var8 = rowIndexes.length;

            for (rowIndex = 0; rowIndex < var8; ++rowIndex) {
                int rowIndex2 = var17[rowIndex];
                DynamicObject billEntryRow = billModel.getEntryRowEntity("adjapprdetailentry", rowIndex2);
                String personRange = (String) DynamicObjectValueHelper.dyObjGetValueIfExist(billEntryRow, "dy_salargrel", (String) null);
                Boolean isUseRank = (Boolean) itemUseRankMap.get(rowIndex2);
                AmountStdRangeEntity amountStdRangeEntity = GradeAndRankNameHelper.convertMatchGradeRankRelToAmountStdRange(personRange, isUseRank, gradeAndRankNameEntity);
                amountStdRangeEntity.getPositionInfo().keySet().stream().findFirst().ifPresent(
                        gradeId -> billModel.setValue("dy_grade", gradeId, rowIndex2)
                );
            }
        }

    }

    private void addAllYearPerson() {
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

        DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("adjapprdetailentry");
        //  根据调薪单默认生效日期，判定年份。取此年份上一年的年度绩效考核成绩为考核成绩
        Calendar calendar = Calendar.getInstance();
        calendar.setTime((Date) this.getModel().getValue("effectivedate"));
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
            int thisRowIndex = entry.getInt("seq") - 1;
            // 人员信息
            DynamicObject person = entry.getDynamicObject("dy_person");
            // 获取此薪档下2档金额和1档金额
            // 本次薪酬标准表
            DynamicObject salarystd = entry.getDynamicObject("dy_salarystd");
            // 上次薪酬标准表
            DynamicObject presalarystd = entry.getDynamicObject("dy_presalarystd");
            if (salarystd == null || presalarystd == null) {
                this.getModel().setValue("dy_nckd_notesinfo", "未作处理：找不到上次薪酬信息", thisRowIndex);
                continue;
            }
            long salaryStdId = salarystd.getLong("id");
            long preSalaryStdId = presalarystd.getLong("id");

            // SPI接口2：获取标准表的薪档信息
            Map<Long, List<Map<String, Object>>> thisResultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(salaryStdId));
            Map<Long, List<Map<String, Object>>> preResultMap = SalaryStdQueryService.get().getRankInfo(Collections.singleton(preSalaryStdId));
            if (thisResultMap == null || thisResultMap.isEmpty()) {
                this.getModel().setValue("dy_nckd_notesinfo", "未作处理：找不到标准表的薪档信息", thisRowIndex);
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
                this.getModel().setValue("dy_nckd_notesinfo", "未作处理：找不到职级信息", thisRowIndex);
                continue;
            }

            // 员工年度考核数据
            List<Map<String, String>> yearKaoheResult = yearKaoheMap.get(person.getString("number"));
            if (yearKaoheResult == null || yearKaoheResult.isEmpty()) {
                this.getModel().setValue("dy_nckd_notesinfo", "未作处理：找不到员工年度考核数据", thisRowIndex);
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
            DynamicObject preGrade = entry.getDynamicObject("dy_pregrade");
            // 薪等对应的索引
            int preGradeIndex = preGrade.getInt("gradeindex");
            // 由于上次和本次的薪酬标准表会不同，把上次的薪等转换为本次的对应薪等id
            Long preGradeId = conversionGradeId(salaryStdId, preGradeIndex);

            // 员工上次薪档
            DynamicObject preRank = entry.getDynamicObject("dy_prerank");
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
                this.getModel().setValue("dy_grade", preGradeId, thisRowIndex);
                int thisRankIndex = preRankIndex + changeRank;
                Long thisRankId = thisRankMap.get(thisRankIndex);
                if (thisRankIndex > thisRankMap.size()) {
                    this.getModel().setValue("dy_rank", thisMaxRankId, thisRowIndex);
                } else if (thisRankIndex < 1) {
                    this.getModel().setValue("dy_rank", thisMinRankId, thisRowIndex);
                } else {
                    this.getModel().setValue("dy_rank", thisRankId, thisRowIndex);
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
                this.getModel().setValue("dy_grade", preGradeId, thisRowIndex);
                int thisRankIndex = preRankIndex + changeRank;
                Long thisRankId = thisRankMap.get(thisRankIndex);
                // 超过最高档，暂按最高档处理
                if (thisRankIndex > thisRankMap.size()) {
                    this.getModel().setValue("dy_rank", thisMaxRankId, thisRowIndex);
                    downMsg = "；调整后超过最高档，按最高档处理";
                    // 最低档，次年工资下调10%，按最低档
                } else if (thisRankIndex < 1) {
                    downMsg = "；调整后低于最低档，工资下调: 10%";
                    // 获取当前薪等的最低档的金额
                    // 根据薪等薪档获取金额
                    List<StdAmountQueryParam> queryParams = new ArrayList<>();
                    queryParams.add(
                            getStdAmountQueryParam(
                                    salaryStdId,
                                    entry.getDynamicObject("dy_standarditem").getLong("id"),
                                    preGradeId,
                                    thisMinRankId,
                                    "this"
                            )
                    );
                    // 查询最低档金额
                    List<StdAmountAndSalaryCountQueryResult> stdAmountAndSalaryCountQueryResults =
                            SalaryStdQueryService.get().queryAmountAndSalaryCount(queryParams);
                    BigDecimal amount = stdAmountAndSalaryCountQueryResults.stream()
                            .filter(result -> "this".equals(result.getUnionId()))
                            .findFirst()
                            .map(StdAmountAndSalaryCountQueryResult::getAmount)
                            .orElse(BigDecimal.ZERO);
                    // 下调10%
                    this.getModel().setValue("dy_amount", amount.multiply(new BigDecimal("0.9")), thisRowIndex);
//                    // 调整方式：比例
//                    this.getModel().setValue("dy_calctype", 1, thisRowIndex);
                    // 薪档：最低档
                    this.getModel().setValue("dy_rank", thisMinRankId, thisRowIndex);
                } else {
                    this.getModel().setValue("dy_rank", thisRankId, thisRowIndex);
                }
            }

            // “系统处理备注信息”字段：“上年度绩效考核”&值“，
            this.getModel().setValue("dy_nckd_notesinfo",
                    "上年度绩效考核：" + lastYearKaoHeResult + downMsg,
                    thisRowIndex
            );
        }
        this.getView().showSuccessNotification("匹配完成");
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
            // 上次薪酬标准表 （并不需要用到上次薪酬标准表）
//            DynamicObject preSalarystd = entry.getDynamicObject("dy_presalarystd");
//            long preSalaryStdId = preSalarystd.getLong("id");

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

            // 定调薪项目id
            long itemId = entry.getDynamicObject("dy_standarditem").getLong("id");
            // 本次薪等id
            long gradeId = grade.getLong("id");
            // 上次薪等id
//            long preGradeId = pregrade.getLong("id");
            // 由于上次和本次的薪酬标准表会不同，把上次的薪等转换为本次的对应薪等id
            long preGradeId = conversionGradeId(salaryStdId, preGradeIndex);


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
                            salaryStdId, itemId, preGradeId, rankId1, "pre1"
                    )
            );
            queryParams.add(
                    getStdAmountQueryParam(
                            salaryStdId, itemId, preGradeId, rankId2, "pre2"
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
                        .findFirst().map(SalaryCountAmountMatchResult::getRankId).orElse(null);

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
                                salaryStdId, itemId, gradeId, finalAmount, rankList, "down", "3", "1"
                        );

                finalRankId = salaryCountAmountMatchResults.stream()
                        .filter(result -> "down".equals(result.getUnionId()))
                        .findFirst().map(SalaryCountAmountMatchResult::getRankId).orElse(null);

                if (finalRankId == null) {
                    Map<Long, List<Pair<Long, Long>>> positionInfo = salaryCountAmountMatchResults.stream()
                            .filter(r -> "down".equals(r.getUnionId()))
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
            DecAdjApprFormUtils.setEnable(this.getView(), Boolean.FALSE, thisRowIndex, "dy_rank");
        }
    }

    static Long conversionGradeId(long salaryStdId, int preGradeIndex) {
        // 本次标准表的薪等信息
        Map<Long, List<Map<String, Object>>> thisGradeInfo = SalaryStdQueryService.get().getGradeInfo(Collections.singleton(salaryStdId));
        // 获取上次标准表中的薪等序号，所对应本次的薪等的id
        Long preGradeId = thisGradeInfo.get(salaryStdId).stream()
                .filter(v -> v.get("gradeIndex").equals(preGradeIndex))
                .findFirst().map(v -> (Long) v.get("gradeId")).orElse(null);
        return preGradeId;
    }

    /**
     * 通过薪点或者金额查询所在标准表的薪等薪档位置
     *
     * @param salaryStdId      标准表id
     * @param itemId           定调薪项目id
     * @param gradeId          查询的范围：薪等id
     * @param finalAmount      查询的金额：标准金额
     * @param rankList         给定薪等薪档范围
     * @param unionId          本次查询标识
     * @param matchStrategy    匹配策略
     * @param isMatchGradeRank 是否显示薪等薪档
     * @return 查询结果
     */
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


    static StdAmountQueryParam getStdAmountQueryParam(Long salaryStdId, Long itemId, Long gradeId, Long rankId1, String unionId) {
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
