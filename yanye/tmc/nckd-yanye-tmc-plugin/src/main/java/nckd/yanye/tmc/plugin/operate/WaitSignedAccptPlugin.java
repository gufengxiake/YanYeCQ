package nckd.yanye.tmc.plugin.operate;

import kd.bos.bec.api.IEventServicePlugin;
import kd.bos.bec.model.EntityEvent;
import kd.bos.bec.model.KDBizEvent;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.common.util.DateUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;
import java.util.List;

/**
 * Module           :cdm_electronic_sign_deal
 * Description      :待签收票据处理导入保存操作
 *
 * @author : guozhiwei
 * @date : 2024/8/27 20：50
 *
 */

public class WaitSignedAccptPlugin implements IEventServicePlugin {

    private static Log logger = LogFactory.getLog(WaitSignedAccptPlugin.class);

    @Override
    public Object handleEvent(KDBizEvent evt) {
        logger.info("待签收票据处理执行插件:-------------------");
        logger.info("插件参数businesskeys：{}", ((EntityEvent) evt).getBusinesskeys());
        List<String> businesskeys = ((EntityEvent) evt).getBusinesskeys();

        DynamicObject transdetail = null;
        String newAcceptancePeriod = null;
        // 获取导入的数据，进行计算
        for (String businesskey : businesskeys) {
            transdetail = BusinessDataServiceHelper.loadSingle(businesskey, "cdm_electronic_sign_deal");
            Date issueticketdate = transdetail.getDate("issueticketdate");
            Date exchangebillexpiredate = transdetail.getDate("exchangebillexpiredate");
            String acceptancePeriod = transdetail.getString("nckd_acceptance_period");
            if(issueticketdate != null && exchangebillexpiredate != null){
                int diffMonths = DateUtil.getDiffMonthsByLocalDate(issueticketdate,exchangebillexpiredate,false,true);

                if(diffMonths>6){
                    newAcceptancePeriod = "B";
                }else{
                    newAcceptancePeriod = "A";
                }
                if(ObjectUtils.isEmpty(acceptancePeriod) || !acceptancePeriod.equals(newAcceptancePeriod)){
                    transdetail.set("nckd_acceptance_period",newAcceptancePeriod);
                    SaveServiceHelper.save(new DynamicObject[]{transdetail});
                    logger.info("计算承兑期限完成key：{}，值:{}",businesskey,newAcceptancePeriod);
                }

            }

        }
        return null;
    }
}
