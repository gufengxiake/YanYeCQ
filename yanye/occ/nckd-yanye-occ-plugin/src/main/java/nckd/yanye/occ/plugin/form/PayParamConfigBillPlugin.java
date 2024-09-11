package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.occ.plugin.mis.sdk.AU011SDK;
import nckd.yanye.occ.plugin.task.UpdateCCBKeyTask;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
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
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        if ("nckd_paybank".equals(key)) {
            ChangeData[] changeSet = e.getChangeSet();
            ChangeData changeData = changeSet[0];
            Object newValue = changeData.getNewValue();
            //A 农业银行 B 建设银行
            if ("A".equals(newValue)) {
                this.getModel().deleteEntryData("nckd_entryentity");
                //创建农行分录
                createABCEntry();
            }
            if ("B".equals(newValue)) {
                this.getModel().deleteEntryData("nckd_entryentity");
                //创建建行分录
                createCCBEntry();
            }
        }
    }

    /**
     * 创建农行分录
     */
    private void createABCEntry() {
        DynamicObjectCollection nckdEntryentity = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_entryentity");
        DynamicObject dynamicObject1 = nckdEntryentity.addNew();
        dynamicObject1.set("nckd_payparamname", "商户私钥");
        dynamicObject1.set("nckd_payparamnbr", "privateKey");
        DynamicObject dynamicObject2 = nckdEntryentity.addNew();
        dynamicObject2.set("nckd_payparamname", "平台公钥");
        dynamicObject2.set("nckd_payparamnbr", "publicKey");
        DynamicObject dynamicObject3 = nckdEntryentity.addNew();
        dynamicObject3.set("nckd_payparamname", "店铺编号");
        dynamicObject3.set("nckd_payparamnbr", "mch_id");
        DynamicObject dynamicObject4 = nckdEntryentity.addNew();
        dynamicObject4.set("nckd_payparamname", "请求地址");
        dynamicObject4.set("nckd_payparamnbr", "url");
        DynamicObject dynamicObject5 = nckdEntryentity.addNew();
        dynamicObject5.set("nckd_payparamname", "gps");
        dynamicObject5.set("nckd_payparamnbr", "gps");
        DynamicObject dynamicObject6 = nckdEntryentity.addNew();
        dynamicObject6.set("nckd_payparamname", "终端设备号");
        dynamicObject6.set("nckd_payparamnbr", "terminal_id");
        this.getView().updateView("nckd_entryentity");
    }

    /**
     * 创建建行分录
     */
    private void createCCBEntry() {
        DynamicObjectCollection nckdEntryentity = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_entryentity");
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
        this.getView().updateView("nckd_entryentity");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        if ("nckd_secretkey".equals(itemKey)) {
            String enable = (String) this.getModel().getValue("enable");
            String status = (String) this.getModel().getValue("status");
            if (!"1".equals(enable) || !"C".equals(status)) {
                this.getView().showErrorNotification("必须单据是已审核且有效时才能触发建行密钥下载功能");
                return;
            }
            DynamicObjectCollection nckdEntryentity = this.getModel().getEntryEntity("nckd_entryentity");
            Map<String, String> map = nckdEntryentity.stream().collect(Collectors.toMap(k -> k.getString("nckd_payparamnbr"), v -> v.getString("nckd_payparamvalue")));
            String authCode = map.get("authCode");
            String merchantCode = map.get("merchantCode");
            String terminalId = map.get("terminalId");
            String url = map.get("url");
            String apiVer = map.get("apiVer");
            //必传条件不能为空
            boolean paramConfigResult = StringUtils.isNotEmpty(authCode) && StringUtils.isNotEmpty(merchantCode) && StringUtils.isNotEmpty(terminalId) && StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiVer);
            if (!paramConfigResult) {
                this.getView().showErrorNotification("请检查支付参数授权码，商户号，终端号，请求地址，版本号是否为空");
                return;
            }
            //调银行获取初始密钥接口
            String key = AU011SDK.au011(authCode, merchantCode, terminalId, url, apiVer);
            if (StringUtils.isNotEmpty(key)) {
                logger.info("PayParamConfigBillPlugin 建行初始密钥 " + key);
                if (StringUtils.isNotEmpty(key)) {
                    DynamicObject dataEntity = this.getModel().getDataEntity();
                    DynamicObjectCollection nckd_entryentity = dataEntity.getDynamicObjectCollection("nckd_entryentity");
                    DynamicObject dy = nckd_entryentity.get(0);
                    dy.set("nckd_payparamvalue", key);
                    SaveServiceHelper.update(dataEntity);
                    this.getView().updateView();
                    //第一次下载后，直接再调更新接口和确认接口进行激活
                    DynamicObject payParamConfig = this.getModel().getDataEntity();
                    UpdateCCBKeyTask.UpdateCCBKey(payParamConfig);
                    logger.info("PayParamConfigBillPlugin 建行密钥下载成功");
                    this.getView().showConfirm("建行密钥下载成功", MessageBoxOptions.OK);
                } else {
                    logger.info("PayParamConfigBillPlugin 建行密钥已下载,无需重复下载");
                    this.getView().showConfirm("建行密钥已下载,无需重复下载", MessageBoxOptions.OK);
                }
            } else {
                //请求失败
                logger.info("PayParamConfigBillPlugin 建行密钥下载失败 ");
                this.getView().showConfirm("建行密钥下载失败 ", MessageBoxOptions.OK);
            }
        }
        if ("nckd_updateccbkey".equals(itemKey)) {
            UpdateCCBKeyTask updateCCBKeyTask = new UpdateCCBKeyTask();
            RequestContext requestContext = RequestContext.get();
            Map<String, Object> map = new HashMap<>();
            Object number = this.getModel().getValue("number");
            map.put("payParamNumber", number);
            updateCCBKeyTask.execute(requestContext, map);
        }
    }
}
