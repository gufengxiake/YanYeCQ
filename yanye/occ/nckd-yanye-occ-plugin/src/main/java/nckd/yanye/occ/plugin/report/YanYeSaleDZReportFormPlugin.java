package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 盐类产品内部销售对账表-界面处理插件
 * 表单标识：nckd_ylcpnbxsdz_rpt
 * author:zzl
 * date:2024/08/27
 */
public class YanYeSaleDZReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给发货组织默认值
        filter.addFilterItem("nckd_saleorgid_q", curLoginOrg);
    }
}