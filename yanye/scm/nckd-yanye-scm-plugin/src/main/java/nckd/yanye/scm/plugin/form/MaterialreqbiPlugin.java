package nckd.yanye.scm.plugin.form;

import java.util.Map;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.FlexEdit;
import kd.bos.servicehelper.BusinessDataServiceHelper;

/**
 * @author husheng
 * @date 2024-07-13 18:48
 * @description  领料申请单-表单插件
 */
public class MaterialreqbiPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        if("auxpty".equals(e.getProperty().getName())){
            FlexEdit flexEdit = this.getControl("auxpty");
            String flexFieldKey = flexEdit.getFieldKey();
            DynamicObject flexFieldVal = (DynamicObject) this.getModel().getValue(flexFieldKey);
            if (flexFieldVal != null) {
                // 将弹性域字段的值赋值给其它字段
                String valueStr = flexFieldVal.get("value").toString();
                Map<String, Object> values = SerializationUtils.fromJsonString(valueStr, Map.class);
//                DynamicObject flexVal = BusinessDataServiceHelper.loadSingle(values.get("f000019"), "bos_adminorg");
//                this.getModel().setValue("nckd_orgfield", flexVal);
                int rowIndex = e.getChangeSet()[0].getRowIndex();
                // 原需求部门
                this.getModel().setValue("nckd_orgfield", values.get("f000019"),rowIndex);
                // 品牌
                this.getModel().setValue("nckd_brand", values.get("f000022"),rowIndex);

                // 判断申请部门和原需求部门是否一致
                DynamicObject applydept = (DynamicObject) this.getModel().getValue("applydept");
                if(applydept != null){
                    if(!applydept.get(0).equals(values.get("f000019"))){
                        this.getModel().setValue("nckd_other_depart",1);
                    }
                }
            }
        } else if ("applydept".equals(e.getProperty().getName())){
            DynamicObject applydept = (DynamicObject) e.getChangeSet()[0].getNewValue();
            if(applydept != null){
                // 获取单据体信息
                DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("billentry");
                for (DynamicObject dynamicObject : entryEntity) {
                    DynamicObject orgfield = (DynamicObject) dynamicObject.get("nckd_orgfield");
                    if(orgfield != null){
                        // 判断申请部门和原需求部门是否一致
                        if(!applydept.get(0).equals(orgfield.get(0))){
                            this.getModel().setValue("nckd_other_depart",1);
                            break;
                        }
                    }
                    this.getModel().setValue("nckd_other_depart",0);
                }
            }
        }
    }
}
