package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
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
 * 片区情况一览表（拜访）-报表取数插件
 * 表单标识：nckd_areasituationvis_rpt
 * author:zhangzhilong
 * date:2024/10/12
 */
public class AreaSituationVisitReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {


    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //过滤组织限定名称带华康的公司
        QFilter initFilter = new QFilter("slaeorginfo.saleorginfonum.name", QCP.like, "%华康%");
        //单据需已审核
        initFilter.and("status", QCP.equals, "C");
        qFilters.add(initFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        //获取公司过滤
        DynamicObject nckdOrgQ = filter.getDynamicObject("nckd_org_q");
        if (nckdOrgQ != null) {
            Long pkValue = (Long) nckdOrgQ.getPkValue();
            qFilters.add(new QFilter("slaeorginfo.saleorginfonum",QCP.equals,pkValue));
        }
        //获取查询日期过滤 如果日期为空则获取当前日期
        Date nckdDateQ = filter.getDate("nckd_date_q");
        if (nckdDateQ == null) {
            nckdDateQ = new Date();
        }
        DateTime beginOfYear = DateUtil.beginOfYear(nckdDateQ);
        DateTime beginOfMonth = DateUtil.beginOfMonth(nckdDateQ);
        DateTime endOfMonth = DateUtil.endOfMonth(nckdDateQ);
        //获取渠道信息
        DataSet ocdbdChannel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel",
                //销售组织
                "slaeorginfo.saleorginfonum as ocdbd_org," +
                        "slaeorginfo.saleorginfonum.name as ocdbd_orgname," +
                        //客户
                        "customer as ocdbd_customer," +
                        //渠道主键
                        "id as ocdbd_id", qFilters.toArray(new QFilter[0]), null);
        //获取客户餐饮信息并关联
        DataSet cyCustomer = this.getCYCustomer();
        ocdbdChannel = ocdbdChannel.leftJoin(cyCustomer).on("ocdbd_customer", "id").select(ocdbdChannel.getRowMeta().getFieldNames(), new String[]{"group"}).finish();

        DataSet channelBfRecord = this.getBfRecord(ocdbdChannel,beginOfMonth,endOfMonth);

//        //关联拜访
//        DataSet ocdbdChannelCopy = ocdbdChannel.copy().leftJoin(hmuaSfaBfRecord).on("ocdbd_id","qdzj")
//                .select(new String[]{"ocdbd_org","ocdbd_orgname","sumbf","jysumbf","zysumbf","xtsumbf","wysumbf","group"}).finish();

        //计算0客户数 即关联拜访记录为空的渠道档案
        DataSet sumzerokh = channelBfRecord.filter("sumbf = null").groupBy(new String[]{"ocdbd_org", "ocdbd_orgname"})
                .count("sumzerokh").finish();

        //计算餐饮客户数
        DataSet sumcy = channelBfRecord.filter("group = '餐饮'").groupBy(new String[]{"ocdbd_org", "ocdbd_orgname"})
                .sum("sumbf", "sumcy").sum("jysumbf","jysumcy").sum("zysumbf","zysumcy")
                .sum("xtsumbf","xtsumcy").sum("wysumbf","wysumcy").finish();

        //计算年度拜访记录
        DataSet yearBfRecord = this.getBfRecord(ocdbdChannel,beginOfYear, endOfMonth);

        DataSet yearSumCY = yearBfRecord.filter("group = '餐饮'").groupBy(new String[]{"ocdbd_org", "ocdbd_orgname"})
                .sum("sumbf", "yearsumcy").sum("jysumbf","yearjysumcy").sum("zysumbf","yearzysumcy")
                .sum("xtsumbf","yearxtsumcy").sum("wysumbf","yearwysumcy").finish();

        yearBfRecord = yearBfRecord.groupBy(new String[]{"ocdbd_org", "ocdbd_orgname"}).sum("sumbf","yearsumbf").finish()
                .leftJoin(yearSumCY).on("ocdbd_org","ocdbd_org").select("ocdbd_org", "ocdbd_orgname","yearsumbf","yearsumcy","yearjysumcy","yearzysumcy","yearxtsumcy","yearwysumcy").finish();

        //计算同一个组织下的渠道数
        DataSet finish = channelBfRecord.groupBy(new String[]{"ocdbd_org","ocdbd_orgname"})
                .count("sumks").sum("sumbf").sum("jysumbf").sum("zysumbf").sum("xtsumbf").sum("wysumbf").finish();

        //关联0客户和餐饮客户
        finish = finish.leftJoin(sumzerokh).on("ocdbd_org","ocdbd_org")
                .select(finish.getRowMeta().getFieldNames(),new String[]{"sumzerokh"}).finish();

        finish = finish.leftJoin(sumcy).on("ocdbd_org","ocdbd_org")
                .select(finish.getRowMeta().getFieldNames(),new String[]{"jysumcy","zysumcy","xtsumcy","wysumcy","sumcy"}).finish();

