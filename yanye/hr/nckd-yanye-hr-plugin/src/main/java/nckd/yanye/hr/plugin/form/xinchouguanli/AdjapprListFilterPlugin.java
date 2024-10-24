package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.PackageDataEvent;
import kd.bos.entity.list.column.DynamicTextColumnDesc;
import kd.bos.entity.list.column.TextColumnDesc;
import kd.bos.form.IFormView;
import kd.bos.form.container.Container;
import kd.bos.form.control.Control;
import kd.bos.form.events.BeforeCreateListColumnsArgs;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.operatecol.OperationColItem;
import kd.bos.list.*;
import kd.bos.list.column.ListOperationColumnDesc;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.swc.hsbp.formplugin.web.SWCDataBaseList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 定调薪档案-单据列表插件
 * 单据标识：nckd_hcdm_adjfileinfo_ext
 * 年度绩效调薪添加人员时，过滤员工
 *
 * @author ：luxiao
 * @since ：Created in 16:44 2024/9/19
 */
public class AdjapprListFilterPlugin extends SWCDataBaseList {


//    @Override
//    public void setFilter(SetFilterEvent event) {
//        super.setFilter(event);
//
//        // 判断页面是否为申请单
//        IFormView parentView = this.getView().getParentView();
//        if (!"hcdm_adjapprbill".equals(parentView.getEntityId())) {
//            return;
//        }
//        // 判断是否传了参数
//        String isYear = this.getView().getParentView().getPageCache().get("isYear");
//        if ("false".equals(isYear) || Objects.isNull(isYear)) {
//            return;
//        }
//
//        List<QFilter> qFilters = event.getQFilters();
//        /*
//         * 查询所有拥有上一年年度绩效考核记录的 员工
//         * 以及近三年拥有绩效记录的 中层管理人员
//         */
//        // 默认生效日期
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        Date effectivedate = null;
//        try {
//            effectivedate = sdf.parse(isYear);
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(effectivedate);
//        // 上一年
//        calendar.add(Calendar.YEAR, -1);
//        String lastYearDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());
//
//        // 上上年
//        calendar.add(Calendar.YEAR, -1);
//        String lastTwoYearsDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());
//
//        // 上上上年
//        calendar.add(Calendar.YEAR, -1);
//        String lastThreeYearsDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());
//
//        // 获取所有年度绩效Map <员工工号, List<考核年度, 考核结果>>
//        Map<String, List<Map<String, String>>> yearKaoheMap = AdjapprSelectExtPlugin.getYearKaoheMap();
//
//        // 获取所有任职经历
//        DynamicObject[] jobExpArray = BusinessDataServiceHelper.load(
//                "hrpi_empposorgrel",
//                "person,nckd_zhiji",
//                new QFilter[]{
//                        // 是否主任职：是
//                        new QFilter("isprimary", QCP.equals, "1"),
//                        // 开始日期小于今天
//                        new QFilter("startdate", QCP.less_than, new Date()),
//                        // 结束日期大于今天
//                        new QFilter("enddate", QCP.large_than, new Date()),
//                        // 业务状态：生效中
//                        new QFilter("businessstatus", QCP.equals, "1"),
//                        // 数据状态
//                        new QFilter("datastatus", QCP.equals, "1"),
//                        // 当前版本
//                        new QFilter("iscurrentversion", QCP.equals, "1"),
//                }
//        );
//
//        // 符合条件的员工集合
//        ArrayList<String> persons = new ArrayList<>();
//        for (DynamicObject jobPerson : jobExpArray) {
//            String personNumber = jobPerson.getString("person.number");
//            String zhiJi = jobPerson.getString("nckd_zhiji.name");
//            List<Map<String, String>> yearKaoheResult = yearKaoheMap.get(personNumber);
//            if (Objects.isNull(yearKaoheResult)) {
//                continue;
//            }
//            // 员工近三年的年度绩效考核成绩
//            String lastYearKaoHeResult = yearKaoheResult.stream()
//                    .map(map -> map.get(lastYearDateString))
//                    .filter(Objects::nonNull)
//                    .findFirst()
//                    .orElse(null);
//            String lastTwoYearsKaoHeResult = yearKaoheResult.stream()
//                    .map(map -> map.get(lastTwoYearsDateString))
//                    .filter(Objects::nonNull)
//                    .findFirst()
//                    .orElse(null);
//            String lastThreeYearsKaoHeResult = yearKaoheResult.stream()
//                    .map(map -> map.get(lastThreeYearsDateString))
//                    .filter(Objects::nonNull)
//                    .findFirst()
//                    .orElse(null);
//
//
//            if (Objects.isNull(lastYearKaoHeResult)) {
//                continue;
//            }
//
//            // 员工 上一年
//            if ("员工".equals(zhiJi)) {
//                persons.add(personNumber);
//            }
//
//            // 中层管理人员 优秀：一年；良好：连续两年；不称职：一年；基本称职：三年累计两次；
//            if (!"其他".equals(zhiJi) && !"员工".equals(zhiJi)) {
//                boolean flag = false;
//                switch (lastYearKaoHeResult) {
//                    case "优秀":
//                    case "不称职":
//                        flag = true;
//                        break;
//                    case "良好":
//                        if ("良好".equals(lastTwoYearsKaoHeResult)) {
//                            flag = true;
//                        }
//                        break;
//                    case "基本称职":
//                        if ("基本称职".equals(lastTwoYearsKaoHeResult) || "基本称职".equals(lastThreeYearsKaoHeResult)) {
//                            flag = true;
//                        }
//                        break;
//                    default:
//                        break;
//                }
//                if (flag) {
//                    persons.add(personNumber);
//                }
//            }
//        }
//        qFilters.add(new QFilter("employee.empnumber", QCP.in, persons));
//    }

