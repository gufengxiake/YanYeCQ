package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
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
 * 每日小包装盐销量-报表取数插件
 * 表单标识：nckd_everydaysaltsale_rpt
 * author:zhangzhilong
 * date:2024/08/29
 */
public class EveryDaySaltSaleReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        //限定源头是要货订单的销售出库单
        QFilter filter = new QFilter("billentry.mainbillentity", QCP.equals,"ocbsoc_saleorder");
        QFilter filterOrg = new QFilter("bizorg.name",  QCP.like,"%华康%");
        //限定单据为已审核
        filterOrg.and("billstatus", QCP.equals, "C");
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件收货单位,标识如不一致,请修改
                case "nckd_org_q":
                    if (filterItem.getValue() != null) {
                        Long nckd_org_q = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        filter.and("bizorg", QCP.equals, nckd_org_q);
                    }
                    break;
                // 查询条件发货日期,标识如不一致,请修改
                case "nckd_date_q":
                    if (filterItem.getDate() != null) {
                        filter.and("biztime",QCP.equals,DateUtil.beginOfDay(filterItem.getDate()));

                    }
                    break;
            }
        }

        String sFields =
                //销售组织
                "bizorg as nckd_bizorg ," +
//                        物料分类
                        "billentry.material.masterid.group.name as materialgroup ," +
//                        数量
                        "billentry.qty as qty," +
//                        收货客户
                        "customer as customer," +
//                        销售员
                        "bizoperator as bizoperator" ;
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", sFields, new QFilter[]{filter,filterOrg} , null);

        //计算同组织下不同收货客户的数量
        DataSet im_saloutbill_customer = im_saloutbill.filter("customer <> 0").groupBy(new String[]{"nckd_bizorg" , "customer"}).finish()
                .groupBy(new String[]{"nckd_bizorg"}).count("customer").finish().select("nckd_bizorg as customerorg","customer");
        //计算同组织不同销售员的数量
        DataSet im_saloutbill_bizoperator= im_saloutbill.filter("bizoperator <> 0").groupBy(new String[]{"nckd_bizorg" , "bizoperator"}).finish()
                .groupBy(new String[]{"nckd_bizorg"}).count("bizoperator").finish().select("nckd_bizorg as bizoperatororg","bizoperator");

        im_saloutbill =  im_saloutbill.groupBy(new String[]{"nckd_bizorg"})
                .sum("CASE WHEN materialgroup <> '竞品' THEN qty ELSE 0 END ","xl" )
                .sum("CASE WHEN materialgroup = '高端' THEN qty ELSE 0 END ","gd" )
                .sum("CASE WHEN materialgroup = '竞品' THEN qty ELSE 0 END ","jp" ).finish();

        //关联取同组织不同客户的数量
        im_saloutbill = im_saloutbill.leftJoin(im_saloutbill_customer).on("nckd_bizorg","customerorg")
                .select(im_saloutbill.getRowMeta().getFieldNames(),new String[]{"customer"}).finish();

        //关联取同组织不同销售员的数量
        im_saloutbill = im_saloutbill.leftJoin(im_saloutbill_bizoperator).on("nckd_bizorg","bizoperatororg")
                .select(im_saloutbill.getRowMeta().getFieldNames(),new String[]{"bizoperator"}).finish();

        return im_saloutbill;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
//        ReportColumn bizorg = ReportColumn.createBaseDataColumn("bizorg","bos_org");
        columns.add(createReportColumn("xl", ReportColumn.TYPE_DECIMAL, "销量"));

        columns.add(createReportColumn("gd", ReportColumn.TYPE_DECIMAL, "高端"));
        columns.add(createReportColumn("jp", ReportColumn.TYPE_DECIMAL, "竞品"));

        columns.add(createReportColumn("customer", ReportColumn.TYPE_TEXT, "盐客户数"));
        columns.add(createReportColumn("bizoperator", ReportColumn.TYPE_TEXT, "业务员数"));

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