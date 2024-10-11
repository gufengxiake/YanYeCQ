package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;

public class ArUnSettleRecordOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("settletype");//结算类型
        e.getFieldKeys().add("corebillno");//核心单据号
        e.getFieldKeys().add("totalsettleamt");//本次结算金额
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] settleRecords = e.getDataEntities();
        if (settleRecords != null) {
            for (DynamicObject dataObject : settleRecords) {
                String settletype = dataObject.getString("settletype");//结算类型
                //手工结算
                if ("manual".equalsIgnoreCase(settletype)) {

                    //本次结算金额
                    BigDecimal totalsettleamt = dataObject.getBigDecimal("totalsettleamt");
                    //核心单据号
                    String corebillno = dataObject.getString("corebillno");
                    //查找当前核心单据号对应得财务应收记录
                    //QFilter settleFilter = new QFilter("corebillno")

                    //查找要货订单 单据编号 收款状态
                    QFilter filter = new QFilter("billno", QCP.equals, corebillno)
                            .and("paystatus", QCP.not_equals, "C");
                    DynamicObjectCollection ocbsocOrder = QueryServiceHelper.query("ocbsoc_saleorder",
                            "id", filter.toArray(), "");
                    if (!ocbsocOrder.isEmpty()) {
                        DynamicObject orderDataObject = ocbsocOrder.get(0);
                        Object pkId = orderDataObject.get("id");
                        DynamicObject updateData = BusinessDataServiceHelper.loadSingle(pkId, "ocbsoc_saleorder");
                        //已收金额
                        BigDecimal sumrecamount = updateData.getBigDecimal("sumrecamount");
                        //应收金额
                        BigDecimal sumreceivableamount = updateData.getBigDecimal("sumreceivableamount");

                        BigDecimal subtarct = sumrecamount.subtract(totalsettleamt).compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : sumrecamount.subtract(totalsettleamt);
                        if (subtarct.compareTo(BigDecimal.ZERO) > 0) {
                            updateData.set("paystatus", "B");
                            updateData.set("sumrecamount", subtarct);//已收金额
                            updateData.set("sumunrecamount", sumreceivableamount.subtract(subtarct));//代收金额
                        } else {
                            updateData.set("paystatus", "A");
                            updateData.set("sumrecamount", subtarct);
                            updateData.set("sumunrecamount", sumreceivableamount.subtract(subtarct));
                        }
                        SaveServiceHelper.update(updateData);
                    }
                }
            }
        }
    }
}
