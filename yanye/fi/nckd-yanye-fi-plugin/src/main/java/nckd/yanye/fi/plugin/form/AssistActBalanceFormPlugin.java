package nckd.yanye.fi.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.property.QtyProp;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.org.utils.DynamicObjectUtils;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module           :总账-账表-辅助核算维度余额表
 * Description      :数量合计单位处理
 *
 * @author 梁秦刚
 * @Date 2024/9/4 15:02
 */
public class AssistActBalanceFormPlugin extends AbstractReportFormPlugin {

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        if (!queryParam.getFilter().getBoolean("showqty") || null == rowData || rowData.isEmpty()) {
            return;
        }
        DynamicObject total = rowData.get(rowData.size() - 1);
        List<DynamicObject> measureunitList = new ArrayList<>();
        for (DynamicObject rowDatum : rowData) {
            if (rowDatum.equals(total)) {
                break;
            }
            // 单位处理
            DynamicObject measureunit = rowDatum.getDynamicObject("measureunit");
            if (measureunit != null) {
                measureunitList.add(measureunit);
            }
        }
        // 取小数位最大的单位
        if (!measureunitList.isEmpty()){
            measureunitList.stream().max(Comparator.comparing(item -> item.getInt("precision"))).ifPresent(item -> {
                DynamicObject newUnit = BusinessDataServiceHelper.newDynamicObject(item.getDataEntityType().getName());
                DynamicObjectUtils.copy(item, newUnit);
                total.set("measureunit", newUnit);
                newUnit.set("name", null);
            });
        }

    }
}
