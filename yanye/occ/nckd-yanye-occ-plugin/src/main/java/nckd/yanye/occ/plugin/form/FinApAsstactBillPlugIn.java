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
 * 单据界面插件
 */
public class FinApAsstactBillPlugIn extends AbstractBillPlugIn implements Plugin {
//    @Override
//    public void afterCreateNewData(EventObject e) {
//        super.afterCreateNewData(e);
////        Object nckdDeliverchannel = this.getModel().getValue("nckd_deliverchannel");
////        Object supplierId = new SalOutSaveOperationPlugIn().getSupplierId((Long)nckdDeliverchannel );
//        DynamicObject bd_supplier = BusinessDataServiceHelper.loadSingle("bd_supplier",
//                new QFilter[]{new QFilter("id", QCP.equals, 1110082709432067072L)});
//        this.getModel().setValue("asstact",bd_supplier);
////        this.getModel().setValue("remark",bd_supplier);
//    }

    @Override
    public void afterLoadData(EventObject e) {
        super.afterLoadData(e);
//        DynamicObject bd_supplier = BusinessDataServiceHelper.loadSingle("bd_supplier",
//                new QFilter[]{new QFilter("id", QCP.equals, 1110082709432067072L)});
//        this.getModel().setValue("asstact",bd_supplier);
//        this.getModel().setValue("remark",supplierId);
    }

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