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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 报表取数插件
 */
public class ManageTeamCustomerReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

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

        qFilters.add(new QFilter("biztime",QCP.large_equals, DateUtil.beginOfYear(new SimpleDateFormat("yyyy-MM").parse(year + "-01")))
                .and("biztime",QCP.less_equals, DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(year + "-"+ month))));

        //公司
        String outFields = "bizorg as out_bizorg," +
                //公司名称
                "bizorg.name as out_bizorgname," +
                //销售部门
                "bizdept as out_bizdept," +
                //销售部门名称
                "bizdept.name as out_bizdeptname," +
                //收货客户
                "customer as out_customer," +
                //核心单据行id
                "billentry.mainbillentryid as out_mainbillentryid," +
                //业务日期
                "biztime as nckd_date";
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", outFields, qFilters.toArray(new QFilter[0]), null);

        DataSet thisYearIm = this.sumImSalOutBill(im_saloutbill,year,month);

        //获取华康年度计划表数据
        DataSet nckd_hkndjhb = this.getHKYearPlan(year,month);

        thisYearIm = thisYearIm.leftJoin(nckd_hkndjhb).on("out_bizorg","hk_org").on("out_bizdept","hk_jytd")
                .select(thisYearIm.getRowMeta().getFieldNames(),nckd_hkndjhb.getRowMeta().getFieldNames()).finish();

        //获取渠道档案
        DataSet ocdbd_channel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel", "slaeorginfo.saleorginfonum as ocdbd_saleorg",
                new QFilter[]{new QFilter("status", QCP.equals, "C")}, null);
        ocdbd_channel = ocdbd_channel.groupBy(new String[]{"ocdbd_saleorg"}).count("ocdbd_sumorg").finish();

        //销售出库单关联渠道档案
        thisYearIm = thisYearIm.leftJoin(ocdbd_channel).on("out_bizorg","ocdbd_saleorg")
                .select(thisYearIm.getRowMeta().getFieldNames(),new String[]{"ocdbd_sumorg"}).finish();

        //获取销售出库核心单据行id
        List<Long> outMainbillentryid = DataSetToList.getOneToList(im_saloutbill, "out_mainbillentryid");
        QFilter ocbsocFilter = new QFilter("itementry.id", QCP.in, outMainbillentryid.toArray(new Long[0]));
