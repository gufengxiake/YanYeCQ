package nckd.yanye.fi.plugin.convert;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.isc.iscb.platform.core.connector.ConnectionWrapper;
import kd.isc.iscb.platform.core.vc.JavaValueConversionRule;
import kd.tmc.gm.common.enums.GuaranteeTypeEnum;

/**
 * @author husheng
 * @date 2024-08-13 9:18
 * @description 担保合同-》合同台账 合同签约乙方名称 集成值转换
 */
public class GmGuaranteecontractBNameValueConversionRule implements JavaValueConversionRule {
    @Override
    public Object cast(String s, ConnectionWrapper connectionWrapper, ConnectionWrapper connectionWrapper1) {
        // 担保合同
        DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(s,"gm_guaranteecontract");
        // 被担保人
        DynamicObject dynamicObject = loadSingle.getDynamicObjectCollection("entry_guaranteedorg").get(0);
        if(dynamicObject != null){
            // 担保人类型
            String gType = dynamicObject.getString("b_reguaranteetype");
            // 对应类型的界面
            String formId = GuaranteeTypeEnum.getFormId(gType);
            DynamicObject object = BusinessDataServiceHelper.loadSingle(dynamicObject.get("b_guaranteedorg"), formId);
            return object.getString("name");
        }

        return null;
    }
}
