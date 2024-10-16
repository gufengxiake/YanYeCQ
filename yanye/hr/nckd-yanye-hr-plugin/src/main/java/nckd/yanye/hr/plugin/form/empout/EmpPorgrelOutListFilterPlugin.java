package nckd.yanye.hr.plugin.form.empout;

import kd.bos.form.events.SetFilterEvent;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.formplugin.web.HRDataBaseList;

import java.util.List;

/**
 * 上线前任职经历-过滤
 * 标识：nckd_hspm_emporgreloulist
 *
 * @author ：luxiao
 * @since ：Created in 11:47 2024/10/16
 */
public class EmpPorgrelOutListFilterPlugin extends HRDataBaseList {
    @Override
    public void setFilter(SetFilterEvent e) {
        super.setFilter(e);

        List<QFilter> qFilters = e.getQFilters();
        // 是否系统内任职：否
        qFilters.add(new QFilter("isinsystem", QCP.equals, "0"));
    }
}
