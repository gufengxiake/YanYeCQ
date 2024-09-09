package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.ValueMapItem;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 地市仓库仓库库存情况-报表取数插件
 * 表单标识：nckd_citywarehouse_rpt
 * author:zhangzhilong
 * date:2024/09/07
 */
public class CityWarehouseReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters= new ArrayList<>();
        QFilter qFilter = new QFilter("createorg.number", QCP.equals,"114").
                or("createorg.structure.viewparent.number", QCP.equals, "114");
        qFilters.add(qFilter);
        String sFields = "createorg.name as createorgname," +
                "address as addressname," +
                "nckd_plans," +
                "nckd_planb," +
                "nckd_storages," +
                "nckd_storageb";
        DataSet bd_warehouse = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "bd_warehouse", sFields, qFilters.toArray(new QFilter[0] ), null);

        return bd_warehouse;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("createorgname", ReportColumn.TYPE_TEXT, "公司"));
        columns.add(createReportColumn("addressname", ReportColumn.TYPE_TEXT, "区域"));
        columns.add(createReportColumn("nckd_plansum", ReportColumn.TYPE_TEXT, "计划小计"));
        columns.add(createReportColumn("nckd_plans", ReportColumn.TYPE_TEXT, "计划小包"));
        columns.add(createReportColumn("nckd_planb", ReportColumn.TYPE_TEXT, "计划大包"));
        columns.add(createReportColumn("nckd_storagesum", ReportColumn.TYPE_TEXT, "库容小记"));
        columns.add(createReportColumn("nckd_storages", ReportColumn.TYPE_TEXT, "库容小包"));
        columns.add(createReportColumn("nckd_storageb", ReportColumn.TYPE_TEXT, "库容大包"));
        columns.add(createReportColumn("nckd_kcxj", ReportColumn.TYPE_TEXT, "库存小计"));
        columns.add(createReportColumn("nckd_xbhj", ReportColumn.TYPE_TEXT, "小包合计"));
        columns.add(createReportColumn("nckd_dbhj", ReportColumn.TYPE_TEXT, "大包合计"));
        columns.add(createReportColumn("nckd_xjcy", ReportColumn.TYPE_TEXT, "小计(差异)"));
        columns.add(createReportColumn("nckd_xbcy", ReportColumn.TYPE_TEXT, "小包差异"));
        columns.add(createReportColumn("nckd_dbcy", ReportColumn.TYPE_TEXT, "大包差异"));

        return columns;
    }

    public ReportColumn createReportColumn(String fileKey, String fileType, String name) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fileKey);
        column.setFieldType(fileType);
        column.setCaption(new LocaleString(name));
        if (Objects.equals(fileType, ReportColumn.TYPE_DECIMAL)) {
            column.setScale(2);
        }
        return column;
    }

}