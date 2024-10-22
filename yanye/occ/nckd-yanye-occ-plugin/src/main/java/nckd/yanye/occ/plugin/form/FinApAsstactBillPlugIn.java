package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;
import nckd.yanye.occ.plugin.operate.SalOutSaveOperationPlugIn;

import java.util.EventObject;

/**
 * 财务应付单
 * 表单标识：nckd_ap_finapbill_ext
 * @author zhangzhilong
 * @since 2024-9-25
 */
public class FinApAsstactBillPlugIn extends AbstractBillPlugIn implements Plugin {


    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        DynamicObject deliverChannel = (DynamicObject) this.getModel().getValue("nckd_deliverchannel");
        if (deliverChannel == null){
            return;
        }
        Object supplierId = new SalOutSaveOperationPlugIn().getSupplierId( deliverChannel.getLong("id"));
        DynamicObject bd_supplier = BusinessDataServiceHelper.loadSingle("bd_supplier",
                new QFilter[]{new QFilter("id", QCP.equals, supplierId)});
        this.getModel().setValue("asstact",bd_supplier);
    }
}