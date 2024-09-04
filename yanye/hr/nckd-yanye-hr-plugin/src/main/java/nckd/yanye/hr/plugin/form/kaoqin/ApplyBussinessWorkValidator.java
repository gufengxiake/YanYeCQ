package nckd.yanye.hr.plugin.form.kaoqin;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.servicehelper.MetadataServiceHelper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Module           :工时假勤云-加班管理-为他人申请加班校验插件
 * Description      :为他人申请加班校验插件
 *
 * @author guozhiwei
 * @date  2024-08-30 11：15
 *
 * wtom_overtimeapp
 *
 */

public class ApplyBussinessWorkValidator extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        // 提前加载表单里的字段
        List<String> fieldKeys = e.getFieldKeys();
        MainEntityType dt = MetadataServiceHelper.getDataEntityType("wtom_overtimeapp");
        Map<String, IDataEntityProperty> fields = dt.getAllFields();
        fields.forEach((Key, value) -> {
            fieldKeys.add(Key);
        });
    }




    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    DynamicObject dataEntityObj = dataEntity.getDataEntity();

                    // 判断类型 在去获取分录，循环分录，获取分录的开始时间，判断是否在本周内
                    String otapplytype = dataEntityObj.getString("otapplytype");
                    // 获取本周一的日期
                    Date monday = getMonday();
                    // 如果存在
                    boolean flag = false;
                    if("1".equals(otapplytype)){
                        // 按时段申请sdentry
                        DynamicObjectCollection dynamicObjectCollection = dataEntityObj.getDynamicObjectCollection("sdentry");
                        for (DynamicObject dynamicObject : dynamicObjectCollection) {
                            if(dynamicObject.getDate("otstartdate").before(monday)){
                                flag = true;
                                break;
                            }
                        }
                    }else{
                        // 按时长申请scentry
                        DynamicObjectCollection dynamicObjectCollection = dataEntityObj.getDynamicObjectCollection("scentry");
                        for (DynamicObject dynamicObject : dynamicObjectCollection) {
                            if(dynamicObject.getDate("otdstarttime").before(monday)){
                                flag = true;
                                break;
                            }
                        }
                    }
                    // 判断是否在本周内
                    if (flag) {
                        this.addErrorMessage(dataEntity, "单据" + dataEntityObj.getString("billno") + "为上周单据，不允许上报！");
                    }
                }
            }
        });
    }

    // 获取本周一时间
    public static Date getMonday() {

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            monday = today;
        } else if (today.getDayOfWeek().compareTo(DayOfWeek.MONDAY) > 0) {
            monday = today.with(DayOfWeek.MONDAY);
        } else {
            monday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        }
        return Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant());

    }
}
