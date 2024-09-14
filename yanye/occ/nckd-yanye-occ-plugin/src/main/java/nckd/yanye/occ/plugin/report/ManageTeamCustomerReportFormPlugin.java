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
public class ManageTeamCustomerReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
            //月客户完成率
            next.set("monthjywcl",calPercent(next,"month_hk_ydjykh","out_customerMonth"));
            //交易客户完成
            next.set("yearjywcl",calPercent(next,"year_hk_ydjykh","out_customerYear"));
            //月度精品客户完成
            next.set("monthjpkhwcl",calPercent(next,"month_hk_ydjpzdkh","out_isJPMonth"));
            //精品客户完成
            next.set("monthjpkhwcl",calPercent(next,"year_hk_ydjpzdkh","out_isJPYear"));
        }
    }

    //计算百分比 目标/完成数
    public String calPercent(DynamicObject next,String target, String count){
        BigDecimal targetDec = next.getBigDecimal(target);
        BigDecimal countDec = next.getBigDecimal(count);
        if (countDec.compareTo(BigDecimal.ZERO)==0){
            return "0.00%";
        }
        BigDecimal divide = targetDec.divide(countDec, RoundingMode.CEILING);
        return new DecimalFormat("0.00%").format(divide);
    }
}