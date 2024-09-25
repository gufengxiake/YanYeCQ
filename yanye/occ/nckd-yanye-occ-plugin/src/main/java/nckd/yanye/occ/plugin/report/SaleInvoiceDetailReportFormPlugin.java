package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateUtil;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.List;

/**
 * 销售发票明细表-报表界面插件
 * 表单标识：nckd_saleinvoicedet_rpt
 * author:zhangzhilong
 * date:2024/09/20
 */
public class SaleInvoiceDetailReportFormPlugin extends AbstractReportFormPlugin implements Plugin , BeforeF7SelectListener {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        //组织默认当前
        filter.addFilterItem("nckd_org_q", RequestContext.get().getOrgId());
        //时间默认当前时间
        filter.addFilterItem("date_s",new Date());
        filter.addFilterItem("date_e",new Date());

    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit nckd_warehouse_q = this.getControl("nckd_warehouse_q");
        nckd_warehouse_q.addBeforeF7SelectListener(this);
        BasedataEdit nckd_dept_q = this.getControl("nckd_dept_q");
        nckd_dept_q.addBeforeF7SelectListener(this);
        BasedataEdit nckd_bizoperator_q = this.getControl("nckd_bizoperator_q");
        nckd_bizoperator_q.addBeforeF7SelectListener(this);

    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        if ("nckd_warehouse_q".equals(name)) {
            // 获取当前登录业务单元id
            long orgId = RequestContext.get().getOrgId();
            QFilter qFilter = new QFilter("createorg", QCP.equals, orgId);

            ListShowParameter showParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();
            // 基础资料添加列表过滤条件
            showParameter.getListFilterParameter().setFilter(qFilter);
            // 基础资料左树添加过滤条件
//            showParameter.getTreeFilterParameter().getQFilters().add(qFilter);
        }else if("nckd_dept_q".equals(name)){
            // 获取当前登录业务单元id
            long orgId = RequestContext.get().getOrgId();
            QFilter qFilter = new QFilter("structure.viewparent", QCP.equals, orgId);

            ListShowParameter showParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();
            // 基础资料添加列表过滤条件
            showParameter.getListFilterParameter().setFilter(qFilter);
        }else if("nckd_bizoperator_q".equals(name)){
            long orgId = RequestContext.get().getOrgId();
            QFilter orgFilter = new QFilter("createorg", QCP.equals, orgId);
            DynamicObjectCollection query = QueryServiceHelper.query("bd_operatorgroup", "id", new QFilter[]{orgFilter});
            if (query != null){
                List<Long> groupIds = new ArrayList<>();
                query.forEach((row) ->{
                    groupIds.add(row.getLong("id"));
                });
                QFilter qFilter = new QFilter("operatorgrpid", QCP.in, groupIds.toArray(new Long[0]));
                ListShowParameter showParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();
                // 基础资料添加列表过滤条件
                showParameter.getListFilterParameter().setFilter(qFilter);
            }
        }
    }
}