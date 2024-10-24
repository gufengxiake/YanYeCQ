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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
            logger.info("收票处理单据信息,{}", casRecbill);

            //查上游单据收款入账中心
            DynamicObject intelRec = BusinessDataServiceHelper.loadSingle(casRecbill.get("sourcebillid"),"bei_intelrec");
            if (intelRec == null) {
                continue;
            }

            String oppunit = intelRec.getString("oppunit");
            if (StringUtils.isBlank(oppunit)) {
                continue;
            }
            //判断对方户名是否包含中文
            Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
            Matcher m = p.matcher(oppunit);
            if (m.find()) {
                continue;
            }
            DynamicObject customer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,name,nckd_entry_english,nckd_entry_english.nckd_engname",
                    new QFilter[]{new QFilter("name", QCP.equals, oppunit)});
            if (customer == null) {
                continue;
            }
            DynamicObject zCustomer = customer.getDynamicObjectCollection("nckd_entry_english").size() > 0 ? customer.getDynamicObjectCollection("nckd_entry_english").get(0).getDynamicObject("nckd_engname") : null;
            if (zCustomer != null) {
                casRecbill.set("payername", zCustomer.getString("name"));
                casRecbill.set("payer", zCustomer.getPkValue());
                logger.info("客户付款人名称，{}", zCustomer.getString("name"));
                logger.info("客户付款人，{}", zCustomer);
                SaveServiceHelper.update(casRecbill);
            }
        }

        return IEventServicePlugin.super.handleEvent(evt);

    }
}
