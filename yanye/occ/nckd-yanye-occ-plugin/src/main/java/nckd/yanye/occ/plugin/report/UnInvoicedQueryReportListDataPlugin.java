package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 未开票量查询-报表取数插件
 * 表单标识：nckd_uninvoicedquery_rpt
 * author:zhangzhilong
 * date:2024/10/07
 */
public class UnInvoicedQueryReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //已审核
        QFilter initFilter = new QFilter("billstatus", QCP.equals, "C");
        //应收数量不为0
        initFilter.and("billentry.joinpriceqty",QCP.not_equals2,0);
        //限定组织为晶昊本部和江西富达盐化有限公司的销售出库单
        initFilter.and("bizorg.number", QCP.in, new String[]{"11901", "121"});
        qFilters.add(initFilter);
//        FilterInfo filter = reportQueryParam.getFilter();
//        //获取销售组织过滤
//        if(filter.getDynamicObject("nckd_org_q") != null){
//            Long pkValue = (Long)filter.getDynamicObject("nckd_org_q").getPkValue();
//            QFilter  qFilter= new QFilter("bizorg",QCP.equals,pkValue);
//            qFilters.add(qFilter);
//        }
//        //获取销售部门过滤
//        if(filter.getDynamicObject("nckd_dept_q") != null){
//            Long pkValue = (Long)filter.getDynamicObject("nckd_dept_q").getPkValue();
//            QFilter qFilter = new QFilter("bizdept",QCP.equals,pkValue);
//            qFilters.add(qFilter);
//        }
//        //获取销售员过滤
//        if(filter.getDynamicObject("nckd_operator_q") != null){
//            Long pkValue = (Long)filter.getDynamicObject("nckd_operator_q").getPkValue();
//            QFilter qFilter = new QFilter("bizoperator",QCP.equals,pkValue);
//            qFilters.add(qFilter);
//        }
//        //获取收货客户过滤
//        if(filter.getDynamicObject("nckd_customer_q") != null){
//            Long pkValue = (Long)filter.getDynamicObject("nckd_customer_q").getPkValue();
//            QFilter qFilter = new QFilter("customer",QCP.equals,pkValue);
//            qFilters.add(qFilter);
//        }
//        //获取物料过滤
//        if(filter.getDynamicObject("nckd_material_q") != null){
//            Long pkValue = (Long)filter.getDynamicObject("nckd_material_q").getPkValue();
//            QFilter qFilter = new QFilter("billentry.material.masterid",QCP.equals,pkValue);
//            qFilters.add(qFilter);
//        }
//        if(filter.getDate("start") != null && filter.getDate("end") != null){
//            DateTime start = DateUtil.beginOfDay(filter.getDate("start"));
//            DateTime end = DateUtil.endOfDay(filter.getDate("end"));
//            QFilter qFilter = new QFilter("biztime",QCP.large_equals,start).and("biztime",QCP.less_equals,end);
//            qFilters.add(qFilter);
//        }
        String sFields = "bizorg.name AS bizorg," +
                //物料编码
                "billentry.material.masterid.number as material," +
                //物料名称
                "billentry.material.masterid.name as materialname," +
                //存货规格
                "billentry.material.masterid.modelnum as materialmodel," +
                //数量
                "billentry.baseqty as baseqty," +
                //含税单价
                "billentry.priceandtax as priceandtax," +
                //无税金额
                "billentry.amount as amount," +
                //价税合计
                "billentry.amountandtax as amountandtax," +
                //成本单价
                "billentry.nckd_cbdj as nckd_cbdj";

        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]), null);
        return im_saloutbill.orderBy(new String[]{"bizorg","material"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"公司"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"物料编码"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"物料名称"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"规格型号"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"发货日期"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"出库日期"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"发货单号"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"出库单号"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"开票单位"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"收货单位"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"实出数量"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"累计途损数量"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"实收数量"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"含税单价"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"无税金额"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"价税合计"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"运费结算方式"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"运输方式"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"承运单位"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"磅单号"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"车号"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"车皮号"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"备注"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"已开票数量"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"未开票数量"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"税率"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"税额"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"订单号"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"业务员"));
        return columns;
    }

    public ReportColumn createReportColumn(String fieldKey, String fieldType, String caption) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fieldKey);
        column.setFieldType(fieldType);
        column.setCaption(new LocaleString(caption));
        if (fieldType.equals(ReportColumn.TYPE_DECIMAL)) {
            column.setScale(2);
//            column.setZeroShow(true);
        }
        return column;
    }
}