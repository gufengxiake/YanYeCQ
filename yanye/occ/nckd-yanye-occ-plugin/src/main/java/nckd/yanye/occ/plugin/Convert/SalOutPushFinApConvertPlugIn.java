package nckd.yanye.occ.plugin.Convert;

import kd.bos.dataentity.metadata.dynamicobject.DynamicProperty;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.sdk.plugin.Plugin;
import nckd.yanye.occ.plugin.operate.SalOutSaveOperationPlugIn;

import java.util.Map;

/**
 * 单据转换插件
 */
public class SalOutPushFinApConvertPlugIn extends AbstractConvertPlugIn implements Plugin {
    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);
        ExtendedDataEntity[] extendedDataEntities = e.getTargetExtDataEntitySet().FindByEntityKey(this.getTgtMainType().toString());
        DynamicProperty nckdDeliverchannel = e.getFldProperties().get("nckd_deliverchannel");
        if(nckdDeliverchannel == null){

            return;
        }
        Long id = (Long) nckdDeliverchannel.getValue("id");
        Long supplierId = new SalOutSaveOperationPlugIn().getSupplierId(id);
        for (ExtendedDataEntity extendedDataEntity : extendedDataEntities) {
            extendedDataEntity.setValue("asstact",supplierId);
        }
    }

}