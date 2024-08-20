package nckd.yanye.scm.plugin.form;


import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import java.util.EventObject;
/**
 * Module           :制造云-生产任务管理-物料申请单
 * Description      :物料申请单表单插件
 *
 * @author : yaosijie
 * @date : 2024/8/16
 */
public class MaterialrequestFormPlugin extends AbstractFormPlugin {

    @Override
    public void afterCreateNewData(EventObject e) {
        QFilter qFilter = new QFilter("number", QCP.equals,"1");
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bos_adminorg",new QFilter[]{qFilter});
        //创建组织默认江盐集团
        this.getModel().setValue("nckd_createorg",dynamicObject);
        this.getModel().setValue("nckd_administrativeorg", RequestContext.get().getOrgId());
        super.afterCreateNewData(e);
    }
}
