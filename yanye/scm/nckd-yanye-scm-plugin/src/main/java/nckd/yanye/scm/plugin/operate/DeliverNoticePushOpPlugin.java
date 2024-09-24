package nckd.yanye.scm.plugin.operate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import kd.bos.servicehelper.BusinessDataServiceHelper;
import nckd.yanye.scm.common.utils.HttpRequestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 供应链-发货通知单推送智慧物流操作插件
 * 表单标识：sm_delivernotice
 * author：xiaoxiaopeng
 * date：2024-09-09
 */

public class DeliverNoticePushOpPlugin extends AbstractOperationServicePlugIn {
    private static final Log log = LogFactory.getLog(DeliverNoticePushOpPlugin.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("billno");
        fieldKeys.add("nckd_biztype");
        fieldKeys.add("createtime");
        fieldKeys.add("customer");
        fieldKeys.add("nckd_transporttype");
        fieldKeys.add("operator");
        fieldKeys.add("nckd_customer");
        fieldKeys.add("nckd_vehicle");
        fieldKeys.add("nckd_driver");
        fieldKeys.add("nckd_iscontainer");
        fieldKeys.add("closestatus");
        fieldKeys.add("comment");
        fieldKeys.add("billentry");
        fieldKeys.add("billentry.material");
        fieldKeys.add("billentry.warehouse");
        fieldKeys.add("billentry.qty");
        fieldKeys.add("billentry.unit");
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
            DynamicObject customer = dataEntity.getDynamicObject("customer");
            String tranSportType = setSendName(dataEntity.getString("nckd_transporttype"));
            DynamicObject operator = dataEntity.getDynamicObject("operator");
            DynamicObject nckdCustomer = dataEntity.getDynamicObject("nckd_customer");
            DynamicObject vehicle = dataEntity.getDynamicObject("nckd_vehicle");
            DynamicObject driver = dataEntity.getDynamicObject("nckd_driver");
            if (driver != null){
                driver = BusinessDataServiceHelper.loadSingle(driver.getPkValue(),"nckd_driver");
            }
            String isContainer = dataEntity.getString("nckd_iscontainer");
            String closeStatus = dataEntity.getString("closestatus");
            String comment = dataEntity.getString("comment");
            DynamicObject billentry = dataEntity.getDynamicObjectCollection("billentry").get(0);
            DynamicObject material = billentry.getDynamicObject("material");
            //material = BusinessDataServiceHelper.loadSingle(material.getPkValue(), "bd_materialsalinfo");
            DynamicObject masterid = material.getDynamicObject("masterid");
            masterid = BusinessDataServiceHelper.loadSingle(masterid.getPkValue(), "bd_material");
            DynamicObject warehouse = billentry.getDynamicObject("warehouse");
            BigDecimal qty = billentry.getBigDecimal("qty");
            DynamicObject unit = billentry.getDynamicObject("unit");


            map.put("DocType", "发货通知单");//单据类型
            map.put("BizType", bizType);//业务类型 SL:散装销售 SP:袋装销售
            map.put("OrderCode", billno);//发货单编号
            map.put("OrderPK",id);//单据头主键
            map.put("OrderDate",simpleDateFormat.format(createtime));//发货单日期
            map.put("CustomPK",customer.getString("id"));//客户主键
            map.put("CustomCode",customer.getString("number"));//客户编号
            map.put("CustomName",customer.getString("name"));//客户名称
            map.put("SendName",tranSportType);//运输方式名称
            map.put("SalesmanPK",operator == null ? null : operator.getString("id"));//业务员主键
            map.put("SalesmanCode",operator == null ? null : operator.getString("operatornumber"));//业务员编号
            map.put("SalesmanName",operator == null ? null : operator.getString("operatorname"));//业务员名称
            map.put("CarrierPK",nckdCustomer == null ? null : nckdCustomer.getString("id"));//承运商主键
            map.put("CarrierCode",nckdCustomer == null ? null : nckdCustomer.getString("number"));//承运商编号
            map.put("CarrierName",nckdCustomer == null ? null : nckdCustomer.getString("name"));//承运商名称
            map.put("CarNumber",vehicle == null ? null : vehicle.getString("name"));//车牌号
            map.put("DriverName",driver == null ? null : driver.getString("name"));//司机姓名
            map.put("DriverID",driver == null ? null : driver.getString("nckd_idcardno"));//司机身份证号
            map.put("TelPhone",driver == null ? null : driver.getString("nckd_phonenumber"));//手机号
            map.put("IsContainer",StringUtils.isEmpty(isContainer) ? null : isContainer);//是否集装箱0:否 1:是
            map.put("EndSign",StringUtils.isEmpty(closeStatus) ? null : closeStatus);//关闭状态 A:正常 B:关闭
            map.put("OrderRemark",comment);//发货单备注
            map.put("OrderRowNo",1);//发货单分录号
            map.put("OrderRowPK",billentry.getString("id"));//发货单分录主键
            map.put("MaterialPK",masterid.getString("id"));//物料主键
            map.put("MaterialCode",masterid.getString("number"));//物料编号
            map.put("MaterialName",masterid.getString("name"));//物料名称
            map.put("SpecificationsModel",masterid.getString("modelnum"));//规格型号
            //map.put("LevelName",);//品级规格
            map.put("WarehouseCode",warehouse.getString("number"));//仓库编号
            map.put("WarehouseName",warehouse.getString("name"));//仓库名称
            map.put("Quantity",qty);//到货数量
            map.put("MainUnit",unit.getString("name"));//计量单位

            //获取token
            Map<String,Object> tokenMap = new HashMap<>();
            tokenMap.put("UserName","30001");
            tokenMap.put("Password","123456");
            tokenMap.put("grant_type","password");
            String tokenJson = JSON.toJSONString(tokenMap);//map转String
            JSONObject tokenJsonObject = JSON.parseObject(tokenJson);//String转json
            JSONObject resultToken = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/token", tokenJsonObject,null);
            if (resultToken == null){
                log.error("调用智慧物流接口失败{}",resultToken);
                e.setCancelMessage("调用智慧物流接口失败");
                e.setCancel(true);
                return;
            }
            Map<String, Object> resultMap = resultToken.getInnerMap();
            String accessToken = resultMap.get("access_token").toString();

            //推送发货通知单
            String json = JSON.toJSONString(map);//map转String
            JSONObject jsonObject = JSON.parseObject(json);//String转json
            JSONObject result = HttpRequestUtils.httpPost("http://5zb5775265qa.vicp.fun/api/Business/PushDelivery", jsonObject, accessToken);
            if (result != null && "1".equals(result.get("errCode").toString())){
                e.setCancelMessage("推送智慧物流派车单失败：" + result.getString("errMsg"));
                e.setCancel(true);
                return;
            }
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
