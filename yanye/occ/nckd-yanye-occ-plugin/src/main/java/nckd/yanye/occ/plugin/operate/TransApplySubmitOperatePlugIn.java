package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调拨单申请单提交服务
 * 校验启用辅助属性的物料行,辅助属性是否为空
 * 表单标识：nckd_im_transapply_ext
 * @author zhangzhilong
 * @since 2024-10-21
 */
public class TransApplySubmitOperatePlugIn extends AbstractOperationServicePlugIn implements Plugin {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("billentry.material");//物料
        e.getFieldKeys().add("billentry.auxpty");//辅助属性
    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        //校验辅助属性是否启用
        e.addValidator(new SalOrderSubmitOperatePlugIn.MaterialAuxPtyCheckValidator());
    }
}