package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;

/**
 * Module           :资金云-收款单-反审核
 * Description      :反审核收款单，那要货订单上的金额也得还原
 *
 * @author : zhujintao
 * @date : 2024/8/27
 */
public class ReceivingBillUnAuditOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] entities = e.getDataEntities();
        for (DynamicObject bill : entities) {
            DynamicObjectCollection casRecbillCollection = bill.getDynamicObjectCollection("entry");
            for (DynamicObject casRecbillEntry : casRecbillCollection) {
                if ("ocbsoc_saleorder".equals(casRecbillEntry.getString("e_corebilltype"))) {
                    BigDecimal eReceivableamt = casRecbillEntry.getBigDecimal("e_receivableamt");
                    String eCorebillno = casRecbillEntry.getString("e_corebillno");
                    QFilter qFilter = new QFilter("billno", QCP.equals, eCorebillno);
                    //应收金额 sumreceivableamount,已收金额 sumrecamount,待收金额 sumunrecamount
                    DynamicObject loadSingleRecbill = BusinessDataServiceHelper.loadSingle("ocbsoc_saleorder", "id,sumreceivableamount,sumrecamount,sumunrecamount,paystatus", qFilter.toArray());
                    loadSingleRecbill.set("sumrecamount", loadSingleRecbill.getBigDecimal("sumrecamount").subtract(eReceivableamt));
                    loadSingleRecbill.set("sumunrecamount", loadSingleRecbill.getBigDecimal("sumreceivableamount").subtract(loadSingleRecbill.getBigDecimal("sumrecamount")));
                    if (loadSingleRecbill.getBigDecimal("sumrecamount").compareTo(new BigDecimal(0)) == 0) {
                        loadSingleRecbill.set("paystatus", "A");
                    } else {
                        loadSingleRecbill.set("paystatus", "B");
                    }
                    SaveServiceHelper.update(loadSingleRecbill);
                }
            }
        }
    }
}
