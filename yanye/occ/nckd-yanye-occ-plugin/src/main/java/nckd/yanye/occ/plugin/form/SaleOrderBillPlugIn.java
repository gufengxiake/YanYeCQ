package nckd.yanye.occ.plugin.form;

import com.alibaba.druid.util.StringUtils;
import com.ccb.core.date.DateUtil;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import   kd.bos.form.operate.FormOperate;
import kd.bos.form.*;
import kd.bos.form.control.Control;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import   kd.bos.dataentity.RefObject;

import java.math.BigDecimal;
import java.util.*;

/*
 *销售订单表单插件，获取销售合同和运输合同
 * 表单标识：nckd_sm_salorder_ext
 * author:吴国强 2024-07-22
 */

public class SaleOrderBillPlugIn extends AbstractBillPlugIn {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //销售合同
        TextEdit salecontractno = getControl("nckd_salecontractno");
        if(salecontractno!=null){
            salecontractno.addClickListener(this);
        }
        //运输合同
        TextEdit trancon = getControl("nckd_trancontractno");
        if(trancon!=null){
            trancon.addClickListener(this);
        }
    }

    @Override
    public void click(EventObject evt) {
        Control control = (Control) evt.getSource();
        String key = control.getKey();
        // 点击销售合同单号字段,打开销售合同单据列表界面
        if (StringUtils.equalsIgnoreCase("nckd_salecontractno", key)) {
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            if (org == null) {
                this.getView().showErrorNotification("请先选择销售组织");
                return;
            }
            DynamicObject customer = (DynamicObject) this.getModel().getValue("customer");
            if (customer == null) {
                this.getView().showErrorNotification("请先选择订货客户");
                return;
            }
            Date bizdate = (Date) this.getModel().getValue("bizdate");
            if(bizdate == null){
                this.getView().showErrorNotification("请先填写订单日期");
                return;
            }
            //打开销售合同列表
            ListShowParameter parameter = ShowFormHelper.createShowListForm("conm_salcontract", false);
            //过滤列表
            ListFilterParameter listFilterParameter = new ListFilterParameter();
            listFilterParameter.setFilter(new QFilter("org.id", QCP.equals, org.getPkValue())
                            .and("customer.id", QCP.equals, customer.getPkValue())
                            .and("closestatus",QCP.equals,"A")//关闭状态
                            .and("biztimeend",QCP.large_equals, DateUtil.beginOfDay(bizdate))//截止日期
                            .and("biztimebegin",QCP.less_equals,DateUtil.endOfDay(bizdate))//起始日期
                    //.and("billstatus",QCP.equals,"C")
            );
            parameter.setListFilterParameter(listFilterParameter);
            //设置回调
            parameter.setCloseCallBack(new CloseCallBack(this, "salcontract"));
            getView().showForm(parameter);
        } else if (StringUtils.equalsIgnoreCase("nckd_trancontractno", key)) {
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            if (org == null) {
                this.getView().showErrorNotification("请先选择销售组织");
                return;
            }
            Date bizdate = (Date) this.getModel().getValue("bizdate");
            if(bizdate == null){
                this.getView().showErrorNotification("请先填写订单日期");
                return;
            }
//            DynamicObject customer = (DynamicObject) this.getModel().getValue("nckd_customer");
//            if (customer == null) {
//                this.getView().showErrorNotification("请先选择承运商");
//                return;
//            }
            //打开采购合同的运输合同列表
            ListShowParameter parameter = ShowFormHelper.createShowListForm("conm_purcontract", false);
            //过滤列表
            ListFilterParameter listFilterParameter = new ListFilterParameter();
            listFilterParameter.setFilter(new QFilter("org.id", QCP.equals, org.getPkValue())
                            .and("type.name",QCP.equals,"采购运费合同")
                            .and("closestatus",QCP.equals,"A")//关闭状态
                            .and("biztimeend",QCP.large_equals, DateUtil.beginOfDay(bizdate))//截止日期
                            .and("biztimebegin",QCP.less_equals,DateUtil.endOfDay(bizdate))//起始日期
                    //.and("supplier.id", QCP.equals, customer.getPkValue())
                    //.and("billstatus",QCP.equals,"C")
            );
            parameter.setListFilterParameter(listFilterParameter);
            //设置分录行支持被选择
            parameter.setCustomParam("ismergerows", false);
            //设置回调
            parameter.setCloseCallBack(new CloseCallBack(this, "trancontract"));
            getView().showForm(parameter);

        }
        super.click(evt);
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        if("nckd_freighttype".equals(fieldKey)){
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            if (org == null) {
                this.getView().showErrorNotification("请先选择销售组织");
                return;
            }
            Date bizdate = (Date) this.getModel().getValue("bizdate");
            if(bizdate == null){
                this.getView().showErrorNotification("请先填写订单日期");
                return;
            }
            ChangeData[] changeSet = e.getChangeSet();
            String newValue = (String) changeSet[0].getNewValue();
            DynamicObject fSupplier = QueryServiceHelper.queryOne("nckd_freight_supplier", "nckd_supplier", new QFilter[]{new QFilter("number",QCP.equals,newValue)});
            if (fSupplier == null || fSupplier.get("nckd_supplier") == null){
                return;
            }
            long supplierId = fSupplier.getLong("nckd_supplier");
            QFilter purFilter = new QFilter("org.id", QCP.equals, org.getPkValue())
                    .and("type.name",QCP.equals,"采购运费合同")
                    .and("closestatus",QCP.equals,"A")//关闭状态
                    .and("biztimeend",QCP.large_equals, DateUtil.beginOfDay(bizdate))//截止日期
                    .and("biztimebegin",QCP.less_equals,DateUtil.endOfDay(bizdate))//起始日期
                    .and("supplier",QCP.equals,supplierId);
            DynamicObject purContract = BusinessDataServiceHelper.loadSingle("conm_purcontract", new QFilter[]{purFilter});
            if (purContract != null){
                this.getModel().setValue("nckd_trancontractno",purContract.get("billno"));
                this.getModel().setValue("nckd_customer", supplierId);//承运商
                //分类明细信息
                DynamicObjectCollection entryData = purContract.getDynamicObjectCollection("billentry");
                entryData.forEach((row)->{
                        //含税单价
                        BigDecimal taxPrice = row.getBigDecimal("priceandtax");
                        //合理途损率
                        BigDecimal damagerate = row.getBigDecimal("nckd_damagerate");
                        int entryRowCount = this.getModel().getEntryRowCount("billentry");
                        for (int i = 0; i < entryRowCount; i++) {
                            this.getModel().setValue("nckd_yfprice", taxPrice, i);
                            this.getModel().setValue("nckd_damagerate", damagerate, i);
                        }
                });
            }else{
                this.getModel().setValue("nckd_trancontractno",null);
                this.getModel().setValue("nckd_customer",null);
                int entryRowCount = this.getModel().getEntryRowCount("billentry");
                for (int i = 0; i < entryRowCount; i++) {
                    this.getModel().setValue("nckd_yfprice", 0, i);
                    this.getModel().setValue("nckd_damagerate", 0, i);
                }
            }

        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent evt) {
        String key = evt.getActionId();
        Object returnData = evt.getReturnData();
        // 将选择的销售合同数据回写至样例单据上的相应字段
        if (StringUtils.equalsIgnoreCase("salcontract", key) && returnData != null) {
            ListSelectedRow row = ((ListSelectedRowCollection) returnData).get(0);
            //DynamicObject billObj = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "conm_salcontract");
            getModel().setValue("nckd_salecontractno", row.getBillNo());//销售合同编码
        } else if (StringUtils.equalsIgnoreCase("trancontract", key) && returnData != null) {
            if(((ListSelectedRowCollection)returnData).size()>1){
                this.getView().showErrorNotification("请勿选中多行记录！");
                return;
            }
            ListSelectedRow row = ((ListSelectedRowCollection) returnData).get(0);
            DynamicObject billObj = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "conm_purcontract");
            getModel().setValue("nckd_trancontractno", row.getBillNo());//运费合同编码
            getModel().setValue("nckd_customer", billObj.get("supplier"));//承运商
            //分类明细信息
            DynamicObjectCollection entryData = billObj.getDynamicObjectCollection("billentry");
            for(DynamicObject rowData:entryData){
                if(rowData.getPkValue().equals(row.getEntryPrimaryKeyValue())){
                    //含税单价
                    BigDecimal taxPrice = rowData.getBigDecimal("priceandtax");
                    //合理途损率
                    BigDecimal damagerate = rowData.getBigDecimal("nckd_damagerate");
                    getModel().setValue("nckd_trancontractno", row.getBillNo());//运费合同编码
                    int entryRowCount = this.getModel().getEntryRowCount("billentry");
                    for (int i = 0; i < entryRowCount; i++) {
                        getModel().setValue("nckd_yfprice", taxPrice, i);
                        getModel().setValue("nckd_damagerate", damagerate, i);
                    }
                }
            }
        }
        //this.getView().invokeOperation("save");//保存单据
        super.closedCallBack(evt);
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate operate = (FormOperate)args.getSource();
        if("submit".equals(operate.getOperateKey())){
            Map<Long,BigDecimal> checkMaps = this.getSalContract();
            if(checkMaps.isEmpty()){
                return;
            }
            int entryRowCount = this.getModel().getEntryRowCount("billentry");
            for (int i = 0; i < entryRowCount; i++) {
                long material = (long) ((DynamicObject)this.getModel().getValue("material", i)).getPkValue();
                BigDecimal priceandtax = (BigDecimal) this.getModel().getValue("priceandtax", i);
                RefObject<String> afterConfirm = new RefObject<>();
                if (checkMaps.containsKey(material) && priceandtax.compareTo(checkMaps.get(material)) != 0 && !operate.getOption().tryGetVariableValue("isSubmit", afterConfirm)){
                    // 显示确认消息
                    ConfirmCallBackListener confirmCallBacks = new ConfirmCallBackListener("submit", this);
                    String confirmTip = "物料行含税单价与销售合同不一致,是否提交?";
                    this.getView().showConfirm(confirmTip, MessageBoxOptions.YesNo, ConfirmTypes.Default, confirmCallBacks);

                    // 在没有确认之前，先取消本次操作
                    args.setCancel(true);
                }
            }
        }

    }

    public Map<Long,BigDecimal> getSalContract(){
        Map<Long,BigDecimal> checkMaps = new HashMap<>();
        String nckdSalecontractno = (String) this.getModel().getValue("nckd_salecontractno");
        if(nckdSalecontractno.isEmpty()){
            return checkMaps;
        }
        QFilter qFilter = new QFilter("billno",QCP.equals,nckdSalecontractno);
        DynamicObjectCollection conmSalcontract = QueryServiceHelper.query("conm_salcontract", "billno,billentry.material as material,billentry.priceandtax as priceandtax", new QFilter[]{qFilter});
        if (conmSalcontract == null){
            return checkMaps;
        }
        conmSalcontract.forEach((e)->{
            checkMaps.put(e.getLong("material"),e.getBigDecimal("priceandtax"));
        });
        return checkMaps;
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        if (StringUtils.equals(messageBoxClosedEvent.getCallBackId(),"submit" ) && messageBoxClosedEvent.getResult() == MessageBoxResult.Yes) {
            OperateOption operateOption = OperateOption.create();
            operateOption.setVariableValue("isSubmit", "true");
            //
            this.getView().invokeOperation("submit",operateOption);
        }


    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        switch (e.getOperateKey()) {
            case "save":
                if (e.getOperationResult().isSuccess()) {
                    String targetEntityNumber = this.getModel().getDataEntityType().getName();
                    Object pkId=this.getModel().getValue("id");
                    String trancontractNo = this.getModel().getValue("nckd_trancontractno").toString();
                    String salecontractNo = this.getModel().getValue("nckd_salecontractno").toString();
                    if (!trancontractNo.trim().equals("") || !salecontractNo.trim().equals("")) {
                        Map<String, HashSet<Long>> targetBills = BFTrackerServiceHelper.findTargetBills(targetEntityNumber, new Long[]{(Long) pkId});
                        //发货通知单
                        if (targetBills.containsKey("sm_delivernotice")) {
                            HashSet<Long> targetBillIds = targetBills.get("sm_delivernotice");
                            this.updateContractNo("sm_delivernotice", targetBillIds, trancontractNo, salecontractNo);
                        }
                        //电子磅单
                        if (targetBills.containsKey("nckd_eleweighing")) {
                            HashSet<Long> targetBillIds = targetBills.get("nckd_eleweighing");
                            this.updateContractNo("nckd_eleweighing", targetBillIds, trancontractNo, salecontractNo);
                        }
                        //销售出库单
                        if (targetBills.containsKey("im_saloutbill")) {
                            HashSet<Long> targetBillIds = targetBills.get("im_saloutbill");
                            this.updateContractNo("im_saloutbill", targetBillIds, trancontractNo, salecontractNo);
                        }
                        //签收单
                        if (targetBills.containsKey("nckd_signaturebill")) {
                            HashSet<Long> targetBillIds = targetBills.get("nckd_signaturebill");
                            this.updateContractNo("nckd_signaturebill", targetBillIds, trancontractNo, null);
                        }
                        //暂估应付单
                        if (targetBills.containsKey("ap_busbill")) {
                            HashSet<Long> targetBillIds = targetBills.get("ap_busbill");
                            this.updateApContractNo("ap_busbill", targetBillIds,"entry", trancontractNo );
                        }
                        //财务应付单
                        if (targetBills.containsKey("ap_finapbill")) {
                            HashSet<Long> targetBillIds = targetBills.get("ap_finapbill");
                            this.updateApContractNo("ap_finapbill", targetBillIds,"detailentry", trancontractNo );
                        }

                    }

                }

        }

    }

    /*
    更新下游单据的销售合同和运输合同号
     */
    private void updateContractNo(String targetBill, HashSet<Long> targetBillIds, String trancontractNo, String salecontractNo) {

        DynamicObject[] targetDynamic = (DynamicObject[]) BusinessDataServiceHelper.load(targetBillIds.toArray(), BusinessDataServiceHelper.newDynamicObject(targetBill).getDataEntityType());
        if (targetDynamic != null) {
            for (DynamicObject targetData : targetDynamic) {
                if (salecontractNo != null) {
                    targetData.set("nckd_salecontractno", salecontractNo);
                }
                targetData.set("nckd_trancontractno", trancontractNo);
            }
            SaveServiceHelper.update(targetDynamic);
        }
    }
    //更新暂估应付和财务应付
    private void updateApContractNo(String targetBill, HashSet<Long> targetBillIds,String entryName, String trancontractNo) {
        DynamicObject[] targetDynamic = (DynamicObject[]) BusinessDataServiceHelper.load(targetBillIds.toArray(), BusinessDataServiceHelper.newDynamicObject(targetBill).getDataEntityType());
        if (targetDynamic != null) {
            for (DynamicObject targetData : targetDynamic) {
                DynamicObjectCollection entry=targetData.getDynamicObjectCollection(entryName);
                for(DynamicObject entryRow:entry){
                    entryRow.set("nckd_carriagenumber",trancontractNo);
                }
            }
            SaveServiceHelper.update(targetDynamic);
        }
    }

}
