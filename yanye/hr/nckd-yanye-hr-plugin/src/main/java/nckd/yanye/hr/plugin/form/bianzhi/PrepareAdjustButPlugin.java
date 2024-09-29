package nckd.yanye.hr.plugin.form.bianzhi;




import java.util.*;
import kd.bos.dataentity.entity.DynamicObject;

import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.swc.hsas.business.cal.helper.HSASCalApproveBillHelper;
import kd.swc.hsbp.business.cal.helper.PayrollTaskHelper;
import kd.swc.hsbp.business.servicehelper.SWCDataServiceHelper;
import kd.swc.hsbp.opplugin.web.SWCCoreBaseBillOp;


public class PrepareAdjustButPlugin extends SWCCoreBaseBillOp {

    private static Log logger = LogFactory.getLog(PrepareAdjustButPlugin.class);
    public static final String RE_CALCRATE_CONFIRMED = "re_calcrate_confirmed";


    public void beginOperationTransaction(BeginOperationTransactionArgs args) {
        super.beginOperationTransaction(args);
        DynamicObject[] regBillObjs = args.getDataEntities();
        List<Long> billIdList = new ArrayList(10);
        Set<Long> calTaskIds = new HashSet(regBillObjs.length);
        DynamicObject[] var5 = regBillObjs;
        int var6 = regBillObjs.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            DynamicObject dynamicObject = var5[var7];
            long id = dynamicObject.getLong("id");
            billIdList.add(id);
//            calTaskIds.addAll(HSASCalApproveBillHelper.getCalTask(dynamicObject));
        }

        SWCDataServiceHelper helper = new SWCDataServiceHelper("nckd_preadjustapplic");
        QFilter filter = new QFilter("id", "in", billIdList);
        DynamicObject[] needUpdateBillDy = helper.query("id,billstatus", new QFilter[]{filter});

        switch (args.getOperationKey()) {
            case "wfauditnotpass":
                this.setApproveStatus(needUpdateBillDy, "E");
                break;
            case "wfrejecttosubmit":
                this.setApproveStatus(needUpdateBillDy, "A");
                break;
            case "wfauditing":
                this.setApproveStatus(needUpdateBillDy, "D");
                break;
            case "wfauditpass":
                this.setApproveStatus(needUpdateBillDy, "C");
                break;
            default:
                break;
        }

        helper.update(needUpdateBillDy);


    }

    private void setApproveStatus(DynamicObject[] regBillObjs, String billstatus) {
        DynamicObject[] var3 = regBillObjs;
        int var4 = regBillObjs.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject dynamicObject = var3[var5];
            dynamicObject.set("billstatus", billstatus);
        }

    }


}

