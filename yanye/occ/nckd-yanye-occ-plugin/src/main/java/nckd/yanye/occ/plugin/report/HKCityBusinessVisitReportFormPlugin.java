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
 * 华康地市公司业务情况（拜访客户）-报表界面插件
 * 表单标识：nckd_hkcitybusivisit_rpt
 * author:zhangzhilong
 * date:2024/10/9
 */
public class HKCityBusinessVisitReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
            //拜访完成率 = 当月拜访/登记客商数
            this.calcPercent(rowDatum,"sumks","sumbf","bfwcl");
            //拜访完成 = 当月拜访/月度拜访目标
            this.calcPercent(rowDatum,"month_hk_ydbfkh","sumbf","bfwc");
            //餐饮月占比 = 当月餐饮/当月拜访
            this.calcPercent(rowDatum,"sumbf","sumcy","monthsumcy");
            //餐饮年占比 = 查询年度拜访客户总数（客户分类为餐饮）/查询年度拜访客户总数
            this.calcPercent(rowDatum,"yearsumbf","yearsumcy","yearsumcy");

            //当月(江盐/中盐/雪天/外盐) = 查询月份，拜访客户在售商品包含(江盐/中盐/雪天/外盐外盐)的客户数/当月拜访
            this.calcPercent(rowDatum,"sumbf","jysumbf","jysumbf");
            this.calcPercent(rowDatum,"sumbf","zysumbf","zysumbf");
            this.calcPercent(rowDatum,"sumbf","xtsumbf","xtsumbf");
            this.calcPercent(rowDatum,"sumbf","wysumbf","wysumbf");

            //餐饮-本月(江盐/中盐/雪天/外盐)占比 = 查询月份，拜访客户在售商品包含(江盐/中盐/雪天/外盐外盐)的客户数/当月拜访
            this.calcPercent(rowDatum,"sumbf","jysumcy","jysumcy");
            this.calcPercent(rowDatum,"sumbf","zysumcy","zysumcy");
            this.calcPercent(rowDatum,"sumbf","xtsumcy","xtsumcy");
            this.calcPercent(rowDatum,"sumbf","wysumcy","wysumcy");

            //餐饮-(江盐/中盐/雪天/外盐)占比 = 查询年度，拜访客户在售商品包含(江盐/中盐/雪天/外盐外盐)的客户数/当月拜访
            this.calcPercent(rowDatum,"yearsumbf","yearjysumcy","yearjysumcy");
            this.calcPercent(rowDatum,"yearsumbf","yearzysumcy","yearzysumcy");
            this.calcPercent(rowDatum,"yearsumbf","yearxtsumcy","yearxtsumcy");
            this.calcPercent(rowDatum,"yearsumbf","yearwysumcy","yearwysumcy");


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