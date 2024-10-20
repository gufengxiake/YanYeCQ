package nckd.yanye.tmc.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowType;
import kd.bos.form.StyleCss;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.EventObject;

/**
 * 资金-付息处理列表插件
 * 表单标识：cfm_interestbill
 * author：xiaoxiaopeng
 * date：2024-08-19
 */
public class CfmInterestBillListPlugin extends AbstractListPlugin {

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        String itemKey = evt.getItemKey();
        if ("nckd_designatec".equals(itemKey)){
            openListShowParameter("bos_listf7","ap_invoice");
        }
        if ("nckd_canceldesignatec".equals(itemKey)){
            deleteInvoiceEntry();
        }
    }

    /**
     * 清空指定发票
     */
    private void deleteInvoiceEntry() {
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        if (selectedRows == null || selectedRows.size() == 0){
            return;
        }
        if (selectedRows.size() > 1){
            this.getView().showErrorNotification("请选择一条数据取消指定发票");
            return;
        }
        Object primaryKeyValue = selectedRows.get(0).getPrimaryKeyValue();
        DynamicObject cfm = BusinessDataServiceHelper.loadSingle(primaryKeyValue, "cfm_interestbill");
        DynamicObjectCollection nckdInventry = cfm.getDynamicObjectCollection("nckd_inventry");
        if (nckdInventry == null || nckdInventry.size() == 0){
            this.getView().showErrorNotification("当前选中行无指定发票,无需取消指定");
            return;
        }
        for (DynamicObject d : nckdInventry) {
            DynamicObject invoice = BusinessDataServiceHelper.loadSingle("ap_invoice", "id,invoicecode,invoiceno,nckd_remainderamount",
                    new QFilter[]{new QFilter("invoicecode", QCP.equals, d.get("nckd_i_invoicecode")).and(new QFilter("invoiceno", QCP.equals, d.get("nckd_i_invoiceno")))});
            BigDecimal nckdIUsedamt = d.getBigDecimal("nckd_i_usedamt");
            updateInvoice(invoice, nckdIUsedamt.add(invoice.getBigDecimal("nckd_remainderamount")));
        }
        nckdInventry.clear();
        cfm.set("nckd_receiptinvoice",false);
        try {
            SaveServiceHelper.save(new DynamicObject[]{cfm});
            this.getView().updateView();
            this.getView().showMessage("取消发票指定成功");
        }catch (Exception ex){
            this.getView().showMessage(ex.getMessage());
        }
    }

    /**
     * 打开列表before7页面
     */
    private void openListShowParameter(String listf7, String bill) {
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        if (selectedRows == null || selectedRows.size() == 0){
            return;
        }
        if (selectedRows.size() > 1){
            this.getView().showErrorNotification("请选择一条数据指定发票");
            return;
        }
        Object primaryKeyValue = selectedRows.get(0).getPrimaryKeyValue();
        DynamicObject cfm = BusinessDataServiceHelper.loadSingle(primaryKeyValue, "cfm_interestbill");
        DynamicObject org = cfm.getDynamicObject("org");
        if (org == null){
            this.getView().showErrorNotification("选中数据未填写借款人");
            return;
        }
        Object actualinstamt = cfm.get("actualinstamt");
        if (actualinstamt == null || actualinstamt.equals(BigDecimal.ZERO)){
            this.getView().showErrorNotification("选中数据未填写付息金额");
            return;
        }
        ListShowParameter listShowParameter = new ListShowParameter();
        listShowParameter.setLookUp(true);
        listShowParameter.setFormId(listf7);//列表界面
        listShowParameter.setBillFormId(bill);  //单据的标识
        listShowParameter.setF7Style(0);
        listShowParameter.setMultiSelect(true);
        StyleCss styleCss = new StyleCss();
        styleCss.setHeight("580");
        styleCss.setWidth("960");
        listShowParameter.getOpenStyle().setInlineStyleCss(styleCss);
        //设置过滤条件
        ListFilterParameter listFilterParameter = new ListFilterParameter();
        QFilter qFilter = setInvoiceFilter(cfm);
        listFilterParameter.setFilter(qFilter);
        listShowParameter.setListFilterParameter(listFilterParameter);

        listShowParameter.getOpenStyle().setShowType(ShowType.Modal);
        listShowParameter.setCloseCallBack(new CloseCallBack(this,bill));
        this.getView().showForm(listShowParameter);
    }

    /**
     * 过滤条件
     * @param dataEntity
     * @return
     */
    private QFilter setInvoiceFilter(DynamicObject dataEntity) {
        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C");
        qFilter.and(new QFilter("org.id",QCP.equals,dataEntity.getDynamicObject("org").getPkValue()));
        qFilter.and(new QFilter("asstactname",QCP.equals,dataEntity.getString("textcreditor")));
        qFilter.and(new QFilter("currency.id",QCP.equals,dataEntity.getDynamicObject("currency").getPkValue()));
        qFilter.and(new QFilter("nckd_remainderamount",QCP.large_than,0));
        DynamicObjectCollection cfms = dataEntity.getDynamicObjectCollection("nckd_inventry");
        if (cfms != null && cfms.size() > 0){
            for (DynamicObject cfm : cfms) {
                qFilter.and(new QFilter("invoicecode",QCP.not_equals,cfm.getString("nckd_i_invoicecode")));
                qFilter.and(new QFilter("invoiceno",QCP.not_equals,cfm.getString("nckd_i_invoiceno")));
            }
        }
        return qFilter;
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        super.closedCallBack(e);
        ListSelectedRow listSelectedRow = this.getSelectedRows().get(0);
        DynamicObject interestbill = BusinessDataServiceHelper.loadSingle(listSelectedRow.getPrimaryKeyValue(), "cfm_interestbill");
        String actionId = e.getActionId();
        if (StringUtils.equals(actionId,"ap_invoice") && actionId != null) {
            ListSelectedRowCollection returnData = (ListSelectedRowCollection) e.getReturnData();
            if (returnData == null){
                return;
            }
            //先清空发票
            DynamicObjectCollection entry = interestbill.getDynamicObjectCollection("nckd_inventry");
            BigDecimal amount = BigDecimal.ZERO;//当前单据发票累加金额
            BigDecimal actualinstamt = interestbill.getBigDecimal("actualinstamt");//当前付息金额
            for (DynamicObject d : entry) {
                amount = amount.add(d.getBigDecimal("nckd_i_usedamt"));
            }
            if (amount.compareTo(actualinstamt) > -1){
                for (DynamicObject d : entry) {
                    DynamicObject invoice = BusinessDataServiceHelper.loadSingle("ap_invoice", "id,invoicecode,invoiceno,nckd_remainderamount",
                            new QFilter[]{new QFilter("invoicecode", QCP.equals, d.get("nckd_i_invoicecode")).and(new QFilter("invoiceno", QCP.equals, d.get("nckd_i_invoiceno")))});
                    BigDecimal nckdIUsedamt = d.getBigDecimal("nckd_i_usedamt");
                    updateInvoice(invoice, nckdIUsedamt.add(invoice.getBigDecimal("nckd_remainderamount")));
                }
                entry.clear();
            }
            int count = 1;
            for (ListSelectedRow date : returnData) {
                DynamicObject invoice = BusinessDataServiceHelper.loadSingle(date.getPrimaryKeyValue(), "ap_invoice");
                Boolean result = verifyInvoice(invoice,interestbill);
                if (!result){
                    this.getView().showErrorNotification("选中第"+count+"条发票，关联的银行付息单金额已超出发票金额");
                    return;
                }
                setInvoice(invoice,interestbill);
                count++;
            }
            DynamicObjectCollection entity = interestbill.getDynamicObjectCollection("nckd_inventry");
            if (entity.size() > 0){
                interestbill.set("nckd_receiptinvoice",true);
            }else {
                interestbill.set("nckd_receiptinvoice",false);
            }
            try {
                SaveServiceHelper.save(new DynamicObject[]{interestbill});
                this.getView().showMessage("发票指定成功");
            }catch (Exception ex){
                this.getView().showMessage(ex.getMessage());
            }
        }
    }

    private void setInvoice(DynamicObject invoice,DynamicObject interestbill) {
        DynamicObjectCollection entry = interestbill.getDynamicObjectCollection("nckd_inventry");
        DynamicObject newEntry = entry.addNew();
        /**
         * 写入单据体
         */
        newEntry.set("nckd_i_invoicetypef7",invoice.getDynamicObject("invoicetypef7").getPkValue());
        newEntry.set("nckd_i_invoicetype",invoice.getString("invoicetypeview"));
        newEntry.set("nckd_i_invoicecode",invoice.getString("invoicecode"));
        newEntry.set("nckd_i_invoiceno",invoice.getString("invoiceno"));
        newEntry.set("nckd_i_invoicedate",invoice.get("issuedate"));
        newEntry.set("nckd_i_currency",invoice.getDynamicObject("currency").getPkValue());
        newEntry.set("nckd_i_pricetaxtotal",invoice.getBigDecimal("pricetaxtotal"));
        newEntry.set("nckd_i_taxrate",invoice.getDynamicObjectCollection("entry").get(0).getBigDecimal("taxrate"));
        newEntry.set("nckd_i_tax",invoice.getBigDecimal("tax"));
        newEntry.set("nckd_i_amount",invoice.getBigDecimal("amount"));
        newEntry.set("nckd_i_asstactname",invoice.getDynamicObject("asstact").getString("name"));
        newEntry.set("nckd_i_buyername",invoice.getDynamicObject("buyer").getString("name"));
        newEntry.set("nckd_i_remark",invoice.getString("remark"));
        newEntry.set("nckd_i_srctype","3");
        newEntry.set("nckd_i_issupplement","0");
        newEntry.set("nckd_i_billno",invoice.getString("billno"));
        newEntry.set("nckd_invid",invoice.getPkValue());
        newEntry.set("nckd_i_serialno",invoice.getString("serialno"));

        BigDecimal pricetaxtotal = invoice.getBigDecimal("pricetaxtotal");//发票金额
        BigDecimal currentActualinstamt = interestbill.getBigDecimal("actualinstamt");//当前付息金额
        DynamicObject[] cfm = BusinessDataServiceHelper.load("cfm_interestbill", "id,actualinstamt,nckd_inventry,nckd_inventry.nckd_i_invoicecode,nckd_inventry.nckd_i_invoiceno,nckd_inventry.nckd_i_usedamt",
                new QFilter[]{new QFilter("nckd_inventry.nckd_i_invoicecode", QCP.equals, invoice.getString("invoicecode"))
                        .and(new QFilter("nckd_inventry.nckd_i_invoiceno", QCP.equals, invoice.getString("invoiceno")))});

        if (cfm == null || cfm.length == 0){
            if (entry.size() - 1 > 0){
                BigDecimal nckdIUsedamt = BigDecimal.ZERO;//发票已用金额
                for (int i = 0; i < entry.size()-1; i++) {
                    nckdIUsedamt = nckdIUsedamt.add(entry.get(i).getBigDecimal("nckd_i_usedamt"));
                }
                if (nckdIUsedamt.compareTo(currentActualinstamt) > -1){
                    newEntry.set("nckd_i_canuseamt",pricetaxtotal);
                    newEntry.set("nckd_i_usedamt",BigDecimal.ZERO);
                    updateInvoice(invoice, pricetaxtotal);
                }else {
                    BigDecimal subtract = currentActualinstamt.subtract(nckdIUsedamt);//当前付息单剩余要冲金额
                    if (pricetaxtotal.compareTo(subtract) > -1){
                        newEntry.set("nckd_i_canuseamt",pricetaxtotal);
                        newEntry.set("nckd_i_usedamt",subtract);
                        updateInvoice(invoice, pricetaxtotal.subtract(subtract));
                    }else {
                        newEntry.set("nckd_i_canuseamt",pricetaxtotal);
                        newEntry.set("nckd_i_usedamt",pricetaxtotal);
                        updateInvoice(invoice, BigDecimal.ZERO);
                    }
                }
                return;
            }else {
                //发票金额比付息金额小，可占用金额为发票金额，本次占用金额为发票金额
                //发票金额比付息金额大，可占用金额为发票金额，本次占用金额为付息金额
                if (pricetaxtotal.compareTo(currentActualinstamt) > -1){
                    newEntry.set("nckd_i_canuseamt",pricetaxtotal);
                    newEntry.set("nckd_i_usedamt",currentActualinstamt);
                    updateInvoice(invoice, pricetaxtotal.subtract(currentActualinstamt));
                }else {
                    newEntry.set("nckd_i_canuseamt",pricetaxtotal);
                    newEntry.set("nckd_i_usedamt",pricetaxtotal);
                    updateInvoice(invoice, BigDecimal.ZERO);
                }
                return;
            }
        }
        if (cfm.length > 0){
            BigDecimal actualinstamt = BigDecimal.ZERO;//发票已用金额
            for (DynamicObject c : cfm) {
                //actualinstamt = actualinstamt.add(c.getBigDecimal("actualinstamt"));
                DynamicObjectCollection inventry = c.getDynamicObjectCollection("nckd_inventry");
                for (DynamicObject e : inventry) {
                    if (invoice.getString("invoicecode").equals(e.getString("nckd_i_invoicecode")) && invoice.getString("invoiceno").equals(e.getString("nckd_i_invoiceno"))) {
                        actualinstamt = actualinstamt.add(e.getBigDecimal("nckd_i_usedamt"));
                    }
                }
            }
            BigDecimal resultPrice = pricetaxtotal.subtract(actualinstamt);
            newEntry.set("nckd_i_canuseamt",resultPrice);
            if (resultPrice.compareTo(currentActualinstamt) > -1){
                newEntry.set("nckd_i_usedamt",currentActualinstamt);
                updateInvoice(invoice, resultPrice.subtract(currentActualinstamt));
            }else {
                newEntry.set("nckd_i_usedamt",resultPrice);
                updateInvoice(invoice, BigDecimal.ZERO);
            }
        }
    }

    /**
     * 发票指定后，反写剩余可用发票金额到收票单
     * @param invoice
     * @param pricetaxtotal
     */
    private static void updateInvoice(DynamicObject invoice, BigDecimal pricetaxtotal) {
        invoice.set("nckd_remainderamount", pricetaxtotal);
        SaveServiceHelper.save(new DynamicObject[]{invoice});
    }

    private Boolean verifyInvoice(DynamicObject invoice,DynamicObject interestbill) {
        String invoicecode = invoice.getString("invoicecode");
        String invoiceno = invoice.getString("invoiceno");
        DynamicObject[] cfm = BusinessDataServiceHelper.load("cfm_interestbill", "id,actualinstamt,nckd_inventry,nckd_inventry.nckd_i_invoicecode,nckd_inventry.nckd_i_invoiceno,nckd_inventry.nckd_i_usedamt",
                new QFilter[]{new QFilter("nckd_inventry.nckd_i_invoicecode", QCP.equals, invoicecode)
                        .and(new QFilter("nckd_inventry.nckd_i_invoiceno", QCP.equals, invoiceno))});
        if (cfm == null || cfm.length == 0){
            return true;
        }
        BigDecimal actualinstamt = BigDecimal.ZERO;//已占用金额
        for (DynamicObject c : cfm) {
            DynamicObjectCollection entry = c.getDynamicObjectCollection("nckd_inventry");
            for (DynamicObject e : entry) {
                if (invoicecode.equals(e.getString("nckd_i_invoicecode")) && invoiceno.equals(e.getString("nckd_i_invoiceno"))) {
                    actualinstamt = actualinstamt.add(e.getBigDecimal("nckd_i_usedamt"));
                }
            }
            //actualinstamt = actualinstamt.add(c.getBigDecimal("actualinstamt"));
        }
        BigDecimal pricetaxtotal = invoice.getBigDecimal("pricetaxtotal");//发票金额
        //BigDecimal currentActualinstamt = interestbill.getBigDecimal("actualinstamt");//当前付息金额
        BigDecimal resultPrice = pricetaxtotal.subtract(actualinstamt);
        if(resultPrice.compareTo(BigDecimal.ZERO) > -1) {
            return true;
        }
        return false;
    }
}
