package nckd.yanye.occ.plugin.mobile;

import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.util.EventObject;

public class MobileTransApplyCopyBillPlugIn extends AbstractMobFormPlugin {
    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        //默认单据类型为外仓转运
        this.getModel().setItemValueByID("billtype", "2025683988596668416");
        Long orgId = RequestContext.get().getOrgId();
        this.getModel().setItemValueByID("nckd_org", orgId);
        //获取调拨申请单编码
        DynamicObject bill = BusinessDataServiceHelper.newDynamicObject("单据标识");
        CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(bill.getDataEntityType().getName(), bill, orgId.toString());
        String billno = CodeRuleServiceHelper.getNumber(codeRule, bill);
        this.getModel().setValue("nckd_billno", billno);

    }
}
