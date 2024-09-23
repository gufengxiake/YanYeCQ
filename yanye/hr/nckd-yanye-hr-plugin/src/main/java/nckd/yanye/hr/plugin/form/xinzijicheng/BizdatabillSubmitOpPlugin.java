package nckd.yanye.hr.plugin.form.xinzijicheng;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;

import java.util.List;

/**
 * Module           :薪酬福利云-薪资数据集成-业务数据提报
 * Description      :提交校验
 * 单据标识：nckd_hpdi_bizdatabill_ext
 *
 * @author : liuxiao
 * @since : 2024/9/23
 */
public class BizdatabillSubmitOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
//        fieldKeys.add("remark");
    }

    /**
     * @param e
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    boolean flag = false;
                    DynamicObjectCollection entryEntity = (DynamicObjectCollection) dataEntity.getValue("entryentity");
                    for (DynamicObject entry : entryEntity) {
                        String remark = entry.getString("remark");
                        if (remark.contains("分配比例超限")) {
                            flag = true;
                        }
                    }
                    if (flag) {
                        this.addErrorMessage(dataEntity, "分配比例超限，请检查分配比例是否正确！");
                    }
                }
            }
        });
    }
}