package nckd.yanye.occ.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.util.Iterator;

/**
 * 地市仓库仓库库存情况-报表界面插件
 * 表单标识：nckd_citywarehouse_rpt
 * author:zhangzhilong
 * date:2024/09/07
 */
public class CityWarehouseReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
            //计划小包
            BigDecimal nckd_plans = next.getBigDecimal("nckd_plans") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_plans");
            //计划大包
            BigDecimal nckd_planb = next.getBigDecimal("nckd_planb") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_planb");
            //计划小计 = 计划小包 + 计划大包
            BigDecimal nckd_plansum = nckd_plans.add(nckd_planb);
            next.set("nckd_plansum",nckd_plansum);
            //库容小包
            BigDecimal nckd_storages = next.getBigDecimal("nckd_storages") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_storages");
            //库容大包
            BigDecimal nckd_storageb = next.getBigDecimal("nckd_storageb") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_storageb");
            //库容小计 = 库容小包 + 库容大包
            BigDecimal nckd_storagesum = nckd_storages.add(nckd_storageb);
            next.set("nckd_storagesum",nckd_storagesum);
            
            //库存小计
            BigDecimal nckdKcxj = next.getBigDecimal("nckd_kcxj");

            //小计（差异）= 库存小计 - 计划小计
            next.set("nckd_xjcy",nckdKcxj.subtract(nckd_plansum));

            //小包差异 = 小包合计 - 计划小包
            next.set("nckd_xbcy",next.getBigDecimal("nckd_xbhj").subtract(nckd_plans));

            //大包差异 = 大包合计 - 计划大包
            next.set("nckd_dbcy",next.getBigDecimal("nckd_dbhj").subtract(nckd_planb));
        }
    }
}