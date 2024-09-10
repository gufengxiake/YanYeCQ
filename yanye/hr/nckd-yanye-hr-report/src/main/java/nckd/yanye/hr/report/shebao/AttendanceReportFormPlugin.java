package nckd.yanye.hr.report.shebao;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.ksql.util.StringUtil;
import kd.bos.report.plugin.AbstractReportFormPlugin;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 工时假勤云-日常考勤-出差申请单
 * 报表标识：nckd_attendancereport
 * 考勤异常报表查询条件插件
 * @author yaosijie
 * @since 2024-09-04
 */
public class AttendanceReportFormPlugin extends AbstractReportFormPlugin {

    @Override
    public boolean verifyQuery(ReportQueryParam queryParam) {
        //考勤管理组织
        List<FilterItemInfo> orgFilters =  queryParam.getFilter().getFilterItems("nckd_orgfield");
        //异常分类
        List<FilterItemInfo> exceptiontypeFilters =  queryParam.getFilter().getFilterItems("nckd_exceptiontype");
        //自定义日期(开始日期)
        List<FilterItemInfo> startDateFilters =  queryParam.getFilter().getFilterItems("startdate");
        //自定义日期(结束日期)
        List<FilterItemInfo> endDateFilters =  queryParam.getFilter().getFilterItems("enddate");
        //异常日期
        List<FilterItemInfo> exceptiondateFilters =  queryParam.getFilter().getFilterItems("nckd_exceptiondate");
        Object orgId = orgFilters.get(0).getValue() != null ? ((DynamicObject) orgFilters.get(0).getValue()).getPkValue() : null;
        String exceptiontype = exceptiontypeFilters.get(0).getValue() != null ? String.valueOf(exceptiontypeFilters.get(0).getValue()) : null;
        Date startDate = startDateFilters.get(0).getValue() != null ? (Date) startDateFilters.get(0).getValue() : null;
        Date endDate = endDateFilters.get(0).getValue() != null ? (Date) endDateFilters.get(0).getValue() : null;
        String exceptiondata = exceptiondateFilters.get(0).getValue() != null ? String.valueOf(exceptiondateFilters.get(0).getValue()) : null;
        if (Objects.isNull(orgId) || Objects.isNull(exceptiontype) || Objects.isNull(exceptiondata)){
            this.getView().showTipNotification(ResManager.loadKDString("考勤管理组织,异常类型,异常日期都必填",
                    "RecordSumDayListRFPlugin_1",
                    "wtc-wtte-formplugin"));
            return false;
        }else {
            if (StringUtil.equals("D",exceptiondata) &&(Objects.isNull(startDate) || Objects.isNull(endDate))){
                this.getView().showTipNotification(ResManager.loadKDString("异常日期选择自定义，结束日期必填",
                        "RecordSumDayListRFPlugin_1",
                        "wtc-wtte-formplugin"));
                return false;
            }
            return super.verifyQuery(queryParam);
        }

    }
}
