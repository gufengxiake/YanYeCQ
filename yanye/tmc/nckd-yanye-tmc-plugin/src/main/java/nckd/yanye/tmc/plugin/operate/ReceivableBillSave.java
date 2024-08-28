package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.AppInfo;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.param.AppParam;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.validate.ErrorLevel;
import kd.bos.entity.validate.ValidationErrorInfo;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;

import java.util.Map;

/**
 * Module           :财务云-资金-开票登记
 * Description      :取交票人全称名称搜索客户，若没有对应客户且参数【票据无供应商或客户信息是否允许保存】为否：报错提示【无客户信息不允许保存】
 *
 * @author : xiaoxiaopeng
 * @date : 2024/8/27
 */
public class ReceivableBillSave extends AbstractOperationServicePlugIn {

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] data = e.getDataEntities();
        for (DynamicObject dt : data) {
            String payeetype = dt.getString("payeetype");
            if (!"bd_customer".equals(payeetype)){
                Long pkValue = (Long) dt.getDynamicObject("company").getPkValue();
                AppInfo appInfo = AppMetadataCache.getAppInfo("cdm");
                String appId = appInfo.getId();
                AppParam appParam = new AppParam();
                appParam.setViewType("08");
                appParam.setAppId(appId);
                appParam.setOrgId(pkValue);
                Map<String,Object> systemMap= SystemParamServiceHelper.loadAppParameterFromCache(appParam);
                Boolean client = (Boolean) systemMap.get("nckd_whetherclient");
                if(!client){
                    e.setCancel(true);
                    e.setCancelMessage("无客户信息不允许保存");
                    return;
                }
            }
        }
    }
}
