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
import nckd.yanye.scm.common.PurapplybillConst;
import nckd.yanye.scm.common.SupplierConst;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;
import nckd.yanye.scm.dto.Content;
import nckd.yanye.scm.utils.ZcEncryptUtil;

import java.io.Serializable;
import java.util.HashMap;

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

        // 判断节点编号-非成交业务不做处理
        if (!"win-release".equals(businessNode)) {
            return CustomApiResult.success("success");
        }

        // 签名校验
        if (ZcEncryptUtil.checkSignature(signature, timestamp, nonce)) {
            return CustomApiResult.success("success");
        }

        // 解密消息体
        String decryptMsg = ZcEncryptUtil.decryptBody(encryptBody);
        JSONObject msgObj = JSON.parseObject(decryptMsg);

        // 采购单id
        String orderId = msgObj.getString("orderId");
        // 苍穹对应申请单
        DynamicObject[] purapplyBillObj = BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.BILLNO,
                new QFilter[]{new QFilter(PurapplybillConst.NCKD_PURCHASEID, QCP.equals, orderId)}
        );

        if (purapplyBillObj.length == 0) {
            return CustomApiResult.success("success");
        }

        // 生成信息接收单
        // 成交通知书data
        JSONObject winData = ZcPlatformApiUtil.getWinData(msgObj.getInteger("purchaseType"),
                orderId,
                msgObj.getString("winId")
        );

        // 对应采购单data
        JSONObject orderData = ZcPlatformApiUtil.getOrderData(orderId, msgObj.getInteger("purchaseType"));

        DynamicObject receiveObject = BusinessDataServiceHelper.newDynamicObject(InforeceivebillConst.FORMBILLID);

        // 单据编号
        receiveObject.set(InforeceivebillConst.BILLNO, winData.getString("winId"));
        // 采购申请单单号
        receiveObject.set(InforeceivebillConst.NCKD_PURAPPLYBILLNO, purapplyBillObj[0].getString(PurapplybillConst.BILLNO));
        // 采购类型:单次采购 or 协议供货
        receiveObject.set(InforeceivebillConst.NCKD_PURCHASETYPE, orderData.getString("negotiatePurchaseType"));
        // 采购方式
        receiveObject.set(InforeceivebillConst.NCKD_PROCUREMENTS, msgObj.getString("purchaseType"));
        // 中标供应商分录
        JSONArray suppliers = winData.getJSONArray("suppliers");
        for (int i = 0; i < suppliers.size(); i++) {
            JSONObject supplier = suppliers.getJSONObject(i);
            String supplierId = supplier.getString("supplierId");
            String supplierName = supplier.getString("supplierName");
            // 新增供应商
            saveSupplier(supplierId);
            DynamicObjectCollection winEntryEntity = receiveObject.getDynamicObjectCollection(InforeceivebillConst.ENTRYENTITYID_NCKD_WINENTRYENTITY);
            DynamicObject addNew = winEntryEntity.addNew();
            // 供应商id
            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_SUPPLIERID, supplierId);
            // 供应商名称
            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_SUPPLIERNAME, supplierName);
            // 社会统一信用代码
            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_USCC, supplier.getString("supplierScc"));
            // 中标价
            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_BIDPRICE, supplier.getString("bidPrice"));
        }

        // 物料明细分录
        // 先抓取采购申请单，然后赋值招采平台成交授标品目
        // 先查成交授标，获取成交品目信息。再根据itemId查品目列表，获取品目信息。对应起来。再根据品目编号，赋值分录
        JSONObject awardData = ZcPlatformApiUtil.getAwardData(msgObj.getInteger("purchaseType"), orderId, winData.getString("awardId"));
        JSONArray items = awardData.getJSONArray("items");

        // 品目列表
        JSONArray orderItemsData = ZcPlatformApiUtil.getOrderItemsData(msgObj.getInteger("purchaseType"), orderId);
        HashMap<Integer, HashMap<String, String>> itemMap = new HashMap<>();
        for (Object orderItemsDatum : orderItemsData) {
            HashMap<String, String> map = new HashMap<>();
            JSONObject item = (JSONObject) orderItemsDatum;
            // 品目id
            Integer itemId = item.getInteger("itemId");
            // 品目编号
            String itemCode = item.getString("itemCode");
            // 品目名称
            String itemName = item.getString("itemName");
            // 单位
            String unit = item.getString("unit");
            map.put("itemCode", itemCode);
            map.put("itemName", itemName);
            map.put("unit", unit);
            itemMap.put(itemId, map);
        }

        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            Integer itemId = item.getInteger("itemId");
            DynamicObjectCollection materialEntry = receiveObject.getDynamicObjectCollection(InforeceivebillConst.ENTRYENTITYID_ENTRYENTITY);
            DynamicObject addNew = materialEntry.addNew();
            // 物料名称
            addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_MATERIALNAME, itemMap.get(itemId).get("itemName"));
            // 单位
            addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_UNIT, itemMap.get(itemId).get("unit"));
            // 数量
            addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_APPLYQTY, item.getInteger("awardNum"));
            // 含税单价
            addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_PRICEANDTAX, item.getInteger("offerPrice"));
            // 税率
            addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_TAXRATE, item.getInteger("offerTaxRate"));
            // 价税合计
            addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_AMOUNTANDTAX, item.getInteger("offerPrice") * item.getInteger("awardNum"));
        }

        // 信息接收单状态-C：已审核
        receiveObject.set(InforeceivebillConst.BILLSTATUS, "C");
        // 供应商/订单生成失败原因
        receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, "测试。不生成");

        // 保存信息接收单
        SaveServiceHelper.saveOperate(InforeceivebillConst.FORMBILLID, new DynamicObject[]{receiveObject});
        // todo 生成 采购订单 或 采购合同



        return CustomApiResult.success("success");
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


























