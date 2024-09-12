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
 * 华康交易客户大表-报表取数插件
 * 表单标识：nckd_hktradecustomer_rpt
 * author:zhangzhilong
 * date:2024/09/03
 */
public class HKTradeCustomerReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        List<FilterItemInfo> filterItems = reportQueryParam.getFilter().getFilterItems();
        //过滤源头不是要货订单的销售出库
        qFilters.add(new QFilter("billentry.mainbillentity", QCP.equals,"ocbsoc_saleorder"));
        //限定单据为已审核
        qFilters.add(new QFilter("billstatus", QCP.equals, "C"));
        for (FilterItemInfo filterItem : filterItems) {
            switch (filterItem.getPropName()) {
                //组织
                case "nckd_bizorg_q":
                    if (filterItem.getValue() != null) {
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("bizorg", QCP.equals, pkValue));
                    }
                    break;
                //客户
                case "nckd_customer_q":
                    if (filterItem.getValue() != null) {
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("customer", QCP.equals, pkValue));
                    }
                    break;
                //开始时间
                case "start":
                    if (filterItem.getValue() != null) {
                        qFilters.add(new QFilter("biztime", QCP.large_equals, DateUtil.beginOfDay(filterItem.getDate())));
                    }
                    break;
                //结束时间
                case "end":
                    if (filterItem.getValue() != null) {
                        qFilters.add(new QFilter("biztime", QCP.less_equals, DateUtil.endOfDay(filterItem.getDate())));

                    }
                    break;
            }
        }

                //销售组织
        String files = "bizorg as nckd_bizorg," +
                //客户
                "customer as nckd_customer," +
                //开票名称
                "nckd_name1," +
                //纳税人识别号
                "nckd_nashuitax," +
                //备注
                "billentry.entrycomment as nckd_entrycomment," +
                //物料分组
                "billentry.material.masterid.group.name as nckd_materialgroup," +
                //基本数量
                "billentry.baseqty as baseqty," +
                //价税合计
                "billentry.amountandtax as amountandtax," +
                //核心单据行ID
                "billentry.mainbillentryid as mainbillentryid" ;
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", files, qFilters.toArray(new QFilter[0]), null);
        imSaloutbill = this.getSaleOrder(imSaloutbill);
        //判断查询出来的数据是否为空
        if(imSaloutbill.isEmpty()){
            return imSaloutbill;
        }

        //根据组织，客户，开票名称，纳税人识别号，省市区，详细地址，电话，联系人进行数量和价税合计的汇总
        imSaloutbill = imSaloutbill.groupBy(new String[]{"nckd_bizorg", "nckd_customer", "nckd_name1", "nckd_nashuitax", "nckd_entrycomment", "nckd_entryaddressid", "nckd_entrydetailaddress", "nckd_entrytelephone", "nckd_entrycontactname"})
                .sum("case when nckd_materialgroup = '小包盐' then baseqty else 0 end", "sumxbybaseqty")
                .sum("case when nckd_materialgroup = '深井盐' then baseqty else 0 end", "sumsjybaseqty")
                .sum("case when nckd_materialgroup = '小包盐' then amountandtax else 0 end", "sumxbyamountandtax")
                .sum("case when nckd_materialgroup = '深井盐' then amountandtax else 0 end", "sumsjyamountandtax")
                .sum("case when nckd_materialgroup = '非盐' then amountandtax else 0 end", "sumfyamountandtax")
                .sum("case when nckd_materialgroup = '大包盐' then baseqty else 0 end", "sumdbybaseqty")
                .finish();
