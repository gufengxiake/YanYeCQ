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
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Module           :工时假勤云-日常考勤-为他人申请补签校验插件
 * Description      :为他人申请补签校验插件
 *
 * @author guozhiwei
 * @date  2024-08-30 15：09
 *
 *   wtpm_supsignpc
 *
 */

public class ApplyBussinessReplaceValidator extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        // 提前加载表单里的字段
        List<String> fieldKeys = e.getFieldKeys();
        MainEntityType dt = MetadataServiceHelper.getDataEntityType("wtpm_supsignpc");
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
                    Date monday = ApplyBussinessWorkValidator.getMonday();
                    boolean flag = false;
                    // 获取分录开始时间，判断是否在本周内
                    DynamicObjectCollection entryentity = dataEntityObj.getDynamicObjectCollection("entryentity");
                    for (DynamicObject dynamicObject : entryentity) {
                        // 获取补签日期
                        Date entrytime = dynamicObject.getDate("signdate");
                        if(entrytime.before(monday)){
                            flag = true;
                            break;
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

}
