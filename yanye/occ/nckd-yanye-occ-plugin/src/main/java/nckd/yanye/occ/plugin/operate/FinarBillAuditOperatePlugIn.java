package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;

/**
 * 财务应收单审核刷新结算与锁定金额
 * 表单标识：nckd_ar_finarbill_ext
 * author:吴国强 2024-09-11
 */

public class FinarBillAuditOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("e_corebilltype");//核心单据类型
        e.getFieldKeys().add("corebillentryid");//核心单据行ID
        e.getFieldKeys().add("e_recamount");//应收金额
        e.getFieldKeys().add("e_reclocalamt");//应收金额本位币
        e.getFieldKeys().add("e_lockedamt");//已锁定金额
        e.getFieldKeys().add("e_unlockamt");//未锁定金额
        e.getFieldKeys().add("e_settledamt");//已结算金额
        e.getFieldKeys().add("e_settledlocalamt");//已结算金额本位币
        e.getFieldKeys().add("e_unsettleamt");//未结算金额
        e.getFieldKeys().add("e_unsettlelocalamt");//未结算金额本位币
        //记录扣减数量，反审核时还原数值
        e.getFieldKeys().add("nckd_settlekamt");//结算金额扣减
        e.getFieldKeys().add("nckd_settleklocalamt");//结算金额扣减本位币
        e.getFieldKeys().add("nckd_lockekamt");//锁定金额扣减
        e.getFieldKeys().add("unsettleamount");//未结算金额
        e.getFieldKeys().add("unsettlelocalamt");//未结算金额本位币
        e.getFieldKeys().add("settleamount");//已结算金额
        e.getFieldKeys().add("settlelocalamt");//已结算金额本位币

    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if (deliverRecords != null) {
            for (DynamicObject dataObject : deliverRecords) {
                DynamicObjectCollection entry = dataObject.getDynamicObjectCollection("entry");
                if (entry != null) {
                    BigDecimal unsettleamount = BigDecimal.ZERO;//未结算金额
                    BigDecimal unsettlelocalamt = BigDecimal.ZERO;//未结算金额本位币
                    BigDecimal settleamount = BigDecimal.ZERO;//已结算金额
                    BigDecimal settlelocalamt = BigDecimal.ZERO;//已结算金额本位币
                    for (DynamicObject entryRow : entry) {
                        //核心单据类型
                        String corebilltype = entryRow.getString("e_corebilltype");
                        if ("ocbsoc_saleorder".equals(corebilltype)) {
                            //核心单据行Id
                            Object corebillentryid = entryRow.get("corebillentryid");
                            //财务应收未结算金额
                            BigDecimal finunsettleamt = entryRow.getBigDecimal("e_unsettleamt");
                            //财务应收未结算金额本位币
                            BigDecimal finunsettlelocalamt = entryRow.getBigDecimal("e_unsettlelocalamt");
                            //财务应付已结算金额
                            BigDecimal finsettledamt = entryRow.getBigDecimal("e_settledamt");
                            //财务应付已结算金额本位币
                            BigDecimal finsettledlocalamt = entryRow.getBigDecimal("e_settledlocalamt");
                            //财务应付已锁定金额
                            BigDecimal finlockedamt = entryRow.getBigDecimal("e_lockedamt");
                            //财务应付未锁定金额
                            BigDecimal finunlockamt = entryRow.getBigDecimal("e_unlockamt");
                            if (corebillentryid != null) {
                                //根据核心单据行Id 查找收款处理单
                                // 构造QFilter
                                QFilter qFilter = new QFilter("sourcebilltype", QCP.equals, "ocbsoc_saleorder")
                                        .and("entry.e_sourcebillentryid", QCP.equals, corebillentryid);
                                DynamicObjectCollection collections = QueryServiceHelper.query("cas_recbill",
                                        "id", qFilter.toArray(), "");
                                if (!collections.isEmpty()) {
                                    BigDecimal settleKamt = BigDecimal.ZERO;
                                    BigDecimal settleLocalKamt = BigDecimal.ZERO;
                                    BigDecimal lockamt = BigDecimal.ZERO;
                                    DynamicObject recbillItem = collections.get(0);
                                    String recbillId = recbillItem.getString("id");
                                    DynamicObject recbill = BusinessDataServiceHelper.loadSingle(recbillId, "cas_recbill");
                                    DynamicObjectCollection recEntry = recbill.getDynamicObjectCollection("entry");
                                    BigDecimal finunsettleamount = BigDecimal.ZERO;
                                    BigDecimal finunsettleamountbase = BigDecimal.ZERO;
                                    BigDecimal finsettleamount = BigDecimal.ZERO;
                                    BigDecimal finsettleamountbase = BigDecimal.ZERO;
                                    for (DynamicObject recRow : recEntry) {
                                        Object recCorEntryId = recRow.get("e_corebillentryid");
                                        if (corebillentryid.equals(recCorEntryId)) {
                                            BigDecimal recunsettledamt = recRow.getBigDecimal("e_unsettledamt");//收款处理未结算金额
                                            BigDecimal recunsettledlocalamt = recRow.getBigDecimal("e_unsettledlocalamt");//收款处理未结算金额本位币
                                            BigDecimal recsettledamt = recRow.getBigDecimal("e_settledamt");//收款处理已结算金额
                                            BigDecimal recsettledlocalamt = recRow.getBigDecimal("e_settledlocalamt");//收款处理已结算金额本位币
                                            //记录未结算金额两者中的较小数
                                            if (finunsettleamt.compareTo(recunsettledamt) > 0) {
                                                settleKamt = recunsettledamt;
                                                settleLocalKamt = recunsettledlocalamt;
                                            } else {
                                                settleKamt = finunsettleamt;
                                                settleLocalKamt = finunsettlelocalamt;
                                            }
                                            BigDecimal recunlockamt = recRow.getBigDecimal("e_unlockamt");//收款处理未锁定金额
                                            BigDecimal reclockamt = recRow.getBigDecimal("e_lockamt");//收款处理已锁定金额
                                            if (finunlockamt.compareTo(recunlockamt) > 0) {
                                                lockamt = recunlockamt;
                                            } else {
                                                lockamt = finunlockamt;
                                            }

                                            if(settleKamt.compareTo(BigDecimal.ZERO)>0){
                                                recRow.set("nckd_settlekamt", settleKamt);//财务应收结算金额扣减
                                            }
                                            if(settleLocalKamt.compareTo(BigDecimal.ZERO)>0){
                                                recRow.set("nckd_settleklocalamt", settleLocalKamt);//财务应收结算金额扣减本位币
                                            }
                                            if(lockamt.compareTo(BigDecimal.ZERO)>0){
                                                recRow.set("nckd_lockekamt", lockamt);//财务应收锁定金额扣减
                                            }
                                            recRow.set("e_unsettledamt", recunsettledamt.subtract(settleKamt));//收款处理未结算金额
                                            recRow.set("e_unsettledlocalamt", recunsettledlocalamt.subtract(settleLocalKamt));//收款处理未结算金额本位币
                                            recRow.set("e_settledamt", recsettledamt.add(settleKamt));//收款处理已结算金额
                                            recRow.set("e_settledlocalamt", recsettledlocalamt.add(settleLocalKamt));//收款处理已结算金额本位币
                                            recRow.set("e_unlockamt", recunlockamt.subtract(lockamt));//收款处理未锁定金额
                                            recRow.set("e_lockamt", reclockamt.add(lockamt));//收款处理已锁定金额
                                        }
                                        finunsettleamount = finunsettleamount.add(recRow.getBigDecimal("e_unsettledamt"));//未结算金额
                                        finunsettleamountbase = finunsettleamountbase.add(recRow.getBigDecimal("e_unsettledlocalamt"));//未结算金额本位币
                                        finsettleamount = finsettleamount.add(recRow.getBigDecimal("e_settledamt"));//已结算金额
                                        finsettleamountbase = finsettleamountbase.add(recRow.getBigDecimal("e_settledlocalamt"));//已结算金额本位币
                                    }
                                    //收款处理表头字段
                                    recbill.set("unsettleamount", finunsettleamount);
                                    recbill.set("unsettleamountbase", finunsettleamountbase);
                                    recbill.set("settleamount", finsettleamount);
                                    recbill.set("settleamountbase", finsettleamountbase);
                                    SaveServiceHelper.update(recbill);

                                    if(settleKamt.compareTo(BigDecimal.ZERO)>0){
                                        entryRow.set("nckd_settlekamt", settleKamt);//财务应收结算金额扣减
                                    }
                                    if(settleLocalKamt.compareTo(BigDecimal.ZERO)>0){
                                        entryRow.set("nckd_settleklocalamt", settleLocalKamt);//财务应收结算金额扣减本位币
                                    }
                                    entryRow.set("e_unsettleamt", finunsettleamt.subtract(settleKamt));//财务应收未结算金额
                                    entryRow.set("e_unsettlelocalamt", finunsettlelocalamt.subtract(settleLocalKamt));//财务应收未结算金额本位币
                                    entryRow.set("e_settledamt", finsettledamt.add(settleKamt));//财务应收已结算金额
                                    entryRow.set("e_settledlocalamt", finsettledlocalamt.add(settleLocalKamt));//财务应收已结算金额本位币
                                    if(lockamt.compareTo(BigDecimal.ZERO)>0){
                                        entryRow.set("nckd_lockekamt", lockamt);//财务应收锁定金额扣减
                                    }
                                    entryRow.set("e_unlockamt", finunlockamt.subtract(lockamt));//财务应收未锁定金额
                                    entryRow.set("e_lockedamt", finlockedamt.add(lockamt));//财务应收已锁定金额
                                }
                            }
                        }
                        unsettleamount = unsettleamount.add(entryRow.getBigDecimal("e_unsettleamt"));//未结算金额
                        unsettlelocalamt = unsettlelocalamt.add(entryRow.getBigDecimal("e_unsettlelocalamt"));//未结算金额本位币
                        settleamount = settleamount.add(entryRow.getBigDecimal("e_settledamt"));//已结算金额
                        settlelocalamt = settlelocalamt.add(entryRow.getBigDecimal("e_settledlocalamt"));//已结算金额本位币
                    }
                    if (unsettleamount.add(unsettlelocalamt).add(settleamount).add(settlelocalamt).compareTo(BigDecimal.ZERO) > 0) {
                        dataObject.set("unsettleamount", unsettleamount);
                        dataObject.set("unsettlelocalamt", unsettlelocalamt);
                        dataObject.set("settleamount", settleamount);
                        dataObject.set("settlelocalamt", settlelocalamt);
                    }
                    SaveServiceHelper.update(dataObject);
                }
            }
        }
    }
}
