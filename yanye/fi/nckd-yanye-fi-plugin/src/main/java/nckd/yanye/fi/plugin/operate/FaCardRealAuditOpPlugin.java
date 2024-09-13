package nckd.yanye.fi.plugin.operate;



import kd.bos.bec.api.IEventServicePlugin;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.ILocaleString;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.message.api.MessageChannels;
import kd.bos.servicehelper.workflow.MessageCenterServiceHelper;
import kd.bos.workflow.engine.msg.info.MessageInfo;
import kd.scmc.scmdi.common.vo.bos.MessageChannel;
import kd.tmc.cim.common.util.StringUtils;


import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务-实物卡片审核操作插件
 * 表单标识：fa_card_real
 * author：xiaoxiaopeng
 * date：2024-09-12
 */
public class FaCardRealAuditOpPlugin extends AbstractOperationServicePlugIn {

    private static Log logger = LogFactory.getLog(FaCardRealAuditOpPlugin.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("assetcat");
        fieldKeys.add("createtime");
        fieldKeys.add("price");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            DynamicObject assetcat = dataEntity.getDynamicObject("assetcat");
            boolean result = assetcat.getBoolean("nckd_fixedasset");
            if (!result) {
                continue;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            //发送消息
            MessageInfo message = new MessageInfo();

            //消息头
            ILocaleString title = new LocaleString();
            title.setLocaleValue_en("Message notifications");
            title.setLocaleValue_zh_CN("消息通知");
            title.setLocaleValue_zh_TW("");

            //消息体
            ILocaleString content = new LocaleString();
            String createtime = simpleDateFormat.format(dataEntity.getDate("createtime"));
            //新增房产，资产编号：XXXX，资产名称：XXX，新增时间：年-月-日，房产金额：XXX；
            String contentMessage = "新增房产，资产编号："+assetcat.getString("number")+",资产名称："+assetcat.getString("name") + ",新增时间："+createtime+",房产金额："+dataEntity.getString("price");
            String contentMessageEng = StringUtils.toEncodedString(contentMessage.getBytes(), StandardCharsets.UTF_8);
            content.setLocaleValue_en(contentMessageEng);
            content.setLocaleValue_zh_CN(contentMessage);
            content.setLocaleValue_zh_TW("");

            ILocaleString tag = new LocaleString();
            tag.setLocaleValue("紧急");

            //发送人
            ArrayList<Long> receivers = new ArrayList<>();
            receivers.add(Long.parseLong(RequestContext.get().getUserId()));

            message.setMessageTitle(title);
            message.setMessageContent(content);
            message.setMessageTag(tag);
            message.setUserIds(receivers);
            message.setSenderId(Long.parseLong(RequestContext.get().getUserId()));
            message.setType(MessageInfo.TYPE_MESSAGE);
            message.setEntityNumber("fa_card_real");
            message.setOperation("audit");
            message.setBizDataId(dataEntity.getLong("id"));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(MessageChannels.YUNZHIJIA).append(",");
            stringBuilder.append(MessageChannels.SMS);
            message.setNotifyType(stringBuilder.toString());
            long l = MessageCenterServiceHelper.sendMessage(message);
            logger.info("消息发送成功：{}",l);
            logger.info("message：{}",message);


        }

    }
}
