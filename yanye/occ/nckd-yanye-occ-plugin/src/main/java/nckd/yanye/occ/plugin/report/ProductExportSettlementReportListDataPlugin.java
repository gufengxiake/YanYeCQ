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
 * 产品出口结算表-报表取数插件
 * 表单标识：nckd_productexpsettle_rpt
 * author:zhangzhilong
 * date:2024/09/19
 */
public class ProductExportSettlementReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //限定组织为晶昊本部和江西富达盐化有限公司的销售出库单
        QFilter initFilter = new QFilter("org.number", QCP.in, new String[]{"11901", "121"});
        //限定单据为已审核
        initFilter.and("billstatus", QCP.equals, "C");
        qFilters.add(initFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        //获取销售组织过滤
        if(filter.getDynamicObject("nckd_org_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_org_q").getPkValue();
            QFilter  qFilter= new QFilter("org",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取运费单位过滤
        if(filter.getDynamicObject("nckd_supplier_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_supplier_q").getPkValue();
            QFilter qFilter = new QFilter("nckd_carcustomer",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取收货单位过滤
        if(filter.getDynamicObject("nckd_customer_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_customer_q").getPkValue();
            QFilter qFilter = new QFilter("nckd_customer",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取物料过滤
        if(filter.getDynamicObject("nckd_material_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_material_q").getPkValue();
            QFilter qFilter = new QFilter("entryentity.nckd_materiel",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //物料编码
        String fields = "entryentity.nckd_materiel.number as materielnumber," +
                //物料名称
                "entryentity.nckd_materiel.name as materielname," +
                //规格型号
                "entryentity.nckd_materiel.modelnum as materielmodelnum," +
                //销售组织
                "org.name as orgname," +
                //运费单位
                "nckd_carcustomer.name as suppliername," +
                //收货单位
                "nckd_customer.name as customername," +
                //发货单号
                "nckd_sale," +
                //发货数
                "entryentity.nckd_outstockqty as nckd_outstockqty," +
//                "nckd_outstockqty - nckd_signqty," +
                //实收数
                "entryentity.nckd_signqty as nckd_signqty," +
                //含税单价
                "entryentity.nckd_xsprice as nckd_xsprice," +
                //运费结算方式
                "nckd_closingtype," +
                //磅单号
                "entryentity.nckd_eleno as nckd_eleno," +
                //车号
                "entryentity.nckd_cpno1 as nckd_cpno1," +
                //船（柜）号
                "entryentity.nckd_ship as nckd_ship," +
                //报关单号
                "nckd_customsno," +
                //来源单据id
                "entryentity.nckd_sourcebillid as nckd_sourcebillid";
        DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_signaturebill", fields, qFilters.toArray(new QFilter[0]), null);
        if (dataSet.isEmpty()){
            return dataSet;
        }

        //获取签收单来源单据id
        List<Long> nckdSourcebillid = DataSetToList.getOneToList(dataSet, "nckd_sourcebillid");
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", "id,biztime", new QFilter[]{new QFilter("id", QCP.in, nckdSourcebillid.toArray(new Long[0]))}, null);

        dataSet = dataSet.leftJoin(im_saloutbill).on("nckd_sourcebillid","id").select(dataSet.getRowMeta().getFieldNames(),new String[]{"biztime"}).finish();
        //根据发货日期过滤
        if(filter.getDate("fhdate_s") != null && filter.getDate("fhdate_e") != null ){
            DateTime begin = DateUtil.beginOfDay(filter.getDate("fhdate_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("fhdate_e"));
            dataSet = dataSet.filter("biztime >=to_date('" + begin + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("biztime <=to_date('" + end + "','yyyy-MM-dd hh:mm:ss')");
        }
        return dataSet;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {

//        columns.add(createReportColumn("orgname", ReportColumn.TYPE_TEXT, "销售组织"));
//        columns.add(createReportColumn("suppliername", ReportColumn.TYPE_TEXT, "运费单位"));
//        columns.add(createReportColumn("customername", ReportColumn.TYPE_TEXT, "收货单位"));
        columns.add(createReportColumn("materielnumber", ReportColumn.TYPE_TEXT, "物料编码"));
        columns.add(createReportColumn("materielname", ReportColumn.TYPE_TEXT, "物料名称"));
        columns.add(createReportColumn("materielmodelnum", ReportColumn.TYPE_TEXT, "规格型号"));
        columns.add(createReportColumn("biztime", ReportColumn.TYPE_DATE, "发货日期"));
        columns.add(createReportColumn("nckd_sale", ReportColumn.TYPE_TEXT, "发货单号"));
        columns.add(createReportColumn("nckd_outstockqty", ReportColumn.TYPE_DECIMAL, "发货数"));
        columns.add(createReportColumn("tusun", ReportColumn.TYPE_DECIMAL, "途损数"));
        columns.add(createReportColumn("nckd_signqty", ReportColumn.TYPE_DECIMAL, "实收数"));
        columns.add(createReportColumn("nckd_xsprice", ReportColumn.TYPE_DECIMAL, "含税单价"));
        columns.add(createReportColumn("jshj", ReportColumn.TYPE_DECIMAL, "价税合计"));
        columns.add(createReportColumn("nckd_closingtype", ReportColumn.TYPE_COMBO, "运费结算方式"));
        columns.add(createReportColumn("nckd_eleno", ReportColumn.TYPE_TEXT, "磅单号"));
        columns.add(createReportColumn("nckd_cpno1", ReportColumn.TYPE_TEXT, "车号"));
        columns.add(createReportColumn("nckd_ship", ReportColumn.TYPE_TEXT, "船（柜）号"));
        columns.add(createReportColumn("nckd_customsno", ReportColumn.TYPE_TEXT, "报关单号"));

//        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,""));

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