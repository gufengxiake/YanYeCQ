package nckd.yanye.scm.plugin.form;

import java.util.Objects;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.AfterDeleteRowEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.FlexEdit;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

/**
 * @author husheng
 * @date 2024-07-13 18:48
 * @description 领料申请单-表单插件
 */
public class MaterialreqbiFormPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        if ("auxpty".equals(e.getProperty().getName())) {
            FlexEdit flexEdit = this.getControl("auxpty");
            String flexFieldKey = flexEdit.getFieldKey();
            DynamicObject flexFieldVal = (DynamicObject) this.getModel().getValue(flexFieldKey);
            int rowIndex = e.getChangeSet()[0].getRowIndex();
            if (flexFieldVal != null && !Objects.equals(flexFieldVal.getPkValue(),0L)) {
                // 将弹性域字段的值赋值给其它字段
//                String valueStr = flexFieldVal.get("value").toString();
//                Map<String, Object> values = SerializationUtils.fromJsonString(valueStr, Map.class);
//                DynamicObject flexVal = BusinessDataServiceHelper.loadSingle(values.get("f000019"), "bos_adminorg");
//                this.getModel().setValue("nckd_orgfield", flexVal);
                QFilter filter = new QFilter("hg", QCP.equals, flexFieldVal.getPkValue());
                DynamicObjectCollection flexauxpropBd = QueryServiceHelper.query("bd_flexauxprop_bd", "hg, auxproptype , auxpropval", filter.toArray());
                if(flexauxpropBd.size() > 0){
                    // 原需求部门
                    this.getModel().setValue("nckd_orgfield", flexauxpropBd.get(0).get("auxpropval"), rowIndex);
                }
                if(flexauxpropBd.size() > 1){
                    // 品牌
                    this.getModel().setValue("nckd_brand", flexauxpropBd.get(1).get("auxpropval"), rowIndex);
                }

                // 判断申请部门和原需求部门是否一致
                DynamicObject applydept = (DynamicObject) this.getModel().getValue("applydept");
                if (applydept != null && flexauxpropBd.size() > 0) {
                    if (!applydept.get(0).equals(flexauxpropBd.get(0).get("auxpropval"))) {
                        this.getModel().setValue("nckd_other_depart", 1);
                    }
                }
            }
        } else if ("applydept".equals(e.getProperty().getName())) {
            DynamicObject applydept = (DynamicObject) e.getChangeSet()[0].getNewValue();
            if (applydept != null) {
                // 获取单据体信息
                DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("billentry");
                for (DynamicObject dynamicObject : entryEntity) {
                    DynamicObject orgfield = (DynamicObject) dynamicObject.get("nckd_orgfield");
                    if (orgfield != null) {
                        // 判断申请部门和原需求部门是否一致
                        if (!applydept.get(0).equals(orgfield.get(0))) {
                            this.getModel().setValue("nckd_other_depart", 1);
                            break;
                        }
                    }
                    this.getModel().setValue("nckd_other_depart", 0);
                }
            }
        } else if ("material".equals(e.getProperty().getName())) {
            ChangeData changeData = e.getChangeSet()[0];
            int rowIndex = e.getChangeSet()[0].getRowIndex();
            if (changeData.getNewValue() == null) {
                // 原需求部门
                this.getModel().setValue("nckd_orgfield", null, rowIndex);
                // 品牌
                this.getModel().setValue("nckd_brand", null, rowIndex);
                // 判断申请部门和原需求部门是否一致
                DynamicObject applydept = (DynamicObject) this.getModel().getValue("applydept");
                if (applydept != null) {
                    // 领用其他部门物资
                    this.getModel().setValue("nckd_other_depart", 0);

                    DynamicObjectCollection billentry = this.getModel().getEntryEntity("billentry");
                    for (DynamicObject dynamicObject : billentry) {
                        DynamicObject nckdOrgfield = dynamicObject.getDynamicObject("nckd_orgfield");
                        if(nckdOrgfield != null){
                            if (!applydept.getPkValue().equals(nckdOrgfield.getPkValue())) {
                                this.getModel().setValue("nckd_other_depart", 1);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void afterDeleteRow(AfterDeleteRowEventArgs e) {
        super.afterDeleteRow(e);

        if("billentry".equals(e.getEntryProp().getName())){
            DynamicObject applydept = (DynamicObject) this.getModel().getValue("applydept");
            if (applydept != null) {
                // 领用其他部门物资
                this.getModel().setValue("nckd_other_depart", 0);

                DynamicObjectCollection billentry = this.getModel().getEntryEntity("billentry");
                for (DynamicObject dynamicObject : billentry) {
                    DynamicObject nckdOrgfield = dynamicObject.getDynamicObject("nckd_orgfield");
                    if(nckdOrgfield != null){
                        if (!applydept.getPkValue().equals(nckdOrgfield.getPkValue())) {
                            this.getModel().setValue("nckd_other_depart", 1);
                            break;
                        }
                    }
                }
            }
        }
    }
}
