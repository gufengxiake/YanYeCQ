package nckd.yanye.hr.report.shebao;

import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.db.SqlBuilder;
import kd.bos.entity.report.*;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 薪酬社保费用表-报表取数插件
 * 报表标识：nckd_salarysocialreport
 *
 * @author liuxiao
 * @since 2024-08-23
 */
public class SalarysocialReportListDataPlugin extends AbstractReportListDataPlugin {

    /**
     * 全部险种
     */
    private final String[] PROJECTS = {
            "养老保险",
            "医疗保险",
            "大病医疗保险",
            "失业保险",
            "工伤保险",
            "补充工伤保险",
            "住房公积金",
            "企业年金",
    };

    private final String[] FIELDS = {
            "ygbh", "ygxm", "qj",
            "sbb1", "gzb1", "pzjl1", "NullToZero(sbb1) - NullToZero(gzb1) - NullToZero(pzjl1) ce1",
            "sbb2", "gzb2", "pzjl2", "NullToZero(sbb2) - NullToZero(gzb2) - NullToZero(pzjl2) ce2",
            "sbb3", "gzb3", "pzjl3", "NullToZero(sbb3) - NullToZero(gzb3) - NullToZero(pzjl3) ce3",
            "sbb4", "gzb4", "pzjl4", "NullToZero(sbb4) - NullToZero(gzb4) - NullToZero(pzjl4) ce4",
            "sbb5", "gzb5", "pzjl5", "NullToZero(sbb5) - NullToZero(gzb5) - NullToZero(pzjl5) ce5",
            "sbb6", "gzb6", "pzjl6", "NullToZero(sbb6) - NullToZero(gzb6) - NullToZero(pzjl6) ce6",
            "sbb7", "gzb7", "pzjl7", "NullToZero(sbb7) - NullToZero(gzb7) - NullToZero(pzjl7) ce7",
            "sbb8", "gzb8", "pzjl8", "NullToZero(sbb8) - NullToZero(gzb8) - NullToZero(pzjl8) ce8",
    };


    @Override
    public DataSet query(ReportQueryParam queryParam, Object o) {
        Map<String, Object> customParam = queryParam.getCustomParam();
        FilterInfo filter = queryParam.getFilter();
        List<FilterItemInfo> filterItems = filter.getFilterItems();

        // 选择人员
        FilterItemInfo users = filter.getFilterItem("nckd_users");
        // 算发薪组织
        FilterItemInfo org = filter.getFilterItem("nckd_org");
        // 社保开始期间
        FilterItemInfo sbksqj = filter.getFilterItem("nckd_sbksqj");
        // 社保结束期间
        FilterItemInfo sbjsqj = filter.getFilterItem("nckd_sbjsqj");
        // 薪酬开始日期
        FilterItemInfo xcksrq = filter.getFilterItem("nckd_xcksrq");
        // 薪酬结束日期
        FilterItemInfo xcjsrq = filter.getFilterItem("nckd_xcjsrq");


        if (sbksqj.getValue() == null && xcksrq.getValue() == null) {
            throw new KDBizException("社保开始期间或薪酬开始日期至少录入一项");
        }

//        if (org.getValue() == null) {
//            throw new KDBizException("算发薪组织不能为空");
//        }

        /*
         * 社保表过滤
         */
        // 选择人员
        QFilter userQfilter = null;
        if (users.getValue() != null) {
            userQfilter = new QFilter("employee.empnumber",
                    QCP.in,
                    ((DynamicObjectCollection) users.getValue())
                            .stream()
                            .map(obj -> obj.getString("empnumber"))
                            .collect(Collectors.toList())
            );
        }

        // 社保开始期间
        QFilter sbksQfilter = null;
        if (sbksqj.getValue() != null) {
            sbksQfilter = new QFilter("sinsurperiod.perioddate",
                    QCP.large_equals,
                    ((DynamicObject) sbksqj.getValue()).getDate("perioddate")
            );
        }

        // 社保结束期间
        QFilter sbjsQfilter = null;
        if (sbjsqj.getValue() != null) {
            sbjsQfilter = new QFilter("sinsurperiod.perioddate",
                    QCP.less_equals,
                    ((DynamicObject) sbjsqj.getValue()).getDate("perioddate")
            );
        }

        // 社保表过滤条件
        QFilter[] sbbQfilters = new QFilter[]{userQfilter, sbksQfilter, sbjsQfilter};

        // 社保表数据
        DataSet sbbDataSet = getSbbDataSet(sbbQfilters);

        /*
         * 工资表过滤
         */
        StringBuilder sqlFilter = new StringBuilder();
        sqlFilter.append("where 1=1");
        // 薪酬开始日期
        if (xcksrq.getValue() != null) {
            sqlFilter.append(" and a.fbelongperiod >= '" + xcksrq.getDate() + "'");
        }
        // 薪酬结束日期
        if (xcjsrq.getValue() != null) {
            sqlFilter.append(" and a.fbelongperiod <= '" + xcjsrq.getDate() + "'");
        }
        // 算发薪组织
        if (org.getValue() != null) {
            sqlFilter.append(" and a.forgid = '" + ((DynamicObject) org.getValue()).getLong("id") + "'");
        }


        // 工资表数据
        DataSet gzbDataSet = getGzbDataSet(sqlFilter);

        // 平账记录数据
        DataSet pzjlDataSet = getPzjlDataSet();


        DataSet finish = sbbDataSet
                .leftJoin(gzbDataSet)
                .on("ygbh", "ygbh")
                .on("ygxm", "ygxm")
                .on("qj", "qj").select(new String[]{
                        "ygbh", "ygxm", "qj",
                        "sbb1", "gzb1",
                        "sbb2", "gzb2",
                        "sbb3", "gzb3",
                        "sbb4", "gzb4",
                        "sbb5", "gzb5",
                        "sbb6", "gzb6",
                        "sbb7", "gzb7",
                        "sbb8", "gzb8",
                })
                .finish()
                .leftJoin(pzjlDataSet)
                .on("ygbh", "ygbh")
                .on("ygxm", "ygxm")
                .on("qj", "qj").select(FIELDS)
                .finish();

        DataSet groupSumRow = addGroupSumRow(finish);
        DataSet onlySumRow = addSumRow(finish);
        DataSet union = groupSumRow.union(onlySumRow);


        return union;
    }

