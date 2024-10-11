package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.basedata.BaseDataResponse;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 渠道申请认证后分配客户
 * 表单标识：nckd_im_transdirbill_ext
 * author:吴国强 2024-08-12
 */
public class CustomerAssignOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("saleorg");//销售组织

    }
    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if (deliverRecords != null) {
            for (DynamicObject dataObject : deliverRecords) {
                DynamicObject saleorg=dataObject.getDynamicObject("saleorg");
                String targetEntityNumber = this.billEntityType.getName();
                Set<Object> billIds = new HashSet<>();
                billIds.add(dataObject.getPkValue());
                // 调用平台的服务，获取所有源单及其内码
                Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findTargetBills(targetEntityNumber, billIds.toArray(new Long[0]));
                HashSet<Long> botpbill1_Ids = new HashSet<>();
                String botpbill1_EntityNumber = "bd_customer";//客户
                if (sourceBillIds.containsKey(botpbill1_EntityNumber)) {

                    botpbill1_Ids = sourceBillIds.get(botpbill1_EntityNumber);
                }
                if(!botpbill1_Ids.isEmpty()){
                    for(Long customerId:botpbill1_Ids){
                        Set<Long> dataIdsTemp = new HashSet<>();
                        dataIdsTemp.add(customerId);
                        Set<Long> orgIds = new HashSet<>();
                        orgIds.add(saleorg.getLong("id"));
                        BaseDataResponse assign = BaseDataServiceHelper.assign("bd_customer",100000L , "basedata", dataIdsTemp, orgIds);
                        if(assign.isSuccess()){

                        }
                    }

                }



            }
        }

    }
}
