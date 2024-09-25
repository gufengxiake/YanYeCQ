package nckd.yanye.scm.plugin.operate.pushzhwl;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import nckd.yanye.scm.common.utils.HttpRequestUtils;

import java.util.List;


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
            bodyJson.put("VenCode", dataEntity.getString("number"));
            bodyJson.put("VenName", dataEntity.getString("name"));
            bodyJson.put("VenPK", dataEntity.getPkValue().toString());
            bodyJson.put("VenCCode", dataEntity.getDynamicObjectCollection("entry_groupstandard").size() < 1 ? null : dataEntity.getDynamicObjectCollection("entry_groupstandard").get(0).getDynamicObject("standardid").getString("number"));
            bodyJson.put("Status", "0");
            JSONObject result = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/Business/PushCustom", bodyJson, accessToken);
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
