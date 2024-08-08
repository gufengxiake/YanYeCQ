package nckd.yanye.scm.plugin.operate;

import java.util.EventObject;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.control.events.ItemClickEvent;
import kd.imc.sim.formplugin.issuing.helper.IssueInvoiceControlHelper;

/**
 * @author husheng
 * @date 2024-08-07 11:36
 * @description
 */
public class TestOpPlugin extends AbstractBillPlugIn {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("toolbarap");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        if (evt.getItemKey().equals("nckd_baritemap")) {
            DynamicObject dataEntity = this.getModel().getDataEntity();
            IssueInvoiceControlHelper.issueInvoice(new DynamicObject[]{dataEntity},1,false,false);
        }
    }
}
