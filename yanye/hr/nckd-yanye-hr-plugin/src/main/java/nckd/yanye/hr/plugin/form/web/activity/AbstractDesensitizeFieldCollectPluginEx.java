package nckd.yanye.hr.plugin.form.web.activity;

import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.DateProp;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.field.DateEdit;
import kd.hr.hpfs.formplugin.privacy.AbstractDesensitizeFieldCommonPlugin;

import java.util.EventObject;


/**
 * PC端-》信息采集协作-》基本信息 ,当选退伍军人为是，入伍和退伍时间设置必填处理
 * 信息组页面 ：nckd_hom_infogroupdyn_ext
 * 2024-09-12
 * chengchaohua
 */
public class AbstractDesensitizeFieldCollectPluginEx extends AbstractDesensitizeFieldCommonPlugin {


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        // 信息采集协作，基本信息：是否退伍军人：field2004353791935130624，入伍时间：field2004391669838916608，退伍时间：field2004391669838916609
        if(StringUtils.equals("field2004353791935130624", key)) {
            String istuiwu = (String)this.getModel().getValue("field2004353791935130624");
            if ("YES".equals(istuiwu)) {
                // 选了是退伍军人
                // 前端属性设置（前端判断必填校验）
                DateEdit property1 = (DateEdit) this.getControl("field2004391669838916608");
                property1.setMustInput(true);
                DateEdit property2 = (DateEdit) this.getControl("field2004391669838916609");
                property2.setMustInput(true);
                // 后端属性设置（后端判断必填校验）
                DateProp Prop1 = (DateProp)this.getModel().getDataEntityType().getProperty("field2004391669838916608");
                Prop1.setMustInput(true);
                DateProp Prop2 = (DateProp)this.getModel().getDataEntityType().getProperty("field2004391669838916609");
                Prop2.setMustInput(true);
                // 可编辑
                this.getView().setEnable(true, "field2004391669838916608");
                this.getView().setEnable(true, "field2004391669838916609");
            } else {
                DateEdit property1 = (DateEdit) this.getControl("field2004391669838916608");
                property1.setMustInput(false);
                DateEdit property2 = (DateEdit) this.getControl("field2004391669838916609");
                property2.setMustInput(false);

                DateProp Prop1 = (DateProp)this.getModel().getDataEntityType().getProperty("field2004391669838916608");
                Prop1.setMustInput(false);
                DateProp Prop2 = (DateProp)this.getModel().getDataEntityType().getProperty("field2004391669838916609");
                Prop2.setMustInput(false);
                // 值置空，不可编辑
                this.getModel().setValue("field2004391669838916608",null);
                this.getView().setEnable(false, "field2004391669838916608");
                this.getModel().setValue("field2004391669838916609",null);
                this.getView().setEnable(false, "field2004391669838916609");
            }
        }
    }
}
