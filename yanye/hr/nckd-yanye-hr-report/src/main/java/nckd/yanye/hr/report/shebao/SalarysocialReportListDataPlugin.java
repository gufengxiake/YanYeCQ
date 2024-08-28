package nckd.yanye.hr.report.shebao;

import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.db.SqlBuilder;
import kd.bos.entity.report.*;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.List;
import java.util.Map;

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
    private final DynamicObject[] load = BusinessDataServiceHelper.load(
            "sitbs_welfaretype",
            "id,number,name,",
            null
    );


    @Override
    public DataSet query(ReportQueryParam queryParam, Object o) {
        Map<String, Object> customParam = queryParam.getCustomParam();
        FilterInfo filter = queryParam.getFilter();
        List<FilterItemInfo> filterItems = filter.getFilterItems();


        // 自定义过滤
        QFilter[] filters = new QFilter[]{};

        // 社保表数据
        DataSet sbbDataSet = getSbbDataSet(filters);


        // 工资表数据
        DataSet gzbDataSet = getGzbDataSet();




        return sbbDataSet;
    }


    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        ReportColumn ygbh = createReportColumn("ygbh", ReportColumn.TYPE_TEXT, "员工编码");
        ReportColumn ygxm = createReportColumn("ygxm", ReportColumn.TYPE_TEXT, "员工姓名");
        ReportColumn sbqj = createReportColumn("qj", ReportColumn.TYPE_TEXT, "社保区间");


        columns.add(ygbh);
        columns.add(ygxm);
        columns.add(sbqj);

        // 险种
        for (int i = 0; i < load.length; i++) {
            DynamicObject obj = load[i];
            ReportColumnGroup welfaretype = createReportColumnGroup(obj.getString("number"), obj.getString("name"));
            welfaretype.getChildren().add(createReportColumn("sbb" + (i + 1), ReportColumn.TYPE_DECIMAL, "社保表"));
            welfaretype.getChildren().add(createReportColumn("gzb" + (i + 1), ReportColumn.TYPE_DECIMAL, "工资表"));
            welfaretype.getChildren().add(createReportColumn("pzjl" + (i + 1), ReportColumn.TYPE_DECIMAL, "平账记录"));
            welfaretype.getChildren().add(createReportColumn("ce" + (i + 1), ReportColumn.TYPE_DECIMAL, "差额"));
            columns.add(welfaretype);
        }


//        columns.add(createReportColumn("test", ReportColumn.TYPE_TEXT, "test"));
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

    /**
     * 社保表数据
     *
     * @param filters
     * @return
     */
    private DataSet getSbbDataSet(QFilter[] filters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < load.length; i++) {
            DynamicObject obj = load[i];
            String name = obj.getString("name");
            sb
                    .append("(CASE WHEN entryentity.insuranceitem.name = '").append(name).append("个人固定金额' THEN entryentity.amountvalue ELSE 0 END)")
                    .append("+(CASE WHEN entryentity.insuranceitem.name = '").append(name).append("个人缴费金额' THEN entryentity.amountvalue ELSE 0 END)")
                    .append("+(CASE WHEN entryentity.insuranceitem.name = '").append(name).append("个人补缴金额' THEN entryentity.amountvalue ELSE 0 END)")
                    .append(" as " + "sbb").append(i + 1);
            if (i != (load.length - 1)) {
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
                filters,
                null
        );

        GroupbyDataSet groupbyDataSet = sbbDataSet.groupBy(new String[]{"ygbh", "ygxm", "to_char(qj,'yyyy-MM') qj"});

        for (int i = 0; i < load.length; i++) {
            groupbyDataSet = groupbyDataSet.max("sbb" + (i + 1));
            if (i == (load.length - 1)) {
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
    private DataSet getGzbDataSet() {
        SqlBuilder sql = new SqlBuilder();
        sql.append("select a.fid\n" +
                // 员工工号
                "     , a.fempnumber              ygbh\n" +
                // 员工姓名
                "     , a.fname                   ygxm\n" +
                // 期间
                "     , d.fperioddate             期间\n" +
                // 公司编码
                "     , e.fnumber                 公司编码\n" +
                // 公司名称
                "     , e.fname                   公司名称\n" +
                // 薪资核算任务名称
                "     , f.fname                   薪资核算任务名称\n" +
                // 项目名称
                "     , g.fname                   项目名称\n" +
                // 项目数据
                "     , case\n" +
                "           when c.ftextvalue = ' ' then (case\n" +
                "                                             when c.fcalamountvalue = 0 then c.fnumvalue || ''\n" +
                "                                             else c.fcalamountvalue || '' end)\n" +
                "           else c.ftextvalue end 项目数据\n" +
                "from t_hsas_calperson a -- 核算名单\n" +
                "         inner join t_hsas_caltable b on a.fcalresultid = b.fid -- 核算列表\n" +
                "         inner join t_hsas_caltableentry c on c.fid = b.fid -- 核算列表-薪酬项目信息(要行转列)\n" +
                "         inner join t_hsbs_calperiod d on d.fentryid = a.fcalperiodid --期间信息\n" +
                "         inner join t_haos_adminorg e on e.fid = a.fadminorgid -- hr行政组织\n" +
                "         inner join t_hsas_calpayrolltask f on f.fid = a.fcaltaskid -- 薪资核算任务\n" +
                "         inner join t_hsbs_salaryitem g on g.fid = c.fsalaryitemid -- 薪酬项目\n" +
                ";");
        DataSet gzbDataSet = DB.queryDataSet(this.getClass().getName(), DBRoute.of("hr"), sql);

        return gzbDataSet;
    }
}
