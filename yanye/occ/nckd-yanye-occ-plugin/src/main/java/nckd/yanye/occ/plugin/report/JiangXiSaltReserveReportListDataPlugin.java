package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.AbstractReportColumn;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.ReportColumn;
import kd.bos.entity.report.ReportQueryParam;
import kd.fi.cal.report.queryplugin.StockGatherRptQueryPlugin;
import kd.sdk.plugin.Plugin;

import java.util.List;
import java.util.Objects;

/**
 * 江西省政府食盐储备-报表取数插件
 * 表单标识：
 * author:zhangzhilong
 * date:2024/09/14
 */
public class JiangXiSaltReserveReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        StockGatherRptQueryPlugin StockGatherRptQueryPlugin = new StockGatherRptQueryPlugin();
        DataSet query = StockGatherRptQueryPlugin.query(reportQueryParam, o);
        return query;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        return super.getColumns(columns);
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