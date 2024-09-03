package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillShowParameter;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.EventObject;

/**
 * 供应链-生产工单
 * 表单标识：pom_mftorder
 * author：xiaoxiaopeng
 * date：2024-09-03
 */
public class MftOrderBillFromPlugin extends AbstractBillPlugIn {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        TextEdit textEdit = this.getControl("nckd_sourcebill");
        textEdit.addClickListener(this);
    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        super.beforeClick(evt);
        TextEdit source = (TextEdit) evt.getSource();
        String key = source.getKey();
        if("nckd_sourcebill".equals(key)){
            Object sourcebill = this.getModel().getValue("nckd_sourcebill");
            DynamicObject plan = BusinessDataServiceHelper.loadSingle("nckd_pom_planning", "id,billno",
                    new QFilter[]{new QFilter("billno", QCP.equals, sourcebill)});
            if (plan == null){
                return;
            }
            BillShowParameter billShowParameter = new BillShowParameter();
            billShowParameter.getOpenStyle().setShowType(ShowType.Modal);
            billShowParameter.setFormId("nckd_pom_planning");
            billShowParameter.setPkId(plan.get("id"));
            getView().showForm(billShowParameter);

        }
    }

}
