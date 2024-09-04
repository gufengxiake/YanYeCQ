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
 * 采购结算价格（不含税）-报表界面插件
 * 表单标识：nckd_pursettlenotax_rpt
 * author:zzl
 * date:2024/09/03
 */
public class PurSettleNoTaxReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
            //获取累计结算数量
            BigDecimal sumBaseQty = next.getBigDecimal("sumbaseqty") == null ? BigDecimal.ZERO : next.getBigDecimal("sumbaseqty");
            //获取累计本位币结算金额
            BigDecimal sumAmount = next.getBigDecimal("sumamount") == null ? BigDecimal.ZERO : next.getBigDecimal("sumamount");
            if (sumBaseQty.compareTo(BigDecimal.ZERO) == 0) continue;
            //计算结算无税价 = 累计本位币结算金额/累计结算数量
            BigDecimal noTaxPrice = sumAmount.divide(sumBaseQty, RoundingMode.CEILING);
            next.set("notaxprice",noTaxPrice);

            //计算含税价 = 结算无税价*（1+物料的默认税率）
            BigDecimal nckdTaxrate = next.getBigDecimal("nckd_taxrate").divide(BigDecimal.valueOf(100), RoundingMode.CEILING);
            BigDecimal nckdTaxratenum = noTaxPrice.multiply(BigDecimal.ONE.add(nckdTaxrate));
            next.set("taxprice",nckdTaxratenum);
        }

    }
}