    private DataSet addSumRow(DataSet finish) {
        GroupbyDataSet groupbyDataSet = finish.groupBy();
        for (int i = 0; i < PROJECTS.length; i++) {
            groupbyDataSet
                    .sum("sbb" + (i + 1))
                    .sum("gzb" + (i + 1))
                    .sum("pzjl" + (i + 1))
                    .sum("ce" + (i + 1));
        }
        DataSet sumDataSet = groupbyDataSet.finish().select(new String[]{
                        "null ygbh", "null ygxm", "'合计' qj",
                        "sbb1", "gzb1", "pzjl1", "ce1",
                        "sbb2", "gzb2", "pzjl2", "ce2",
                        "sbb3", "gzb3", "pzjl3", "ce3",
                        "sbb4", "gzb4", "pzjl4", "ce4",
                        "sbb5", "gzb5", "pzjl5", "ce5",
                        "sbb6", "gzb6", "pzjl6", "ce6",
                        "sbb7", "gzb7", "pzjl7", "ce7",
                        "sbb8", "gzb8", "pzjl8", "ce8",
                        "0 nckd_iflight"
                }
        );

        return sumDataSet;
    }

    /**
     * 添加分组合计行
     *
     * @param finish
     * @return
     */
    private DataSet addGroupSumRow(DataSet finish) {
        // 分组后，进行合计
        GroupbyDataSet groupbyDataSet = finish.groupBy(new String[]{"ygbh", "ygxm"});
        for (int i = 0; i < PROJECTS.length; i++) {
            groupbyDataSet
                    .sum("sbb" + (i + 1))
                    .sum("gzb" + (i + 1))
                    .sum("pzjl" + (i + 1))
                    .sum("ce" + (i + 1));
        }
        DataSet sumDataSet = groupbyDataSet.finish();

        // 添加高亮字段
        finish = finish.addField("0", "nckd_iflight");

        // union前，需要保证两个dataSet的字段序列一致，因此这里对sumDataSet对象重新排列字段序列
        sumDataSet = sumDataSet.select(new String[]{
                "ygbh", "ygxm +'的金额合计' ygxm", "null qj",
                "sbb1", "gzb1", "pzjl1", "ce1",
                "sbb2", "gzb2", "pzjl2", "ce2",
                "sbb3", "gzb3", "pzjl3", "ce3",
                "sbb4", "gzb4", "pzjl4", "ce4",
                "sbb5", "gzb5", "pzjl5", "ce5",
                "sbb6", "gzb6", "pzjl6", "ce6",
                "sbb7", "gzb7", "pzjl7", "ce7",
                "sbb8", "gzb8", "pzjl8", "ce8",
                "1 nckd_iflight"
        });
        DataSet unionDataSet = finish.union(sumDataSet).orderBy(new String[]{"ygxm"});

        return unionDataSet;
    }


    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        ReportColumn ygbh = createReportColumn("ygbh", ReportColumn.TYPE_TEXT, "员工编码");
        ReportColumn ygxm = createReportColumn("ygxm", ReportColumn.TYPE_TEXT, "员工姓名");
        ReportColumn sbqj = createReportColumn("qj", ReportColumn.TYPE_TEXT, "期间");

