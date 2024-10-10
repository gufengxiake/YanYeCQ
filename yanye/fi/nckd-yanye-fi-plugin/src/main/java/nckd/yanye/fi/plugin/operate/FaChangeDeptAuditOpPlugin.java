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
import org.apache.commons.lang3.StringUtils;


import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务-资产变更单审核操作插件
 * 表单标识：fa_change_dept
 * author：xiaoxiaopeng
 * date：2024-09-13
 */
public class FaChangeDeptAuditOpPlugin extends AbstractOperationServicePlugIn {

    private static Log logger = LogFactory.getLog(FaChangeDeptAuditOpPlugin.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("changetype");
        fieldKeys.add("main_changebillentry");
        fieldKeys.add("createtime");
        fieldKeys.add("org");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            DynamicObject changeType = dataEntity.getDynamicObject("changetype");
            DynamicObjectCollection changeBillEntry = dataEntity.getDynamicObjectCollection("main_changebillentry");
            if (changeBillEntry.size() > 0) {
                for (DynamicObject entry : changeBillEntry) {
                    DynamicObject realCard = entry.getDynamicObject("m_realcard");
                    realCard = BusinessDataServiceHelper.loadSingle(realCard.getPkValue(), "fa_card_real_base");
                    DynamicObject assetcat = realCard.getDynamicObject("assetcat");
                    assetcat = BusinessDataServiceHelper.loadSingle(assetcat.getPkValue(), "fa_assetcategory");
                    String fixedassettype = assetcat.getString("nckd_fixedassettype");
                    if ("1".equals(fixedassettype) && "002".equals(changeType.getString("number"))) {
                        sedMessage(dataEntity, "房产原值变更", entry, assetcat, realCard, fixedassettype);
                    } else if ("2".equals(fixedassettype) && "025".equals(changeType.getString("number"))) {
                        sedMessage(dataEntity, "土地面积变更", entry, assetcat, realCard, fixedassettype);
                    }
                }
            }
        }
    }

    private void sedMessage(DynamicObject dataEntity, String messageInformersName, DynamicObject entry, DynamicObject assetcat, DynamicObject realCard, String fixedassettype) {
        //查维护税源信息消息通知人拿到模板
        DynamicObject messageInformers = BusinessDataServiceHelper.loadSingle("nckd_messageinformers", "id,name,nckd_user,nckd_mescontent", new QFilter[]{new QFilter("name", QCP.equals, messageInformersName)});
        if (messageInformers == null) {
            return;
        }

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
            if (s.equals(arr[arr.length - 1])) {
                continue;
            }
            resultMes = resultMes + s + "：%s";
        }
        String formatMes = "";
        if ("1".equals(fixedassettype)) {
            formatMes = String.format(resultMes, dataEntity.getDynamicObject("org").getString("name"), realCard.getString("number"), realCard.getString("assetname"), createtime, entry.getString("m_bef_originalval"), entry.getString("m_aft_originalval"));
        } else if ("2".equals(fixedassettype)) {
            formatMes = String.format(resultMes, dataEntity.getDynamicObject("org").getString("name"), realCard.getString("number"), realCard.getString("assetname"), createtime, entry.getString("m_bef_nckd_textfield6"), entry.getString("m_aft_nckd_textfield6"));
        }
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
