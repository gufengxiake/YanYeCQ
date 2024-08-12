package nckd.yanye.fi.plugin.report;

import kd.bos.dataentity.entity.LocaleString;
import kd.bos.report.events.CreateFilterInfoEvent;
import kd.bos.report.events.SortAndFilterEvent;
import kd.bos.report.plugin.AbstractReportFormPlugin;

import java.util.*;

/**
 * Module           :财务云-固定资产-报表
 * Description      :资产清单筛选图标插件
 *
 * @author : yaosijie
 * @date : 2024/8/2
 */
public class AssetListReportFormPlugin extends AbstractReportFormPlugin {

    /**
     * 支持自定义过滤和排序的列
     */
    private final List<String> COLUMNS = Arrays.asList("nckd_specializedequipmen");


    @Override
    public void setSortAndFilter(List<SortAndFilterEvent> allColumns) {
        super.setSortAndFilter(allColumns);
        // 自定义过滤和排序的列
        for (SortAndFilterEvent event : allColumns) {
            if (COLUMNS.contains(event.getColumnName())) {
                event.setSort(true);
                event.setFilter(true);
            }
        }
    }

    @Override
    public void beforeCreateFilterInfo(CreateFilterInfoEvent event) {

        ArrayList<HashMap> filterItemList = new ArrayList();
        if ("nckd_specializedequipmen".equals(event.getFieldKey())) {
            /**
             * kd.bos.entity.filter.FilterMetadata #getCompareTypeByCompareTypeId
             */
            HashMap filterItem1 = new HashMap();
            filterItem1.put("name", new LocaleString("包含"));
            filterItem1.put("value", "");
            filterItem1.put("inputCtlType", 0);
            filterItem1.put("id", "59");
            filterItemList.add(filterItem1);
            HashMap filterItem2 = new HashMap();
            filterItem2.put("name", new LocaleString("不包含"));
            filterItem2.put("value", "");
            filterItem2.put("inputCtlType", 0);
            filterItem2.put("id", "58");
            filterItemList.add(filterItem2);

            HashMap filterItem3 = new HashMap();
            filterItem3.put("name", new LocaleString("等于"));
            filterItem3.put("value", "");
            filterItem3.put("inputCtlType", 0);
            filterItem3.put("id", "67");
            filterItemList.add(filterItem3);

            HashMap filterItem4 = new HashMap();
            filterItem4.put("name", new LocaleString("不等于"));
            filterItem4.put("value", "");
            filterItem4.put("inputCtlType", 0);
            filterItem4.put("id", "83");
            filterItemList.add(filterItem4);

            HashMap filterItem5 = new HashMap();
            filterItem5.put("name", new LocaleString("以...开始"));
            filterItem5.put("value", "");
            filterItem5.put("inputCtlType", 0);
            filterItem5.put("id", "60");
            filterItemList.add(filterItem5);

            HashMap filterItem6 = new HashMap();
            filterItem6.put("name", new LocaleString("以...结束"));
            filterItem6.put("value", "");
            filterItem6.put("inputCtlType", 0);
            filterItem6.put("id", "211");
            filterItemList.add(filterItem6);

            HashMap filterItem7 = new HashMap();
            filterItem7.put("name", new LocaleString("为空"));
            filterItem7.put("value", "");
            filterItem7.put("inputCtlType", -1);
            filterItem7.put("id", "37");
            filterItemList.add(filterItem7);

            HashMap filterItem8 = new HashMap();
            filterItem8.put("name", new LocaleString("不为空"));
            filterItem8.put("value", "");
            filterItem8.put("inputCtlType", -1);
            filterItem8.put("id", "9");
            filterItemList.add(filterItem8);

        }

        Map filterInfoMap = new HashMap();
        filterInfoMap.put("filterItems", filterItemList);
        event.setFilterInfo(filterInfoMap);
        event.setCancel(true);
    }
}
