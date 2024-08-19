package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.AppInfo;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.param.AppParam;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;

import java.util.Map;

/**
 * 收票登记保存操作插件
 */
public class ReceivableBillSave extends AbstractOperationServicePlugIn {

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        DynamicObject[] data = e.getDataEntities();
        for (DynamicObject dt : data) {
            String payeetype = dt.getString("payeetype");
            if (!"bd_customer".equals(payeetype)){
                continue;
            }
            DynamicObject deliver = dt.getDynamicObject("deliver");
            if (deliver != null){
                DynamicObject customer = BusinessDataServiceHelper.loadSingle(deliver.getPkValue(), "bd_customer");
                Long pkValue = (Long) dt.getDynamicObject("company").getPkValue();
                AppInfo appInfo = AppMetadataCache.getAppInfo("cdm");
                String appId = appInfo.getId();
                AppParam appParam = new AppParam();
                appParam.setViewType("08");
                appParam.setAppId(appId);
                appParam.setOrgId(pkValue);
                Map<String,Object> systemMap= SystemParamServiceHelper.loadAppParameterFromCache(appParam);
                Object client =  systemMap.get("nckd_whetherclient");
                DynamicObject nckdclient = dt.getDynamicObject("nckd_client");
                if(nckdclient == null && client.equals("true")){
                    this.operationResult.setMessage("无客户信息不允许保存");
                    return;
                }
                if (customer == null && client.equals("false")){
                    this.operationResult.setMessage("无客户信息不允许保存");
                    return;
                }
            }
        }


    }
}
