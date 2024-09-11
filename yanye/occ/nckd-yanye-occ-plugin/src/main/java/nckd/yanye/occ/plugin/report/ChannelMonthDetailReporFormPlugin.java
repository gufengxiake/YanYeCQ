package nckd.yanye.occ.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

/**
 * 报表界面插件
 */
public class ChannelMonthDetailReporFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            BigDecimal sum = BigDecimal.ZERO, amountandtax = BigDecimal.ZERO;
            for (int i = 1; i < 13 ; i++) {
                BigDecimal sumMonth = next.getBigDecimal("sum"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("sum"+i);
                BigDecimal amountandtaxMonth = next.getBigDecimal("amountandtax"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("amountandtax"+i);
                if (sumMonth.compareTo(BigDecimal.ZERO) != 0) {
                    next.set("amountandtax"+i,amountandtaxMonth.divide(sumMonth, RoundingMode.CEILING));
                }
                sum = sum.add(sumMonth);
                amountandtax = amountandtax.add(amountandtaxMonth);
            }
            next.set("yearsum",sum);
            if (sum.compareTo(BigDecimal.ZERO) != 0){
                next.set("yearamountandtax",amountandtax.divide(sum, RoundingMode.CEILING));
            }
        }
    }
}