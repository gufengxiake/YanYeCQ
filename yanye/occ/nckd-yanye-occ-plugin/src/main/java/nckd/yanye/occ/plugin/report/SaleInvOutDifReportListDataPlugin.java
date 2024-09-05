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
 * 发票出库差额全表-报表取数插件
 * 表单标识：nckd_saleinvoutdif_rpt
 * author:zhangzhilong
 * date:2024/08/30
 */
public class SaleInvOutDifReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        //限定源头是要货订单的销售出库单
        QFilter filter = new QFilter("billentry.mainbillentity", QCP.equals,"ocbsoc_saleorder");
        qFilters.add(filter);
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件销售组织,标识如不一致,请修改
                case "nckd_bizorg_q":
                    if (!(filterItem.getValue() == null)) {
                        QFilter qFilter = new QFilter("bizorg", QCP.equals, ((DynamicObject) filterItem.getValue()).getPkValue());
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件销售部门,标识如不一致,请修改
                case "nckd_bizdept_q":
                    if (!(filterItem.getValue() == null)) {
                        QFilter qFilter = new QFilter("bizdept", QCP.equals, ((DynamicObject) filterItem.getValue()).getPkValue());
                        qFilters.add(qFilter);
                    }
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "start":
                    if (!(filterItem.getDate() == null)) {
                        QFilter qFilter = new QFilter("biztime", QCP.large_equals,
                                DateUtil.beginOfDay(filterItem.getDate()));
                        qFilters.add(qFilter);
                    }
                    break;
                case "end":
                    if (!(filterItem.getDate() == null)) {
                        QFilter qFilter = new QFilter("biztime", QCP.less_equals,
                                DateUtil.endOfDay(filterItem.getDate()));
                        qFilters.add(qFilter);
                    }
                    break;

                // 查询条件物料,标识如不一致,请修改
                case "nckd_material_q":
                    if (!(filterItem.getValue() == null)) {
                        QFilter qFilter = new QFilter("billentry.material", QCP.equals, ((DynamicObject) filterItem.getValue()).getPkValue());
                        qFilters.add(qFilter);
                    }
                    break;
            }
        }
        //公司
        String sFields = "bizorg AS nckd_bizorg," +
//                部门
                "bizdept AS nckd_bizdept," +
//                物料编码
                "billentry.material AS nckd_material," +
//                销售出库数
                "billentry.qty AS nckd_qty," +
//                销售出库价税合计
                "billentry.amountandtax as nckd_outamount," +
//                核心单据行id
                "billentry.mainbillentryid as mainbillentryid";

        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]), null);

        im_saloutbill = this.linkOriginal(im_saloutbill);

        return im_saloutbill;

    }

    //关联开票申请单
    public DataSet linkOriginal(DataSet ds){
        List<Long> mainbillentryidToList = DataSetToList.getMainbillentryidToList(ds);
        if (mainbillentryidToList.isEmpty()) return ds;

        QFilter aimFilter = new QFilter("sim_original_bill_item.corebillentryid" , QCP.in , mainbillentryidToList.toArray(new Long[0]));
        DataSet originalBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sim_original_bill",
                //查询开票申请单核心单据行id，
                "sim_original_bill_item.corebillentryid as sim_corebillentryid, " +
//                        开票申请单数量
                        "sim_original_bill_item.num as nckd_invnum ," +
//                        开票申请单金额（不含税）
                        "sim_original_bill_item.taxamount as nckd_invamount" ,new QFilter[]{aimFilter},null);
        ds = ds.leftJoin(originalBill).on("mainbillentryid","sim_corebillentryid")
                .select(ds.getRowMeta().getFieldNames(),originalBill.getRowMeta().getFieldNames()).finish();
        return ds;
    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {

        ReportColumn nckd_qty = createReportColumn("nckd_qty", ReportColumn.TYPE_DECIMAL, "销售出库数");
        ReportColumn nckd_outamount = createReportColumn("nckd_outamount", ReportColumn.TYPE_DECIMAL, "销售出库金额");
        ReportColumn nckd_invnum = createReportColumn("nckd_invnum", ReportColumn.TYPE_DECIMAL, "销售开票数");
        ReportColumn nckd_invamount = createReportColumn("nckd_invamount", ReportColumn.TYPE_DECIMAL, "销售开票金额");
        ReportColumn nckd_outinvqty = createReportColumn("nckd_outinvqty", ReportColumn.TYPE_DECIMAL, "数量差额");
        ReportColumn nckd_outinvamount = createReportColumn("nckd_outinvamount", ReportColumn.TYPE_DECIMAL, "金额差额");

        columns.add(nckd_qty);
        columns.add(nckd_outamount);
        columns.add(nckd_invnum);
        columns.add(nckd_invamount);
        columns.add(nckd_outinvqty);
        columns.add(nckd_outinvamount);
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