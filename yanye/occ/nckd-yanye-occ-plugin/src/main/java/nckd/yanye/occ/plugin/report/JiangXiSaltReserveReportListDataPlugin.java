package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.AbstractReportColumn;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.ReportColumn;
import kd.bos.entity.report.ReportQueryParam;
import kd.scmc.im.report.algox.util.ImReportQueryHelper;
import kd.scmc.im.report.algox.util.ReqParam;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 江西省政府食盐储备-报表取数插件
 * 表单标识：nckd_jxsaltreserve_rpt
 * author:zhangzhilong
 * date:2024/09/14
 */
public class JiangXiSaltReserveReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ReqParam reqParam = new ReqParam();
        List<Long> orgIds = new ArrayList<>();
        orgIds.add(1956460855902094336L);

        reqParam.setOrgIds(orgIds);
//        reqParam.setqFilter(qFilter);
        reqParam.setBeginDate("2024-08-01");
        reqParam.setEndDate("2024-10-01");
//        owner, expirydate, configuredcode, producedate, org, baseunit, keeper, materialgroup, project, warehouse, unit2nd, unit, invtype, material, keepertype, lotnumber, ownertype, location, invstatus, auxpty, tracknumber
        DataSet dataSet = ImReportQueryHelper.query(reqParam);
        dataSet.filter("");

        return dataSet;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("warehouse",ReportColumn.TYPE_TEXT,"仓库名称"));
        columns.add(createReportColumn("materialgroup",ReportColumn.TYPE_TEXT,"类别名称"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"期初库存"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"总收入"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"总发出"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"期末库存"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"计划"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"完成率"));

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