package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowType;
import kd.bos.form.StyleCss;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.EventObject;

/**
 * 资金-付息处理表单插件
 * 表单标识：cfm_interestbill
 * author：xiaoxiaopeng
 * date：2024-08-19
 */
public class CfmInterestBillFromPlugin extends AbstractBillPlugIn {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("tbmain");
        this.addItemClickListeners("billlisttop_toolbarap");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        if (itemKey.equals("nckd_designate")){
            openListShowParameter("bos_listf7","ap_invoice");
        }
        if (itemKey.equals("nckd_canceldesignate")){
            int index = this.getModel().getEntryCurrentRowIndex("nckd_inventry");
            DynamicObject nckdInventry = this.getModel().getEntryRowEntity("nckd_inventry", index);
            if (nckdInventry == null){
                this.getView().showErrorNotification("当前选中行无指定发票,无需取消指定");
                return;
            }
            this.getModel().deleteEntryRow("nckd_inventry",index);
            this.getView().updateView();
            DynamicObjectCollection entity = this.getModel().getEntryEntity("nckd_inventry");
            if (entity.size() == 0){
                this.getModel().setValue("nckd_receiptinvoice",false);
            }
            DynamicObject dataEntity = this.getModel().getDataEntity();
            SaveServiceHelper.save(new DynamicObject[]{dataEntity});
        }

    }

    /**
     * 选择对应发票,写入发票明细
     */
    private void setInvoice(DynamicObject invoice) {
        DynamicObjectCollection entry = this.getModel().getEntryEntity("nckd_inventry");
        int nckdInventry = this.getModel().insertEntryRow("nckd_inventry", entry.size());
        /**
         * 写入单据体
         */
        this.getModel().setValue("nckd_i_invoicetypef7",invoice.getDynamicObject("invoicetypef7").getPkValue(),nckdInventry);
        this.getModel().setValue("nckd_i_invoicetype",invoice.getString("invoicetypeview"),nckdInventry);
        this.getModel().setValue("nckd_i_invoicecode",invoice.getString("invoicecode"),nckdInventry);
        this.getModel().setValue("nckd_i_invoiceno",invoice.getString("invoiceno"),nckdInventry);
        this.getModel().setValue("nckd_i_invoicedate",invoice.get("issuedate"),nckdInventry);
        this.getModel().setValue("nckd_i_currency",invoice.getDynamicObject("currency").getPkValue(),nckdInventry);
        this.getModel().setValue("nckd_i_pricetaxtotal",invoice.getBigDecimal("pricetaxtotal"),nckdInventry);
        this.getModel().setValue("nckd_i_taxrate",invoice.getDynamicObjectCollection("entry").get(0).getBigDecimal("taxrate"),nckdInventry);
        this.getModel().setValue("nckd_i_tax",invoice.getBigDecimal("tax"),nckdInventry);
        this.getModel().setValue("nckd_i_amount",invoice.getBigDecimal("amount"),nckdInventry);
        this.getModel().setValue("nckd_i_asstactname",invoice.getDynamicObject("asstact") == null ? null : invoice.getDynamicObject("asstact").getString("name"),nckdInventry);
        this.getModel().setValue("nckd_i_buyername",invoice.getDynamicObject("buyer") == null ? null : invoice.getDynamicObject("buyer").getString("name"),nckdInventry);
        this.getModel().setValue("nckd_i_remark",invoice.getString("remark"),nckdInventry);
        this.getModel().setValue("nckd_i_srctype","3",nckdInventry);
        this.getModel().setValue("nckd_i_issupplement","0",nckdInventry);
        this.getModel().setValue("nckd_i_billno",invoice.getString("billno"),nckdInventry);
        this.getModel().setValue("nckd_invid",invoice.getPkValue(),nckdInventry);
        this.getModel().setValue("nckd_i_serialno",invoice.getString("serialno"),nckdInventry);

        BigDecimal pricetaxtotal = invoice.getBigDecimal("pricetaxtotal");//发票金额
        BigDecimal currentActualinstamt = (BigDecimal)this.getModel().getValue("actualinstamt");//当前付息金额
        DynamicObject[] cfm = BusinessDataServiceHelper.load("cfm_interestbill", "id,actualinstamt,nckd_inventry,nckd_inventry.nckd_i_invoicecode,nckd_inventry.nckd_i_invoiceno",
                new QFilter[]{new QFilter("nckd_inventry.nckd_i_invoicecode", QCP.equals, invoice.getString("invoicecode"))
                        .and(new QFilter("nckd_inventry.nckd_i_invoiceno", QCP.equals, invoice.getString("invoiceno")))});
        if (cfm == null || cfm.length == 0){
            this.getModel().setValue("nckd_i_canuseamt",pricetaxtotal,nckdInventry);
            this.getModel().setValue("nckd_i_usedamt",currentActualinstamt,nckdInventry);
            return;
        }
        if (cfm.length > 0){
            BigDecimal actualinstamt = BigDecimal.ZERO;//付息金额
            for (DynamicObject c : cfm) {
                actualinstamt = actualinstamt.add(c.getBigDecimal("actualinstamt"));
            }
            BigDecimal resultPrice = pricetaxtotal.subtract(actualinstamt);
            this.getModel().setValue("nckd_i_canuseamt",resultPrice,nckdInventry);
            if (resultPrice.compareTo(currentActualinstamt) > -1){
                this.getModel().setValue("nckd_i_usedamt",currentActualinstamt,nckdInventry);
            }else {
                this.getModel().setValue("nckd_i_usedamt",resultPrice,nckdInventry);
            }
        }
    }

    /**
     * 发票类型转换
     * @param invoicetypeview
     * @return
     */
    private String getInvoiceType(String invoicetypeview) {
        if (invoicetypeview == null){
            return null;
        }
        String invoicetypeNumber = "";
        switch (invoicetypeview) {
            case "ELE":
                //普通发票(电子)
                invoicetypeNumber = "1";
                break;
            case "SE":
                //专用发票(电子)
                invoicetypeNumber = "2";
                break;
            case "GE":
                //普通发票(纸质)
                invoicetypeNumber = "3";
                break;
            case "SP":
                //专用发票(纸质)
                invoicetypeNumber = "4";
                break;
            case "PAPER":
                //普通纸质卷票
                invoicetypeNumber = "5";
                break;
            case "MACH":
                //通用机打发票
                invoicetypeNumber = "7";
                break;
            case "TAXI":
                //的士票发票
                invoicetypeNumber = "8";
                break;
            case "TRAIN":
                //火车票发票
                invoicetypeNumber = "9";
                break;
            case "PLANE":
                //飞机票发票
                invoicetypeNumber = "10";
                break;
            case "OTHERCLOUD":
                //其他发票
                invoicetypeNumber = "11";
                break;
            case "MOTOR":
                //机动车销售发票
                invoicetypeNumber = "12";
                break;
            case "USERCAR":
                //二手车发票
                invoicetypeNumber = "13";
                break;
            case "QUOTA":
                //定额发票
                invoicetypeNumber = "14";
                break;
            case "BRIDGE":
                //过路过桥费发票
                invoicetypeNumber = "17";
                break;
            case "PAID":
                //完税证明发票
                invoicetypeNumber = "19";
                break;
            case "STEAMER":
                //轮船票发票
                invoicetypeNumber = "20";
                break;
            default:
                invoicetypeNumber = "11";

        }
        return invoicetypeNumber;
    }

    /**
     * 打开列表before7页面
     */
    private void openListShowParameter(String listf7, String bill) {
        IDataModel model = this.getModel();
        DynamicObject org = (DynamicObject)model.getValue("org");
        if (org == null){
            this.getView().showErrorNotification("请先填写借款人");
            return;
        }
        Object actualinstamt = model.getValue("actualinstamt");
        if (actualinstamt == null || actualinstamt.equals(BigDecimal.ZERO)){
            this.getView().showErrorNotification("请先填写付息金额");
            return;
        }
        DynamicObject dataEntity = model.getDataEntity();
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
        QFilter qFilter = setInvoiceFilter(dataEntity);
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
        qFilter.and(new QFilter("currency.id",QCP.equals,dataEntity.getDynamicObject("currency").getPkValue()));
        //发票金额大于当前结息金额
        qFilter.and(new QFilter("pricetaxtotal",QCP.large_equals,dataEntity.getBigDecimal("actualinstamt")));
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
        String actionId = e.getActionId();
        if (StringUtils.equals(actionId,"ap_invoice") && actionId != null) {
            ListSelectedRowCollection returnData = (ListSelectedRowCollection) e.getReturnData();
            if (returnData == null){
                return;
            }
            int count = 1;
            for (ListSelectedRow date : returnData) {
                DynamicObject invoice = BusinessDataServiceHelper.loadSingle(date.getPrimaryKeyValue(), "ap_invoice");
                Boolean result = verifyInvoice(invoice);
                if (!result){
                    this.getView().showErrorNotification("选中第"+count+"条发票，关联的银行付息单金额已超出发票金额");
                    return;
                }
                setInvoice(invoice);
                count++;
            }
            DynamicObjectCollection entity = this.getModel().getEntryEntity("nckd_inventry");
            if (entity.size() > 0){
                this.getModel().setValue("nckd_receiptinvoice",true);
            }else {
                this.getModel().setValue("nckd_receiptinvoice",false);
            }
            DynamicObject dataEntity = this.getModel().getDataEntity();
            try {
                SaveServiceHelper.save(new DynamicObject[]{dataEntity});
                this.getView().updateView();
                this.getView().showMessage("发票指定成功");
            }catch (Exception ex){
                this.getView().showMessage(ex.getMessage());
            }

        }
    }

    /**
     * 校验发票是否已关联付息单，判断该发票已关联的付息单和待关联的付息单总金额是否大于发票上的总金额
     * @param invoice
     * @return
     */
    private Boolean verifyInvoice(DynamicObject invoice) {
        String invoicecode = invoice.getString("invoicecode");
        String invoiceno = invoice.getString("invoiceno");
        DynamicObject[] cfm = BusinessDataServiceHelper.load("cfm_interestbill", "id,actualinstamt,nckd_inventry,nckd_inventry.nckd_i_invoicecode,nckd_inventry.nckd_i_invoiceno",
                new QFilter[]{new QFilter("nckd_inventry.nckd_i_invoicecode", QCP.equals, invoicecode)
                        .and(new QFilter("nckd_inventry.nckd_i_invoiceno", QCP.equals, invoiceno))});
        if (cfm == null || cfm.length == 0){
            return true;
        }
        BigDecimal actualinstamt = BigDecimal.ZERO;//付息金额
        for (DynamicObject c : cfm) {
            actualinstamt = actualinstamt.add(c.getBigDecimal("actualinstamt"));
        }
        BigDecimal pricetaxtotal = invoice.getBigDecimal("pricetaxtotal");//发票金额
        BigDecimal currentActualinstamt = (BigDecimal)this.getModel().getValue("actualinstamt");//当前付息金额
        BigDecimal resultPrice = pricetaxtotal.subtract(actualinstamt);
        if(resultPrice.subtract(currentActualinstamt).compareTo(BigDecimal.ZERO) > -1) {
            return true;
        }
        return false;
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        DynamicObjectCollection entity = this.getModel().getEntryEntity("nckd_inventry");
        if (entity.size() > 0){
            this.getModel().setValue("nckd_receiptinvoice",true);
        }else {
            this.getModel().setValue("nckd_receiptinvoice",false);
        }
    }
}
