package nckd.yanye.occ.plugin.mobile;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.db.DB;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.IOperateInfo;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.*;
import kd.bos.form.cardentry.CardEntry;
import kd.bos.form.container.Tab;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.TabSelectEvent;
import kd.bos.form.control.events.TabSelectListener;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.operate.AbstractOperate;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.BillList;
import kd.bos.list.MobileSearch;
import kd.bos.orm.query.QFilter;
import kd.bos.orm.query.fulltext.QMatches;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.occ.ocbase.common.util.*;
import kd.occ.ocdma.business.order.SaleOrderHelper;
import kd.occ.ocdma.formplugin.OcdmaFormMobPlugin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/*
 * 要货订单移动列表插件
 * 表单标识：nckd_ocdma_saleorder_list
 * author:吴国强 2024-08-26
 */
public class MobileSalOrderListPlugIn extends OcdmaFormMobPlugin implements TabSelectListener {

    private static final String[] billStatusArray = new String[]{"D", "E"};
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addTabSelectListener(this, new String[]{"tabap"});
        this.addClickListeners(new String[]{"nckd_unsign"});
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        if (e.getOperateKey().equals("unsign")) {
            BillList billList = this.getControl("billlistap");
            Object id = billList.getCurrentSelectedRowInfo().getPrimaryKeyValue();
            //获取下游单据
            Map<String, HashSet<Long>> targetBillIds = BFTrackerServiceHelper.findTargetBills("ocbsoc_saleorder", new Long[]{(Long) id});
            // 从所有下游单中寻找需要的
            HashSet<Long> botpbill1_Ids = new HashSet<>();
            String botpbill1_EntityNumber = "im_saloutbill";//销售出库单
            if (targetBillIds.containsKey(botpbill1_EntityNumber)) {
                botpbill1_Ids = targetBillIds.get(botpbill1_EntityNumber);
            } else {
                this.getView().showErrorNotification("当前要货订单还未生成出库记录，无法拒签！");
                return;
            }
            for (Long deliveryBillId : botpbill1_Ids) {
                DynamicObject dataObject = BusinessDataServiceHelper.loadSingle(deliveryBillId, botpbill1_EntityNumber);
                String billstatus = dataObject.getString("billstatus");
                if (!"B".equalsIgnoreCase(billstatus)) {
                    this.getView().showErrorNotification("当前要货订单的出库状态未提交，无法拒签！");
                    return;
                }
                dataObject.set("nckd_autosign",false);
                OperateOption auditOption = OperateOption.create();
                auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
                auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
                //审核
                OperationResult auditResult = OperationServiceHelper.executeOperate("audit", botpbill1_EntityNumber, new DynamicObject[]{dataObject}, auditOption);
                if (!auditResult.isSuccess()) {
                    String detailMessage = auditResult.getMessage();
                    // 演示提取保存详细错误
                    for (IOperateInfo errInfo : auditResult.getAllErrorOrValidateInfo()) {
                        detailMessage += errInfo.getMessage();
                    }
                    this.getView().showErrorNotification("出库单审核失败：" + detailMessage);
                    return;
                }
                Object pkId=dataObject.getPkValue();
                Map<String, HashSet<Long>> targetBills = BFTrackerServiceHelper.findTargetBills("im_saloutbill", new Long[]{(Long) pkId});
                String  delivery_record= "ocbsoc_delivery_record";//发货记录
                if (targetBills.containsKey(delivery_record)) {
                    HashSet<Long> delivery_Ids = targetBills.get(delivery_record);
                    for (Long Id : delivery_Ids) {
                        DynamicObject delivery = BusinessDataServiceHelper.loadSingle(Id, delivery_record);
                        OperationResult unsignResult = OperationServiceHelper.executeOperate("unsign", delivery_record, new DynamicObject[]{delivery});
                        if (!unsignResult.isSuccess()) {
                            String detailMessage = "";
                            // 演示提取保存详细错误
                            for (IOperateInfo errInfo : unsignResult.getAllErrorOrValidateInfo()) {
                                detailMessage += errInfo.getMessage();
                            }
                            this.getView().showErrorNotification("发货记录拒签失败：" + detailMessage);
                            return;
                        }
                    }

                }
            }
            //刷新列表
            billList.refresh();
            this.getView().showSuccessNotification("拒签成功！");

        }
        //自定义签收处理
        else if(e.getOperateKey().equalsIgnoreCase("nckdsign")){
            BillList billList = this.getControl("billlistap");
            Object id = billList.getCurrentSelectedRowInfo().getPrimaryKeyValue();
            //获取下游单据
            Map<String, HashSet<Long>> targetBillIds = BFTrackerServiceHelper.findTargetBills("ocbsoc_saleorder", new Long[]{(Long) id});
            // 从所有下游单中寻找需要的
            HashSet<Long> botpbill1_Ids = new HashSet<>();
            String botpbill1_EntityNumber = "im_saloutbill";//销售出库单
            if (targetBillIds.containsKey(botpbill1_EntityNumber)) {
                botpbill1_Ids = targetBillIds.get(botpbill1_EntityNumber);
            } else {
                this.getView().showErrorNotification("当前要货订单还未生成出库记录，无法签收！");
                return;
            }
            for (Long deliveryBillId : botpbill1_Ids) {
                DynamicObject dataObject = BusinessDataServiceHelper.loadSingle(deliveryBillId, botpbill1_EntityNumber);
                String billstatus = dataObject.getString("billstatus");
                if (!"B".equalsIgnoreCase(billstatus)) {
                    this.getView().showErrorNotification("当前要货订单的出库状态未提交，无法签收！");
                    return;
                }
                dataObject.set("nckd_autosign",true);
                OperateOption auditOption = OperateOption.create();
                auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
                auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
                //审核
                OperationResult auditResult = OperationServiceHelper.executeOperate("audit", botpbill1_EntityNumber, new DynamicObject[]{dataObject}, auditOption);
                if (!auditResult.isSuccess()) {
                    String detailMessage = auditResult.getMessage();
                    // 演示提取保存详细错误
                    for (IOperateInfo errInfo : auditResult.getAllErrorOrValidateInfo()) {
                        detailMessage += errInfo.getMessage();
                    }
                    this.getView().showErrorNotification("出库单审核失败：" + detailMessage);
                    return;
                }
            }
            //刷新列表
            billList.refresh();
            this.getView().showSuccessNotification("签收成功！");

        }
        //自定义退货申请
        else if(e.getOperateKey().equalsIgnoreCase("return")){
            BillList billList = this.getControl("billlistap");
            Object id = billList.getCurrentSelectedRowInfo().getPrimaryKeyValue();
            String sourceBill = "ocbsoc_saleorder";//要货订单
            String targetBill = "ocbsoc_returnorder";//退货申请
            //String ruleId = "2025459918533834752";//单据转换Id

            List<ListSelectedRow> selectedRows = new ArrayList<>();
            ListSelectedRow row = new ListSelectedRow();
            //必填，设置源单单据id
            row.setPrimaryKeyValue(id);
            //可选，设置源单分录标识
            //row.setEntryEntityKey("billentry");
            //可选，设置源单分录id
            //row.setEntryPrimaryKeyValue(entryId);
            selectedRows.add(row);

            // 创建下推参数
            PushArgs pushArgs = new PushArgs();
            // 必填，源单标识
            pushArgs.setSourceEntityNumber(sourceBill);
            // 必填，目标单标识
            pushArgs.setTargetEntityNumber(targetBill);
            // 可选，传入true，不检查目标单新增权
            pushArgs.setHasRight(true);
            // 可选，传入目标单验权使用的应用编码
            //pushArgs.setAppId("");
            // 可选，传入目标单主组织默认值
            //pushArgs.setDefOrgId(orgId);
            //可选，转换规则id
            pushArgs.setRuleId("");
            //自动保存
            pushArgs.setAutoSave(true);
            // 可选，是否输出详细错误报告
            pushArgs.setBuildConvReport(true);
            // 必选，设置需要下推的源单及分录内码
            pushArgs.setSelectedRows(selectedRows);
            // 调用下推引擎，下推目标单
            ConvertOperationResult pushResult = ConvertServiceHelper.pushAndSave(pushArgs);
            Set<Object> targetBillIds = pushResult.getTargetBillIds();
            if (targetBillIds.isEmpty()){
                String message = pushResult.getMessage();
                if (StringUtils.isEmpty(message)){
                    if (pushResult.getBillReports()!=null&&!pushResult.getBillReports().isEmpty()){
                        message=pushResult.getBillReports().get(0).getFailMessage();
                    }
                }
                this.getView().showErrorNotification(message);
                return;
            }
            for(Object pkId:targetBillIds){
                MobileFormShowParameter mobileFormShowParameter = new MobileFormShowParameter();
                mobileFormShowParameter.getOpenStyle().setShowType(ShowType.Floating);
                mobileFormShowParameter.setFormId("ocbsoc_returnmobedit");
                mobileFormShowParameter.setStatus(OperationStatus.EDIT);
                mobileFormShowParameter.setCustomParam("returnorderid", pkId);
                String supplierIdStr = (String)this.getView().getParentView().getFormShowParameter().getCustomParam("supplierid");
                if (StringUtils.isNotNull(supplierIdStr)) {
                    mobileFormShowParameter.setCustomParam("supplierid", supplierIdStr);
                }

                mobileFormShowParameter.setCloseCallBack(new CloseCallBack(this, "modify"));
                this.getView().showForm(mobileFormShowParameter);
            }
        }

    }

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        String formid = this.getView().getParentView().getFormShowParameter().getFormId();
        if ("ocsaa_home".equals(formid)) {
            this.setVisible(new String[]{"btnclose"});
        } 
    }
    public void afterCreateNewData(EventObject e) {
        //父页面标识
        String formid = this.getView().getParentView().getFormShowParameter().getFormId();
        //父页面为销售助手首页
        if ("ocsaa_home".equals(formid)) {
            //标记
            String lable= (String)this.getView().getFormShowParameter().getCustomParam("lable");
            if(lable!=null){
                //日期
                String orderDateSpan = (String)this.getView().getFormShowParameter().getCustomParam("orderDateSpan");
                this.getModel().setValue("orderdatespan", orderDateSpan);
                //状态
                String tabap = (String)this.getView().getFormShowParameter().getCustomParam("tabap");
                String supplierIdStr;
                if (StringUtils.isNotNull(tabap)) {
                    Tab tab = (Tab)this.getControl("tabap");
                    supplierIdStr = tabap;
                    tab.selectTab(supplierIdStr);
                    tab.activeTab(supplierIdStr);
                }
                this.refreshBillList();
            }

        }
    }

    public void tabSelected(TabSelectEvent tabSelectEvent) {
        switch (((Control)tabSelectEvent.getSource()).getKey().toLowerCase()) {
            case "tabap":
                BillList billList = (BillList)this.getControl("billlistap");
                MobileControlUtils.BillListRefresh(billList, new QFilter[]{this.getOrderFilter(tabSelectEvent.getTabKey())});
            default:
        }
    }

    public void propertyChanged(PropertyChangedArgs e) {
        switch (e.getProperty().getName()) {
            case "enddate":
            case "startdate":
            case "orderdatespan":
                this.refreshBillList();
            default:
        }
    }

    private void refreshBillList() {
        BillList billList = (BillList)this.getControl("billlistap");
        Tab tab = (Tab)this.getControl("tabap");
        String curTabKey = tab.getCurrentTab();
        MobileControlUtils.BillListRefresh(billList, new QFilter[]{this.getOrderFilter(curTabKey)});
    }
    private QFilter getOrderFilter(String key) {
        QFilter filter = SaleOrderHelper.getOrderChannelFilter();
        filter.and("billtypeid", "!=", 100001L);
        switch (key) {
            case "tp_all":
                break;
            case "a":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "b":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "c":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "d":
                filter.and("billstatus", "in", billStatusArray);
                break;
            case "f":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "nckd_s":
                filter.and("paystatus","!=","C");//收款状态不等于已收款
                filter.and("billstatus","!=","G");//单据状态不为拒签
        }

        MobileSearch search = (MobileSearch)this.getControl("ordersearch");
        String searchText = search.getText();
        if (searchText != null && !"".equals(searchText.trim())) {
            filter = filter.and(QMatches.ftlike(new String[]{searchText}, new String[]{"billno,itementry.itemid.name,itementry.itemid.number,nckd_salerid.operatorname"}));
        }

        this.setDateFilter(filter);
        Object fromFormId = this.getParameter("fromFormId");
        if ("ocsaa_channel".equals(fromFormId)) {
            long channelId = (Long)this.getParameter("channelId");
            filter.and("orderchannelid", "=", channelId);
        }

        return filter;
    }
    private void setDateFilter(QFilter filter) {
        Date startDate = (Date)this.getValue("startdate");
        Date endDate = (Date)this.getValue("enddate");
        if (startDate != null && endDate != null) {
            filter.and("orderdate", ">=", DateUtil.getDayFirst(startDate));
            filter.and("orderdate", "<=", DateUtil.getDayLast(endDate));
        } else {
            Object orderDateSpan = this.getValue("orderdatespan");
            String selectDate = "F";
            if (!CommonUtils.isNull(orderDateSpan)) {
                selectDate = orderDateSpan.toString();
            }

            switch (selectDate) {
                case "A":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(6L))));
                    break;
                case "B":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.getFirstDayOfMonth()));
                    break;
                case "C":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(90L))));
                    break;
                case "D":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(2L))));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().minusDays(2L))));
                    break;
                case "E":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(1L))));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().minusDays(1L))));
                    break;
                case "G":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().plusDays(1L))));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().plusDays(1L))));
                    break;
                case "F":
                default:
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.getNowDate()));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.getNowDate()));
            }

        }
    }

}
