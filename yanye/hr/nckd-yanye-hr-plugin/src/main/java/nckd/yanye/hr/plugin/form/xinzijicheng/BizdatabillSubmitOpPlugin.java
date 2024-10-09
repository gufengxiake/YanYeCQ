package nckd.yanye.hr.plugin.form.xinzijicheng;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.util.StringUtils;

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
                    DynamicObjectCollection entryEntity = (DynamicObjectCollection) dataEntity.getValue("entryentity");
                    boolean flag = entryEntity.stream()
                            .map(entry -> entry.getString("remark"))
                            .anyMatch(remark -> !StringUtils.isEmpty(remark));
                    if (flag) {
                        this.addErrorMessage(dataEntity, "业务数据异常，请参照备注进行修改！");
                    }
                }
            }
        });
    }
}