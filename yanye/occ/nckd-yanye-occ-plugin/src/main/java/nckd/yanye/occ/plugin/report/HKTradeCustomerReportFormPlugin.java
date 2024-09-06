package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

/**
 * 华康交易客户大表-报表界面插件
 * 表单标识：nckd_hktradecustomer_rpt
 * author:zhangzhilong
 * date:2024/09/03
 */
public class HKTradeCustomerReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给组织默认值
        filter.addFilterItem("nckd_bizorg_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //深井盐销量
            BigDecimal sumsjybaseqty = next.getBigDecimal("sumsjybaseqty") == null ? BigDecimal.ZERO : next.getBigDecimal("sumsjybaseqty");
            //深井盐交易金额
            BigDecimal sumsjyamountandtax = next.getBigDecimal("sumsjyamountandtax") == null ? BigDecimal.ZERO : next.getBigDecimal("sumsjyamountandtax");
            if(sumsjybaseqty.compareTo(BigDecimal.ZERO) == 0) continue;
            //计算深井盐均价 = 深井盐交易金额 / 深井盐销量
            BigDecimal sjyavgprice = sumsjyamountandtax.divide(sumsjybaseqty, RoundingMode.CEILING);
            next.set("sjyavgprice",sjyavgprice);
        }
    }
}