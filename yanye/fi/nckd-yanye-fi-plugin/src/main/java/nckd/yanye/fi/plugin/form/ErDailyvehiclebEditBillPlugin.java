package nckd.yanye.fi.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;

import java.util.EventObject;

/**
 * Module           :员工服务云-员工费用-用车申请
 * Description      :车辆信息选择车辆类别的资产卡片。只能选择单据上费用承担公司的卡片
 *
 * @author : zhujintao
 * @date : 2024/10/11
 */
public class ErDailyvehiclebEditBillPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 侦听基础资料字段的事件
        BasedataEdit fieldEdit = this.getView().getControl("nckd_carinfo");
        fieldEdit.addBeforeF7SelectListener(this);
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        DynamicObject costcompany = (DynamicObject) this.getModel().getValue("costcompany");
        beforeF7SelectEvent.getCustomQFilters().add(new QFilter("org", QCP.equals, costcompany.getPkValue()));
    }
}