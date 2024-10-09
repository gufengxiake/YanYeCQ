package nckd.yanye.occ.plugin.report;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

/**
 * 华康地市公司业务情况（拜访客户）-报表界面插件
 * 表单标识：nckd_hkcitybusivisit_rpt
 * author:zhangzhilong
 * date:2024/10/9
 */
public class HKCityBusinessVisitReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
    }
}