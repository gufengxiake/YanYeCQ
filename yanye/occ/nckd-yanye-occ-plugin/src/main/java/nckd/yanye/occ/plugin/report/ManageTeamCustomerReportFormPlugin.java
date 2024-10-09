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
 * 经营团队情况一览表（交易客户）-报表界面插件
 * 表单标识：nckd_manageteamcust_rpt
 * author:zhangzhilong
 * date:2024/09/13
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
            next.set("nckd_monthjywcl",calPercent(next,"month_hk_ydjykh","out_customerMonth"));
            //交易客户完成
            next.set("nckd_yearjywcl",calPercent(next,"year_hk_ydjykh","out_customerYear"));
            //月度精品客户完成
            next.set("nckd_monthjpkhwcl",calPercent(next,"month_hk_ydjpzdkh","out_isJPMonth"));
            //精品客户完成
            next.set("nckd_monthjpkhwcl",calPercent(next,"year_hk_ydjpzdkh","out_isJPYear"));
        }
    }

    //计算百分比 目标/完成数
    public String calPercent(DynamicObject next,String target, String count){
        BigDecimal targetDec = next.getBigDecimal(target) == null ? BigDecimal.ZERO : next.getBigDecimal(target);
        BigDecimal countDec = next.getBigDecimal(count) == null ? BigDecimal.ZERO : next.getBigDecimal(count);
        if (targetDec.compareTo(BigDecimal.ZERO)==0){
            return "0.00%";
        }
        double v = countDec.doubleValue() / targetDec.doubleValue();
        return new DecimalFormat("0.00%").format(v);
    }
}