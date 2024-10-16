package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 片区情况一览表（交易客户）-报表取数插件
 * 表单标识：nckd_areasituationcus_rpt
 * author:zhangzhilong
 * date:2024/10/12
 */
public class AreaSituationCustomerReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        QFilter initFilter = new QFilter("bizorg.name", QCP.like, "%华康%");
        initFilter.and("billstatus", QCP.equals, "C");
        qFilters.add(initFilter);
        FilterInfo filter = reportQueryParam.getFilter();
        //公司过滤
        if (filter.getDynamicObject("nckd_org_q") != null) {
            Long pkValue = (Long) filter.getDynamicObject("nckd_org_q").getPkValue();
            qFilters.add(new QFilter("bizorg", QCP.equals, pkValue));
        }
        //日期过滤
        Date nckdDateQ = filter.getDate("nckd_date_q");
        if (nckdDateQ == null) {
            nckdDateQ = new Date();
        }
        DateTime beginOfYear = DateUtil.beginOfYear(nckdDateQ);
        DateTime beginOfMonth = DateUtil.beginOfMonth(nckdDateQ);
        DateTime endOfMonth = DateUtil.endOfMonth(nckdDateQ);

        qFilters.add(new QFilter("biztime", QCP.large_equals, beginOfYear).and("biztime", QCP.less_equals, endOfMonth));

        //公司
        String outFields = "bizorg as out_bizorg," +
                //部门
                "bizdept as out_bizdept," +
                //销售片区
                "nckd_basedatafield as out_xspq," +
                //收货客户
                "customer as out_customer," +
                //业务日期
                "biztime as nckd_date";
        DataSet imSaloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", outFields, qFilters.toArray(new QFilter[0]), null);

        //获取精品客户
        List<Long> outCustomer = DataSetToList.getOneToList(imSaloutbill, "out_customer");
        QFilter cusFilter = new QFilter("nckd_boutiquecustomer", QCP.equals, "1").and("id", QCP.in, outCustomer.toArray(new Long[0]));
        DataSet customerDS = QueryServiceHelper.queryDataSet(this.getClass().getName(),"bd_customer", "id,'精品' as isjp", new QFilter[]{cusFilter}, null);

        //关联客户档案判断是否精品
        imSaloutbill = imSaloutbill.leftJoin(customerDS).on("out_customer", "id").select(imSaloutbill.getRowMeta().getFieldNames(), new String[]{"isjp"}).finish();

        //计算月度和年度精品交易客户并关联
        DataSet jpMonthSum = imSaloutbill.filter("isjp = '精品'").filter("nckd_date >= to_date('" + beginOfMonth + "','yyyy-MM-dd hh:mm:ss')")
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq", "out_customer"}).finish()
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq"})
                .count("monthSumJPCustomer").finish();
        DataSet jpYearSum = imSaloutbill.filter("isjp = '精品'")
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq", "out_customer"}).finish()
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq"})
                .count("yearSumJPCustomer").finish();
        jpYearSum = jpYearSum.leftJoin(jpMonthSum)
                .on("out_bizorg","out_bizorg")
                .on("out_bizdept","out_bizdept")
                .on("out_xspq","out_xspq")
                .select(new String[]{"out_bizorg", "out_bizdept","out_xspq","monthSumJPCustomer","yearSumJPCustomer"}).finish();

        //计算月度和年度交易客户并关联
        DataSet monthSum = imSaloutbill.filter("nckd_date >= to_date('" + beginOfMonth + "','yyyy-MM-dd hh:mm:ss')")
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq", "out_customer"}).finish()
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq"})
                .count("monthSumCustomer").finish();
        DataSet yearSum = imSaloutbill
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq", "out_customer"}).finish()
                .groupBy(new String[]{"out_bizorg", "out_bizdept","out_xspq"})
                .count("yearSumCustomer").finish();
        yearSum = yearSum.leftJoin(monthSum)
                .on("out_bizorg","out_bizorg")
                .on("out_bizdept","out_bizdept")
                .on("out_xspq","out_xspq")
                .select(new String[]{"out_bizorg", "out_bizdept","out_xspq","monthSumCustomer","yearSumCustomer"}).finish();

        imSaloutbill = yearSum.leftJoin(jpYearSum)
                .on("out_bizorg","out_bizorg")
                .on("out_bizdept","out_bizdept")
                .on("out_xspq","out_xspq")
                .select(yearSum.getRowMeta().getFieldNames(),new String[]{"monthSumJPCustomer","yearSumJPCustomer"}).finish();
        jpMonthSum.close();
        jpYearSum.close();
        monthSum.close();
        yearSum.close();

        //获取华康年度计划表
        String hkFields = "org as hk_org," +
                //经营团队
                "entryentity.nckd_jytd as nckd_jytd," +
                //业务团队
                "entryentity.nckd_dqfl as nckd_ywtd," +
                "entryentity.nckd_date as hk_date," +
                //月度交易客户数目标
                "entryentity.nckd_ydjykh as nckd_ydjykh," +
                //月度精品客户数目标
                "entryentity.nckd_ydjpzdkh as nckd_ydjpzdkh" ;
        QFilter hkFilter = new QFilter("entryentity.nckd_date", QCP.large_equals,beginOfYear )
                .and("entryentity.nckd_date",QCP.less_equals, endOfMonth);
        DataSet nckdHkndjhb = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_hkndjhb", hkFields, new QFilter[]{hkFilter}, null);
        //汇总月度和年度目标并关联
        DataSet hkMonthSum = nckdHkndjhb.filter("hk_date >= to_date('" + beginOfMonth + "','yyyy-MM-dd hh:mm:ss')")
                .groupBy(new String[]{"hk_org","nckd_jytd","nckd_ywtd"})
                .sum("nckd_ydjykh", "month_nckd_ydjykh")
                .sum("nckd_ydjpzdkh","month_nckd_ydjpzdkh").finish();
        nckdHkndjhb = nckdHkndjhb.groupBy(new String[]{"hk_org","nckd_jytd","nckd_ywtd"})
                .sum("nckd_ydjykh", "year_nckd_ydjykh")
                .sum("nckd_ydjpzdkh","year_nckd_ydjpzdkh").finish();
        nckdHkndjhb = nckdHkndjhb.leftJoin(hkMonthSum)
                .on("hk_org","hk_org")
                .on("nckd_jytd","nckd_jytd")
                .on("nckd_ywtd","nckd_ywtd")
                .select(new String[]{"hk_org","nckd_jytd","nckd_ywtd","month_nckd_ydjykh","month_nckd_ydjpzdkh","year_nckd_ydjykh","year_nckd_ydjpzdkh"}).finish();

        //交易客户关联华康年度计划表
        imSaloutbill = imSaloutbill.leftJoin(nckdHkndjhb)
                .on("out_bizorg","hk_org")
                .on("out_bizdept","nckd_jytd")
                .on("out_xspq","nckd_ywtd")
                .select(imSaloutbill.getRowMeta().getFieldNames(),
                        new String[]{"nckd_ywtd","month_nckd_ydjykh","month_nckd_ydjpzdkh","year_nckd_ydjykh","year_nckd_ydjpzdkh"}).finish();

        imSaloutbill = imSaloutbill.leftJoin(this.getChannelSumKS())
                .on("out_bizorg","ocdbd_org")
                .on("out_bizdept","ocdbd_department")
                .on("out_xspq","ocdbd_regiongroup")
                .select(imSaloutbill.getRowMeta().getFieldNames(),new String[]{"channelSumKS"}).finish();

        nckdHkndjhb.close();
        return imSaloutbill;
    }

    public DataSet getChannelSumKS() {
        QFilter qFilter = new QFilter("slaeorginfo.saleorginfonum.name", QCP.like, "%华康%");
        qFilter.and("status", QCP.equals, "C");
        //获取渠道信息
        DataSet ocdbdChannel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel",
                //销售组织
                "slaeorginfo.saleorginfonum as ocdbd_org," +
                        //部门
                        "slaeorginfo.department as ocdbd_department," +
                        //业务员
                        "slaeorginfo.saler as ocdbd_saler," +
                        //销售片区
                        "nckd_regiongroup as ocdbd_regiongroup," +
                        //客户
                        "customer as ocdbd_customer," +
                        //渠道主键
                        "id as ocdbd_id", new QFilter[]{qFilter}, null);
        return ocdbdChannel.groupBy(new String[]{"ocdbd_org","ocdbd_department","ocdbd_regiongroup"}).count("channelSumKS").finish();
    }


    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createBaseDataColumn("out_bizorg","bos_org","公司名称"));
        columns.add(createBaseDataColumn("out_bizdept","bos_org","经营团队"));
        columns.add(createBaseDataColumn("nckd_ywtd","nckd_regiongroup","片区"));
