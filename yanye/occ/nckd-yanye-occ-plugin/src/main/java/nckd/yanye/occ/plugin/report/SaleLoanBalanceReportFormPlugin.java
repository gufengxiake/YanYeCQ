package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

/**
 * 业务员借货余额表-报表界面插件
 * 表单标识：nckd_xsjhyeb_rpt
 * author:zhangzhilong
 * date:2024/08/28
 */
public class SaleLoanBalanceReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给发货组织默认值
        filter.addFilterItem("nckd_orgfield_query", curLoginOrg);
    }
}