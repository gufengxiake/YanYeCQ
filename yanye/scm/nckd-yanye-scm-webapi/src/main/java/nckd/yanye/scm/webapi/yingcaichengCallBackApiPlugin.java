package nckd.yanye.scm.webapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.openapi.common.custom.annotation.*;
import kd.bos.openapi.common.result.CustomApiResult;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.InforeceivebillConst;
import nckd.yanye.scm.common.SupplierConst;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;
import nckd.yanye.scm.dto.Content;
import nckd.yanye.scm.utils.ZcEncryptUtil;

import java.io.Serializable;

/**
 * 招采平台回调统一消息体
 *
 * @author liuxiao
 */
@ApiController(value = "yingcaicheng", desc = "招采平台回调")
@ApiMapping("/back")
public class yingcaichengCallBackApiPlugin implements Serializable {
    private static final Log log = LogFactory.getLog(yingcaichengCallBackApiPlugin.class);

    @ApiPostMapping(value = "/post", desc = "招采平台回调")
    public CustomApiResult<@ApiResponseBody("返回参数") String> getCallBack(
            @ApiParam(value = "回调编号", required = true) String callbackCode,
            @ApiParam(value = "应用id", required = true) String openAppKey,
            @ApiParam(value = "业务类型编号", required = true) String businessType,
            @ApiParam(value = "业务节点编号", required = true) String businessNode,
            @ApiParam(value = "内容", required = true) Content content
    ) {
        // 随机数
        String nonce = content.getNonce();
        // 时间戳
        String timestamp = content.getTimestamp();
        // 签名
        String signature = content.getSignature();
        // 加密消息体
        String encryptBody = content.getEncryptBody();


        log.debug("招采平台回调开始!回调编号:{}", callbackCode);
        log.debug("应用id:{}", openAppKey);
        log.debug("业务类型编号:{}", businessType);
        log.debug("业务节点编号:{}", businessNode);
        log.debug("随机数:{}", nonce);
        log.debug("时间戳:{}", timestamp);
        log.debug("签名:{}", signature);
        log.debug("加密消息体:{}", encryptBody);
//
//
//        // 判断节点编号-非成交业务不做处理
//        if (!"win-release".equals(businessNode)) {
//            return CustomApiResult.success("success");
//        }
//
//        // 签名校验
//        if (ZcEncryptUtil.checkSignature(signature, timestamp, nonce)) {
//            return CustomApiResult.success("success");
//        }
//
//        // 解密消息体
//        String decryptMsg = ZcEncryptUtil.decryptBody(encryptBody);
//        JSONObject msgObj = JSON.parseObject(decryptMsg);
//
//        // 采购单id
//        String orderId = msgObj.getString("orderId");
//
//        // todo 生成信息接收单
//        // 成交通知书data
//        JSONObject winData = ZcPlatformApiUtil.getWinData(msgObj.getInteger("purchaseType"),
//                orderId,
//                msgObj.getString("winId")
//        );
//        // 对应采购单data
//        JSONObject orderData = ZcPlatformApiUtil.getOrderData(orderId, msgObj.getInteger("purchaseType"));
//
//        DynamicObject receiveObject = BusinessDataServiceHelper.newDynamicObject(InforeceivebillConst.FORMBILLID);
//
//        // 单据编号
//        receiveObject.set(InforeceivebillConst.BILLNO, winData.getString("winId"));
//        // 采购申请单单号
////        receiveObject.set(InforeceivebillConst.NCKD_PURAPPLYBILLNO, );
//        // 采购类型:单次采购 or 协议供货
//        receiveObject.set(InforeceivebillConst.NCKD_PURCHASETYPE, orderData.getString("negotiatePurchaseType"));
//        // todo 采购方式
//        receiveObject.set(InforeceivebillConst.NCKD_PURCHASETYPE, "");
//        // todo 采购方式
//        receiveObject.set(InforeceivebillConst.NCKD_PROCUREMENTS, msgObj.getString("purchaseType"));
//        // 中标供应商分录
//        JSONArray suppliers = winData.getJSONArray("suppliers");
//        for (int i = 0; i < suppliers.size(); i++) {
//            JSONObject supplier = suppliers.getJSONObject(i);
//            String supplierId = supplier.getString("supplierId");
//            String supplierName = supplier.getString("supplierName");
//            // 新增供应商
//            saveSupplier(supplierId);
//            DynamicObjectCollection winEntryEntity = receiveObject.getDynamicObjectCollection("nckd_winentryentity");
//            DynamicObject addNew = winEntryEntity.addNew();
//            // 供应商id
//            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_SUPPLIERID, supplierId);
//            // 供应商名称
//            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_SUPPLIERNAME, supplierName);
//            // 社会统一信用代码
//            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_USCC, supplier.getString("supplierScc"));
//            // 中标价
//            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_BIDPRICE, supplier.getString("bidPrice"));
//        }


        // todo 生成 采购订单 或 采购合同


        CustomApiResult<String> result = CustomApiResult.success("success");
        return result;
    }


    /**
     * 新增成交供应商
     *
     * @param supplierId
     */
    private static void saveSupplier(String supplierId) {
        // 查询成交公司数据
        JSONObject companyData = ZcPlatformApiUtil.getCompanyDataById(supplierId);
        // 成交公司统一社会信用代码
        String uscc = companyData.getString("socialCreditCode");

        //根据招采平台供应商id查询供应商信息
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(
                SupplierConst.FORMBILLID,
                SupplierConst.ALLPROPERTY,
                new QFilter[]{new QFilter(SupplierConst.NCKD_PLATFORMSUPID, QCP.equals, supplierId)}
        );

        // 供应商不存在，则新增
        if (dynamicObjects.length == 0) {
            //根据社会统一信用代码再次查询，如果存在则更新已有供应商信息，不存在则新建
            DynamicObject[] load = BusinessDataServiceHelper.load(
                    SupplierConst.FORMBILLID,
                    SupplierConst.ALLPROPERTY,
                    new QFilter[]{new QFilter(SupplierConst.SOCIETYCREDITCODE, QCP.equals, uscc)}
            );
            if (load.length != 0) {
                DynamicObject supplier = load[0];
                supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
                supplier.set(SupplierConst.SOCIETYCREDITCODE, uscc);
                SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
            }

            //不存在，则新增保存至金蝶供应商
            DynamicObject supplier = BusinessDataServiceHelper.newDynamicObject(SupplierConst.FORMBILLID);
            DynamicObject org = BusinessDataServiceHelper.loadSingle(
                    "100000",
                    "bos_org"
            );

            //名称
            supplier.set(SupplierConst.NAME, companyData.getString("companyName"));
            //创建组织
            supplier.set(SupplierConst.CREATEORG, org);
            //业务组织
            supplier.set(SupplierConst.USEORG, org);
            //统一社会信用代码
            supplier.set(SupplierConst.SOCIETYCREDITCODE, uscc);
            //招采平台id
            supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
            //控制策略：自由分配
            supplier.set(SupplierConst.CTRLSTRATEGY, "5");
            //数据状态
            supplier.set(SupplierConst.STATUS, "C");
            //使用状态
            supplier.set(SupplierConst.ENABLE, "1");

            SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
        }
    }
}


























