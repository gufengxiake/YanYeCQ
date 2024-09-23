package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.IFormView;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.swc.hsbp.formplugin.web.SWCDataBaseList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定调薪档案-单据列表插件
 * 单据标识：nckd_hcdm_adjfileinfo_ext
 * 年度绩效调薪添加人员时，过滤员工
 *
 * @author ：luxiao
 * @since ：Created in 16:44 2024/9/19
 */
public class AdjapprListFilterPlugin extends SWCDataBaseList {
    @Override
    public void setFilter(SetFilterEvent event) {
        super.setFilter(event);

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

        List<QFilter> qFilters = event.getQFilters();
        /*
         * 查询所有拥有上一年年度绩效考核记录的 员工
         * 以及近三年拥有绩效记录的 中层管理人员
         */
        // 默认生效日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date effectivedate = null;
        try {
            effectivedate = sdf.parse(isYear);
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
        Map<String, List<Map<String, String>>> yearKaoheMap = getYearKaoheMap();

        // 获取所有任职经历
        DynamicObject[] jobExpArray = BusinessDataServiceHelper.load(
                "hrpi_empposorgrel",
                "person,nckd_zhiji",
                new QFilter[]{
                        // 是否主任职：是
                        new QFilter("isprimary", QCP.equals, "1"),
                        // 开始日期小于今天
                        new QFilter("startdate", QCP.less_than, new Date()),
                        // 结束日期大于今天
                        new QFilter("enddate", QCP.large_than, new Date()),
                        // 业务状态：生效中
                        new QFilter("businessstatus", QCP.equals, "1"),
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );

        // 符合条件的员工集合
        ArrayList<String> persons = new ArrayList<>();
        for (DynamicObject jobPerson : jobExpArray) {
            String personNumber = jobPerson.getString("person.number");
            String zhiJi = jobPerson.getString("nckd_zhiji.name");
            List<Map<String, String>> yearKaoheResult = yearKaoheMap.get(personNumber);
            if (Objects.isNull(yearKaoheResult)) {
                continue;
            }
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


            if (Objects.isNull(lastYearKaoHeResult)) {
                continue;
            }

            // 员工 上一年
            if ("员工".equals(zhiJi)) {
                persons.add(personNumber);
            }

            // 中层管理人员 优秀：一年；良好：连续两年；不称职：一年；基本称职：三年累计两次；
            if (!"其他".equals(zhiJi) && !"员工".equals(zhiJi)) {
                boolean flag = false;
                switch (lastYearKaoHeResult) {
                    case "优秀":
                    case "不称职":
                        flag = true;
                        break;
                    case "良好":
                        if ("良好".equals(lastTwoYearsKaoHeResult)) {
                            flag = true;
                        }
                        break;
                    case "基本称职":
                        if ("基本称职".equals(lastTwoYearsKaoHeResult) || "基本称职".equals(lastThreeYearsKaoHeResult)) {
                            flag = true;
                        }
                        break;
                    default:
                        break;
                }
                if (flag) {
                    persons.add(personNumber);
                }
            }
        }
        qFilters.add(new QFilter("employee.empnumber", QCP.in, persons));
    }

    private Map<String, List<Map<String, String>>> getYearKaoheMap() {
        // 绩效结果
        DynamicObject[] jobExpArray = BusinessDataServiceHelper.load(
                "epa_performanceresult",
                "id,name,number,person,activity,assessleveltext",
                new QFilter[]{
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                        // 非历史绩效
                        new QFilter("number", QCP.not_like, "%HI%"),
                }
        );
        Map<String, List<Map<String, String>>> yearKaoheMap = Arrays.stream(jobExpArray)
                .collect(Collectors.groupingBy(
                        job -> job.getString("person.number"),
                        Collectors.mapping(
                                job -> {
                                    Map<String, String> yearResultMap = new HashMap<>();
                                    yearResultMap.put(
                                            job.getString("activity.periodname"),
                                            job.getString("assessleveltext")
                                    );
                                    return yearResultMap;
                                },
                                Collectors.toList()
                        )
                ));
        return yearKaoheMap;
    }


}
