package nckd.yanye.occ.plugin.operate;

import com.alibaba.fastjson.JSON;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.tx.TX;
import kd.bos.db.tx.TXHandle;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.servicehelper.DispatchServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.mpscmm.msbd.reserve.mservice.ReserveService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 销售出库单审核服务插件 释放预留
 * 表单标识：nckd_im_saloutbill_ext
 * author:吴国强 2024-07-12
 */
public class SaloutReserveFreeOperPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        // TODO Auto-generated method stub
        super.onPreparePropertys(e);
        e.getFieldKeys().add("billentry.mainbillentity");//核心单据实体
        e.getFieldKeys().add("billentry.mainbillid");//核心单据Id
        e.getFieldKeys().add("billentry.mainbillentryid");//核心单据行Id
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        // TODO Auto-generated method stub
        super.beginOperationTransaction(e);
        DynamicObject[] dataEntitiesList = e.getDataEntities();
        for (DynamicObject dataEntities : dataEntitiesList) {
            DynamicObjectCollection billentry = dataEntities.getDynamicObjectCollection("billentry");
            for (DynamicObject entryRow : billentry) {
                String mainbillentity = entryRow.getString("mainbillentity");
                if (mainbillentity.equalsIgnoreCase("ocbsoc_saleorder")) {
                    Object mainbillid = entryRow.get("mainbillid");
                    Object mainbillentryid = entryRow.get("mainbillentryid");
                    TXHandle h1 = TX.requiresNew();
                    Exception exception = null;
                    try {
                        //解除预留
                        ReserveService.reserveRemove(mainbillid, mainbillentryid);
                    } catch (Throwable es) {
                        h1.markRollback();
                        exception = (Exception) es;
                        throw es;
                    } finally {
                        if (h1 != null) {
                            if (exception != null) {
                                try {
                                    h1.close();
                                } catch (Throwable ess) {
                                    exception.addSuppressed(ess);
                                }
                            } else {
                                h1.close();
                            }
                        }
                    }

                }
            }

        }

    }
}
