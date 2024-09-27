package nckd.yanye.scm.plugin.operate.pushzhwl;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import nckd.yanye.scm.common.utils.HttpRequestUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 主数据-车辆信息操作插件（推送智慧物流）
 * 表单标识：nckd_vehicle
 * author：xiaoxiaopeng
 * date：2024-09-25
 */
public class VehiclePushOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_load");
        fieldKeys.add("nckd_pz");
        fieldKeys.add("nckd_axles");
        fieldKeys.add("nckd_emission");
        fieldKeys.add("nckd_type");
        fieldKeys.add("nckd_brand");
        fieldKeys.add("nckd_indentifition");
        fieldKeys.add("nckd_engineno");
        fieldKeys.add("nckd_registerdate");
        fieldKeys.add("nckd_licensdate");
        fieldKeys.add("nckd_serviceno");
        fieldKeys.add("nckd_servicedate");
        fieldKeys.add("nckd_drivingno");
        fieldKeys.add("nckd_drivingdate");
        fieldKeys.add("nckd_translicense");
        fieldKeys.add("nckd_translicensedate");
        fieldKeys.add("nckd_length");
        fieldKeys.add("nckd_width");
        fieldKeys.add("nckd_height");
        fieldKeys.add("nckd_lengthconct");
        fieldKeys.add("nckd_loadheight");
        fieldKeys.add("nckd_step");
        fieldKeys.add("nckd_stepheight");
        fieldKeys.add("nckd_ljqty");
        fieldKeys.add("nckd_environmental");
        fieldKeys.add("nckd_drivingpicture");
        fieldKeys.add("nckd_headpicture");
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        JSONObject tokenJson = new JSONObject();
        tokenJson.put("UserName","30001");
        tokenJson.put("Password","123456");
        tokenJson.put("grant_type","password");
        JSONObject resultToken = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/token", tokenJson,null);

        Map<String,Object> tokenMap = new HashMap<>();
        tokenMap.put("number","nckd_vehicle");
        tokenMap.put("name","车辆信息");
        tokenMap.put("creator", RequestContext.get().getCurrUserId());
        tokenMap.put("nckd_system", "zhwl");
        tokenMap.put("nckd_interfaceurl", "http://5zb5775265qa.vicp.fun/api/token");
        tokenMap.put("createtime", new Date());
        tokenMap.put("nckd_parameter", tokenJson.toJSONString());

        if (resultToken == null){
            this.operationResult.setMessage("推送智慧物流失败");
            tokenMap.put("nckd_returnparameter",null);
            HttpRequestUtils.setGeneralLog(tokenMap);
            e.setCancel(true);
            return;
        }
        tokenMap.put("nckd_returnparameter",resultToken.toJSONString());
        HttpRequestUtils.setGeneralLog(tokenMap);

        String accessToken = resultToken.getString("access_token");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (DynamicObject dataEntity : dataEntities) {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("CarNumber", dataEntity.getString("name"));
            bodyJson.put("LimitQty", dataEntity.getBigDecimal("nckd_load"));
            bodyJson.put("Tare", dataEntity.getBigDecimal("nckd_pz"));
            bodyJson.put("AxleNumber", dataEntity.getBigDecimal("nckd_axles"));
            bodyJson.put("EmissionStandard", dataEntity.getString("nckd_emission"));
            bodyJson.put("VehicleType", dataEntity.getString("nckd_type"));
            bodyJson.put("BrandModel", dataEntity.getString("nckd_brand"));
            bodyJson.put("VehicleIdentificationNumber", dataEntity.getString("nckd_indentifition"));
            bodyJson.put("EngineNo", dataEntity.getString("nckd_engineno"));
            bodyJson.put("RegisterDate", dataEntity.getDate("nckd_registerdate") == null ? null : simpleDateFormat.format(dataEntity.getDate("nckd_registerdate")));
            bodyJson.put("IssueDate", dataEntity.getDate("nckd_licensdate") == null ? null : simpleDateFormat.format(dataEntity.getDate("nckd_licensdate")));
            bodyJson.put("BusinessLicense", dataEntity.getString("nckd_serviceno"));
            bodyJson.put("BusinessLicenseLimitDate", dataEntity.getDate("nckd_servicedate") == null ? null : simpleDateFormat.format(dataEntity.getDate("nckd_servicedate")));
            bodyJson.put("DrivingLicense", dataEntity.getString("nckd_drivingno"));
            bodyJson.put("DrivingLicenseLimitDate", dataEntity.getDate("nckd_drivingdate") == null ? null : simpleDateFormat.format(dataEntity.getDate("nckd_drivingdate")));
            bodyJson.put("RoadTransportCertificate", dataEntity.getString("nckd_translicense"));
            bodyJson.put("RoadTransportCertificateLimitDate", dataEntity.getDate("nckd_translicensedate") == null ? null : simpleDateFormat.format(dataEntity.getDate("nckd_translicensedate")));
            bodyJson.put("CarLength", dataEntity.getBigDecimal("nckd_length"));
            bodyJson.put("CarWidth", dataEntity.getBigDecimal("nckd_width"));
            bodyJson.put("CarHeight", dataEntity.getBigDecimal("nckd_height"));
            bodyJson.put("TotalLength", dataEntity.getBigDecimal("nckd_lengthconct"));
            bodyJson.put("PlateHeight", dataEntity.getBigDecimal("nckd_loadheight"));
            bodyJson.put("StepPos", dataEntity.getBigDecimal("nckd_step"));
            bodyJson.put("StepHeight", dataEntity.getBigDecimal("nckd_stepheight"));
            bodyJson.put("TieBarNum", dataEntity.getBigDecimal("nckd_ljqty"));
            bodyJson.put("ImgUrl1", dataEntity.getString("nckd_environmental"));
            bodyJson.put("ImgUrl2", dataEntity.getString("nckd_drivingpicture"));
            bodyJson.put("ImgUrl3", dataEntity.getString("nckd_headpicture"));

            JSONObject result = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/Business/PushVehicle", bodyJson, accessToken);

            Map<String,Object> parmMap = new HashMap<>();
            parmMap.put("number","nckd_vehicle");
            parmMap.put("name","车辆信息");
            parmMap.put("creator", RequestContext.get().getCurrUserId());
            parmMap.put("nckd_system", "zhwl");
            parmMap.put("nckd_interfaceurl", "http://5zb5775265qa.vicp.fun/api/Business/PushVehicle");
            parmMap.put("createtime", new Date());
            parmMap.put("nckd_parameter", bodyJson.toJSONString());

            if (result != null && "1".equals(result.get("errCode").toString())){
                this.operationResult.setMessage("推送智慧物流失败"+ result.getString("errMsg"));
                parmMap.put("nckd_returnparameter",result.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancel(true);
            }else if (result != null && "0".equals(result.get("errCode").toString())){
                parmMap.put("nckd_returnparameter",result.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
                this.operationResult.setMessage("推送成功");
            }else {
                this.operationResult.setMessage("推送智慧物流失败");
                e.setCancel(true);
            }
        }
    }
}
