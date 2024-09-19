package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateTime;
import com.ccb.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 华康各公司销售同比表（大包食用盐）-报表取数插件
 * 表单标识：
 * author:zhangzhilong
 * date:2024/09/18
 */
public class HKSaleYearOnYearReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    private final String[] groupName = {"小包盐","深井盐","大包食用盐","其它大包盐","非盐"};

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //销售组织限定为华康
        QFilter initFilter = new QFilter("bizorg.name", QCP.like, "%华康%");
        //限定单据状态为已审核
        initFilter.and("billstatus", QCP.equals, "C");
        qFilters.add(initFilter);
        FilterInfo filter = reportQueryParam.getFilter();
        //获取多选组织信息
        DynamicObjectCollection nckdOrgQ = filter.getDynamicObjectCollection("nckd_org_q");
        ArrayList<Long> bizorg = new ArrayList<>();
        if (nckdOrgQ != null) {
            nckdOrgQ.forEach((e) -> {
                bizorg.add((Long) e.getPkValue());
            });
        }
        if (!bizorg.isEmpty()) {
            QFilter orgFilter = new QFilter("bizorg", QCP.in, bizorg.toArray(new Long[0]));
            qFilters.add(orgFilter);
        }
        //获取查询年份
        Date nckdYearQ = filter.getDate("nckd_year_q");
        String nckdMonthQ = filter.getString("nckd_month_q");
        //获取本年过滤时间
        QFilter thisYearMonth = getThisAndLastYearMonth(nckdYearQ, nckdMonthQ, false);
        qFilters.add(thisYearMonth);
        //本年数据
        DataSet thisYearImSalOutBill = this.getImSalOutBill(qFilters).addField("0","year");

        qFilters.remove(thisYearMonth);

        //获取去年过滤时间
        qFilters.add(getThisAndLastYearMonth(nckdYearQ, nckdMonthQ, true));
        DataSet lastYearImSalOutBill = this.getImSalOutBill(qFilters).addField("1","year");
        //合并汇总
        GroupbyDataSet groupbyDataSet = thisYearImSalOutBill.union(lastYearImSalOutBill).groupBy(new String[]{"bizorg", "bizorgname"});

        for (int i = 0 ; i < groupName.length ; i ++){
            groupbyDataSet.sum("case when groupname = '"+groupName[i]+"' and year = 0 then amount else 0 end","thisYearAmount"+groupName[i])
                    .sum("case when groupname = '"+groupName[i]+"' and year = 1 then amount else 0 end","lastYearAmount"+groupName[i])
                    .sum("case when groupname = '"+groupName[i]+"' and year = 0 then qty else 0 end","thisYearQty"+groupName[i])
                    .sum("case when groupname = '"+groupName[i]+"' and year = 1 then qty else 0 end","lastYearQty"+groupName[i])
                    .sum("case when groupname = '"+groupName[i]+"' and year = 0 then amount - nckd_cbj else 0 end","thisYearML"+groupName[i])
                    .sum("case when groupname = '"+groupName[i]+"' and year = 1 then amount - nckd_cbj else 0 end","lastYearML"+groupName[i]);
        }
        DataSet finish = groupbyDataSet.finish();
        return finish.orderBy(new String[]{"bizorgname"});
    }

    public DataSet getImSalOutBill(List<QFilter> qFilters) {
        //销售组织
        String fields = "bizorg as bizorg," +
                //销售组织名称
                "bizorg.name as bizorgname," +
                //物料分类
                "billentry.material.masterid.group as group," +
                //物料分类名称
                "billentry.material.masterid.group.name as groupname," +
                //金额
                "billentry.amount as amount," +
                //成本金额
                "billentry.nckd_cbj as nckd_cbj," +
                //数量
                "billentry.qty as qty";
        DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", fields, qFilters.toArray(new QFilter[0]), null);
        return dataSet;
    }

    public QFilter getThisAndLastYearMonth(Date dateYear, String dateMonth,Boolean isLastYear) throws ParseException {
        int year = DateUtil.year(dateYear);
        String date = year + "-" + dateMonth;
        DateTime begin = null, end = null;
        if(!isLastYear){
            //获取本年年月份
            Date thisYear = new SimpleDateFormat("yyyy-MM").parse(date);
            begin = DateUtil.beginOfYear(thisYear);
            end = DateUtil.endOfMonth(thisYear);
            return new QFilter("biztime", QCP.large_equals, begin)
                    .and("biztime", QCP.less_equals, end);
        }

        //获取去年年月份
        year = year - 1;
        date = year + "-" + dateMonth;
        Date lastYear = new SimpleDateFormat("yyyy-MM").parse(date);
        begin = DateUtil.beginOfYear(lastYear);
        end = DateUtil.endOfMonth(lastYear);
        return new QFilter("biztime", QCP.large_equals, begin)
                .and("biztime", QCP.less_equals, end);
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("bizorgname",ReportColumn.TYPE_TEXT,"公司名称"));
        ReportColumnGroup xbyGroup = new ReportColumnGroup();
        for (int i = 0; i < groupName.length; i++) {
            ReportColumnGroup xsGroup = new ReportColumnGroup();
            xsGroup.setCaption(new LocaleString(groupName[i]+"销售收入（无税）（万元）"));
            xsGroup.getChildren().add((createReportColumn("thisYearAmount"+groupName[i],ReportColumn.TYPE_DECIMAL,"今年累计")));
            xsGroup.getChildren().add((createReportColumn("lastYearAmount"+groupName[i],ReportColumn.TYPE_DECIMAL,"去年同期")));
            xsGroup.getChildren().add((createReportColumn("yearAmountTB"+groupName[i],ReportColumn.TYPE_TEXT,"同比")));

            ReportColumnGroup xlGroup = new ReportColumnGroup();
            xlGroup.setCaption(new LocaleString(groupName[i]+"销售量（吨）"));
            xlGroup.getChildren().add((createReportColumn("thisYearQty"+groupName[i],ReportColumn.TYPE_DECIMAL,"今年销量")));
            xlGroup.getChildren().add((createReportColumn("lastYearQty"+groupName[i],ReportColumn.TYPE_DECIMAL,"去年销量")));
            xlGroup.getChildren().add((createReportColumn("yearQtyTB"+groupName[i],ReportColumn.TYPE_TEXT,"同比")));

            ReportColumnGroup mlGroup = new ReportColumnGroup();
            mlGroup.setCaption(new LocaleString(groupName[i]+"销售毛利（万元）"));
            mlGroup.getChildren().add((createReportColumn("thisYearML"+groupName[i],ReportColumn.TYPE_DECIMAL,"今年累计")));
            mlGroup.getChildren().add((createReportColumn("lastYearML"+groupName[i],ReportColumn.TYPE_DECIMAL,"去年同期")));
            mlGroup.getChildren().add((createReportColumn("yearMLTB"+groupName[i],ReportColumn.TYPE_TEXT,"同比")));

            ReportColumnGroup jjGroup = new ReportColumnGroup();
            jjGroup.setCaption(new LocaleString(groupName[i]+"销售均价（无税）（元）"));
            jjGroup.getChildren().add((createReportColumn("thisYearPrice"+groupName[i],ReportColumn.TYPE_DECIMAL,"今年均价")));
            jjGroup.getChildren().add((createReportColumn("lastYearPrice"+groupName[i],ReportColumn.TYPE_DECIMAL,"去年均价")));
            jjGroup.getChildren().add((createReportColumn("yearPriceTB"+groupName[i],ReportColumn.TYPE_TEXT,"同比")));
            if (groupName[i].equals("小包盐")){
                xbyGroup.setCaption(new LocaleString("小包盐销售情况"));
                xbyGroup.getChildren().add(xsGroup);
                xbyGroup.getChildren().add(xlGroup);
                xbyGroup.getChildren().add(mlGroup);
                xbyGroup.getChildren().add(jjGroup);
            }else if (groupName[i].equals("深井盐")){
                ReportColumnGroup sjyGroup = new ReportColumnGroup();
                sjyGroup.setCaption(new LocaleString("其中深井盐"));
                sjyGroup.getChildren().add(xsGroup);
                sjyGroup.getChildren().add(xlGroup);
                sjyGroup.getChildren().add(mlGroup);
                sjyGroup.getChildren().add(jjGroup);
                xbyGroup.getChildren().add(sjyGroup);
                columns.add(xbyGroup);
            }else if (groupName[i].equals("非盐")){
                ReportColumnGroup qtGroup = new ReportColumnGroup();
                qtGroup.setCaption(new LocaleString(groupName[i]+"销售情况"));
                qtGroup.getChildren().add(xsGroup);
//                qtGroup.getChildren().add(xlGroup);
                qtGroup.getChildren().add(mlGroup);
//                qtGroup.getChildren().add(jjGroup);
                columns.add(qtGroup);
            }else{
                ReportColumnGroup qtGroup = new ReportColumnGroup();
                qtGroup.setCaption(new LocaleString(groupName[i]+"销售情况"));
                qtGroup.getChildren().add(xsGroup);
                qtGroup.getChildren().add(xlGroup);
                qtGroup.getChildren().add(mlGroup);
                qtGroup.getChildren().add(jjGroup);
                columns.add(qtGroup);
            }
        }
        columns.add(createReportColumn("zxssr",ReportColumn.TYPE_DECIMAL,"总销售收入"));
        columns.add(createReportColumn("zml",ReportColumn.TYPE_DECIMAL,"总毛利"));
        columns.add(createReportColumn("tqzsr",ReportColumn.TYPE_DECIMAL,"同期总收入"));
        columns.add(createReportColumn("tqml",ReportColumn.TYPE_DECIMAL,"同期毛利"));

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