package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.ComboProp;
import kd.bos.entity.property.TextProp;
import kd.bos.form.field.ComboEdit;
import kd.bos.form.field.TextEdit;
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
        DynamicObject applyorg = (DynamicObject) this.getModel().getValue("applyorg");
        if (applyorg.getPkValue().equals(org.getPkValue())) {
            model.setValue("e_payorg", null);
            model.setValue("e_payee", model.getValue("applyorg"));
        }else {
            model.setValue("e_payorg", org);
            model.setValue("e_payee", model.getValue("applyorg"));
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        if ("transfertype".equals(name)) {
            String transfertype = this.getModel().getValue("transfertype").toString();
            TextEdit payeraccbanknumEdit = this.getControl("e_payeraccbanknum");
            TextProp payeraccbanknumProp = (TextProp) payeraccbanknumEdit.getProperty();
            ComboEdit paymentchannelEdit = this.getControl("e_paymentchannel");
            ComboProp paymentchannelProp = (ComboProp) paymentchannelEdit.getProperty();
            if ("B".equals(transfertype)) {
                payeraccbanknumEdit.setMustInput(true);
                payeraccbanknumProp.setMustInput(true);
                paymentchannelEdit.setMustInput(true);
                paymentchannelProp.setMustInput(true);
            } else {
                payeraccbanknumEdit.setMustInput(false);
                payeraccbanknumProp.setMustInput(false);
                paymentchannelEdit.setMustInput(false);
                paymentchannelProp.setMustInput(false);

                //收款人带出申请人组织，付款组织默认集团本部，结算方式默认“电汇”；
                DynamicObject org = BusinessDataServiceHelper.loadSingle("1956460831289919488", "bos_org");
                DynamicObject applyorg = (DynamicObject) this.getModel().getValue("applyorg");
                if (applyorg.getPkValue().equals(org.getPkValue())) {
                    this.getModel().setValue("e_payorg", null);
                    this.getModel().setValue("e_payee", this.getModel().getValue("applyorg"));
                }else {
                    this.getModel().setValue("e_payorg", org);
                    this.getModel().setValue("e_payee", this.getModel().getValue("applyorg"));
                }
            }
        }
    }
}
