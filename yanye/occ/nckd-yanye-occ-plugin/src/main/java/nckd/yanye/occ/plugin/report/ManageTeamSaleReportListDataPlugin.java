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
 * 经营团队情况一览表（销量）-报表取数插件
 * 表单标识：nckd_manageteamsale_rpt
 * author:zhangzhilong
 * date:2024/09/13
 */
public class ManageTeamSaleReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //过滤组织限定名称带华康的公司
        QFilter orgFilter = new QFilter("bizorg.name", QCP.like,"%华康%");
        //单据需已审核
        orgFilter.and("billstatus",QCP.equals,"C");
        qFilters.add(orgFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        int year =  DateUtil.year(new Date());
        int month = DateUtil.month(new Date()) + 1;
        if(filter.getDate("nckd_date_q") != null){
            year =  DateUtil.year(filter.getDate("nckd_date_q"));
            month = DateUtil.month(filter.getDate("nckd_date_q")) + 1;
        }

        DateTime yearBegin = DateUtil.beginOfYear(new SimpleDateFormat("yyyy-MM").parse((year-1) + "-01"));
        qFilters.add(new QFilter("biztime",QCP.large_equals, yearBegin)
                .and("biztime",QCP.less_equals, DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(year + "-"+ month))));

        //公司
        String outFields = "bizorg as out_bizorg," +
                //公司名称
                "bizorg.name as out_bizorgname," +
                //销售部门
                "bizdept as out_bizdept," +
                //销售部门名称
                "bizdept.name as out_bizdeptname," +
                //基本数量
                "billentry.baseqty as out_baseqty," +
                //价税合计
                "billentry.amountandtax as out_amountandtax," +
                //成本金额
                "billentry.nckd_cbj as out_cbj," +
                //物料分类
                "billentry.material.masterid.group as out_group," +
                //物料分类名称
                "billentry.material.masterid.group.name as out_groupname," +
                //业务日期
                "biztime as nckd_date";
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", outFields, qFilters.toArray(new QFilter[0]), null);
        if (im_saloutbill.isEmpty()) {
            return im_saloutbill;
        }

        DateTime monthBegin = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse((year-1) + "-" +month));
        DateTime monthEnd = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse((year-1) + "-" +month));
        //获取去年同期数据
        DataSet lastYearIm = im_saloutbill.copy().filter("nckd_date >=to_date('" + yearBegin + "','yyyy-MM-dd hh:mm:ss')")
                .filter("nckd_date <=to_date('" + monthEnd + "','yyyy-MM-dd hh:mm:ss')").addField("3","yearMonth");

        //获取去年本月同期数据
        DataSet lastYearMonthIm = im_saloutbill.copy().filter("nckd_date >=to_date('" + monthBegin + "','yyyy-MM-dd hh:mm:ss')")
                .filter("nckd_date <=to_date('" + monthEnd + "','yyyy-MM-dd hh:mm:ss')").addField("4","yearMonth");

        yearBegin = DateUtil.beginOfYear(new SimpleDateFormat("yyyy-MM").parse(year + "-01"));
        monthBegin = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(year + "-" + month));
        monthEnd = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(year + "-" + month));
        //获取本月数据
        DataSet thisYearMonthIm  = im_saloutbill.copy().filter("nckd_date >=to_date('" + monthBegin + "','yyyy-MM-dd hh:mm:ss')")
                .filter("nckd_date <=to_date('" + monthEnd + "','yyyy-MM-dd hh:mm:ss')").addField("2","yearMonth");

        //获取本年数据
        im_saloutbill = im_saloutbill.copy().filter("nckd_date >=to_date('" + yearBegin + "','yyyy-MM-dd hh:mm:ss')")
                .filter("nckd_date <=to_date('" + monthEnd + "','yyyy-MM-dd hh:mm:ss')").addField("1","yearMonth");

        //关联各个数据
        im_saloutbill = im_saloutbill.union(thisYearMonthIm).union(lastYearMonthIm).union(lastYearIm);
        //进行数据分组汇总
        im_saloutbill = im_saloutbill.groupBy(new String[]{"out_bizorg", "out_bizorgname", "out_bizdept", "out_bizdeptname", "out_group", "out_groupname"})
                .sum("case when yearMonth = 1 then out_baseqty else 0 end", "thisYear_out_baseqty")
                .sum("case when yearMonth = 1 then out_amountandtax else 0 end", "thisYear_out_amountandtax")
                .sum("case when yearMonth = 1 then out_amountandtax - out_cbj else 0 end", "thisYear_out_ljml")
                .sum("case when yearMonth = 2 then out_baseqty else 0 end", "thisYearMonth_out_baseqty")
                .sum("case when yearMonth = 2 then out_amountandtax else 0 end", "thisYearMonth_out_amountandtax")
                .sum("case when yearMonth = 2 then out_amountandtax - out_cbj else 0 end", "thisYearMonth_out_byml")
                .sum("case when yearMonth = 3 then out_baseqty else 0 end", "lastYear_out_baseqty")
                .sum("case when yearMonth = 4 then out_baseqty else 0 end", "lastYearMonth_out_baseqty")
                .finish();

