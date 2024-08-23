package nckd.yanye.hr.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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
        super.processRowData(gridPK, rowData, queryParam);
    }
}
