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
 * 运费结算表-报表界面插件
 * 表单标识：nckd_freightsettlementrpt
 * author:zhangzhilong
 * date:2024/09/18
 */
public class FreightSettlementReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给组织默认值
        filter.addFilterItem("nckd_org_q", curLoginOrg);
        filter.addFilterItem("nckd_freightclass_q","bd_supplier");

    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        for (DynamicObject row : rowData) {
            //计算价税合计 = 客户签收数量*运费
            BigDecimal multiply = row.getBigDecimal("nckd_receiveqty").multiply(row.getBigDecimal("yf"));
            row.set("i_pricetaxtotal",multiply);
        }
    }
}