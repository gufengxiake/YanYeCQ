package nckd.yanye.scm.plugin.form;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.datamodel.events.AfterDeleteRowEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.FlexEdit;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

/**
 * @author husheng
 * @date 2024-07-12 17:01
 * @description 领料出库单-表单插件
 */
public class MaterialreqouFormPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);

        String name = e.getProperty().getName();
        if ("material".equals(name)) {
            // 物料编码
            ChangeData changeData = e.getChangeSet()[0];
            DynamicObject newValue = (DynamicObject) changeData.getNewValue();
            int rowIndex = changeData.getRowIndex(); //修改行所在行行号
            if(newValue != null){
                DynamicObject masterid = newValue.getDynamicObject("masterid");
                DynamicObject group = masterid.getDynamicObject("group");
                if (group != null) {
                    this.getModel().setValue("nckd_material_group", group,rowIndex);

                    DynamicObject org = (DynamicObject) this.getModel().getValue("org");
                    QFilter qFilter1 = new QFilter("nckd_material_class.id", QCP.equals, group.getPkValue());
                    QFilter qFilter2 = new QFilter("useorg.id", QCP.equals, org.getPkValue());
                    QFilter qFilter3 = new QFilter("status", QCP.equals, "C");
                    DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("nckd_im_evaluate_material", "nckd_evaluate_period", new QFilter[]{qFilter1, qFilter2, qFilter3});
                    if(dynamicObject != null){
                        Integer evaluatePeriod = (Integer) dynamicObject.get("nckd_evaluate_period");
                        if(evaluatePeriod != null){
                            this.getModel().setValue("nckd_evaluate_period", evaluatePeriod,rowIndex);

                            Date biztime = (Date) this.getModel().getValue("biztime");
                            LocalDateTime localDateTime = biztime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            this.getModel().setValue("nckd_evaluate_date", localDateTime.plusDays(evaluatePeriod),rowIndex);
                            this.getModel().setValue("nckd_evaluate_flag", 1,rowIndex);
                        }
                    }

                    // 刷新页面
                    this.getView().updateView();
                }
            }else{
                // 原需求部门
                this.getModel().setValue("nckd_orgfield", null, rowIndex);
                // 品牌
                this.getModel().setValue("nckd_brand", null, rowIndex);
                // 判断申请部门和原需求部门是否一致
                DynamicObject applydept = (DynamicObject) this.getModel().getValue("bizdept");
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
        } else if ("auxpty".equals(name)) {
            // 辅助属性
            FlexEdit flexEdit = this.getControl("auxpty");
            String flexFieldKey = flexEdit.getFieldKey();
            DynamicObject flexFieldVal = (DynamicObject) this.getModel().getValue(flexFieldKey);
            if (flexFieldVal != null && !Objects.equals(flexFieldVal.getPkValue(),0L)) {
//                // 将弹性域字段的值赋值给其它字段
//                String valueStr = flexFieldVal.get("value").toString();
//                Map<String, Object> values = SerializationUtils.fromJsonString(valueStr, Map.class);
                int rowIndex = e.getChangeSet()[0].getRowIndex();
                QFilter filter=new QFilter( "hg", QCP.equals, flexFieldVal.getPkValue());
                DynamicObjectCollection flexauxpropBd = QueryServiceHelper.query("bd_flexauxprop_bd", "hg, auxproptype , auxpropval", filter.toArray());
                if(flexauxpropBd.size() > 0){
                    // 原需求部门
                    this.getModel().setValue("nckd_orgfield", flexauxpropBd.get(0).get("auxpropval"),rowIndex);
                }
                if(flexauxpropBd.size() > 1){
                    // 品牌
                    this.getModel().setValue("nckd_brand", flexauxpropBd.get(1).get("auxpropval"),rowIndex);
                }

                // 判断需求部门和原需求部门是否一致
                DynamicObject bizdept = (DynamicObject) this.getModel().getValue("bizdept");
                if (bizdept != null && flexauxpropBd.size() > 0) {
                    if (!bizdept.get(0).equals(flexauxpropBd.get(0).get("auxpropval"))) {
                        this.getModel().setValue("nckd_other_depart", 1);
                    }
                }
            }
        } else if ("bizdept".equals(name)) {
            // 需求部门
            DynamicObject bizdept = (DynamicObject) e.getChangeSet()[0].getNewValue();
            if (bizdept != null) {
                // 获取单据体信息
                DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("billentry");
                for (DynamicObject dynamicObject : entryEntity) {
                    DynamicObject orgfield = (DynamicObject) dynamicObject.get("nckd_orgfield");
                    if (orgfield != null) {
                        // 判断需求部门和原需求部门是否一致
                        if (!bizdept.get(0).equals(orgfield.get(0))) {
                            this.getModel().setValue("nckd_other_depart", 1);
                            break;
                        }
                    }
                    this.getModel().setValue("nckd_other_depart", 0);
                }
            }
        } else if ("biztime".equals(name)) {
            // 业务日期
            Date biztime = (Date) e.getChangeSet()[0].getNewValue();
            Calendar cal = Calendar.getInstance();
            if(biztime != null){
                DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("billentry");
                for (DynamicObject dynamicObject : entryEntity) {
                    Integer evaluatePeriod = (Integer) dynamicObject.get("nckd_evaluate_period");
                    if(evaluatePeriod != null){
                        cal.setTime(biztime);
                        cal.add(Calendar.DATE, evaluatePeriod);
                        dynamicObject.set("nckd_evaluate_date",cal.getTime());
                        dynamicObject.set("nckd_evaluate_flag", 1);
                    }
                }
                // 刷新页面
                this.getView().updateView();
            }
        }
    }

    @Override
    public void afterDeleteRow(AfterDeleteRowEventArgs e) {
        super.afterDeleteRow(e);

        if("billentry".equals(e.getEntryProp().getName())){
            DynamicObject applydept = (DynamicObject) this.getModel().getValue("bizdept");
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
