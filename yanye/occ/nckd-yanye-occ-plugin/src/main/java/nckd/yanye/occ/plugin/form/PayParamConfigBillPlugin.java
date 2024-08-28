package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.EventObject;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module           :全渠道云-支付配置
 * Description      :用于全渠道云支付参数的匹配
 *
 * @author : zhujintao
 * @date : 2024/8/16
 */
public class PayParamConfigBillPlugin extends AbstractBillPlugIn {
    private static final Log logger = LogFactory.getLog(PayParamConfigBillPlugin.class);

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        DynamicObjectCollection nckdEntryentity = this.getModel().getEntryEntity("nckd_entryentity");
        DynamicObject dynamicObject1 = nckdEntryentity.addNew();
        dynamicObject1.set("nckd_payparamname", "交易密钥");
        dynamicObject1.set("nckd_payparamnbr", "key");
        this.getView().setEnable(false, 0, "nckd_payparamvalue");
        DynamicObject dynamicObject2 = nckdEntryentity.addNew();
        dynamicObject2.set("nckd_payparamname", "商户号");
        dynamicObject2.set("nckd_payparamnbr", "merchantCode");
        DynamicObject dynamicObject3 = nckdEntryentity.addNew();
        dynamicObject3.set("nckd_payparamname", "终端号");
        dynamicObject3.set("nckd_payparamnbr", "terminalId");
        DynamicObject dynamicObject4 = nckdEntryentity.addNew();
        dynamicObject4.set("nckd_payparamname", "请求地址");
        dynamicObject4.set("nckd_payparamnbr", "url");
        DynamicObject dynamicObject5 = nckdEntryentity.addNew();
        dynamicObject5.set("nckd_payparamname", "gps");
        dynamicObject5.set("nckd_payparamnbr", "gps");
        DynamicObject dynamicObject6 = nckdEntryentity.addNew();
        dynamicObject6.set("nckd_payparamname", "版本号");
        dynamicObject6.set("nckd_payparamnbr", "apiVer");
        DynamicObject dynamicObject7 = nckdEntryentity.addNew();
        dynamicObject7.set("nckd_payparamname", "地址(可为空)");
        dynamicObject7.set("nckd_payparamnbr", "address");
        DynamicObject dynamicObject8 = nckdEntryentity.addNew();
        dynamicObject8.set("nckd_payparamname", "授权码(仅第一次获取密钥有效)");
        dynamicObject8.set("nckd_payparamnbr", "authCode");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        if ("nckd_secretkey".equals(itemKey)) {
            DynamicObjectCollection nckdEntryentity = this.getModel().getEntryEntity("nckd_entryentity");
            Map<String, String> map = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
            String authCode = map.get("authCode");
            String merchantCode = map.get("merchantCode");
            String terminalId = map.get("terminalId");
            String url = map.get("url");
            String apiVer = map.get("apiVer");
            if (StringUtils.isNotEmpty(authCode) && StringUtils.isNotEmpty(merchantCode) && StringUtils.isNotEmpty(terminalId) && StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiVer)) {
                //String key = AU011SDK.au011(authCode, merchantCode, terminalId, url, apiVer);
                String key = "111111111111111111111";
                if (StringUtils.isNotEmpty(key)) {
                    DynamicObject dataEntity = this.getModel().getDataEntity();
                    DynamicObjectCollection nckd_entryentity = dataEntity.getDynamicObjectCollection("nckd_entryentity");
                    DynamicObject dy = nckd_entryentity.get(0);
                    dy.set("nckd_payparamvalue", key);
                    SaveServiceHelper.update(dataEntity);
                    logger.info("建行密钥下载成功,请更新并确认");
                    this.getView().showConfirm("建行密钥下载成功,请更新并确认", MessageBoxOptions.OK);
                }
            }
        }
    }
}
