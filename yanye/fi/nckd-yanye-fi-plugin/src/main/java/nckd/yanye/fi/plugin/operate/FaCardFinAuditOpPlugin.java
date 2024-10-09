package nckd.yanye.fi.plugin.operate;


import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.ILocaleString;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.message.api.MessageChannels;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.workflow.MessageCenterServiceHelper;
import kd.bos.workflow.engine.msg.info.MessageInfo;
import kd.tmc.cim.common.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务-财务卡片审核操作插件
 * 表单标识：nckd_fa_card_fin_ext
 * author：xiaoxiaopeng
 * date：2024-09-18
 */
public class FaCardFinAuditOpPlugin extends AbstractOperationServicePlugIn {

    private static Log logger = LogFactory.getLog(FaCardFinAuditOpPlugin.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("assetcat");
        fieldKeys.add("createtime");
        fieldKeys.add("originalval");
        fieldKeys.add("org");
        fieldKeys.add("realcard");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            DynamicObject assetcat = dataEntity.getDynamicObject("assetcat");
            String fixedassettype = assetcat.getString("nckd_fixedassettype");
            if (StringUtils.isEmpty(fixedassettype) || "3".equals(fixedassettype)) {
                continue;
            }
            if ("1".equals(fixedassettype)) {
                sedMessage(dataEntity, "新增房产");
            } else if ("2".equals(fixedassettype)) {
                return;
                //sedMessage(dataEntity, "新增土地");
            }
        }
    }

    private void sedMessage(DynamicObject dataEntity, String messageInformersName) {
        //查维护税源信息消息通知人拿到模板
        DynamicObject messageInformers = BusinessDataServiceHelper.loadSingle("nckd_messageinformers", "id,name,nckd_user,nckd_mescontent", new QFilter[]{new QFilter("name", QCP.equals, messageInformersName)});
        if (messageInformers == null) {
            return;
        }

        DynamicObject realcard = dataEntity.getDynamicObject("realcard");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String createtime = simpleDateFormat.format(dataEntity.getDate("createtime"));
        //发送消息
        MessageInfo message = new MessageInfo();

        //消息头
        ILocaleString title = new LocaleString();
        title.setLocaleValue_en("Message notifications");
        title.setLocaleValue_zh_CN("消息通知");
        title.setLocaleValue_zh_TW("");

        //消息体
        ILocaleString content = new LocaleString();
        //新增房产，资产编号：XXXX，资产名称：XXX，新增时间：年-月-日，房产金额：XXX；
        String contentMessage = messageInformers.getString("nckd_mescontent");
        String[] arr = contentMessage.split("：");
        String resultMes = "【%s】";
        for (String s : arr) {
            if (s.equals(arr[arr.length-1])){
                continue;
            }
            resultMes = resultMes + s + "：%s";
        }
        String formatMes = String.format(resultMes, dataEntity.getDynamicObject("org").getString("name"),dataEntity.getString("number"), realcard.getString("assetname"), createtime, dataEntity.getBigDecimal("originalval").toString());
        //String contentMessage = "新增房产，资产编号：" + assetcat.getString("number") + ",资产名称：" + assetcat.getString("number") + ",新增时间：" + createtime + ",房产金额：" + dataEntity.getString("price");
        String contentMessageEng = StringUtils.toEncodedString(formatMes.getBytes(), StandardCharsets.UTF_8);
        content.setLocaleValue_en(contentMessageEng);
        content.setLocaleValue_zh_CN(formatMes);
        content.setLocaleValue_zh_TW("");

        ILocaleString tag = new LocaleString();
        tag.setLocaleValue("通知");

        //消息接收人
        ArrayList<Long> receivers = new ArrayList<>();
        DynamicObjectCollection users = messageInformers.getDynamicObjectCollection("nckd_user");
        for (DynamicObject user : users) {
            DynamicObject bos_user = user.getDynamicObject("fbasedataid");
            receivers.add(Long.parseLong(bos_user.getPkValue().toString()));
        }

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
        stringBuilder.append(MessageChannels.YUNZHIJIA.getNumber()).append(",");
        stringBuilder.append(MessageChannels.SMS.getNumber());
        message.setNotifyType(stringBuilder.toString());
        long l = MessageCenterServiceHelper.sendMessage(message);
        logger.info("消息发送成功：{}", l);
        logger.info("message：{}", message);
    }
}
