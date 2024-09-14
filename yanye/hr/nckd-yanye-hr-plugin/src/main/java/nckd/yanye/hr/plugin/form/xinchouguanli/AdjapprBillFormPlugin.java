package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.control.RichTextEditor;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.sdk.swc.hcdm.common.Pair;
import kd.sdk.swc.hcdm.common.stdtab.SalaryCountAmountMatchParam;
import kd.sdk.swc.hcdm.common.stdtab.SalaryCountAmountMatchResult;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountAndSalaryCountQueryResult;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountQueryParam;
import kd.sdk.swc.hcdm.service.spi.SalaryStdQueryService;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
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
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        IFormView view = this.getView();

        // 监听字段：本次调薪信息：薪等
        String propertyName = e.getProperty().getName();
        if (!"dy_grade".equals(propertyName)) {
            return;
        }

        // 非调动调薪不作处理
        DynamicObject salaryadjrsn = (DynamicObject) model.getValue("salaryadjrsn");
        if (salaryadjrsn == null) {
            return;
        }
        String salaryadjrsnName = salaryadjrsn.getString("name");
        if (!"调动调薪".equals(salaryadjrsnName)) {
            return;
        }
        // 调薪明细信息
        DynamicObjectCollection entryentity = model.getEntryEntity("adjapprdetailentry");
        for (DynamicObject entry : entryentity) {
            // 本次调薪信息：薪等
            DynamicObject grade = entry.getDynamicObject("dy_grade");
            // 上次薪酬信息：薪等
            DynamicObject pregrade = entry.getDynamicObject("dy_pregrade");
            if (pregrade == null || grade == null) {
                continue;
            }

            /*
             *薪等相同，则薪档为原薪档。
             */
            int gradeIndex = grade.getInt("gradeindex");
            int preGradeIndex = pregrade.getInt("gradeindex");
            if (gradeIndex == preGradeIndex) {
                DynamicObject preRank = entry.getDynamicObject("dy_prerank");
                if (preRank == null) {
                    continue;
                }
                model.setValue("dy_rank", preRank, (entry.getInt("seq") - 1));
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


            // 晋升：取调动后岗位的薪等，取（此薪等下2档金额-1档金额）*2+原金额，得出调整后金额，
            // 再按照就高原则匹配到对应的薪档，系统会根据薪档得到本次调薪后的金额
            // 原金额
            BigDecimal preAmount = entry.getBigDecimal("dy_presalary");
            if (gradeIndex > preGradeIndex) {
                // 最终金额
                BigDecimal finalAmount = this2_this1_amount.multiply(new BigDecimal("2")).add(preAmount);
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
                salaryCountAmountMatchParam.setUnionId("up");
                // 匹配策略:就高
                salaryCountAmountMatchParam.setMatchStrategy("1");
                // 是否显示薪等薪档
                salaryCountAmountMatchParam.setisMatchGradeRank("1");

                List<SalaryCountAmountMatchResult> salaryCountAmountMatchResults = SalaryStdQueryService.get().matchTableRangeBySalaryCountOrAmount(
                        Collections.singletonList(salaryCountAmountMatchParam)
                );

                Long finalRankId;
                finalRankId = salaryCountAmountMatchResults.stream()
                        .filter(result -> "up".equals(result.getUnionId()))
                        .findFirst().map(SalaryCountAmountMatchResult::getGradeId).orElse(null);

                if (finalRankId == null) {
                    Map<Long, List<Pair<Long, Long>>> positionInfo = salaryCountAmountMatchResults.stream().filter(r -> "up".equals(r.getUnionId()))
                            .findFirst().map(SalaryCountAmountMatchResult::getPositionInfo).orElse(null);
                    Pair<Long, Long> longLongPair = positionInfo.get(gradeId).get(0);
                    finalRankId = positionInfoCheck(longLongPair);
                }

                model.setValue("dy_rank", finalRankId, (entry.getInt("seq") - 1));
            }

            // 下降：取（原薪等下2档金额-1档金额）*（-2）+原金额，得出调整后金额，
            // 再按照就低原则匹配到对应的薪档，系统会根据薪档得到本次调薪后的金额。
            if (gradeIndex < preGradeIndex) {
                // 最终金额
                BigDecimal finalAmount = pre2_pre1_amount.multiply(new BigDecimal("-2")).add(preAmount);
                // 查询
                SalaryCountAmountMatchParam salaryCountAmountMatchParam = new SalaryCountAmountMatchParam();
                // 标准表id
                salaryCountAmountMatchParam.setStdTableId(preSalaryStdId);
                // 定调薪项目id
                salaryCountAmountMatchParam.setItemId(itemId);
                // 薪等id：原薪等
                salaryCountAmountMatchParam.setGradeId(preGradeId);
                // 标准金额
                salaryCountAmountMatchParam.setAmount(finalAmount);
                // 给定薪等薪档范围
                HashMap<Long, List<Long>> gradeRankInfoMap = new HashMap<>();
                gradeRankInfoMap.put(preGradeId, preRankList);
                salaryCountAmountMatchParam.setGradeRankInfo(gradeRankInfoMap);
                // unionId
                salaryCountAmountMatchParam.setUnionId("down");
                // 匹配策略:就低
                salaryCountAmountMatchParam.setMatchStrategy("3");
                // 是否显示薪等薪档
                salaryCountAmountMatchParam.setisMatchGradeRank("1");

                List<SalaryCountAmountMatchResult> salaryCountAmountMatchResults = SalaryStdQueryService.get().matchTableRangeBySalaryCountOrAmount(
                        Collections.singletonList(salaryCountAmountMatchParam)
                );

                Long finalRankId;
                finalRankId = salaryCountAmountMatchResults.stream()
                        .filter(result -> "down".equals(result.getUnionId()))
                        .findFirst().map(SalaryCountAmountMatchResult::getGradeId).orElse(null);

                if (finalRankId == null) {
                    Map<Long, List<Pair<Long, Long>>> positionInfo = salaryCountAmountMatchResults.stream().filter(r -> "down".equals(r.getUnionId()))
                            .findFirst().map(SalaryCountAmountMatchResult::getPositionInfo).orElse(null);
                    Pair<Long, Long> longLongPair = positionInfo.get(gradeId).get(0);
                    finalRankId = positionInfoCheck(longLongPair);
                }
                model.setValue("dy_rank", finalRankId, (entry.getInt("seq") - 1));
            }
        }
    }

    private Long positionInfoCheck(Pair<Long, Long> positionInfo) {
        Long minRank = positionInfo.getKey();
        Long maxRank = positionInfo.getValue();
        // ①最低最高等d相同代表传入的值正好等于某个等
        if (minRank.equals(maxRank)) {
            return minRank;
        }
        // ②最低等id为0，最高等id不为0，代表传入的值小于最低等
        if (minRank.equals(0L)) {
            return maxRank;
        }

        // ③最低等id不为0，最高等id为0，代表传入的值高于最高等
        if (maxRank.equals(0L)) {
            return minRank;
        }
        return null;
    }

    @NotNull
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
