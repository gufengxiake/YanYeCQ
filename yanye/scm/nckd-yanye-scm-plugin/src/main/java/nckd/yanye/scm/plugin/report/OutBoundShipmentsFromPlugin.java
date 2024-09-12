package nckd.yanye.scm.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

/**
 * 供应链-出库汇总表报表表单插件
 * 表单标识：nckd_outboundshipments
 * author：xiaoxiaopeng
 * date：2024-09-12
 */
public class OutBoundShipmentsFromPlugin extends AbstractReportFormPlugin {


    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        List<FilterItemInfo> filterItems = queryParam.getFilter().getFilterItems();
        if (filterItems == null || filterItems.size() == 0) {
            return;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String star = null;
        String end = null;
        for (FilterItemInfo filterItem : filterItems) {
            if ("nckd_daterange_startdate".equals(filterItem.getPropName())) {
                if (filterItem.getDate() != null){
                    star = simpleDateFormat.format(filterItem.getDate());
                }
            }
            if ("nckd_daterange_enddate".equals(filterItem.getPropName())) {
                if (filterItem.getDate() != null){
                    end = simpleDateFormat.format(filterItem.getDate());
                }
            }
        }
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();
            row.set("nckd_date",star + "~" + end);
        }
    }
}
