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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Module           :工时假勤云-日常考勤-出差申请单-为他人申请出差校验插件
 * Description      :为他人申请出差校验插件
 *
 * @author guozhiwei
 * @date  2024-08-30 11：15
 *
 *  wtam_busitripbill
 *
 */

public class ApplyBussinessTripValidator extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        // 提前加载表单里的字段
        List<String> fieldKeys = e.getFieldKeys();
        MainEntityType dt = MetadataServiceHelper.getDataEntityType("wtam_busitripbill");
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
                    // 获取本周一的日期
                    Date monday = ApplyBussinessWorkValidator.getMonday();
                    boolean flag = false;
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    DynamicObjectCollection entryentity = dataEntityObj.getDynamicObjectCollection("entryentity");
                    for (DynamicObject dynamicObject : entryentity) {
                        String starttimetext = dynamicObject.getString("startdatestr");
                        try {
                            // 转换为日期格式
                            Date date = formatter.parse(starttimetext);
                            if(date.before(monday)){
                                flag = true;
                            }
                        } catch (ParseException ex) {
                            throw new RuntimeException(ex);
                        }
                        if(flag){
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