package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import kd.bos.algo.*;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 重点盐种价格情况表-报表取数插件
 * 表单标识：nckd_keysaltprice_rpt
 * author:zhangzhilong
 * date:2024/09/04
 */
public class KeySaltPriceReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    Map<Long, String> material = new HashMap<>();

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //限定源头是要货订单的销售出库单
        QFilter mainFilter = new QFilter("billentry.mainbillentity", QCP.equals, "ocbsoc_saleorder");
        //限定单据为已审核
        mainFilter.and("billstatus", QCP.equals, "C");
        //限定组织为华康及其分公司
        QFilter orgFilter = new QFilter("bizorg.name",  QCP.like,"%华康%");
        //限定物料为重点物料
        QFilter materialFilter = new QFilter("billentry.material.masterid.nckd_iskey", QCP.equals, "1");
        qFilters.add(mainFilter);
        qFilters.add(orgFilter);
        qFilters.add(materialFilter);

        //多选组织
        FilterInfo dFilters = reportQueryParam.getFilter();
        DynamicObjectCollection nckdBizorgQd = dFilters.getDynamicObjectCollection("nckd_bizorg_qd");
        List<Long> orgIds = new ArrayList<>();
        if(nckdBizorgQd!=null ){
            nckdBizorgQd.forEach((e)->{
                orgIds.add(e.getLong("id"));
            });
        }
        if(!orgIds.isEmpty()){
            qFilters.add(new QFilter("bizorg", QCP.in, orgIds.toArray(new Long[0])));
        }

        //多选物料
        DynamicObjectCollection nckdMaterielQd = dFilters.getDynamicObjectCollection("nckd_materiel_qd");
        List<Long> materialIds = new ArrayList<>();
        if(nckdMaterielQd!=null ){
            nckdMaterielQd.forEach((e)->{
                materialIds.add(e.getLong("id"));
            });
        }
        if(!materialIds.isEmpty()){
            qFilters.add(new QFilter("billentry.material.masterid", QCP.in, materialIds.toArray(new Long[0])));
        }

        //获取年份
        int year = DateUtil.year(new Date());
        if(dFilters.getDate("nckd_date_q")!=null){
            QFilter qFilter = new QFilter("biztime", QCP.large_equals, DateUtil.beginOfYear(dFilters.getDate("nckd_date_q")))
                    .and("biztime", QCP.less_equals, DateUtil.endOfYear(dFilters.getDate("nckd_date_q")));
            qFilters.add(qFilter);
            year = DateUtil.year(dFilters.getDate("nckd_date_q"));
        }else {
            //不选默认今年
            QFilter qFilter = new QFilter("biztime", QCP.large_equals, DateUtil.beginOfYear(new Date()))
                    .and("biztime", QCP.less_equals, DateUtil.endOfYear(new Date()));
            qFilters.add(qFilter);
        }
        //公司
        String sFields = "bizorg AS nckd_bizorg," +
                "bizorg.name as nckd_bizorgname," +

                "biztime," +
//                物料编码
                "billentry.material.masterid AS nckd_material," +
                "billentry.material.masterid.name AS nckd_materialname," +
//                销售出库数
                "billentry.qty AS nckd_qty," +
