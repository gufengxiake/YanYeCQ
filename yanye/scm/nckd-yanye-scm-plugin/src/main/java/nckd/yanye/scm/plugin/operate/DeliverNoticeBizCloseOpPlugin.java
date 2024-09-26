package nckd.yanye.scm.plugin.operate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.validate.AbstractValidator;
import nckd.yanye.scm.common.utils.HttpRequestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应链-发货通知单关闭操作插件
 * 表单标识：sm_delivernotice
 * author：xiaoxiaopeng
 * date：2024-09-26
 */

public class DeliverNoticeBizCloseOpPlugin extends AbstractOperationServicePlugIn {

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
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "物流状态非未执行，不允许关闭");
                continue;
            }
            //获取token
            Map<String,Object> tokenMap = new HashMap<>();
            tokenMap.put("UserName","30001");
            tokenMap.put("Password","123456");
            tokenMap.put("grant_type","password");
            String tokenJson = JSON.toJSONString(tokenMap);//map转String
            JSONObject tokenJsonObject = JSON.parseObject(tokenJson);//String转json
            JSONObject resultToken = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/token", tokenJsonObject,null);
            if (resultToken == null){
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }
            String accessToken = resultToken.getString("access_token");
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("DeliveryOrderCode", dataEntity.getString("billno"));
            JSONObject result = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/Business/Dispatch/GetSaleBillState", bodyJson, accessToken);
            if (result != null && "1".equals(result.get("errCode").toString())){
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }else if (result == null){
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }
            String resultMsg = result.getString("result");
            if (StringUtils.isEmpty(resultMsg)){
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "获取物流状态失败，请稍后再试");
                continue;
            }
            String erpStatus = getErpStatus(resultMsg);
            if (!"0".equals(erpStatus)){
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "对应派车单状态为：" + resultMsg + ",不允许关闭");
                continue;
            }
            JSONObject deResult = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/Business/Dispatch/EndSaleBill", bodyJson, accessToken);
            if (deResult != null && "1".equals(deResult.get("errCode").toString())){
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "删除派车单失败，不允许关闭");
            }else if (result == null) {
                e.setCancel(true);
                e.setCancelMessage("单据" + dataEntity.getString("billno") + "接口调用失败，请稍后再试");
            }
        }

    }

    private String getErpStatus(String status){
        String newStatus = "";
        switch (status){
            case "未执行":
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