    @Override
    public void beforeCreateListColumns(BeforeCreateListColumnsArgs args) {
        // 判断页面是否为申请单
        IFormView parentView = this.getView().getParentView();
        if (!"hcdm_adjapprbill".equals(parentView.getEntityId())) {
            return;
        }
        // 判断是否传了参数
        String isYear = this.getView().getParentView().getPageCache().get("isYear");
        if ("false".equals(isYear) || Objects.isNull(isYear)) {
            return;
        }
    }


    @Override
    public void packageData(PackageDataEvent evt) {
        // 判断页面是否为申请单
        IFormView parentView = this.getView().getParentView();
        if (!"hcdm_adjapprbill".equals(parentView.getEntityId())) {
            return;
        }

        // 判断是否传了参数
        String isYear = this.getView().getParentView().getPageCache().get("isYear");
        if ("false".equals(isYear) || Objects.isNull(isYear)) {
            return;
        }

        this.batUpdateListFieldVal(evt);
        super.packageData(evt);
    }


    @Override
    public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
        // 判断页面是否为申请单
        IFormView parentView = this.getView().getParentView();
        // 判断是否传了参数
        String isYear = this.getView().getParentView().getPageCache().get("isYear");
        if (!"hcdm_adjapprbill".equals(parentView.getEntityId())
                ||
                ("false".equals(isYear) || Objects.isNull(isYear))) {
            BillList billList = this.getControl(AbstractListPlugin.BILLLISTID);
            billList.getView().setVisible(false,
                    "nckd_lastyear",
                    "nckd_lasttwoyears",
                    "nckd_lastthreeyears",
                    "nckd_zhiji"
            );
        }
    }


    /**
     * 修改数据包中的数据, 使前端展示与数据表中的数据不相同
     *
     * @param evt
     */
    private void batUpdateListFieldVal(PackageDataEvent evt) {
        String colKey = evt.getColKey();
        if ("nckd_zhiji".equals(colKey)) {
            Map<String, String> jobExpMap = AdjapprSelectExtPlugin.getJobExpMap();
            evt.setFormatValue(jobExpMap.get(evt.getRowData().getString("employee.empnumber")));
        }

        if ("nckd_lastyear".equals(colKey) || "nckd_lasttwoyears".equals(colKey) || "nckd_lastthreeyears".equals(colKey)) {
            /*
             * 查询所有拥有上一年年度绩效考核记录的 员工
             * 以及近三年拥有绩效记录的 中层管理人员
             */
            // 默认生效日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date effectivedate = null;
            try {
                effectivedate = sdf.parse(this.getView().getParentView().getPageCache().get("isYear"));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(effectivedate);
            // 上一年
            calendar.add(Calendar.YEAR, -1);
            String lastYearDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());

            // 上上年
            calendar.add(Calendar.YEAR, -1);
            String lastTwoYearsDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());

            // 上上上年
            calendar.add(Calendar.YEAR, -1);
            String lastThreeYearsDateString = new SimpleDateFormat("yyyy年").format(calendar.getTime());

            // 获取所有年度绩效Map <员工工号, List<考核年度, 考核结果>>
            Map<String, List<Map<String, String>>> yearKaoheMap = AdjapprSelectExtPlugin.getYearKaoheMap();
            List<Map<String, String>> yearKaoheResult = yearKaoheMap.get(evt.getRowData().getString("employee.empnumber"));
            // 员工近三年的年度绩效考核成绩
            String lastYearKaoHeResult = yearKaoheResult.stream()
                    .map(map -> map.get(lastYearDateString))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            String lastTwoYearsKaoHeResult = yearKaoheResult.stream()
                    .map(map -> map.get(lastTwoYearsDateString))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            String lastThreeYearsKaoHeResult = yearKaoheResult.stream()
                    .map(map -> map.get(lastThreeYearsDateString))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);


            if ("nckd_lastyear".equals(colKey)) {
                evt.setFormatValue(lastYearKaoHeResult);
            }
            if ("nckd_lasttwoyears".equals(colKey)) {
                evt.setFormatValue(lastTwoYearsKaoHeResult);
            }
            if ("nckd_lastthreeyears".equals(colKey)) {
                evt.setFormatValue(lastThreeYearsKaoHeResult);
            }
        }
    }
}
