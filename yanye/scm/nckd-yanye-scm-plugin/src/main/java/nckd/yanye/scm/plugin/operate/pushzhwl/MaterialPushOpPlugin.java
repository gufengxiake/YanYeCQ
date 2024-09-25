package nckd.yanye.scm.plugin.operate.pushzhwl;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import nckd.yanye.scm.common.utils.HttpRequestUtils;

import java.util.List;


/**
 * 主数据-物料操作插件（推送智慧物流）
 * 表单标识：bd_material
 * author：xiaoxiaopeng
 * date：2024-09-25
 */
public class MaterialPushOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("modelnum");
        fieldKeys.add("group");
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
        if (resultToken == null){
            this.operationResult.setMessage("推送智慧物流失败");
            e.setCancel(true);
            return;
        }
        String accessToken = resultToken.getString("access_token");
        for (DynamicObject dataEntity : dataEntities) {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("InvCode", dataEntity.getString("number"));
            bodyJson.put("InvName", dataEntity.getString("name"));
            bodyJson.put("InvPK", dataEntity.getPkValue().toString());
            bodyJson.put("InvStd", dataEntity.getString("modelnum"));
            bodyJson.put("InvMainUnit", dataEntity.getDynamicObject("baseunit") == null ? null : dataEntity.getDynamicObject("baseunit").getString("name"));
            bodyJson.put("InvCCode", dataEntity.getDynamicObject("group") == null ? null : dataEntity.getDynamicObject("group").getString("number"));
            bodyJson.put("Status", "0");
            JSONObject result = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/Business/PushMaterial", bodyJson, accessToken);
            if (result != null && "1".equals(result.get("errCode").toString())){
                this.operationResult.setMessage("推送智慧物流失败"+ result.getString("errMsg"));
                e.setCancel(true);
            }else if (result != null && "0".equals(result.get("errCode").toString())){
                this.operationResult.setMessage("推送成功");
            }else {
                this.operationResult.setMessage("推送智慧物流失败");
                e.setCancel(true);
            }
        }
    }
}
