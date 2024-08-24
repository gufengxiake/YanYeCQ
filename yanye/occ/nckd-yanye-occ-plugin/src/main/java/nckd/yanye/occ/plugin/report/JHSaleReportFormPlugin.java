package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.EventObject;
import java.util.Iterator;
import java.util.Map;

/**
 * 晶昊销售统计表界面插件
 * 表单标识：nckd_jhxstj_rpt
 * author:zzl
 * date:2024/08/24
 */
public class JHSaleReportFormPlugin extends AbstractReportFormPlugin implements Plugin {

    public void afterCreateNewData(EventObject e) {
        Long curLoginOrg = RequestContext.get().getOrgId();
        this.getModel().setValue("nckd_bizorg_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();
            //含税单价（销售出库单上运费单价）
            BigDecimal nckd_priceyf = row.getBigDecimal("nckd_priceyf") == null
                    ? BigDecimal.ZERO : row.getBigDecimal("nckd_priceyf");
            if (nckd_priceyf.compareTo(BigDecimal.ZERO) != 0){
                //计算不含税单价 = 含税单价/(1+(税率/100))
                BigDecimal nckdFtaxrateid = (row.getBigDecimal("nckd_ftaxrateid").divide(BigDecimal.valueOf(100))).add(BigDecimal.ONE);
                BigDecimal nckd_priceyfnotax = nckd_priceyf.divide(nckdFtaxrateid,BigDecimal.ROUND_CEILING);
                row.set("nckd_priceyfnotax",nckd_priceyfnotax);
            }

            //销售出库单上签收数量
            BigDecimal nckd_signqty = row.getBigDecimal("nckd_signqty") == null
                    ? BigDecimal.ZERO : row.getBigDecimal("nckd_signqty");
            //不含税单价
            BigDecimal nckd_priceyfnotax = row.getBigDecimal("nckd_priceyfnotax") == null
                    ? BigDecimal.ZERO : row.getBigDecimal("nckd_priceyfnotax");
            //计算运费
            row.set("nckd_amountyf",nckd_signqty.multiply(nckd_priceyfnotax));

        }
    }

}