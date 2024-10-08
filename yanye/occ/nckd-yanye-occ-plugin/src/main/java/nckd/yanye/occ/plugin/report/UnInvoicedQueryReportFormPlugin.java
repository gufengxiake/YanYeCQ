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
 * 未开票量查询-报表界面插件
 * 表单标识：nckd_uninvoicedquery_rpt
 * author:zhangzhilong
 * date:2024/10/07
 */
public class UnInvoicedQueryReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        filter.addFilterItem("nckd_org_q",RequestContext.get().getOrgId());
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        for (DynamicObject rowDatum : rowData) {
            //计算累计途损 = 数量 - 签收数量
            BigDecimal tuSun = rowDatum.getBigDecimal("qty").subtract(rowDatum.getBigDecimal("nckd_signqty"));
            rowDatum.set("ljts",tuSun);
            //计算未开票数量 = 基本数量 - 已开票数量
            BigDecimal subtract = rowDatum.getBigDecimal("e_baseunitqty").subtract(rowDatum.getBigDecimal("e_issueinvqty"));
            rowDatum.set("e_baseunitqty",subtract);
        }
    }
}