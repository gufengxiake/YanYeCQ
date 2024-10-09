package nckd.yanye.occ.plugin.report;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * 销售发票汇总表-报表界面插件
 * 表单标识：nckd_saleinvsum
 * author:zhangzhilong
 * date:2024/10/06
 */
public class SaleInvSumReportFormPlugin extends AbstractReportFormPlugin implements Plugin,BeforeF7SelectListener {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        //给组织默认值
        filter.addFilterItem("nckd_org_q", RequestContext.get().getOrgId());
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        for (DynamicObject rowDatum : rowData) {
            //计算成本金额
            BigDecimal baseqty = rowDatum.getBigDecimal("baseqty") == null ? BigDecimal.ZERO : rowDatum.getBigDecimal("baseqty");
            BigDecimal nckdCbdj = rowDatum.getBigDecimal("nckd_cbdj") == null ? BigDecimal.ZERO : rowDatum.getBigDecimal("nckd_cbdj");
            rowDatum.set("nckd_cbdj",baseqty.multiply(nckdCbdj));
            //赠品改为是/否
            String ispresent = rowDatum.getString("ispresent");
            if(ispresent.equals("true")){
                rowDatum.set("ispresent","是");
            }else{
                rowDatum.set("ispresent","否");
            }
        }
    }
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit nckdOperator = this.getControl("nckd_operator_q");
        nckdOperator.addBeforeF7SelectListener(this);

    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        if("nckd_operator_q".equals(name)){
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