//        columns.add(createBaseDataColumn("","","责任人"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"责任人"));

        columns.add(createReportColumn("",ReportColumn.TYPE_INTEGER,"属性"));
        columns.add(createReportColumn("channelSumKS",ReportColumn.TYPE_INTEGER,"登记客商数"));

        ReportColumnGroup jykh = createReportColumnGroup("jykh","交易客户情况");
        ReportColumnGroup monthjykh = createReportColumnGroup("monthjykh","月度");
        monthjykh.getChildren().add(createReportColumn("month_nckd_ydjykh",ReportColumn.TYPE_INTEGER,"月度交易客户数目标"));
        monthjykh.getChildren().add(createReportColumn("monthSumCustomer",ReportColumn.TYPE_INTEGER,"本月客户"));
        monthjykh.getChildren().add(createReportColumn("monthCustomer",ReportColumn.TYPE_TEXT,"月客户完成率"));
        ReportColumnGroup yearjykh = createReportColumnGroup("yearjykh","年度");
        yearjykh.getChildren().add(createReportColumn("year_nckd_ydjykh",ReportColumn.TYPE_INTEGER,"交易客户目标"));
        yearjykh.getChildren().add(createReportColumn("yearSumCustomer",ReportColumn.TYPE_INTEGER,"交易客户累计"));
        yearjykh.getChildren().add(createReportColumn("yearCustomer",ReportColumn.TYPE_TEXT,"交易客户完成"));
        jykh.getChildren().add(monthjykh);
        jykh.getChildren().add(yearjykh);
        columns.add(jykh);

        ReportColumnGroup jpjykh = createReportColumnGroup("jpjykh","精品客户情况");
        ReportColumnGroup jpmonthjykh = createReportColumnGroup("jpmonthjykh","月度");
        jpmonthjykh.getChildren().add(createReportColumn("month_nckd_ydjpzdkh",ReportColumn.TYPE_INTEGER,"月度精品客户目标"));
        jpmonthjykh.getChildren().add(createReportColumn("monthSumJPCustomer",ReportColumn.TYPE_INTEGER,"本月精品客户"));
        jpmonthjykh.getChildren().add(createReportColumn("monthJPCustomer",ReportColumn.TYPE_TEXT,"月度精品客户完成"));
        ReportColumnGroup jpyearjykh =  createReportColumnGroup("jpyearjykh","年度");
        jpyearjykh.getChildren().add(createReportColumn("year_nckd_ydjpzdkh",ReportColumn.TYPE_INTEGER,"精品客户目标"));
        jpyearjykh.getChildren().add(createReportColumn("yearSumJPCustomer",ReportColumn.TYPE_INTEGER,"精品客户累计"));
        jpyearjykh.getChildren().add(createReportColumn("yearJPCustomer",ReportColumn.TYPE_TEXT,"精品客户完成"));
        jpjykh.getChildren().add(jpmonthjykh);
        jpjykh.getChildren().add(jpyearjykh);
        columns.add(jpjykh);

//        columns.add(createBaseDataColumn("","","所属地区"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"所属地区"));
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

    public ReportColumn createBaseDataColumn(String fileKey, String entityId, String name) {
        ReportColumn column = ReportColumn.createBaseDataColumn(fileKey, entityId);
        column.setFieldKey(fileKey);
        column.setCaption(new LocaleString(name));
        return column;
    }

    public ReportColumnGroup createReportColumnGroup(String fileKey, String name) {
        ReportColumnGroup group = new ReportColumnGroup();
        group.setFieldKey(fileKey);
        group.setCaption(new LocaleString(name));
        return group;
    }
}