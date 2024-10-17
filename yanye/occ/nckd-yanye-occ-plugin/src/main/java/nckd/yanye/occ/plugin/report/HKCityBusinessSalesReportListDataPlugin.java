package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.dataentity.entity.DynamicObject;
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
 *
 */

/**
 * 华康地市公司业务情况（销售同比）-报表取数插件
 * 表单标识：nckd_hkcitybusisal_rpt
 * @author zhangzhilong
 * @since 2024/10/11
 */
public class HKCityBusinessSalesReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        List<QFilter> qFilters = new ArrayList<>();
        QFilter initFilter = new QFilter("bizorg.name", QCP.like,"%华康%");
        initFilter.and("billstatus",QCP.equals,"C");
        qFilters.add(initFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        //公司过滤
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if (nckdOrgQ != null) {
            Long pkValue = (Long) nckdOrgQ.getPkValue();
            qFilters.add(new QFilter("bizorg",QCP.equals,pkValue));
        }
        //日期过滤,默认当前年月
        Date nckdDateQ = filter.getDate("nckd_date_q");
        if(nckdDateQ == null){
            nckdDateQ = new Date();
        }
        int lastYear = DateUtil.year(nckdDateQ) - 1;
        int month = DateUtil.month(nckdDateQ) + 1;
        Date lastYearDate = new SimpleDateFormat("yyyy-MM").parse(lastYear + "-" + month);
        qFilters.add(new QFilter("biztime",QCP.large_equals,DateUtil.beginOfYear(lastYearDate))
                .and("biztime",QCP.less_equals,DateUtil.endOfMonth(nckdDateQ)));
        String fields = "bizorg as out_org," +
                //公司
                "bizorg.name as out_orgname," +
                //物料分类
                "billentry.material.masterid.group as out_group," +
                "billentry.material.masterid.group.name as out_groupname," +
                //基本数量
                "billentry.baseqty as out_baseqty," +
                //金额
                "billentry.amount as out_amount," +
                //配送渠道
                "nckd_deliverchannel," +
                //业务日期
                "biztime as out_biztime" ;
        DataSet salOutBill = QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", fields, qFilters.toArray(new QFilter[0]), null);

        //数据按时间分为今年/本月和去年/去年今月再进行合并
        DataSet yearDS = salOutBill.filter("out_biztime >= to_date('" + DateUtil.beginOfYear(nckdDateQ) + "','yyyy-MM-dd hh:mm:ss')").addField("1", "year");
        yearDS = yearDS.union(salOutBill.filter("out_biztime < to_date('" + DateUtil.beginOfYear(nckdDateQ) + "','yyyy-MM-dd hh:mm:ss')").addField("2", "year"))
                .union(salOutBill.filter("out_biztime >= to_date('" + DateUtil.beginOfMonth(nckdDateQ) + "','yyyy-MM-dd hh:mm:ss')")
                        .filter("out_biztime <= to_date('" + DateUtil.endOfMonth(nckdDateQ) + "','yyyy-MM-dd hh:mm:ss')").addField("3", "year"))
                .union(salOutBill.filter("out_biztime >= to_date('" + DateUtil.beginOfMonth(lastYearDate) + "','yyyy-MM-dd hh:mm:ss')")
                        .filter("out_biztime <= to_date('" + DateUtil.endOfMonth(lastYearDate) + "','yyyy-MM-dd hh:mm:ss')").addField("4", "year"));

        //根据物料分类和年份进行汇总
        GroupbyDataSet groupbyDataSet = yearDS.groupBy(new String[]{"out_org", "out_orgname"});
        String [] salts= {"小包盐","竞品盐","深井盐","高端盐","高端产品","果蔬盐","小苏打","深海盐","晶粒盐"};
        String [] saltEen= {"xby","jpy","sjy","gdy","gdcp","gsy","xsd","shy","jly"};
        for (int i = 0; i < salts.length ; i++) {
            groupbyDataSet.sum("case when year = 1 and out_groupname like '%" + salts[i] + "%' then out_baseqty else 0 end", saltEen[i] + "QtyThisYear")
                    .sum("case when year = 2 and out_groupname like '%" + salts[i] + "%' then out_baseqty else 0 end", saltEen[i] + "QtyLastYear")
                    .sum("case when year = 1 and out_groupname like '%" + salts[i] + "%' then out_amount else 0 end", saltEen[i] + "AmountThisYear")
                    .sum("case when year = 2 and out_groupname like '%" + salts[i] + "%' then out_amount else 0 end", saltEen[i] + "AmountLastYear");
        }
        groupbyDataSet.sum("case when year = 3 and out_groupname like '%小包盐%' then out_baseqty else 0 end","XBYThisMonth")
                .sum("case when year = 4 and out_groupname like '%小包盐%' then out_baseqty else 0 end","XBYLastMonth");
//        groupbyDataSet.sum("case when year = 3 and out_groupname like '%待分类%' then out_baseqty else 0 end","XBYThisMonth")
//                .sum("case when year = 4 and out_groupname like '%待分类%' then out_baseqty else 0 end","XBYLastMonth");
        yearDS = groupbyDataSet.finish();

        //关联获取月度目标数
        yearDS = yearDS.leftJoin(this.getHK(nckdDateQ)).on("out_org","hk_org").select(yearDS.getRowMeta().getFieldNames(),new String[]{"month_nckd_ydxbzy","year_nckd_ydxbzy"}).finish();

        //关联渠道客商数
        yearDS = yearDS.leftJoin(this.getChannelSumKS()).on("out_org","ocdbd_org").select(yearDS.getRowMeta().getFieldNames(),new String[]{"channelSumKS"}).finish();

        //获取渠道信息中的渠道分类信息
        List<Long> nckdDeliverchannel = DataSetToList.getOneToList(salOutBill, "nckd_deliverchannel");
        QFilter channelFilter = new QFilter("id",QCP.in,nckdDeliverchannel.toArray(new Long[0]));
        DataSet ocdbdChannel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel",
                //渠道分类
                "channelclassentity.channelclass.name as ocdbd_channelclass," +
                        //渠道主键
                        "id as ocdbd_id", new QFilter[]{channelFilter}, null);
        //关联渠道
        DataSet salChannel = salOutBill.leftJoin(ocdbdChannel).on("nckd_deliverchannel", "ocdbd_id").select(salOutBill.getRowMeta().getFieldNames(), new String[]{"ocdbd_channelclass"}).finish();
        //计算渠道分类不同的累计销售数量
        salChannel = salChannel.filter("out_biztime >= to_date('" + DateUtil.beginOfYear(nckdDateQ) + "','yyyy-MM-dd hh:mm:ss')").groupBy(new String[]{"out_org"})
                .sum("case when ocdbd_channelclass like '%批发商%' then out_baseqty else 0 end","ljep")
                .sum("case when ocdbd_channelclass like '%终端%' then out_baseqty else 0 end","ljzd")
                .sum("case when ocdbd_channelclass like '%商超%' then out_baseqty else 0 end","ljsc")
                .sum("case when ocdbd_channelclass like '%餐饮%' then out_baseqty else 0 end","ljcy")
                .sum("case when ocdbd_channelclass like '%电商%' then out_baseqty else 0 end","ljds")
                .finish();

        yearDS = yearDS.leftJoin(salChannel).on("out_org","out_org").select(yearDS.getRowMeta().getFieldNames(),new String[]{"ljep","ljzd","ljsc","ljcy","ljds"}).finish();
        salOutBill.close();
        ocdbdChannel.close();
        salChannel.close();
        return yearDS;
    }

    //获取渠道客商数
    public DataSet getChannelSumKS() {
        QFilter qFilter = new QFilter("slaeorginfo.saleorginfonum.name", QCP.like, "%华康%");
        qFilter.and("status", QCP.equals, "C");
        //获取渠道信息
        DataSet ocdbdChannel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel",
                //销售组织
                "slaeorginfo.saleorginfonum as ocdbd_org," +
                        //客户
                        "customer as ocdbd_customer," +
                        //渠道主键
                        "id as ocdbd_id", new QFilter[]{qFilter}, null);
        return ocdbdChannel.groupBy(new String[]{"ocdbd_org"}).count("channelSumKS").finish();
    }

    /**
     * 获取华康年度计划表中月度小包盐目标
     *
     * @param nckdDateQ
     * @return
     */
    public DataSet getHK(Date nckdDateQ){
        //获取华康年度计划表
        String hkFields = "org as hk_org," +
                "entryentity.nckd_date as hk_date," +
                //月度小包盐目标
                "entryentity.nckd_ydxbzy as nckd_ydxbzy" ;
        QFilter hkFilter = new QFilter("entryentity.nckd_date", QCP.large_equals,DateUtil.beginOfYear(nckdDateQ)).and("entryentity.nckd_date",QCP.less_equals, DateUtil.endOfMonth(nckdDateQ));
        DataSet nckdHkndjhb = QueryServiceHelper.queryDataSet(this.getClass().getName(),"nckd_hkndjhb", hkFields, new QFilter[]{hkFilter}, null);
        //汇总月度和年度目标并关联
        DataSet hkMonthSum = nckdHkndjhb.filter("hk_date >= to_date('" + DateUtil.beginOfMonth(nckdDateQ) + "','yyyy-MM-dd hh:mm:ss')").groupBy(new String[]{"hk_org"}).sum("nckd_ydxbzy","month_nckd_ydxbzy").finish();
        nckdHkndjhb = nckdHkndjhb.groupBy(new String[]{"hk_org"}).sum("nckd_ydxbzy","year_nckd_ydxbzy").finish();

        nckdHkndjhb = nckdHkndjhb.leftJoin(hkMonthSum).on("hk_org","hk_org").select(new String[]{"hk_org","month_nckd_ydxbzy","year_nckd_ydxbzy"}).finish();
        hkMonthSum.close();
        return nckdHkndjhb;
    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("out_orgname",ReportColumn.TYPE_TEXT,"公司简称"));

        ReportColumnGroup month = createReportColumnGroup("month","月度情况");
        month.getChildren().add(createReportColumn("month_nckd_ydxbzy",ReportColumn.TYPE_INTEGER,"月度小包盐目标"));
        month.getChildren().add(createReportColumn("XBYThisMonth",ReportColumn.TYPE_DECIMAL,"本月销量"));
        month.getChildren().add(createReportColumn("XBYLastMonth",ReportColumn.TYPE_DECIMAL,"同期"));
        month.getChildren().add(createReportColumn("xbtb",ReportColumn.TYPE_TEXT,"同比"));
        month.getChildren().add(createReportColumn("ydxbwc",ReportColumn.TYPE_TEXT,"月度小包完成"));
        columns.add(month);

        ReportColumnGroup year = createReportColumnGroup("year","年度情况");
        year.getChildren().add(createReportColumn("year_nckd_ydxbzy",ReportColumn.TYPE_INTEGER,"小包盐目标"));
        year.getChildren().add(createReportColumn("xbyQtyThisYear",ReportColumn.TYPE_DECIMAL,"小包盐数量"));
        year.getChildren().add(createReportColumn("xbyQtyLastYear",ReportColumn.TYPE_DECIMAL,"去年累计"));
        year.getChildren().add(createReportColumn("qntb",ReportColumn.TYPE_TEXT,"去年同比"));
        year.getChildren().add(createReportColumn("xswcl",ReportColumn.TYPE_TEXT,"销量完成率"));
        year.getChildren().add(createReportColumn("xbyAmountThisYear",ReportColumn.TYPE_DECIMAL,"小包均价"));
        columns.add(year);

        ReportColumnGroup jpy = createReportColumnGroup("jpy","竞品盐");
        jpy.getChildren().add(createReportColumn("jpyQtyThisYear",ReportColumn.TYPE_DECIMAL,"竞品数量"));
        jpy.getChildren().add(createReportColumn("jpyAmountThisYear",ReportColumn.TYPE_DECIMAL,"竞品盐均价"));
        jpy.getChildren().add(createReportColumn("jpzb",ReportColumn.TYPE_TEXT,"竞品占比"));
        jpy.getChildren().add(createReportColumn("jpyQtyLastYear",ReportColumn.TYPE_DECIMAL,"竞品盐同期"));
        jpy.getChildren().add(createReportColumn("jptb",ReportColumn.TYPE_TEXT,"竞品同比"));
        columns.add(jpy);

        ReportColumnGroup sjy = createReportColumnGroup("sjy","深井盐");
        sjy.getChildren().add(createReportColumn("sjyQtyThisYear",ReportColumn.TYPE_DECIMAL,"深井盐数量"));
        sjy.getChildren().add(createReportColumn("sjyAmountThisYear",ReportColumn.TYPE_DECIMAL,"深井盐均价"));
        sjy.getChildren().add(createReportColumn("sjzb",ReportColumn.TYPE_TEXT,"深井占比"));
        sjy.getChildren().add(createReportColumn("sjyQtyLastYear",ReportColumn.TYPE_DECIMAL,"深井盐同期"));
        sjy.getChildren().add(createReportColumn("sjtb",ReportColumn.TYPE_TEXT,"深井同比"));
        columns.add(sjy);

        ReportColumnGroup gdy = createReportColumnGroup("gdy","高端盐");
        gdy.getChildren().add(createReportColumn("gdyQtyThisYear",ReportColumn.TYPE_DECIMAL,"高端盐数量"));
        gdy.getChildren().add(createReportColumn("gdyAmountThisYear",ReportColumn.TYPE_DECIMAL,"高端盐均价"));
        gdy.getChildren().add(createReportColumn("gdzb",ReportColumn.TYPE_TEXT,"高端占比"));
        gdy.getChildren().add(createReportColumn("gdyQtyLastYear",ReportColumn.TYPE_DECIMAL,"高端盐同期"));
        gdy.getChildren().add(createReportColumn("gdtb",ReportColumn.TYPE_TEXT,"高端同比"));
        columns.add(gdy);

        ReportColumnGroup gdcpsr = createReportColumnGroup("gdcpsr","高端产品收入");
        gdcpsr.getChildren().add(createReportColumn("gdcpAmountThisYear",ReportColumn.TYPE_DECIMAL,"高端产品收入"));
        gdcpsr.getChildren().add(createReportColumn("gdcpAmountLastYear",ReportColumn.TYPE_DECIMAL,"同期高端产品收入"));
        gdcpsr.getChildren().add(createReportColumn("gdsrtb",ReportColumn.TYPE_TEXT,"高端收入同比"));
        columns.add(gdcpsr);

        ReportColumnGroup gdcpsl = createReportColumnGroup("gdcpsl","高端产品数量");
        gdcpsl.getChildren().add(createReportColumn("gdcpQtyThisYear",ReportColumn.TYPE_DECIMAL,"高端产品数量"));
        gdcpsl.getChildren().add(createReportColumn("gdcpQtyLastYear",ReportColumn.TYPE_DECIMAL,"高端产品数量同期"));
        gdcpsl.getChildren().add(createReportColumn("gdsltb",ReportColumn.TYPE_TEXT,"高端数量同比"));
        columns.add(gdcpsl);

        ReportColumnGroup gsy = createReportColumnGroup("gsy","果蔬盐");
        gsy.getChildren().add(createReportColumn("gsyQtyThisYear",ReportColumn.TYPE_DECIMAL,"果蔬盐销量"));
        gsy.getChildren().add(createReportColumn("gsyQtyLastYear",ReportColumn.TYPE_DECIMAL,"果蔬盐销量（上）"));
        gsy.getChildren().add(createReportColumn("gsytb",ReportColumn.TYPE_TEXT,"果蔬盐同比"));
        columns.add(gsy);

        ReportColumnGroup xsd = createReportColumnGroup("xsd","小苏打");
        xsd.getChildren().add(createReportColumn("xsdQtyThisYear",ReportColumn.TYPE_DECIMAL,"小苏打销量"));
        xsd.getChildren().add(createReportColumn("xsdQtyLastYear",ReportColumn.TYPE_DECIMAL,"小苏打销量（上）"));
        xsd.getChildren().add(createReportColumn("xsdtb",ReportColumn.TYPE_TEXT,"小苏打同比"));
        columns.add(xsd);

        ReportColumnGroup shy = createReportColumnGroup("shy","深海盐");
        shy.getChildren().add(createReportColumn("shyQtyThisYear",ReportColumn.TYPE_DECIMAL,"深海盐销量"));
        shy.getChildren().add(createReportColumn("shyQtyLastYear",ReportColumn.TYPE_DECIMAL,"深海盐销量（上）"));
        shy.getChildren().add(createReportColumn("shytb",ReportColumn.TYPE_TEXT,"深海盐同比"));
        columns.add(shy);

        ReportColumnGroup jly = createReportColumnGroup("jly","晶粒盐");
        jly.getChildren().add(createReportColumn("jlyQtyThisYear",ReportColumn.TYPE_DECIMAL,"晶粒盐销量"));
        jly.getChildren().add(createReportColumn("jlyQtyLastYear",ReportColumn.TYPE_DECIMAL,"晶粒盐销量（上）"));
        jly.getChildren().add(createReportColumn("jlytb",ReportColumn.TYPE_TEXT,"晶粒盐同比"));
        columns.add(jly);

        ReportColumnGroup channel = createReportColumnGroup("channel","渠道情况");
        channel.getChildren().add(createReportColumn("ljep",ReportColumn.TYPE_DECIMAL,"累计二批"));
        channel.getChildren().add(createReportColumn("ljzd",ReportColumn.TYPE_DECIMAL,"累计终端"));
        channel.getChildren().add(createReportColumn("ljsc",ReportColumn.TYPE_DECIMAL,"累计商超"));
        channel.getChildren().add(createReportColumn("ljcy",ReportColumn.TYPE_DECIMAL,"累计餐饮"));
        channel.getChildren().add(createReportColumn("ljds",ReportColumn.TYPE_DECIMAL,"累计电商"));
        columns.add(channel);

        ReportColumnGroup ml = createReportColumnGroup("ml","毛利情况");

        ReportColumnGroup ydml = createReportColumnGroup("ydml","月度毛利");
        ydml.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"月度毛利目标"));
        ydml.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"本月毛利"));
        ydml.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"月度毛利完成"));
        ml.getChildren().add(ydml);

        ReportColumnGroup ndml = createReportColumnGroup("ndml","年度毛利");
        ndml.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"毛利目标"));
        ndml.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"总毛利"));
        ndml.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"毛利完成率"));
        ml.getChildren().add(ndml);

        ReportColumnGroup qz = createReportColumnGroup("qz","其中");
        qz.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"盐毛利（含竞品）"));
        qz.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"大包盐毛利"));
        qz.getChildren().add(createReportColumn("",ReportColumn.TYPE_TEXT,"非盐毛利"));
        ml.getChildren().add(qz);
        columns.add(ml);

        columns.add(createReportColumn("channelSumKS",ReportColumn.TYPE_TEXT,"登记客商数"));
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

    public ReportColumnGroup createReportColumnGroup(String fileKey, String name) {
        ReportColumnGroup group = new ReportColumnGroup();
        group.setFieldKey(fileKey);
        group.setCaption(new LocaleString(name));
        return group;
    }

}