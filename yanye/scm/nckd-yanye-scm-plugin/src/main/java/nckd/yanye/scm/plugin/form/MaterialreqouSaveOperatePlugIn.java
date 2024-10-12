package nckd.yanye.scm.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.HashSet;
import java.util.Map;

/**
 * 领用出库单，库存事务为研发领用时，领料出库单反写领料申请单对应的领用部门级库存事务、研发项目
 * 表单标识：nckd_im_materialreqou_ext》
 * author:黄文波 2024-10-12
 */
    public class MaterialreqouSaveOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("invscheme");//库存事务
        e.getFieldKeys().add("bizdept");//需求部门
        e.getFieldKeys().add("nckd_yfxm");//研发项目
    }
    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        String targetEntityNumber = this.billEntityType.getName();
        if(deliverRecords != null){
            for(DynamicObject dataObject:deliverRecords){
//                Object signdate=dataObject.getDate("nckd_signdate");

                DynamicObject invscheme=dataObject.getDynamicObject("invscheme");
                DynamicObject bizdept=dataObject.getDynamicObject("bizdept");
                DynamicObject nckdyfxm=dataObject.getDynamicObject("nckd_yfxm");

                //判断库存事务是否为JY-029（研发领用出库）
                String invschemenum = invscheme.getString("number");

                if(invschemenum=="JY-029") {
                    if (invscheme != null) {
                        Object billId = dataObject.getPkValue();
                        Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, new Long[]{(Long) billId});
                        if (sourceBillIds.containsKey("im_materialreqbill")) {
                            HashSet<Long> botpbill1_Ids = sourceBillIds.get("im_materialreqbill");
                            for (long id : botpbill1_Ids) {
                                //获取上游单据数据包
                                DynamicObject date = BusinessDataServiceHelper.loadSingle(id, "im_materialreqbill");
                                date.set("nckd_basedatafield5", invscheme);
                                SaveServiceHelper.update(new DynamicObject[]{date});
                            }
                        }
                    }


                    if (bizdept != null) {
                        Object billId = dataObject.getPkValue();
                        Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, new Long[]{(Long) billId});
                        if (sourceBillIds.containsKey("im_materialreqbill")) {
                            HashSet<Long> botpbill1_Ids = sourceBillIds.get("im_materialreqbill");
                            for (long id : botpbill1_Ids) {
                                //获取上游单据数据包
                                DynamicObject date = BusinessDataServiceHelper.loadSingle(id, "im_materialreqbill");
                                date.set("applydept", bizdept);
                                SaveServiceHelper.update(new DynamicObject[]{date});
                            }
                        }
                    }

                    if (nckdyfxm != null) {
                        Object billId = dataObject.getPkValue();
                        Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, new Long[]{(Long) billId});
                        if (sourceBillIds.containsKey("im_materialreqbill")) {
                            HashSet<Long> botpbill1_Ids = sourceBillIds.get("im_materialreqbill");
                            for (long id : botpbill1_Ids) {
                                //获取上游单据数据包
                                DynamicObject date = BusinessDataServiceHelper.loadSingle(id, "im_materialreqbill");
                                date.set("nckd_basedatafield3", nckdyfxm);
                                SaveServiceHelper.update(new DynamicObject[]{date});
                            }
                        }
                    }
                }
            }
        }
    }
}