        columns.add(ygbh);
        columns.add(ygxm);
        columns.add(sbqj);

        // 险种
        for (int i = 0; i < PROJECTS.length; i++) {
            String project = PROJECTS[i];
            ReportColumnGroup welfaretype = createReportColumnGroup(String.valueOf(i), project);
            welfaretype.getChildren().add(createReportColumn("sbb" + (i + 1), ReportColumn.TYPE_DECIMAL, "社保表"));
            welfaretype.getChildren().add(createReportColumn("gzb" + (i + 1), ReportColumn.TYPE_DECIMAL, "工资表"));
            welfaretype.getChildren().add(createReportColumn("pzjl" + (i + 1), ReportColumn.TYPE_DECIMAL, "平账记录"));
            welfaretype.getChildren().add(createReportColumn("ce" + (i + 1), ReportColumn.TYPE_DECIMAL, "差额"));
            columns.add(welfaretype);
        }

        return columns;
    }


    /**
     * 社保表数据
     *
     * @param sbbQfilters
     * @return
     */
    private DataSet getSbbDataSet(QFilter[] sbbQfilters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PROJECTS.length; i++) {
            String project = PROJECTS[i];
            sb
                    .append("(CASE WHEN entryentity.insuranceitem.name = '").append(project).append("个人固定金额' THEN entryentity.amountvalue ELSE 0 END)")
                    .append("+(CASE WHEN entryentity.insuranceitem.name = '").append(project).append("个人缴费金额' THEN entryentity.amountvalue ELSE 0 END)")
                    .append("+(CASE WHEN entryentity.insuranceitem.name = '").append(project).append("个人补缴金额' THEN entryentity.amountvalue ELSE 0 END)")
                    .append(" as " + "sbb").append(i + 1);
            if (i != (PROJECTS.length - 1)) {
                sb.append(",");
            }
        }
        // 社保表数据
        DataSet sbbDataSet = QueryServiceHelper.queryDataSet(
                "hcsi_calperson",
                "hcsi_calperson",
                "" +
                        //员工编号
                        "empnumberdb ygbh," +
                        // 员工姓名
                        "namedb ygxm," +
                        // 社保区间
                        "sinsurperiod.perioddate qj," +
                        // 社保表
                        sb
                ,
                sbbQfilters,
                null
        );

        GroupbyDataSet groupbyDataSet = sbbDataSet.groupBy(new String[]{"ygbh", "ygxm", "to_char(qj,'yyyy-MM') qj"});

        for (int i = 0; i < PROJECTS.length; i++) {
            groupbyDataSet = groupbyDataSet.max("sbb" + (i + 1));
            if (i == (PROJECTS.length - 1)) {
                sbbDataSet = groupbyDataSet.finish();
            }
        }
        return sbbDataSet;
    }

    /**
     * 工资表数据
     *
     * @return
     */
    private DataSet getGzbDataSet(StringBuilder sqlFilter) {
        SqlBuilder sql = new SqlBuilder();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PROJECTS.length; i++) {
            String project = PROJECTS[i];
            sb
                    .append("CASE WHEN g.fname = '").append(project).append("个人缴费金额' THEN (" + "case when c.ftextvalue = ' ' then (case when c.fcalamountvalue = 0 then c.fnumvalue || '' else c.fcalamountvalue || '' end) else c.ftextvalue end" + ") END")
//                    .append("(CASE WHEN g.fname = '").append(project).append("个人缴费金额' THEN 0 ELSE 0 END)")
                    .append(" " + "gzb").append(i + 1);
            sb.append(",");
        }

        sql.append("select a.fid\n" +
                "     -- 员工工号\n" +
                "     , a.fempnumber              ygbh\n" +
                "     -- 员工姓名\n" +
                "     , a.fname                   ygxm\n" +
                "     -- 期间\n" +
                "     , d.fperioddate                 qj\n" +
                "     , " + sb +
                "     -- 项目名称\n" +
                "     g.fname                   项目名称\n" +
                "     -- 金额\n" +
                "     , case when c.ftextvalue = ' ' then (case when c.fcalamountvalue = 0 then c.fnumvalue || '' else c.fcalamountvalue || '' end) else c.ftextvalue end 项目数据\n" +
                "         from t_hsas_calperson a -- 核算名单\n" +
                "         inner join t_hsas_caltable b on a.fcalresultid = b.fid -- 核算列表\n" +
                "         inner join t_hsas_caltableentry c on c.fid = b.fid -- 核算列表-薪酬项目信息(要行转列)\n" +
                "         inner join t_hsbs_calperiod d on d.fentryid = a.fcalperiodid --期间信息\n" +
                "         inner join t_haos_adminorg e on e.fid = a.fadminorgid -- hr行政组织\n" +
                "         inner join t_hsas_calpayrolltask f on f.fid = a.fcaltaskid -- 薪资核算任务\n" +
                "         inner join t_hsbs_salaryitem g on g.fid = c.fsalaryitemid -- 薪酬项目\n" +
                sqlFilter +
                ";"
        );

        DataSet gzbDataSet = DB.queryDataSet(this.getClass().getName(), DBRoute.of("hr"), sql);

        gzbDataSet = gzbDataSet.select(new String[]{"ygbh", "ygxm", "to_char(qj,'yyyy-MM') qj",
                        "CAST(gzb1 AS DECIMAL) gzb1",
                        "CAST(gzb2 AS DECIMAL) gzb2",
                        "CAST(gzb3 AS DECIMAL) gzb3",
                        "CAST(gzb4 AS DECIMAL) gzb4",
                        "CAST(gzb5 AS DECIMAL) gzb5",
                        "CAST(gzb6 AS DECIMAL) gzb6",
                        "CAST(gzb7 AS DECIMAL) gzb7",
                        "CAST(gzb8 AS DECIMAL) gzb8",
                }
        );

        GroupbyDataSet groupbyDataSet = gzbDataSet.groupBy(new String[]{"ygbh", "ygxm", "qj"});

        for (int i = 0; i < PROJECTS.length; i++) {
            groupbyDataSet = groupbyDataSet.max("gzb" + (i + 1));
            if (i == (PROJECTS.length - 1)) {
                gzbDataSet = groupbyDataSet.finish();
            }
        }


        return gzbDataSet;
    }

    /**
     * 平账记录数据
     *
     * @return
     */
    private DataSet getPzjlDataSet() {
        QFilter[] filters = null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PROJECTS.length; i++) {
            String project = PROJECTS[i];
            sb
                    .append("(CASE WHEN entryentity.bizitem.name = '").append(project).append("个人交回' THEN entryentity.value ELSE '0' END)")
                    .append(" as " + "pzjl").append(i + 1);
            if (i != (PROJECTS.length - 1)) {
                sb.append(",");
            }
        }

        DataSet dataSet = QueryServiceHelper.queryDataSet(
                "hpdi_bizdatabillent",
                "hpdi_bizdatabillent",
                "" +
                        // 提报单ID
                        "bizdatabillid tbdid," +
                        //员工编号
                        "empposorgrel.person.number ygbh," +
                        // 员工姓名
                        "empposorgrel.person.name ygxm," +
                        // 平账记录
                        sb +
                        ""
                ,
                filters,
                null
        );

        DataSet dataSet1 = QueryServiceHelper.queryDataSet(
                "hpdi_bizdatabill",
                "hpdi_bizdatabill",
                "" +
                        // 提报单ID
                        "id tbdid," +
                        // 薪资期间
                        "calperiod.perioddate qj"
                ,
                filters,
                null
        );

        DataSet finish = dataSet1.leftJoin(dataSet).on("tbdid", "tbdid")
                .select(new String[]{"ygbh", "ygxm", "to_char(qj,'yyyy-MM') qj",
                        "CAST(pzjl1 AS DOUBLE) pzjl1",
                        "CAST(pzjl2 AS DOUBLE) pzjl2",
                        "CAST(pzjl3 AS DOUBLE) pzjl3",
                        "CAST(pzjl4 AS DOUBLE) pzjl4",
                        "CAST(pzjl5 AS DOUBLE) pzjl5",
                        "CAST(pzjl6 AS DOUBLE) pzjl6",
                        "CAST(pzjl7 AS DOUBLE) pzjl7",
                        "CAST(pzjl8 AS DOUBLE) pzjl8"
                })
                .finish().groupBy(new String[]{"ygbh", "ygxm", "qj"})
                .max("pzjl1")
                .max("pzjl2")
                .max("pzjl3")
                .max("pzjl4")
                .max("pzjl5")
                .max("pzjl6")
                .max("pzjl7")
                .max("pzjl8")
                .finish();


        return finish;
    }

    public ReportColumn createReportColumn(String fieldKey, String fieldType, String caption) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fieldKey);
        column.setFieldType(fieldType);
        column.setCaption(new LocaleString(caption));
        if (fieldType.equals(ReportColumn.TYPE_DECIMAL)) {
            column.setScale(2);
            column.setZeroShow(true);
//            column.setNoDisplayScaleZero(true);
        }
        return column;
    }

    public ReportColumnGroup createReportColumnGroup(String fieldKey, String caption) {
        ReportColumnGroup columnGroup = new ReportColumnGroup();
        columnGroup.setFieldKey(fieldKey);
        columnGroup.setHideSingleColumnRow(false);
        columnGroup.setCaption(new LocaleString(caption));
        return columnGroup;
    }
}
