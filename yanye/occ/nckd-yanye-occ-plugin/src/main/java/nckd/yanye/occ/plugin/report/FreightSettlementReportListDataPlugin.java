package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateTime;
import com.ccb.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
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
 * 运费结算表-报表取数插件
 * 表单标识：nckd_freightsettlementrpt
 * author:zhangzhilong
 * date:2024/09/18
 */
public class FreightSettlementReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        QFilter initFilter = new QFilter("billtypeid.name", QCP.like,"%销售费用应付%");
        qFilters.add(initFilter);
        FilterInfo filter = reportQueryParam.getFilter();
        //获取组织过滤条件
        this.getQFilter(qFilters,filter,"nckd_org_q","org");
        //获取运费单位过滤条件
        this.getQFilter(qFilters,filter,"nckd_freight_q","asstact");
        //获取收货单位过滤条件
        this.getQFilter(qFilters,filter,"nckd_customer_q","nckd_customer");
        //获取物料过滤条件
        this.getQFilter(qFilters,filter,"nckd_material_q","detailentry.material");
        //获取发货日期过滤条件
        if(filter.getDate("fhdate_s") != null && filter.getDate("fhdate_e") != null ){
            DateTime begin = DateUtil.beginOfDay(filter.getDate("fhdate_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("fhdate_e"));
            qFilters.add(new QFilter("detailentry.nckd_fhdate",QCP.large_equals,begin)
                    .and("detailentry.nckd_fhdate",QCP.less_equals,end));
        }
        //获取运输合同号过滤条件
        if(!filter.getString("nckd_yshth_q").isEmpty()){
            String nckdYshthQ = filter.getString("nckd_yshth_q");
            qFilters.add(new QFilter("detailentry.nckd_carriagenumber",QCP.like,"%"+nckdYshthQ+"%"));
        }

        //物料编码
        String fields = "detailentry.material.number as materialNumber," +
                //物料名称
                "detailentry.material.name as materialName," +
                //规格型号
                "detailentry.material.modelnum as materialModelnum," +
                //发货日期
                "detailentry.nckd_fhdate as nckd_fhdate," +
                //发货单号
                "detailentry.nckd_fhbillno as nckd_fhbillno," +
                //发货数量
                "detailentry.nckd_outqty as nckd_outqty," +
                //途损数
                "detailentry.nckd_damageqty as nckd_damageqty," +
                //客户签收数
                "detailentry.nckd_receiveqty as nckd_receiveqty," +
                //运费
                "detailentry.pricetax as yf," +
                //价税合计
                "inventry.i_pricetaxtotal as i_pricetaxtotal," +
                //结算方式
                "nckd_freighttype as nckd_freighttype," +
                //磅单号
                "detailentry.nckd_poundnumber as nckd_poundnumber," +
                //车号
                "detailentry.nckd_licensenumber as nckd_licensenumber," +
                //船号
                "detailentry.nckd_shipnumber as nckd_shipnumber," +
                //运输方式
                "nckd_transporttype," +
                //报关单号
                "detailentry.nckd_declarationnumber as nckd_declarationnumber," +
                //运输合同号
                "detailentry.nckd_carriagenumber as nckd_carriagenumber";
        DataSet ap_finapbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "ap_finapbill",
                fields, qFilters.toArray(new QFilter[0]) , null);

        return ap_finapbill.orderBy(new String[]{"nckd_fhdate","materialNumber"});
    }

    public void getQFilter(List<QFilter>qFilters, FilterInfo filter,String query,String property){
        if (filter.getDynamicObject(query) != null) {
            Long pkValue = (Long) filter.getDynamicObject(query).getPkValue();
            QFilter qFilter = new QFilter(property,QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("materialNumber",ReportColumn.TYPE_TEXT,"物料编码"));
        columns.add(createReportColumn("materialName",ReportColumn.TYPE_TEXT,"物料名称"));
        columns.add(createReportColumn("materialModelnum",ReportColumn.TYPE_TEXT,"规格型号"));
        columns.add(createReportColumn("nckd_fhdate",ReportColumn.TYPE_DATE,"发货日期"));
        columns.add(createReportColumn("nckd_fhbillno",ReportColumn.TYPE_TEXT,"发货单号"));

        columns.add(createReportColumn("nckd_outqty",ReportColumn.TYPE_DECIMAL,"发货数量"));
        columns.add(createReportColumn("nckd_damageqty",ReportColumn.TYPE_DECIMAL,"途损数"));
        columns.add(createReportColumn("nckd_receiveqty",ReportColumn.TYPE_DECIMAL,"客户签收数"));
        columns.add(createReportColumn("yf",ReportColumn.TYPE_DECIMAL,"运费"));
        columns.add(createReportColumn("i_pricetaxtotal",ReportColumn.TYPE_DECIMAL,"价税合计"));
        columns.add(createReportColumn("nckd_freighttype",ReportColumn.TYPE_COMBO,"结算方式"));
        columns.add(createReportColumn("nckd_poundnumber",ReportColumn.TYPE_TEXT,"磅单号"));
        columns.add(createReportColumn("nckd_licensenumber",ReportColumn.TYPE_TEXT,"车号"));
        columns.add(createReportColumn("nckd_shipnumber",ReportColumn.TYPE_TEXT,"船号"));
        columns.add(createReportColumn("nckd_transporttype",ReportColumn.TYPE_COMBO,"运输方式"));
        columns.add(createReportColumn("nckd_declarationnumber",ReportColumn.TYPE_TEXT,"报关单号"));
        columns.add(createReportColumn("nckd_carriagenumber",ReportColumn.TYPE_TEXT,"运输合同号"));

        return columns;
    }

    public ReportColumn createReportColumn(String fileKey, String fileType, String name) {
        if (Objects.equals(fileType, ReportColumn.TYPE_COMBO)) {
            ComboReportColumn column = new ComboReportColumn();
            column.setFieldKey(fileKey);
            column.setFieldType(fileType);
            column.setCaption(new LocaleString(name));
            List<ValueMapItem> comboItems = new ArrayList<>();
            if (Objects.equals(fileKey, "nckd_transporttype")) {
                comboItems.add(new ValueMapItem("", "A", new LocaleString("船运")));
                comboItems.add(new ValueMapItem("", "B", new LocaleString("汽运")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("火车")));
            } else if (Objects.equals(fileKey, "nckd_freighttype")){
                comboItems.add(new ValueMapItem("", "A", new LocaleString("出厂")));
                comboItems.add(new ValueMapItem("", "B", new LocaleString("一票")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("出口CNF")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("出口FOB")));
            }
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