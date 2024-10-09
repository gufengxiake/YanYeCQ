package nckd.yanye.occ.plugin.Convert;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.ExtendedDataEntitySet;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.occ.ocbase.common.strategy.CtrlStrategyUtils;
import kd.occ.ocbase.common.util.DynamicObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class ChannelReqPushCustomerConverPlugIn extends AbstractConvertPlugIn {

    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);
        ExtendedDataEntitySet entitySet = e.getTargetExtDataEntitySet();
        if (entitySet != null) {
            ExtendedDataEntity[] entitys = entitySet.FindByEntityKey(this.getTgtMainType().toString());
            Map<Long, String> cache = new HashMap(entitys.length);
            ExtendedDataEntity[] var5 = entitys;
            int var6 = entitys.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                ExtendedDataEntity entity = var5[var7];
                DynamicObject customer = entity.getDataEntity();
                DynamicObjectCollection groupstandard=customer.getDynamicObjectCollection("entry_groupstandard");
//                if(groupstandard.isEmpty()){
//                    DynamicObject group= new DynamicObject(groupstandard.getDynamicObjectType());
//                    //group.set("");
//
//                }
            }
        }

    }

}
