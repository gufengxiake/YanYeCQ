package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateTime;
import com.ccb.core.date.DateUtil;
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
import java.util.Objects;

/**
 * 发出商品报表-报表取数插件
 * 表单标识：nckd_sendoutproduct_rpt
 * author:zhangzhilong
 * date:2024/09/18
 */
public class SendOutProductReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //限定组织为晶昊本部和江西富达盐化有限公司的销售出库单
        QFilter initFilter= new QFilter("bizorg.number", QCP.in,new String[]{"11901","121"});
        //限定单据为已审核
        initFilter.and("billstatus", QCP.equals, "C");
        qFilters.add(initFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        //获取过滤组织
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if(nckdOrgQ != null){
            Long pkValue =(Long)nckdOrgQ.getPkValue();
            QFilter orgFilter= new QFilter("bizorg",QCP.equals,pkValue);
            qFilters.add(orgFilter);
        }
        //获取过滤客户
        DynamicObject nckdCustomerQ = filter.getDynamicObject("nckd_customer_q");
        if(nckdCustomerQ != null){
            Long pkValue =(Long)nckdCustomerQ.getPkValue();
            QFilter cusFilter= new QFilter("customer",QCP.equals,pkValue);
            qFilters.add(cusFilter);
        }
        //获取发货日期
        if(filter.getDate("fhdate_s") != null && filter.getDate("fhdate_e") != null){
            DateTime begin = DateUtil.beginOfDay(filter.getDate("fhdate_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("fhdate_e"));
            QFilter dateFilter= new QFilter("biztime",QCP.large_equals,begin).and("biztime",QCP.less_equals,end);
            qFilters.add(dateFilter);
        }
        //合同编号
        String fields = "nckd_xshth," +
                //发货单号
                "billno as out_billno," +
                //发货日期
                "biztime as out_biztime," +
                //客户编码
                "customer.number as out_customernumber," +
                //客户名称
                "customer.name as out_customername," +
                //客户省份
                "customer.admindivision as out_admindivision," +
                //客户性质
                "customer.nckd_customerxz.name as out_customerxz," +
                //集团内/外
                "customer.nckd_customertype.name as out_customertype," +
                //产品类别
                "billentry.material.masterid.group.name as out_groupname," +
                //存货编码
                "billentry.material.masterid.number as out_materialnumber," +
                //存货名称
                "billentry.material.masterid.name as out_materialname," +
                //规格
                "billentry.material.masterid.modelnum as out_gg," +
                //型号
                "billentry.material.masterid.nckd_model as out_xh," +
                //销售数量
                "billentry.baseqty as out_baseqty," +
                //计量单位
                "billentry.baseunit.name as out_baseunit," +
                //单价
                "billentry.price as out_price," +
                //结算币别
                "settlecurrency.name as out_settlecurrency," +
                //销售金额
                "billentry.amount as out_amount," +
                //发出商品成本
                "billentry.nckd_cbj as out_cbj," +
                //签收数量
                "billentry.nckd_signbaseqty as out_signbaseqty," +
                //核心单据行id
                "billentry.mainbillentryid as out_mainbillentryid," +
                //单据行id
                "billentry.id as out_entryid";
        DataSet imSalOutBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", fields, qFilters.toArray(new QFilter[0]), null);
        if (imSalOutBill.isEmpty()){
            return imSalOutBill;
        }
        //根据销售出库表体id获取财务应付单信息
        List<Long> outEntryid = DataSetToList.getOneToList(imSalOutBill, "out_entryid");
        DataSet finArBill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "ar_finarbill",
                //金额，单据日期，源单分录id
                "entry.e_amount as fin_amount,bizdate as fin_bizdate ,entry.e_srcentryid as fin_srcentryid",
                new QFilter[]{new QFilter("entry.e_srcentryid", QCP.in, outEntryid.toArray(new Long[0]))}, null);
        //关联财务应收单
        imSalOutBill = imSalOutBill.leftJoin(finArBill).on("out_entryid","fin_srcentryid").select(imSalOutBill.getRowMeta().getFieldNames(),new String[]{"fin_amount","fin_bizdate"}).finish();
        //根据财务应收单的日期进行过滤
        if(filter.getDate("jsdate_s") != null && filter.getDate("jsdate_e") != null){
            DateTime begin = DateUtil.beginOfDay(filter.getDate("jsdate_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("jsdate_e"));
            imSalOutBill = imSalOutBill.filter("fin_bizdate >=to_date('" + begin + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("fin_bizdate <=to_date('" + end + "','yyyy-MM-dd hh:mm:ss')");
        }

        //根据销售出库的核心单据行id获取销售订单信息
        List<Long> outMainbillentryid = DataSetToList.getOneToList(imSalOutBill, "out_mainbillentryid");
        DataSet salOrder = QueryServiceHelper.queryDataSet(this.getClass().getName(), "sm_salorder",
                //单据编号，分录id
                "billentry.id as orderid,billno as order_billno",
                new QFilter[]{new QFilter("billentry.id", QCP.in, outMainbillentryid.toArray(new Long[0]))}, null);
        //关联销售订单
        imSalOutBill = imSalOutBill.leftJoin(salOrder).on("out_mainbillentryid","orderid").select(imSalOutBill.getRowMeta().getFieldNames(),new String[]{"order_billno"}).finish();


        return imSalOutBill.orderBy(new String[]{"out_biztime","out_billno"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("nckd_xshth",ReportColumn.TYPE_TEXT,"合同编号"));
        columns.add(createReportColumn("order_billno",ReportColumn.TYPE_TEXT,"订单编号"));
        columns.add(createReportColumn("out_billno",ReportColumn.TYPE_TEXT,"发货单号"));
        columns.add(createReportColumn("out_biztime",ReportColumn.TYPE_DATE,"发货日期"));
        columns.add(createReportColumn("out_customernumber",ReportColumn.TYPE_TEXT,"客户编码"));
        columns.add(createReportColumn("out_customername",ReportColumn.TYPE_TEXT,"客户名称"));
        columns.add(createReportColumn("admindivision",ReportColumn.TYPE_TEXT,"客户省份"));
        columns.add(createReportColumn("out_customerxz",ReportColumn.TYPE_TEXT,"客户性质"));
        columns.add(createReportColumn("out_customertype",ReportColumn.TYPE_TEXT,"集团内/外"));

        columns.add(createReportColumn("out_groupname",ReportColumn.TYPE_TEXT,"产品类别"));
        columns.add(createReportColumn("out_materialnumber",ReportColumn.TYPE_TEXT,"存货编码"));
        columns.add(createReportColumn("out_materialname",ReportColumn.TYPE_TEXT,"存货名称"));
        columns.add(createReportColumn("out_gg",ReportColumn.TYPE_TEXT,"规格"));
        columns.add(createReportColumn("out_xh",ReportColumn.TYPE_TEXT,"型号"));

        columns.add(createReportColumn("out_baseqty",ReportColumn.TYPE_DECIMAL,"销售数量"));
        columns.add(createReportColumn("out_baseunit",ReportColumn.TYPE_TEXT,"计量单位"));
        columns.add(createReportColumn("out_price",ReportColumn.TYPE_DECIMAL,"单价"));
        columns.add(createReportColumn("out_settlecurrency",ReportColumn.TYPE_TEXT,"结算币别"));
        columns.add(createReportColumn("out_amount",ReportColumn.TYPE_DECIMAL,"销售金额"));
        columns.add(createReportColumn("out_cbj",ReportColumn.TYPE_DECIMAL,"发出商品成本"));
        columns.add(createReportColumn("out_signbaseqty",ReportColumn.TYPE_DECIMAL,"签收数量"));
        columns.add(createReportColumn("fin_amount",ReportColumn.TYPE_DECIMAL,"开票金额"));
        //特殊处理一下行政区划，获取行政区划的对象并隐藏后续用来获取省份
        ReportColumn out_admindivision = ReportColumn.createBaseDataColumn("out_admindivision", "bd_admindivision");
        out_admindivision.setHide(true);
        columns.add(out_admindivision);

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