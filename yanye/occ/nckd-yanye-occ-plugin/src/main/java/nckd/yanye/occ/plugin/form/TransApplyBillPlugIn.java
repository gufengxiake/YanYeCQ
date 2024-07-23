package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;


/*
调拨申请单表单插件
 */
public class TransApplyBillPlugIn extends AbstractBillPlugIn implements BeforeF7SelectListener {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit inWareHouseEdit = this.getView().getControl("inwarehouse");
        inWareHouseEdit.addBeforeF7SelectListener(this);
        BasedataEdit wareHoseEdit=this.getView().getControl("warehouse");
        wareHoseEdit.addBeforeF7SelectListener(this);
    }
    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name= evt.getProperty().getName();
        if(name.equalsIgnoreCase("inwarehouse")){
            DynamicObject billtype= (DynamicObject) this.getModel().getValue("billtype",0);
            String nameq=billtype.getString("name");
            Object id=billtype.getPkValue();
            if(id.equals("1994937113375673344")||nameq.equalsIgnoreCase("借货申请")){
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            }

        }else if(name.equalsIgnoreCase("warehouse")){
            DynamicObject billtype= (DynamicObject) this.getModel().getValue("billtype",0);
            String nameq=billtype.getString("name");
            Object id=billtype.getPkValue();
            if(id.equals("1994937462568258560")||nameq.equalsIgnoreCase("借货归还申请")){
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            }
        }




    }
}
