package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.util.EventObject;

/**
 * 订单流程查询表界面插件
 * 表单标识：nckd_orderprocessquery
 * author:zhangzhilong
 * date:2024/08/26
 */
public class OrderProcessQueryReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    public void afterCreateNewData(EventObject e) {
        Long curLoginOrg = RequestContext.get().getOrgId();
        this.getModel().setValue("nckd_saleorgid_q", curLoginOrg);
    }

}