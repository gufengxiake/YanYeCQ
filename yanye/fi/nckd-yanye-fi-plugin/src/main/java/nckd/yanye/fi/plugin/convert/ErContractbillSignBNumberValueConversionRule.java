package nckd.yanye.fi.plugin.convert;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.isc.iscb.platform.core.connector.ConnectionWrapper;
import kd.isc.iscb.platform.core.vc.JavaValueConversionRule;

/**
 * @author husheng
 * @date 2024-08-13 9:18
 * @description 合同台账单-》合同台账 签约放乙方编码 集成值转换
 */
public class ErContractbillSignBNumberValueConversionRule implements JavaValueConversionRule {
    @Override
    public Object cast(String s, ConnectionWrapper connectionWrapper, ConnectionWrapper connectionWrapper1) {
        DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(s,"er_contractbill");
        DynamicObject object= loadSingle.getDynamicObjectCollection("contractpartyentry").stream()
                .filter(dynamicObject -> "1".equals(dynamicObject.getString("signcontract")))
                .findFirst().orElse(null);

        if(object != null){
            if(object.getDynamicObject("contractparty") != null){
                return object.getDynamicObject("contractparty").getString("number");
            }
        }

        return null;
    }
}
