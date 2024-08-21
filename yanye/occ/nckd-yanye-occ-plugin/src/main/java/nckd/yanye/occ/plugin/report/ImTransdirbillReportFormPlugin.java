package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.*;

/**
 * 报表界面插件
 */
public class ImTransdirbillReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    private static String [] FIELDS ={"nckd_forg","nckd_ywy","nckd_material","nckd_materialname",
            "nckd_materialmodelnum","nckd_unit","nckd_jhqty",
            "nckd_xsqty","nckd_jchhqty","nckd_jhyeqty"};
    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
//        Map<Long, Long> materialId = this.checkAndChangeMaterial(rowData);
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();

            long totalAmount = 0L;
            totalAmount = row.getLong(FIELDS[6]) - row.getLong(FIELDS[7]) - row.getLong(FIELDS[8]);
            row.set(FIELDS[9], totalAmount);
        }

    }

}