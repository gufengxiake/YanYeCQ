package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.Control;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.mvc.bill.BillModel;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.arapcommon.helper.LspWapper;
import kd.fi.er.business.daily.reimburse.topublic.AccountInfo;
import kd.fi.er.business.daily.reimburse.topublic.PublicReimbursePayerAcctUtils;
import kd.fi.er.business.expand.ErExpandServiceFacade;
import kd.fi.er.business.servicehelper.ListConstructorHelper;
import kd.fi.er.common.PayerTypeEnum;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


/**
 * Module           :财务云-费用核算-对公费用单据-对公报销单
 * Description      :对公报销单-收款信息分录
 *
 *
 * @author guozhiwei
 * @date  2024/8/12 17:05
 * 标识 er_publicreimbursebill
 *
 */

public class RrimbursebillCollectionPlugin extends AbstractBillPlugIn {

    private final List<String> NAME_LIST = Arrays.asList(new String[]{"paymode", "payertype", "supplier","payeraccount"});

    private final String BANK_ACCEP = "JSFS06"; // 银行承兑汇票

    private final String TRADE_ACCEP = "JSFS07";//  商业承兑汇票


    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"nckd_payeraccount"});
//        this.addClickListeners(new String[]{"nckd_e_assacct"});
//        this.addClickListeners(new String[]{"e_corebillno"});
//        this.addClickListeners(new String[]{"sameinfo_view", "sameinfo_ignore", "refund_view", "refund_ignore"});
//        this.addItemClickListeners(new String[]{"tbmain"});
//        this.addItemClickListeners(new String[]{"advcontoolbarap2"});

    }


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        // 支付方式的类别为 银行承兑汇票 或 商业承兑汇票，收款人类型为供应商，并且选了收款人，然后自动带出承兑银行到开户银行字段

        String name = e.getProperty().getName();
        if(NAME_LIST.contains(name)){

            ChangeData changeData = e.getChangeSet()[0];
            String payeraccountStr = "";
            Object newValuetest = changeData.getNewValue();
            DynamicObject newValue = new DynamicObject();

            if (newValuetest instanceof DynamicObject) {
                newValue = (DynamicObject) changeData.getNewValue();
            }else if(newValuetest != null){
                payeraccountStr = ObjectUtils.isNotEmpty(newValuetest)?newValuetest.toString():"";
            }

            //获取改变的行号
            int rowIndex = changeData.getRowIndex();

            if(name.equals("payeraccount") && StringUtils.isNotEmpty(payeraccountStr)){
                this.getModel().setValue("payeraccount01", getHideTailPayerAccount(payeraccountStr), rowIndex);
                this.getModel().setValue("payeraccount02", getHideMidPayerAccount(payeraccountStr), rowIndex);
            }

            DynamicObjectCollection collection = this.getModel().getEntryEntity("accountentry");

            DynamicObject dynamicObject = collection.get(rowIndex);
            // 定义出支付方式，收款人类型，收款人
//            String paymode = name.equals("paymode")? newValue.getString("number"): ObjectUtils.isNotEmpty(dynamicObject.getDynamicObject("paymode")) ? dynamicObject.getDynamicObject("paymode").getString("number"): null;

            DynamicObject paymodeObj = name.equals("paymode")? newValue: dynamicObject.getDynamicObject("paymode");
            String paymode = null;
            if(ObjectUtils.isNotEmpty(paymodeObj)){
                paymode = ObjectUtils.isNotEmpty(paymodeObj.getDataStorage()) ? paymodeObj.getString("number"): null;
            }
            String payertype = name.equals("payertype")?payeraccountStr: dynamicObject.getString("payertype");

            if(name.equals("payertype")){
                this.getModel().setValue("nckd_payeraccount",null,rowIndex);
            }
            DynamicObject supplier = name.equals("supplier")?newValue: dynamicObject.getDynamicObject("supplier");

            if(StringUtils.isNotEmpty(payertype) && (payertype.equals("bd_supplier")) ){

                String eAssacct = name.equals("payeraccount") ? payeraccountStr:dynamicObject.getString("payeraccount");

                if(ObjectUtils.isNotEmpty(supplier)){
                    Object masterid = supplier.get("masterid");
                    if(name.equals("supplier")){
                        // 内部供应商
                        Map<String, Object> innerSupplier = isInnerSupplier(masterid,paymode);
                        if(innerSupplier != null){
                            String number = (String) innerSupplier.get("number");
                            Long bankid = (Long) innerSupplier.get("bankid");
                            String baknName =(String) innerSupplier.get("name");
                            this.getModel().setValue("payeraccountname",baknName,rowIndex);
                            this.getModel().setValue("payerbank",bankid,rowIndex);
                            setPayerAccount(number,rowIndex);
                            this.getView().updateView();
                            return;
                        }
                        //外部供应商设置新账号信息
                        AccountInfo bdSupplier = PublicReimbursePayerAcctUtils.getPayerDefaultInfo("bd_supplier", (Long) masterid);
                        String accnumber = null;
                        if(ObjectUtils.isNotEmpty(bdSupplier)){
                            accnumber = bdSupplier.getAccount();
                        }
                        this.getModel().setValue("nckd_payeraccount",accnumber,rowIndex);
                    }

                    if(StringUtils.isNotEmpty(paymode)  && (paymode.equals(BANK_ACCEP) || paymode.equals(TRADE_ACCEP))){

                        DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");
                        DynamicObject o = eAsstactObject.getDynamicObject("internal_company");
                        if(ObjectUtils.isNotEmpty(o)){
                            QFilter qFilter3 = new QFilter("account.bankaccountnumber", "=", eAssacct);
                            // 查询供应商的银行账户信息
                            // 根据银行账户去查询对应票据开户行信息
                            DynamicObject billbank = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","account,billbank",new QFilter[]{qFilter3});
                            if(ObjectUtils.isNotEmpty(billbank)){
                                // 合作金融机构信息
                                DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(billbank.getLong("billbank.id"),"bd_finorginfo");
                                dynamicObject.set("payerbank", bdFinorginfo.getDynamicObject("bebank"));
                                this.getView().updateView();
                                return;
                            }
                        }
                        // 把供应商承兑银行信息自动带出到往来银行字段
                        DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");

                        if (ObjectUtils.isNotEmpty(entryBank)) {
                            for (DynamicObject object : entryBank) {
                                if (eAssacct.equals(object.getString("bankaccount"))) {
                                    dynamicObject.set("payerbank", object.getDynamicObject("nckd_acceptingbank"));
                                    // 刷新页面
                                    break; // 找到符合条件的记录后退出循环
                                }
                            }

                        }
                        this.getView().updateView();
                    }else{

                        //  处理内部供应商情况
                        DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");
                        DynamicObject o = eAsstactObject.getDynamicObject("internal_company");
                        if(ObjectUtils.isNotEmpty(o)){
                            QFilter qFilter = new QFilter("openorg.masterid", "=", o.getPkValue());
                            QFilter qFilter2 = new QFilter("bankaccountnumber", "=", eAssacct);
                            // 查询供应商的银行账户信息
                            DynamicObject amAccountbank = BusinessDataServiceHelper.loadSingle("am_accountbank", "bank,bankaccountnumber,currency", new QFilter[]{qFilter,qFilter2});
                            if(ObjectUtils.isNotEmpty(amAccountbank)){
                                long aLong = amAccountbank.getLong("bank.id");
                                // 行名行号
                                DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(aLong, "bd_finorginfo");
                                dynamicObject.set("payerbank", bdFinorginfo.getDynamicObject("bebank"));
                                this.getView().updateView();
                                return;
                            }
                        }

                        // 不是承兑银行带出正常银行到往来银行字段
                        DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");
                        if (ObjectUtils.isNotEmpty(entryBank)) {

                            for (DynamicObject object : entryBank) {
                                if (eAssacct.equals(object.getString("bankaccount"))) {
                                    dynamicObject.set("payerbank", object.getDynamicObject("bank"));
                                    break; // 找到符合条件的记录后退出循环
                                }
                            }
                            // 刷新页面
                            this.getView().updateView();
                        }



                    }
                }
            }
        }
    }

    // 获取内部供应商信息
    public Map<String,Object> isInnerSupplier(Object supplierid,String paymode) {
        Map<String, Object> map = new HashMap<>();

        // 查询是否存在内部公司
        DynamicObject o = (DynamicObject) BusinessDataServiceHelper.loadSingle(supplierid, "bd_supplier").get("internal_company");
        if (ObjectUtils.isNotEmpty(o)) {
            boolean isAccept = false;
            if(ObjectUtils.isNotEmpty(paymode) && (paymode.equals(BANK_ACCEP) || paymode.equals(TRADE_ACCEP)) ){
                isAccept = true;
            }
            QFilter qFilter = new QFilter("openorg.masterid", "=", o.getPkValue());
            // 查询供应商的银行账户信息
            DynamicObject amAccountbank = BusinessDataServiceHelper.loadSingle("am_accountbank", "acctname,bank,bankaccountnumber,currency", new QFilter[]{qFilter});
            if (ObjectUtils.isNotEmpty(amAccountbank)) {
                // 账户名称
                map.put("name",amAccountbank.getString("acctname"));
                // 合作金融机构
                map.put("number", amAccountbank.getString("bankaccountnumber"));
                amAccountbank.getLong("bank.id");
                //  查询银行账户是否有对应票据开户行信息，如果有，则设置到e_bebank
                QFilter qFilter2 = new QFilter("account.masterid", "=", amAccountbank.getPkValue());
                // 合作金融机构
                Object cooperationId = null;
                // 票据开户行信息
                DynamicObject billbank = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","billbank.id",new QFilter[]{qFilter2});
                if (ObjectUtils.isNotEmpty(billbank) && isAccept) {
                    cooperationId = billbank.getLong("billbank.id");
                }else{
                    cooperationId = amAccountbank.get("bank.id");
                }
                // 合作金融机构信息
                DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(cooperationId, "bd_finorginfo");
                map.put("bankid",bdFinorginfo.getLong("bebank.id"));
                return map;
            }
        }
        return null;
    }


    // 点击新银行账号监听
    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control)evt.getSource();
        switch (c.getKey().toLowerCase()) {
            case "nckd_payeraccount":
                this.selectAccountByPayer();
                break;
        }

    }


    private void selectAccountByPayer() {
        String payerType = this.getPayerType();
        if (StringUtils.isBlank(payerType)) {
            this.getView().showTipNotification(ResManager.loadKDString("请选择收款人类型。", "AbstractPublicRBSReceivePlugin_0", "fi-er-formplugin", new Object[0]));
        } else {
            PayerTypeEnum payerTypeEnum = PayerTypeEnum.getValue(payerType);
            if (payerTypeEnum != null) {
                DynamicObject payer = null;
                switch (payerTypeEnum) {
                    case SUPPLIER:
                        payer = (DynamicObject)this.getModel().getValue("supplier");
                        break;
                    case CUSTOMER:
                        payer = (DynamicObject)this.getModel().getValue("customer");
                        break;
                    case CASORG:
                        payer = (DynamicObject)this.getModel().getValue("casorg");
                        break;
                    case PAYER:
                    case OTHER:
                        return;
                }

                if (payer == null && payerTypeEnum != PayerTypeEnum.PAYER) {
                    this.getView().showErrorNotification(ResManager.loadKDString("请先选择收款人。", "AbstractPublicRBSReceivePlugin_1", "fi-er-formplugin", new Object[0]));
                    return;
                }

                if (payer != null) {
                    this.showBankInfo(payerType, (Long)payer.getPkValue());
                }
            }

        }
    }

    protected String getPayerType() {
        EntryGrid entry = (EntryGrid)this.getControl("accountentry");
        int rowIndex = entry.getEntryState().getFocusRow();
        return (String)this.getModel().getValue("payertype", rowIndex);
    }

    //  修改内部供应商展示的银行账号信息
    private void showBankInfo(String payerEntityType, Long payerId) {
        PayerTypeEnum payerTypeEnum = PayerTypeEnum.getValue(payerEntityType);
        ListShowParameter lsp = null;
        CloseCallBack closeCallBack = null;
        switch (payerTypeEnum) {
            case SUPPLIER:
                // 查询是否存在内部公司
                DynamicObject o = (DynamicObject) BusinessDataServiceHelper.loadSingle(payerId, "bd_supplier").get("internal_company");
                if (ObjectUtils.isNotEmpty(o)) {
                    // 获取票据账号开户行维护信息
                    QFilter qFilter = new QFilter("openorg.masterid", "=", o.getPkValue());
                    // 查询供应商的银行账户信息

                    DynamicObject[] load = BusinessDataServiceHelper.load("am_accountbank", "bank,bankaccountnumber,currency",new QFilter[]{qFilter},null );

                    if (ObjectUtils.isNotEmpty(load)) {
                        // 获取结算类型 判断获取哪个信息
                        // 存在票据账号开户行维护信息
                        lsp= getSupplierBankInfoShowParameter(o);
                        closeCallBack = new CloseCallBack(this, "nckd_payeraccount");
                    }
                }else{
                    lsp = ListConstructorHelper.getSupplierBankInfoShowParameter(payerId);
                }

                break;
            case CUSTOMER:
                lsp = ListConstructorHelper.getCustomerBankInfoShowParameter(payerId);
                break;
            case CASORG:
                lsp = ListConstructorHelper.getCasOrgBankInfoShowParameter(payerId);
        }

        Object invokeExtService = ErExpandServiceFacade.get().invokeExtService("ext.service.er.receiveBankInfoF7", "receiveBankInfoF7", new Object[]{payerEntityType, payerId, this.getModel()}, new Class[]{String.class, Long.class, BillModel.class});
        if (invokeExtService != null && !"-1".equals(invokeExtService)) {
            lsp = (ListShowParameter)invokeExtService;
        }

        if (lsp != null) {

            if(ObjectUtils.isEmpty(closeCallBack)){
                closeCallBack = new CloseCallBack(this, "payeeaccountbank");
            }
            lsp.setCloseCallBack(closeCallBack);
            this.getView().showForm(lsp);
        }

    }

    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        String actionId = closedCallBackEvent.getActionId();
        if ("payeeaccountbank".equals(actionId)) {
            Object returnData = closedCallBackEvent.getReturnData();
            if (returnData != null) {
                ListSelectedRowCollection rowInfo = (ListSelectedRowCollection)returnData;
                EntryGrid entry = (EntryGrid)this.getControl("accountentry");
                int rowIndex;
                if (entry != null) {
                    rowIndex = entry.getEntryState().getFocusRow();
                } else {
                    rowIndex = -1;
                }

                String payerType = this.getPayerType();
                Object entryKey;
                if (PayerTypeEnum.CASORG.getType().equals(payerType)) {
                    entryKey = rowInfo.getPrimaryKeyValues()[0];
                } else {
                    entryKey = rowInfo.getEntryPrimaryKeyValues()[0];
                }

                AccountInfo accountInfo = PublicReimbursePayerAcctUtils.getBankInfo(payerType, (Long)entryKey);
                this.getModel().setValue("nckd_payeraccount",accountInfo.getAccount());
                PublicReimbursePayerAcctUtils.fillBankInfo(this.getModel(), accountInfo, rowIndex);
                ErExpandServiceFacade.get().invokeExtService("ext.service.er.fillbankInfoPC", "fillbankInfoPC", new Object[]{payerType, (Long)entryKey, this.getModel(), rowIndex}, new Class[]{String.class, Long.class, IDataModel.class, Integer.class});
            }
        }else if("nckd_payeraccount".equals(actionId)){
            int curentrow = this.getModel().getEntryCurrentRowIndex("entry");
            Object returnData = closedCallBackEvent.getReturnData();
            // 结算方式
            boolean isAccept = false;
            DynamicObject paymode = (DynamicObject) this.getModel().getValue("paymode", curentrow);
            if(ObjectUtils.isNotEmpty(paymode) && (paymode.getString("number").equals(BANK_ACCEP) || paymode.getString("number").equals(TRADE_ACCEP))){
                // 结算方式为承兑汇票
                isAccept = true;
            }
            if (returnData != null ){
                ListSelectedRow listSelectedRow = ((ListSelectedRowCollection) returnData).get(0);
                // 银行账户的key
                Object primaryKeyValue = listSelectedRow.getPrimaryKeyValue();
                // 银行账户号
                String number = listSelectedRow.getNumber();
                // 设置账户银行信息
                setPayerAccount(number, curentrow);
                // 银行账户
//                this.getModel().setValue("nckd_payeraccount", number, curentrow);
//                this.getModel().setValue("payeraccount", number, curentrow);
//                this.getModel().setValue("payeraccount01", getHideTailPayerAccount(number), curentrow);
//                this.getModel().setValue("payeraccount02", getHideMidPayerAccount(number), curentrow);

                QFilter qFilter = new QFilter("account.masterid", "=", primaryKeyValue);

                // 银行账号
                DynamicObject amAccountbank = BusinessDataServiceHelper.loadSingle(primaryKeyValue, "am_accountbank");
                Object cooperationId = null;
                String amAccountbankName = amAccountbank.getString("acctname");
                // 账户名称
                this.getModel().setValue("payeraccountname", amAccountbankName, curentrow);

                // 合作金融机构
                DynamicObject billbank = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","billbank.id",new QFilter[]{qFilter});

                if(ObjectUtils.isNotEmpty(billbank) && isAccept){
                    cooperationId = billbank.getLong("bebank.id");
                }else{
                    cooperationId = amAccountbank.getLong("bank.id");
                }
                // 查询合作金融机构对应的行名行号信息
                DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(cooperationId, "bd_finorginfo");
                this.getModel().setValue("payerbank", bdFinorginfo.getLong("bebank.id"), curentrow);
                this.getView().updateView();

            }

        }

    }
    // 设置收款信息
    public void setPayerAccount(String number, int curentrow) {
        this.getModel().setValue("nckd_payeraccount", number, curentrow);
        this.getModel().setValue("payeraccount", number, curentrow);
        this.getModel().setValue("payeraccount01", getHideTailPayerAccount(number), curentrow);
        this.getModel().setValue("payeraccount02", getHideMidPayerAccount(number), curentrow);
    }

    public static String getHideMidPayerAccount(String account) {
        if (account == null) {
            return "";
        } else {
            StringBuffer accountShow = new StringBuffer();
            if (account.length() > 10) {
                accountShow.append(account.substring(0, 6));
                accountShow.append("***");
                accountShow.append(account.substring(account.length() - 4, account.length()));
            } else {
                accountShow.append(account);
            }

            return String.valueOf(accountShow);
        }
    }

    public static String getHideTailPayerAccount(String account) {
        if (account == null) {
            return "";
        } else {
            StringBuffer accountShow = new StringBuffer();
            if (account.length() > 4) {
                accountShow.append('(');
                accountShow.append('*');
                accountShow.append(account.substring(account.length() - 4, account.length()));
                accountShow.append(')');
            } else {
                accountShow.append(account);
            }

            return String.valueOf(accountShow);
        }
    }


    // 构建内部供应商的选择列表
    public static ListShowParameter getSupplierBankInfoShowParameter(DynamicObject pk) {
        List<String> showFields = new ArrayList();
        showFields.add("bank.name");
        showFields.add("bankaccountnumber");
        showFields.add("acctname");
        showFields.add("currency.name");
        ListShowParameter lsp = createDynamicListShowParameter("am_accountbank", null, showFields);
        ListFilterParameter lfp = new ListFilterParameter();
        lfp.setFilter(new QFilter("openorg.id", "=", pk.getPkValue()));
        lsp.setListFilterParameter(lfp);
        lsp.setCaption(ResManager.loadKDString("内部供应商-银行信息", "DynamicListHelper_0", "fi-arapcommon", new Object[0]));
        return lsp;
    }

    public static ListShowParameter createDynamicListShowParameter(String entity, String entry, List<String> showFields) {
        ListShowParameter lsp = ShowFormHelper.createShowListForm(entity, false);
        lsp.setCustomParam("entity", entity);
//        lsp.setCustomParam("entry", entry);
        lsp.setCustomParam("isEntryMain", Boolean.TRUE);
        lsp.setCustomParam("showFields", showFields);
        LspWapper lspWapper = new LspWapper(lsp);
        lspWapper.clearPlugins();
        lspWapper.registerScript("kingdee.fi.ap.mainpage.arapdynamiclistscriptplugin");
        lspWapper.setMergeRow(false);
        lsp.setAppId("ap");
        return lsp;
    }



}
