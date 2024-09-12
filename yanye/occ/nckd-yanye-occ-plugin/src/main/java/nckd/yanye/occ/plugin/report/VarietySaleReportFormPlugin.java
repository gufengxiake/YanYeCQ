package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateUtil;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Iterator;

/**
 * 报表界面插件
 */
public class VarietySaleReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给组织默认值
        filter.addFilterItem("nckd_org_q", curLoginOrg);
        //给年份默认值
        filter.addFilterItem("nckd_date_q",new Date());
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //获取查询年份的各个月份的数量与价税合计之和
            BigDecimal thisQty = BigDecimal.ZERO, thisAmount = BigDecimal.ZERO;
            //获取查询年份去年的各个月份的数量与价税合计之和
            BigDecimal lastQty = BigDecimal.ZERO, lastAmount = BigDecimal.ZERO;
            for (int i = 1; i < 13 ; i++) {
                BigDecimal thisQtyMonth = next.getBigDecimal("thisQty"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("thisQty"+i);
                BigDecimal thisAmountMonth = next.getBigDecimal("thisAmount"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("thisAmount"+i);
                if (thisQtyMonth.compareTo(BigDecimal.ZERO) != 0) {
                    //用价税合计/数量得出月均单价
                    next.set("thisQty"+i,thisAmountMonth.divide(thisQtyMonth, RoundingMode.CEILING));
                }
                //查询年份数量和价税合计
                thisQty = thisQty.add(thisQtyMonth);
                thisAmount = thisAmount.add(thisAmountMonth);

                //获取查询年份去年的月份数量与价税合计之和
                BigDecimal lastQtyMonth = next.getBigDecimal("lastQty"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("lastQty"+i);
                BigDecimal lastAmountMonth = next.getBigDecimal("lastAmount"+i) == null ? BigDecimal.ZERO : next.getBigDecimal("lastAmount"+i);
                if(lastQtyMonth.compareTo(BigDecimal.ZERO) != 0) {
                    //用价税合计/数量得出月均单价
                    next.set("lastQty"+i,lastAmountMonth.divide(lastQtyMonth, RoundingMode.CEILING));
                }
                //查询年份前一年的数量和价税合计
                lastQty = lastQty.add(lastQtyMonth);
                lastAmount = lastAmount.add(lastAmountMonth);

            }
            //给查询年度金额设置值
            next.set("yearAmount",thisAmount);
            //给查询年度之前一年金额设置值
            next.set("lastAmount",lastAmount);
            if (thisQty.compareTo(BigDecimal.ZERO) != 0){
                //计算查询年份年均单价
                next.set("yearPrice",thisAmount.divide(thisQty, RoundingMode.CEILING));
            }
            if (lastQty.compareTo(BigDecimal.ZERO) != 0){
                //计算查询年份之前一年的年均单价
                next.set("lastPrice",lastAmount.divide(lastQty, RoundingMode.CEILING));
            }
        }

    }
}