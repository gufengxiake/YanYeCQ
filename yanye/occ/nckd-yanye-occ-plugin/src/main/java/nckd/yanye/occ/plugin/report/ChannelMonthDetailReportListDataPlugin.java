package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateTime;
import com.ccb.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 报表取数插件
 */
public class ChannelMonthDetailReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        //限定为华康及其子公司
        QFilter mainFilter = new QFilter("bizorg.name", QCP.like, "%华康%");
        //限定单据为已审核
        mainFilter.and("billstatus", QCP.equals, "C");
        qFilters.add(mainFilter);

        //获取年份，默认今年
        int year = DateUtil.year(new Date());
        Date nckdDateQ = reportQueryParam.getFilter().getDate("nckd_date_q");
        //根据选择的查询条件进行组织过滤
        if (nckdDateQ != null) {
            DateTime strat = DateUtil.beginOfDay(nckdDateQ);
            DateTime end = DateUtil.endOfDay(nckdDateQ);
            QFilter dateFilter = new QFilter("biztime", QCP.large_equals, strat);
            dateFilter.and("biztime", QCP.less_equals, end);
            qFilters.add(dateFilter);
            year = DateUtil.year(nckdDateQ);
        }else{
            //不选默认今年
            QFilter qFilter = new QFilter("biztime", QCP.large_equals, DateUtil.beginOfYear(new Date()))
                    .and("biztime", QCP.less_equals, DateUtil.endOfYear(new Date()));
            qFilters.add(qFilter);
        }
        //公司
        String outFields = "bizorg AS out_bizorg," +
                //公司名称
                "bizorg.name AS out_bizorgname," +
                //业务日期
                "biztime as out_biztime," +
                //物料分组
                "billentry.material.masterid.group.name as out_group," +
                //基本数量
                "billentry.baseqty as out_baseqty," +
                //价税合计
                "billentry.amountandtax as out_amountandtax" ;
        //查询销售出库单
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", outFields, qFilters.toArray(new QFilter[0]), null);
        DataSet sumYear = null,sumMonth = null;
        //汇总当年数量和价税合计
        sumYear = im_saloutbill.copy().groupBy(new String[]{"out_bizorg","out_bizorgname","out_group"})
                .sum("out_baseqty","yearsum").sum("out_amountandtax","yearamountandtax")
                .finish().addField("0","yearmonth");
        //循环汇总每个月份的数量和价税合计
        for (int i = 1; i < 13; i++) {
            //拼接年月份
            String date = year + "-" + i ;
            //获取月份开始日期
            DateTime s = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
            //获取月份结束日期
            DateTime e = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
            //过滤一个月的数据
            sumYear = im_saloutbill.copy().filter("out_biztime >=to_date('" + s + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("out_biztime <=to_date('" + e + "','yyyy-MM-dd hh:mm:ss')").addField(String.valueOf(i),"yearmonth");
            if(i == 1){
                sumMonth = sumYear;
            }else{
                sumMonth = sumMonth.union(sumYear);
            }
//            //汇总一个月的数量和价税合计
//            sumMonth = sumMonth.groupBy(new String[]{"out_bizorg","out_bizorgname","out_group","yearmonth"+i})
//                    .sum("out_baseqty","sum"+i).sum("out_amountandtax","amountandtax"+i).finish();
//            sumYear = sumYear.leftJoin(sumMonth).on("out_bizorg","out_bizorg").select(sumYear.getRowMeta().getFieldNames(),new String[]{"yearmonth"+i,"sum"+i,"amountandtax"+i}).finish();
        }
        GroupbyDataSet groupbyDataSet = sumMonth.groupBy(new String[]{"out_bizorg", "out_bizorgname", "out_group"});
        for (int i = 1; i < 13; i++) {
            groupbyDataSet.sum("case when yearmonth = "+i+" then out_baseqty else 0 end" , "sum"+i)
                    .sum("case when yearmonth = "+i+" then out_amountandtax else 0 end" , "amountandtax"+i);
        }
        sumYear = groupbyDataSet.finish();

        GroupbyDataSet groupbyDataSet1 = sumYear.groupBy(new String[]{"out_group"});
        for (int i = 1; i < 13; i++) {
            groupbyDataSet1.sum("sum"+i).sum("amountandtax"+i);
        }
        sumMonth = groupbyDataSet1.finish().addField("'合计'" , "out_bizorgname")
                .addNullField(new String[]{"out_bizorg"})
                .select(sumYear.getRowMeta().getFieldNames());

        sumYear =sumYear.union(sumMonth);
        sumMonth.close();
        im_saloutbill.close();

        return sumYear.orderBy(new String[]{"out_group","out_bizorg desc" });
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("out_bizorgname",ReportColumn.TYPE_TEXT,"公司名称"));
        columns.add(createReportColumn("out_group",ReportColumn.TYPE_TEXT,"属性"));
        for (int i = 0; i < 13; i++) {
            if(i == 0){
                //创建年分组
                ReportColumnGroup qtyReportColumnGroup = new ReportColumnGroup();
                qtyReportColumnGroup.setFieldKey("qty");
                qtyReportColumnGroup.setCaption(new LocaleString("全年"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("yearjh",ReportColumn.TYPE_TEXT,"年计划"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("yearsum",ReportColumn.TYPE_DECIMAL,"全年累计"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("yearwcl",ReportColumn.TYPE_TEXT,"年完成率"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("yearamountandtax",ReportColumn.TYPE_DECIMAL,"全年均价"));
                columns.add(qtyReportColumnGroup);
            }else{
                //创建月份分组
                ReportColumnGroup qtyReportColumnGroup = new ReportColumnGroup();
                qtyReportColumnGroup.setFieldKey("qty");
                qtyReportColumnGroup.setCaption(new LocaleString(i+"月"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("monthjh"+i,ReportColumn.TYPE_TEXT,"计划"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("sum"+i,ReportColumn.TYPE_DECIMAL,"销售"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("monthwcl"+i,ReportColumn.TYPE_TEXT,"完成率"));
                qtyReportColumnGroup.getChildren().add(createReportColumn("amountandtax"+i,ReportColumn.TYPE_DECIMAL,i+"月均价"));
                columns.add(qtyReportColumnGroup);
            }

        }
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