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
 * 渠道月度销售情况（明细表）-报表界面插件
 * 表单标识：nckd_channelmonthdetail
 * author:zhangzhilong
 * date:2024/09/11
 */
public class ChannelMonthDetailReporFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //获取各个月份的数量和价税合计
            BigDecimal sum = BigDecimal.ZERO, amountandtax = BigDecimal.ZERO;
            for (int i = 1; i < 13 ; i++) {
                BigDecimal sumMonth = next.getBigDecimal("sum"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("sum"+i);
                BigDecimal amountandtaxMonth = next.getBigDecimal("amountandtax"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("amountandtax"+i);
                if (sumMonth.compareTo(BigDecimal.ZERO) != 0) {
                    //用价税合计/数量得出月均单价
                    next.set("amountandtax"+i,amountandtaxMonth.divide(sumMonth, RoundingMode.CEILING));
                }
                sum = sum.add(sumMonth);
                amountandtax = amountandtax.add(amountandtaxMonth);
            }
            //给年度数量设置值
            next.set("yearsum",sum);
            if (sum.compareTo(BigDecimal.ZERO) != 0){
                //计算年均单价
                next.set("yearamountandtax",amountandtax.divide(sum, RoundingMode.CEILING));
            }
        }
    }
}