//        ocbsocFilter.and("orderchannelid.nckd_boutiquecustomer",QCP.equals ,"1");
        //查询要货订单订货渠道
        DataSet ocbsoc_saleorder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", "itementry.id as entryid,orderchannelid.nckd_boutiquecustomer as boutiquecustomer",
                new QFilter[]{ocbsocFilter}, null);
        //销售出库单关联要货订单
        im_saloutbill = im_saloutbill.leftJoin(ocbsoc_saleorder).on("out_mainbillentryid","entryid")
                .select(im_saloutbill.getRowMeta().getFieldNames(),new String[]{"boutiquecustomer"}).finish();

        //过滤不为精品客户的数据
        im_saloutbill = im_saloutbill.filter("boutiquecustomer = " + Boolean.TRUE);
        if (!im_saloutbill.isEmpty()){
            im_saloutbill = this.sumImSalOutBill(im_saloutbill,year,month)
                    .select("out_bizorg", "out_bizorgname", "out_bizdept", "out_bizdeptname","out_customerYear as out_isJPYear","out_customerMonth as out_isJPMonth");
            thisYearIm = thisYearIm.leftJoin(im_saloutbill).on("out_bizorg","out_bizorg").on("out_bizdept","out_bizdept")
                    .select(thisYearIm.getRowMeta().getFieldNames(),new String[]{"out_isJPYear","out_isJPMonth"}).finish();
        }


        return thisYearIm.orderBy(new String[]{"out_bizorg"});
    }

    public DataSet sumImSalOutBill(DataSet im_saloutbill,int year,int month) throws ParseException {
        DateTime monthBegin = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(year + "-" + month));
        DateTime monthEnd = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(year + "-" + month));
        //获取本月数据
        DataSet thisYearMonthIm  = im_saloutbill.copy().filter("nckd_date >=to_date('" + monthBegin + "','yyyy-MM-dd hh:mm:ss')")
                .filter("nckd_date <=to_date('" + monthEnd + "','yyyy-MM-dd hh:mm:ss')")
                .groupBy(new String[]{"out_bizorg", "out_bizorgname", "out_bizdept", "out_bizdeptname","out_customer"}).finish()
                .groupBy(new String[]{"out_bizorg", "out_bizorgname", "out_bizdept", "out_bizdeptname"})
                .count("out_customerMonth").finish();

        //获取本年数据
        DataSet thisYearIm = im_saloutbill.copy().groupBy(new String[]{"out_bizorg", "out_bizorgname", "out_bizdept", "out_bizdeptname","out_customer"}).finish()
                .groupBy(new String[]{"out_bizorg", "out_bizorgname", "out_bizdept", "out_bizdeptname"})
                .count("out_customerYear").finish();

        thisYearIm = thisYearIm.leftJoin(thisYearMonthIm).on("out_bizorg","out_bizorg").on("out_bizdept","out_bizdept")
                .select(new String[]{"out_bizorg", "out_bizorgname", "out_bizdept", "out_bizdeptname","out_customerYear"},new String[]{"out_customerMonth"}).finish();

        thisYearMonthIm.close();
        return thisYearIm;

    }

    //获取华康年度计划表数据
    public DataSet getHKYearPlan(int year,int month) throws ParseException {

        //组织
        String hkFields = "org as hk_org," +
                //经营团队
                "entryentity.nckd_jytd as hk_jytd," +
                "entryentity.nckd_date as hk_date," +
                //月度交易客户数目标
                "entryentity.nckd_ydjykh as hk_ydjykh," +
                //月度精品终端客户数目标
                "entryentity.nckd_ydjpzdkh as hk_ydjpzdkh" ;
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
                .groupBy(new String[]{"hk_org", "hk_jytd"})
                .sum("hk_ydjykh", "month_hk_ydjykh").sum("hk_ydjpzdkh","month_hk_ydjpzdkh").finish();
        //汇总年度目标
        nckd_hkndjhb = nckd_hkndjhb.groupBy(new String[]{"hk_org", "hk_jytd"})
                .sum("hk_ydjykh", "year_hk_ydjykh").sum("hk_ydjpzdkh","year_hk_ydjpzdkh").finish();
        //合并月度和年度目标
        nckd_hkndjhb = nckd_hkndjhb.leftJoin(monthHK).on("hk_org","hk_org").on("hk_jytd","hk_jytd").
                select(nckd_hkndjhb.getRowMeta().getFieldNames(),new String[]{"month_hk_ydjykh","month_hk_ydjpzdkh"}).finish();
        monthHK.close();
        return nckd_hkndjhb;
    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("out_bizorgname",ReportColumn.TYPE_TEXT,"公司名称"));
        columns.add(createReportColumn("out_bizdeptname",ReportColumn.TYPE_TEXT,"经营团队"));
        columns.add(createReportColumn("ocdbd_sumorg",ReportColumn.TYPE_TEXT,"登记客商数"));

        ReportColumnGroup monthjyReportColumnGroup = new ReportColumnGroup();
        monthjyReportColumnGroup.setFieldKey("monthjy");
        monthjyReportColumnGroup.setCaption(new LocaleString("月度交易客户"));
        monthjyReportColumnGroup.getChildren().add(createReportColumn("month_hk_ydjykh",ReportColumn.TYPE_DECIMAL,"月度交易客户数目标"));
        monthjyReportColumnGroup.getChildren().add(createReportColumn("out_customerMonth",ReportColumn.TYPE_DECIMAL,"本月客户"));
        monthjyReportColumnGroup.getChildren().add(createReportColumn("monthjywcl",ReportColumn.TYPE_DECIMAL,"月客户完成率"));
        columns.add(monthjyReportColumnGroup);

        ReportColumnGroup yearjyReportColumnGroup = new ReportColumnGroup();
        yearjyReportColumnGroup.setFieldKey("yearjy");
        yearjyReportColumnGroup.setCaption(new LocaleString("年度交易客户"));
        yearjyReportColumnGroup.getChildren().add(createReportColumn("year_hk_ydjykh",ReportColumn.TYPE_DECIMAL,"交易客户目标"));
        yearjyReportColumnGroup.getChildren().add(createReportColumn("out_customerYear",ReportColumn.TYPE_DECIMAL,"交易客户累计"));
        yearjyReportColumnGroup.getChildren().add(createReportColumn("yearjywcl",ReportColumn.TYPE_DECIMAL,"交易客户完成"));
        columns.add(yearjyReportColumnGroup);

        ReportColumnGroup monthjpReportColumnGroup = new ReportColumnGroup();
        monthjpReportColumnGroup.setFieldKey("monthjp");
        monthjpReportColumnGroup.setCaption(new LocaleString("月度精品客户"));
        monthjpReportColumnGroup.getChildren().add(createReportColumn("month_hk_ydjpzdkh",ReportColumn.TYPE_DECIMAL,"月度精品客户目标"));
        monthjpReportColumnGroup.getChildren().add(createReportColumn("out_isJPMonth",ReportColumn.TYPE_DECIMAL,"本月精品客户"));
        monthjpReportColumnGroup.getChildren().add(createReportColumn("monthjpkhwcl",ReportColumn.TYPE_DECIMAL,"月度精品客户完成"));
        columns.add(monthjpReportColumnGroup);

        ReportColumnGroup yearjpReportColumnGroup = new ReportColumnGroup();
        yearjpReportColumnGroup.setFieldKey("yearjp");
        yearjpReportColumnGroup.setCaption(new LocaleString("年度精品客户"));
        yearjpReportColumnGroup.getChildren().add(createReportColumn("year_hk_ydjpzdkh",ReportColumn.TYPE_DECIMAL,"精品客户目标"));
        yearjpReportColumnGroup.getChildren().add(createReportColumn("out_isJPYear",ReportColumn.TYPE_DECIMAL,"精品客户累计"));
        yearjpReportColumnGroup.getChildren().add(createReportColumn("monthjpkhwcl",ReportColumn.TYPE_DECIMAL,"精品客户完成"));
        columns.add(yearjpReportColumnGroup);


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