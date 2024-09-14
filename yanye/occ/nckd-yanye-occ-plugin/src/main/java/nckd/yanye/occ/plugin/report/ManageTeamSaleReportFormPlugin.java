package nckd.yanye.occ.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Iterator;

/**
 * 报表界面插件
 */
public class ManageTeamSaleReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
            //本月销量
            BigDecimal thisYearMonthOutBaseQty = next.getBigDecimal("thisYearMonth_out_baseqty");
            //同期
            BigDecimal lastYearMonthOutBaseQty = next.getBigDecimal("lastYearMonth_out_baseqty");
            if(lastYearMonthOutBaseQty.compareTo(BigDecimal.ZERO) != 0){
                //计算同比 = （本月销量 - 同期）/ 同期
                BigDecimal subtract = thisYearMonthOutBaseQty.subtract(lastYearMonthOutBaseQty);
                BigDecimal divide = subtract.divide(lastYearMonthOutBaseQty, RoundingMode.CEILING);
                next.set("tb",new DecimalFormat("0.00%").format(divide));
            }
            //月度小包盐目标数
            BigDecimal monthHkYdxbzy = next.getBigDecimal("month_hk_ydxbzy");
            if(monthHkYdxbzy.compareTo(BigDecimal.ZERO) != 0){
                //计算月度小包完成 = 本月销量 / 月度小包盐目标数
                BigDecimal divide = thisYearMonthOutBaseQty.divide(monthHkYdxbzy, RoundingMode.CEILING);
                next.set("ydxbwc",new DecimalFormat("0.00%").format(divide));
            }

            //小包盐销量
            BigDecimal thisYearOutBaseqty = next.getBigDecimal("thisYear_out_baseqty");
            //上年累计
            BigDecimal lastYearOutBaseqty = next.getBigDecimal("lastYear_out_baseqty");
            if(lastYearOutBaseqty.compareTo(BigDecimal.ZERO) != 0){
                //计算累计同比 = （小包盐销量 - 上年累计）/ 上年累计
                BigDecimal subtract = thisYearOutBaseqty.subtract(lastYearOutBaseqty);
                BigDecimal divide = subtract.divide(lastYearOutBaseqty, RoundingMode.CEILING);
                next.set("ljtb",new DecimalFormat("0.00%").format(divide));
            }
            //小包盐目标
            BigDecimal yearHkYdxbzy = next.getBigDecimal("year_hk_ydxbzy");
            if(yearHkYdxbzy.compareTo(BigDecimal.ZERO) != 0){
                //计算销量完成率 = 小包盐销量 / 小包盐目标
                BigDecimal divide = thisYearOutBaseqty.divide(yearHkYdxbzy, RoundingMode.CEILING);
                next.set("xlwcl",new DecimalFormat("0.00%").format(divide));
            }
            //获取年度价税合计
            BigDecimal thisYearOutAmountandtax = next.getBigDecimal("thisYear_out_amountandtax");
            if(thisYearOutBaseqty.compareTo(BigDecimal.ZERO) != 0){
                next.set("thisYear_out_amountandtax",thisYearOutAmountandtax.divide(thisYearOutBaseqty, RoundingMode.CEILING));
            }
        }
    }
}