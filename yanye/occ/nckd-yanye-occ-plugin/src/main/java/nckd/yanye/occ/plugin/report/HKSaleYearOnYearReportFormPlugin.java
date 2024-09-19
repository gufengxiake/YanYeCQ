package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateUtil;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * 华康各公司销售同比表（大包食用盐）-报表界面插件
 * 表单标识：
 * author:zhangzhilong
 * date:2024/09/18
 */
public class HKSaleYearOnYearReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        //给年份和日期设置一个默认值
        filter.addFilterItem("nckd_year_q",new Date());
        filter.addFilterItem("nckd_month_q", DateUtil.month(new Date())+1);

    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        String[] groupName = {"小包盐","深井盐","大包食用盐","其它大包盐","非盐"};
        for (DynamicObject e : rowData) {
            BigDecimal allAmount = BigDecimal.ZERO, allML = BigDecimal.ZERO;
            BigDecimal tqAllAmount = BigDecimal.ZERO, tqAllML = BigDecimal.ZERO;
            for (int i = 0; i < groupName.length; i++) {
                BigDecimal thisYearAmount = e.getBigDecimal("thisYearAmount" + groupName[i]);
                BigDecimal lastYearAmount = e.getBigDecimal("lastYearAmount" + groupName[i]);
                //累计计算总销售收入
                allAmount = allAmount.add(thisYearAmount);
                //累计计算同期总收入
                tqAllAmount = tqAllAmount.add(lastYearAmount);
                //计算销售收入同比
                if (lastYearAmount.compareTo(BigDecimal.ZERO) != 0){
                    double v = thisYearAmount.subtract(lastYearAmount).doubleValue() / lastYearAmount.doubleValue();
                    e.set("yearAmountTB" + groupName[i],new DecimalFormat("0.00%").format(v) );
                }
                BigDecimal thisYearML = e.getBigDecimal("thisYearML" + groupName[i]);
                //计算总毛利
                allML = allML.add(thisYearML);
                BigDecimal lastYearML = e.getBigDecimal("lastYearML" + groupName[i]);
                //计算同期总毛利
                tqAllML = tqAllML.add(lastYearML);
                //计算销售毛利同比
                if(lastYearML.compareTo(BigDecimal.ZERO) != 0){
                    double v = thisYearML.subtract(lastYearML).doubleValue() / lastYearML.doubleValue();
                    e.set("yearMLTB" + groupName[i],new DecimalFormat("0.00%").format(v) );
                }

                if (groupName[i].equals("非盐")){
                    continue;
                }
                BigDecimal thisYearQty = e.getBigDecimal("thisYearQty" + groupName[i]);
                BigDecimal lastYearQty = e.getBigDecimal("lastYearQty" + groupName[i]);
                //计算销售量同比
                if(lastYearQty.compareTo(BigDecimal.ZERO) != 0){
                    double v = thisYearQty.subtract(lastYearQty).doubleValue() / lastYearQty.doubleValue();
                    e.set("yearQtyTB" + groupName[i],new DecimalFormat("0.00%").format(v) );
                }

                double thisPrice = 0 , lastPrice = 0;
                //计算今年均价
                if(thisYearQty.compareTo(BigDecimal.ZERO) != 0){
                    thisPrice = thisYearAmount.doubleValue() / thisYearQty.doubleValue();
                    e.set("thisYearPrice" + groupName[i],BigDecimal.valueOf(thisPrice));
                }
                //计算去年均价
                if(lastYearQty.compareTo(BigDecimal.ZERO) != 0){
                    lastPrice = lastYearAmount.doubleValue() / lastYearQty.doubleValue();
                    e.set("lastYearPrice" + groupName[i],BigDecimal.valueOf(lastPrice));
                }
                //计算均价同比
                if (lastPrice != 0){
                    e.set("yearPriceTB" + groupName[i],new DecimalFormat("0.00%").format( (thisPrice - lastPrice)/ lastPrice) );

                }
            }
            e.set("zxssr",allAmount);
            e.set("zml",allML);
            e.set("tqzsr",tqAllAmount);
            e.set("tqml",tqAllML);

        }
    }
}