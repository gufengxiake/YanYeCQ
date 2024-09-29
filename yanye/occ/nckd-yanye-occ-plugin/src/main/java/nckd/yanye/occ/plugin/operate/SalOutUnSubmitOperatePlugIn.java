package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.*;

/**
 * 销售出库单撤销修改要货订单单据状态，签收状态
 * 表单标识：nckd_im_saloutbill_ext
 * author:wgq
 * date:2024/09/26
 */
public class SalOutUnSubmitOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        // 获取当前单据的主实体编码、单据内码
        String targetEntityNumber = this.billEntityType.getName();
        Set<Object> billIds = new HashSet<>();
        for (DynamicObject dataEntity : e.getDataEntities()) {
            billIds.add(dataEntity.getPkValue());
        }
        // 调用平台的服务，获取所有源单及其内码
        Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, billIds.toArray(new Long[0]));
        // 从所有源单中寻找需要的demo_botpbill1
        HashSet<Long> botpbill1_Ids = new HashSet<>();
        String sourceBill = "ocbsoc_saleorder";//要货订单
        if (sourceBillIds.containsKey(sourceBill)) {
            botpbill1_Ids = sourceBillIds.get(sourceBill);
        }
        if(botpbill1_Ids.isEmpty()){
            return;
        }
        List<DynamicObject> saveValue = new ArrayList(16);
        for (Object pk : botpbill1_Ids) {
            //根据Id获取要货订单单实体
            DynamicObject saleBill = BusinessDataServiceHelper.loadSingle(pk, sourceBill);
            saleBill.set("billstatus","C");//订单状态已审核
            saleBill.set("signstatus","A");//签收状态未发货
            saveValue.add(saleBill);
        }
        DynamicObject[] saveValues = saveValue.toArray(new DynamicObject[saveValue.size()]);
        SaveServiceHelper.save(saveValues);

    }
}
