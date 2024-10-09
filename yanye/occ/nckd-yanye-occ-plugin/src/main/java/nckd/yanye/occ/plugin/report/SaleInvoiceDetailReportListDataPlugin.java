package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateTime;
import com.ccb.core.date.DateUtil;
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
 * 销售发票明细表-报表取数插件
 * 表单标识：nckd_saleinvoicedet_rpt
 * author:zhangzhilong
 * date:2024/09/20
 */
public class SaleInvoiceDetailReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //限定单据状态为已审核
        QFilter initFilter = new QFilter("billstatus", QCP.equals, "C");
        qFilters.add(initFilter);
        FilterInfo filter = reportQueryParam.getFilter();
        //获取组织过滤
        if (filter.getDynamicObject("nckd_org_q") != null) {
            Long pkValue = (Long) filter.getDynamicObject("nckd_org_q").getPkValue();
            qFilters.add(new QFilter("bizorg", QCP.equals, pkValue));
        }
        //获取单据日期过滤
        if (filter.getDate("date_s") != null && filter.getDate("date_e") != null) {
            DateTime begin = DateUtil.beginOfDay(filter.getDate("date_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("date_e"));
            qFilters.add(new QFilter("biztime", QCP.large_equals, begin).and("biztime", QCP.less_equals, end));
        }
        //获取客户过滤
        if (filter.getDynamicObject("nckd_customer_q") != null) {
            Long pkValue = (Long) filter.getDynamicObject("nckd_customer_q").getPkValue();
            qFilters.add(new QFilter("customer", QCP.equals, pkValue));
        }
        //获取物料过滤
        if (filter.getDynamicObject("nckd_material_q") != null) {
            Long pkValue = (Long) filter.getDynamicObject("nckd_material_q").getPkValue();
            qFilters.add(new QFilter("billentry.material.masterid", QCP.equals, pkValue));
        }
        //获取业务员过滤
        if (filter.getDynamicObject("nckd_bizoperator_q") != null) {
            Long pkValue = (Long) filter.getDynamicObject("nckd_bizoperator_q").getPkValue();
            qFilters.add(new QFilter("bizoperator", QCP.equals, pkValue));
        }
        //获取仓库过滤
        if (filter.getDynamicObject("nckd_warehouse_q") != null) {
            Long pkValue = (Long) filter.getDynamicObject("nckd_warehouse_q").getPkValue();
            qFilters.add(new QFilter("warehouse", QCP.equals, pkValue));
        }
        //获取业务员过滤
        if (filter.getDynamicObject("nckd_dept_q") != null) {
            Long pkValue = (Long) filter.getDynamicObject("nckd_dept_q").getPkValue();
            qFilters.add(new QFilter("bizdept", QCP.equals, pkValue));
        }

        String fields =
                //公司
                "bizorg.name as bizorg," +
                //单据日期
                "biztime," +
                //单据类型
                "billtype.name as billtype," +
                //单据编号
                "billno," +
                //业务员
                "bizoperator.operatorname as  bizoperator," +
                //客户编码
                "customer.number as customernumber," +
                //客户名称
                "customer.name as customername," +
                //部门
                "bizdept.name as bizdept," +
                //物料编码
                "billentry.material.masterid.number as material," +
                //物料名称
                "billentry.material.masterid.name as materialname," +
                //批次
                "billentry.lotnumber as lotnumber," +
                //赠品
                "billentry.ispresent as ispresent," +
                //数量
                "billentry.baseqty as baseqty," +
                //无税净价
                "billentry.price as price," +
                //金额（本位币）
                "billentry.curamount as curamount," +
                //税额（本位币）
                "billentry.curtaxamount as curtaxamount," +
                //价税合计（本位币）
                "billentry.curamountandtax as curamountandtax" ;
        DataSet salOutBill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", fields, qFilters.toArray(new QFilter[0]), "biztime");

        return salOutBill.orderBy(new String[]{"bizorg","biztime", "material"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("bizorg",ReportColumn.TYPE_TEXT,"公司"));
        columns.add(createReportColumn("biztime",ReportColumn.TYPE_DATE,"单据日期"));
        columns.add(createReportColumn("billtype",ReportColumn.TYPE_TEXT,"单据类型"));
        columns.add(createReportColumn("billno",ReportColumn.TYPE_TEXT,"单据编号"));
        columns.add(createReportColumn("bizoperator",ReportColumn.TYPE_TEXT,"业务员"));
        columns.add(createReportColumn("customernumber",ReportColumn.TYPE_TEXT,"客户编码"));
        columns.add(createReportColumn("customername",ReportColumn.TYPE_TEXT,"客户名称"));
        columns.add(createReportColumn("bizdept",ReportColumn.TYPE_TEXT,"部门"));
        columns.add(createReportColumn("material",ReportColumn.TYPE_TEXT,"物料编码"));
        columns.add(createReportColumn("materialname",ReportColumn.TYPE_TEXT,"物料名称"));
        columns.add(createReportColumn("lotnumber",ReportColumn.TYPE_TEXT,"批次"));
        columns.add(createReportColumn("ispresent",ReportColumn.TYPE_BOOLEAN,"赠品"));
        columns.add(createReportColumn("baseqty",ReportColumn.TYPE_DECIMAL,"数量"));
        columns.add(createReportColumn("price",ReportColumn.TYPE_DECIMAL,"无税单价"));
        columns.add(createReportColumn("curamount",ReportColumn.TYPE_DECIMAL,"金额（本位币）"));
        columns.add(createReportColumn("curtaxamount",ReportColumn.TYPE_DECIMAL,"税额（本位币）"));
        columns.add(createReportColumn("curamountandtax",ReportColumn.TYPE_DECIMAL,"价税合计（本位币）"));
        return columns;
    }

    public ReportColumn createReportColumn(String fileKey, String fileType, String name) {
        if (Objects.equals(fileType, ReportColumn.TYPE_COMBO)) {
            ComboReportColumn column = new ComboReportColumn();
            column.setFieldKey(fileKey);
            column.setFieldType(fileType);
            column.setCaption(new LocaleString(name));
            List<ValueMapItem> comboItems = new ArrayList<>();
            comboItems.add(new ValueMapItem("", "A", new LocaleString("出厂")));
            comboItems.add(new ValueMapItem("", "B", new LocaleString("一票")));
            comboItems.add(new ValueMapItem("", "C", new LocaleString("出口CNF")));
            comboItems.add(new ValueMapItem("", "C", new LocaleString("出口FOB")));
            column.setComboItems(comboItems);
            return column;
        }
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