package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
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
 * 华康地市公司业务情况（销售同比）-报表界面插件
 * 表单标识：nckd_hkcitybusisal_rpt
 * author:zhangzhilong
 * date:2024/10/11
 */
public class HKCityBusinessSalesReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        filter.addFilterItem("nckd_org_q", RequestContext.get().getOrgId());
        filter.addFilterItem("nckd_date_q", new Date());
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        for (DynamicObject rowDatum : rowData) {
            //同比 =（本月销量-同期）/同期
            this.calcPercent(rowDatum,"XBYLastMonth","XBYThisMonth","xbtb",false);
            // 月度小包完成 = 本月销量/月度小包盐目标
            this.calcPercent(rowDatum,"month_nckd_ydxbzy","XBYThisMonth","ydxbwc",true);
            //去年同比 =（小包盐数量-去年累计）/去年累计
            this.calcPercent(rowDatum,"xbyQtyLastYear","xbyQtyThisYear","qntb",false);
            // 销量完成率 = 小包盐数量/小包盐目标
            this.calcPercent(rowDatum,"year_nckd_ydxbzy","xbyQtyThisYear","xswcl",true);
            // 小包均价 = 截止到查询月份小包盐销售总金额/小包盐销售总数量
            if(rowDatum.getBigDecimal("xbyQtyThisYear").compareTo(BigDecimal.ZERO) != 0){
                double v = rowDatum.getBigDecimal("xbyAmountThisYear").doubleValue() / rowDatum.getBigDecimal("xbyQtyThisYear").doubleValue();
                rowDatum.set("xbyAmountThisYear",v);
            }

            // 竞品盐均价 = 截止到查询月份竞品盐销售金额合计/竞品盐销售数量合计
            if(rowDatum.getBigDecimal("jpyQtyThisYear").compareTo(BigDecimal.ZERO) != 0){
                double v = rowDatum.getBigDecimal("jpyAmountThisYear").doubleValue() / rowDatum.getBigDecimal("jpyQtyThisYear").doubleValue();
                rowDatum.set("jpyAmountThisYear",v);
            }
            // 竞品占比 = 竞品数量/小包盐数量
            this.calcPercent(rowDatum,"xbyQtyThisYear","jpyQtyThisYear","jpzb",true);
            // 竞品同比 = （竞品数量-竞品盐同期）/竞品盐同期
            this.calcPercent(rowDatum,"jpyQtyLastYear","jpyQtyThisYear","jptb",false);

            // 深井盐均价 = 截止到查询月份深井盐销售金额合计/深井盐销售数量合计
            if(rowDatum.getBigDecimal("sjyQtyThisYear").compareTo(BigDecimal.ZERO) != 0){
                double v = rowDatum.getBigDecimal("sjyAmountThisYear").doubleValue() / rowDatum.getBigDecimal("sjyQtyThisYear").doubleValue();
                rowDatum.set("sjyAmountThisYear",v);
            }
            // 深井占比 = 竞品数量/小包盐数量
            this.calcPercent(rowDatum,"xbyQtyThisYear","sjyQtyThisYear","sjzb",true);
            // 深井同比 = （深井盐数量数量-深井盐同期）/深井盐同期
            this.calcPercent(rowDatum,"sjyQtyLastYear","sjyQtyThisYear","sjtb",false);

            // 高端盐均价 = 截止到查询月份高端盐销售金额合计/高端盐销售数量合计
            if(rowDatum.getBigDecimal("gdyQtyThisYear").compareTo(BigDecimal.ZERO) != 0){
                double v = rowDatum.getBigDecimal("gdyAmountThisYear").doubleValue() / rowDatum.getBigDecimal("gdyQtyThisYear").doubleValue();
                rowDatum.set("gdyAmountThisYear",v);
            }
            // 高端占比 = 竞品数量/小包盐数量
            this.calcPercent(rowDatum,"xbyQtyThisYear","gdyQtyThisYear","gdzb",true);
            // 高端同比 = （高端盐数量数量-高端盐同期）/高端盐同期
            this.calcPercent(rowDatum,"gdyQtyLastYear","gdyQtyThisYear","gdtb",false);

            // 高端收入同比 = （高端产品收入-同期高端产品收入）/同期高端产品收入
            this.calcPercent(rowDatum,"gdcpAmountLastYear","gdcpAmountThisYear","gdsrtb",false);
            // 高端数量同比 = （高端产品数量-高端产品数量同期）/高端产品数量同期
            this.calcPercent(rowDatum,"gdcpQtyLastYear","gdcpQtyThisYear","gdsltb",false);
            // 果蔬盐同比 = （果蔬盐销量-果蔬盐销量（上））/果蔬盐销量（上）
            this.calcPercent(rowDatum,"gsyQtyLastYear","gsyQtyThisYear","gsytb",false);
            // 小苏打同比 = （小苏打销量-小苏打销量（上））/小苏打销量（上）
            this.calcPercent(rowDatum,"xsdQtyLastYear","xsdQtyThisYear","xsdtb",false);
            // 深海盐同比 = （深海盐销量-深海盐销量（上））/深海盐销量（上）
            this.calcPercent(rowDatum,"shyQtyLastYear","shyQtyThisYear","shytb",false);
            // 晶粒盐同比 = （晶粒盐销量-晶粒盐销量（上））/晶粒盐销量（上）
            this.calcPercent(rowDatum,"jlyQtyLastYear","jlyQtyThisYear","jlytb",false);
        }
    }

    public void calcPercent(DynamicObject row, String dividend, String divisor, String setName, Boolean flag) {
        if (row.getBigDecimal(dividend).compareTo(BigDecimal.ZERO) != 0 && flag) {
            double v = row.getBigDecimal(divisor).doubleValue() / row.getBigDecimal(dividend).doubleValue();
            row.set(setName, new DecimalFormat("0.00%").format(v));
        } else if(row.getBigDecimal(dividend).compareTo(BigDecimal.ZERO) != 0 && !flag){
            double v = (row.getBigDecimal(divisor).subtract(row.getBigDecimal(dividend))).doubleValue() / row.getBigDecimal(dividend).doubleValue();
            row.set(setName, new DecimalFormat("0.00%").format(v));
        }else {
            row.set(setName, "0.00%");
        }
    }
}