package nckd.yanye.fi.plugin.convert;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.isc.iscb.platform.core.connector.ConnectionWrapper;
import kd.isc.iscb.platform.core.vc.JavaValueConversionRule;

/**
 * @author husheng
 * @date 2024-08-13 9:18
 * @description 担保合同-》合同台账 担保方式 集成值转换
 */
public class GmGuaranteecontractValueConversionRule implements JavaValueConversionRule {
    @Override
    public Object cast(String s, ConnectionWrapper connectionWrapper, ConnectionWrapper connectionWrapper1) {
        // 担保合同
        DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(s,"gm_guaranteecontract");
        String[] fguaranteeways = loadSingle.getString("guaranteeway").split(",");
        String value = Arrays.stream(fguaranteeways).filter(s1 -> !Objects.isNull(s1)).map(s1 -> {
            String way = null;
            if ("ensure".equals(s1)) {//保证
                way = "保证";
            } else if ("mortgage".equals(s1)) {//抵押
                way = "抵押";
            } else if ("pledge".equals(s1)) {//质押
                way = "质押";
            }
            return way;
        }).filter(s1 -> !Objects.isNull(s1)).collect(Collectors.joining(";"));

        return value;
    }
}
