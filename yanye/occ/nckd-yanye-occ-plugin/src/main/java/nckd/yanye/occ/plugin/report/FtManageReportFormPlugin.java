package nckd.yanye.occ.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.util.Iterator;

/**
 * 外贸管理报表-报表界面插件
 * 表单标识：nckd_salemanage_rpt
 * author:zzl
 * date:2024/08/29
 */
public class FtManageReportFormPlugin extends AbstractReportFormPlugin implements Plugin {

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //运价
            BigDecimal nckdPricefieldyf1 = next.getBigDecimal("nckd_pricefieldyf1") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_pricefieldyf1");
            //客户签收数量
            BigDecimal nckd_signqty = next.getBigDecimal("nckd_signqty") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_signqty");
           //计算价税合计
            BigDecimal multiply = nckd_signqty.multiply(nckdPricefieldyf1);
            next.set("nckd_e_pricetaxtotal",multiply);
        }

    }
}