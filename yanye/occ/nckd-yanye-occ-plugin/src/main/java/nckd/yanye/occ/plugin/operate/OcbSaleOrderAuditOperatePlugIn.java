package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.Date;

/**
 * 要货订单审核后,更新渠道最近交易日期
 * 表单标识：nckd_ocbsoc_saleorder_ext
 * author:吴国强 2024-09-02
 */

public class OcbSaleOrderAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("orderchannelid");//订货渠道
    }
    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if(deliverRecords != null){
            for(DynamicObject dataEntity:deliverRecords){
                DynamicObject orderchannelid=dataEntity.getDynamicObject("orderchannelid");
                orderchannelid.set("nckd_lastdate", new Date());
                SaveServiceHelper.update(orderchannelid);
            }

        }

    }
}
