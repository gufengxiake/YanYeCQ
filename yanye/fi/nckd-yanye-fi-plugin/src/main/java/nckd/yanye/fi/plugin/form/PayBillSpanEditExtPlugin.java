package nckd.yanye.fi.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.fi.cas.consts.BillTypeConstants;

import java.util.EventObject;
import java.util.Objects;

/**
 * Module           :资金云-资金调度-调拨处理
 * Description      :将付款方的切换功能打开
 *
 * @author : zhujintao
 * @date : 2024/9/25
 */
public class PayBillSpanEditExtPlugin extends AbstractBillPlugIn {
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        this.setEntryVisible();
    }

    private void setEntryVisible() {
        DynamicObject billType = (DynamicObject)this.getModel().getValue("billtype");
        Object sourceBillType = this.getModel().getValue("sourcebilltype");
        if (billType != null && BillTypeConstants.PAYBILL_SPAN.equals(billType.getPkValue())) {
            this.getView().setVisible(true, new String[]{"advconap"});
            if (Objects.equals(sourceBillType, "cas_transferapply")) {
                this.getView().setEnable(true, new String[]{"settletype", "isdiffcur", "flex_payinfo", "flex_recinfo", "actpayamt", "currency", "dpcurrency", "dpamt"});
                this.getView().setVisible(true, new String[]{"changepayer"});
            }
        }

    }
}
