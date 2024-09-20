package nckd.yanye.hr.plugin.form;

import com.kingdee.util.StringUtils;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.EventObject;

/**
 * Module           :薪酬福利云-薪资核算-薪酬发放
 * Description      :薪资审批单查看给备注set数据
 * nckd_hsas_approvebill_ext
 * @author : yaosijie
 * @date : 2024/9/20
 */
public class HsasCalpayFormPlugin extends AbstractFormPlugin {


    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        Object pkValue = this.getModel().getValue("id");
        DynamicObject hsasApprovebillObject = BusinessDataServiceHelper.loadSingle(pkValue, "hsas_approvebill");
        if (StringUtils.isEmpty(hsasApprovebillObject.getString("description"))){
            HsasCalpayrolltEventServicePlugin hsasCalpayrolltEventServicePlugin = new HsasCalpayrolltEventServicePlugin();
            hsasCalpayrolltEventServicePlugin.getDynamicObject(hsasApprovebillObject);
            this.getModel().setValue("description",hsasApprovebillObject.get("description"));
        }
    }

}
