package nckd.yanye.tmc.plugin.task;


import kd.bos.dataentity.entity.ILocaleString;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.workflow.engine.msg.MessageServiceConfig;
import kd.bos.workflow.engine.msg.MessageServiceUtil;
import kd.bos.workflow.engine.msg.info.MessageInfo;
import kd.bos.bec.model.EntityEvent;
import kd.bos.bec.model.KDBizEvent;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.bec.api.IEventServicePlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.workflow.MessageCenterServiceHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Module           :
 * Description      :银行流水推送业务人员
 *
 * @author : guozhiwei
 * @date : 2024/8/7
 *
 */
public class BankAccountTask  implements IEventServicePlugin {

    private static Log logger = LogFactory.getLog(BankAccountTask.class);


    @Override
    public Object handleEvent(KDBizEvent evt) {
        if(evt instanceof EntityEvent){
            logger.info("付款申请单.退票消息通知.执行插件:-------------------");
//            logger.info("插件参数EventId：{}", evt.getEventId());
//            logger.info("插件参数Source：{}", evt.getSource());
//            logger.info("插件参数EventNumber：{}", evt.getEventNumber());
            logger.info("插件参数businesskeys：{}", ((EntityEvent) evt).getBusinesskeys());
//            logger.info("插件参数entityNumber：{}", ((EntityEvent) evt).getEntityNumber());

            // 交易明细id
            List<String> businesskeys = ((EntityEvent) evt).getBusinesskeys();
            for (Object businesskey : businesskeys) {
                // 查询出交易明细
                DynamicObject transdetail = BusinessDataServiceHelper.loadSingle(businesskey, "bei_transdetail");
                String oppunit = transdetail.getString("oppunit");
                if(StringUtils.isNotEmpty(oppunit)){
                    // 对方户名查询出客户信息
                    logger.info("对方户名：{}", oppunit);
                    QFilter qFilter = new QFilter("name", QCP.equals, oppunit);
                    DynamicObject customer = BusinessDataServiceHelper.loadSingle("bd_customer", "name,salerid",new QFilter[]{qFilter});
                    if(ObjectUtils.isNotEmpty(customer)){
                        logger.info("交易明细信息：{}", transdetail);
                        // 查询需要发送通知的业务人员
                        DynamicObject salerid = customer.getDynamicObject("salerid");
                        logger.info("业务人员信息：{}", salerid);
                        if(ObjectUtils.isNotEmpty(salerid)){
                            // 发送通知
                            sendMessageChannel(salerid,transdetail);
                        }
                    }
                }
            }
        }

        return null;
    }


    static void sendMessageChannel(DynamicObject salerid,DynamicObject customer){
        // 发送通知
        logger.info("开始发送通知:-------------------");
        // 云之家通知 金蝶云苍穹消息助手 标识：systempubacc
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setTitle("您好，您有一条银行流水信息，请注意查收。");
        messageInfo.setContent("收到银行推送流水通知，请尽快查看和处理。");

        List<Long> userids = new ArrayList<Long>();
        userids.add(salerid.getLong("id"));
        logger.info("设置接收人成功：{}",userids);
        messageInfo.setSenderId( (Long) customer.getDynamicObject("creator").getPkValue());
        logger.info("设置发送人成功：{}",(Long) customer.getDynamicObject("creator").getPkValue());
//        messageInfo.setToUser(salerid.getString("id"));
        messageInfo.setType(MessageInfo.TYPE_MESSAGE);
        messageInfo.setNotifyType("yunzhijia");
        messageInfo.setTag("银行流水");
        logger.info("发送信息体messageInfo:{}",messageInfo);
//        MessageServiceConfig messageServiceConfig = new MessageServiceConfig();
//        messageServiceConfig.setServiceKey("yunzhijia");

//        MessageServiceUtil.updateToDoMsgContent(messageServiceConfig,messageInfo);
//        String contentToReplace = messageInfo.getContent();
//        messageInfo.setContent(contentToReplace);

        long l = MessageCenterServiceHelper.sendMessage(messageInfo);
        logger.info("发送通知成功：{}",l);


    }



}
