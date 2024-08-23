package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.*;

/**
 * 业务员借货汇总表界面插件
 * 表单标识：nckd_ywyjhhz_rpt
 * author:zzl
 * date:2024/08/21
 *  */

public class ImTransdirbillReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    private static String [] FIELDS ={"nckd_forg","nckd_ywy","nckd_material","nckd_materialname",
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
            long jhyeqty = 0L;
            //计算借货余额数量 = 借货数量-销售数量-借出还回数量
            jhyeqty = row.getLong(FIELDS[6]) - row.getLong(FIELDS[7]) - row.getLong(FIELDS[8]);
            row.set(FIELDS[9], jhyeqty);
        }

    }

}