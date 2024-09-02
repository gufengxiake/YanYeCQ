package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;

/**
 * 发票出库差额全表-报表界面插件
 * 表单标识：nckd_saleinvoutdif_rpt
 * author:zzl
 * date:2024/08/30
 */
public class SaleInvOutDifReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给组织默认值
        filter.addFilterItem("nckd_bizorg_q", curLoginOrg);
        Date date = new Date();
        //给时间当前默认值
        filter.addFilterItem("start", date);
        filter.addFilterItem("end", date);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            BigDecimal nckd_qty = next.getBigDecimal("nckd_qty") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_qty");
            BigDecimal nckd_amount = next.getBigDecimal("nckd_outamount") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_outamount");
            BigDecimal nckd_invnum = next.getBigDecimal("nckd_invnum") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_invnum");
            BigDecimal nckd_invamount = next.getBigDecimal("nckd_invamount") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_invamount");
//            出库数量-开票申请数量 = 数量差额
            next.set("nckd_outinvqty",nckd_qty.subtract(nckd_invnum));
//            出库金额-开票申请金额 = 金额差额
            next.set("nckd_outinvamount",nckd_amount.subtract(nckd_invamount));
        }
    }
}