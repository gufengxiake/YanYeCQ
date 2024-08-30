package nckd.yanye.hr.plugin.form.kaoqin;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.query.QFilter;
import java.util.*;

/**
 * Module           :工时假勤云-加班管理-为他人申请加班,为他人申请休假,为他人申请加班,为他人申请补签去除f7过滤本人
 * Description      :通用插件，为其他人申请加班、休假、加班、补签时，不过滤本人。
 *
 * @author guozhiwei
 * @date  2024-08-30 9：15
 *
 */

public class ApplyBussinessPlugin  extends AbstractBillPlugIn implements BeforeF7SelectListener {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        // 侦听基础资料字段的事件
        BasedataEdit fieldEdit = this.getView().getControl("attfilebasef7");
        fieldEdit.addBeforeF7SelectListener(this);

    }



    @Override
    public void beforeF7Select(BeforeF7SelectEvent arg0) {
        List<QFilter> qFilters = new ArrayList<>();
        QFilter qFilter1 = null;
        qFilters.add(qFilter1);
        arg0.setCustomQFilters(qFilters);

    }

}
