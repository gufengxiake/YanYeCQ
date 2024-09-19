package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.MainEntityType;
import kd.bos.form.IFormView;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.swc.hsbp.formplugin.web.SWCDataBaseList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        // 查询所有拥有上一年年度绩效考核记录的 员工
        // 以及近三年拥有绩效记录的 中层管理人员
        // 获取所有年度绩效Map <员工id, <考核年度, 考核结果>>
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
        Map<Long, Map<String, String>> yearKaoheMap = Arrays.stream(yearKaoheArray)
                .collect(Collectors.groupingBy(
                        obj -> obj.getLong("person.id"),
                        Collectors.toMap(
                                obj -> obj.getString("nckd_kaoheyear"),
                                obj -> obj.getString("nckd_kaoheresult.name") == null ? "" : obj.getString("nckd_kaoheresult.name")
                        )
                ));


        qFilters.add(new QFilter("employee.empnumber", QCP.in, new String[]{"003532"}));
    }
}
