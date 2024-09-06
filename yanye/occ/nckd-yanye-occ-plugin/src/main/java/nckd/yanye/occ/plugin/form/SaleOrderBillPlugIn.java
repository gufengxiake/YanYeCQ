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
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
//import kd.mmc.pdm.common.constants.BomBatchSearchConst;

import java.math.BigDecimal;
import java.util.EventObject;

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
            ListSelectedRow row = ((ListSelectedRowCollection) returnData).get(0);
            DynamicObject billObj = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "conm_purcontract");
            getModel().setValue("nckd_trancontractno", row.getBillNo());//运费合同编码
            getModel().setValue("nckd_customer", billObj.get("supplier"));//承运商
            //分类明细信息
            DynamicObjectCollection entryData = billObj.getDynamicObjectCollection("billentry");
            DynamicObject firstRowData = entryData.get(0);
            //含税单价
            BigDecimal taxPrice = firstRowData.getBigDecimal("priceandtax");
            //合理途损率
            BigDecimal damagerate = firstRowData.getBigDecimal("nckd_damagerate");
            getModel().setValue("nckd_trancontractno", row.getBillNo());//运费合同编码
            int entryRowCount = this.getModel().getEntryRowCount("billentry");
            for (int i = 0; i < entryRowCount; i++) {
                getModel().setValue("nckd_yfprice", taxPrice, i);
                getModel().setValue("nckd_damagerate", damagerate, i);
            }

        }
        //this.getView().invokeOperation("save");//保存单据
        super.closedCallBack(evt);
    }

}
