package nckd.yanye.scm.webapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.openapi.common.custom.annotation.*;
import kd.bos.openapi.common.result.CustomApiResult;
import kd.bos.openapi.service.context.ServiceApiContext;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.*;
import nckd.yanye.scm.common.dto.Content;
import nckd.yanye.scm.common.utils.ZcEncryptUtil;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 招采平台回调api
 * api编码：back/post
 *
 * @author liuxiao
 * @since 2024/08/19
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
        Map<String, String> repHeader = new HashMap<>(2);
        repHeader.put("Accept", "text/plain");
        ServiceApiContext.getResponse().setResponseHeaders(repHeader);

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
        if (!ZcEncryptUtil.checkSignature(signature, timestamp, nonce)) {
            return CustomApiResult.success("failed");
        }

        // 解密消息体
        String decryptMsg = ZcEncryptUtil.decryptBody(encryptBody);
        JSONObject msgObj = JSON.parseObject(decryptMsg);

        // 采购单id
        String procurements = msgObj.getString("purchaseType");
        String orderIdPre = null;
        switch (procurements) {
            case "1":
                orderIdPre = "ZB-";
                break;
            case "8":
                orderIdPre = "TP-";
                break;
            case "2":
                orderIdPre = "XB-";
                break;
            default:
                break;
        }
        String orderId = msgObj.getString("orderId");
        // 是否有苍穹对应申请单
        DynamicObject[] purapplyBillObj = BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.NCKD_PURCHASEID, QCP.equals, orderIdPre + orderId)}
        );

        if (purapplyBillObj.length == 0) {
            return CustomApiResult.success("success");
        }

        // 是否有重复信息接收单
        DynamicObject[] receiveBillObj = BusinessDataServiceHelper.load(
                InforeceivebillConst.FORMBILLID,
                InforeceivebillConst.BILLNO,
                new QFilter[]{new QFilter(InforeceivebillConst.BILLNO, QCP.equals, msgObj.getString("winId"))}
        );

        if (receiveBillObj.length > 0) {
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
        // 订单/合同生成失败原因
        receiveObject.set(InforeceivebillConst.NCKD_GENERATIONSTATUS, false);
        receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, "生成失败!");

        // 单据编号
        String billNo = winData.getString("winId");
        receiveObject.set(InforeceivebillConst.BILLNO, billNo);
        // 采购申请单单号
        String purapplyBillNo = purapplyBillObj[0].getString(PurapplybillConst.BILLNO);
        receiveObject.set(InforeceivebillConst.NCKD_PURAPPLYBILLNO, purapplyBillNo);
        // 采购方式
        receiveObject.set(InforeceivebillConst.NCKD_PROCUREMENTS, procurements);
        // 采购类型:单次采购 or 协议供货
        String purchaseType = null;
        switch (purapplyBillObj[0].getString(PurapplybillConst.NCKD_PROCUREMENTS)) {
            case "pricecomparison":
            case "singlebrand":
                purchaseType = purapplyBillObj[0].getString("nckd_purchasetype");
                break;
            case "competitive":
                purchaseType = purapplyBillObj[0].getString("nckd_purchasetype1");
                break;
            case "singlesupplier":
                purchaseType = purapplyBillObj[0].getString("nckd_purchasetype2");
                break;
            case "bidprocurement":
                purchaseType = purapplyBillObj[0].getString("nckd_purchasetype3");
                break;
            default:
                break;
        }
        receiveObject.set(InforeceivebillConst.NCKD_PURCHASETYPE, purchaseType);

        // todo 币别
        receiveObject.set(InforeceivebillConst.NCKD_CURRENCY, 1);
        // 采购单id
        receiveObject.set(InforeceivebillConst.NCKD_ORDERID, orderId);


        // 中标供应商分录
        JSONArray suppliers = winData.getJSONArray("suppliers");
        BigDecimal totalPrice = new BigDecimal(0);
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
            // 中标价(分)
            BigDecimal bidPrice = supplier.getBigDecimal("bidPrice").divide(new BigDecimal(100), RoundingMode.HALF_UP);
            totalPrice = totalPrice.add(bidPrice);
            addNew.set(InforeceivebillConst.NCKD_WINENTRYENTITY_NCKD_BIDPRICE, bidPrice);
        }

        // 物料明细分录
        // 先抓取采购申请单，然后赋值招采平台成交授标品目
        // 先查成交授标，获取成交品目信息。再根据itemId查品目列表，获取品目信息。对应起来。再根据品目编号，赋值分录
        // 询比才有物料
        if ("XBJ".equals(businessType)) {
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
                // 品目行号
                String spuCode = item.getString("spuCode");
                // 品目编号
                String itemCode = item.getString("itemCode");
                // 品目名称
                String itemName = item.getString("itemName");
                // 单位
                String unit = item.getString("unit");


                map.put("spuCode", spuCode);
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
                // 物料行号
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_SPUCODE, itemMap.get(itemId).get("spuCode"));
                // 物料编码
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_MATERIAL, BusinessDataServiceHelper.load(
                        "bd_materialpurchaseinfo",
                        "id,materialname,masterid",
                        new QFilter[]{new QFilter("masterid.number", QCP.equals, itemMap.get(itemId).get("itemCode"))}
                )[0]);
                // 物料名称
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_MATERIALNAME, itemMap.get(itemId).get("itemName"));
                // 数量
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_APPLYQTY, item.getBigDecimal("awardNum").divide(new BigDecimal(100), RoundingMode.HALF_UP));
                // 含税单价
                BigDecimal offerPrice = item.getBigDecimal("offerPrice").divide(new BigDecimal(100), RoundingMode.HALF_UP);
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_PRICEANDTAX, offerPrice);
                // 税率
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_TAXRATE, item.getBigDecimal("offerTaxRate").divide(new BigDecimal(100), RoundingMode.HALF_UP));
                // 价税合计
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_AMOUNTANDTAX, offerPrice.multiply(item.getBigDecimal("awardNum")));
            }
        }
        // 查看供应商是否存在
        DynamicObject supplier = null;
        DynamicObject dynamicObject = receiveObject.getDynamicObjectCollection(InforeceivebillConst.ENTRYENTITYID_NCKD_WINENTRYENTITY).get(0);
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(
                SupplierConst.FORMBILLID,
                SupplierConst.ALLPROPERTY,
                new QFilter[]{new QFilter(SupplierConst.NCKD_PLATFORMSUPID, QCP.equals, dynamicObject.getString("nckd_supplierid"))}
        );
        if (dynamicObjects.length == 0) {
            receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, "系统未找到中标供应商信息，请维护");
        } else {
            supplier = dynamicObjects[0];
        }


        // 招采成交价税合计
        receiveObject.set(InforeceivebillConst.NCKD_TOTALPRICE, totalPrice);
        // 信息接收单状态-C：已审核
        receiveObject.set(InforeceivebillConst.BILLSTATUS, "C");


        // 询比：1-单次采购-下推采购订单；2-协议采购-下推采购合同
        if (supplier != null) {
            // 生成采购订单
            if ("1".equals(purchaseType)) {
                addOrder(billNo, purapplyBillNo, totalPrice, receiveObject, supplier);
                // 生成采购合同
            } else if ("0".equals(purchaseType)) {
                addContract(billNo, purapplyBillNo, totalPrice, receiveObject, supplier);
            } else {
                receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, "采购类型错误!");
            }
        }

        // 保存信息接收单
        SaveServiceHelper.saveOperate(InforeceivebillConst.FORMBILLID, new DynamicObject[]{receiveObject});

        return CustomApiResult.success("success");
    }

    /**
     * 生成采购合同
     */
    private void addContract(String billNo, String purapplyBillNo, BigDecimal totalPrice, DynamicObject receiveObject, DynamicObject supplier) {
        //如果下游有单据，打断操作
        DynamicObject[] billnos = BusinessDataServiceHelper.load(
                PurcontractConst.FORMBILLID,
                PurcontractConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurcontractConst.NCKD_UPINFORECEIVEBILL, QCP.equals, billNo)}
        );
        if (billnos.length != 0) {
            return;
        }

        //获取源单
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplyBillNo)}
        ))[0];

        // 单据pkid
        Long pkid = (long) srcObj.getPkValue();

        List<ListSelectedRow> selectedRows = new ArrayList<>();
        ListSelectedRow selectedRow = new ListSelectedRow(pkid);
        selectedRows.add(selectedRow);

        // 生成下推参数PushArgs
        PushArgs pushArgs = new PushArgs();
        // 必选，源单标识
        pushArgs.setSourceEntityNumber(PurapplybillConst.FORMBILLID);
        // 必选，目标单标识
        pushArgs.setTargetEntityNumber(PurcontractConst.FORMBILLID);
        // 可选，自动保存
        pushArgs.setAutoSave(true);
        // 可选，设置单据转换规则的id，如果没有设置，会自动匹配一个规则进行转换
