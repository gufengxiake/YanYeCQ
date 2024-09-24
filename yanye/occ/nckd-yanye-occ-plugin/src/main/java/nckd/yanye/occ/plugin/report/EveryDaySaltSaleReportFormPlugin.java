package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

/**
 * 每日小包装盐销量-报表界面插件
 * 表单标识：nckd_everydaysaltsale_rpt
 * author:zhangzhilong
 * date:2024/09/24
 */
public class EveryDaySaltSaleReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        filter.addFilterItem("nckd_org_q", RequestContext.get().getOrgId());
    }
}