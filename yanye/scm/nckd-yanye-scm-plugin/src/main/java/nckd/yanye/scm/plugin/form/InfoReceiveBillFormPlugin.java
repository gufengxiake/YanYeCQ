package nckd.yanye.scm.plugin.form;

import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.tx.TX;
import kd.bos.db.tx.TXHandle;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.ConfirmCallBackListener;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.MessageBoxResult;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.*;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 信息接收单-表单插件
 * 单据标识：nckd_pm_inforeceivebill
 *
 * @author liuxiao
 * @since 2024-08-19
 */
public class InfoReceiveBillFormPlugin extends AbstractFormPlugin {
    /**
     * 按钮标识-生成供应商
     */
    final static String ADDSUPPLIER = "bar_addsupplier";
    /**
     * 按钮标识-生成采购订单或合同
     */
    final static String ADDORDERORCONT = "bar_addorderorcont";

    /**
     * 按钮标识-查看成交通知书
     */
    final static String VIEWNOTICE = "bar_viewnotice";


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 侦听主菜单按钮点击事件
        this.addItemClickListeners("tbmain");
    }

    /**
     * 按钮点击事件
     *
     * @param evt
     */
    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.itemClick(evt);
        switch (evt.getItemKey()) {
            // 生成供应商
            case ADDSUPPLIER:
                addSup();
                break;
            // 生成采购订单/合同
            case ADDORDERORCONT:
                evt.setCancel(true);
                ConfirmCallBackListener addOrderOrContListener = new ConfirmCallBackListener("addorderorcont", this);
                this.getView().showConfirm("您确认生成采购订单/合同吗？", MessageBoxOptions.YesNo, addOrderOrContListener);
                break;
            // 查看成交通知书
            case VIEWNOTICE:
                viewNotice();
                break;
            default:
                break;
        }
    }


    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        //判断回调参数id
        String callBackId = messageBoxClosedEvent.getCallBackId();
        if ("addorderorcont".equals(callBackId)) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                // 查看供应商是否存在
                String supplierId = (String) this.getModel().getValue("nckd_supplierid");
                DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(
                        SupplierConst.FORMBILLID,
                        SupplierConst.ALLPROPERTY,
                        new QFilter[]{new QFilter(SupplierConst.NCKD_PLATFORMSUPID, QCP.equals, supplierId)}
                );
                if (dynamicObjects.length == 0) {
                    throw new KDBizException("系统未找到中标供应商信息，请维护!");
                }
                DynamicObject supplier = dynamicObjects[0];

                String purchaseType = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PURCHASETYPE);
                String msg = "";
                DynamicObject tgtObj = null;
                // 1-单次采购-下推采购订单；
                if ("1".equals(purchaseType)) {
                    tgtObj = addOrder(supplier);
                    msg = "生成采购订单成功";
                    //2-协议采购-下推采购合同
                } else if ("0".equals(purchaseType)) {
                    tgtObj = addOrder(supplier);
                    msg = "生成采购合同成功";
                } else {
                    throw new KDBizException("采购类型错误");
                }
                // 招采平台价税合计
                tgtObj.set(PurorderbillConst.NCKD_TOTALPRICE, this.getModel().getValue(InforeceivebillConst.NCKD_TOTALPRICE));
                // 上游采购申请单
                tgtObj.set(PurorderbillConst.NCKD_UPAPPLYBILL, this.getModel().getValue(InforeceivebillConst.NCKD_PURAPPLYBILLNO));
                // 上游信息接收单
                tgtObj.set(PurorderbillConst.NCKD_UPINFORECEIVEBILL, this.getModel().getValue(InforeceivebillConst.BILLNO));
                // 采购方式为询比：设置含税单价
                if ("2".equals(this.getModel().getValue(InforeceivebillConst.NCKD_PURCHASETYPE))) {
                    setPriceandtax(tgtObj, this.getModel().getDataEntity(true));
                }

                // 供应商信息
                tgtObj.set("supplier", supplier);
                tgtObj.set("providersupplier", supplier);
                tgtObj.set("invoicesupplier", supplier);
                tgtObj.set("receivesupplier", supplier);

                SaveServiceHelper.save(new DynamicObject[]{tgtObj});

                this.getModel().setValue(InforeceivebillConst.NCKD_GENERATIONSTATUS, true);
                this.getModel().setValue(InforeceivebillConst.NCKD_FAILINFO, null);
                SaveServiceHelper.save(new DynamicObject[]{this.getModel().getDataEntity(true)});
                this.getView().showSuccessNotification(msg);
            }
        }
    }


    /**
     * 生成采购订单
     *
     * @return
     */
    private DynamicObject addOrder(DynamicObject supplier) {
        //如果下游有单据，打断操作
        DynamicObject[] billnos = BusinessDataServiceHelper.load(
                PurorderbillConst.FORMBILLID,
                PurorderbillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurorderbillConst.NCKD_UPINFORECEIVEBILL, QCP.equals, this.getModel().getValue("billno"))}
        );
        if (billnos.length != 0) {
            throw new KDBizException("已生成过采购订单");
        }

        //获取源单
        String purapplyBillNo = (String) this.getModel().getValue("nckd_purapplybillno");
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplyBillNo)}
        ))[0];

        // 构建下推参数
        PushArgs pushArgs = getPushArgs(srcObj, PurorderbillConst.FORMBILLID);
        // 调用下推引擎，下推目标单并保存
        try (TXHandle h = TX.required("addOrder")) {
            try {
                ConvertOperationResult pushResult = ConvertServiceHelper.pushAndSave(pushArgs);
                if (pushResult.isSuccess()) {
                    Set<Object> targetBillIds = pushResult.getTargetBillIds();
                    DynamicObject tgtObj = BusinessDataServiceHelper.loadSingle(
                            targetBillIds.toArray()[0],
                            PurorderbillConst.FORMBILLID
                    );
                    return tgtObj;
                } else {
                    throw new KDBizException("生成订单失败！" + pushResult.getMessage());
                }
            } catch (Throwable e) {
                h.markRollback();
                throw e;
            }
        }

    }


    /**
     * 生成采购合同
     *
     * @return
     */
    private DynamicObject addContract(DynamicObject supplier) {
        //如果下游有单据，打断操作
        DynamicObject[] billnos = BusinessDataServiceHelper.load(
                PurcontractConst.FORMBILLID,
                PurcontractConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurcontractConst.NCKD_UPINFORECEIVEBILL, QCP.equals, this.getModel().getValue("billno"))}
        );
        if (billnos.length != 0) {
            throw new KDBizException("已生成过采购合同");
        }

        //获取源单
        String purapplyBillNo = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PURAPPLYBILLNO);
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplyBillNo)}
        ))[0];

        // 构建下推参数
        PushArgs pushArgs = getPushArgs(srcObj, PurcontractConst.FORMBILLID);
        // 调用下推引擎，下推目标单并保存
        try (TXHandle h = TX.required("addContract")) {
            try {
                ConvertOperationResult pushResult = ConvertServiceHelper.pushAndSave(pushArgs);
                if (pushResult.isSuccess()) {
                    Set<Object> targetBillIds = pushResult.getTargetBillIds();
                    DynamicObject tgtObj = BusinessDataServiceHelper.loadSingle(
                            targetBillIds.toArray()[0],
                            PurcontractConst.FORMBILLID
                    );
                    return tgtObj;
                } else {
                    throw new KDBizException("生成采购合同失败！" + pushResult.getMessage());
                }
            } catch (Throwable e) {
                h.markRollback();
                throw e;
            }
        }

    }

    /**
     * 生成供应商
     */
    private void addSup() {
        String supplierId = (String) this.getModel().getValue("nckd_supplierid");
        OperationResult result = addSupplier(supplierId);
        if (result.isSuccess()) {
            this.getView().showSuccessNotification("生成成功!");
        } else {
            this.getView().showErrorNotification("生成失败！" + result.getMessage());
        }
    }

    /**
     * 查看成交通知书
     */
    private void viewNotice() {
        // 采购方式
        String procurements = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PROCUREMENTS);
        // 采购单id
        String orderId = (String) this.getModel().getValue(InforeceivebillConst.NCKD_ORDERID);
        String url = ZcPlatformApiUtil.getViewWinNoticeUrl(procurements, orderId);
        getView().openUrl(url);
    }


    /**
     * 新增供应商
     *
     * @param supplierId
     * @return
     */
    public static OperationResult addSupplier(String supplierId) {
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
                return SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
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

            return SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
        }

        OperationResult result = new OperationResult();
        result.setSuccess(false);
        result.setMessage("系统已存在对应的供应商");
        return result;
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


        for (DynamicObject obj : tgtMaterialEntry) {
            String seq = obj.getString("seq");
            BigDecimal priceandtax = map.get(seq).getBigDecimal(InforeceivebillConst.ENTRYENTITY_NCKD_PRICEANDTAX);
            boolean toPush = priceandtax != null;
            // 含税单价
            obj.set("priceandtax", priceandtax);
            // 是否来自招采平台推送
            obj.set("nckd_topush", toPush);
            // 税率(%)
            BigDecimal taxrate = map.get(seq).getBigDecimal("nckd_taxrate");
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
            // 单价=含税单价/（1+税率 / 100）
            BigDecimal price = obj.getBigDecimal("priceandtax").divide(BigDecimal.ONE.add(obj.getBigDecimal("taxrate").divide(new BigDecimal(100))), 2, RoundingMode.HALF_UP);
            obj.set("price", price);
            // 金额=单价*数量
            BigDecimal amount = price.multiply(obj.getBigDecimal("qty"));
            obj.set("amount", amount);
            obj.set("curamount", amount);

            // 税额 = 价税合计 - 金额
            BigDecimal taxAmount = amountandtax.subtract(amount);
            obj.set("taxamount", taxAmount);
            obj.set("curtaxamount", taxAmount);
        }


    }


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
//        pushArgs.setAutoSave(true);
        // 可选，设置单据转换规则的id，如果没有设置，会自动匹配一个规则进行转换
//        pushArgs.setRuleId("1134727974310918144");
        // 是否输出详细错误报告
        pushArgs.setBuildConvReport(false);
        // 必选，设置需要下推的源单及分录内码
        pushArgs.setSelectedRows(selectedRows);
        return pushArgs;
    }
}
