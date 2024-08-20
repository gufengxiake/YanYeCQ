package nckd.yanye.scm.plugin.form;

import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Set;

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
                ConfirmCallBackListener addOrderOrContListener = new ConfirmCallBackListener("addOrderOrCont", this);
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
        if ("addOrderOrCont".equals(callBackId)) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                // 询比：1-单次采购-下推采购订单；2-协议采购-下推采购合同
                // 其他：直接生成采购订单
                String purchaseType = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PURCHASETYPE);
                String procurements = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PROCUREMENTS);
                if ("2".equals(procurements)) {
                    if ("1".equals(purchaseType)) {
                        addOrder();
                    } else if ("2".equals(purchaseType)) {
                        addContract();
                    } else {
                        throw new KDBizException("采购类型错误");
                    }
                } else {
                    addOrder();
                }
            }
        }
    }


    /**
     * 生成采购订单
     */
    private void addOrder() {
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
        String purapplybillno = (String) this.getModel().getValue("nckd_purapplybillno");
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplybillno)}
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
                    targetBillIds,
                    PurorderbillConst.FORMBILLID
            );
            // 招采平台价税合计
            tgtObj.set(PurorderbillConst.NCKD_TOTALPRICE, this.getModel().getValue(InforeceivebillConst.NCKD_TOTALPRICE));
            SaveServiceHelper.saveOperate(PurorderbillConst.FORMBILLID, new DynamicObject[]{tgtObj});


            this.getModel().setValue(InforeceivebillConst.NCKD_GENERATIONSTATUS, true);
            this.getModel().setValue(InforeceivebillConst.NCKD_FAILINFO, null);
            SaveServiceHelper.saveOperate(this.getView().getEntityId(), new DynamicObject[]{this.getModel().getDataEntity(true)});
            this.getView().showSuccessNotification("生成采购订单成功!");
        } else {
            this.getView().showErrorNotification("生成采购订单失败！" + pushResult.getMessage());
        }
    }

    /**
     * 生成采购合同
     */
    private void addContract() {
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
        String purapplybillno = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PURAPPLYBILLNO);
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplybillno)}
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
                    targetBillIds,
                    PurcontractConst.FORMBILLID
            );
            // 招采平台价税合计
            tgtObj.set(PurcontractConst.NCKD_TOTALPRICE, this.getModel().getValue(InforeceivebillConst.NCKD_TOTALPRICE));
            SaveServiceHelper.saveOperate(PurcontractConst.FORMBILLID, new DynamicObject[]{tgtObj});

            this.getModel().setValue(InforeceivebillConst.NCKD_GENERATIONSTATUS, true);
            this.getModel().setValue(InforeceivebillConst.NCKD_FAILINFO, null);
            SaveServiceHelper.saveOperate(this.getView().getEntityId(), new DynamicObject[]{this.getModel().getDataEntity(true)});
            this.getView().showSuccessNotification("生成采购合同成功!");
        } else {
            this.getView().showErrorNotification("生成采购合同失败！" + pushResult.getMessage());
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

}
