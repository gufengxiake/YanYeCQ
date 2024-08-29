package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.AppInfo;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.param.AppParam;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Module           :财务云-资金-开票登记
 * Description      :取交票人全称名称搜索客户，若没有对应客户且参数【票据无供应商或客户信息是否允许保存】为否：报错提示【无客户信息不允许保存】
 *
 * @author : xiaoxiaopeng
 * @date : 2024/8/27
 */
public class ReceivableBillSubmit extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("payeetype");
        fieldKeys.add("deliver");
        fieldKeys.add("nckd_client");
        fieldKeys.add("draftbillno");
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);

    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);

        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] data = this.getDataEntities();
                for (ExtendedDataEntity dt : data) {
                    DynamicObject dataEntity = dt.getDataEntity();
                    String draftbillno = dataEntity.getString("draftbillno");
                    String payeetype = dataEntity.getString("payeetype");
                    if (StringUtils.isNotEmpty(draftbillno)) {
                        DynamicObject electronic = BusinessDataServiceHelper.loadSingle("cdm_electronic_sign_deal", "id,billno",
                                new QFilter[]{new QFilter("billno", QCP.equals, draftbillno)});
                        if (electronic != null) {
                            if ("bd_customer".equals(payeetype)){
                                DynamicObject deliver = dataEntity.getDynamicObject("deliver");
                                if (deliver != null){
                                    dataEntity.set("nckd_client",deliver.getPkValue());
                                    SaveServiceHelper.update(dataEntity);
                                }
                            }
                            return;
                        }
                    }
                    if (!"bd_customer".equals(payeetype)){
                        Long pkValue = (Long) dataEntity.getDynamicObject("company").getPkValue();
                        AppInfo appInfo = AppMetadataCache.getAppInfo("cdm");
                        String appId = appInfo.getId();
                        AppParam appParam = new AppParam();
                        appParam.setViewType("08");
                        appParam.setAppId(appId);
                        appParam.setOrgId(pkValue);
                        Map<String,Object> systemMap= SystemParamServiceHelper.loadAppParameterFromCache(appParam);
                        Boolean client = (Boolean) systemMap.get("nckd_whetherclient");
                        if(!client){
                            this.addErrorMessage(dt, String.format("单据%s：无客户信息不允许保存", dataEntity.getString("billno")));
                        }

                    }
                }
            }
        });

    }
}
