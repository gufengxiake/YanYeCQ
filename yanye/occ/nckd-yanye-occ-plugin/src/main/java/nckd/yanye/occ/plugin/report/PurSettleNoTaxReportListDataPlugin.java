package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.scm.bid.common.util.DateUtils;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 采购结算价格（不含税）-报表取数插件
 * 表单标识：nckd_pursettlenotax_rpt
 * author:zzl
 * date:2024/09/03
 */
public class PurSettleNoTaxReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        List<FilterItemInfo> filterItems = reportQueryParam.getFilter().getFilterItems();
        for (FilterItemInfo filterItem : filterItems) {
            switch (filterItem.getPropName()){
                //公司
                case "nckd_bizorg_q":
                    if (filterItem.getValue() != null){
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("bizorg", QCP.equals , pkValue));
                    }
                    break;
                //物料分类
                case "nckd_materialclass_q":
                    if (filterItem.getValue() != null){
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("billentry.material.masterid.group", QCP.equals , pkValue));
                    }
                    break;
                //根据物料库存信息的masterid过滤物料
                case "nckd_material_q":
                    if (filterItem.getValue() != null){
                        Long pkValue = (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                        qFilters.add(new QFilter("billentry.material.masterid", QCP.equals , pkValue));
                    }
                    break;
                //开始时间
                case "start":
                    if (filterItem.getValue() != null){
                        qFilters.add(new QFilter("biztime", QCP.large_equals , DateUtils.startOfDay(filterItem.getDate())));
                    }
                    break;
                //结束时间
                case "end":
                    if (filterItem.getValue() != null){
                        qFilters.add(new QFilter("biztime", QCP.less_equals , DateUtils.endOfDay(filterItem.getDate())));
                    }
                    break;
            }

        }


        String files =
                //采购组织
                "bizorg as nckd_bizorg," +
                //物料
                "billentry.material.masterid as nckd_material," +
                //基本数量
                "billentry.baseqty as baseqty," +
                //金额
                "billentry.amount as amount";
        DataSet imPurinbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_purinbill", files, qFilters.toArray(new QFilter[0]), null);

        //汇总计算基本数量和金额
        imPurinbill = imPurinbill.groupBy(new String[]{"nckd_bizorg","nckd_material"}).sum("baseqty","sumbaseqty").sum("amount","sumamount").finish();

        //关联物料信息获取物料默认税率
        imPurinbill = this.getMaterialInfo(imPurinbill);

        return imPurinbill.orderBy(imPurinbill.getRowMeta().getFieldNames());
    }

    //获取物料信息
    public DataSet getMaterialInfo(DataSet ds){
        DataSet copy = ds.copy();
        Set<Long> materialid = new HashSet<>();
        while (copy.hasNext()) {
            Row next = copy.next();
            if (next.getLong("nckd_material") != null ){
                materialid.add(next.getLong("nckd_material"));
            }
        }
        if(materialid.isEmpty()) return ds;

        DataSet material = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_material",
                "id," +
                        //物料分组
                        "group," +
                        //默认税率
                        "taxrate.taxrate as nckd_taxrate", new QFilter[]{new QFilter("id", QCP.in, materialid.toArray(new Long[0]))}, null);
        //关联物料信息
        ds = ds.leftJoin(material).on("nckd_material","id").select(ds.getRowMeta().getFieldNames(),new String[]{"group","nckd_taxrate"}).finish();

        return ds;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        ReportColumn sumbaseqty = createReportColumn("sumbaseqty", ReportColumn.TYPE_DECIMAL, "累计结算数量");
        ReportColumn sumamount = createReportColumn("sumamount", ReportColumn.TYPE_DECIMAL, "累计本位币结算金额");
        ReportColumn notaxprice = createReportColumn("notaxprice", ReportColumn.TYPE_DECIMAL, "结算无税价");
        ReportColumn taxprice = createReportColumn("taxprice", ReportColumn.TYPE_DECIMAL, "含税价");

        columns.add(sumbaseqty);
        columns.add(sumamount);
        columns.add(notaxprice);
        columns.add(taxprice);

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