package nckd.yanye.scm.plugin.operate.pushzhwl;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import nckd.yanye.scm.common.utils.HttpRequestUtils;
import nckd.yanye.scm.common.utils.ZhWlUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 主数据-客户操作插件（推送智慧物流）
 * 表单标识：bd_customer
 * author：xiaoxiaopeng
 * date：2024-09-25
 */
public class CustomerPushOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("group");
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        JSONObject tokenJson = new JSONObject();
        tokenJson.put("UserName", ZhWlUtil.USERNAME);
        tokenJson.put("Password",ZhWlUtil.PASSWORD);
        tokenJson.put("grant_type",ZhWlUtil.GRANTTYPE);
        JSONObject resultToken = HttpRequestUtils.httpPost(ZhWlUtil.URL + "/api/token", tokenJson,null);

        Map<String,Object> tokenMap = new HashMap<>();
        tokenMap.put("number","bd_customer");
        tokenMap.put("name","客户");
        tokenMap.put("creator", RequestContext.get().getCurrUserId());
        tokenMap.put("nckd_system", "zhwl");
        tokenMap.put("nckd_interfaceurl", ZhWlUtil.URL + "/api/token");
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
        for (DynamicObject dataEntity : dataEntities) {
            dataEntity = BusinessDataServiceHelper.loadSingle(dataEntity.getPkValue(), "bd_customer");
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("VenCode", dataEntity.getString("number"));
            bodyJson.put("VenName", dataEntity.getString("name"));
            bodyJson.put("VenPK", dataEntity.getPkValue().toString());
            bodyJson.put("VenCCode", dataEntity.getDynamicObject("group") == null ? null : dataEntity.getDynamicObject("group").getString("number"));
            bodyJson.put("Status", "0");
            JSONObject result = HttpRequestUtils.httpPost(ZhWlUtil.URL + "/api/Business/PushCustom", bodyJson, accessToken);

            Map<String,Object> parmMap = new HashMap<>();
            parmMap.put("number","bd_customer");
            parmMap.put("name","客户");
            parmMap.put("creator", RequestContext.get().getCurrUserId());
            parmMap.put("nckd_system", "zhwl");
            parmMap.put("nckd_interfaceurl", ZhWlUtil.URL + "/api/Business/PushCustom");
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
