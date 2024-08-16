package nckd.yanye.fi.plugin.convert;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.isc.iscb.platform.core.connector.ConnectionWrapper;
import kd.isc.iscb.platform.core.vc.JavaValueConversionRule;
import kd.tmc.gm.common.enums.GuaranteeTypeEnum;

/**
 * @author husheng
 * @date 2024-08-13 9:18
 * @description 担保合同-》合同台账 合同签约甲方编码 集成值转换
 */
public class GmGuaranteecontractANumberValueConversionRule implements JavaValueConversionRule {
    @Override
    public Object cast(String s, ConnectionWrapper connectionWrapper, ConnectionWrapper connectionWrapper1) {
        // 担保合同
        DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(s,"gm_guaranteecontract");
        // 担保人
        DynamicObject dynamicObject = loadSingle.getDynamicObjectCollection("entry_guaranteeorg").get(0);
        if(dynamicObject != null){
            // 担保人类型
            String gType = dynamicObject.getString("a_guaranteetype");
            // 对应类型的界面
            String formId = GuaranteeTypeEnum.getFormId(gType);
            DynamicObject object = BusinessDataServiceHelper.loadSingle(dynamicObject.get("a_guaranteeorg"), formId);
            return object.getString("number");
        }

        return null;
    }
}
