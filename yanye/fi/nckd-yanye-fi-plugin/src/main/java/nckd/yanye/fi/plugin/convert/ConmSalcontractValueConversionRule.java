package nckd.yanye.fi.plugin.convert;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.isc.iscb.platform.core.connector.ConnectionWrapper;
import kd.isc.iscb.platform.core.vc.JavaValueConversionRule;

/**
 * @author husheng
 * @date 2024-08-13 9:18
 * @description  销售合同-》合同台账 税率 集成值转换
 */
public class ConmSalcontractValueConversionRule implements JavaValueConversionRule {
    @Override
    public Object cast(String s, ConnectionWrapper connectionWrapper, ConnectionWrapper connectionWrapper1) {
        DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(s, "conm_salcontract");
        String result = loadSingle.getDynamicObjectCollection("billentry").stream()
                .filter(dynamicObject -> dynamicObject.getBigDecimal("taxrate").compareTo(new BigDecimal(0)) != 0)
                .map(dynamicObject -> dynamicObject.get("taxrate") + "%")
                .distinct()
                .collect(Collectors.toList())
                .stream().collect(Collectors.joining(","));

        return result;
    }
}
