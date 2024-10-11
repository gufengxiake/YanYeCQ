package nckd.yanye.hr.plugin.form.zhaoping;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.swc.hsbp.business.servicehelper.SWCDataServiceHelper;
import kd.swc.hsbp.opplugin.web.SWCCoreBaseBillOp;

import java.util.ArrayList;
import java.util.List;


/**
 * Module           :人才供应云-招聘直通车-首页-年度招聘计划
 * Description      :审批流按钮操作插件
 *
 * @author guozhiwei
 * @date  2024/10/11 11：06
 * 标识 nckd_yearapply
 */


public class YearcrapplyPlanButPlugin extends SWCCoreBaseBillOp {

    private static Log logger = LogFactory.getLog(YearcrapplyPlanButPlugin.class);


    public void beginOperationTransaction(BeginOperationTransactionArgs args) {
        super.beginOperationTransaction(args);
        DynamicObject[] regBillObjs = args.getDataEntities();
        List<Long> billIdList = new ArrayList(10);
        DynamicObject[] var5 = regBillObjs;
        int var6 = regBillObjs.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            DynamicObject dynamicObject = var5[var7];
            long id = dynamicObject.getLong("id");
            billIdList.add(id);
        }

        SWCDataServiceHelper helper = new SWCDataServiceHelper("nckd_yearcasreplan");
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

