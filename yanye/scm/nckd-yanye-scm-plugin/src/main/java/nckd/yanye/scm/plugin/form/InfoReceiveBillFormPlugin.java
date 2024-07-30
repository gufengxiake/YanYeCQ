package nckd.yanye.scm.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.*;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;

/**
 * 信息接收单-表单插件
 *
 * @author liuxiao
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
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        switch (evt.getItemKey()) {
            // 生成供应商
            case ADDSUPPLIER:
                addSup();
                break;
            // 生成采购订单/合同
            case ADDORDERORCONT:
                String purchaseType = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PURCHASETYPE);
                // 1-单次采购-下推采购订单；2-协议采购-下推采购合同
                if ("1".equals(purchaseType)) {
                    addOrder();
                } else if ("2".equals(purchaseType)) {
                    addContract();
                } else {
                    throw new KDBizException("采购类型不正确!");
                }
                break;
            // 查看成交通知书
            case VIEWNOTICE:
                viewNotice();
                break;
            default:
                break;
        }
    }

    /**
     * 生成供应商
     */
    private void addSup() {
        String supplierId = (String) this.getModel().getValue("nckd_supplierid");
        String uscc = (String) this.getModel().getValue("nckd_uscc");
        OperationResult result = addSupplier(supplierId, uscc);
        if (result.isSuccess()) {
            this.getView().showSuccessNotification("生成成功!");
        } else {
            this.getView().showErrorNotification("生成失败！" + result.getMessage());
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

        //创建采购订单
        DynamicObject tgtObj = BusinessDataServiceHelper.newDynamicObject(PurorderbillConst.FORMBILLID);
        /*
        拼装赋值
         */
        //单据类型：物料类采购
        tgtObj.set(PurorderbillConst.BILLTYPE, BusinessDataServiceHelper.loadSingle(
                        "539008795674673152",
                        "bos_billtype"
                )
        );
        //业务类型
        tgtObj.set(PurorderbillConst.BIZTYPE, srcObj.get(PurapplybillConst.BIZTYPE));
        //订单日期
        tgtObj.set(PurorderbillConst.BIZTIME, srcObj.get(PurapplybillConst.BIZTIME));
        //todo 招采成交价税合计(将招采平台“成交通知书”的总金额记录至该字段)
        tgtObj.set(PurorderbillConst.NCKD_TOTALPRICE, new BigDecimal(456));
        //上游信息接收单
        tgtObj.set(PurorderbillConst.NCKD_UPINFORECEIVEBILL, this.getModel().getValue("billno"));
        //采购组织
        tgtObj.set(PurorderbillConst.ORG, srcObj.get(PurapplybillConst.ORG));
        //订货供应商
        String supplierid = (String) this.getModel().getValue("nckd_supplierid");
        DynamicObject supplier = BusinessDataServiceHelper.load(
                SupplierConst.FORMBILLID,
                SupplierConst.ALLPROPERTY,
                new QFilter[]{new QFilter(SupplierConst.NCKD_PLATFORMSUPID, QCP.equals, supplierid)}
        )[0];
        tgtObj.set(PurorderbillConst.SUPPLIER, supplier);
        //财务信息
        DynamicObject RMB = BusinessDataServiceHelper.loadSingle(
                "1",
                "bd_currency"
        );
        tgtObj.set(PurorderbillConst.CURRENCY, RMB);
        tgtObj.set(PurorderbillConst.SETTLECURRENCY, RMB);
        tgtObj.set(PurorderbillConst.EXRATETABLE, BusinessDataServiceHelper.loadSingle(
                "1890691225300776960",
                "bd_exratetable"

        ));
        tgtObj.set(PurorderbillConst.EXRATEDATE, new Date());
        tgtObj.set(PurorderbillConst.EXCHANGERATE, 1);

        //物料明细
        DynamicObjectCollection srcEntryEntity = srcObj.getDynamicObjectCollection(PurapplybillConst.ENTRYENTITYID_BILLENTRY);
        DynamicObjectCollection tgtEntryEntity = tgtObj.getDynamicObjectCollection(PurorderbillConst.ENTRYENTITYID_BILLENTRY);
        for (int i = 0; i < srcEntryEntity.size(); i++) {
            tgtEntryEntity.addNew();
            DynamicObject srcEntry = srcEntryEntity.get(i);
            DynamicObject tgtEntry = tgtEntryEntity.get(i);
            //分录行
            tgtEntry.set("seq", i + 1);
            //行类型
            tgtEntry.set(PurorderbillConst.BILLENTRY_LINETYPE, srcEntry.get(PurapplybillConst.BILLENTRY_LINETYPE));
            //物料编码
            tgtEntry.set(PurorderbillConst.BILLENTRY_MATERIAL, srcEntry.get(PurapplybillConst.BILLENTRY_MATERIAL));
            //物料名称
            tgtEntry.set(PurorderbillConst.BILLENTRY_MATERIALNAME, srcEntry.get(PurapplybillConst.BILLENTRY_MATERIALNAME));
            //规格型号
            tgtEntry.set(PurorderbillConst.BILLENTRY_AUXPTY, srcEntry.get(PurapplybillConst.BILLENTRY_AUXPTY));
            //计量单位
            tgtEntry.set(PurorderbillConst.BILLENTRY_UNIT, srcEntry.get(PurapplybillConst.BILLENTRY_UNIT));
            //数量
            tgtEntry.set(PurorderbillConst.BILLENTRY_QTY, srcEntry.get(PurapplybillConst.BILLENTRY_QTY));
            //基本单位
            tgtEntry.set(PurorderbillConst.BILLENTRY_BASEUNIT, srcEntry.get(PurapplybillConst.BILLENTRY_BASEUNIT));
            //基本数量
            tgtEntry.set(PurorderbillConst.BILLENTRY_BASEQTY, srcEntry.get(PurapplybillConst.BILLENTRY_BASEQTY));
            //辅助单位
            tgtEntry.set(PurorderbillConst.BILLENTRY_AUXUNIT, srcEntry.get(PurapplybillConst.BILLENTRY_AUXUNIT));
            //辅助数量
            tgtEntry.set(PurorderbillConst.BILLENTRY_AUXQTY, srcEntry.get(PurapplybillConst.BILLENTRY_AUXQTY));
            //单价
            tgtEntry.set(PurorderbillConst.BILLENTRY_PRICE, srcEntry.get(PurapplybillConst.BILLENTRY_PRICE));
            //含税单价
            tgtEntry.set(PurorderbillConst.BILLENTRY_PRICEANDTAX, srcEntry.get(PurapplybillConst.BILLENTRY_PRICEANDTAX));
            //todo 含税单价来自招采平台
            tgtEntry.set(PurorderbillConst.BILLENTRY_NCKD_TOPUSH, true);
            //税率
            tgtEntry.set(PurorderbillConst.BILLENTRY_TAXRATEID, srcEntry.get(PurapplybillConst.BILLENTRY_TAXRATEID));
            //税率(%)
            tgtEntry.set(PurorderbillConst.BILLENTRY_TAXRATE, srcEntry.get(PurapplybillConst.BILLENTRY_TAXRATE));
            //价税合计
            tgtEntry.set(PurorderbillConst.BILLENTRY_AMOUNTANDTAX, srcEntry.get(PurapplybillConst.BILLENTRY_AMOUNTANDTAX));
            //需求组织
            tgtEntry.set(PurorderbillConst.BILLENTRY_ENTRYREQORG, srcEntry.get(PurapplybillConst.BILLENTRY_ENTRYREQORG));
            //需求部门
            tgtEntry.set(PurorderbillConst.BILLENTRY_ENTRYREQDEPT, srcEntry.get(PurapplybillConst.BILLENTRY_ENTRYREQDEPT));
            //收货组织
            tgtEntry.set(PurorderbillConst.BILLENTRY_ENTRYRECORG, srcEntry.get(PurapplybillConst.BILLENTRY_ENTRYRECORG));
            //收货部门
            tgtEntry.set(PurorderbillConst.BILLENTRY_ENTRYRECDEPT, srcEntry.get(PurapplybillConst.BILLENTRY_ENTRYRECDEPT));
            //收货仓库
            tgtEntry.set(PurorderbillConst.BILLENTRY_WAREHOUSE, srcEntry.get(PurapplybillConst.BILLENTRY_WAREHOUSE));
            //变更方式
            tgtEntry.set(PurorderbillConst.BILLENTRY_ENTRYCHANGETYPE, "B");
        }

        //数据状态:暂存
        tgtObj.set(PurorderbillConst.BILLSTATUS, "A");
        //变更状态：正常
        tgtObj.set(PurorderbillConst.CHANGESTATUS, "A");
        //关闭状态：正常
        tgtObj.set(PurorderbillConst.CLOSESTATUS, "A");
        //异步状态：已完成
        tgtObj.set(PurorderbillConst.ASYNCSTATUS, "B");
        //版本号
        tgtObj.set(PurorderbillConst.VERSION, 1);
        //子版本号
        tgtObj.set(PurorderbillConst.SUBVERSION, 1);
        //作废状态：未作废
        tgtObj.set(PurorderbillConst.CANCELSTATUS, "A");


        OperationResult result = SaveServiceHelper.saveOperate(PurorderbillConst.FORMBILLID, new DynamicObject[]{tgtObj});
        if (result.isSuccess()) {
            this.getModel().setValue("nckd_generationstatus", true);
            SaveServiceHelper.saveOperate(this.getView().getEntityId(), new DynamicObject[]{this.getModel().getDataEntity(true)});
            this.getView().showSuccessNotification("成功");
        } else {
            this.getView().showErrorNotification("失败");
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
        String purapplybillno = (String) this.getModel().getValue("nckd_purapplybillno");
        DynamicObject srcObj = (BusinessDataServiceHelper.load(
                PurapplybillConst.FORMBILLID,
                PurapplybillConst.ALLPROPERTY,
                new QFilter[]{new QFilter(PurapplybillConst.BILLNO, QCP.equals, purapplybillno)}
        ))[0];

        //创建采购合同
        DynamicObject tgtObj = BusinessDataServiceHelper.newDynamicObject(PurcontractConst.FORMBILLID);

        /*
        拼装赋值
         */
        //单据类型：标准采购合同
        tgtObj.set(PurcontractConst.BILLTYPE, BusinessDataServiceHelper.loadSingle(
                "712981204034394112",
                "bos_billtype"

        ));
        //合同种类：采购合同
        tgtObj.set(PurcontractConst.CATEGORY, BusinessDataServiceHelper.loadSingle(
                "860129821311646720",
                "conm_category"

        ));
        //合同类型：标准采购合同
        tgtObj.set(PurcontractConst.TYPE, BusinessDataServiceHelper.loadSingle(
                "712021156571496448",
                "conm_type"
        ));
        //合同属性：合同
        tgtObj.set(PurcontractConst.CONMPROP, "B");
        //适用组织范围
        tgtObj.set(PurcontractConst.SUITSCOPE, "A");
        //签订日期
        tgtObj.set(PurcontractConst.BIZTIME, new Date());
        //起始日期
        tgtObj.set(PurcontractConst.BIZTIMEBEGIN, new Date());
        //截止日期
        tgtObj.set(PurcontractConst.BIZTIMEEND, new Date());
        //todo 招采成交价税合计(将招采平台“成交通知书”的总金额记录至该字段)
        tgtObj.set(PurcontractConst.NCKD_TOTALPRICE, new BigDecimal(456));
        //上游信息接收单
        tgtObj.set(PurcontractConst.NCKD_UPINFORECEIVEBILL, this.getModel().getValue("billno"));

        //采购组织
        tgtObj.set(PurcontractConst.ORG, srcObj.get(PurapplybillConst.ORG));

        //财务信息
        DynamicObject RMB = BusinessDataServiceHelper.loadSingle(
                "1",
                "bd_currency"
        );
        tgtObj.set(PurcontractConst.CURRENCY, RMB);
        tgtObj.set(PurcontractConst.SETTLECURRENCY, RMB);
        tgtObj.set(PurcontractConst.EXRATETABLE, BusinessDataServiceHelper.loadSingle(
                "1890691225300776960",
                "bd_exratetable"

        ));
        tgtObj.set(PurcontractConst.EXRATEDATE, new Date());
        tgtObj.set(PurcontractConst.EXCHANGERATE, 1);

        //物料明细
        DynamicObjectCollection srcEntryEntity = srcObj.getDynamicObjectCollection(PurapplybillConst.ENTRYENTITYID_BILLENTRY);
        DynamicObjectCollection tgtEntryEntity = tgtObj.getDynamicObjectCollection(PurcontractConst.ENTRYENTITYID_BILLENTRY);
        for (int i = 0; i < srcEntryEntity.size(); i++) {
            tgtEntryEntity.addNew();
            DynamicObject srcEntry = srcEntryEntity.get(i);
            DynamicObject tgtEntry = tgtEntryEntity.get(i);
            //分录行
            tgtEntry.set("seq", i + 1);
            tgtEntry.set(PurcontractConst.BILLENTRY_LINENO, i + 1);
            //物料编码
            tgtEntry.set(PurcontractConst.BILLENTRY_MATERIAL, srcEntry.get(PurapplybillConst.BILLENTRY_MATERIAL));
            //物料名称
            tgtEntry.set(PurcontractConst.BILLENTRY_MATERIALNAME, srcEntry.get(PurapplybillConst.BILLENTRY_MATERIALNAME));
            //规格型号
            tgtEntry.set(PurcontractConst.BILLENTRY_AUXPTY, srcEntry.get(PurapplybillConst.BILLENTRY_AUXPTY));
            //计量单位
            tgtEntry.set(PurcontractConst.BILLENTRY_UNIT, srcEntry.get(PurapplybillConst.BILLENTRY_UNIT));
            //数量
            tgtEntry.set(PurcontractConst.BILLENTRY_QTY, srcEntry.get(PurapplybillConst.BILLENTRY_QTY));
            //基本单位
            tgtEntry.set(PurcontractConst.BILLENTRY_BASEUNIT, srcEntry.get(PurapplybillConst.BILLENTRY_BASEUNIT));
            //基本数量
            tgtEntry.set(PurcontractConst.BILLENTRY_BASEQTY, srcEntry.get(PurapplybillConst.BILLENTRY_BASEQTY));
            //辅助单位
            tgtEntry.set(PurcontractConst.BILLENTRY_AUXUNIT, srcEntry.get(PurapplybillConst.BILLENTRY_AUXUNIT));
            //辅助数量
            tgtEntry.set(PurcontractConst.BILLENTRY_AUXQTY, srcEntry.get(PurapplybillConst.BILLENTRY_AUXQTY));
            //单价
            tgtEntry.set(PurcontractConst.BILLENTRY_PRICE, srcEntry.get(PurapplybillConst.BILLENTRY_PRICE));
            //含税单价
            tgtEntry.set(PurcontractConst.BILLENTRY_PRICEANDTAX, srcEntry.get(PurapplybillConst.BILLENTRY_PRICEANDTAX));
            //todo 含税单价来自招采平台
            tgtEntry.set(PurcontractConst.BILLENTRY_NCKD_TOPUSH, true);
            //税率
            tgtEntry.set(PurcontractConst.BILLENTRY_TAXRATEID, srcEntry.get(PurapplybillConst.BILLENTRY_TAXRATEID));
            //税率(%)
            tgtEntry.set(PurcontractConst.BILLENTRY_TAXRATE, srcEntry.get(PurapplybillConst.BILLENTRY_TAXRATE));
            //价税合计
            tgtEntry.set(PurcontractConst.BILLENTRY_AMOUNTANDTAX, srcEntry.get(PurapplybillConst.BILLENTRY_AMOUNTANDTAX));
            //需求组织
            tgtEntry.set(PurcontractConst.BILLENTRY_ENTRYREQORG, srcEntry.get(PurapplybillConst.BILLENTRY_ENTRYREQORG));
            //收货组织
            tgtEntry.set(PurcontractConst.BILLENTRY_ENTRYINVORG, srcEntry.get(PurapplybillConst.BILLENTRY_ENTRYRECORG));
            //收货仓库
            tgtEntry.set(PurcontractConst.BILLENTRY_WAREHOUSE, srcEntry.get(PurapplybillConst.BILLENTRY_WAREHOUSE));
            //变更方式
            tgtEntry.set(PurcontractConst.BILLENTRY_BILLENTRYCHANGETYPE, "B");
        }

        //数据状态：暂存
        tgtObj.set(PurcontractConst.BILLSTATUS, "A");
        //变更状态：正常
        tgtObj.set(PurcontractConst.CHANGESTATUS, "A");
        //关闭状态：正常
        tgtObj.set(PurcontractConst.CLOSESTATUS, "A");
        //生效状态：未生效
//        tgtObj.set(PurcontractConst.VALIDSTATUS, "A");
        //版本号
        tgtObj.set(PurcontractConst.VERSION, 1);
        //子版本号
        tgtObj.set(PurcontractConst.SUBVERSION, 1);
        //作废状态：未作废
        tgtObj.set(PurcontractConst.CANCELSTATUS, "A");


        OperationResult result = SaveServiceHelper.saveOperate(PurcontractConst.FORMBILLID, new DynamicObject[]{tgtObj});
        if (result.isSuccess()) {
            this.getModel().setValue("nckd_generationstatus", true);
            SaveServiceHelper.saveOperate(this.getView().getEntityId(), new DynamicObject[]{this.getModel().getDataEntity(true)});
            this.getView().showSuccessNotification("下推采购合同成功!");
        } else {
            this.getView().showErrorNotification("下推采购合同失败!");
        }
    }

    /**
     * todo 查看成交通知书
     */
    private void viewNotice() {
        // 采购方式
        String procurements = (String) this.getModel().getValue(InforeceivebillConst.NCKD_PROCUREMENTS);
        // 采购单id
        String orderId = (String) this.getModel().getValue(InforeceivebillConst.NCKD_ORDERID);
        // 成交id
        String winId = (String) this.getModel().getValue(InforeceivebillConst.NCKD_WINID);

        String url = ZcPlatformApiUtil.viewWinNotice(procurements, orderId);
        getView().openUrl(url);

    }


    /**
     * fixme 新增供应商
     *
     * @param supplierId
     * @param uscc
     * @return
     */
    public static OperationResult addSupplier(String supplierId, String uscc) {
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
                //todo 保存成功校验
                return SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
            }


            //不存在，则新增保存至金蝶供应商
            DynamicObject supplier = BusinessDataServiceHelper.newDynamicObject(SupplierConst.FORMBILLID);
            DynamicObject org = BusinessDataServiceHelper.loadSingle(
                    "100000",
                    "bos_org"
            );
            //todo 查询招采平台供应商信息

            //编码
//            supplier.set(SupplierConst.NUMBER, "123");
            //名称
            supplier.set(SupplierConst.NAME, "测试11");
            //创建组织
            supplier.set(SupplierConst.CREATEORG, org);
            //业务组织
            supplier.set(SupplierConst.USEORG, org);
            //统一社会信用代码
            supplier.set(SupplierConst.SOCIETYCREDITCODE, "123123");
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
