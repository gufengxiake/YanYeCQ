package nckd.yanye.hr.plugin.form.xinzijicheng;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.LoadCustomControlMetasArgs;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.AbstractFormPlugin;

import java.util.EventObject;

/**
 * Module           :薪酬福利云-薪资数据集成-业务数据提报
 * Description      :表单插件
 * 单据标识：nckd_hpdi_bizdatabill_ext
 *
 * @author ：luxiao
 * @since ：Created in 14:19 2024/9/23
 */
public class BizdatabillFormPlugin extends AbstractFormPlugin {
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        // 业务数据模板
        DynamicObject item = (DynamicObject) this.getModel().getValue("bizitemgroup");
        String itemNumber = item.getString("number");
        if ("jh001".equals(itemNumber)) {
            BizdatabillnewentryFormPlugin.lockField(this.getModel(), this.getView());
        }
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);

        FormOperate formOperate = (FormOperate) args.getSource();
        if ("donothing_newentry".equals(formOperate.getOperateKey())) {
            // 业务数据模板
            DynamicObject item = (DynamicObject) this.getModel().getValue("bizitemgroup");
            String itemNumber = item.getString("number");
            if ("JH001".equals(itemNumber)) {
                this.getView().getPageCache().put("isJH001", "true");
            } else {
                this.getView().getPageCache().put("isJH001", "false");
            }
        }
    }


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        String propertyName = e.getProperty().getName();
        ChangeData changeData = e.getChangeSet()[0];
        int changeRowIndex = changeData.getRowIndex();

        String errMsg = null;
        if ("jh004".equals(propertyName) || "jh038".equals(propertyName) || "jh037".equals(propertyName)) {
            errMsg = BizdatabillnewentryFormPlugin.autoGetProportion(model, changeRowIndex);
        }
        if ("bizdate".equals(propertyName)) {
            errMsg = BizdatabillnewentryFormPlugin.autoGetCardinal(model, changeRowIndex);
        }
        if (errMsg != null) {
            this.getView().showErrorNotification(String.format("注意：“业务数据”第%s行，%s", changeRowIndex + 1, errMsg));
        }
    }
}
