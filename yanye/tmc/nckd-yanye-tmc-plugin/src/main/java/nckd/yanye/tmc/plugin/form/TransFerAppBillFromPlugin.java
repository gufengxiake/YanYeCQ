package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.EventObject;

/**
 * 资金-调拨申请表单插件
 * 表单标识：nckd_cas_transferappl_ext
 * author：xiaoxiaopeng
 * date：2024-09-29
 */

public class TransFerAppBillFromPlugin extends AbstractBillPlugIn {

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        IDataModel model = this.getModel();
        //收款人带出申请人组织，付款组织默认集团本部，结算方式默认“电汇”；
        DynamicObject org = BusinessDataServiceHelper.loadSingle("1956460831289919488", "bos_org");
        model.setValue("e_payorg",org);
        model.setValue("e_payee",model.getValue("applyorg"));
    }
}
