package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 发票出库差额全表-报表取数插件
 * 表单标识：nckd_saleinvoutdif_rpt
 * author:zzl
 * date:2024/08/30
 */
public class SaleInvOutDifReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_bizorg_q":
                    if (!(filterItem.getValue() == null)) {
                        QFilter qFilter = new QFilter("bizorg", QCP.equals, (Long) ((DynamicObject) filterItem.getValue()).getPkValue());
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件销售部门,标识如不一致,请修改
                case "nckd_bizdept_q":
                    if (!(filterItem.getValue() == null)) {
                        QFilter qFilter = new QFilter("bizoperator", QCP.equals, (Long) ((DynamicObject) filterItem.getValue()).getPkValue());
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "start":
                    if (!(filterItem.getDate() == null)) {
                        QFilter qFilter = new QFilter("biztime", QCP.large_equals,
                                DateUtil.beginOfDay(filterItem.getDate()));
                        qFilters.add(qFilter);
                    }
                    break;
                case "end":
                    if (!(filterItem.getDate() == null)) {
                        QFilter qFilter = new QFilter("biztime", QCP.less_equals,
                                DateUtil.endOfDay(filterItem.getDate()));
                        qFilters.add(qFilter);
                    }
                    break;

                // 查询条件物料,标识如不一致,请修改
                case "nckd_material_q":
                    if (!(filterItem.getValue() == null)) {
                        QFilter qFilter = new QFilter("customer", QCP.equals, (Long) ((DynamicObject) filterItem.getValue()).getPkValue());
                        qFilters.add(qFilter);
                    }
                    break;
            }
        }
        //公司
        String sFields = "bizorg AS nckd_bizorg," +
//                部门
                "bizdept AS nckd_bizdept," +
//                物料编码
                "billentry.material.number AS nckd_materialmnumber," +
//                物料名称
                "billentry.material.name AS nckd_materialname," +
//                销售出库数
                "billentry.qty AS nckd_qty," +
//                销售出库金额
                "billentry.amount as nckd_amount," +
//                核心单据行id
                "billentry.mainbillentryid as mainbillentryid";


        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]), null);

        return im_saloutbill;

    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {

        ReportColumn nckd_bizorg = createReportColumn("nckd_bizorg", ReportColumn.TYPE_TEXT, "公司");
        ReportColumn nckd_bizdept = createReportColumn("nckd_bizdept", ReportColumn.TYPE_TEXT, "部门");
        ReportColumn nckd_materialmnumber = createReportColumn("nckd_materialmnumber", ReportColumn.TYPE_TEXT, "物料编码");
        ReportColumn nckd_materialname = createReportColumn("nckd_materialname", ReportColumn.TYPE_TEXT, "物料名称");
        ReportColumn nckd_qty = createReportColumn("nckd_qty", ReportColumn.TYPE_DECIMAL, "销售出库数");
        ReportColumn nckd_amount = createReportColumn("nckd_amount", ReportColumn.TYPE_DECIMAL, "销售出库金额");

        columns.add(nckd_bizorg);
        columns.add(nckd_bizdept);
        columns.add(nckd_materialmnumber);
        columns.add(nckd_materialname);
        columns.add(nckd_qty);
        columns.add(nckd_amount);
        return columns;
    }


    public ReportColumn createReportColumn(String fieldKey, String fieldType, String caption) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fieldKey);
        column.setFieldType(fieldType);
        column.setCaption(new LocaleString(caption));
        if (fieldType.equals(ReportColumn.TYPE_DECIMAL)) {
            column.setScale(2);
            column.setZeroShow(true);
        }

        return column;
    }

}