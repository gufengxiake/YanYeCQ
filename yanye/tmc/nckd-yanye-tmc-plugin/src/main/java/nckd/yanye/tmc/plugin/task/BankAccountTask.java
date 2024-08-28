package nckd.yanye.tmc.plugin.task;


import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.cache.IAppCache;
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
import kd.tmc.bei.business.helper.CasFlowConfirmLogHelper;
import kd.tmc.bei.business.helper.RecClaimHelper;
import kd.tmc.bei.common.helper.ExtendConfigHelper;
import kd.tmc.fbp.common.helper.TmcAppCache;
import kd.tmc.fbp.common.helper.TmcOperateServiceHelper;
import kd.tmc.fbp.common.util.EmptyUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Module           :bei_betransdetail_imp
 * Description      :银行流水推送业务人员
 *
 * @author : guozhiwei
 * @date : 2024/8/7
 *
 */
public class BankAccountTask  implements IEventServicePlugin {

    private static Log logger = LogFactory.getLog(BankAccountTask.class);

    private static IAppCache cache = TmcAppCache.get("cas", "intelrec", "claim");


    @Override
    public Object handleEvent(KDBizEvent evt) {
        if(evt instanceof EntityEvent){
            logger.info("离线导入执行插件:-------------------");
            logger.info("插件参数businesskeys：{}", ((EntityEvent) evt).getBusinesskeys());

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
                            DynamicObject user = loadSingle(salerid.getPkValue());
                            if(ObjectUtils.isNotEmpty(user)){
                                // 获取用户云之家id
                                String useropenid = user.getString("useropenid");
                                logger.info("用户云之家id：{}", useropenid);
                                if(useropenid!=null){
                                    sendMessageChannel(user,transdetail);
                                }else{
                                    logger.info("用户云之家id为空，不发送通知");
                                }
                            }
                        }
//                        else{
//                            //  未配置负责人，根据企业匹配系统参数触发认领操作
//                            logger.info("未配置客户负责人，根据企业匹配系统参数触发认领操作");
//                            // 交易明细编号
//                            String billno = transdetail.getString("billno");
//                            logger.info("交易明细编号：{}", billno);
//                            // 根据交易明细获取 收款入账中心 数据, 应用 fs，
//                            Long companyid = transdetail.getLong("company.masterid");
//                            //  orgid 100000, appid /SIQN87JDP2A, ,new AppParam("/SIQN87JDP2A","08",100000L,null)
//                            AppInfo appInfo = AppMetadataCache.getAppInfo("fs");
//                            String appId = appInfo.getId();
//                            AppParam appParam = new AppParam();
//                            appParam.setViewType("08");
//                            appParam.setAppId(appId);
//                            appParam.setOrgId(companyid);
//
//                            Map<String,Object> systemMap= SystemParamServiceHelper.loadAppParameterFromCache(appParam);
//                            Object client =  systemMap.get("nckd_usergroup");
//                            logger.info("收款入账中心配置信息：{}",client);
//
//                            if(ObjectUtils.isEmpty(client)){
//                                // 如果企业没有配置默认的用户组，则获取最上层用户组
//                                // todo 暂时不执行
//                                logger.info("如果企业没有配置默认的用户组，暂时不执行认领操作");
//                                break;
//                            }
//                            // 获取到需要执行认领收款入账中心key 2024178764979648512
//                            QFilter qFilter2 = new QFilter("billno", QCP.equals, billno);
//                            DynamicObject beiIntelrec = BusinessDataServiceHelper.loadSingle("bei_intelrec", "id,billno", new QFilter[]{qFilter2});
//                            logger.info("收款入账中心信息：{}",beiIntelrec);
//                            Object pkValue = beiIntelrec.getPkValue();
//                            String failidString = pkValue.toString();
//                            JSONObject jsonObject = new JSONObject(client);
//                            Map<String,Object> noticeData = new HashMap<>();
//                            // 人员组 key
//                            Object masterid = jsonObject.get("id");
//                            // 组名
//                            Object name = jsonObject.getJSONObject("name").get("zh_CN");
//                            ArrayList<String> nameList = new ArrayList<>();
//                            nameList.add(name.toString());
//                            ArrayList<String> groupsids = new ArrayList<>();
//                            groupsids.add(masterid.toString());
//                            noticeData.put("usergroupnames",nameList);
//                            noticeData.put("usergroupids",groupsids);
//
//                            String jsonString = SerializationUtils.toJsonString(noticeData);
//                            String[] failids = failidString.split(",");
//                            Map<String, String> ruleNotice = new HashMap(failids.length);
//                            for(int i = 0; i < failids.length; ++i) {
//                                cache.put(failids[i], jsonString);
//                                ruleNotice.put(failids[i], jsonString);
//                            }
//
//                            Long[] ids = (Long[])Arrays.stream(failids).mapToLong(Long::valueOf).boxed().toArray((x$0) -> {
//                                return new Long[x$0];
//                            });
//                            OperationResult result = TmcOperateServiceHelper.execOperateWithoutThrow("pushandsave", "bei_transdetail_cas", ids, OperateOption.create());
//                            logger.info("认领结果：{}",result);
//                            // 推送认领通知到用户
//                            this.noticeMessage(ruleNotice);
//                            TXHandle tx = TX.requiresNew();
//                            Throwable var29 = null;
//
//                            try {
//                                CasFlowConfirmLogHelper.saveNoticeLog((List)Arrays.stream(ids).collect(Collectors.toList()));
//                            } catch (Throwable var22) {
//                                var29 = var22;
//                                throw var22;
//                            } finally {
//                                if (tx != null) {
//                                    if (var29 != null) {
//                                        try {
//                                            tx.close();
//                                        } catch (Throwable var21) {
//                                            var29.addSuppressed(var21);
//                                        }
//                                    } else {
//                                        tx.close();
//                                    }
//                                }
//
//                            }
//                        }

                    }
                }else{
                    logger.info("未查询到对应客户信息：{}",oppunit);
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
        BigDecimal number = customer.getBigDecimal("creditamount");
        // 设置两位小数的精度
        BigDecimal scaledNumber = number.setScale(2, RoundingMode.DOWN);

        messageInfo.setContent("收到"+customer.getString("oppunit")+"款项"+customer.get("currency.sign")+scaledNumber +",请确认对应的订单;");

        List<Long> userids = new ArrayList<Long>();
        userids.add((Long) salerid.getPkValue());
        logger.info("设置接收人成功：{}",userids);
        messageInfo.setUserIds(userids);
        // 发送人信息
        messageInfo.setSenderId((Long) customer.getDynamicObject("creator").getPkValue());
        logger.info("设置发送人成功：{}",(Long) customer.getDynamicObject("creator").getPkValue());
        messageInfo.setType(MessageInfo.TYPE_MESSAGE);
        messageInfo.setNotifyType("yunzhijia");
        messageInfo.setTag("银行流水");
        logger.info("发送信息体messageInfo:{}",messageInfo);

        long l = MessageCenterServiceHelper.sendMessage(messageInfo);
        logger.info("发送通知成功：{}",l);
    }
    /**
     * 获取用户信息
     */
    static DynamicObject loadSingle(Object userId) {
        DynamicObject useInfo = BusinessDataServiceHelper.loadSingle(userId, "bos_user");
        return useInfo;
    }

    /**
     * @deprecated  推送认领通知
     * @param ruleNotice
     */

    private void noticeMessage(Map<String, String> ruleNotice) {
        Map<Object, Map<String, List<Object>>> claimTypeMap = new HashMap(ruleNotice.size());
        Map<String, List<Object>> typeValue = null;
        Iterator var4 = ruleNotice.entrySet().iterator();

        while(var4.hasNext()) {
            Map.Entry<String, String> rule = (Map.Entry)var4.next();
            Map<String, Object> map = (Map)SerializationUtils.fromJsonString((String)rule.getValue(), Map.class);
            typeValue = new HashMap();
            typeValue.put("usergroupids", (List)map.get("usergroupids"));
            typeValue.put("orgids", (List)map.get("orgids"));
            typeValue.put("roleids", (List)map.get("roleids"));
            typeValue.put("userids", (List)map.get("userids"));
            claimTypeMap.put(rule.getKey(), typeValue);
        }

        if (ExtendConfigHelper.shouldSendClaimMsg()) {
            RecClaimHelper.sendClaimNoticeMessage(claimTypeMap, "notice");
        }

    }



}
