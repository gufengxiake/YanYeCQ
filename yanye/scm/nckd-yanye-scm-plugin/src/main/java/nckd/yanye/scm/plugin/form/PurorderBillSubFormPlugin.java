package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.exception.KDBizException;
import kd.bos.form.control.events.BeforeItemClickEvent;

import java.math.BigDecimal;

/**
 * 采购订单：提交校验价税合计
 * 单据编码：nckd_pm_purorderbill_ext
 *
 * @author ：liuxiao
 * @since ：Created in 09:58 2024/8/30
 */
public class PurorderBillSubFormPlugin extends AbstractBillPlugIn {
    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        if ("bar_submit".equals(evt.getItemKey()) || "bar_submitandnew".equals(evt.getItemKey())) {
            BigDecimal totalprice = (BigDecimal) this.getModel().getValue("nckd_totalprice");
            BigDecimal totalallamount = (BigDecimal) this.getModel().getValue("totalallamount");
            if (totalprice == null) {
                return;
            }
            if (totalprice.compareTo(totalallamount) != 0) {
                throw new KDBizException("价税合计金额与招采成交价税合计金额不符");
            }
        }
    }
}
