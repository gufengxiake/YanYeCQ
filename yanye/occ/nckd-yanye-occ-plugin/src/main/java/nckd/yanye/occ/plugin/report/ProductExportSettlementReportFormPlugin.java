package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;

/**
 * 产品出口结算表-报表取数插件
 * 表单标识：nckd_productexpsettle_rpt
 * author:zhangzhilong
 * date:2024/09/19
 */
public class ProductExportSettlementReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        //给组织设置一个默认值
        filter.addFilterItem("nckd_org_q", RequestContext.get().getOrgId());

    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        for (DynamicObject row : rowData) {
            //计算途损
            BigDecimal ts = row.getBigDecimal("nckd_outstockqty").subtract(row.getBigDecimal("nckd_signqty"));
            if (ts.compareTo(BigDecimal.ZERO)>0){
                row.set("tusun",ts);
            }
            BigDecimal jshj = row.getBigDecimal("nckd_signqty").multiply(row.getBigDecimal("nckd_xsprice"));
            row.set("jshj",jshj);

        }
    }
}