//        获取华康年度计划表数据
        //组织
        String hkFields = "org as hk_org," +
                //经营团队
                "entryentity.nckd_jytd as hk_jytd," +
                "entryentity.nckd_date as hk_date," +
                //月度小包装盐销售目标
                "entryentity.nckd_ydxbzy as hk_ydxbzy" ;
        //计算汇总本月月度小包装盐销售目标
        String date= year + "-" + month;
        QFilter hkFilter = new QFilter("entryentity.nckd_date", QCP.large_equals, DateUtil.beginOfYear(new SimpleDateFormat("yyyy-MM").parse(date)))
                .and("entryentity.nckd_date",QCP.less_equals, DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(date)));
        DataSet nckd_hkndjhb = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                        "nckd_hkndjhb", hkFields, new QFilter[]{hkFilter}, null);
        //隔离出本月数据并进行汇总
        DateTime begin = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
        DateTime end = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(date));
        DataSet monthHK = nckd_hkndjhb.copy().filter("hk_date >=to_date('" + begin + "','yyyy-MM-dd hh:mm:ss')")
                .filter("hk_date <=to_date('" + end + "','yyyy-MM-dd hh:mm:ss')")
                .groupBy(new String[]{"hk_org", "hk_jytd"}).sum("hk_ydxbzy", "month_hk_ydxbzy").finish();
        //汇总年度目标
        nckd_hkndjhb = nckd_hkndjhb.groupBy(new String[]{"hk_org", "hk_jytd"}).sum("hk_ydxbzy", "year_hk_ydxbzy").finish();
        //合并月度和年度目标
        nckd_hkndjhb = nckd_hkndjhb.leftJoin(monthHK).on("hk_org","hk_org").on("hk_jytd","hk_jytd").select(nckd_hkndjhb.getRowMeta().getFieldNames(),new String[]{"month_hk_ydxbzy"}).finish();

        //合并销售出库表和华康年度计划表
        im_saloutbill = im_saloutbill.leftJoin(nckd_hkndjhb).on("out_bizorg","hk_org").on("out_bizdept","hk_jytd")
                .select(im_saloutbill.getRowMeta().getFieldNames(),nckd_hkndjhb.getRowMeta().getFieldNames()).finish();

        //获取渠道档案
        DataSet ocdbd_channel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel", "slaeorginfo.saleorginfonum as saleorg",
                new QFilter[]{new QFilter("status", QCP.equals, "C")}, null);
        ocdbd_channel = ocdbd_channel.groupBy(new String[]{"ocdbd_saleorg"}).count("ocdbd_sumorg").finish();

        return im_saloutbill.orderBy(new String[]{"out_bizorg"});
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("out_bizorgname",ReportColumn.TYPE_TEXT,"公司名称"));
        columns.add(createReportColumn("out_bizdeptname",ReportColumn.TYPE_TEXT,"经营团队"));
        columns.add(createReportColumn("out_groupname",ReportColumn.TYPE_TEXT,"物料分类"));

        ReportColumnGroup monthReportColumnGroup = new ReportColumnGroup();
        monthReportColumnGroup.setFieldKey("month");
        monthReportColumnGroup.setCaption(new LocaleString("月度销售"));
        monthReportColumnGroup.getChildren().add(createReportColumn("month_hk_ydxbzy",ReportColumn.TYPE_DECIMAL,"月度小包装盐目标"));
        monthReportColumnGroup.getChildren().add(createReportColumn("thisYearMonth_out_baseqty",ReportColumn.TYPE_DECIMAL,"本月销量"));
        monthReportColumnGroup.getChildren().add(createReportColumn("lastYearMonth_out_baseqty",ReportColumn.TYPE_DECIMAL,"同期"));
        monthReportColumnGroup.getChildren().add(createReportColumn("tb",ReportColumn.TYPE_TEXT,"同比"));
        monthReportColumnGroup.getChildren().add(createReportColumn("ydxbwc",ReportColumn.TYPE_TEXT,"月度小包完成"));
        columns.add(monthReportColumnGroup);

        ReportColumnGroup yearReportColumnGroup = new ReportColumnGroup();
        yearReportColumnGroup.setFieldKey("year");
        yearReportColumnGroup.setCaption(new LocaleString("累计销售"));
        yearReportColumnGroup.getChildren().add(createReportColumn("year_hk_ydxbzy",ReportColumn.TYPE_DECIMAL,"小包盐目标"));
        yearReportColumnGroup.getChildren().add(createReportColumn("thisYear_out_baseqty",ReportColumn.TYPE_DECIMAL,"小包盐销量"));
        yearReportColumnGroup.getChildren().add(createReportColumn("lastYear_out_baseqty",ReportColumn.TYPE_DECIMAL,"上年累计"));
        yearReportColumnGroup.getChildren().add(createReportColumn("ljtb",ReportColumn.TYPE_TEXT,"累计同比"));
        yearReportColumnGroup.getChildren().add(createReportColumn("xlwcl",ReportColumn.TYPE_TEXT,"销量完成率"));
        yearReportColumnGroup.getChildren().add(createReportColumn("thisYear_out_amountandtax",ReportColumn.TYPE_DECIMAL,"小包均价"));
        columns.add(yearReportColumnGroup);

        ReportColumnGroup jpReportColumnGroup = new ReportColumnGroup();
        jpReportColumnGroup.setFieldKey("jp");
        jpReportColumnGroup.setCaption(new LocaleString("竞品盐"));
        jpReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"竞品数量"));
        jpReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"竞品盐均价"));
        jpReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"竞品占比"));
        jpReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"竞品盐同期"));
        jpReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"竞品同比"));
        columns.add(jpReportColumnGroup);

        ReportColumnGroup sjReportColumnGroup = new ReportColumnGroup();
        sjReportColumnGroup.setFieldKey("sj");
        sjReportColumnGroup.setCaption(new LocaleString("深井盐"));
        sjReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"深井数量"));
        sjReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"深井盐均价"));
        sjReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"深井占比"));
        sjReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"深井盐同期"));
        sjReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"深井同比"));
        columns.add(sjReportColumnGroup);

        ReportColumnGroup gdReportColumnGroup = new ReportColumnGroup();
        gdReportColumnGroup.setFieldKey("gd");
        gdReportColumnGroup.setCaption(new LocaleString("高端盐"));
        gdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"高端盐数量"));
        gdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"高端盐均价"));
        gdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"高端盐占比"));
        gdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"高端盐同期"));
        gdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"高端盐同比"));
        columns.add(gdReportColumnGroup);

        ReportColumnGroup gdcpslReportColumnGroup = new ReportColumnGroup();
        gdcpslReportColumnGroup.setFieldKey("gdcpsl");
        gdcpslReportColumnGroup.setCaption(new LocaleString("高端产品收入"));
        gdcpslReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"高端产品收入"));
        gdcpslReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"同期高端产品收入"));
        gdcpslReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"高端收入同比"));
        columns.add(gdcpslReportColumnGroup);

        ReportColumnGroup gsyReportColumnGroup = new ReportColumnGroup();
        gsyReportColumnGroup.setFieldKey("gsy");
        gsyReportColumnGroup.setCaption(new LocaleString("果蔬盐"));
        gsyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"果蔬盐销售"));
        gsyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"果蔬盐销量同期数"));
        gsyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"果蔬销售同比"));
        columns.add(gsyReportColumnGroup);


        ReportColumnGroup xsdReportColumnGroup = new ReportColumnGroup();
        xsdReportColumnGroup.setFieldKey("xsd");
        xsdReportColumnGroup.setCaption(new LocaleString("小苏打"));
        xsdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"小苏打盐销售"));
        xsdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"小苏打销量同期数"));
        xsdReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"小苏打销售同比"));
        columns.add(xsdReportColumnGroup);

        ReportColumnGroup shyReportColumnGroup = new ReportColumnGroup();
        shyReportColumnGroup.setFieldKey("shy");
        shyReportColumnGroup.setCaption(new LocaleString(" 深海盐"));
        shyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"深海盐销售"));
        shyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"深海盐销量同期数"));
        shyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"深海盐销售同比"));
        columns.add(shyReportColumnGroup);


        ReportColumnGroup jlyReportColumnGroup = new ReportColumnGroup();
        jlyReportColumnGroup.setFieldKey("jly");
        jlyReportColumnGroup.setCaption(new LocaleString(" 晶粒盐"));
        jlyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"晶粒盐销售"));
        jlyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"晶粒盐销量同期数"));
        jlyReportColumnGroup.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"晶粒盐销售同比"));
        columns.add(jlyReportColumnGroup);

        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"累计二批"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"累计终端"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"累计商超"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"累计餐饮"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"累计电商"));

        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"月底毛利目标"));
        columns.add(createReportColumn("thisYearMonth_out_byml",ReportColumn.TYPE_DECIMAL,"本月毛利"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"月度毛利完成率"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"毛利目标"));
        columns.add(createReportColumn("thisYear_out_ljml",ReportColumn.TYPE_DECIMAL,"总毛利"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"毛利完成率"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"小包毛利"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"大包毛利"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"非盐毛利"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"登记客商数"));

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