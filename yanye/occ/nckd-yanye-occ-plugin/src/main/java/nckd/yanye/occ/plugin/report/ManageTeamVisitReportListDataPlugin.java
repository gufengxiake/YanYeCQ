package nckd.yanye.occ.plugin.report;

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
 * 经营团队情况一览表（拜访）-报表取数插件
 * 表单标识：nckd_manageteamvisit_rpt
 * author:zhangzhilong
 * date:2024/09/14
 */
public class ManageTeamVisitReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        ArrayList<QFilter> qFilters = new ArrayList<>();
        //过滤组织限定名称带华康的公司
        QFilter orgFilter = new QFilter("saleorginfonum.name", QCP.like,"%华康%");
        //单据需已审核
        orgFilter.and("status",QCP.equals,"C");
        qFilters.add(orgFilter);

        FilterInfo filter = reportQueryParam.getFilter();
        int year =  DateUtil.year(new Date());
        int month = DateUtil.month(new Date()) + 1;
        if(filter.getDate("nckd_date_q") != null){
            year =  DateUtil.year(filter.getDate("nckd_date_q"));
            month = DateUtil.month(filter.getDate("nckd_date_q")) + 1;
        }

//        qFilters.add(new QFilter("biztime",QCP.large_equals, DateUtil.beginOfYear(new SimpleDateFormat("yyyy-MM").parse(year + "-01")))
//                .and("biztime",QCP.less_equals, DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(year + "-"+ month))));
        DataSet ocdbdChannel = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocdbd_channel",
                //销售组织
                "slaeorginfo.saleorginfonum as ocdbd_saleorginfonum," +
                        //部门
                        "slaeorginfo.department as ocdbd_department, " +
                        //渠道主键
                        "id as ocdbd_id", qFilters.toArray(new QFilter[0]), null);
        //查询拜访记录
        DataSet hmuaSfaBfRecord = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "hmua_sfa_bf_record",
                //门店
                "hmua_visit_stores," +
                        //经销商
                        "hmua_bf_cust, " +
                        //渠道主键
                        "id as sfa_id", new QFilter[]{new QFilter("billstatus", QCP.equals, "B")}, null);
        //根据门店汇总
        DataSet mdsumbf = hmuaSfaBfRecord.groupBy(new String[]{"hmua_visit_stores"}).count("sumbf").finish()
                .select("hmua_visit_stores as qdzj", "sumbf");
        //根据经销商汇总
        DataSet jxssumbf = hmuaSfaBfRecord.groupBy(new String[]{"hmua_bf_cust"}).count("sumbf").finish()
                .select("hmua_bf_cust as qdzj", "sumbf");

        //过滤渠道主键为0的数据
        hmuaSfaBfRecord = mdsumbf.union(jxssumbf).filter(" qdzj <> 0");

        //根据组织汇总渠道数量
        DataSet ocdbdSumks = ocdbdChannel.copy().groupBy(new String[]{"ocdbd_saleorginfonum"}).count("ocdbd_sumks").finish();

        //根据主键关联拜访数据
        ocdbdChannel = ocdbdChannel.leftJoin(hmuaSfaBfRecord).on("ocdbd_id","qdzj").select(new String[]{"ocdbd_saleorginfonum","ocdbd_department","sumbf"}).finish();

        //根据组织和部门对拜访数据进行汇总
        ocdbdChannel = ocdbdChannel.groupBy(new String[]{"ocdbd_saleorginfonum","ocdbd_department"}).sum("sumbf").finish();

        //关联获取汇总的渠道数量
        ocdbdChannel = ocdbdChannel.leftJoin(ocdbdSumks).on("ocdbd_saleorginfonum","ocdbd_saleorginfonum").select(new String[]{"ocdbd_saleorginfonum","ocdbd_department","sumbf","ocdbd_sumks"}).finish();

        DataSet hkYearPlan = getHKYearPlan(year, month);
        ocdbdChannel = ocdbdChannel.leftJoin(hkYearPlan).on("ocdbd_saleorginfonum","hk_org").on("ocdbd_department","hk_jytd")
                .select(new String[]{"ocdbd_saleorginfonum","ocdbd_department","sumbf","ocdbd_sumks","year_hk_ydbfkh"}).finish();


        return ocdbdChannel;

    }

    //获取华康年度计划表数据
    public DataSet getHKYearPlan(int year,int month) throws ParseException {

        //组织
        String hkFields = "org as hk_org," +
                //经营团队
                "entryentity.nckd_jytd as hk_jytd," +
                "entryentity.nckd_date as hk_date," +
                //月度拜访客户数目标
                "entryentity.nckd_ydbfkh as hk_ydbfkh" ;
        //计算汇总本月月度小包装盐销售目标
        String date= year + "-" + month;
        QFilter hkFilter = new QFilter("entryentity.nckd_date", QCP.large_equals, DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(date)))
                .and("entryentity.nckd_date",QCP.less_equals, DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(date)));
        DataSet nckdHkndjhb = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "nckd_hkndjhb", hkFields, new QFilter[]{hkFilter}, null);
        //汇总年度目标
        nckdHkndjhb = nckdHkndjhb.groupBy(new String[]{"hk_org", "hk_jytd"})
                .sum("hk_ydbfkh", "year_hk_ydbfkh").finish();

        return nckdHkndjhb;
    }
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        ReportColumn saleorginfonum = ReportColumn.createBaseDataColumn("ocdbd_saleorginfonum", "bos_org");
        saleorginfonum.setCaption(new LocaleString("公司名称"));
        columns.add(saleorginfonum);
        ReportColumn department = ReportColumn.createBaseDataColumn("ocdbd_department", "bos_org");
        department.setCaption(new LocaleString("经营团队"));
        columns.add(department);
        columns.add(createReportColumn("ocdbd_sumks",ReportColumn.TYPE_DECIMAL,"登记客商数"));
        columns.add(createReportColumn("lbfkhs",ReportColumn.TYPE_DECIMAL,"零拜访客户数"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"年平均次数"));
        columns.add(createReportColumn("year_hk_ydbfkh",ReportColumn.TYPE_DECIMAL,"月度拜访目标"));
        columns.add(createReportColumn("sumbf",ReportColumn.TYPE_DECIMAL,"当月拜访"));
        columns.add(createReportColumn("",ReportColumn.TYPE_DECIMAL,"当月餐饮"));
        columns.add(createReportColumn("nckd_ybffgl",ReportColumn.TYPE_TEXT,"月拜访覆盖率"));
        columns.add(createReportColumn("nckd_ybfwcl",ReportColumn.TYPE_TEXT,"月拜访完成率"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"餐饮月占比"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"餐饮年占比"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"当月江盐"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"当月中盐"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"当月雪天"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"当月外盐"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"月餐饮江盐"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"月餐饮中盐"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"月餐饮雪天"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"月餐饮外盐"));

        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"餐饮江盐"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"餐饮中盐"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"餐饮雪天"));
        columns.add(createReportColumn("",ReportColumn.TYPE_TEXT,"餐饮外盐"));

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