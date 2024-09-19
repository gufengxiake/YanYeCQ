package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.IFormView;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.swc.hsbp.formplugin.web.SWCDataBaseList;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
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
        String s = this.getView().getParentView().getPageCache().get("isYear");
        if (!"true".equals(s)) {
            return;
        }

        List<QFilter> qFilters = event.getQFilters();


        /*
         * 查询所有拥有上一年年度绩效考核记录的 员工
         * 以及近三年拥有绩效记录的 中层管理人员
         */
        Date effectivedate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(effectivedate);
        // 上一年
        calendar.add(Calendar.YEAR, -1);
        String lastYearDateString = new SimpleDateFormat("yyyy").format(calendar.getTime());

        // 上上年
        calendar.add(Calendar.YEAR, -1);
        String lastTwoYearsDateString = new SimpleDateFormat("yyyy").format(calendar.getTime());

        // 上上上年
        calendar.add(Calendar.YEAR, -1);
        String lastThreeYearsDateString = new SimpleDateFormat("yyyy").format(calendar.getTime());

        // 获取所有年度绩效Map <员工工号, List<考核年度, 考核结果>>
        DynamicObject[] yearKaoheArray = BusinessDataServiceHelper.load(
                "nckd_hspm_yearkaohe",
                "person,nckd_kaoheyear,nckd_kaoheresult,nckd_pingjiaorg,nckd_wcjreason",
                new QFilter[]{
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );
        Map<String, List<Map<String, String>>> yearKaoheMap = Arrays.stream(yearKaoheArray)
                .collect(Collectors.groupingBy(obj -> obj.getString("person.number"),
                        Collectors.mapping(obj -> {
                            Map<String, String> map = new HashMap<>();
                            map.put("nckd_kaoheyear", obj.getString("nckd_kaoheyear"));
                            map.put("nckd_kaoheresult.name", obj.getString("nckd_kaoheresult.name"));
                            return map;
                        }, Collectors.toList())));


        // 获取所有任职经历Map <员工id, 职级名称>
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

        // 先初始化符合条件的员工集合
        ArrayList<String> persons = new ArrayList<>();
        for (DynamicObject jobPerson : jobExpArray) {
            String personNumber = jobPerson.getString("person.number");
            String zhiJi = jobPerson.getString("nckd_zhiji.name");
            // 员工 上一年
            if ("员工".equals(zhiJi)) {
                List<Map<String, String>> maps = yearKaoheMap.get(personNumber);
                if (maps != null && !maps.isEmpty()) {
                    Map<String, String> lastYearKaoheResult = maps.get(0);
                    String lastYearKaoHeResult = lastYearKaoheResult.get(lastYearDateString);
                    if (lastYearKaoHeResult != null && !lastYearKaoHeResult.isEmpty()) {
                        persons.add(personNumber);
                    }
                }
            }

            // 中层管理人员 近三年
            if (!"其他".equals(zhiJi) && !"员工".equals(zhiJi)) {

            }

        }


        qFilters.add(new QFilter("employee.empnumber", QCP.in, persons));
    }
}
