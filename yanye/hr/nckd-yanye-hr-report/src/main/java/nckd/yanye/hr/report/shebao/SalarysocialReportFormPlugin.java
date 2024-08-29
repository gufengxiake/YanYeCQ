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


/**
 * 薪酬社保费用表-报表表单插件
 * 报表标识：nckd_salarysocialreport
 *
 * @author liuxiao
 * @since 2024-08-23
 */
public class SalarysocialReportFormPlugin extends AbstractReportFormPlugin {

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
        super.processRowData(gridPK, rowData, queryParam);
        for (DynamicObject data : rowData) {
            for (int i = 1; i <= 6; i++) {
                BigDecimal sbb = data.getBigDecimal("sbb" + i);
                BigDecimal gzb = data.getBigDecimal("gzb" + i);
                BigDecimal pzjl = data.getBigDecimal("pzjl" + i);
                data.set("ce" + i, sbb.subtract(gzb).subtract(pzjl));
            }
        }
    }
}
