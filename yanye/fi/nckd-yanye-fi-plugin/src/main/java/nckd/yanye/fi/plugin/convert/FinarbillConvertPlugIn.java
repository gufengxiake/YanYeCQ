package nckd.yanye.fi.plugin.convert;

import java.util.Arrays;
import java.util.stream.Collectors;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.imc.sim.formplugin.issuing.helper.IssueInvoiceControlHelper;

/**
 * @author husheng
 * @date 2024-08-08 18:11
 * @description  开票申请-一键开票
 */
public class FinarbillConvertPlugIn extends AbstractConvertPlugIn {
    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);

        // 获取目标单
        String name = this.getTgtMainType().getName();
        ExtendedDataEntity[] dataEntities = e.getTargetExtDataEntitySet().FindByEntityKey(name);

        DynamicObject[] dynamicObjects = Arrays.stream(dataEntities).map(d -> d.getDataEntity())
                .collect(Collectors.toList()).toArray(new DynamicObject[]{});

        // 一键开票
        IssueInvoiceControlHelper.issueInvoice(dynamicObjects, 1, false, false);
    }
}
