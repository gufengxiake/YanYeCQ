package nckd.yanye.scm.webapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        log.info("收到招采平台回调信息:\r\n回调编号:{}\r\n应用id:{}\r\n业务类型编号:{}\r\n业务节点编号:{}\r\n内容:{}",
                callbackCode,
                openAppKey,
                businessType,
                businessNode,
                content
        );
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
            log.warn("招采平台回调结束。非成交业务不做处理，返回参数:{}", "success");
            return CustomApiResult.success("success");
        }

        // 根据应用id获取平台
        ZcPlatformConst zcPlatformConst = new ZcPlatformConst(openAppKey);

        // 签名校验
        if (!ZcEncryptUtil.checkSignature(zcPlatformConst, signature, timestamp, nonce)) {
            log.error("招采平台回调结束。签名校验失败，返回参数:{}", "failed");
            return CustomApiResult.success("failed");
        }

        // 解密消息体
        String decryptMsg = ZcEncryptUtil.decryptBody(zcPlatformConst, encryptBody);
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
            log.warn("招采平台回调结束。无苍穹对应申请单，返回参数:{}", "success");
            return CustomApiResult.success("success");
        }

        // 是否有重复信息接收单
        DynamicObject[] receiveBillObj = BusinessDataServiceHelper.load(
                InforeceivebillConst.FORMBILLID,
                InforeceivebillConst.BILLNO,
                new QFilter[]{new QFilter(InforeceivebillConst.BILLNO, QCP.equals, msgObj.getString("winId"))}
        );

        if (receiveBillObj.length > 0) {
            log.warn("招采平台回调结束。有重复信息接收单，返回参数:{}", "success");
            return CustomApiResult.success("success");
        }

        // 生成信息接收单
        // 成交通知书data
        JSONObject winData = ZcPlatformApiUtil.getWinData(zcPlatformConst, msgObj.getInteger("purchaseType"),
                orderId,
                msgObj.getString("winId")
        );
        String msg = "生成失败!";

        // 对应采购单data
        JSONObject orderData = ZcPlatformApiUtil.getOrderData(zcPlatformConst, orderId, msgObj.getInteger("purchaseType"));

        DynamicObject receiveObject = BusinessDataServiceHelper.newDynamicObject(InforeceivebillConst.FORMBILLID);

        // 申请组织
        receiveObject.set("nckd_org", purapplyBillObj[0].get(PurapplybillConst.ORG));
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
            JSONObject awardData = ZcPlatformApiUtil.getAwardData(zcPlatformConst, msgObj.getInteger("purchaseType"), orderId, winData.getString("awardId"));
            JSONArray items = awardData.getJSONArray("items");

            // 品目列表
            JSONArray orderItemsData = ZcPlatformApiUtil.getOrderItemsData(zcPlatformConst, msgObj.getInteger("purchaseType"), orderId);
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
                addNew.set(InforeceivebillConst.ENTRYENTITY_NCKD_AMOUNTANDTAX, offerPrice.multiply(item.getBigDecimal("awardNum")).divide(new BigDecimal(100), RoundingMode.HALF_UP));
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
            msg = "系统未找到中标供应商信息，请维护";
        } else {
            supplier = dynamicObjects[0];
        }


        // 招采成交价税合计
        receiveObject.set(InforeceivebillConst.NCKD_TOTALPRICE, totalPrice);
        // 信息接收单状态-C：已审核
        receiveObject.set(InforeceivebillConst.BILLSTATUS, "C");


        receiveObject.set(InforeceivebillConst.NCKD_GENERATIONSTATUS, false);
        receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, msg);
        // 保存信息接收单
        SaveServiceHelper.save(new DynamicObject[]{receiveObject});

        try {
            DynamicObject tgtObj = null;
            String formBillId = null;
            if (supplier != null) {
                // 生成采购订单
                if ("1".equals(purchaseType)) {
                    tgtObj = addOrder(billNo, purapplyBillNo);
                    formBillId = PurorderbillConst.FORMBILLID;
                    // 生成采购合同
                } else if ("0".equals(purchaseType)) {
                    tgtObj = addContract(billNo, purapplyBillNo);
                    formBillId = PurcontractConst.FORMBILLID;
                }
            }

            if (tgtObj == null) {
                throw new KDBizException("采购类型错误");
            }

            // 招采平台价税合计
            tgtObj.set(PurorderbillConst.NCKD_TOTALPRICE, totalPrice);
            // 上游采购申请单
            tgtObj.set(PurorderbillConst.NCKD_UPAPPLYBILL, purapplyBillNo);
            // 上游信息接收单
            tgtObj.set(PurorderbillConst.NCKD_UPINFORECEIVEBILL, billNo);
            // 采购方式为询比：设置含税单价
            if ("2".equals(procurements)) {
                setPriceandtax(tgtObj, receiveObject);
            }

            // 供应商信息
            tgtObj.set("supplier", supplier);
            tgtObj.set("providersupplier", supplier);
            tgtObj.set("invoicesupplier", supplier);
            tgtObj.set("receivesupplier", supplier);

            if ("conm_purcontract".equals(tgtObj.getDataEntityType().getName())) {
                tgtObj.set("party2nd", supplier.getString("name"));
            }

            OperationResult operationResult = SaveServiceHelper.saveOperate(formBillId, new DynamicObject[]{tgtObj});
            if (operationResult.isSuccess()) {
                receiveObject.set(InforeceivebillConst.NCKD_GENERATIONSTATUS, true);
                msg = null;
            } else {
                receiveObject.set(InforeceivebillConst.NCKD_GENERATIONSTATUS, false);
                msg = operationResult.getMessage();
            }
        } catch (Exception e) {
            // 保存信息接收单
            receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, e.getMessage());
            SaveServiceHelper.save(new DynamicObject[]{receiveObject});
            log.error("招采平台回调结束。生成采购订单/合同失败:{}。返回参数:{}", e.getMessage(), "success");
            return CustomApiResult.success("success");
        }

        // 保存信息接收单
        receiveObject.set(InforeceivebillConst.NCKD_FAILINFO, msg);
        SaveServiceHelper.save(new DynamicObject[]{receiveObject});
        log.info("招采平台回调结束，保存信息接收单成功。返回参数:{}", "success");
        return CustomApiResult.success("success");
    }

    /**
     * 生成采购订单
     *
     * @return
     */
    private DynamicObject addOrder(String billNo, String purapplyBillNo) {
        //如果下游有单据，打断操作
        DynamicObject[] billnos = BusinessDataServiceHelper.load(
                PurorderbillConst.FORMBILLID,
                PurorderbillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurorderbillConst.NCKD_UPINFORECEIVEBILL, QCP.equals, billNo)}
        );
        if (billnos.length != 0) {
            throw new KDBizException("已生成过采购订单");
        }

        //获取源单
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplyBillNo)}
        ))[0];

        // 构建下推参数
        PushArgs pushArgs = getPushArgs(srcObj, PurorderbillConst.FORMBILLID);
        // 调用下推引擎，下推目标单并保存
        ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
        if (pushResult.isSuccess()) {
            MainEntityType mainEntityType = EntityMetadataCache.getDataEntityType(pushArgs.getTargetEntityNumber());
            List<DynamicObject> targetDos = pushResult.loadTargetDataObjects(BusinessDataServiceHelper::loadRefence, mainEntityType);
            return targetDos.get(0);
        } else {
            throw new KDBizException("生成订单失败！" + pushResult.getMessage());
        }
    }


    /**
     * 生成采购合同
     *
     * @return
     */
    private DynamicObject addContract(String billNo, String purapplyBillNo) {
        //如果下游有单据，打断操作
        DynamicObject[] billnos = BusinessDataServiceHelper.load(
                PurcontractConst.FORMBILLID,
                PurcontractConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurcontractConst.NCKD_UPINFORECEIVEBILL, QCP.equals, billNo)}
        );
        if (billnos.length != 0) {
            throw new KDBizException("已生成过采购合同");
        }

        //获取源单
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplyBillNo)}
        ))[0];

        // 构建下推参数
        PushArgs pushArgs = getPushArgs(srcObj, PurcontractConst.FORMBILLID);
        // 调用下推引擎，下推目标单并保存
        ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
        if (pushResult.isSuccess()) {
            MainEntityType mainEntityType = EntityMetadataCache.getDataEntityType(pushArgs.getTargetEntityNumber());
            List<DynamicObject> targetDos = pushResult.loadTargetDataObjects(BusinessDataServiceHelper::loadRefence, mainEntityType);
            return targetDos.get(0);
        } else {
            throw new KDBizException("生成采购合同失败！" + pushResult.getMessage());
        }
    }


