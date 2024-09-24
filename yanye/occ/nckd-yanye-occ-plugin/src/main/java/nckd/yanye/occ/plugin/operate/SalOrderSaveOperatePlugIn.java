package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.HashSet;
import java.util.Map;


/**
 * 销售订单保存服务插件 销售合同号或者运输合同号补录的情况，携带至下游单据
 * 表单标识：nckd_sm_salorder_ext
 * author:吴国强 2024-07-16
 */
public class SalOrderSaveOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_trancontractno");//运输合同
        e.getFieldKeys().add("nckd_salecontractno");//销售合同

    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        String targetEntityNumber = this.billEntityType.getName();

        for (DynamicObject dataEntity : deliverRecords) {
            //当前单据Id
            Object pkId = dataEntity.getPkValue();
            String trancontractNo = dataEntity.getString("nckd_trancontractno");
            String salecontractNo = dataEntity.getString("nckd_salecontractno");
            if (!trancontractNo.trim().equals("") && !salecontractNo.trim().equals("")) {
                Map<String, HashSet<Long>> targetBills = BFTrackerServiceHelper.findTargetBills(targetEntityNumber, new Long[]{(Long) pkId});
                //发货通知单
                if (targetBills.containsKey("sm_delivernotice")) {
                    HashSet<Long> targetBillIds = targetBills.get("sm_delivernotice");
                    this.updateContractNo("sm_delivernotice", targetBillIds, trancontractNo, salecontractNo);
                }
                //电子磅单
                if (targetBills.containsKey("nckd_eleweighing")) {
                    HashSet<Long> targetBillIds = targetBills.get("nckd_eleweighing");
                    this.updateContractNo("nckd_eleweighing", targetBillIds, trancontractNo, salecontractNo);
                }
                //销售出库单
                if (targetBills.containsKey("im_saloutbill")) {
                    HashSet<Long> targetBillIds = targetBills.get("im_saloutbill");
                    this.updateContractNo("im_saloutbill", targetBillIds, trancontractNo, salecontractNo);
                }
                //签收单
                if (targetBills.containsKey("nckd_signaturebill")) {
                    HashSet<Long> targetBillIds = targetBills.get("nckd_signaturebill");
                    this.updateContractNo("nckd_signaturebill", targetBillIds, trancontractNo, null);
                }
                //暂估应付单
                if (targetBills.containsKey("ap_busbill")) {
                    HashSet<Long> targetBillIds = targetBills.get("ap_busbill");
                    this.updateApContractNo("ap_busbill", targetBillIds,"entry", trancontractNo );
                }
                //财务应付单
                if (targetBills.containsKey("ap_finapbill")) {
                    HashSet<Long> targetBillIds = targetBills.get("ap_finapbill");
                    this.updateApContractNo("ap_finapbill", targetBillIds,"detailentry", trancontractNo );
                }

            }
        }

    }

    /*
    更新下游单据的销售合同和运输合同号
     */
    private void updateContractNo(String targetBill, HashSet<Long> targetBillIds, String trancontractNo, String salecontractNo) {

        DynamicObject[] targetDynamic = (DynamicObject[]) BusinessDataServiceHelper.load(targetBillIds.toArray(), BusinessDataServiceHelper.newDynamicObject(targetBill).getDataEntityType());
        if (targetDynamic != null) {
            for (DynamicObject targetData : targetDynamic) {
                if (salecontractNo != null) {
                    targetData.set("nckd_salecontractno", salecontractNo);
                }
                targetData.set("nckd_trancontractno", trancontractNo);
            }
            SaveServiceHelper.update(targetDynamic);
        }
    }

    //更新暂估应付和财务应付
    private void updateApContractNo(String targetBill, HashSet<Long> targetBillIds,String entryName, String trancontractNo) {

        DynamicObject[] targetDynamic = (DynamicObject[]) BusinessDataServiceHelper.load(targetBillIds.toArray(), BusinessDataServiceHelper.newDynamicObject(targetBill).getDataEntityType());
        if (targetDynamic != null) {
            for (DynamicObject targetData : targetDynamic) {
                DynamicObjectCollection entry=targetData.getDynamicObjectCollection(entryName);
                for(DynamicObject entryRow:entry){
                    entryRow.set("nckd_carriagenumber",trancontractNo);
                }
            }
            SaveServiceHelper.update(targetDynamic);
        }
    }

}
