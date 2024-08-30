package nckd.yanye.hr.plugin.form.kaoqin;

import kd.bos.dataentity.entity.DynamicObject;
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
 * Module           :工时假勤云-加班管理-为他人申请加班,为他人申请休假,为他人申请加班,为他人申请补签校验插件
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
                    // 获取
                    Date createtime = dataEntityObj.getDate("createtime");
                    // 获取本周一的日期
                    Date monday = getMonday();
                    // 判断是否在本周内
                    if (createtime.before(monday)) {
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
        if (today.getDayOfWeek().compareTo(DayOfWeek.MONDAY) > 0) {
            monday = today.with(DayOfWeek.MONDAY);
        } else {
            monday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        }
        return Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant());

    }
}
