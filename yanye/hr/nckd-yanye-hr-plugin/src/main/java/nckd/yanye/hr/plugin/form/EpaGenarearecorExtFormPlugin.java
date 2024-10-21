package nckd.yanye.hr.plugin.form;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.CollectionUtils;
import kd.hr.hbp.common.constants.HRBaseConstants;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module           :目标绩效云-个人绩效考核-绩效评估
 * Description      :adjustval 调整值字段重新赋值
 * nckd_hsas_approvebill_ext
 * @author : yaosijie
 * @date : 2024/9/29
 */
public class EpaGenarearecorExtFormPlugin extends AbstractFormPlugin {

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
                    DynamicObject object = BusinessDataServiceHelper.loadSingle(dynamicObject.getDynamicObject("indicator").getPkValue(), "epa_genareaind_assign");
                    //根据考核活动id查询

                    DynamicObject epaActivityObject = BusinessDataServiceHelper.loadSingle(object.get("activity"),"epa_activity");
                    DynamicObject executeschemeObject = BusinessDataServiceHelper.loadSingle(epaActivityObject.getDynamicObject("executescheme").getPkValue(),"epa_executescheme");//考核周期执行计划
                    DynamicObject scheme = BusinessDataServiceHelper.loadSingle(epaActivityObject.getDynamicObject("scheme").getPkValue(),"epa_scheme");//考核计划
                    String period = executeschemeObject.getString("timetag.name");//考核周期：2024M3
                    Date startyear = scheme.getDate("startyear");//考核年度
                    Long cycletype = scheme.getLong("cycletype.id");//周期类型
                    Long cyclescheme = scheme.getLong("cyclescheme.id");//周期方案
                    //构造考核量化指标填报查询条件 周期类型，周期方案，考核量化指标
                    QFilter quantiFilter = new QFilter("nckd_cycletype.id", QCP.equals,cycletype).and("nckd_cycleprogramme.id",QCP.equals,cyclescheme)
                            .and("nckd_year",QCP.equals,startyear).and("nckd_assessment",QCP.equals,period);
                    DynamicObject[] quantizationfillingObject = BusinessDataServiceHelper.load("nckd_quantizationfilling" ,"id,entryentity.nckd_completevalue,entryentity.nckd_examinetarget,entryentity.nckd_completevalue" , new QFilter[]{quantiFilter});
                    //取第一条核量化指标填报数据
                    DynamicObject quantiDynamicObject = quantizationfillingObject[0];
                    //考核量化指标填报分录
                    DynamicObjectCollection dynamicObjectCollection = quantiDynamicObject.getDynamicObjectCollection("entryentity");
                    List<DynamicObject> objects = dynamicObjectCollection.stream().filter(t-> dynamicObject.getString("customfiled4").equals(t.getString("nckd_examinetarget.id"))).collect(Collectors.toList());
                    //获取完成值
                    if (CollectionUtils.isNotEmpty(objects)){
                        BigDecimal qualityres  = objects.get(0).getBigDecimal("nckd_completevalue");//完成值
//                invcountscheme.set("nckd_completevalue",completevalue);//完成值
//                    invcountscheme.set("qualityres",completevalue);//完成值
                        //完成值
//                    BigDecimal qualityres = object.getBigDecimal("qualityres");

                        //目标值
                        BigDecimal qualitytarget = object.getBigDecimal("qualitytarget");
                        BigDecimal addValue = null;
                        if (StringUtils.isNotEmpty(object.getLocaleString("customfiled2").getLocaleValue_zh_CN())){
                            //增加值
                            addValue = new BigDecimal(object.getLocaleString("customfiled2").getLocaleValue_zh_CN());
                        }

                        //每增加分
                        BigDecimal addGrade = null;
                        if (StringUtils.isNotEmpty(object.getLocaleString("customfiled3").getLocaleValue_zh_CN())){
                            //增加值
                            addGrade = new BigDecimal(object.getLocaleString("customfiled3").getLocaleValue_zh_CN());
                        }
                        //权重 = 1
                        BigDecimal weight = BigDecimal.ONE;
                        if (StringUtils.isNotEmpty(object.getLocaleString("customfiled5").getLocaleValue_zh_CN())){
                            weight = new BigDecimal(object.getLocaleString("customfiled5").getLocaleValue_zh_CN());
                        }
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
                            dynamicObject.set("qualityres",qualityres.setScale(2, RoundingMode.HALF_UP));
                            dynamicObject.set("customfiled5",weight.toString());
                            dynamicObject.set("soe",adjustValue.add(dynamicObject.getBigDecimal("beforeadjust")));
                            this.getView().updateView("entryentity");
                            //重新计算完成值
                        }
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
