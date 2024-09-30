package nckd.yanye.hr.plugin.form;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.hr.hbp.common.constants.HRBaseConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EventObject;
/**
 * Module           :目标绩效云-个人绩效考核-绩效评估
 * Description      :adjustval 调整值字段重新赋值
 * nckd_hsas_approvebill_ext
 * @author : yaosijie
 * @date : 2024/9/29
 */
public class EpaGenarearecorExtFormPlugin extends AbstractFormPlugin {

//    @Override
//    public void beforeBindData(EventObject e) {
//        super.beforeBindData(e);
//        Object pkValue = this.getModel().getValue("id");
//        //常规指标区评估小计
//        DynamicObject epaGenarearecorObject = BusinessDataServiceHelper.loadSingle(pkValue, "epa_genarearecor");
//        //常规指标区评估小计分录
//        DynamicObjectCollection dynamicObjects = epaGenarearecorObject.getDynamicObjectCollection("entryentity");
//        for (DynamicObject dynamicObject : dynamicObjects){
//            //完成值
//            BigDecimal qualityres = dynamicObject.getBigDecimal("qualityres");
//
//            //目标值
//            BigDecimal qualitytarget = dynamicObject.getBigDecimal("qualitytarget");
//            BigDecimal addValue = null;
//            if (dynamicObject.get("customfiled2") instanceof BigDecimal){
//                //增加值
//                addValue = dynamicObject.getBigDecimal("customfiled2");
//            }
//
//            //每增加分
//            BigDecimal addGrade = null;
//            if (dynamicObject.get("customfiled3") instanceof BigDecimal){
//                //增加值
//                addGrade = dynamicObject.getBigDecimal("customfiled3");
//            }
//            //权重 = 1
//            BigDecimal weight = BigDecimal.ONE;
//            //计算 可调整分值
//            if (qualityres != null
//                    && qualitytarget != null
//                    && addValue != null
//                    && addGrade != null
//                    && qualityres.compareTo(BigDecimal.ZERO) > 0
//                    && qualitytarget.compareTo(BigDecimal.ZERO) > 0
//                    && addValue.compareTo(BigDecimal.ZERO) > 0
//                    && addGrade.compareTo(BigDecimal.ZERO) > 0){
//                BigDecimal adjustValue = ((qualityres.subtract(qualitytarget)).divide(addValue)).multiply(addGrade).multiply(weight);
//                dynamicObject.set("adjustval",adjustValue.setScale(2, RoundingMode.HALF_UP));
//            }
//        }
//        SaveServiceHelper.update(epaGenarearecorObject);
//    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
        if (OperationStatus.VIEW != status){
            DynamicObjectCollection entryEntity = this.getModel().getDataEntity(true).getDynamicObjectCollection(HRBaseConstants.ENTRYENTITY);
            if (entryEntity != null){
                for (DynamicObject dynamicObject : entryEntity) {
                    if (dynamicObject == null){
                        continue;
                    }
                    //完成值
                    BigDecimal qualityres = dynamicObject.getBigDecimal("qualityres");

                    //目标值
                    BigDecimal qualitytarget = dynamicObject.getBigDecimal("qualitytarget");
                    BigDecimal addValue = null;
                    if (dynamicObject.get("customfiled2") instanceof BigDecimal){
                        //增加值
                        addValue = dynamicObject.getBigDecimal("customfiled2");
                    }

                    //每增加分
                    BigDecimal addGrade = null;
                    if (dynamicObject.get("customfiled3") instanceof BigDecimal){
                        //增加值
                        addGrade = dynamicObject.getBigDecimal("customfiled3");
                    }
                    //权重 = 1
                    BigDecimal weight = BigDecimal.ONE;
                    //计算 可调整分值
                    if (qualityres != null
                            && qualitytarget != null
                            && addValue != null
                            && addGrade != null
                            && qualityres.compareTo(BigDecimal.ZERO) > 0
                            && qualitytarget.compareTo(BigDecimal.ZERO) > 0
                            && addValue.compareTo(BigDecimal.ZERO) > 0
                            && addGrade.compareTo(BigDecimal.ZERO) > 0){
                        BigDecimal adjustValue = ((qualityres.subtract(qualitytarget)).divide(addValue)).multiply(addGrade).multiply(weight);
                        dynamicObject.set("adjustval",adjustValue.setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
    }
}