//        pushArgs.setRuleId("1134727974310918144");
        // 是否输出详细错误报告
        pushArgs.setBuildConvReport(false);
        // 必选，设置需要下推的源单及分录内码
        pushArgs.setSelectedRows(selectedRows);
        // 调用下推引擎，下推目标单并保存
        ConvertOperationResult pushResult = ConvertServiceHelper.pushAndSave(pushArgs);
        if (pushResult.isSuccess()) {
            Set<Object> targetBillIds = pushResult.getTargetBillIds();
            DynamicObject tgtObj = BusinessDataServiceHelper.loadSingle(
                    targetBillIds.toArray()[0],
                    PurcontractConst.FORMBILLID
            );
            // 招采平台价税合计
            tgtObj.set(PurcontractConst.NCKD_TOTALPRICE, totalPrice);
            // 上游采购申请单
            tgtObj.set(PurcontractConst.NCKD_UPAPPLYBILL, purapplyBillNo);
            // 上游信息接收单
            tgtObj.set(PurcontractConst.NCKD_UPINFORECEIVEBILL, billNo);

            // 设置含税单价
            setPriceandtax(tgtObj, receiveObject);

            // 供应商信息
            tgtObj.set("supplier", supplier);
            tgtObj.set("providersupplier", supplier);
            tgtObj.set("invoicesupplier", supplier);
            tgtObj.set("receivesupplier", supplier);

            // 保存采购合同
            SaveServiceHelper.save(new DynamicObject[]{tgtObj});

            receiveObject.set(InforeceivebillConst.NCKD_GENERATIONSTATUS, true);
            receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, null);
        }
    }

    /**
     * 生成采购订单
     */
    private void addOrder(String billNo, String purapplyBillNo, BigDecimal totalPrice, DynamicObject receiveObject, DynamicObject supplier) {
        //如果下游有单据，打断操作
        DynamicObject[] billnos = BusinessDataServiceHelper.load(
                PurorderbillConst.FORMBILLID,
                PurorderbillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurorderbillConst.NCKD_UPINFORECEIVEBILL, QCP.equals, billNo)}
        );
        if (billnos.length != 0) {
            return;
        }

        //获取源单
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplyBillNo)}
        ))[0];

        // 单据pkid
        Long pkid = (long) srcObj.getPkValue();

        List<ListSelectedRow> selectedRows = new ArrayList<>();
        ListSelectedRow selectedRow = new ListSelectedRow(pkid);
        selectedRows.add(selectedRow);

        // 生成下推参数PushArgs
        PushArgs pushArgs = new PushArgs();
        // 必选，源单标识
        pushArgs.setSourceEntityNumber(PurapplybillConst.FORMBILLID);
        // 必选，目标单标识
        pushArgs.setTargetEntityNumber(PurorderbillConst.FORMBILLID);
        // 可选，自动保存
        pushArgs.setAutoSave(true);
        // 可选，设置单据转换规则的id，如果没有设置，会自动匹配一个规则进行转换
