package nckd.yanye.hr.plugin.form.hesuanmingdan;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.workflow.engine.impl.calculator.MacroParser;

import javax.json.Json;
import javax.json.JsonObject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 *
 *  hr-核算名单列表插件
 *  表单标识：nckd_hsas_calperson_ext
 *  author：xiaoxiaopeng
 *  date：2024-10-17
 *
 */
public class HsAsCalPersonListPlugin extends AbstractListPlugin {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        String key = afterDoOperationEventArgs.getOperateKey();
        if ("createapprove".equals(key)) {
            boolean success = afterDoOperationEventArgs.getOperationResult().isSuccess();
            if (success) {
                FormOperate source =  (FormOperate)afterDoOperationEventArgs.getSource();
                ListSelectedRowCollection selectedRows = source.getListSelectedData();
                for (ListSelectedRow row : selectedRows) {
                    //核算名单
                    DynamicObject hsasCalperson = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "hsas_calperson");
                    //核算任务
                    DynamicObject caltask = hsasCalperson.getDynamicObject("caltask");
                    //薪资所属年月
                    Date payrolldate = caltask.getDate("payrolldate");
                    //核算次数
                    BigDecimal calcount = caltask.getBigDecimal("calcount");
                    //核算场景
                    DynamicObject payrollscene = caltask.getDynamicObject("payrollscene");
                    //查核算审批单
                    DynamicObject hsasApprovebill = BusinessDataServiceHelper.loadSingle("hsas_approvebill", "id,nckd_payrolldate,nckd_calcount,nckd_payrollscene,calentryentity,calentryentity.calpersonid",
                            new QFilter[]{new QFilter("calentryentity.calpersonid", QCP.equals, row.getPrimaryKeyValue())});
                    if (hsasApprovebill != null) {
                        hsasApprovebill.set("nckd_payrolldate",payrolldate);
                        hsasApprovebill.set("nckd_calcount",calcount);
                        hsasApprovebill.set("nckd_payrollscene",payrollscene);
                        SaveServiceHelper.update(hsasApprovebill);
                    }
                }
            }
        }
    }
}
