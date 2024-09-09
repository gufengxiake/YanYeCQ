package nckd.yanye.hr.report.shebao;

import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代扣代缴情况表-报表取数插件
 * 报表标识：nckd_withholdreport
 *
 * @author liuxiao
 * @since 2024-08-22
 */
public class WithholdReportListDataPlugin extends AbstractReportListDataPlugin {
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
            "ygbh", "ygxm", "sbqj", "sjjndw", "lljndw",
            "grjfje1", "dwjfje1", "grbjje1", "dwbjje1", "grhj1", "dwhj1",
            "grjfje2", "dwjfje2", "grbjje2", "dwbjje2", "grhj2", "dwhj2",
            "grjfje3", "dwjfje3", "grbjje3", "dwbjje3", "grhj3", "dwhj3",
            "grjfje4", "dwjfje4", "grbjje4", "dwbjje4", "grhj4", "dwhj4",
            "grjfje5", "dwjfje5", "grbjje5", "dwbjje5", "grhj5", "dwhj5",
            "grjfje6", "dwjfje6", "grbjje6", "dwbjje6", "grhj6", "dwhj6",
            "grjfje7", "dwjfje7", "grbjje7", "dwbjje7", "grhj7", "dwhj7",
            "grjfje8", "dwjfje8", "grbjje8", "dwbjje8", "grhj8", "dwhj8"
    };

    private final String[] amountFields = {
            "grjfje1", "dwjfje1", "grbjje1", "dwbjje1", "grhj1", "dwhj1",
            "grjfje2", "dwjfje2", "grbjje2", "dwbjje2", "grhj2", "dwhj2",
            "grjfje3", "dwjfje3", "grbjje3", "dwbjje3", "grhj3", "dwhj3",
            "grjfje4", "dwjfje4", "grbjje4", "dwbjje4", "grhj4", "dwhj4",
            "grjfje5", "dwjfje5", "grbjje5", "dwbjje5", "grhj5", "dwhj5",
            "grjfje6", "dwjfje6", "grbjje6", "dwbjje6", "grhj6", "dwhj6",
            "grjfje7", "dwjfje7", "grbjje7", "dwbjje7", "grhj7", "dwhj7",
            "grjfje8", "dwjfje8", "grbjje8", "dwbjje8", "grhj8", "dwhj8",
    };


    @Override
    public DataSet query(ReportQueryParam queryParam, Object o) {
        Map<String, Object> customParam = queryParam.getCustomParam();
        FilterInfo filter = queryParam.getFilter();
        List<FilterItemInfo> filterItems = filter.getFilterItems();

        // 选择人员
        FilterItemInfo users = filter.getFilterItem("nckd_users");
        // 社保开始期间
        FilterItemInfo sbksqj = filter.getFilterItem("nckd_sbksqj");
        // 社保结束期间
        FilterItemInfo sbjsqj = filter.getFilterItem("nckd_sbjsqj");
        // 实际参保单位
        FilterItemInfo sjcbdw = filter.getFilterItem("nckd_sjcbdw");
        // 理论参保单位
        FilterItemInfo llcbdw = filter.getFilterItem("nckd_llcbdw");

        if (sbksqj.getValue() == null) {
            throw new KDBizException("社保开始期间不能为空");
        }

        // 实际参保单位或理论参保单位至少录入一项
        if (sjcbdw.getValue() == null && llcbdw.getValue() == null) {
            throw new KDBizException("实际参保单位或理论参保单位至少录入一项");
        }

        // 过滤
        // 社保开始期间
        QFilter qFilter = new QFilter("sinsurperiod.perioddate",
                QCP.large_equals,
                ((DynamicObject) sbksqj.getValue()).getDate("perioddate")
        );
        // 人员
        if (users.getValue() != null) {
            qFilter = qFilter.and(
                    new QFilter("employee.person", QCP.in, ((DynamicObjectCollection) users.getValue())
                            .stream()
                            .map(obj -> obj.getLong("id"))
                            .collect(Collectors.toList()))
            );
        }
        // 社保结束期间
        if (sbjsqj.getValue() != null) {
            qFilter = qFilter.and(new QFilter("sinsurperiod.perioddate", QCP.less_equals, ((DynamicObject) sbjsqj.getValue()).getDate("perioddate")));
        }
        // 实际参保单位
        if (sjcbdw.getValue() != null) {
            qFilter = qFilter.and(new QFilter("welfarepayer", QCP.in, ((DynamicObjectCollection) sjcbdw.getValue()).stream().map(obj -> obj.getLong("id")).collect(Collectors.toList())));
        }
        // 理论参保单位
        if (llcbdw.getValue() != null) {
            qFilter = qFilter.and(new QFilter("sinsurfilev.welfarepayertheory", QCP.in, ((DynamicObjectCollection) llcbdw.getValue()).stream().map(obj -> obj.getLong("id")).collect(Collectors.toList())));
        }

        // 自定义过滤
        QFilter[] filters = new QFilter[]{qFilter};

        // 查询语句
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PROJECTS.length; i++) {
            String project = PROJECTS[i];
            // 个人缴费金额
            sb.append("(CASE WHEN entryentity.insuranceitem.name = '").append(project).append("个人缴费金额' THEN entryentity.amountvalue END) ").append("grjfje").append(i + 1);
            sb.append(",");
            // 单位缴费金额
            sb.append("(CASE WHEN entryentity.insuranceitem.name = '").append(project).append("单位缴费金额' THEN entryentity.amountvalue END) ").append("dwjfje").append(i + 1);
            sb.append(",");
            // 个人补缴金额
            sb.append("(CASE WHEN entryentity.insuranceitem.name = '").append(project).append("个人补缴金额' THEN entryentity.amountvalue END) ").append("grbjje").append(i + 1);
            sb.append(",");
            // 单位补缴金额
            sb.append("(CASE WHEN entryentity.insuranceitem.name = '").append(project).append("单位补缴金额' THEN entryentity.amountvalue END) ").append("dwbjje").append(i + 1);
            if (i != (PROJECTS.length - 1)) {
                sb.append(",");
            }
        }

        DataSet dataSet = QueryServiceHelper.queryDataSet(
                "hcsi_calperson",
                "hcsi_calperson",
                "" +
                        //员工编号
                        "empnumberdb ygbh," +
                        // 员工姓名
                        "namedb ygxm," +
                        // 社保区间
                        "sinsurperiod.perioddate sbqj," +
                        // 理论缴纳单位
                        "sinsurfilev.welfarepayertheory.name lljndw," +
                        // 实际缴纳单位
                        "welfarepayer.name sjjndw," +
                        sb +
                        ""
                ,
                filters,
                null
        );
        dataSet = dataSet.select(new String[]{
                "ygbh", "ygxm", "sbqj", "lljndw", "sjjndw",
                "grjfje1", "dwjfje1", "grbjje1", "dwbjje1", "grjfje1+grbjje1 grhj1", "dwjfje1+dwbjje1 dwhj1",
                "grjfje2", "dwjfje2", "grbjje2", "dwbjje2", "grjfje2+grbjje2 grhj2", "dwjfje2+dwbjje2 dwhj2",
                "grjfje3", "dwjfje3", "grbjje3", "dwbjje3", "grjfje3+grbjje3 grhj3", "dwjfje3+dwbjje3 dwhj3",
                "grjfje4", "dwjfje4", "grbjje4", "dwbjje4", "grjfje4+grbjje4 grhj4", "dwjfje4+dwbjje4 dwhj4",
                "grjfje5", "dwjfje5", "grbjje5", "dwbjje5", "grjfje5+grbjje5 grhj5", "dwjfje5+dwbjje5 dwhj5",
                "grjfje6", "dwjfje6", "grbjje6", "dwbjje6", "grjfje6+grbjje6 grhj6", "dwjfje6+dwbjje6 dwhj6",
                "grjfje7", "dwjfje7", "grbjje7", "dwbjje7", "grjfje7+grbjje7 grhj7", "dwjfje7+dwbjje7 dwhj7",
                "grjfje8", "dwjfje8", "grbjje8", "dwbjje8", "grjfje8+grbjje8 grhj8", "dwjfje8+dwbjje8 dwhj8"
        });

        GroupbyDataSet groupbyDataSet = dataSet.groupBy(new String[]{"ygbh", "ygxm", "to_char(sbqj,'yyyy-MM') sbqj", "lljndw", "sjjndw",});
        for (int i = 0; i < PROJECTS.length; i++) {
            groupbyDataSet = groupbyDataSet
                    .max("grjfje" + (i + 1))
                    .max("dwjfje" + (i + 1))
                    .max("grbjje" + (i + 1))
                    .max("dwbjje" + (i + 1))
                    .max("grhj" + (i + 1))
                    .max("dwhj" + (i + 1));
        }
        dataSet = groupbyDataSet.finish().orderBy(new String[]{"ygbh", "lljndw", "sjjndw", "sbqj"});

        // 添加合计行
        DataSet finish = addSumRow(dataSet);
        return finish;
    }

    /**
     * 添加合计行
     *
     * @param dataSet
     * @return
     */
    private DataSet addSumRow(DataSet dataSet) {
        DataSet copy = dataSet.copy();
        GroupbyDataSet groupbyDataSet = copy.groupBy(new String[]{"lljndw"});
        for (int i = 0; i < PROJECTS.length; i++) {
            groupbyDataSet = groupbyDataSet
                    .sum("grjfje" + (i + 1))
                    .sum("dwjfje" + (i + 1))
                    .sum("grbjje" + (i + 1))
                    .sum("dwbjje" + (i + 1))
                    .sum("grhj" + (i + 1))
                    .sum("dwhj" + (i + 1));
        }
        copy = groupbyDataSet.finish().addNullField("ygbh", "ygxm", "sbqj", "sjjndw")
                .select(new String[]{
                                "ygbh", "ygxm", "sbqj", "null lljndw", "'理论缴纳单位合计:'+lljndw sjjndw",
                                "grjfje1", "dwjfje1", "grbjje1", "dwbjje1", "grhj1", "dwhj1",
                                "grjfje2", "dwjfje2", "grbjje2", "dwbjje2", "grhj2", "dwhj2",
                                "grjfje3", "dwjfje3", "grbjje3", "dwbjje3", "grhj3", "dwhj3",
                                "grjfje4", "dwjfje4", "grbjje4", "dwbjje4", "grhj4", "dwhj4",
                                "grjfje5", "dwjfje5", "grbjje5", "dwbjje5", "grhj5", "dwhj5",
                                "grjfje6", "dwjfje6", "grbjje6", "dwbjje6", "grhj6", "dwhj6",
                                "grjfje7", "dwjfje7", "grbjje7", "dwbjje7", "grhj7", "dwhj7",
                                "grjfje8", "dwjfje8", "grbjje8", "dwbjje8", "grhj8", "dwhj8"
                        }

                ).orderBy(new String[]{"lljndw"});


        DataSet copy2 = dataSet.copy();
        GroupbyDataSet groupbyDataSet2 = copy2.groupBy(new String[]{"sjjndw"});
        for (int i = 0; i < PROJECTS.length; i++) {
            groupbyDataSet2 = groupbyDataSet2
                    .sum("grjfje" + (i + 1))
                    .sum("dwjfje" + (i + 1))
                    .sum("grbjje" + (i + 1))
                    .sum("dwbjje" + (i + 1))
                    .sum("grhj" + (i + 1))
                    .sum("dwhj" + (i + 1));
        }
        copy2 = groupbyDataSet2.finish().addNullField("ygbh", "ygxm", "sbqj", "lljndw")
                .select(new String[]{
                                "ygbh", "ygxm", "sbqj", "lljndw", "'实际缴纳单位合计:'+sjjndw sjjndw",
                                "grjfje1", "dwjfje1", "grbjje1", "dwbjje1", "grhj1", "dwhj1",
                                "grjfje2", "dwjfje2", "grbjje2", "dwbjje2", "grhj2", "dwhj2",
                                "grjfje3", "dwjfje3", "grbjje3", "dwbjje3", "grhj3", "dwhj3",
                                "grjfje4", "dwjfje4", "grbjje4", "dwbjje4", "grhj4", "dwhj4",
                                "grjfje5", "dwjfje5", "grbjje5", "dwbjje5", "grhj5", "dwhj5",
                                "grjfje6", "dwjfje6", "grbjje6", "dwbjje6", "grhj6", "dwhj6",
                                "grjfje7", "dwjfje7", "grbjje7", "dwbjje7", "grhj7", "dwhj7",
                                "grjfje8", "dwjfje8", "grbjje8", "dwbjje8", "grhj8", "dwhj8"
                        }
                ).orderBy(new String[]{"sjjndw"});

        DataSet union = dataSet.union(copy).union(copy2);
        return union;
    }


    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        ReportColumn ygbh = createReportColumn("ygbh", ReportColumn.TYPE_TEXT, "员工编号");
        ReportColumn ygxm = createReportColumn("ygxm", ReportColumn.TYPE_TEXT, "员工姓名");
        ReportColumn sbqj = createReportColumn("sbqj", ReportColumn.TYPE_TEXT, "社保区间");

        ReportColumn lljndw = createReportColumn("lljndw", ReportColumn.TYPE_TEXT, "理论缴纳单位");
        ReportColumn sjjndw = createReportColumn("sjjndw", ReportColumn.TYPE_TEXT, "实际缴纳单位");
        columns.add(ygbh);
        columns.add(ygxm);
        columns.add(sbqj);
        columns.add(lljndw);
        columns.add(sjjndw);

        // 险种
        for (int i = 0; i < PROJECTS.length; i++) {
            String project = PROJECTS[i];
            ReportColumnGroup welfaretype = createReportColumnGroup(String.valueOf(i), project);
            welfaretype.getChildren().add(createReportColumn("grjfje" + (i + 1), ReportColumn.TYPE_DECIMAL, "个人缴费金额"));
            welfaretype.getChildren().add(createReportColumn("dwjfje" + (i + 1), ReportColumn.TYPE_DECIMAL, "单位缴费金额"));
            welfaretype.getChildren().add(createReportColumn("grbjje" + (i + 1), ReportColumn.TYPE_DECIMAL, "个人补缴金额"));
            welfaretype.getChildren().add(createReportColumn("dwbjje" + (i + 1), ReportColumn.TYPE_DECIMAL, "单位补缴金额"));
            welfaretype.getChildren().add(createReportColumn("grhj" + (i + 1), ReportColumn.TYPE_DECIMAL, "个人合计"));
            welfaretype.getChildren().add(createReportColumn("dwhj" + (i + 1), ReportColumn.TYPE_DECIMAL, "单位合计"));
            columns.add(welfaretype);
        }

        return columns;
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
