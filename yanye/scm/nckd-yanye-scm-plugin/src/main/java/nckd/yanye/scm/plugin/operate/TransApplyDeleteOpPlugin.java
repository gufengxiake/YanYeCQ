package nckd.yanye.scm.plugin.operate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.utils.HttpRequestUtils;
import nckd.yanye.scm.common.utils.ZhWlUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应链-调拨申请单删除操作插件
 * 表单标识：im_transapply
 * author：xiaoxiaopeng
 * date：2024-09-26
 */

public class TransApplyDeleteOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_erpstatus");
    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {

            }
        });
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            String erpstatus = dataEntity.getString("nckd_erpstatus");
            if (StringUtils.isEmpty(erpstatus) || !"0".equals(erpstatus)) {
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "物流状态非未执行，不允许删除");
                continue;
            }
            //获取token
            JSONObject tokenJson = new JSONObject();
            tokenJson.put("UserName", ZhWlUtil.USERNAME);
            tokenJson.put("Password",ZhWlUtil.PASSWORD);
            tokenJson.put("grant_type",ZhWlUtil.GRANTTYPE);
            JSONObject resultToken = HttpRequestUtils.httpPost(ZhWlUtil.URL + "/api/token", tokenJson,null);

            Map<String,Object> tokenMap = new HashMap<>();
            tokenMap.put("number","im_transapply");
            tokenMap.put("name","调拨申请单");
            tokenMap.put("creator", RequestContext.get().getCurrUserId());
            tokenMap.put("nckd_system", "zhwl");
            tokenMap.put("nckd_interfaceurl", ZhWlUtil.URL + "/api/token");
            tokenMap.put("createtime", new Date());
            tokenMap.put("nckd_parameter", tokenJson.toJSONString());

            if (resultToken == null){
                tokenMap.put("nckd_returnparameter",null);
                HttpRequestUtils.setGeneralLog(tokenMap);
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }
            tokenMap.put("nckd_returnparameter",resultToken.toJSONString());
            HttpRequestUtils.setGeneralLog(tokenMap);

            String accessToken = resultToken.getString("access_token");
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("DeliveryOrderCode", dataEntity.getString("billno"));
            JSONObject result = HttpRequestUtils.httpPost(ZhWlUtil.URL + "/api/Business//GetSaleBillState", bodyJson, accessToken);

            Map<String,Object> parmMap = new HashMap<>();
            parmMap.put("number","im_transapply");
            parmMap.put("name","调拨申请单");
            parmMap.put("creator", RequestContext.get().getCurrUserId());
            parmMap.put("nckd_system", "zhwl");
            parmMap.put("nckd_interfaceurl", ZhWlUtil.URL + "/api/Business/GetSaleBillState");
            parmMap.put("createtime", new Date());
            parmMap.put("nckd_parameter", bodyJson.toJSONString());

            if (result != null && "1".equals(result.get("errCode").toString())){
                parmMap.put("nckd_returnparameter",result.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }else if (result == null){
                parmMap.put("nckd_returnparameter",null);
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }
            String resultMsg = result.getString("result");
            if (StringUtils.isEmpty(resultMsg)){
                parmMap.put("nckd_returnparameter",result.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }
            parmMap.put("nckd_returnparameter",result.toJSONString());
            HttpRequestUtils.setGeneralLog(parmMap);

            String erpStatus = getErpStatus(resultMsg);
            if (!"0".equals(erpStatus)){
                dataEntity.set("nckd_erpstatus", erpStatus);
                SaveServiceHelper.update(dataEntity);
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "对应派车单状态为：" + resultMsg + ",不允许删除");
                continue;
            }
            JSONObject deResult = HttpRequestUtils.httpPost(ZhWlUtil.URL + "/api/Business/EndSaleBill", bodyJson, accessToken);

            Map<String,Object> deMap = new HashMap<>();
            deMap.put("number","im_transapply");
            deMap.put("name","调拨申请单");
            deMap.put("creator", RequestContext.get().getCurrUserId());
            deMap.put("nckd_system", "zhwl");
            deMap.put("nckd_interfaceurl", ZhWlUtil.URL + "/api/Business/EndSaleBill");
            deMap.put("createtime", new Date());
            deMap.put("nckd_parameter", bodyJson.toJSONString());

            if (deResult != null && "1".equals(deResult.get("errCode").toString())){
                parmMap.put("nckd_returnparameter",deResult.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "删除派车单失败，不允许删除");
                continue;
            }else if (deResult == null) {
                parmMap.put("nckd_returnparameter",null);
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "接口调用失败，请稍后再试");
                continue;
            }
            parmMap.put("nckd_returnparameter",deResult.toJSONString());
            HttpRequestUtils.setGeneralLog(parmMap);
        }

    }

    private String getErpStatus(String status){
        String newStatus = "";
        switch (status){
            case "未使用":
                newStatus = "0";
                break;
            case "司机签到排队中":
                newStatus = "1";
                break;
            case "待过一磅":
                newStatus = "2";
                break;
            case "待装卸车":
                newStatus = "3";
                break;
            case "在装卸车":
                newStatus = "4";
                break;
            case "待过二磅":
                newStatus = "5";
                break;
            case "待出厂":
                newStatus = "6";
                break;
            case "已出厂":
                newStatus = "7";
                break;
            case "空车出厂":
                newStatus = "8";
                break;
        }
        return newStatus;
    }
}
