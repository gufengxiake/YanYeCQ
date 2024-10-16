package nckd.yanye.occ.plugin.form;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.Control;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
//import kd.mmc.pdm.common.constants.BomBatchSearchConst;

import java.math.BigDecimal;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Map;

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
            //打开销售合同列表
            ListShowParameter parameter = ShowFormHelper.createShowListForm("conm_salcontract", false);
            //过滤列表
            ListFilterParameter listFilterParameter = new ListFilterParameter();
            listFilterParameter.setFilter(new QFilter("org.id", QCP.equals, org.getPkValue())
                            .and("customer.id", QCP.equals, customer.getPkValue())
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
