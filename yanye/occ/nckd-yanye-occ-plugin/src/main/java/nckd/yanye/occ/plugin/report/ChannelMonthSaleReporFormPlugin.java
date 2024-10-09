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
 * 渠道月度销售情况（汇总表）-报表界面插件
 * 表单标识：nckd_channelmonthsale_rpt
 * author:zhangzhilong
 * date:2024/09/11
 */
public class ChannelMonthSaleReporFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            BigDecimal yearsum = next.getBigDecimal("yearsum") == null ? BigDecimal.ZERO : next.getBigDecimal("yearsum");
            if (yearsum.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal yearamountandtax = next.getBigDecimal("yearamountandtax") == null ? BigDecimal.ZERO : next.getBigDecimal("yearamountandtax");
                next.set("yearamountandtax",yearamountandtax.divide(yearsum, RoundingMode.CEILING));
            }
            for (int i = 1; i < 13 ; i++) {
                BigDecimal sum = next.getBigDecimal("sum"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("sum"+i);
                if (sum.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal amountandtax = next.getBigDecimal("amountandtax"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("amountandtax"+i);
                    next.set("amountandtax"+i,amountandtax.divide(sum, RoundingMode.CEILING));
                }
            }
        }
        
    }
}