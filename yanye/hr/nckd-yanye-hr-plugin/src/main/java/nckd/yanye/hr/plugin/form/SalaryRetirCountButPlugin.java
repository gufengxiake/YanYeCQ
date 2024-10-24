package nckd.yanye.hr.plugin.form;

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
 *
 * Module           :薪酬福利云-薪资数据集成-员工离岗退养工资统计
 * Description      :员工离岗退养工资统计审批流按钮操作插件
 *
 * @author guozhiwei
 * @date  2024/10/23 15:39
 *  标识:nckd_staffretiresalacount
 *
 */



public class SalaryRetirCountButPlugin extends SWCCoreBaseBillOp {


    private static Log logger = LogFactory.getLog(SalaryRetirCountButPlugin.class);


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

        SWCDataServiceHelper helper = new SWCDataServiceHelper("nckd_preadjustapplic");
        QFilter filter = new QFilter("id", "in", billIdList);
        DynamicObject[] needUpdateBillDy = helper.query("id,billstatus", new QFilter[]{filter});

        switch (args.getOperationKey()) {
            case "wfauditnotpass":
                // 审批不通过
                this.setApproveStatus(needUpdateBillDy, "E");
                break;
            case "wfrejecttosubmit":
                // 驳回到提交人
                this.setApproveStatus(needUpdateBillDy, "A");
                break;
            case "wfauditing":
                // 审批中
                this.setApproveStatus(needUpdateBillDy, "D");
                break;
            case "wfauditpass":
                // 审批通过
                this.setApproveStatus(needUpdateBillDy, "C");
                break;
            case "audit":
                // 审批
                this.setApproveStatus(needUpdateBillDy,"C");
            case "unsubmit":
                // 反审批
                this.setApproveStatus(needUpdateBillDy,"A");
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
            dynamicObject.set("nckd_auditortype",billstatus);

        }

    }


}