        finish = finish.leftJoin(yearBfRecord).on("ocdbd_org","ocdbd_org")
                .select(finish.getRowMeta().getFieldNames(),new String[]{"yearsumbf","yearsumcy","yearjysumcy","yearzysumcy","yearxtsumcy","yearwysumcy"}).finish();
        //获取华康年度计划表
        String hkFields = "org as hk_org," +
                "entryentity.nckd_date as hk_date," +
                //月度拜访客户数目标
                "entryentity.nckd_ydbfkh as hk_ydbfkh" ;
        //计算汇总本月月度小包装盐销售目标
        QFilter hkFilter = new QFilter("entryentity.nckd_date", QCP.large_equals,beginOfMonth )
                .and("entryentity.nckd_date",QCP.less_equals, endOfMonth);
        DataSet nckdHkndjhb = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_hkndjhb", hkFields, new QFilter[]{hkFilter}, null);
        //汇总月度目标
        nckdHkndjhb = nckdHkndjhb.groupBy(new String[]{"hk_org"})
                .sum("hk_ydbfkh", "month_hk_ydbfkh").finish();
        //
        finish = finish.leftJoin(nckdHkndjhb).on("ocdbd_org","hk_org")
                .select(finish.getRowMeta().getFieldNames(),new String[]{"month_hk_ydbfkh"}).finish();

