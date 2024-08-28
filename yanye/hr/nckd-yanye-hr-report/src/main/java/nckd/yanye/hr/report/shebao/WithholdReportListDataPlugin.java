package nckd.yanye.hr.report.shebao;

import kd.bos.algo.DataSet;
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
        QFilter qFilter = new QFilter("sinsurperiod.perioddate", QCP.large_equals, ((DynamicObject) sbksqj.getValue()).getDate("perioddate"));
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
            qFilter = qFilter.and(new QFilter("welfarepayer", QCP.equals, ((DynamicObject) sjcbdw.getValue()).getLong("id")));
        }
        // 理论参保单位
        if (llcbdw.getValue() != null) {
            qFilter = qFilter.and(new QFilter("sinsurfilev.welfarepayertheory", QCP.equals, ((DynamicObject) llcbdw.getValue()).getLong("id")));
        }


        // 自定义过滤
        QFilter[] filters = new QFilter[]{qFilter};

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
                        // 实际缴纳单位
                        "welfarepayer.name sjjndw," +
                        // 理论缴纳单位
                        "sinsurfilev.welfarepayertheory.name lljndw," +
                        // 养老保险个人缴费金额
                        "(CASE WHEN entryentity.insuranceitem.name = '养老保险个人缴费金额' THEN entryentity.amountvalue END) grjfje," +
                        // 养老保险单位缴费金额
                        "(CASE WHEN entryentity.insuranceitem.name = '养老保险单位缴费金额' THEN entryentity.amountvalue END) dwjnje," +
                        // 养老保险个人补缴金额
                        "(CASE WHEN entryentity.insuranceitem.name = '养老保险个人补缴金额' THEN entryentity.amountvalue END) grbjje," +
                        // 养老保险单位补缴金额
                        "(CASE WHEN entryentity.insuranceitem.name = '养老保险单位补缴金额' THEN entryentity.amountvalue END) dwbjje" +
                        ""
                ,
                filters,
                null
        );

        dataSet = dataSet.groupBy(new String[]{"ygbh", "ygxm", "to_char(sbqj,'yyyy-MM') sbqj", "sjjndw", "lljndw"})
                .max("grjfje")
                .max("dwjnje")
                .max("grbjje")
                .max("dwbjje")
                .max("grhj")
                .max("dwhj")
                .finish().orderBy(new String[]{"ygbh", "sjjndw", "lljndw", "sbqj"});

        return dataSet;
    }


    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) {
        ReportColumn ygbh = createReportColumn("ygbh", ReportColumn.TYPE_TEXT, "员工编号");
        ReportColumn ygxm = createReportColumn("ygxm", ReportColumn.TYPE_TEXT, "员工姓名");
        ReportColumn sbqj = createReportColumn("sbqj", ReportColumn.TYPE_TEXT, "社保区间");

        ReportColumn sjjndw = createReportColumn("sjjndw", ReportColumn.TYPE_TEXT, "实际缴纳单位");
        ReportColumn lljndw = createReportColumn("lljndw", ReportColumn.TYPE_TEXT, "理论缴纳单位");

        ReportColumnGroup ylbx = createReportColumnGroup("ylbx", "养老保险");
        ylbx.getChildren().add(createReportColumn("grjfje", ReportColumn.TYPE_DECIMAL, "个人缴费金额"));
        ylbx.getChildren().add(createReportColumn("dwjnje", ReportColumn.TYPE_DECIMAL, "单位缴纳金额"));
        ylbx.getChildren().add(createReportColumn("grbjje", ReportColumn.TYPE_DECIMAL, "个人补交金额"));
        ylbx.getChildren().add(createReportColumn("dwbjje", ReportColumn.TYPE_DECIMAL, "单位补交金额"));
        ylbx.getChildren().add(createReportColumn("grhj", ReportColumn.TYPE_DECIMAL, "个人合计"));
        ylbx.getChildren().add(createReportColumn("dwhj", ReportColumn.TYPE_DECIMAL, "单位合计"));


        columns.add(ygbh);
        columns.add(ygxm);
        columns.add(sbqj);
        columns.add(sjjndw);
        columns.add(lljndw);
        columns.add(ylbx);
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
}
