package nckd.yanye.scm.plugin.convert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

/**
 * @author husheng
 * @date 2024-08-01 13:01
 * @description 物料申请单下推物料出库单字段赋值
 */
public class MaterialreqConvertPlugIn extends AbstractConvertPlugIn {

    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);

        // 获取目标单
        String name = this.getTgtMainType().getName();
        ExtendedDataEntity[] dataEntities = e.getTargetExtDataEntitySet().FindByEntityKey(name);

        for (ExtendedDataEntity dataEntity : dataEntities) {
            DynamicObject dynamicObject = dataEntity.getDataEntity();
            for (DynamicObject object : dynamicObject.getDynamicObjectCollection("billentry")) {
                DynamicObject material = object.getDynamicObject("material");
                DynamicObject masterid = material.getDynamicObject("masterid");
                DynamicObject group = masterid.getDynamicObject("group");
                if (group != null) {
                    object.set("nckd_material_group", group);

                    DynamicObject org = dynamicObject.getDynamicObject("org");
                    QFilter qFilter1 = new QFilter("nckd_material_class.id", QCP.equals, group.getPkValue());
                    QFilter qFilter2 = new QFilter("nckd_org.id", QCP.equals, org.getPkValue());
                    QFilter qFilter3 = new QFilter("status", QCP.equals, "C");
                    DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("nckd_im_evaluate_material", "nckd_evaluate_period", new QFilter[]{qFilter1, qFilter2, qFilter3});
                    if (loadSingle != null) {
                        Integer evaluatePeriod = (Integer) loadSingle.get("nckd_evaluate_period");
                        if (evaluatePeriod != null) {
                            object.set("nckd_evaluate_period", evaluatePeriod);

                            Date biztime = (Date) dynamicObject.get("biztime");
                            LocalDateTime localDateTime = biztime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            object.set("nckd_evaluate_date", Date.from(localDateTime.plusDays(evaluatePeriod).atZone(ZoneId.systemDefault()).toInstant()));
                            object.set("nckd_evaluate_flag", 1);
                        }
                    }
                }
            }
        }
    }
}