        return finish;
    }

    //获取餐饮客户
    public DataSet getCYCustomer(){
        //获取物料分类为餐饮的id
        List<Object> pks = QueryServiceHelper.queryPrimaryKeys("bd_customergroup", new QFilter[]{new QFilter("name", QCP.like, "%餐饮%")}, null, 1);

        return QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "bd_customer", "id,'餐饮' as group", new QFilter[]{new QFilter("nckd_group",QCP.in,pks.toArray(new Object[0]))}, null);
    }
    //
    public DataSet getBfRecord(DataSet ds,DateTime begin,DateTime end){
        //查询拜访记录
        QFilter bfFilter = new QFilter("billstatus", QCP.equals, "B")
                .and("hmua_bf_date",QCP.large_equals,begin)
                .and("hmua_bf_date",QCP.less_equals,end);
        DataSet hmuaSfaBfRecord = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "hmua_sfa_bf_record",
                //门店
                "hmua_visit_stores," +
                        //经销商
                        "hmua_bf_cust," +
                        //在售信息
                        "nckd_salitem, " +
                        //主键
                        "id as sfa_id", new QFilter[]{bfFilter}, null);
        //根据门店汇总
        DataSet mdsumbf = hmuaSfaBfRecord.groupBy(new String[]{"hmua_visit_stores"}).count("sumbf")
                .sum("case when nckd_salitem like '%A%' then 1 else 0 END","jysumbf")
                .sum("case when nckd_salitem like '%B%' then 1 else 0 END","zysumbf")
                .sum("case when nckd_salitem like '%C%' then 1 else 0 END","xtsumbf")
                .sum("case when nckd_salitem like '%D%' then 1 else 0 END","wysumbf").finish()
                .select("hmua_visit_stores as qdzj", "sumbf","jysumbf","zysumbf","xtsumbf","wysumbf");
        //根据经销商汇总
        DataSet jxssumbf = hmuaSfaBfRecord.groupBy(new String[]{"hmua_bf_cust"}).count("sumbf")
                .sum("case when nckd_salitem like '%A%' then 1 else 0 END","jysumbf")
                .sum("case when nckd_salitem like '%B%' then 1 else 0 END","zysumbf")
                .sum("case when nckd_salitem like '%C%' then 1 else 0 END","xtsumbf")
                .sum("case when nckd_salitem like '%D%' then 1 else 0 END","wysumbf").finish()
                .select("hmua_bf_cust as qdzj", "sumbf","jysumbf","zysumbf","xtsumbf","wysumbf");

        hmuaSfaBfRecord = mdsumbf.union(jxssumbf).filter(" qdzj <> 0");

        //关联拜访
        hmuaSfaBfRecord = ds.copy().leftJoin(hmuaSfaBfRecord).on("ocdbd_id","qdzj")
                .select(new String[]{"ocdbd_org","ocdbd_orgname","sumbf","jysumbf","zysumbf","xtsumbf","wysumbf","group"}).finish();

        mdsumbf.close();
        jxssumbf.close();
        return hmuaSfaBfRecord;

    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        columns.add(createReportColumn("ocdbd_orgname", ReportColumn.TYPE_TEXT,"公司简称"));
        columns.add(createReportColumn("sumks",ReportColumn.TYPE_INTEGER,"登记客商数"));
        columns.add(createReportColumn("sumzerokh",ReportColumn.TYPE_INTEGER,"零拜访客户"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"年平均次数"));

        ReportColumnGroup monthbfReportColumnGroup = new ReportColumnGroup();
        monthbfReportColumnGroup.setFieldKey("monthbf");
        monthbfReportColumnGroup.setCaption(new LocaleString("月度拜访完成情况"));
        monthbfReportColumnGroup.getChildren().add(createReportColumn("month_hk_ydbfkh",ReportColumn.TYPE_INTEGER,"月度拜访目标"));
        monthbfReportColumnGroup.getChildren().add(createReportColumn("sumbf",ReportColumn.TYPE_INTEGER,"当月拜访"));
        monthbfReportColumnGroup.getChildren().add(createReportColumn("sumcy",ReportColumn.TYPE_INTEGER,"当月餐饮"));
        monthbfReportColumnGroup.getChildren().add(createReportColumn("bfwcl",ReportColumn.TYPE_TEXT,"拜访完成率"));
        monthbfReportColumnGroup.getChildren().add(createReportColumn("bfwc",ReportColumn.TYPE_TEXT,"拜访完成"));
        columns.add(monthbfReportColumnGroup);

        ReportColumnGroup cybfReportColumnGroup = new ReportColumnGroup();
        cybfReportColumnGroup.setFieldKey("cybf");
        cybfReportColumnGroup.setCaption(new LocaleString("餐饮拜访指标"));
        cybfReportColumnGroup.getChildren().add(createReportColumn("monthsumcy",ReportColumn.TYPE_TEXT,"餐饮月占比"));
        cybfReportColumnGroup.getChildren().add(createReportColumn("yearsumcy",ReportColumn.TYPE_TEXT,"餐饮年占比"));
        columns.add(cybfReportColumnGroup);

        ReportColumnGroup dyglReportColumnGroup = new ReportColumnGroup();
        dyglReportColumnGroup.setFieldKey("dygl");
        dyglReportColumnGroup.setCaption(new LocaleString("当月各类拜访占比"));
        dyglReportColumnGroup.getChildren().add(createReportColumn("jysumbf",ReportColumn.TYPE_TEXT,"当月江盐"));
        dyglReportColumnGroup.getChildren().add(createReportColumn("zysumbf",ReportColumn.TYPE_TEXT,"当月中盐"));
        dyglReportColumnGroup.getChildren().add(createReportColumn("xtsumbf",ReportColumn.TYPE_TEXT,"当月雪天"));
        dyglReportColumnGroup.getChildren().add(createReportColumn("wysumbf",ReportColumn.TYPE_TEXT,"当月外盐"));
        columns.add(dyglReportColumnGroup);

        ReportColumnGroup cylReportColumnGroup = new ReportColumnGroup();
        cylReportColumnGroup.setFieldKey("cyl");
        cylReportColumnGroup.setCaption(new LocaleString("餐饮类拜访占比"));

        ReportColumnGroup cylydReportColumnGroup = new ReportColumnGroup();
        cylydReportColumnGroup.setFieldKey("cylyd");
        cylydReportColumnGroup.setCaption(new LocaleString("月度占比"));
        cylydReportColumnGroup.getChildren().add(createReportColumn("jysumcy",ReportColumn.TYPE_TEXT,"餐饮-本月江盐占比"));
        cylydReportColumnGroup.getChildren().add(createReportColumn("zysumcy",ReportColumn.TYPE_TEXT,"餐饮-本月中盐占比"));
        cylydReportColumnGroup.getChildren().add(createReportColumn("xtsumcy",ReportColumn.TYPE_TEXT,"餐饮-本月雪天占比"));
        cylydReportColumnGroup.getChildren().add(createReportColumn("wysumcy",ReportColumn.TYPE_TEXT,"餐饮-本月外盐占比"));

        ReportColumnGroup cylndReportColumnGroup = new ReportColumnGroup();
        cylndReportColumnGroup.setFieldKey("cylnd");
        cylndReportColumnGroup.setCaption(new LocaleString("年度占比"));
        cylndReportColumnGroup.getChildren().add(createReportColumn("yearjysumcy",ReportColumn.TYPE_TEXT,"餐饮-江盐占比"));
        cylndReportColumnGroup.getChildren().add(createReportColumn("yearzysumcy",ReportColumn.TYPE_TEXT,"餐饮-中盐占比"));
        cylndReportColumnGroup.getChildren().add(createReportColumn("yearxtsumcy",ReportColumn.TYPE_TEXT,"餐饮-雪天占比"));
        cylndReportColumnGroup.getChildren().add(createReportColumn("yearwysumcy",ReportColumn.TYPE_TEXT,"餐饮-外盐占比"));

        cylReportColumnGroup.getChildren().add(cylydReportColumnGroup);
        cylReportColumnGroup.getChildren().add(cylndReportColumnGroup);
        columns.add(cylReportColumnGroup);

        columns.add(createReportColumn("yearsumbf",ReportColumn.TYPE_INTEGER,"年度拜访数量"));
//        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,""));
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