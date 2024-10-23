package nckd.yanye.occ.plugin.mobile;

import kd.bos.bill.BillShowParameter;
import kd.bos.bill.MobileFormPosition;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.result.IOperateInfo;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.*;
import kd.bos.form.control.Control;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.list.MobileListShowParameter;
import kd.bos.metadata.dao.MetadataDao;
import kd.bos.mvc.FormConfigFactory;
import kd.bos.mvc.SessionManager;
import kd.bos.mvc.bill.BillModel;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.math.BigDecimal;
import java.util.*;

/*
直接调拨单移动表单插件
标识：nckd_im_transdirbill_ext_mob
author:wgl
date:2024/08/21
 */
public class MobileTransdirBillPlugIn extends AbstractMobFormPlugin implements BeforeF7SelectListener {


    private static final String targetBill = "im_transdirbill";

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        //默认单据类型为借货归还单
        this.getModel().setItemValueByID("billtype", "1980435141796826112");

        //设置业务员
        Long orgId = RequestContext.get().getOrgId();
        DynamicObject org = (DynamicObject) this.getModel().getValue("org");
        if (org != null) {
            orgId = (Long) org.getPkValue();
        }
        if (orgId != 0) {
            // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组
            QFilter qFilter = new QFilter("createorg.id", QCP.equals, orgId)
                    .and("operatorgrouptype", QCP.equals, "XSZ");
            //查找业务组
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_operatorgroup",
                    "id", qFilter.toArray(), "");
            if (!collections.isEmpty()) {
                DynamicObject operatorGroupItem = collections.get(0);
                long operatorGroupId = (long) operatorGroupItem.get("id");
                //this.getModel().setItemValueByID("nckd_operatorgroup", operatorGroupId);
                DynamicObject user = UserServiceHelper.getCurrentUser("id,number,name");
                if (user != null && operatorGroupId != 0) {
                    String number = user.getString("number");
                    Long userId = user.getLong("id");
                    // 构造QFilter  operatornumber业务员   operatorgrpid 业务组id
                    QFilter Filter = new QFilter("operatornumber", QCP.equals, number)
                            .and("operatorgrpid", QCP.equals, operatorGroupId);
                    //查找业务员
                    DynamicObjectCollection opreatorColl = QueryServiceHelper.query("bd_operator",
                            "id", Filter.toArray(), "");
                    if (!opreatorColl.isEmpty()) {
                        DynamicObject operatorItem = opreatorColl.get(0);
                        String operatorId = operatorItem.getString("id");
                        this.getModel().setItemValueByID("nckd_ywy", operatorId);
                    }
                    //查找业务员对应的销售片区
                    // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组 entryentity.operator 业务员
                    QFilter gFilter = new QFilter("createorg.id", QCP.equals, orgId)
                            .and("operatorgrouptype", QCP.equals, "XSZ")
                            .and("entryentity.operator.id", QCP.equals, userId);
                    //查找销售片区
                    DynamicObjectCollection gcollections = QueryServiceHelper.query("bd_operatorgroup",
                            "entryentity.nckd_regiongroup regiongroup", gFilter.toArray(), "");
                    if (!gcollections.isEmpty()) {
                        DynamicObject regionGroupItem = gcollections.get(0);
                        long regionGroupId = (long) regionGroupItem.get("regiongroup");
                        this.getModel().setItemValueByID("nckd_regiongroup", regionGroupId);
                    }
                }
            }

        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        String propName = e.getProperty().getName();
        if ("material".equals(propName)) {
            DynamicObject material = (DynamicObject) e.getChangeSet()[0].getNewValue();
            String name = material.getDynamicObject("masterid").getString("name");
            DynamicObject unit = material.getDynamicObject("inventoryunit");
            //获取当前行
            int index = e.getChangeSet()[0].getRowIndex();
            this.getModel().setValue("materialname", name, index);
            this.getModel().setValue("unit", unit, index);
        }
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners("nckd_return");
        BasedataEdit unitEdit = this.getView().getControl("unit");
        if (unitEdit != null) {
            unitEdit.addBeforeF7SelectListener(this);
        }
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name = evt.getProperty().getName();
        if(name.equalsIgnoreCase("unit")){
            int row=evt.getRow();
            DynamicObject wl= (DynamicObject) this.getModel().getValue("material",row);
            if(wl!=null){
                Object untigroupId=null;
                DynamicObject masterid=wl.getDynamicObject("masterid");
                if(masterid!=null){
                    DynamicObject baseUnit=masterid.getDynamicObject("baseunit");
                    if(baseUnit!=null){
                        Object baseUnitId=baseUnit.getPkValue();
                        DynamicObject baseUnitData=BusinessDataServiceHelper.loadSingle(baseUnitId,"bd_measureunits");
                        untigroupId=baseUnitData.getDynamicObject("group").getPkValue();
                    }

                }
                Set unitList=new HashSet<Long>();
                Object masterid_Id=masterid.getPkValue();
                if (null != masterid_Id && !"0".equals(masterid_Id)) {
                    QFilter[] filters = new QFilter[]{new QFilter("materialid", "=", masterid_Id)};
                    Map<Object, DynamicObject> multimeasureunits = BusinessDataServiceHelper.loadFromCache("bd_multimeasureunit", "id,measureunitid.id", filters);
                    if (multimeasureunits != null && multimeasureunits.size() > 0) {
                        int index = 0;
                        for(Iterator var6 = multimeasureunits.values().iterator(); var6.hasNext(); ++index) {
                            DynamicObject object = (DynamicObject)var6.next();
                            Long unit= (Long) object.get("measureunitid.id");
                            unitList.add(unit);
                        }
                    }
                }
                if(untigroupId!=null){
                    ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                    List<QFilter> qFilters = new ArrayList<>();
                    qFilters.add(new QFilter("group.id", QCP.equals, untigroupId).or("id",QCP.in,unitList));
                    formShowParameter.getListFilterParameter().setQFilters(qFilters);
                }

            }

        }
    }


    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        switch (e.getOperateKey()) {
            case "mobilesave":
                if (e.getOperationResult().isSuccess()) {
                    this.save();
                }
                break;
            case "mobilesubmit":
                if (e.getOperationResult().isSuccess()) {
                    Long pkId = this.getPkId();
                    if (pkId != 0) {
                        this.update(pkId);
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
                        this.getModel().setValue("billstatus", "B");
                        //this.getView().setEnable(false,"nckd_save");//锁定按钮
                        //this.getView().setEnable(false,"nckd_submit");//锁定按钮
                    } else {
                        this.getView().showErrorNotification("请先保存单据");
                        return;
                    }

                }
        }

    }

    public void click(EventObject evt) {
        super.click(evt);
        Control control = (Control) evt.getSource();
        String key = control.getKey();
        //一键还货按钮
        if (key.equalsIgnoreCase("nckd_return")) {

            //单据类型
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            if (billtype == null) {
                this.getView().showErrorNotification("单据类型为空");
                return;
            }
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if (id.equals("1980435141796826112") || nameq.equalsIgnoreCase("借货归还单")) {
                DynamicObject ywy = (DynamicObject) this.getModel().getValue("nckd_ywy", 0);
                if (ywy == null) {
                    this.getView().showErrorNotification("请先维护业务员!");
                    return;
                }
                DynamicObject dept = (DynamicObject) this.getModel().getValue("dept", 0);
                if (dept == null) {
                    this.getView().showErrorNotification("请先维护调入部门!");
                    return;
                }
                Object ywyId = ywy.getPkValue();
                DynamicObject org = (DynamicObject) this.getModel().getValue("org", 0);
                Object orgId = org.getPkValue();
                //获取基础资料的单据参数
                //BillParam billParam = ParameterHelper.getBillParam("nckd_xsyjhyebf_mob");
                MobileListShowParameter listPara = createShowMobileF7ListForm("nckd_xsyjhyebf", true);//第二个参数为是否支持多选;
                ListFilterParameter listFilterParameter = new ListFilterParameter();
                listFilterParameter.setFilter(new QFilter("nckd_qty", QCP.not_equals, 0)
                        .and("nckd_fapplyuserid.id", QCP.equals, ywyId)
                        .and("nckd_orgfield.id", QCP.equals, orgId));
                listPara.setListFilterParameter(listFilterParameter);
                // 设置回调
                listPara.setCloseCallBack(new CloseCallBack(this, "return"));
                this.getView().showForm(listPara);

            } else {
                this.getView().showErrorNotification("单据类型不为借货归还单,请修改!");
            }
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {

        super.closedCallBack(closedCallBackEvent);
        String key = closedCallBackEvent.getActionId();
        // 接收回调
        if (com.alibaba.druid.util.StringUtils.equalsIgnoreCase("return", key) && closedCallBackEvent.getReturnData() instanceof ListSelectedRowCollection) {
            ListSelectedRowCollection selectCollections = (ListSelectedRowCollection) closedCallBackEvent.getReturnData();
            ArrayList list = new ArrayList();
            for (ListSelectedRow row : selectCollections) {
                // list存储id
                list.add(row.getPrimaryKeyValue());
            }
            // 构造QFilter
            QFilter qFilter = new QFilter("id", QFilter.in, list);

            // 将选中的id对应的数据从数据库加载出来
            DynamicObjectCollection collections = QueryServiceHelper.query("nckd_xsyjhyebf",
                    "id,nckd_fmaterialid.number number,nckd_fmaterialid.name name,nckd_funitid.id unitId,nckd_qty,nckd_fwarehouseid.id stock,nckd_lotnum", qFilter.toArray(), "");
            if (!collections.isEmpty()) {
                //清空单据体
                this.getModel().deleteEntryData("billentry");
                this.getModel().batchCreateNewEntryRow("billentry", collections.size());
                DynamicObject dept = (DynamicObject) this.getModel().getValue("dept", 0);
                Object deptId = dept.getPkValue();
                DynamicObject org = (DynamicObject) this.getModel().getValue("org", 0);
                Object orgId = org.getPkValue();
                DynamicObject ph = (DynamicObject) this.getModel().getValue("nckd_regiongroup", 0);
                //从部门 仓库设置基础资料中获取对应仓库
                //部门对应仓库
                DynamicObject depStock = this.getStock(orgId, deptId, ph, "0");
                int row = 0;
                for (DynamicObject object : collections) {
                    Object matId = object.get("number");//物料编码
                    String matName = object.getString("name");//物料名称
                    Object unitId = object.get("unitId");//单位
                    BigDecimal qty = object.getBigDecimal("nckd_qty");//库存数量
                    Object stock = object.getString("stock");//仓库
                    String lotNum = object.getString("nckd_lotnum");//批号
                    this.getModel().setItemValueByNumber("material", matId.toString(), row);
                    this.getModel().setValue("materialname", matName, row);
                    this.getModel().setItemValueByID("unit", unitId, row);
                    this.getModel().setValue("qty", qty, row);
                    this.getModel().setItemValueByID("outwarehouse", stock, row);//调出仓库
                    this.getModel().setValue("warehouse", depStock, row);//调入仓库
                    this.getModel().setValue("inlotnumber", lotNum, row);
                    //this.getModel().setItemValueByNumber("lot", lotNum, row);
                    row++;
                }
            }

        }
    }

    /*
    创建移动列表
     */
    private static MobileListShowParameter createShowMobileF7ListForm(String formId, boolean isMultiSelect) {
        MobileListShowParameter para = new MobileListShowParameter();
        FormConfig formConfig = FormMetadataCache.getMobListFormConfig(formId);
        para.setCaption(formConfig.getCaption().toString());
        para.setLookUp(true);
        para.setBillFormId(formId);
        ShowType showType;
        if (formConfig.getShowType() == ShowType.MainNewTabPage) {
            showType = ShowType.Floating;
        } else {
            showType = ShowType.Modal;
            para.setPosition(MobileFormPosition.Bottom);
        }
        para.getOpenStyle().setShowType(showType);
        para.setMultiSelect(isMultiSelect);

        String f7ListFormId = formConfig.getF7ListFormId();
        if (StringUtils.isNotBlank(f7ListFormId)) {
            para.setFormId(f7ListFormId);
        }

        return para;
    }

    /*
    保存直接调拨单
     */
    private void save() {
        Long pkId = this.getPkId();
        //单据类型
        DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype");
        //业务员
        DynamicObject ywy = (DynamicObject) this.getModel().getValue("nckd_ywy");

        IFormView formView = null;
        try {
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
            formView = sm.getView(para.getPageId());
            if (formView != null) {
                // 设置视图应用id和数据模型
                formView.getFormShowParameter().setAppId(appId);
                formView.getModel().createNewData();
                formView.updateView();
            }
            BillModel mode = (BillModel) formView.getModel();
            mode.setPKValue(pkId);
            if (pkId > 0) {
                mode.load(pkId);
            }
            mode.setValue("billtype", billtype);
            mode.setValue("nckd_ywy", ywy);
            mode.deleteEntryData("billentry");
            DynamicObjectCollection entry = this.getModel().getEntryEntity("billentry");
            mode.batchCreateNewEntryRow("billentry", entry.size());
            int row = 0;
            for (DynamicObject entryRow : entry) {
                mode.setValue("material", entryRow.getDynamicObject("material"), row);
                mode.setValue("unit", entryRow.getDynamicObject("unit"), row);
                mode.setValue("qty", entryRow.getBigDecimal("qty"), row);
                mode.setValue("outwarehouse", entryRow.getDynamicObject("outwarehouse"), row);
                mode.setValue("warehouse", entryRow.getDynamicObject("warehouse"), row);
                mode.setValue("lotnumber", entryRow.getString("inlotnumber"), row);
                mode.setValue("inlotnumber", entryRow.getString("inlotnumber"), row);
                row++;
            }
            OperationResult saveOp = formView.invokeOperation("save");
            if (saveOp.isSuccess()) {
                formView.close();
            }
        } finally {
            if (formView != null) {
                formView.close();
            }
        }


    }

    /*
    更新调拨申请单
     */
    private void update(Long pkId) {
        DynamicObject dataObj = BusinessDataServiceHelper.loadSingle(pkId, targetBill);
        //业务员
        DynamicObject ywy = (DynamicObject) this.getModel().getValue("nckd_ywy");
        dataObj.set("nckd_ywy", ywy);
        SaveServiceHelper.update(dataObj);
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

    private Long getPkId() {
        Long pkId = 0L;
        String billno = this.getModel().getValue("billno").toString();
        QFilter qFilter = new QFilter("billno", QCP.equals, billno);
        DynamicObject collections = QueryServiceHelper.queryOne(targetBill,
                "id", qFilter.toArray());
        if (collections != null) {
            pkId = (Long) collections.get("id");
        }
        return pkId;
    }

    //获取部门对应仓库
    private DynamicObject getStock(Object orgId, Object deptId, DynamicObject pq, String jh) {
        DynamicObject depStock = null;
        //从部门 仓库设置基础资料中获取对应仓库
        // 构造QFilter
        QFilter depqFilter = new QFilter("createorg", QCP.equals, orgId)
                .and("status", QCP.equals, "C")
                .and("nckd_bm", QCP.equals, deptId)
                .and("nckd_isjh", QCP.equals, jh);//借货仓
        boolean pqSelect = false;
        if (pq != null) {
            Object pqPkId = pq.getPkValue();
            depqFilter.and("nckd_regiongroup", QCP.equals, pqPkId);
            pqSelect = true;
        } else {
            depqFilter.and("nckd_regiongroup", QCP.equals, 0L);
        }
        //查找部门对应仓库
        DynamicObjectCollection depcollections = QueryServiceHelper.query("nckd_bmcksz",
                "id,nckd_ck.id stockId", depqFilter.toArray(), "modifytime");
        if (!depcollections.isEmpty()) {
            DynamicObject stockItem = depcollections.get(0);
            String stockId = stockItem.getString("stockId");
            depStock = BusinessDataServiceHelper.loadSingle(stockId, "bd_warehouse");
        } else if (pqSelect) {
            // 构造QFilter
            QFilter nFilter = new QFilter("createorg", QCP.equals, orgId)
                    .and("status", QCP.equals, "C")
                    .and("nckd_bm", QCP.equals, deptId)
                    .and("nckd_isjh", QCP.equals, jh)//借货仓
                    .and("nckd_regiongroup", QCP.equals, 0L);
            //查找部门对应仓库
            DynamicObjectCollection query = QueryServiceHelper.query("nckd_bmcksz",
                    "id,nckd_ck.id stockId", nFilter.toArray(), "modifytime");
            if (!query.isEmpty()) {
                DynamicObject stockItem = query.get(0);
                String stockId = stockItem.getString("stockId");
                depStock = BusinessDataServiceHelper.loadSingle(stockId, "bd_warehouse");
            }

        }

        return depStock;
    }

}