//                销售出库价税合计
                "billentry.amountandtax as nckd_outamount";


        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", sFields, qFilters.toArray(new QFilter[0]), null);

        //获取物料名称
        for (Row row : im_saloutbill.copy()) {
            material.put(row.getLong("nckd_material"), row.getString("nckd_materialname"));
        }

        //按年月份进行数据过滤并添加期间
        DataSet dataSets = null, ds = null;
        for (int i = 0; i < 12; i++) {
            //获取年月份
            String date = year + "-" + (i + 1);
            DateTime begin = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
            DateTime end = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
            ds = im_saloutbill.filter("biztime >=to_date('" + begin + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("biztime <=to_date('" + end + "','yyyy-MM-dd hh:mm:ss')")
                    .addField(""+(i+1), "nckd_qj");
            if (i >= 1) {
                dataSets = dataSets.union(ds);

            } else {
                dataSets = ds;
            }

        }
        ds.close();

        //汇总物料和金额
        Long[] id = material.keySet().toArray(new Long[0]);
        GroupbyDataSet sumGroup = dataSets
                .groupBy(new String[]{"nckd_bizorg","nckd_bizorgname", "nckd_qj"});
        for (int i = 0; i < id.length; i++) {
            sumGroup.sum("case when nckd_material = " + id[i] + " then nckd_qty else 0 end", id[i] + "qty");
            sumGroup.sum("case when nckd_material = " + id[i] + " then nckd_outamount else 0 end", id[i] + "amount");
        }
        im_saloutbill = sumGroup.finish();

        //获取后续需要添加的金额字段
        ArrayList<String> idAmount = new ArrayList<>();
        ArrayList<String> idSelect = new ArrayList<>();
        idAmount.add("nckd_bizorg");
        idSelect.add("nckd_bizorgname");
        for (Long l : id) {
            idAmount.add(l + "amount");
            idSelect.add(l + "qty");
        }
        Collections.addAll(idSelect,idAmount.toArray(new String[0]));

        DataSet sum = null,filter = null,finish =null;
        //根据月份隔离期间的数据
        for (int i = 0; i < 12; i++) {
            filter = im_saloutbill.copy().filter("nckd_qj = " + (i + 1));
            if(sum == null){
                sum = filter.orderBy(new String[]{"nckd_qj", "nckd_bizorg"});
            }else {
                sum = sum.union(filter.orderBy(new String[]{"nckd_qj", "nckd_bizorg"}));
            }
            GroupbyDataSet groupbyDataSet = filter.groupBy(new String[]{"nckd_qj"});
            for (int j = 0; j < id.length ; j++) {
                groupbyDataSet.sum(id[j] + "qty");
            }

            //添加缺少的字段并根据源数据进行字段顺序排序
            finish = groupbyDataSet.finish();
            finish = finish.addNullField(idAmount.toArray(new String[0])).addField("'总省合计'","nckd_bizorgname")
                    .select(sum.getRowMeta().getFieldNames());
            sum = sum.union(finish);

            //增加一行间隔
            if(!(finish.copy()).isEmpty()) {
                RowMeta rowMeta = finish.getRowMeta();
                DataSetBuilder newDataSetBuilder = Algo.create("test").createDataSetBuilder(rowMeta);
                Object[] values = new Object[rowMeta.getFieldCount()];
                values[1] = "小计间隔";
                newDataSetBuilder.append(values);
                finish = newDataSetBuilder.build();
                sum = sum.union(finish);
            }
        }
        //关闭数据源
        dataSets.close();
        filter.close();
        finish.close();
        im_saloutbill.close();

        return sum;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        ReportColumn nckd_bizorg = createReportColumn("nckd_bizorgname",  ReportColumn.TYPE_TEXT, "公司");
//        nckd_bizorg.setCaption(new LocaleString("公司"));
        columns.add(nckd_bizorg);
        ReportColumn nckd_qj = createReportColumn("nckd_qj", ReportColumn.TYPE_TEXT, "期间");
        columns.add(nckd_qj);

        Long[] id = material.keySet().toArray(new Long[0]);
        String[] array = material.values().toArray(new String[0]);
        if(id.length == 1){
            ReportColumn materialnameqty = createReportColumn(id[0]  + "qty", ReportColumn.TYPE_DECIMAL, array[0]+"的数量");
            ReportColumn materialnameamount = createReportColumn(id[0]  + "amount", ReportColumn.TYPE_DECIMAL, array[0]+"的单价");
            columns.add(materialnameqty);
            columns.add(materialnameamount);

        }else{
            //创建数量分组
            ReportColumnGroup qtyReportColumnGroup = new ReportColumnGroup();
            qtyReportColumnGroup.setFieldKey("qty");
            qtyReportColumnGroup.setCaption(new LocaleString("数量"));
            //创建金额分组（后续会替换成含税单价）
            ReportColumnGroup amountReportColumnGroup = new ReportColumnGroup();
            amountReportColumnGroup.setFieldKey("amount");
            amountReportColumnGroup.setCaption(new LocaleString("含税单价"));
            for (int i = 0; i < array.length; i++) {
                ReportColumn materialnameqty = createReportColumn(id[i]  + "qty", ReportColumn.TYPE_DECIMAL, array[i]);
                qtyReportColumnGroup.getChildren().add(materialnameqty);
                ReportColumn materialnameamount = createReportColumn(id[i]  + "amount", ReportColumn.TYPE_DECIMAL, array[i]);
                amountReportColumnGroup.getChildren().add(materialnameamount);
            }
            columns.add(qtyReportColumnGroup);
            columns.add(amountReportColumnGroup);
        }

        //清空防止误用
        material.clear();

        ReportColumn sumQty = createReportColumn("sumqty", ReportColumn.TYPE_DECIMAL, "数量合计");
        columns.add(sumQty);

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