//    /**
//     * 新增成交供应商
//     *
//     * @param supplierId
//     */
//    private static void saveSupplier(String supplierId) {
//        // 查询成交公司数据
//        JSONObject companyData = ZcPlatformApiUtil.getCompanyDataById(supplierId);
//        // 成交公司统一社会信用代码
//        String uscc = companyData.getString("socialCreditCode");
//
//        //根据招采平台供应商id查询供应商信息
//        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(
//                SupplierConst.FORMBILLID,
//                SupplierConst.ALLPROPERTY,
//                new QFilter[]{new QFilter(SupplierConst.NCKD_PLATFORMSUPID, QCP.equals, supplierId)}
//        );
//
//        // 供应商不存在，则新增
//        if (dynamicObjects.length == 0) {
//            //根据社会统一信用代码再次查询，如果存在则更新已有供应商信息，不存在则新建
//            DynamicObject[] load = BusinessDataServiceHelper.load(
//                    SupplierConst.FORMBILLID,
//                    SupplierConst.ALLPROPERTY,
//                    new QFilter[]{new QFilter(SupplierConst.SOCIETYCREDITCODE, QCP.equals, uscc)}
//            );
//            if (load.length != 0) {
//                DynamicObject supplier = load[0];
//                supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
//                supplier.set(SupplierConst.SOCIETYCREDITCODE, uscc);
//                SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
//            }
//
//            //不存在，则新增保存至金蝶供应商
//            DynamicObject supplier = BusinessDataServiceHelper.newDynamicObject(SupplierConst.FORMBILLID);
//            DynamicObject org = BusinessDataServiceHelper.loadSingle(
//                    "100000",
//                    "bos_org"
//            );
//
//            //名称
//            supplier.set(SupplierConst.NAME, companyData.getString("companyName"));
//            //创建组织
//            supplier.set(SupplierConst.CREATEORG, org);
//            //业务组织
//            supplier.set(SupplierConst.USEORG, org);
//            //统一社会信用代码
//            supplier.set(SupplierConst.SOCIETYCREDITCODE, uscc);
//            //招采平台id
//            supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
//            //控制策略：自由分配
//            supplier.set(SupplierConst.CTRLSTRATEGY, "5");
//            //数据状态
//            supplier.set(SupplierConst.STATUS, "C");
//            //使用状态
//            supplier.set(SupplierConst.ENABLE, "1");
//
//            SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
//        }
//    }

    private PushArgs getPushArgs(DynamicObject srcObj, String formbillid) {
        Long pkid = (long) srcObj.getPkValue();

        List<ListSelectedRow> selectedRows = new ArrayList<>();
        ListSelectedRow selectedRow = new ListSelectedRow(pkid);
        selectedRows.add(selectedRow);

        // 生成下推参数PushArgs
        PushArgs pushArgs = new PushArgs();
        // 必选，源单标识
        pushArgs.setSourceEntityNumber(PurapplybillConst.FORMBILLID);
        // 必选，目标单标识
        pushArgs.setTargetEntityNumber(formbillid);
        // 可选，自动保存
        pushArgs.setAutoSave(false);
        // 可选，设置单据转换规则的id，如果没有设置，会自动匹配一个规则进行转换
//        pushArgs.setRuleId("1134727974310918144");
        // 是否输出详细错误报告
        pushArgs.setBuildConvReport(true);
        // 必选，设置需要下推的源单及分录内码
        pushArgs.setSelectedRows(selectedRows);
        return pushArgs;
    }

    /**
     * 含税单价
     */
    private static void setPriceandtax(DynamicObject tgtObj, DynamicObject receiveObject) {
        DynamicObjectCollection materialEntry = receiveObject.getDynamicObjectCollection(InforeceivebillConst.ENTRYENTITYID_ENTRYENTITY);
        HashMap<String, DynamicObject> map = new HashMap<>();
        for (DynamicObject obj : materialEntry) {
            map.put(obj.getString(InforeceivebillConst.ENTRYENTITY_NCKD_SPUCODE), obj);
        }

        DynamicObjectCollection tgtMaterialEntry = tgtObj.getDynamicObjectCollection("billentry");


        // 财务信息：税额
        BigDecimal totaltaxamount = BigDecimal.ZERO;
        // 财务信息：金额
        BigDecimal totalamount = BigDecimal.ZERO;
        // 财务信息：价税合计
        BigDecimal totalallamount = BigDecimal.ZERO;
        for (DynamicObject obj : tgtMaterialEntry) {
            String seq = obj.getString("seq");
            DynamicObject dynamicObject = map.get(seq);
            BigDecimal priceandtax = dynamicObject.getBigDecimal(InforeceivebillConst.ENTRYENTITY_NCKD_PRICEANDTAX);
            boolean toPush = priceandtax != null;
            // 含税单价
            obj.set("priceandtax", priceandtax);
            // 是否来自招采平台推送
            obj.set("nckd_topush", toPush);
            // 税率(%)
            BigDecimal taxrate = dynamicObject.getBigDecimal("nckd_taxrate");
            obj.set("taxrate", taxrate);
            // 税率
            obj.set("taxrateid", BusinessDataServiceHelper.load(
                    "bd_taxrate",
                    "id",
                    new QFilter[]{new QFilter("taxrate", QCP.equals, taxrate)}
            )[0]);


            // 价税合计 = 数量 * 含税单价
            BigDecimal amountandtax = obj.getBigDecimal("qty").multiply(obj.getBigDecimal("priceandtax"));
            obj.set("amountandtax", amountandtax);
            obj.set("curamountandtax", amountandtax);
            totalallamount = totalallamount.add(amountandtax);

            // 单价=含税单价/（1+税率 / 100）
            BigDecimal price = obj.getBigDecimal("priceandtax").divide(BigDecimal.ONE.add(obj.getBigDecimal("taxrate").divide(new BigDecimal(100))), 2, RoundingMode.HALF_UP);
            obj.set("price", price);

            // 金额=单价*数量
            BigDecimal amount = price.multiply(obj.getBigDecimal("qty"));
            obj.set("amount", amount);
            obj.set("curamount", amount);
            totalamount = totalamount.add(amount);

            // 税额 = 价税合计 - 金额
            BigDecimal taxAmount = amountandtax.subtract(amount);
            obj.set("taxamount", taxAmount);
            obj.set("curtaxamount", taxAmount);
            totaltaxamount = totaltaxamount.add(taxAmount);

            // 采购合同：含税单价上下限
            if ("conm_purcontract".equals(tgtObj.getDataEntityType().getName())) {
                obj.set("nckd_priceandtaxup", priceandtax);
                obj.set("nckd_priceandtaxlow", priceandtax);
            }
        }
        tgtObj.set("totaltaxamount", totaltaxamount);
        tgtObj.set("totalamount", totalamount);
        tgtObj.set("totalallamount", totalallamount);


    }

}