//        imSaloutbill = imSaloutbill.groupBy(new String[]{"nckd_bizorg", "nckd_customer", "nckd_name1", "nckd_nashuitax", "nckd_entrycomment", "nckd_entryaddressid", "nckd_entrydetailaddress", "nckd_entrytelephone", "nckd_entrycontactname"})
//                .sum("case when nckd_materialgroup = '高端' then baseqty else 0 end", "sumxbybaseqty")
//                .sum("case when nckd_materialgroup = '产成品' then baseqty else 0 end", "sumsjybaseqty")
//                .sum("case when nckd_materialgroup = '高端' then amountandtax else 0 end", "sumxbyamountandtax")
//                .sum("case when nckd_materialgroup = '产成品' then amountandtax else 0 end", "sumsjyamountandtax")
//                .sum("case when nckd_materialgroup = '待分类' then amountandtax else 0 end", "sumfyamountandtax")
//                .sum("case when nckd_materialgroup = '待分类' then baseqty else 0 end", "sumdbybaseqty")
//                .finish();
        return imSaloutbill.orderBy(new String[]{"nckd_bizorg"});
    }

    //获取要货订单信息
    public DataSet getSaleOrder(DataSet ds){
        List<Long> mainbillentryidToList = DataSetToList.getMainbillentryidToList(ds);
        if (mainbillentryidToList.isEmpty()) return ds;

        //取要货订单交付明细主键，
        String sFields = "itementry.id as fdetailid," +
                //省市区，
                "itementry.entryaddressid as nckd_entryaddressid ," +
                //详细地址，
                "itementry.entrydetailaddress as nckd_entrydetailaddress," +
                //电话，
                "itementry.entrytelephone as nckd_entrytelephone," +
                //收货人
                "itementry.entrycontactname as nckd_entrycontactname";
        QFilter qFilter = new QFilter("itementry.id" ,QCP.in , mainbillentryidToList.toArray(new Long[0]));
        DataSet saleOrder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", sFields,
                new QFilter[]{qFilter},null);

        //销售出库关联要货订单
        ds = ds.leftJoin(saleOrder).on("mainbillentryid","fdetailid").select(ds.getRowMeta().getFieldNames(),saleOrder.getRowMeta().getFieldNames()).finish();
        return ds;
    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
//        ReportColumn nckd_materialgroup = createReportColumn("nckd_materialgroup", ReportColumn.TYPE_TEXT, "分类");
        ReportColumn sumxbybaseqty = createReportColumn("sumxbybaseqty", ReportColumn.TYPE_DECIMAL, "小包盐销量");
        ReportColumn sumxbyamountandtax = createReportColumn("sumxbyamountandtax", ReportColumn.TYPE_DECIMAL, "小包交易金额");
        ReportColumn sumsjybaseqty = createReportColumn("sumsjybaseqty", ReportColumn.TYPE_DECIMAL, "深井盐销量");
        ReportColumn sumsjyamountandtax = createReportColumn("sumsjyamountandtax", ReportColumn.TYPE_DECIMAL, "深井盐交易金额");
        ReportColumn sjyavgprice = createReportColumn("sjyavgprice", ReportColumn.TYPE_DECIMAL, "深井盐均价");
        ReportColumn sumfyamountandtax = createReportColumn("sumfyamountandtax", ReportColumn.TYPE_DECIMAL, "非盐交易金额");
        ReportColumn sumdbybaseqty = createReportColumn("sumdbybaseqty", ReportColumn.TYPE_DECIMAL, "大包盐销量");

//        columns.add(nckd_materialgroup);
        columns.add(sumxbybaseqty);
        columns.add(sumxbyamountandtax);
        columns.add(sumsjybaseqty);
        columns.add(sumsjyamountandtax);
        columns.add(sjyavgprice);
        columns.add(sumfyamountandtax);
        columns.add(sumdbybaseqty);


        return columns;
    }


    public ReportColumn createReportColumn(String fieldKey, String fieldType, String caption) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fieldKey);
        column.setFieldType(fieldType);
        column.setCaption(new LocaleString(caption));
        if (fieldType.equals(ReportColumn.TYPE_DECIMAL)) {
            //精度
            column.setScale(2);
            //是否显示0
//            column.setZeroShow(true);
        }

        return column;
    }
}