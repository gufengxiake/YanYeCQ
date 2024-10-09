package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateTime;
import com.ccb.core.date.DateUtil;
import kd.bos.algo.DataSet;
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
 * 渠道月度销售情况（汇总表）-报表取数插件
 * 表单标识：nckd_channelmonthsale_rpt
 * author:zhangzhilong
 * date:2024/09/11
 */
public class ChannelMonthSaleReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

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
            DateTime strat = DateUtil.beginOfYear(nckdDateQ);
            DateTime end = DateUtil.endOfYear(nckdDateQ);
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
                //基本数量
                "billentry.baseqty as out_baseqty," +
                //价税合计
                "billentry.amountandtax as out_amountandtax" ;
        //查询销售出库单
        DataSet imSalOutBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", outFields, qFilters.toArray(new QFilter[0]), null);
        DataSet sumYear = null,sumMonth = null;
        //汇总当年数量和价税合计
        sumYear = imSalOutBill.copy().groupBy(new String[]{"out_bizorg","out_bizorgname"})
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
            sumMonth = imSalOutBill.copy().filter("out_biztime >=to_date('" + s + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("out_biztime <=to_date('" + e + "','yyyy-MM-dd hh:mm:ss')").addField(String.valueOf(i),"yearmonth"+i);
            //汇总一个月的数量和价税合计
            sumMonth = sumMonth.groupBy(new String[]{"out_bizorg","out_bizorgname","yearmonth"+i})
                    .sum("out_baseqty","sum"+i).sum("out_amountandtax","amountandtax"+i).finish();
            sumYear = sumYear.leftJoin(sumMonth).on("out_bizorg","out_bizorg").select(sumYear.getRowMeta().getFieldNames(),new String[]{"yearmonth"+i,"sum"+i,"amountandtax"+i}).finish();
        }
        sumMonth.close();
        imSalOutBill.close();
        return sumYear.orderBy(new String[]{"out_bizorg"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("out_bizorgname",ReportColumn.TYPE_TEXT,"公司名称"));
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