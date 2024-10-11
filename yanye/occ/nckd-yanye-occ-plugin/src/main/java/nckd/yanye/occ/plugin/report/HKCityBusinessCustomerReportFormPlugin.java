package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * 华康地市公司业务情况（交易客户）-报表界面插件
 * 表单标识：nckd_hkcitybusicustom_rpt
 * author:zhangzhilong
 * date:2024/10/10
 */
public class HKCityBusinessCustomerReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        filter.addFilterItem("nckd_org_q", RequestContext.get().getOrgId());
        filter.addFilterItem("nckd_date_q",new Date());
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        for (DynamicObject rowDatum : rowData) {
            //月客户完成率 = 本月客户/月度交易客户数目标
            this.calcPercent(rowDatum,"month_nckd_ydjykh","monthSumCustomer","monthCustomer");
            //交易客户完成 = 交易客户累计/交易客户目标
            this.calcPercent(rowDatum,"year_nckd_ydjykh","yearSumCustomer","yearCustomer");
            //月度精品客户完成 = 本月精品客户/月度精品客户目标
            this.calcPercent(rowDatum,"month_nckd_ydjpzdkh","monthSumJPCustomer","monthJPCustomer");
            //精品客户完成 = 精品客户累计/精品客户目标
            this.calcPercent(rowDatum,"year_nckd_ydjpzdkh","yearSumJPCustomer","yearJPCustomer");
        }
    }
    public void calcPercent(DynamicObject row,String dividend,String divisor,String setName){
        if (row.getInt(dividend) > 0){
            double v = (double) row.getInt(divisor) / row.getInt(dividend);
            row.set(setName,new DecimalFormat("0.00%").format(v));
        }else{
            row.set(setName,"0.00%");
        }
    }
}