//        pushArgs.setRuleId("1134727974310918144");
        // 是否输出详细错误报告
        pushArgs.setBuildConvReport(false);
        // 必选，设置需要下推的源单及分录内码
        pushArgs.setSelectedRows(selectedRows);
        // 调用下推引擎，下推目标单并保存
        ConvertOperationResult pushResult = ConvertServiceHelper.pushAndSave(pushArgs);
        if (pushResult.isSuccess()) {
            Set<Object> targetBillIds = pushResult.getTargetBillIds();
            DynamicObject tgtObj = BusinessDataServiceHelper.loadSingle(
                    targetBillIds.toArray()[0],
                    PurorderbillConst.FORMBILLID
            );
            // 招采平台价税合计
            tgtObj.set(PurorderbillConst.NCKD_TOTALPRICE, totalPrice);

            // 上游采购申请单
            tgtObj.set(PurorderbillConst.NCKD_UPAPPLYBILL, purapplyBillNo);
            // 上游信息接收单
            tgtObj.set(PurorderbillConst.NCKD_UPINFORECEIVEBILL, billNo);
            // 设置含税单价
            setPriceandtax(tgtObj, receiveObject);

            // 供应商信息
            tgtObj.set("supplier", supplier);
            tgtObj.set("providersupplier", supplier);
            tgtObj.set("invoicesupplier", supplier);
            tgtObj.set("receivesupplier", supplier);

            // 保存采购订单
            SaveServiceHelper.save(new DynamicObject[]{tgtObj});

            receiveObject.set(InforeceivebillConst.NCKD_GENERATIONSTATUS, true);
            receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, null);
        }
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

    /**
     * 含税单价
     */
    private static void setPriceandtax(DynamicObject tgtObj, DynamicObject receiveObject) {
        DynamicObjectCollection materialEntry = receiveObject.getDynamicObjectCollection(InforeceivebillConst.ENTRYENTITYID_ENTRYENTITY);
        HashMap<String, BigDecimal> map = new HashMap<>();
        for (DynamicObject obj : materialEntry) {
            map.put(obj.getString(InforeceivebillConst.ENTRYENTITY_NCKD_SPUCODE), obj.getBigDecimal(InforeceivebillConst.ENTRYENTITY_NCKD_PRICEANDTAX));
        }

        DynamicObjectCollection tgtMaterialEntry = tgtObj.getDynamicObjectCollection("billentry");
        for (DynamicObject obj : tgtMaterialEntry) {
            BigDecimal seq = map.get(obj.getString("seq"));
            boolean flag = seq != null;
            obj.set("priceandtax", seq);
            obj.set("nckd_topush", flag);
        }
    }

}






