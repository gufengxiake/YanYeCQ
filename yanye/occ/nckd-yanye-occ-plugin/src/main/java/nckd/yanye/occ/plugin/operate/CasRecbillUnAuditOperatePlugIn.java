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
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;

/**
 * 收款处理反审核刷新结算与锁定金额
 * 表单标识：nckd_cas_recbill_ext
 * author:吴国强 2024-09-11
 */

public class CasRecbillUnAuditOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("sourcebilltype");//源单类型
        e.getFieldKeys().add("e_sourcebillentryid");//源单单据行ID
        e.getFieldKeys().add("e_lockamt");//已锁定金额
        e.getFieldKeys().add("e_unlockamt");//未锁定金额
        e.getFieldKeys().add("e_settledamt");//已结算金额
        e.getFieldKeys().add("e_settledlocalamt");//已结算金额本位币
        e.getFieldKeys().add("e_unsettledamt");//未结算金额
        e.getFieldKeys().add("e_unsettledlocalamt");//未结算金额本位币
        //记录扣减数量，反审核时还原数值
        e.getFieldKeys().add("nckd_settlekamt");//结算金额扣减
        e.getFieldKeys().add("nckd_settleklocalamt");//结算金额扣减本位币
        e.getFieldKeys().add("nckd_lockekamt");//锁定金额扣减
        e.getFieldKeys().add("unsettleamount");//未结算金额
        e.getFieldKeys().add("unsettleamountbase");//未结算金额本位币
        e.getFieldKeys().add("settleamount");//已结算金额
        e.getFieldKeys().add("settleamountbase");//已结算金额本位币

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
                    BigDecimal unsettleamountbase = BigDecimal.ZERO;//未结算金额本位币
                    BigDecimal settleamount = BigDecimal.ZERO;//已结算金额
                    BigDecimal settleamountbase = BigDecimal.ZERO;//已结算金额本位币
                    for (DynamicObject entryRow : entry) {
                        //源单单据类型
                        String sourcebilltype = dataObject.getString("sourcebilltype");
                        if ("ocbsoc_saleorder".equals(sourcebilltype)) {
                            //源单单据行Id
                            Object sourcebillentryid = entryRow.get("e_sourcebillentryid");
                            //收款处理未结算金额
                            BigDecimal finunsettleamt = entryRow.getBigDecimal("e_unsettledamt");
                            //收款处理未结算金额本位币
                            BigDecimal finunsettlelocalamt = entryRow.getBigDecimal("e_unsettledlocalamt");
                            //收款处理已结算金额
                            BigDecimal finsettledamt = entryRow.getBigDecimal("e_settledamt");
                            //收款处理已结算金额本位币
                            BigDecimal finsettledlocalamt = entryRow.getBigDecimal("e_settledlocalamt");
                            //收款处理已锁定金额
                            BigDecimal finlockedamt = entryRow.getBigDecimal("e_lockamt");
                            //收款处理未锁定金额
                            BigDecimal finunlockamt = entryRow.getBigDecimal("e_unlockamt");
                            //收款处理结算扣减金额
                            BigDecimal finsettlekamt = entryRow.getBigDecimal("nckd_settlekamt");
                            //收款处理结算扣减金额本位币
                            BigDecimal finsettleklocalamt = entryRow.getBigDecimal("nckd_settleklocalamt");
                            //收款处理锁定扣减金额
                            BigDecimal finlockekamt = entryRow.getBigDecimal("nckd_lockekamt");
                            if (sourcebillentryid != null) {
                                //根据核心单据行Id 查找财务应收单
                                // 构造QFilter
                                QFilter qFilter = new QFilter("entry.e_corebilltype", QCP.equals, "ocbsoc_saleorder")
                                        .and("entry.corebillentryid", QCP.equals, sourcebillentryid);
                                DynamicObjectCollection collections = QueryServiceHelper.query("ar_finarbill",
                                        "id", qFilter.toArray(), "");
                                if (!collections.isEmpty()) {
                                    DynamicObject recbillItem = collections.get(0);
                                    String recbillId = recbillItem.getString("id");
                                    DynamicObject recbill = BusinessDataServiceHelper.loadSingle(recbillId, "ar_finarbill");
                                    DynamicObjectCollection recEntry = recbill.getDynamicObjectCollection("entry");
                                    BigDecimal finunsettleamount = BigDecimal.ZERO;//财务应收未结算金额
                                    BigDecimal finunsettleamountbase = BigDecimal.ZERO;//财务应收未结算金额本位币
                                    BigDecimal finsettleamount = BigDecimal.ZERO;//财务应收已结算金额
                                    BigDecimal finsettleamountbase = BigDecimal.ZERO;//财务应收已结算金额本位币
                                    for (DynamicObject recRow : recEntry) {
                                        Object recCorEntryId = recRow.get("corebillentryid");
                                        if (sourcebillentryid.equals(recCorEntryId)) {
                                            BigDecimal recunsettledamt = recRow.getBigDecimal("e_unsettleamt");//财务应收未结算金额
                                            BigDecimal recunsettledlocalamt = recRow.getBigDecimal("e_unsettlelocalamt");//财务应收未结算金额本位币
                                            BigDecimal recsettledamt = recRow.getBigDecimal("e_settledamt");//财务应收已结算金额
                                            BigDecimal recsettledlocalamt = recRow.getBigDecimal("e_settledlocalamt");//财务应收已结算金额本位币
                                            BigDecimal recunlockamt = recRow.getBigDecimal("e_unlockamt");//财务应收未锁定金额
                                            BigDecimal reclockamt = recRow.getBigDecimal("e_lockedamt");//财务应收已锁定金额
                                            recRow.set("e_unsettleamt", recunsettledamt.add(finsettlekamt));//财务应收未结算金额
                                            recRow.set("e_unsettlelocalamt", recunsettledlocalamt.add(finsettleklocalamt));//财务应收未结算金额本位币
                                            recRow.set("e_settledamt", recsettledamt.subtract(finsettlekamt));//财务应收已结算金额
                                            recRow.set("e_settledlocalamt", recsettledlocalamt.subtract(finsettleklocalamt));//财务应收已结算金额本位币
                                            recRow.set("e_unlockamt", recunlockamt.add(finlockekamt));//财务应收未锁定金额
                                            recRow.set("e_lockedamt", reclockamt.subtract(finlockekamt));//财务应收已锁定金额
                                            recRow.set("nckd_settlekamt", 0);//财务应收结算金额扣减
                                            recRow.set("nckd_settleklocalamt", 0);//财务应收结算金额扣减本位币
                                            recRow.set("nckd_lockekamt", 0);//财务应收锁定金额扣减
                                        }
                                        finunsettleamount = finunsettleamount.add(recRow.getBigDecimal("e_unsettleamt"));//未结算金额
                                        finunsettleamountbase = finunsettleamountbase.add(recRow.getBigDecimal("e_unsettlelocalamt"));//未结算金额本位币
                                        finsettleamount = finsettleamount.add(recRow.getBigDecimal("e_settledamt"));//已结算金额
                                        finsettleamountbase = finsettleamountbase.add(recRow.getBigDecimal("e_settledlocalamt"));//已结算金额本位币
                                    }
                                    //财务应收表头字段
                                    recbill.set("unsettleamount", finunsettleamount);
                                    recbill.set("unsettlelocalamt", finunsettleamountbase);
                                    recbill.set("settleamount", finsettleamount);
                                    recbill.set("settlelocalamt", finsettleamountbase);
                                    SaveServiceHelper.update(recbill);


                                    entryRow.set("nckd_settlekamt", 0);//收款处理结算金额扣减
                                    entryRow.set("nckd_settleklocalamt", 0);//收款处理结算金额扣减本位币
                                    entryRow.set("e_unsettledamt", finunsettleamt.add(finsettlekamt));//收款处理未结算金额
                                    entryRow.set("e_unsettledlocalamt", finunsettlelocalamt.add(finsettleklocalamt));//收款处理未结算金额本位币
                                    entryRow.set("e_settledamt", finsettledamt.subtract(finsettlekamt));//收款处理已结算金额
                                    entryRow.set("e_settledlocalamt", finsettledlocalamt.subtract(finsettleklocalamt));//收款处理已结算金额本位币
                                    entryRow.set("nckd_lockekamt", 0);//收款处理锁定金额扣减
                                    entryRow.set("e_unlockamt", finunlockamt.add(finlockekamt));//收款处理未锁定金额
                                    entryRow.set("e_lockamt", finlockedamt.subtract(finlockekamt));//收款处理已锁定金额

//                                    entryRow.set("nckd_settlekamt", 0);//收款处理结算金额扣减
//                                    entryRow.set("nckd_settleklocalamt", 0);//收款处理结算金额扣减本位币
//                                    entryRow.set("e_unsettledamt", 0.01);//收款处理未结算金额
//                                    entryRow.set("e_unsettledlocalamt", 0.01);//收款处理未结算金额本位币
//                                    entryRow.set("e_settledamt", 0);//收款处理已结算金额
//                                    entryRow.set("e_settledlocalamt", 0);//收款处理已结算金额本位币
//                                    entryRow.set("nckd_lockekamt", 0);//收款处理锁定金额扣减
//                                    entryRow.set("e_unlockamt", 0.01);//收款处理未锁定金额
//                                    entryRow.set("e_lockamt", 0);//收款处理已锁定金额
                                }
                            }
                        }
                        unsettleamount = unsettleamount.add(entryRow.getBigDecimal("e_unsettledamt"));//未结算金额
                        unsettleamountbase = unsettleamountbase.add(entryRow.getBigDecimal("e_unsettledlocalamt"));//未结算金额本位币
                        settleamount = settleamount.add(entryRow.getBigDecimal("e_settledamt"));//已结算金额
                        settleamountbase = settleamountbase.add(entryRow.getBigDecimal("e_settledlocalamt"));//已结算金额本位币
                    }
                    if (unsettleamount.add(unsettleamountbase).add(settleamount).add(settleamountbase).compareTo(BigDecimal.ZERO) > 0) {
                        dataObject.set("unsettleamount", unsettleamount);
                        dataObject.set("unsettleamountbase", unsettleamountbase);
                        dataObject.set("settleamount", settleamount);
                        dataObject.set("settleamountbase", settleamountbase);
                    }
                    SaveServiceHelper.update(dataObject);
                }
            }
        }
    }
}
