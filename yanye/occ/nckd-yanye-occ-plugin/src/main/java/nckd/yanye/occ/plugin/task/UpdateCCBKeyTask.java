package nckd.yanye.occ.plugin.task;

import cn.hutool.core.util.ObjectUtil;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.exception.KDBizException;
import kd.bos.exception.KDException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.api.MessageHandler;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.occ.plugin.mis.sdk.AU012SDK;
import nckd.yanye.occ.plugin.mis.sdk.AU013SDK;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module           :系统服务云-调度中心-调度执行程序
 * Description      :定时更新建设银行交易的密钥并确认 updateCCBKey	定时更新建设银行交易的密钥	小时	2024-08-23 10:23:03	2099-12-31 23:59:59	禁用	朱锦涛	2024-08-23 13:38:10
 *
 * @author : zhujintao
 * @date : 2024/8/23
 */
public class UpdateCCBKeyTask extends AbstractTask {
    private static final Log logger = LogFactory.getLog(UpdateCCBKeyTask.class);

    @Override
    public MessageHandler getMessageHandle() {
        return super.getMessageHandle();
    }

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        //获取传入的支付参数配置编码
        Object payparamNumber = map.get("payParamNumber");
        if (ObjectUtil.isNotEmpty(payparamNumber)) {
            logger.info("UpdateCCBKeyTask 根据支付参数的编码 " + payparamNumber + " 去更新支付参数配置的建行密钥");
            QFilter qFilter = new QFilter("number", QCP.equals, payparamNumber);
            qFilter.and("nckd_paybank", QCP.equals, "B").and("status", QCP.equals, "C").and("enable", QCP.equals, "1");
            DynamicObject payParamConfig = BusinessDataServiceHelper.loadSingle("nckd_payparamconfig", "id,number,nckd_paybank,nckd_entryentity.nckd_payparamname,nckd_entryentity.nckd_payparamnbr,nckd_entryentity.nckd_payparamvalue", qFilter.toArray());
            //拿到建行下载的密钥
            if (ObjectUtil.isNotEmpty(payParamConfig)) {
                logger.info("UpdateCCBKeyTask 获取到支付参数配置");
                UpdateCCBKey(payParamConfig);
            } else {
                logger.info("UpdateCCBKeyTask 没有获取到支付参数配置");
            }
        } else {
            logger.info("UpdateCCBKeyTask 全量更新支付参数配置的建行密钥start");
            QFilter qFilter = new QFilter("nckd_paybank", QCP.equals, "B")
                    .and("status", QCP.equals, "C").and("enable", QCP.equals, "1");
            DynamicObject[] payparamconfigArr = BusinessDataServiceHelper.load("nckd_payparamconfig", "id,number,nckd_paybank,nckd_entryentity.nckd_payparamname,nckd_entryentity.nckd_payparamnbr,nckd_entryentity.nckd_payparamvalue", qFilter.toArray());
            for (DynamicObject dynamicObject : payparamconfigArr) {
                UpdateCCBKey(dynamicObject);
            }
            logger.info("UpdateCCBKeyTask 全量更新支付参数配置的建行密钥end");
        }
    }

    /**
     * @param payparamconfig
     */
    public static void UpdateCCBKey(DynamicObject payparamconfig) {
        DynamicObjectCollection nckdEntryentity = payparamconfig.getDynamicObjectCollection("nckd_entryentity");
        Map<String, String> nckdEntryentityMap = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
        String oldKey = nckdEntryentityMap.get("key");
        String merchantCode = nckdEntryentityMap.get("merchantCode");
        String terminalId = nckdEntryentityMap.get("terminalId");
        String url = nckdEntryentityMap.get("url");
        String apiVer = nckdEntryentityMap.get("apiVer");
        boolean paramConfigResult = StringUtils.isNotEmpty(oldKey) && StringUtils.isNotEmpty(merchantCode) && StringUtils.isNotEmpty(terminalId) && StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiVer);
        if (!paramConfigResult) {
            logger.info("UpdateCCBKeyTask 请检查支付参数授权码，商户号，终端号，请求地址，版本号是否为空");
            throw new KDBizException("UpdateCCBKeyTask 请检查支付参数授权码，商户号，终端号，请求地址，版本号是否为空");
        }
        //在支付参数配置界面的建行密钥下载时初始key，需要拿初始key取获取待确认的key，待确认的key激活后才能使用
        String newKey = AU012SDK.au012(oldKey, merchantCode, terminalId, url, apiVer);
        if (StringUtils.isNotEmpty(newKey)) {
            //获取到待确认的key
            if (StringUtils.isEmpty(newKey)) {
                logger.info("UpdateCCBKeyTask 未获取建行交易密钥");
                throw new KDBizException("UpdateCCBKeyTask 未获取建行交易密钥");
            }
            //确认激活
            boolean result = AU013SDK.confirmKey(newKey, merchantCode, terminalId, url, apiVer);
            if (result) {
                //更新支付参数配置表
                DynamicObject dy = nckdEntryentity.get(0);
                dy.set("nckd_payparamvalue", newKey);
                SaveServiceHelper.update(payparamconfig);
                logger.info("UpdateCCBKeyTask " + payparamconfig.getString("number") + " 建行密钥确认成功");
            } else {
                logger.info("UpdateCCBKeyTask " + payparamconfig.getString("number") + " 建行密钥确认失败");
                throw new KDBizException("UpdateCCBKeyTask " + payparamconfig.getString("number") + " 建行密钥确认失败");
            }

        } else {
            //请求失败
            logger.info("UpdateCCBKeyTask 建行密钥更新失败");
            throw new KDBizException("UpdateCCBKeyTask 建行密钥更新失败");
        }
    }

    @Override
    public boolean isSupportReSchedule() {
        return super.isSupportReSchedule();
    }
}
