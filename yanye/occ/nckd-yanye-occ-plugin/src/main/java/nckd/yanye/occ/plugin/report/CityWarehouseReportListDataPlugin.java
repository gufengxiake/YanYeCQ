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
        QFilter qFilter = new QFilter("createorg.name", QCP.like,"%华康%");
        qFilters.add(qFilter);
        String sFields = "createorg.name as createorgname," +
                "address as addressname," +
                "nckd_plans," +
                "nckd_planb," +
                "nckd_storages," +
                "nckd_storageb," +
                "id";
        DataSet bd_warehouse = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "bd_warehouse", sFields,
                        qFilters.toArray(new QFilter[0]) ,
                        null);

        return bd_warehouse.orderBy(new String[]{"createorgname"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("createorgname", ReportColumn.TYPE_TEXT, "公司"));
//        columns.add(createReportColumn("addressname", ReportColumn.TYPE_TEXT, "区域"));
        ReportColumn baseDataColumn = ReportColumn.createBaseDataColumn("addressname", "bd_admindivision");
        baseDataColumn.setCaption(new LocaleString("区域"));
        columns.add(baseDataColumn);
        columns.add(createReportColumn("nckd_plansum", ReportColumn.TYPE_DECIMAL, "计划小计"));
        columns.add(createReportColumn("nckd_plans", ReportColumn.TYPE_DECIMAL, "计划小包"));
        columns.add(createReportColumn("nckd_planb", ReportColumn.TYPE_DECIMAL, "计划大包"));
        columns.add(createReportColumn("nckd_storagesum", ReportColumn.TYPE_DECIMAL, "库容小记"));
        columns.add(createReportColumn("nckd_storages", ReportColumn.TYPE_DECIMAL, "库容小包"));
        columns.add(createReportColumn("nckd_storageb", ReportColumn.TYPE_DECIMAL, "库容大包"));
        columns.add(createReportColumn("nckd_kcxj", ReportColumn.TYPE_DECIMAL, "库存小计"));
        columns.add(createReportColumn("nckd_xbhj", ReportColumn.TYPE_DECIMAL, "小包合计"));
        columns.add(createReportColumn("nckd_dbhj", ReportColumn.TYPE_DECIMAL, "大包合计"));
        columns.add(createReportColumn("nckd_xjcy", ReportColumn.TYPE_DECIMAL, "小计(差异)"));
        columns.add(createReportColumn("nckd_xbcy", ReportColumn.TYPE_DECIMAL, "小包差异"));
        columns.add(createReportColumn("nckd_dbcy", ReportColumn.TYPE_DECIMAL, "大包差异"));

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