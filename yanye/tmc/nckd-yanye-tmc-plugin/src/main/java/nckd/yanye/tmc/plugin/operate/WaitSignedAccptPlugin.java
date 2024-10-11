package nckd.yanye.tmc.plugin.operate;

import kd.bos.bec.api.IEventServicePlugin;
import kd.bos.bec.model.EntityEvent;
import kd.bos.bec.model.KDBizEvent;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.common.util.DateUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
        // 将 List<String> 转换为 List<Long>
        List<Long> businessKeyLongs = businesskeys.stream()
                .map(Long::valueOf) // 将每个 String 转换为 Long
                .collect(Collectors.toList());
        String newAcceptancePeriod = null;

        DynamicObject[] transdetails = BusinessDataServiceHelper.load("cdm_electronic_sign_deal", "id,issueticketdate,exchangebillexpiredate,nckd_acceptance_period", new QFilter[]{new QFilter("id", QCP.in, businessKeyLongs)});
        for (DynamicObject transdetail : transdetails) {
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
                    logger.info("计算承兑期限完成key：{}，值:{}",transdetail.getPkValue(),newAcceptancePeriod);
                }

            }
        }

        return null;
    }
}
