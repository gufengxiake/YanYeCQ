package nckd.yanye.scm.plugin.operate.pushzhwl;

import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import nckd.yanye.scm.common.utils.HttpRequestUtils;
import nckd.yanye.scm.common.utils.ZhWlUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应链-智慧物流对接单推送智慧物流按钮（推送智慧物流）
 * 表单标识：nckd_zhwl_docking
 * author：xiaoxiaopeng
 * date：2024-10-22
 */
public class ZhWlDockingPushOpPlugin extends AbstractOperationServicePlugIn {
    private static Log logger = LogFactory.getLog(ZhWlDockingPushOpPlugin.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_entryentity");
        fieldKeys.add("createtime");
        fieldKeys.add("nckd_customer");
        fieldKeys.add("nckd_comment");
        fieldKeys.add("nckd_qty");
        fieldKeys.add("nckd_pricefield");
        fieldKeys.add("nckd_unit");
        fieldKeys.add("nckd_materiel");
        fieldKeys.add("nckd_customer");
        fieldKeys.add("nckd_srcbillnumber");
        fieldKeys.add("nckd_comment");

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
        tokenMap.put("number","nckd_zhwl_docking");
        tokenMap.put("name","智慧物流对接单");
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
            DynamicObject entry = dataEntity.getDynamicObjectCollection("nckd_entryentity").get(0);
            //源单编码
            String srcBillNumber = entry.getString("nckd_srcbillnumber");
            if (StringUtils.isBlank(srcBillNumber)){
                logger.error("srcBillNumber is null,{}",entry);
                continue;
            }
            //上查销售订单
            DynamicObject salorder = BusinessDataServiceHelper.loadSingle("sm_salorder", "id,billno",
                    new QFilter[]{new QFilter("billno", QCP.equals, srcBillNumber)});
            logger.info("销售订单,{}",salorder);
            if (salorder == null){
                logger.error("salorder is null,{}",salorder);
                continue;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            //构建推送参数
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("PlanCode", srcBillNumber);//计划编号
            bodyJson.put("PlanPK", salorder.getPkValue());//计划主键
            bodyJson.put("PlanDate", simpleDateFormat.format(dataEntity.getDate("createtime")));//计划日期
            DynamicObject customer = dataEntity.getDynamicObject("nckd_customer");
            if (customer != null){
                bodyJson.put("CustomPK", customer.getPkValue());//客户主键
                bodyJson.put("CustomCode", customer.getString("number"));//客户编号
                bodyJson.put("CustomName", customer.getString("name"));//客户名称
            }
            DynamicObject materiel = entry.getDynamicObject("nckd_materiel");
            if (materiel != null){
                bodyJson.put("MaterialPK", materiel.getPkValue());//物料主键
                bodyJson.put("MaterialCode", materiel.getString("number"));//物料编号
                bodyJson.put("MaterialName", materiel.getString("name"));//物料名称
                bodyJson.put("SpecificationsModel", materiel.getString("modelnum"));//规格型号
            }
            bodyJson.put("Quantity",entry.getString("nckd_qty"));//计划数量
            bodyJson.put("Price",entry.getString("nckd_pricefield"));//单价
            bodyJson.put("MainUnit",entry.getDynamicObject("nckd_unit") == null ? null : entry.getDynamicObject("nckd_unit").getString("name"));//计量单位
            bodyJson.put("PlanRemark",dataEntity.getString("nckd_comment"));//计划备注
            //调用httpPOST发送请求
            JSONObject result = HttpRequestUtils.httpPost(ZhWlUtil.URL + "/api/Business/PushPlan", bodyJson, accessToken);
            logger.info("接口返回结果，{}",result);

            Map<String,Object> parmMap = new HashMap<>();
            parmMap.put("number","nckd_zhwl_docking");
            parmMap.put("name","智慧物流对接单");
            parmMap.put("creator", RequestContext.get().getCurrUserId());
            parmMap.put("nckd_system", "zhwl");
            parmMap.put("nckd_interfaceurl", ZhWlUtil.URL + "/api/Business/PushPlan");
            parmMap.put("createtime", new Date());
            parmMap.put("nckd_parameter", bodyJson.toJSONString());

            if (result != null && "1".equals(result.get("errCode").toString())){
                logger.error("推送智慧物流失败,{}",result.getString("errMsg"));
                this.operationResult.setMessage("推送智慧物流失败"+ result.getString("errMsg"));
                parmMap.put("nckd_returnparameter",result.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancel(true);
            }else if (result != null && "0".equals(result.get("errCode").toString())){
                this.operationResult.setMessage("推送成功");
                parmMap.put("nckd_returnparameter",result.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
            }else {
                this.operationResult.setMessage("推送智慧物流失败");
                e.setCancel(true);
            }
        }
    }
}
