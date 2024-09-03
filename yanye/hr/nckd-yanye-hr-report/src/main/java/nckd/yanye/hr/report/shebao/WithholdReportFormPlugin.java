package nckd.yanye.hr.report.shebao;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;


/**
 * 代扣代缴情况表-报表表单插件
 * 报表标识：nckd_withholdreport
 *
 * @author liuxiao
 * @since 2024-08-22
 */
public class WithholdReportFormPlugin extends AbstractReportFormPlugin {

    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        LocalDate lastDayOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle(
                "sitbs_sinsurperiod",
                "id,perioddate",
                new QFilter[]{new QFilter("perioddate", QCP.equals, lastDayOfMonth)}
        );
        if (dynamicObject == null) {
            throw new KDBizException("未找到当月对应的社保期间，请维护！");
        }
        filter.addFilterItem("nckd_sbksqj", dynamicObject);
    }

    @Override
    public boolean verifyQuery(ReportQueryParam queryParam) {
        return true;
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        // 此方法会进入两次。第一次为空
        if (rowData.isEmpty()) {
            return;
        }

        String[] amountFields = {
                "grjfje1", "dwjfje1", "grbjje1", "dwbjje1", "grhj1", "dwhj1",
                "grjfje2", "dwjfje2", "grbjje2", "dwbjje2", "grhj2", "dwhj2",
                "grjfje3", "dwjfje3", "grbjje3", "dwbjje3", "grhj3", "dwhj3",
                "grjfje4", "dwjfje4", "grbjje4", "dwbjje4", "grhj4", "dwhj4",
                "grjfje5", "dwjfje5", "grbjje5", "dwbjje5", "grhj5", "dwhj5",
                "grjfje6", "dwjfje6", "grbjje6", "dwbjje6", "grhj6", "dwhj6",
                "grjfje7", "dwjfje7", "grbjje7", "dwbjje7", "grhj7", "dwhj7",
                "grjfje8", "dwjfje8", "grbjje8", "dwbjje8", "grhj8", "dwhj8",
        };


        // 实际缴纳单位金额总和
        Map<String, Map<String, BigDecimal>> sjAmountMap = new HashMap<>();
        // 理论缴纳单位金额总和
        Map<String, Map<String, BigDecimal>> llAmountMap = new HashMap<>();


        for (DynamicObject data : rowData) {
            String sjjndw = data.getString("sjjndw");
            String lljndw = data.getString("lljndw");

            // 初始化单位对应的金额总和
            sjAmountMap.putIfAbsent(sjjndw, new HashMap<>());
            llAmountMap.putIfAbsent(lljndw, new HashMap<>());

            // 遍历金额字段，按单位名进行求和
            for (String amountField : amountFields) {
                BigDecimal amount = data.getBigDecimal(amountField);
                sjAmountMap.get(sjjndw).merge(amountField, amount, BigDecimal::add);
                llAmountMap.get(lljndw).merge(amountField, amount, BigDecimal::add);
            }
        }

        // 根据Map中的数据动态生成合计数据
        for (Map.Entry<String, Map<String, BigDecimal>> entry : sjAmountMap.entrySet()) {
            String unitName = entry.getKey();
            Map<String, BigDecimal> amountMap = entry.getValue();

            // 添加实际缴纳单位合计数据
            DynamicObject newRecord2 = rowData.addNew();
            newRecord2.set("lljndw", "实际缴纳单位合计(" + unitName + ")");
            for (String amountField : amountFields) {
                newRecord2.set(amountField, amountMap.getOrDefault(amountField, BigDecimal.ZERO));
            }
        }

        for (Map.Entry<String, Map<String, BigDecimal>> entry : llAmountMap.entrySet()) {
            String unitName = entry.getKey();
            Map<String, BigDecimal> amountMap = entry.getValue();

            // 添加理论缴纳单位合计数据
            DynamicObject newRecord1 = rowData.addNew();
            newRecord1.set("lljndw", "理论缴纳单位合计(" + unitName + ")");
            for (String amountField : amountFields) {
                newRecord1.set(amountField, amountMap.getOrDefault(amountField, BigDecimal.ZERO));
            }
        }
    }


}











