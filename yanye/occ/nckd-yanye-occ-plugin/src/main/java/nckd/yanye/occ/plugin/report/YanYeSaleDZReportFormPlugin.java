package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;

/**
 * 盐类产品内部销售对账表-界面处理插件
 * 表单标识：nckd_ylcpnbxsdz_rpt
 * author:zzl
 * date:2024/08/27
 */
public class YanYeSaleDZReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给发货组织默认值
        filter.addFilterItem("nckd_saleorgid_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //获取数量
            BigDecimal nckdSaleqty = next.getBigDecimal("nckd_saleqty") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_saleqty") ;
            //获取收货数量
            BigDecimal nckdPurqty = next.getBigDecimal("nckd_purqty") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_purqty") ;
            //差异数量
            next.set("nckd_cyqty",nckdSaleqty.subtract(nckdPurqty));
        }
    }
}