package nckd.yanye.occ.plugin.report;


import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.util.*;

/**
 * 业务员借货汇总表界面插件
 * 表单标识：nckd_ywyjhhz_rpt
 * author:zhangzhilong
 * date:2024/08/21
 *  */

public class ImTransdirbillReportFormPlugin extends AbstractReportFormPlugin implements Plugin, BeforeF7SelectListener {
    private static final String [] FIELDS ={"nckd_forg","nckd_ywy","nckd_material","nckd_materialname",
            "nckd_materialmodelnum","nckd_unit","nckd_jhqty",
            "nckd_xsqty","nckd_jchhqty","nckd_jhyeqty"};


    public void afterCreateNewData(EventObject e) {
        Long curLoginOrg = RequestContext.get().getOrgId();
        this.getModel().setValue("nckd_forg_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();
            //计算借货余额数量 = 借货数量-销售数量-借出还回数量
            BigDecimal jhyeqty = row.getBigDecimal(FIELDS[6]).subtract( row.getBigDecimal(FIELDS[7])) ;
            jhyeqty = jhyeqty.subtract(row.getBigDecimal(FIELDS[8]));
            row.set(FIELDS[9], jhyeqty);
        }

    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit baseData = this.getControl("nckd_ywy_q");
        baseData.addBeforeF7SelectListener(this);
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        if ("nckd_ywy_q".equals(name)) {
            // 获取当前登录业务单元id
            long orgId = RequestContext.get().getOrgId();
            QFilter orgFilter = new QFilter("opergrptype", QCP.equals, "XSZ");
            ListShowParameter showParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();
            // 基础资料添加列表过滤条件
//            showParameter.getListFilterParameter().getQFilters().add(orgFilter);
            showParameter.getListFilterParameter().setFilter(orgFilter);
            // 基础资料左树添加过滤条件
            showParameter.getTreeFilterParameter().getQFilters().add(orgFilter);
        }
    }
}