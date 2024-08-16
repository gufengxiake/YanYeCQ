package nckd.yanye.scm.plugin.form;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.EventObject;

public class MaterialrequestFormPlugin extends AbstractFormPlugin {

    @Override
    public void afterCreateNewData(EventObject e) {
        QFilter qFilter = new QFilter("number", QCP.equals,"1");
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bos_adminorg",new QFilter[]{qFilter});
        //组织默认江盐集团
        this.getModel().setValue("org",dynamicObject);
        super.afterCreateNewData(e);
    }
}
