package nckd.yanye.tmc.plugin.task;

import kd.bos.bec.api.IEventServicePlugin;
import kd.bos.bec.model.EntityEvent;
import kd.bos.bec.model.KDBizEvent;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * 资金-收票处理
 * 表单标识：nckd_cas_recbill_ext
 * author：xiaoxiaopeng
 * date：2024-10-15
 */
public class CasRecBillSaveTask implements IEventServicePlugin {
    private static Log logger = LogFactory.getLog(CasRecBillSaveTask.class);


    @Override
    public Object handleEvent(KDBizEvent evt) {
        List<String> businesskeys = ((EntityEvent) evt).getBusinesskeys();
        for (String businesskey : businesskeys) {
            DynamicObject casRecbill = BusinessDataServiceHelper.loadSingle(businesskey, "cas_recbill");
            logger.info("收票处理单据信息,{}",casRecbill);

            //查上游单据收款入账中心
            DynamicObject intelRec = BusinessDataServiceHelper.loadSingle("bei_intelrec", "id,recedbillnumber,billno,rulename,oppunit",
                    new QFilter[]{new QFilter("recedbillnumber", QCP.equals, casRecbill.getString("billno"))});
            if (intelRec == null) {
                continue;
            }
            if ("销售回款".equals(intelRec.getString("rulename"))){
                String oppunit = intelRec.getString("oppunit");
                if (StringUtils.isBlank(oppunit)){
                    continue;
                }
                DynamicObject customer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,name,nckd_engname",
                        new QFilter[]{new QFilter("nckd_engname", QCP.equals, oppunit)});
                if (customer == null) {
                    continue;
                }
                casRecbill.set("payername", customer.getString("name"));
                logger.info("客户付款人名称，{}",customer.getString("name"));
                SaveServiceHelper.update(casRecbill);
            }
        }

        return IEventServicePlugin.super.handleEvent(evt);

    }
}
