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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 销售发票汇总表-报表取数插件
 * 表单标识：nckd_saleinvoicesum_rpt
 * author:zhangzhilong
 * date:2024/09/20
 */
public class SaleInvoiceSumReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {
    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //限定单据状态为已审核
        QFilter initFilter = new QFilter("billstatus", QCP.equals,"C");
        qFilters.add(initFilter);
        FilterInfo filter = reportQueryParam.getFilter();
        //获取组织过滤
        if(filter.getDynamicObject("nckd_org_q") != null){
            Long pkValue = (Long) filter.getDynamicObject("nckd_org_q").getPkValue();
            qFilters.add(new QFilter("bizorg",QCP.equals,pkValue));
        }
        //获取业务员过滤
        if(filter.getDynamicObject("nckd_bizoperator_q") != null){
            Long pkValue = (Long) filter.getDynamicObject("nckd_bizoperator_q").getPkValue();
            qFilters.add(new QFilter("bizoperator",QCP.equals,pkValue));
        }
        //获取客户过滤
        if(filter.getDynamicObject("nckd_customer_q") != null){
            Long pkValue = (Long) filter.getDynamicObject("nckd_customer_q").getPkValue();
            qFilters.add(new QFilter("customer",QCP.equals,pkValue));
        }
        //获取物料过滤
        if(filter.getDynamicObject("nckd_material_q") != null){
            Long pkValue = (Long) filter.getDynamicObject("nckd_material_q").getPkValue();
            qFilters.add(new QFilter("billentry.material.masterid",QCP.equals,pkValue));
        }
        if(filter.getDate("date_s") != null && filter.getDate("date_e") != null){
            DateTime begin = DateUtil.beginOfDay(filter.getDate("date_s"));
            DateTime end = DateUtil.endOfDay(filter.getDate("date_e"));
            qFilters.add(new QFilter("biztime",QCP.large_equals,begin).and("biztime",QCP.less_equals,end));
        }
        String fields =
                //公司
                "bizorg.name as bizorg," +
                //业务员
                "bizoperator.operatorname as  bizoperator," +
                //客户
                "customer as customer," +
                //客户编码
                "customer.number as customernumber," +
                //客户名称
                "customer.name as customername," +
                //物料分类编码
                "billentry.material.masterid.group.number as group," +
                //物料分类
                "billentry.material.masterid.group.name as groupname," +
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
                //参考成本价
                "billentry.nckd_cbdj as nckd_cbdj," +
                //成本金额
                "billentry.nckd_cbj as nckd_cbj";
        DataSet salOutBill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", fields, qFilters.toArray(new QFilter[0]),"biztime");
        if(salOutBill.isEmpty()){
            return salOutBill.orderBy(new String[]{"bizorg","material"});
        }
        //获取客户主键
        List<Long> customer = DataSetToList.getOneToList(salOutBill, "customer");
        DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), "ocdbd_channel",
                "nckd_regiongroup," +
                        //地区分类编码
                        "nckd_regiongroup.number as regiongroupnumber," +
                        //地区分类名称
                        "nckd_regiongroup.name as regiongroupname," +
                        //客户
                        "customer as ocdbd_customer",
                new QFilter[]{new QFilter("customer", QCP.in, customer.toArray(new Long[0]))}, null);

        salOutBill = salOutBill.leftJoin(dataSet).on("customer","ocdbd_customer").select(salOutBill.getRowMeta().getFieldNames(),new String[]{"nckd_regiongroup","regiongroupnumber","regiongroupname"}).finish();
        //获取地区分类过滤
        if(filter.getDynamicObject("nckd_regiongroup_q") != null){
            Long pkValue = (Long) filter.getDynamicObject("nckd_regiongroup_q").getPkValue();
            salOutBill = salOutBill.filter("nckd_regiongroup = "+pkValue);
        }

        return salOutBill.orderBy(new String[]{"bizorg","material"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {

        columns.add(createReportColumn("bizorg",ReportColumn.TYPE_TEXT,"公司"));
        columns.add(createReportColumn("bizoperator",ReportColumn.TYPE_TEXT,"业务员"));
        columns.add(createReportColumn("regiongroupnumber",ReportColumn.TYPE_TEXT,"地区编码"));
        columns.add(createReportColumn("regiongroupname",ReportColumn.TYPE_TEXT,"地区"));
        columns.add(createReportColumn("customernumber",ReportColumn.TYPE_TEXT,"客户编码"));
        columns.add(createReportColumn("customername",ReportColumn.TYPE_TEXT,"客户名称"));
        columns.add(createReportColumn("group",ReportColumn.TYPE_TEXT,"物料分类编码"));
        columns.add(createReportColumn("groupna",ReportColumn.TYPE_TEXT,"物料分类"));
        columns.add(createReportColumn("material",ReportColumn.TYPE_TEXT,"物料编码"));
        columns.add(createReportColumn("materialname",ReportColumn.TYPE_TEXT,"物料名称"));
        columns.add(createReportColumn("lotnumber",ReportColumn.TYPE_TEXT,"批次"));
        columns.add(createReportColumn("ispresent",ReportColumn.TYPE_BOOLEAN,"赠品"));
        columns.add(createReportColumn("baseqty",ReportColumn.TYPE_DECIMAL,"数量"));
        columns.add(createReportColumn("priceandtax",ReportColumn.TYPE_DECIMAL,"含税单价"));
        columns.add(createReportColumn("curamount",ReportColumn.TYPE_DECIMAL,"金额（本位币）"));
        columns.add(createReportColumn("curtaxamount",ReportColumn.TYPE_DECIMAL,"税额（本位币）"));
        columns.add(createReportColumn("curamountandtax",ReportColumn.TYPE_DECIMAL,"价税合计（本位币）"));
        columns.add(createReportColumn("discountamount",ReportColumn.TYPE_DECIMAL,"折扣额（本位币）"));
        columns.add(createReportColumn("taxamount",ReportColumn.TYPE_DECIMAL,"原币税额"));
        columns.add(createReportColumn("amount",ReportColumn.TYPE_DECIMAL,"原币无税金额"));
        columns.add(createReportColumn("amountandtax",ReportColumn.TYPE_DECIMAL,"原币价税合计"));
        columns.add(createReportColumn("nckd_cbdj",ReportColumn.TYPE_DECIMAL,"参考成本价"));
        columns.add(createReportColumn("nckd_cbj",ReportColumn.TYPE_DECIMAL,"成本金额"));
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