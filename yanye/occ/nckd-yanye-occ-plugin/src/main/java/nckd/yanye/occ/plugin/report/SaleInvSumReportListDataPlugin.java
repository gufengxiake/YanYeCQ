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
 * 销售发票汇总表-报表取数插件
 * 表单标识：nckd_saleinvsum
 * author:zhangzhilong
 * date:2024/10/06
 */
public class SaleInvSumReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //已审核
        QFilter initFilter = new QFilter("billstatus", QCP.equals, "C");
        //应收数量不为0
        initFilter.and("billentry.joinpriceqty",QCP.not_equals2,0);
        //限定组织为晶昊本部和江西富达盐化有限公司的销售出库单
        initFilter.and("bizorg.number", QCP.in, new String[]{"119.01", "121"});
        qFilters.add(initFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        //获取销售组织过滤
        if(filter.getDynamicObject("nckd_org_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_org_q").getPkValue();
            QFilter  qFilter= new QFilter("bizorg",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取销售部门过滤
        if(filter.getDynamicObject("nckd_dept_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_dept_q").getPkValue();
            QFilter qFilter = new QFilter("bizdept",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取销售员过滤
        if(filter.getDynamicObject("nckd_operator_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_operator_q").getPkValue();
            QFilter qFilter = new QFilter("bizoperator",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取收货客户过滤
        if(filter.getDynamicObject("nckd_customer_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_customer_q").getPkValue();
            QFilter qFilter = new QFilter("customer",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取物料过滤
        if(filter.getDynamicObject("nckd_material_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_material_q").getPkValue();
            QFilter qFilter = new QFilter("billentry.material.masterid",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        if(filter.getDate("start") != null && filter.getDate("end") != null){
            DateTime start = DateUtil.beginOfDay(filter.getDate("start"));
            DateTime end = DateUtil.endOfDay(filter.getDate("end"));
            QFilter qFilter = new QFilter("biztime",QCP.large_equals,start).and("biztime",QCP.less_equals,end);
            qFilters.add(qFilter);
        }
        String sFields = "bizorg.name AS bizorg," +
                //部门
                "bizdept.name AS bizdept," +
                //业务员
                "bizoperator.operatorname as bizoperator," +
                //客户编码
                "customer.number as customer ," +
                //客户名称
                "customer.name as customername," +
                //出货仓库
                "billentry.warehouse.name as warehouse," +
                //物料分类编码
                "billentry.material.masterid.group.number as group," +
                //物料分类名称
                "billentry.material.masterid.group.name as groupname," +
                //物料编码
                "billentry.material.masterid.number as material," +
                //物料名称
                "billentry.material.masterid.name as materialname," +
                //存货规格
                "billentry.material.masterid.modelnum as materialmodel," +
                //赠品
                "billentry.ispresent as ispresent," +
                //基本数量
                "billentry.baseqty as baseqty," +
                //签收基本数量
                "billentry.nckd_signbaseqty as nckd_signbaseqty," +
                //含税单价
                "billentry.priceandtax as priceandtax," +
                //金额（本位币）
                "billentry.curamount as curamount," +
                //税额（本位币）
                "billentry.curtaxamount as curtaxamount," +
                //价税合计（本位币）
                "billentry.curamountandtax as curamountandtax," +
                //折扣额（本位币）
                "billentry.discountamount as discountamount," +
                //原币税额
                "billentry.taxamount as taxamount," +
                //原币无税金额
                "billentry.amount as amount," +
                //原币价税合计
                "billentry.amountandtax as amountandtax," +
                //成本金额
                "billentry.nckd_cbj as nckd_cbj," +
                "nckd_ismore," +
                "billentry.id as out_entryid";

        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]), null);
        if(imSaloutbill.isEmpty()){
            return imSaloutbill;
        }
        //根据销售出库表体id获取财务应付单信息
        List<Long> outEntryid = DataSetToList.getOneToList(imSaloutbill, "out_entryid");
        DataSet finArBill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "ar_finarbill",
                //金额，单据日期，源单分录id
                "nckd_area,entry.e_srcentryid as fin_srcentryid",
                new QFilter[]{new QFilter("entry.e_srcentryid", QCP.in, outEntryid.toArray(new Long[0]))}, null);
        //关联财务应收单
        imSaloutbill = imSaloutbill.leftJoin(finArBill).on("out_entryid", "fin_srcentryid").select(imSaloutbill.getRowMeta().getFieldNames(), new String[]{"nckd_area"}).finish();

        imSaloutbill = imSaloutbill.groupBy(new String[]{"bizorg","nckd_area","bizdept","bizoperator","customer","customername", "warehouse","group","groupname","material","materialname","materialmodel","ispresent","nckd_ismore"})
                .sum("baseqty")
                .sum("case when nckd_signbaseqty = 0 then baseqty else nckd_signbaseqty end","nckd_signbaseqty")
                .sum("curamount")
                .sum("curtaxamount")
                .sum("curamountandtax")
                .sum("discountamount")
                .sum("taxamount")
                .sum("amount")
                .sum("amountandtax").finish();

        //根据财务应收单的日期进行过滤
        return imSaloutbill.orderBy(new String[]{"bizorg","material"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("bizorg",ReportColumn.TYPE_TEXT,"公司"));
        columns.add(createReportColumn("nckd_area",ReportColumn.TYPE_TEXT,"所属地区"));
        columns.add(createReportColumn("bizdept",ReportColumn.TYPE_TEXT,"销售部门"));
        columns.add(createReportColumn("bizoperator",ReportColumn.TYPE_TEXT,"业务员"));
        columns.add(createReportColumn("customer",ReportColumn.TYPE_TEXT,"客户编码"));
        columns.add(createReportColumn("customername",ReportColumn.TYPE_TEXT,"客户名称"));
        columns.add(createReportColumn("warehouse",ReportColumn.TYPE_TEXT,"出货仓库"));
        columns.add(createReportColumn("group",ReportColumn.TYPE_TEXT,"物料分类编码"));
        columns.add(createReportColumn("groupname",ReportColumn.TYPE_TEXT,"物料分类名称"));
        columns.add(createReportColumn("material",ReportColumn.TYPE_TEXT,"物料编码"));
        columns.add(createReportColumn("materialname",ReportColumn.TYPE_TEXT,"物料名称"));
        columns.add(createReportColumn("materialmodel",ReportColumn.TYPE_TEXT,"存货规格"));
        columns.add(createReportColumn("ispresent",ReportColumn.TYPE_TEXT,"赠品"));
        columns.add(createReportColumn("baseqty",ReportColumn.TYPE_DECIMAL,"实发数量"));
        columns.add(createReportColumn("kjdb",ReportColumn.TYPE_DECIMAL,"亏斤短包"));
        columns.add(createReportColumn("nckd_signbaseqty",ReportColumn.TYPE_DECIMAL,"结算数量"));
        columns.add(createReportColumn("priceandtax",ReportColumn.TYPE_DECIMAL,"含税单价"));
        columns.add(createReportColumn("curamount",ReportColumn.TYPE_DECIMAL,"金额（本位币）"));
        columns.add(createReportColumn("curtaxamount",ReportColumn.TYPE_DECIMAL,"税额（本位币）"));
        columns.add(createReportColumn("curamountandtax",ReportColumn.TYPE_DECIMAL,"价税合计（本位币）"));
        columns.add(createReportColumn("discountamount",ReportColumn.TYPE_DECIMAL,"折扣额（本位币）"));
        columns.add(createReportColumn("taxamount",ReportColumn.TYPE_DECIMAL,"原币税额"));
        columns.add(createReportColumn("amount",ReportColumn.TYPE_DECIMAL,"原币无税金额"));
        columns.add(createReportColumn("amountandtax",ReportColumn.TYPE_DECIMAL,"原币价税合计"));
        columns.add(createReportColumn("nckd_cbj",ReportColumn.TYPE_DECIMAL,"成本金额"));
//        columns.add(createReportColumn("nckd_ismore",ReportColumn.TYPE_CHECKBOX,"是否长吨出库"));

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