package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
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
        //应收数量为0
        initFilter.and("billentry.joinpriceqty",QCP.equals,0);
        //限定组织为晶昊本部和江西富达盐化有限公司的销售出库单
        initFilter.and("bizorg.number", QCP.in, new String[]{"11901", "121"});
        qFilters.add(initFilter);
        FilterInfo filter = reportQueryParam.getFilter();
        //获取销售组织过滤
        if(filter.getDynamicObject("nckd_org_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_org_q").getPkValue();
            QFilter  qFilter= new QFilter("bizorg",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //获取物料过滤
        if(filter.getDynamicObject("nckd_material_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_material_q").getPkValue();
            QFilter qFilter = new QFilter("billentry.material.masterid",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }
        //出库日期过滤
        if(filter.getDate("outdate_s") != null && filter.getDate("outdate_e") != null){
            DateTime start = DateUtil.beginOfDay(filter.getDate("outdate_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("outdate_e"));
            QFilter qFilter = new QFilter("biztime",QCP.large_equals,start).and("biztime",QCP.less_equals,end);
            qFilters.add(qFilter);
        }
        //获取收货客户过滤
        if(filter.getDynamicObject("nckd_customer_q") != null){
            Long pkValue = (Long)filter.getDynamicObject("nckd_customer_q").getPkValue();
            QFilter qFilter = new QFilter("customer",QCP.equals,pkValue);
            qFilters.add(qFilter);
        }

        String sFields = "bizorg.name AS bizorg," +
                //物料编码
                "billentry.material.masterid.number as material," +
                //物料名称
                "billentry.material.masterid.name as materialname," +
                //存货规格
                "billentry.material.masterid.modelnum as materialmodel," +
                //出库日期
                "biztime," +
                //出库单号
                "billno," +
                //收货单位
                "customer.name as customer," +
                //实出数量
                "billentry.qty as qty," +
                //签收数量
                "billentry.nckd_signqty as nckd_signqty," +
                //含税单价
                "billentry.priceandtax as priceandtax," +
                //无税金额
                "billentry.amount as amount," +
                //价税合计
                "billentry.amountandtax as amountandtax," +
                //运费结算方式
                "nckd_freighttype as nckd_freighttype," +
                //运输方式
                "nckd_cartype as nckd_cartype," +
                //承运单位
                "nckd_carcustomer.name as nckd_carcustomer," +
                //磅单号
                "nckd_carsysno," +
                //车号
                "nckd_vehicle.name as nckd_vehicle," +
                //车皮号
                "nckd_railwaywagon," +
                //备注
                "comment," +
                //税率
                "billentry.taxrate as taxrate," +
                //税额
                "billentry.taxamount as taxamount," +
                //业务员
                "bizoperator.operatorname as bizoperator," +
                //来源单据行id
                "billentry.srcbillentryid as out_srcbillentryid," +
                //分录id
                "billentry.id as out_entryid";

        DataSet imSalOutBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]), null);
        if(imSalOutBill.isEmpty()){
            return imSalOutBill;
        }
        //关联获取上游单据
        imSalOutBill = this.linkUpBills(imSalOutBill);

        //过滤发货日期
        if(filter.getDate("fhdate_s") != null && filter.getDate("fhdate_e") != null){
            DateTime begin = DateUtil.beginOfDay(filter.getDate("fhdate_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("fhdate_e"));
            imSalOutBill = imSalOutBill.filter("del_bizdate >= to_date('" + begin + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("del_bizdate <= to_date('" + end + "', 'yyyy-MM-dd hh:mm:ss')");
        }
        //关联财务应收单
        List<Long> outEntryId = DataSetToList.getOneToList(imSalOutBill, "out_entryid");
        DataSet finArBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ar_finarbill",
                //购方名称
                "nckd_name1," +
                        //基本数量
                        "entry.e_baseunitqty as e_baseunitqty," +
                        //已开票数量
                        "entry.e_issueinvqty as e_issueinvqty," +
                        //来源单据体id
                        "entry.e_srcentryid as fin_srcentryid",
                new QFilter[]{new QFilter("entry.e_srcentryid" ,QCP.in,outEntryId.toArray(new Long[0]))}, null);

        imSalOutBill = imSalOutBill.leftJoin(finArBill).on("out_entryid","fin_srcentryid")
                .select(imSalOutBill.getRowMeta().getFieldNames(),new String[]{"nckd_name1","e_issueinvqty","e_baseunitqty"}).finish();

        if (!filter.getString("nckd_kpdw_q").isEmpty()){
            imSalOutBill = imSalOutBill.filter("nckd_name1 like '%"+filter.getString("nckd_kpdw_q")+"%'");
        }

        return imSalOutBill.orderBy(new String[]{"bizorg","biztime","material"});
    }

    //关联上游单据单据
    public DataSet linkUpBills(DataSet ds) {
        //获取销售出库来源单据行id
        List<Long> outSrcbillentryid = DataSetToList.getOneToList(ds, "out_srcbillentryid");
        if (outSrcbillentryid.isEmpty()) {
            return ds;
        }
        //查询上游电子磅单
        DataSet nckdEleweighing = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_eleweighing",
                //来源单据行id
                "entryentity.nckd_srcbillentryid as ele_srcbillentryid," +
                        //单据体id
                        "entryentity.id as ele_entryentityid ",
                new QFilter[]{new QFilter("entryentity.id", QCP.in, outSrcbillentryid.toArray(new Long[0]))}, null);

        //获取电子磅单来源单据行id
        List<Long> eleSrcbillentryid = DataSetToList.getOneToList(nckdEleweighing, "ele_srcbillentryid");
        //查询上游发货通知单
        DataSet smDelivernotice = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sm_delivernotice",
                //来源单据行id
                "billentry.srcbillentryid as del_srcbillentryid," +
                        //发货日期
                        "bizdate as del_bizdate," +
                        //发货单号
                        "billno as del_billno," +
                        //单据体id
                        "billentry.id as del_billentryid ",
                new QFilter[]{new QFilter("billentry.id", QCP.in, eleSrcbillentryid.toArray(new Long[0]))}, null);
        //电子磅单关联发货申请单
        nckdEleweighing = nckdEleweighing.leftJoin(smDelivernotice).on("ele_srcbillentryid", "del_billentryid").select(nckdEleweighing.getRowMeta().getFieldNames(), smDelivernotice.getRowMeta().getFieldNames()).finish();

        //获取发货申请单来源单据行id
        List<Long> delSrcbillentryid = DataSetToList.getOneToList(smDelivernotice, "del_srcbillentryid");
        //查询销售订单
        DataSet smSalorder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sm_salorder",
                //订单号
                "billno as order_billno," +
                        //单据体id
                        "billentry.id as order_billentryid ",
                new QFilter[]{new QFilter("billentry.id", QCP.in, delSrcbillentryid.toArray(new Long[0]))}, null);
        //关联销售订单
        nckdEleweighing = nckdEleweighing.leftJoin(smSalorder).on("del_srcbillentryid", "order_billentryid").select(nckdEleweighing.getRowMeta().getFieldNames(), smSalorder.getRowMeta().getFieldNames()).finish();

        //销售出库单关联电子磅单
        ds = ds.leftJoin(nckdEleweighing).on("out_srcbillentryid", "ele_entryentityid")
                .select(ds.getRowMeta().getFieldNames(),new String[]{"ele_entryentityid","order_billno","del_bizdate","del_billno"}).finish();

        nckdEleweighing.close();
        smDelivernotice.close();
        smSalorder.close();
        return ds;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("bizorg",ReportColumn.TYPE_TEXT,"公司"));
        columns.add(createReportColumn("material",ReportColumn.TYPE_TEXT,"物料编码"));
        columns.add(createReportColumn("materialname",ReportColumn.TYPE_TEXT,"物料名称"));
        columns.add(createReportColumn("materialmodel",ReportColumn.TYPE_TEXT,"规格型号"));
        columns.add(createReportColumn("del_bizdate",ReportColumn.TYPE_DATE,"发货日期"));
        columns.add(createReportColumn("biztime",ReportColumn.TYPE_DATE,"出库日期"));
        columns.add(createReportColumn("del_billno",ReportColumn.TYPE_TEXT,"发货单号"));
        columns.add(createReportColumn("billno",ReportColumn.TYPE_TEXT,"出库单号"));
        columns.add(createReportColumn("nckd_name1",ReportColumn.TYPE_TEXT,"开票单位"));
        columns.add(createReportColumn("customer",ReportColumn.TYPE_TEXT,"收货单位"));
        columns.add(createReportColumn("qty",ReportColumn.TYPE_DECIMAL,"实出数量"));
        columns.add(createReportColumn("ljts",ReportColumn.TYPE_DECIMAL,"累计途损数量"));
        columns.add(createReportColumn("nckd_signqty",ReportColumn.TYPE_DECIMAL,"实收数量"));
        columns.add(createReportColumn("priceandtax",ReportColumn.TYPE_DECIMAL,"含税单价"));
        columns.add(createReportColumn("amount",ReportColumn.TYPE_DECIMAL,"无税金额"));
        columns.add(createReportColumn("amountandtax",ReportColumn.TYPE_DECIMAL,"价税合计"));
        columns.add(createReportColumn("nckd_freighttype",ReportColumn.TYPE_COMBO,"运费结算方式"));
        columns.add(createReportColumn("nckd_cartype",ReportColumn.TYPE_COMBO,"运输方式"));
        columns.add(createReportColumn("nckd_carcustomer",ReportColumn.TYPE_TEXT,"承运单位"));
        columns.add(createReportColumn("nckd_carsysno",ReportColumn.TYPE_TEXT,"磅单号"));
        columns.add(createReportColumn("nckd_vehicle",ReportColumn.TYPE_TEXT,"车号"));
        columns.add(createReportColumn("nckd_railwaywagon",ReportColumn.TYPE_TEXT,"车皮号"));
        columns.add(createReportColumn("comment",ReportColumn.TYPE_TEXT,"备注"));
        columns.add(createReportColumn("e_issueinvqty",ReportColumn.TYPE_DECIMAL,"已开票数量"));
        columns.add(createReportColumn("e_baseunitqty",ReportColumn.TYPE_DECIMAL,"未开票数量"));
        columns.add(createReportColumn("taxrate",ReportColumn.TYPE_DECIMAL,"税率"));
        columns.add(createReportColumn("taxamount",ReportColumn.TYPE_DECIMAL,"税额"));
        columns.add(createReportColumn("order_billno",ReportColumn.TYPE_TEXT,"订单号"));
        columns.add(createReportColumn("bizoperator",ReportColumn.TYPE_TEXT,"业务员"));

        return columns;
    }

    public ReportColumn createReportColumn(String fieldKey, String fieldType, String caption) {
        if (Objects.equals(fieldType, ReportColumn.TYPE_COMBO)) {
            ComboReportColumn column = new ComboReportColumn();
            column.setFieldKey(fieldKey);
            column.setFieldType(fieldType);
            column.setCaption(new LocaleString(caption));
            List<ValueMapItem> comboItems = new ArrayList<>();
            if (Objects.equals(fieldKey, "nckd_cartype")) {
                comboItems.add(new ValueMapItem("", "A", new LocaleString("船运")));
                comboItems.add(new ValueMapItem("", "B", new LocaleString("汽运")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("火车")));
            } else if (Objects.equals(fieldKey, "nckd_freighttype")){
                comboItems.add(new ValueMapItem("", "A", new LocaleString("出厂")));
                comboItems.add(new ValueMapItem("", "B", new LocaleString("一票")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("出口CNF")));
                comboItems.add(new ValueMapItem("", "C", new LocaleString("出口FOB")));
            }
            column.setComboItems(comboItems);
            return column;
        }
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