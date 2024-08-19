package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.AppInfo;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.param.AppParam;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;

import java.util.Map;

/**
 * 开票登记保存操作插件
 */
public class payableBillSave extends AbstractOperationServicePlugIn {

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        DynamicObject[] data = e.getDataEntities();
        for (DynamicObject dt : data) {
            String payeetype = dt.getString("payeetype");
            if (!"bd_supplier".equals(payeetype)){
                continue;
            }
            DynamicObject receiver = dt.getDynamicObject("receiver");
            if (receiver != null){
                DynamicObject supplier = BusinessDataServiceHelper.loadSingle(receiver.getPkValue(), "bd_supplier");
                Long pkValue = (Long) dt.getDynamicObject("drawercompany").getPkValue();
                AppInfo appInfo = AppMetadataCache.getAppInfo("cdm");
                String appId = appInfo.getId();
                AppParam appParam = new AppParam();
                appParam.setViewType("08");
                appParam.setAppId(appId);
                appParam.setOrgId(pkValue);
                Map <String,Object> systemMap= SystemParamServiceHelper.loadAppParameterFromCache(appParam);
                Object client =  systemMap.get("nckd_whetherclient");
                DynamicObject nckdVendor = dt.getDynamicObject("nckd_vendor");
                if(nckdVendor == null && client.equals("true")){
                    this.operationResult.setMessage("无供应商信息不允许保存");
                    return;
                }
                if (supplier == null && client.equals("false")){
                    this.operationResult.setMessage("无供应商信息不允许保存");
                    return;
                }
            }
        }


    }
}
