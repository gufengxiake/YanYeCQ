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

public class ArSettleRecordAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("settletype");//结算类型
        e.getFieldKeys().add("corebillno");//核心单据号
        e.getFieldKeys().add("totalsettleamt");//本次结算金额
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e){
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] settleRecords = e.getDataEntities();
        if (settleRecords != null) {
            for (DynamicObject dataObject : settleRecords) {
                String settletype=dataObject.getString("settletype");//结算类型
                //手工结算
                if("manual".equalsIgnoreCase(settletype)){
                    //本次结算金额
                    BigDecimal totalsettleamt=dataObject.getBigDecimal("totalsettleamt");
                    //核心单据号
                    String corebillno=dataObject.getString("corebillno");
                    //查找要货订单 单据编号 收款状态
                    QFilter filter=new QFilter("billno", QCP.equals,corebillno)
                            .and("paystatus",QCP.not_equals,"C");
                    DynamicObjectCollection ocbsocOrder= QueryServiceHelper.query("ocbsoc_saleorder",
                            "id", filter.toArray(), "");
                    if(!ocbsocOrder.isEmpty()){
                        DynamicObject orderDataObject=ocbsocOrder.get(0);
                        Object pkId=orderDataObject.get("id");
                        DynamicObject updateData= BusinessDataServiceHelper.loadSingle(pkId,"ocbsoc_saleorder");
                        //已收金额
                        BigDecimal sumrecamount=updateData.getBigDecimal("sumrecamount");
                        //应收金额
                        BigDecimal sumreceivableamount=updateData.getBigDecimal("sumreceivableamount");
                        BigDecimal subtract = sumreceivableamount.subtract(sumrecamount).subtract(totalsettleamt);
                        BigDecimal add = totalsettleamt.add(sumrecamount);
                        if(add.compareTo(sumreceivableamount)>=0){
                            updateData.set("paystatus","C");
                            updateData.set("sumrecamount",add);
                            updateData.set("sumunrecamount", subtract);
                        }else {
                            updateData.set("paystatus","B");
                            updateData.set("sumrecamount",add);
                            updateData.set("sumunrecamount", subtract);
                        }
                        SaveServiceHelper.update(updateData);
                    }
                }
            }
        }
    }
}
