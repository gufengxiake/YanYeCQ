package nckd.yanye.hr.report.shebao;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Objects;


/**
 * 薪酬社保费用表-报表表单插件
 * 报表标识：nckd_salarysocialreport
 *
 * @author liuxiao
 * @since 2024-08-23
 */
public class SalarysocialReportFormPlugin extends AbstractReportFormPlugin {
    /**
     * 全部险种
     */
    private final String[] PROJECTS = {
            "养老保险",
            "医疗保险",
            "大病医疗保险",
            "失业保险",
            "工伤保险",
            "补充工伤保险",
            "住房公积金",
            "企业年金",
    };

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
            this.getView().showErrorNotification("未找到当月对应的社保期间，请维护！");
        }
        // 社保开始期间：默认当月
        filter.addFilterItem("nckd_sbksqj", dynamicObject);
        // 薪酬开始日期：默认当月
        filter.addFilterItem("nckd_xcksrq", new Date());
    }

    @Override
    public boolean verifyQuery(ReportQueryParam queryParam) {
        return true;
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
//        super.processRowData(gridPK, rowData, queryParam);
//        for (DynamicObject data : rowData) {
//            for (int i = 1; i <= PROJECTS.length; i++) {
//                BigDecimal sbb = data.getBigDecimal("sbb" + i);
//                BigDecimal gzb = data.getBigDecimal("gzb" + i);
//                BigDecimal pzjl = data.getBigDecimal("pzjl" + i);
//                data.set("ce" + i, sbb.subtract(gzb).subtract(pzjl));
//            }
//        }
//
//        DynamicObject total = new DynamicObject(rowData.getDynamicObjectType());
//        total.set("qj", "合计");
//
//        for (DynamicObject data : rowData) {
//            if (StringUtils.isEmpty(data.getString("qj"))) {
//                continue;
//            }
//            for (String field : amountFields) {
//                BigDecimal value = data.getBigDecimal(field);
//                BigDecimal sum = total.getBigDecimal(field).add(value);
//                total.set(field, sum);
//            }
//        }
//
//        rowData.add(total);
    }
}
