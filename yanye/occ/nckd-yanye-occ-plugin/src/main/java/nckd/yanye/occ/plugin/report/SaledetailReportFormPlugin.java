package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.EventObject;
import java.util.Iterator;

/**
 * 报表界面插件
 */
public class SaledetailReportFormPlugin extends AbstractReportFormPlugin implements Plugin {

    public void afterCreateNewData(EventObject e) {
        Long curLoginOrg = RequestContext.get().getOrgId();
        this.getModel().setValue("nckd_bizorg_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();
            BigDecimal nckd_amount =  row.getBigDecimal("nckd_amount") == null
                    ? BigDecimal.ZERO :  row.getBigDecimal("nckd_amount");
            if( nckd_amount.compareTo(BigDecimal.ZERO) != 0){
                BigDecimal nckd_mll = BigDecimal.ZERO;
                //计算毛利率 = 金额-结算成本/金额
                nckd_mll = nckd_amount.subtract(row.getBigDecimal("nckd_cbj") == null
                        ? BigDecimal.ZERO : row.getBigDecimal("nckd_cbj"))  ;
                nckd_mll = nckd_mll.divide(nckd_amount);
                DecimalFormat df = new DecimalFormat("0.00%");
                String percent=df.format(nckd_mll);
                row.set("nckd_mll", percent);
            }

        }

    }

}