package nckd.yanye.tmc.plugin.report;

import java.util.*;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.events.TreeReportListEvent;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.tmc.fbp.common.util.DateUtils;
import kd.tmc.fbp.common.util.EmptyUtil;

/**
 * @author husheng
 * @date 2024-09-19 13:41
 * @description 交易汇总表（nckd_transaction_summary）报表界面插件
 */
public class TransactionSummaryFormPlugin extends AbstractReportFormPlugin {
    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

        DynamicObjectCollection dynamicObjects = QueryServiceHelper.query("fbd_companysysviewsch", "id", new QFilter[]{new QFilter("number", QCP.equals, "08")});
        long userDefaultOrgID = UserServiceHelper.getUserDefaultOrgID(RequestContext.get().getCurrUserId());
        this.getModel().setValue("nckd_filter_orgview", dynamicObjects.get(0).getLong("id"));
        this.getModel().setValue("nckd_mulcalorg", new Object[]{userDefaultOrgID});
        this.getModel().setValue("nckd_startdate", DateUtils.getFirstDayOfCurMonth());
        this.getModel().setValue("nckd_enddate", DateUtils.getLastDayOfCurMonth());
    }

    @Override
    public boolean verifyQuery(ReportQueryParam queryParam) {
        if (EmptyUtil.isEmpty(this.getModel().getValue("nckd_filter_orgview"))) {
            this.getView().showTipNotification(ResManager.loadKDString("请选择资金组织视图", "TransactionSummaryFormPlugin_2", "tmc-mon-report", new Object[0]));
            return false;
        } else if (EmptyUtil.isEmpty(this.getModel().getValue("nckd_mulcalorg"))) {
            this.getView().showTipNotification(ResManager.loadKDString("请选择资金组织", "TransactionSummaryFormPlugin_3", "tmc-mon-report", new Object[0]));
            return false;
        } else if (EmptyUtil.isEmpty(this.getModel().getValue("nckd_startdate"))) {
            this.getView().showTipNotification(ResManager.loadKDString("请选择开始日期", "TransactionSummaryFormPlugin_4", "tmc-mon-report", new Object[0]));
            return false;
        } else if (EmptyUtil.isEmpty(this.getModel().getValue("nckd_enddate"))) {
            this.getView().showTipNotification(ResManager.loadKDString("请选择结束日期", "TransactionSummaryFormPlugin_5", "tmc-mon-report", new Object[0]));
            return false;
        } else {
            FilterInfo filterInfo = queryParam.getFilter();
            Date startDate = (Date) filterInfo.getFilterItem("nckd_startdate").getValue();
            Date endDate = (Date) filterInfo.getFilterItem("nckd_enddate").getValue();
            if (DateUtils.getDiffDays(startDate, endDate) <= 1) {
                this.getView().showTipNotification(ResManager.loadKDString("日期范围相差要大于等于1天", "TransactionSummaryFormPlugin_0", "tmc-mon-report", new Object[0]));
                return false;
            } else if (startDate.compareTo(endDate) > 0) {
                this.getView().showTipNotification(ResManager.loadKDString("开始日期不能大于结束日期", "TransactionSummaryFormPlugin_1", "tmc-mon-report", new Object[0]));
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public void setTreeReportList(TreeReportListEvent event) {
        super.setTreeReportList(event);

        event.setTreeReportList(true);
    }
}
