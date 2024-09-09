package nckd.yanye.scm.plugin.operate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.validate.AbstractValidator;

/**
 * @author husheng
 * @date 2024-09-09 9:19
 * @description 领料出库单（nckd_im_materialreqou_ext） 提交时校验项目号
 */
public class MaterialreqouSubmitOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] entities = this.getDataEntities();
                Arrays.asList(entities).forEach(k -> {
                    DynamicObject dynamicObject = k.getDataEntity();
                    DynamicObject invscheme = dynamicObject.getDynamicObject("invscheme");
                    // 库存事务是在建工程领用出库
                    if("JY-032".equals(invscheme.getString("number"))){
                        // 在建工程项目
                        DynamicObject nckdZjgcxm = dynamicObject.getDynamicObject("nckd_zjgcxm");
                        if(nckdZjgcxm != null){
                            DynamicObjectCollection billentry = dynamicObject.getDynamicObjectCollection("billentry");
                            long count = billentry.stream().filter(d -> d.getDynamicObject("project") != null).count();
                            if(count > 0 && count != billentry.size()){
                                this.addErrorMessage(k, "物料明细的项目号要全都有值或全都没有值");
                            }
                            List<Object> projectList = billentry.stream()
                                    .filter(d -> d.getDynamicObject("project") != null && d.getDynamicObject("project").getPkValue() != nckdZjgcxm.getPkValue())
                                    .map(d -> d.getDynamicObject("project").getPkValue())
                                    .collect(Collectors.toList());
                            if(projectList.size() > 0){
                                this.addErrorMessage(k, "物料明细的项目号要与在建工程项目一致");
                            }
                        }
                    }
                });
            }
        });
    }
}
