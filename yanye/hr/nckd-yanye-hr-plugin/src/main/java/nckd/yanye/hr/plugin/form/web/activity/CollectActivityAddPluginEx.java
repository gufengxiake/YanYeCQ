package nckd.yanye.hr.plugin.form.web.activity;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.FieldProp;
import kd.bos.form.control.events.UploadListener;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.FieldEdit;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.hr.hom.formplugin.web.activity.AbstractCollectDynViewPlugin;

public class CollectActivityAddPluginEx  extends AbstractCollectDynViewPlugin implements UploadListener {
    private static final Log logger = LogFactory.getLog(CollectActivityAddPluginEx.class);


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        if(StringUtils.equals("field1249297648691749891", key)) {
            // 学历：field1249297648691749891
            IDataModel model = this.getModel();
            DynamicObject  xueli = (DynamicObject )model.getValue("field1249297648691749891");
            // 1050_S:高中
            int result = "1050_S".compareTo(xueli.getString("number"));
            if (result <= 0) {
                // number比1050_S大的，比高中学历还要低
                // 1)毕业院系名称:前端属性设置（前端判断必填校验）
                FieldEdit property1 = (FieldEdit) this.getControl("field2004400643116121089");
                property1.setMustInput(false);
                // 后端属性设置（后端判断必填校验）
                FieldProp Prop1 = (FieldProp)this.getModel().getDataEntityType().getProperty("field2004400643116121089");
                Prop1.setMustInput(false);
                // 2)学位证附件: field12492976486917498940
               /* // 前端属性设置（前端判断必填校验）
                FieldEdit property2 = (FieldEdit) this.getControl("field12492976486917498940");
                property2.setMustInput(false);
                // 后端属性设置（后端判断必填校验）
                FieldProp Prop2 = (FieldProp)this.getModel().getDataEntityType().getProperty("field12492976486917498940");
                Prop2.setMustInput(false);*/
            } else {
                // 前端属性设置（前端判断必填校验）
                FieldEdit property1 = (FieldEdit) this.getControl("field2004400643116121089");
                property1.setMustInput(true);
                // 后端属性设置（后端判断必填校验）
                FieldProp Prop1 = (FieldProp)this.getModel().getDataEntityType().getProperty("field2004400643116121089");
                Prop1.setMustInput(true);
            }


        }

    }
}
