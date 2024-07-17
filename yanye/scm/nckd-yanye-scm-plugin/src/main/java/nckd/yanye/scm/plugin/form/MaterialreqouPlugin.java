package nckd.yanye.scm.plugin.form;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.FlexEdit;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

/**
 * @author husheng
 * @date 2024-07-12 17:01
 * @description 领料出库单-表单插件
 */
public class MaterialreqouPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);

        String name = e.getProperty().getName();
        if ("material".equals(name)) {
            // 物料编码
            ChangeData changeData = e.getChangeSet()[0];
            DynamicObject newValue = (DynamicObject) changeData.getNewValue();
            int rowIndex = changeData.getRowIndex(); //修改行所在行行号
            DynamicObject masterid = newValue.getDynamicObject("masterid");
            DynamicObject group = masterid.getDynamicObject("group");
            if (group != null) {
                this.getModel().setValue("nckd_material_group", group,rowIndex);

                DynamicObject org = (DynamicObject) this.getModel().getValue("org");
                QFilter qFilter1 = new QFilter("nckd_material_class.id", QCP.equals, group.getPkValue());
                QFilter qFilter2 = new QFilter("createorg.id", QCP.equals, org.getPkValue());
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
        } else if ("auxpty".equals(name)) {
            // 辅助属性
            FlexEdit flexEdit = this.getControl("auxpty");
            String flexFieldKey = flexEdit.getFieldKey();
            DynamicObject flexFieldVal = (DynamicObject) this.getModel().getValue(flexFieldKey);
            if (flexFieldVal != null) {
                // 将弹性域字段的值赋值给其它字段
                String valueStr = flexFieldVal.get("value").toString();
                Map<String, Object> values = SerializationUtils.fromJsonString(valueStr, Map.class);
//                DynamicObject flexVal = BusinessDataServiceHelper.loadSingle(values.get("f000019"), "bos_adminorg");
                this.getModel().setValue("nckd_orgfield", values.get("f000019"),0);
//				DynamicObject flexVal2 = BusinessDataServiceHelper.loadSingle(values.get("f000022"), "bos_assistantdata_detail");
                this.getModel().setValue("nckd_brand", values.get("f000022"),0);

                // 判断需求部门和原需求部门是否一致
                DynamicObject bizdept = (DynamicObject) this.getModel().getValue("bizdept");
                if (bizdept != null) {
                    if (!bizdept.get(0).equals(values.get("f000019"))) {
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
}
