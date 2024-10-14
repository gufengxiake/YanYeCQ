package nckd.yanye.tmc.plugin.form;

import java.util.EventObject;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.entity.property.BasedataProp;
import kd.bos.form.field.BasedataEdit;

/**
 * @author husheng
 * @date 2024-10-12 15:11
 * @description  银行提款处理(nckd_cfm_loanbill_b_l_ext)
 */
public class CfmLoanbillBIFormPlugin extends AbstractBillPlugIn {
    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);

        // 设置占用授信字段必填 页面上的必填和数据校验的必填
        BasedataEdit creditlimit = this.getControl("creditlimit");
        creditlimit.setMustInput(true);
        BasedataProp creditlimit2 = (BasedataProp) this.getModel().getDataEntityType().getProperty("creditlimit");
        creditlimit2.setMustInput(true);
    }
}
