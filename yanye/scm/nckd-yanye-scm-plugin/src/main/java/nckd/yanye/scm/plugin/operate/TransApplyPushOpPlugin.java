package nckd.yanye.scm.plugin.operate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.DispatchServiceHelper;
import nckd.yanye.scm.common.utils.HttpRequestUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应链-调拨申请单推送智慧物流操作插件
 * 表单标识：im_transapply
 * author：xiaoxiaopeng
 * date：2024-09-09
 */

public class TransApplyPushOpPlugin extends AbstractOperationServicePlugIn {
    private static final Log log = LogFactory.getLog(TransApplyPushOpPlugin.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("billno");
        fieldKeys.add("nckd_biztype");
        fieldKeys.add("createtime");
        fieldKeys.add("nckd_cartype");
        fieldKeys.add("nckd_address");
        fieldKeys.add("nckd_car");
        fieldKeys.add("nckd_dirver");
        fieldKeys.add("nckd_iscontainer");
        fieldKeys.add("nckd_closestatus");
        fieldKeys.add("comment");
        fieldKeys.add("billentry");
        fieldKeys.add("billentry.material");
        fieldKeys.add("billentry.inwarehouse");
        fieldKeys.add("auxpty");
    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    DynamicObject data = dataEntity.getDataEntity();
                    DynamicObjectCollection billentry = data.getDynamicObjectCollection("billentry");
                    if (billentry.size() > 1){
                        log.error("单据：" + data.getString("billno") + "的明细行有多条明细，不允许下推",billentry);
                        this.addErrorMessage(dataEntity, String.format("单据：%s的明细行有多条明细，不允许下推",data.getString("billno")));
                    }
                }
            }
        });
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < dataEntities.length; i++) {
            DynamicObject dataEntity = dataEntities[i];
            Map<String, Object> map = new HashMap<>();
            String id = dataEntity.getString("id");
            String bizType = dataEntity.getString("nckd_biztype");
            String billno = dataEntity.getString("billno");
            Date createtime = dataEntity.getDate("createtime");
            String tranSportType = setSendName(dataEntity.getString("nckd_cartype"));
            DynamicObject address = dataEntity.getDynamicObject("nckd_address");
            DynamicObject vehicle = dataEntity.getDynamicObject("nckd_car");
            DynamicObject driver = dataEntity.getDynamicObject("nckd_dirver");
            if (driver != null){
                driver = BusinessDataServiceHelper.loadSingle(driver.getPkValue(),"nckd_driver");
            }
            String isContainer = dataEntity.getString("nckd_iscontainer");
            String closeStatus = dataEntity.getString("nckd_closestatus");
            String comment = dataEntity.getString("comment");
            DynamicObject billentry = dataEntity.getDynamicObjectCollection("billentry").get(0);
            DynamicObject material = billentry.getDynamicObject("material");
            //material = BusinessDataServiceHelper.loadSingle(material.getPkValue(), "bd_materialsalinfo");
            DynamicObject masterid = material.getDynamicObject("masterid");
            masterid = BusinessDataServiceHelper.loadSingle(masterid.getPkValue(), "bd_material");
            DynamicObject warehouse = billentry.getDynamicObject("inwarehouse");
            DynamicObject auxpty = billentry.getDynamicObject("auxpty");



            map.put("DocType", "调拨申请单");//单据类型
            map.put("BizType", StringUtils.isEmpty(bizType) ? null : bizType);//业务类型 SL:散装销售 SP:袋装销售
            map.put("OrderCode", billno);//发货单编号
            map.put("OrderPK",id);//单据头主键
            map.put("OrderDate",simpleDateFormat.format(createtime));//发货单日期
            map.put("SendName",tranSportType);//运输方式名称
            map.put("VroutePK",address == null ? null : address.getString("id"));//运输线路主键
            map.put("VrouteCode",address == null ? null : address.getString("number"));//运输线路编号
            map.put("VrouteName",address == null ? null : address.getString("name"));//运输线路名称
            map.put("CarNumber",vehicle == null ? null : vehicle.getString("name"));//车牌号
            map.put("DriverName",driver == null ? null : driver.getString("name"));//司机姓名
            map.put("DriverID",driver == null ? null : driver.getString("nckd_idcardno"));//司机身份证号
            map.put("TelPhone",driver == null ? null : driver.getString("nckd_phonenumber"));//手机号
            map.put("IsContainer", StringUtils.isEmpty(isContainer) ? null : isContainer);//是否集装箱0:否 1:是
            map.put("EndSign", StringUtils.isEmpty(closeStatus) ? null : closeStatus);//关闭状态 A:正常 B:关闭
            map.put("OrderRemark",comment);//发货单备注
            map.put("OrderRowNo",1);//发货单分录号
            map.put("OrderRowPK",billentry.getString("id"));//发货单分录主键
            map.put("MaterialPK",masterid.getString("id"));//物料主键
            map.put("MaterialCode",masterid.getString("number"));//物料编号
            map.put("MaterialName",masterid.getString("name"));//物料名称
            map.put("SpecificationsModel",masterid.getString("modelnum"));//规格型号
            //map.put("LevelName",);//品级规格
            map.put("ZRStorPK",warehouse.getString("id"));//转入仓库主键
            map.put("ZRStorCode",warehouse.getString("number"));//转入仓库编号
            map.put("ZRStorName",warehouse.getString("name"));//转入仓库名称
            if (auxpty != null && "004".equals(auxpty.getDynamicObjectType().getName())){
                DynamicObject flexauxprop = BusinessDataServiceHelper.loadSingle("bd_flexauxprop_bd", "hg,auxproptype,auxpropval",new QFilter[]{new QFilter("hg", QCP.equals,auxpty.getPkValue())});
                DynamicObject flex = BusinessDataServiceHelper.loadSingle("bos_flex_property", "valuesource", new QFilter[]{new QFilter("flexfield", QCP.equals, flexauxprop.get("auxproptype"))});
                String dateType = flex.getDynamicObject("valuesource").getString("number");
                String dateTypeId = flexauxprop.getString("auxpropval");
                DynamicObject dy = BusinessDataServiceHelper.loadSingle(dateTypeId,dateType);
                map.put("LevelName",dy.getString("name"));//品级规格
            }


            //获取token
            JSONObject tokenJson = new JSONObject();
            tokenJson.put("UserName","30001");
            tokenJson.put("Password","123456");
            tokenJson.put("grant_type","password");
            JSONObject resultToken = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/token", tokenJson,null);

            Map<String,Object> tokenMap = new HashMap<>();
            tokenMap.put("number","im_transapply");
            tokenMap.put("name","调拨申请单");
            tokenMap.put("creator", RequestContext.get().getCurrUserId());
            tokenMap.put("nckd_system", "zhwl");
            tokenMap.put("nckd_interfaceurl", "http://5zb5775265qa.vicp.fun/api/token");
            tokenMap.put("createtime", new Date());
            tokenMap.put("nckd_parameter", tokenJson.toJSONString());

            if (resultToken == null){
                log.error("调用智慧物流接口失败{}",resultToken);
                tokenMap.put("nckd_returnparameter",null);
                HttpRequestUtils.setGeneralLog(tokenMap);
                e.setCancelMessage("调用智慧物流接口失败");
                e.setCancel(true);
                return;
            }
            tokenMap.put("nckd_returnparameter",resultToken.toJSONString());
            HttpRequestUtils.setGeneralLog(tokenMap);

            Map<String, Object> resultMap = resultToken.getInnerMap();
            String accessToken = resultMap.get("access_token").toString();

            //推送发货通知单
            String json = JSON.toJSONString(map);//map转String
            JSONObject jsonObject = JSON.parseObject(json);//String转json
            JSONObject result = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/Business/PushTransfer", jsonObject, accessToken);

            Map<String,Object> parmMap = new HashMap<>();
            parmMap.put("number","im_transapply");
            parmMap.put("name","调拨申请单");
            parmMap.put("creator", RequestContext.get().getCurrUserId());
            parmMap.put("nckd_system", "zhwl");
            parmMap.put("nckd_interfaceurl", "http://5zb5775265qa.vicp.fun/api/Business/PushTransfer");
            parmMap.put("createtime", new Date());
            parmMap.put("nckd_parameter", jsonObject.toJSONString());

            if (result != null && "1".equals(result.get("errCode").toString())){
                parmMap.put("nckd_returnparameter",result.toJSONString());
                HttpRequestUtils.setGeneralLog(parmMap);
                e.setCancelMessage("推送智慧物流派车单失败：" + result.getString("errMsg"));
                e.setCancel(true);
                return;
            }
            parmMap.put("nckd_returnparameter",result.toJSONString());
            HttpRequestUtils.setGeneralLog(parmMap);
        }
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
    }

    private String setSendName(String nckdTransporttype) {
        if (StringUtils.isEmpty(nckdTransporttype)){
            return null;
        }
        String sendName = "";
        switch (nckdTransporttype){
            case "A":
                sendName = "船运";
                break;
            case "B":
                sendName = "汽运";
                break;
            case "C":
                sendName = "火车";
                break;
        }
        return sendName;
    }
}
