package nckd.yanye.occ.plugin.report;

import com.ccb.core.date.DateTime;
import com.ccb.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 报表取数插件
 */
public class VarietySaleReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        FilterInfo filter = reportQueryParam.getFilter();
        //获取过滤组织
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if (nckdOrgQ != null) {
            Long pkValue = (Long) nckdOrgQ.getPkValue();
            qFilters.add(new QFilter("bizorg", QCP.equals, pkValue));
        }
        //获取今年和去年年份
        int year = DateUtil.thisYear();
        int lastYear = DateUtil.thisYear() - 1;
        //获取年份
        Date nckdStartdateQ = filter.getDate("nckd_date_q");
        if (nckdStartdateQ != null) {
            //判断是不是本年
            if (DateUtil.year(nckdStartdateQ) == DateUtil.thisYear()) {
                String lastDate = lastYear + "-01";
                DateTime start = DateUtil.beginOfYear(new SimpleDateFormat("yyyy-MM").parse(lastDate));
                //本年的数据截止到当前月份的月底
//                DateTime end = DateUtil.endOfMonth(new Date());
                DateTime end = DateUtil.endOfYear(nckdStartdateQ);
                qFilters.add(new QFilter("biztime", QCP.large_equals, start).and("biztime", QCP.less_equals, end));
            } else {
                year = DateUtil.year(nckdStartdateQ);
                lastYear = year - 1;
                String lastDate = lastYear + "-01";
                //非本年则取全年数据
                DateTime start = DateUtil.beginOfYear(new SimpleDateFormat("yyyy-MM").parse(lastDate));
                DateTime end = DateUtil.endOfYear(nckdStartdateQ);
                qFilters.add(new QFilter("biztime", QCP.large_equals, start).and("biztime", QCP.less_equals, end));
            }
        }
        //获取过滤存货分类
        DynamicObject nckdMaterialclassQ = filter.getDynamicObject("nckd_materialclass_q");
        if (nckdMaterialclassQ != null) {
            Long pkValue = (Long) nckdMaterialclassQ.getPkValue();
            qFilters.add(new QFilter("billentry.material.masterid.group", QCP.equals, pkValue));
        }
        //获取过滤存货编码
        DynamicObject nckdMaterialQ = filter.getDynamicObject("nckd_material_q");
        if (nckdMaterialQ != null) {
            Long pkValue = (Long) nckdMaterialQ.getPkValue();
            qFilters.add(new QFilter("billentry.material.masterid", QCP.equals, pkValue));
        }
        //分类编码
        String outFiles = "billentry.material.masterid.group.number as classnumber," +
                //分类名称
                "billentry.material.masterid.group.name as classname," +
                //存货编码
                "billentry.material.masterid.number as materialnumber," +
                //存货名称
                "billentry.material.masterid.name as materialname," +
                //数量
                "billentry.qty as qty," +
                //价税合计
                "billentry.amountandtax as amountandtax," +
                "biztime as out_biztime";
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", outFiles, qFilters.toArray(new QFilter[0]), null);


        DataSet thisYearDS = null, lastYearDS = null;
        //循环获取查询年份每月数据
        thisYearDS = getYearDataSet(year, imSaloutbill, thisYearDS);
        //循环获取查询年份去年的每月数据
        lastYearDS = getYearDataSet(lastYear, imSaloutbill, lastYearDS);
        //关联获取今年和去年的数据
        imSaloutbill = thisYearDS.union(lastYearDS);
        GroupbyDataSet groupbyDataSet = imSaloutbill.groupBy(new String[]{"classnumber", "classname", "materialnumber", "materialname"});
        for (int i = 1; i < 13; i++) {
            groupbyDataSet.sum("case when year = " + year + " and month ="+ i +" then qty else 0 end", "thisQty" + i)
                    .sum("case when year = " + year + " and month ="+ i +" then amountandtax else 0 end", "thisAmount" + i)
                    .sum("case when year = " + lastYear + " and month ="+ i +" then qty else 0 end", "lastQty" + i)
                    .sum("case when year = " + lastYear + " and month ="+ i +" then amountandtax else 0 end", "lastAmount" + i);
        }
        imSaloutbill = groupbyDataSet.finish();
        return imSaloutbill.orderBy(new String[]{"classnumber","materialnumber"});
    }

    //按年月分割数据
    private DataSet getYearDataSet(int year, DataSet imSaloutbill, DataSet ds) throws ParseException {
        DataSet dataSet = null;
        for (int i = 1; i < 13; i++) {
            //拼接年月份
            String date = year + "-" + i;
            //获取月份开始日期
            DateTime s = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
            //获取月份结束日期
            DateTime e = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
            dataSet = imSaloutbill.copy().filter("out_biztime >=to_date('" + s + "','yyyy-MM-dd hh:mm:ss')")
                    .filter("out_biztime <=to_date('" + e + "','yyyy-MM-dd hh:mm:ss')").addField(String.valueOf(year), "year").addField(String.valueOf(i),"month");
            if (i == 1) {
                ds = dataSet;
            } else {
                ds = ds.union(dataSet);
            }
        }
        dataSet.close();
        return ds;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("classnumber", ReportColumn.TYPE_TEXT, "分类编码"));
        columns.add(createReportColumn("classname", ReportColumn.TYPE_TEXT, "分类名称"));
        columns.add(createReportColumn("materialnumber", ReportColumn.TYPE_TEXT, "存货编码"));
        columns.add(createReportColumn("materialname", ReportColumn.TYPE_TEXT, "存货名称"));
        //创建年分组
        ReportColumnGroup yearReportColumnGroup = new ReportColumnGroup();
        yearReportColumnGroup.setFieldKey("year");
        yearReportColumnGroup.setCaption(new LocaleString("年度"));
        yearReportColumnGroup.getChildren().add(createReportColumn("yearAmount", ReportColumn.TYPE_DECIMAL, "今年总金额"));
        yearReportColumnGroup.getChildren().add(createReportColumn("yearPrice", ReportColumn.TYPE_DECIMAL, "今年均价"));
        yearReportColumnGroup.getChildren().add(createReportColumn("lastAmount", ReportColumn.TYPE_DECIMAL, "去年总金额"));
        yearReportColumnGroup.getChildren().add(createReportColumn("lastPrice", ReportColumn.TYPE_DECIMAL, "去年均价"));
        columns.add(yearReportColumnGroup);
        for (int i = 1; i < 13; i++) {
            //创建月份分组
            ReportColumnGroup monthReportColumnGroup = new ReportColumnGroup();
            monthReportColumnGroup.setFieldKey("month");
            monthReportColumnGroup.setCaption(new LocaleString(i + "月"));
            monthReportColumnGroup.getChildren().add(createReportColumn("thisAmount" + i, ReportColumn.TYPE_DECIMAL, i + "月总金额"));
            monthReportColumnGroup.getChildren().add(createReportColumn("thisQty" + i, ReportColumn.TYPE_DECIMAL, i + "月均价"));
            monthReportColumnGroup.getChildren().add(createReportColumn("lastAmount" + i, ReportColumn.TYPE_DECIMAL, "去年" + i + "月总金额"));
            monthReportColumnGroup.getChildren().add(createReportColumn("lastQty" + i, ReportColumn.TYPE_DECIMAL, "去年" + i + "月均价"));
            columns.add(monthReportColumnGroup);
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