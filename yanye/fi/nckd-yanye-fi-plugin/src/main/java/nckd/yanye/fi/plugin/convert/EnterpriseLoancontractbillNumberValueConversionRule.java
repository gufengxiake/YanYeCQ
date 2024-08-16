package nckd.yanye.fi.plugin.convert;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.isc.iscb.platform.core.connector.ConnectionWrapper;
import kd.isc.iscb.platform.core.vc.JavaValueConversionRule;
import kd.tmc.fbp.common.enums.CreditorTypeEnum;
import kd.tmc.gm.common.enums.GuaranteeTypeEnum;

/**
 * @author husheng
 * @date 2024-08-13 9:18
 * @description 企业借款合同-》合同台账 合同签约乙方编码 集成值转换
 */
public class EnterpriseLoancontractbillNumberValueConversionRule implements JavaValueConversionRule {
    @Override
    public Object cast(String s, ConnectionWrapper connectionWrapper, ConnectionWrapper connectionWrapper1) {
        // 企业借款合同
        DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(s, "cfm_loancontractbill");
        // 债权人类型
        String creditortype = loadSingle.getString("creditortype");
        // 对应类型的界面
        String formId = CreditorTypeEnum.getFormIdByValue(creditortype);
        DynamicObject object = BusinessDataServiceHelper.loadSingle(loadSingle.get("creditor"), formId);
        return object.getString("number");
    }
}
