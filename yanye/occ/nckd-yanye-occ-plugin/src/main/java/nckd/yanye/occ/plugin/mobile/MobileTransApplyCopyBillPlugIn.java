package nckd.yanye.occ.plugin.mobile;

import kd.bos.bill.BillShowParameter;
import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.result.IOperateInfo;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.IFormView;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.metadata.dao.MetadataDao;
import kd.bos.mvc.FormConfigFactory;
import kd.bos.mvc.SessionManager;
import kd.bos.mvc.bill.BillModel;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

public class MobileTransApplyCopyBillPlugIn extends AbstractMobFormPlugin {
    private static final String targetBill = "im_transapply";

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        //默认单据类型为外仓转运
        this.getModel().setItemValueByID("nckd_billtype", "2025683988596668416");
        Long orgId = RequestContext.get().getOrgId();
        this.getModel().setItemValueByID("nckd_org", orgId);
        //获取调拨申请单编码
//        DynamicObject bill = BusinessDataServiceHelper.newDynamicObject("im_transapply");
//        CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(bill.getDataEntityType().getName(), bill, orgId.toString());
//        String billno = CodeRuleServiceHelper.getNumber(codeRule, bill);
//        this.getModel().setValue("nckd_billno", billno);

    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        String propName = e.getProperty().getName();
        if ("nckd_material".equals(propName)) {
            DynamicObject material = (DynamicObject) e.getChangeSet()[0].getNewValue();
            String name = material.getDynamicObject("masterid").getString("name");
            DynamicObject unit = material.getDynamicObject("inventoryunit");
            this.getModel().setValue("nckd_materialname", name, 0);
            this.getModel().setValue("nckd_unit", unit, 0);
        }
    }

    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        switch (e.getOperateKey()) {
            case "mobilesave":
                if (e.getOperationResult().isSuccess()) {
                    this.save();
                    this.getModel().setValue("nckd_billstatus", "A");
                    this.getView().showSuccessNotification("保存成功！");
                }
                break;
            case "mobilesubmit":
                if (e.getOperationResult().isSuccess()) {
                    Long pkId = this.getPkId();
                    if (pkId == 0) {
                       this.getView().showErrorNotification("请先保存单据！");
                       return;
                    }
                    OperateOption submitOption = OperateOption.create();
                    submitOption.setVariableValue("ignorewarn", String.valueOf(true));
                    OperationResult result = OperationServiceHelper.executeOperate("submit", targetBill, new Long[]{pkId}, submitOption);
                    if (!result.isSuccess()) {
                        List<IOperateInfo> errInfo = result.getAllErrorOrValidateInfo();
                        StringBuilder errMessage = new StringBuilder();
                        for (IOperateInfo err : errInfo) {
                            errMessage.append(err.getMessage());
                        }
                        this.getView().showErrorNotification(errMessage.toString());
                        return;
                    }
                    this.getModel().setValue("nckd_billstatus", "B");
                    this.getView().setEnable(false, "nckd_combofield", "nckd_applyuser", "nckd_dirver", "nckd_car", "nckd_cartype", "nckd_material", "nckd_materialname", "nckd_unit", "nckd_qty", "nckd_warehouse", "nckd_inwarehouse");//锁定字段
                    this.getView().setEnable(false, "nckd_mobilesave", "nckd_submit");//锁定按钮
                    this.getView().showSuccessNotification("提交成功！");
                }
                break;
            case "mobileaudit":
                if (e.getOperationResult().isSuccess()) {
                    Long pkId = this.getPkId();
                    if (pkId == 0) {
                        this.getView().showErrorNotification("请先保存单据！");
                        return;
                    }
                    String status=this.getModel().getValue("nckd_billstatus").toString();
                    if(!status.equals("B")){
                        this.getView().showErrorNotification("单据未提交，不允许审核！");
                        return;
                    }
                    OperateOption submitOption = OperateOption.create();
                    submitOption.setVariableValue("ignorewarn", String.valueOf(true));
                    OperationResult result = OperationServiceHelper.executeOperate("audit", targetBill, new Long[]{pkId}, submitOption);
                    if (!result.isSuccess()) {
                        List<IOperateInfo> errInfo = result.getAllErrorOrValidateInfo();
                        StringBuilder errMessage = new StringBuilder();
                        for (IOperateInfo err : errInfo) {
                            errMessage.append(err.getMessage());
                        }
                        this.getView().showErrorNotification(errMessage.toString());
                        return;
                    }
                    this.getModel().setValue("nckd_billstatus", "C");
                    //this.getView().setEnable(false, "nckd_combofield", "nckd_applyuser", "nckd_dirver", "nckd_car", "nckd_cartype", "nckd_material", "nckd_materialname", "nckd_unit", "nckd_qty", "nckd_warehouse", "nckd_inwarehouse");//锁定字段
                    this.getView().setEnable(false, "btnsubmit");//锁定按钮
                    this.getView().showSuccessNotification("审核成功！");
                }
        }

    }

    /*
    保存直接调拨单
     */
    private void save() {
        Long pkId = this.getPkId();
        //申请组织
        DynamicObject org = (DynamicObject) this.getModel().getValue("nckd_org");
        //单据类型
        DynamicObject billtype = (DynamicObject) this.getModel().getValue("nckd_billtype");
        //运费结算方式
        String jsType = this.getModel().getValue("nckd_combofield") != null ? this.getModel().getValue("nckd_combofield").toString() : null;
        //承运商
        DynamicObject supplier = (DynamicObject) this.getModel().getValue("nckd_applyuser");
        //司机
        DynamicObject nckd_dirver = (DynamicObject) this.getModel().getValue("nckd_dirver");
        //车辆
        DynamicObject nckd_car = (DynamicObject) this.getModel().getValue("nckd_car");
        //运输方式
        String ys = this.getModel().getValue("nckd_cartype") != null ? this.getModel().getValue("nckd_cartype").toString() : null;
        //物料
        DynamicObject material = (DynamicObject) this.getModel().getValue("nckd_material");
        //数量
        Object qty = this.getModel().getValue("nckd_qty");
        //调出仓库
        DynamicObject warehouse = (DynamicObject) this.getModel().getValue("nckd_warehouse");
        //调入仓库
        DynamicObject inwarehouse = (DynamicObject) this.getModel().getValue("nckd_inwarehouse");


        MainEntityType dt = EntityMetadataCache.getDataEntityType(targetBill);
        String appId = getAppId(targetBill, dt);
        // 设置单据显示参数
        BillShowParameter para = new BillShowParameter();
        para.setFormId(targetBill);
        para.setPkId(0);
        para.setAppId(appId);

        // 创建单据配置
        FormConfigFactory.createConfigInCurrentAppService(para);
        // 获取单据页面视图
        final SessionManager sm = SessionManager.getCurrent();
        final IFormView formView = sm.getView(para.getPageId());
        if (formView != null) {
            // 设置视图应用id和数据模型
            formView.getFormShowParameter().setAppId(appId);
            formView.getModel().createNewData();
            //formView.updateView();
        }
        BillModel mode = (BillModel) formView.getModel();
        mode.setPKValue(pkId);
        if (pkId > 0) {
            mode.load(pkId);
        }

        mode.setValue("org", org);
        mode.setValue("billtype", billtype);
        mode.setValue("nckd_combofield", jsType);
        mode.setValue("nckd_applyuser", supplier);
        mode.setValue("nckd_dirver", nckd_dirver);
        mode.setValue("nckd_car", nckd_car);
        mode.setValue("nckd_cartype", ys);

        mode.deleteEntryData("billentry");
        mode.batchCreateNewEntryRow("billentry", 1);
        int row = 0;
        mode.setValue("material", material, row);
        mode.setValue("qty", qty, row);
        mode.setValue("warehouse", warehouse, row);
        mode.setValue("inwarehouse", inwarehouse, row);


        OperationResult saveOp = formView.invokeOperation("save");
        if (saveOp.isSuccess()) {
            formView.close();
            Map<Object, String> billnos = saveOp.getBillNos();
            Object Key = billnos.keySet().iterator().next();
            String no = billnos.get(Key);
            this.getModel().setValue("nckd_billno", no);
        }

    }

    private Long getPkId() {
        Long pkId = 0L;
        String billno = this.getModel().getValue("nckd_billno").toString();
        if (billno != null && !billno.isEmpty()) {
            QFilter qFilter = new QFilter("billno", QCP.equals, billno);
            DynamicObject collections = QueryServiceHelper.queryOne(targetBill,
                    "id", qFilter.toArray());
            if (collections != null) {
                pkId = (Long) collections.get("id");
            }
        }

        return pkId;
    }

    private static String getAppId(String entityNumber, MainEntityType dt) {
        String appId = dt.getAppId();
        if (!"bos".equals(appId)) {
            return appId;
        } else {
            String bizAppNumber = dt.getBizAppNumber();
            if (StringUtils.isBlank(bizAppNumber)) {
                bizAppNumber = MetadataDao.getAppNumberByEntityNumber(entityNumber);
            }

            if (StringUtils.isNotBlank(bizAppNumber)) {
                appId = String.format("%s.%s", "bos", bizAppNumber);
            }

            return appId;
        }
    }
}
