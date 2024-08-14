package nckd.yanye.tmc.plugin.operate;


import cn.hutool.core.util.ObjectUtil;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.dataentity.metadata.clr.DataEntityPropertyCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.extplugin.PluginProxy;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.IFormView;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.TextEdit;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.cas.business.opservice.helper.PaymentPayeeInfo;
import kd.fi.cas.consts.BillTypeConstants;
import kd.fi.cas.enums.AsstActTypeAddressEnum;
import kd.fi.cas.enums.AsstActTypeEnum;
import kd.fi.cas.enums.PayBusinessTypeEnum;
import kd.fi.cas.formplugin.common.DynamicFormPlugin;
import kd.fi.cas.helper.*;
import kd.fi.cas.info.SCAccountInfo;
import kd.fi.cas.util.EmptyUtil;
import kd.sdk.fi.cas.extpoint.paybill.IPayeeBankInfoFilter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :财务云-出纳-付款处理-选择收款方
 * Description      :付款处理-选择收款方
 *
 * @author : guozhiwei
 * @date : 2024/8/7
 *
 */
public class PayeeDisposeInfoEdit extends DynamicFormPlugin {
    public static final String ER_PAYEER = "er_payeer";
    private boolean isSelectAcctBankF7 = false;
    private boolean isSelectBankF7 = false;
    private boolean isSelectErpayeef7F7 = false;
    private static String[] temp = new String[]{"normal", "freeze", "closing", "changing"};

    private final List<String> ACCEPT_TYPE = Arrays.asList(new String[]{"JSFS06", "JSFS07"});


    private static final Log LOGGER = LogFactory.getLog(PayeeDisposeInfoEdit.class);

    public PayeeDisposeInfoEdit() {
    }

    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"btnok"});
        this.initF7();
        TextEdit payeebanknum = (TextEdit)this.getView().getControl("payeebanknum");
        payeebanknum.addButtonClickListener(this);
        TextEdit payeebankname = (TextEdit)this.getView().getControl("payeebankname");
        payeebankname.addButtonClickListener(this);
    }

    private void initF7() {
        this.fillOrgF7();
        this.fillPayeeAcctBank();
        this.fillErPayeeF7();
        this.fillPayeeAcctCash();
    }

    private void fillOrgF7() {
        BasedataEdit orgF7;
        if (this.isCompany()) {
            orgF7 = (BasedataEdit)this.getControl("payee");
            orgF7.addBeforeF7SelectListener((beforeF7SelectEvent) -> {
                ListShowParameter showParameter = (ListShowParameter)beforeF7SelectEvent.getFormShowParameter();
                QFilter qFilter = new QFilter("enable", "=", "1");
                qFilter.and(new QFilter("fisbankroll", "=", "1"));
                showParameter.getListFilterParameter().setFilter(qFilter);
            });
        }

        if (this.isSupplier()) {
            orgF7 = (BasedataEdit)this.getControl("payee");
            orgF7.addBeforeF7SelectListener((beforeF7SelectEvent) -> {
                ListShowParameter showParameter = (ListShowParameter)beforeF7SelectEvent.getFormShowParameter();
                showParameter.getListFilterParameter().getQFilters().add(new QFilter("payhold", "=", "0"));
            });
        }

    }

    private void fillPayeeAcctBank() {
        BasedataEdit payeeAcctBankF7 = (BasedataEdit)this.getControl("payeeacctbankf7");
        payeeAcctBankF7.addBeforeF7SelectListener((beforeF7SelectEvent) -> {
            FormShowParameter parameter = beforeF7SelectEvent.getFormShowParameter();
            DynamicObject payee = this.getDynamicObject("payee");
            if (this.isSupplier() || this.isCustomer()) {
                payee = BaseDataHelper.getInternalOrg(this.getDynamicObject("org"), payee);
            }

            QFilter[] accountFilter = null;
            QFilter virtualQFilter;
            if (payee != null) {
                if (!this.isCompany() && !this.isCustomer() && !this.isSupplier()) {
                    accountFilter = AccountBankHelper.getUsableAccountFilter((Long)payee.getPkValue());
                } else {
                    accountFilter = new QFilter[2];
                    int i = 0;
                    long orgID = payee.getPkValue() == null ? 0L : Long.parseLong(payee.getPkValue().toString());
                    virtualQFilter = AccountBankHelper.getAccountBankFilterByOrg(orgID);
                    accountFilter[i++] = virtualQFilter;
                    parameter.setCustomParam("org", orgID);
                    QFilter qFilter = new QFilter("acctstatus", "in", temp);
                    accountFilter[i++] = qFilter;
                    if (parameter instanceof ListShowParameter) {
                        ((ListShowParameter)parameter).setUseOrgId(orgID);
                    }
                }
            } else {
                if (!this.isOther()) {
                    this.getView().showTipNotification(ResManager.loadKDString("请先选择收款人。", "PayeeInfoEdit_0", "fi-cas-formplugin", new Object[0]));
                    beforeF7SelectEvent.setCancel(true);
                    return;
                }

                accountFilter = AccountBankHelper.getUsableFilter().toArray();
            }

            List<QFilter> filters = new ArrayList(3);
            Map<String, Object> paramMap = parameter.getCustomParams();
            if (paramMap.containsKey("virtual")) {
                boolean virtual = (Boolean)paramMap.get("virtual");
                if (virtual) {
                    virtualQFilter = new QFilter("isvirtual", "=", "1");
                    filters.add(virtualQFilter);
                }
            }

            filters.addAll(Arrays.asList(accountFilter));
            IDataModel parentModel = this.getView().getParentView().getModel();
            DynamicObject billtype = (DynamicObject)parentModel.getValue("billtype");
            if (billtype != null && (BillTypeConstants.PAYBILL_SYN.equals(billtype.getPkValue()) || BillTypeConstants.PAYBILL_CASH.equals(billtype.getPkValue()) || BillTypeConstants.PAYBILL_DCEP.equals(billtype.getPkValue()))) {
                DynamicObject currency = (DynamicObject)parentModel.getValue("currency");
                if (currency != null) {
                    filters.add(new QFilter("currency.fbasedataid.id", "=", currency.getLong("id")));
                }
            }

            this.setDcepFilter(parentModel, filters);
            (new LspWapper(beforeF7SelectEvent)).setFilters((QFilter[])filters.toArray(new QFilter[0]));
        });
    }

    private void setDcepFilter(IDataModel parentModel, List<QFilter> filters) {
        String filterIsElec = "0";
        if (parentModel.getDataEntity().containsProperty("businesstype")) {
            DynamicObject settleType = (DynamicObject)parentModel.getValue("settletype");
            boolean isDcep = BaseDataHelper.isSettleTypeDcep(settleType);
            if (isDcep) {
                String businessType = (String)parentModel.getValue("businesstype");
                DynamicObject payAccBank = (DynamicObject)parentModel.getValue("payeracctbank");
                if (PayBusinessTypeEnum.WALLETESSAY.getValue().equals(businessType)) {
                    if (EmptyUtil.isNoEmpty(payAccBank)) {
                        DynamicObjectCollection relatedsettleacct = payAccBank.getDynamicObjectCollection("relatedsettleacct");
                        List<Object> accId = (List)relatedsettleacct.stream().map((o) -> {
                            return o.get("fbasedataid_id");
                        }).collect(Collectors.toList());
                        filters.add(new QFilter("id", "in", accId));
                    }
                } else if (PayBusinessTypeEnum.WALLETUP.getValue().equals(businessType)) {
                    if (EmptyUtil.isNoEmpty(payAccBank)) {
                        filters.add(new QFilter("relatedsettleacct.fbasedataid.id", "=", payAccBank.getPkValue()));
                    }

                    filterIsElec = "1";
                }

                if (PayBusinessTypeEnum.WALLETTRANSFER.getValue().equals(businessType)) {
                    String payeeType = (String)parentModel.getValue("payeetype");
                    if (!AsstActTypeEnum.SUPPLIER.getValue().equals(payeeType) && !AsstActTypeEnum.CUSTOMER.getValue().equals(payeeType)) {
                        if (AsstActTypeEnum.COMPANY.getValue().equals(payeeType)) {
                            filterIsElec = "1";
                        }
                    } else {
                        DynamicObject payeeObj = (DynamicObject)this.getModel().getValue("payee");
                        if (EmptyUtil.isNoEmpty(payeeObj)) {
                            DynamicObject internalOrg = BaseDataHelper.getInternalOrg((DynamicObject)parentModel.getValue("org"), payeeObj);
                            if (EmptyUtil.isNoEmpty(internalOrg)) {
                                filterIsElec = "1";
                            }
                        }
                    }
                }
            }
        }

        filters.add(new QFilter("iselecpayment", "=", filterIsElec));
    }

    private void fillPayeeAcctCash() {
        BasedataEdit payeeacctcashf7 = (BasedataEdit)this.getControl("payeeacctcashf7");
        payeeacctcashf7.addBeforeF7SelectListener((beforeF7SelectEvent) -> {
            DynamicObject payee = this.getDynamicObject("payee");
            if (this.isSupplier() || this.isCustomer()) {
                payee = BaseDataHelper.getInternalOrg(this.getDynamicObject("org"), payee);
            }

            QFilter[] accountFilter = null;
            if (payee != null) {
                accountFilter = AccountCashHelper.getUsableAccountFilter(Long.valueOf(payee.getPkValue().toString()));
                ArrayList filters = new ArrayList(3);
                filters.addAll(Arrays.asList(accountFilter));
                IDataModel parentModel = this.getView().getParentView().getModel();
                DynamicObject billtype = (DynamicObject)parentModel.getValue("billtype");
                if (billtype != null && BillTypeConstants.PAYBILL_CASH.equals(billtype.getPkValue())) {
                    DynamicObject currency = (DynamicObject)parentModel.getValue("currency");
                    if (currency != null) {
                        filters.add(new QFilter("currency.fbasedataid.id", "=", currency.getLong("id")));
                    }
                }

                (new LspWapper(beforeF7SelectEvent)).setFilters((QFilter[])filters.toArray(new QFilter[0]));
            } else {
                this.getView().showTipNotification(ResManager.loadKDString("请先选择收款人。", "PayeeInfoEdit_0", "fi-cas-formplugin", new Object[0]));
                beforeF7SelectEvent.setCancel(true);
            }
        });
    }

    private void fillErPayeeF7() {
        BasedataEdit erpayeeF7 = (BasedataEdit)this.getControl("erpayeef7");
        erpayeeF7.addBeforeF7SelectListener((beforeF7SelectEvent) -> {
            ListShowParameter showParameter = (ListShowParameter)beforeF7SelectEvent.getFormShowParameter();
            DynamicObject user = this.getDynamicObject("payee");
            QFilter uFilter = new QFilter("status", "=", 'C');
            if (user != null) {
                QFilter payerFilter = new QFilter("payer", "=", user.getPkValue());
                QFilter isemployeeFilter = new QFilter("isemployee", "=", '1');
                DynamicObjectCollection query = QueryServiceHelper.query("er_payeer", "id", new QFilter[]{uFilter, payerFilter, isemployeeFilter});
                if (query.isEmpty()) {
                    uFilter = uFilter.and(new QFilter("name", "=", user.getLocaleString("name").getLocaleValue()));
                    uFilter.and(new QFilter("isemployee", "=", '0'));
                    uFilter.and(new QFilter("enable", "=", "1"));
                } else {
                    uFilter = new QFilter("id", "in", query.stream().map((dy) -> {
                        return dy.get("id");
                    }).toArray());
                }
            }

            showParameter.getListFilterParameter().setFilter(uFilter);
        });
    }

    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        FormShowParameter parameter = this.getView().getFormShowParameter();
        Map<String, Object> paramMap = parameter.getCustomParams();
        IDataModel model = this.getModel();
        this.setValue("org", paramMap.get("org"));
        String asstActType = (String)paramMap.get("asstActType");
        this.setValue("asstacttype", asstActType);
        DynamicObject settletype = (DynamicObject) this.getView().getParentView().getModel().getValue("settletype");
        // 结算方式
        this.setValue("nckd_settletype", settletype);

        boolean isBE = paramMap.get("isBE") == null ? false : (Boolean)paramMap.get("isBE");
        this.setValue("isbe", isBE);
        if (this.isOther()) {
            this.getView().setVisible(false, new String[]{"payee"});
            this.getView().setVisible(true, new String[]{"payeename"});
            Map<String, Object> map = new HashMap();
            Map<String, Object> style = new HashMap();
            style.put("showEditButton", Boolean.FALSE);
            map.put("item", style);
            this.getView().updateControlMetadata("payeebanknum", map);
        }

        IFormView parentView = this.getView().getParentView();
        if (parentView != null) {
            IDataModel parentModel = parentView.getModel();
            if (parameter.getStatus() != OperationStatus.ADDNEW) {
                DataEntityPropertyCollection properties = model.getDataEntityType().getProperties();
                Iterator var10 = properties.iterator();

                label90:
                while(true) {
                    String propName;
                    String asstacttype;
                    do {
                        do {
                            if (!var10.hasNext()) {
                                this.setValue("bebankf7", parentModel.getValue("payeebank"));
                                if (this.isCashOut()) {
                                    this.setValue("payeeacctcashf7", this.getValue("payeeacctcash"));
                                } else {
                                    this.setValue("payeeacctbankf7", this.getValue("payeeacctbank"));
                                }

                                String sourceBillType = (String)parentModel.getValue("sourcebilltype");
                                List<String> sourceBillTypes = Arrays.asList("ap_finapbill", "pm_purorderbill", "ap_payapply", "er_vehiclecheckingbill", "er_planecheckingbill", "er_hotelcheckingbill", "er_publicreimbursebill", "fca_transupbill", "cfm_repaymentbill", "cfm_interestbill", "lc_arrival", "pmct_paymentapply");
                                if (sourceBillTypes.contains(sourceBillType) || "cas_paybill".equals(sourceBillType) && parentModel.getValue("applyorg") != null) {
                                    this.getView().setEnable(false, new String[]{"payee", "payeename"});
                                }
                                break label90;
                            }

                            IDataEntityProperty property = (IDataEntityProperty)var10.next();
                            propName = property.getName();
                            asstacttype = (String)this.getModel().getValue("asstacttype");
                        } while(parentModel.getProperty(propName) == null);
                    } while(!StringUtils.isNotBlank(asstacttype));

                    boolean isValue = "other".equals(asstacttype) && propName.equals("payee");
                    if (!isValue) {
                        this.setValue(propName, parentModel.getValue(propName));
                    }
                }
            }

            Object billtype = paramMap.get("billtype");
            boolean isShow = false;
            if (parentModel.getDataEntity().containsProperty("businesstype")) {
                String businessType = (String)parentModel.getValue("businesstype");
                isShow = PayBusinessTypeEnum.WALLETESSAY.getValue().equals(businessType) || PayBusinessTypeEnum.WALLETTRANSFER.getValue().equals(businessType);
            }

            this.getView().setVisible(isShow, new String[]{"institutioncode", "institutionname"});
            if (!BillTypeConstants.PAYBILL_SYN.equals(billtype) && !BillTypeConstants.PAYBILL_CASH.equals(billtype) && !BillTypeConstants.PAYBILL_DCEP.equals(billtype)) {
                if (BillTypeConstants.PAYBILL_SPAN.equals(billtype)) {
                    this.changeStyle();
                }
            } else {
                this.getView().setEnable(false, new String[]{"payee", "payeename"});
                this.changeStyle();
            }

            if (this.isCashOut()) {
                this.getView().setVisible(false, new String[]{"payeebankname"});
            }
        }

    }

    private void changeStyle() {
        Map<String, Object> map = new HashMap();
        Map<String, Object> style = new HashMap();
        style.put("eb", Boolean.FALSE);
        map.put("item", style);
        this.getView().updateControlMetadata("payeebanknum", map);
        Map<String, Object> map2 = new HashMap();
        Map<String, Object> style2 = new HashMap();
        style2.put("eb", Boolean.FALSE);
        style2.put("showEditButton", Boolean.FALSE);
        map2.put("item", style2);
        this.getView().updateControlMetadata("payeebankname", map2);
    }

    public void propertyChanged(PropertyChangedArgs e) {
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        if (newValue != oldValue) {
            switch (key) {
                case "payee":
                    if (this.isSupplier()) {
                        this.supplierOrCustomerChanged((DynamicObject)newValue);
                        break;
                    } else if (this.isCustomer()) {
                        this.supplierOrCustomerChanged((DynamicObject)newValue);
                        break;
                    } else if (this.isUser()) {
                        this.userF7Changed((DynamicObject)newValue);
                        break;
                    } else if (this.isCompany()) {
                        this.orgF7Changed((DynamicObject)newValue);
                        break;
                    }
                case "payeeacctbankf7":
                    this.payeeAcctBankF7Changed((DynamicObject)newValue);
                    break;
                case "payeeacctcashf7":
                    this.payeeAcctCashF7Changed((DynamicObject)newValue);
                    break;
                case "erpayeef7":
                    this.erPayeeF7Changed((DynamicObject)newValue);
                    break;
                case "payeebanknum":
                    this.payeeBankNumChanged((String)newValue);
                    break;
                case "bebankf7":
                    this.beBankF7Changed((DynamicObject)newValue);
                    break;
                case "payeebankname":
                    this.payeeBankNameChanged((String)newValue);
            }

        }
    }

    private void orgF7Changed(DynamicObject org) {
        if (!this.isSelectAcctBankF7) {
            this.setValue("payeeacctbankf7", (Object)null);
            this.setValue("payeeacctcashf7", (Object)null);
            this.setValue("payeeaccformid", (Object)null);
            this.setValue("payeebanknum", (Object)null);
            if (org != null) {
                DynamicObject defaultPayAccount = AccountBankHelper.getDefaultPayAccount((Long)org.getPkValue());
                boolean setPayeeInfo = true;
                IDataModel parentModel = this.getView().getParentView().getModel();
                if (parentModel.getDataEntity().containsProperty("businesstype")) {
                    DynamicObject settleType = (DynamicObject)parentModel.getValue("settletype");
                    String businessType = (String)this.getView().getParentView().getModel().getValue("businesstype");
                    boolean isDcep = BaseDataHelper.isSettleTypeDcep(settleType);
                    if (EmptyUtil.isNoEmpty(defaultPayAccount) && isDcep && (PayBusinessTypeEnum.WALLETUP.getValue().equals(businessType) || PayBusinessTypeEnum.WALLETTRANSFER.getValue().equals(businessType)) && !defaultPayAccount.getBoolean("iselecpayment")) {
                        setPayeeInfo = false;
                    }
                }

                if (setPayeeInfo) {
                    this.setValue("payeeacctbankf7", defaultPayAccount);
                    this.setValue("payeeaccformid", "bd_accountbanks");
                }
            }

        }
    }

    private void supplierOrCustomerChanged(DynamicObject basedata) {
        if (basedata == null) {
            this.setValue("payeebanknum", (Object)null);
            this.setValue("recaccbankname", (Object)null);
            this.setValue("bebankf7", (Object)null);
            this.setValue("payeecurrency", (Object)null);
            this.setValue("supplierBankId", (Object)null);
        } else {
            DynamicObject internalOrg = BaseDataHelper.getInternalOrg(this.getDynamicObject("org"), basedata);
            if (internalOrg != null) {
                DynamicObject defaultRecAccount = AccountBankHelper.getDefaultRecAccount((Long)internalOrg.getPkValue());
                if (defaultRecAccount != null) {
                    this.setValue("payeeaccformid", "bd_accountbanks");
                    this.setValue("payeeacctbankf7", defaultRecAccount);
                } else {
                    this.setValue("payeeaccformid", (Object)null);
                    this.setValue("payeeacctbankf7", (Object)null);
                    this.setValue("payeebanknum", (Object)null);
                    this.setValue("recaccbankname", basedata.getLocaleString("name").getLocaleValue());
                    this.setValue("bebankf7", (Object)null);
                    this.setValue("payeecurrency", (Object)null);
                }

                this.setValue("supplierBankId", (Object)null);
            } else {
                SCAccountInfo defaultBankInfo = getDefaultBankInfo(basedata);

                // 供应商id
                Object pkValue = basedata.getPkValue();
                if (defaultBankInfo != null) {
                    this.setValue("payeebanknum", defaultBankInfo.getAccount());
                    String accountName = defaultBankInfo.getAccountName();
                    if (CasHelper.isEmpty(accountName)) {
                        this.setValue("recaccbankname", basedata.getLocaleString("name").getLocaleValue());
                    } else {
                        this.setValue("recaccbankname", accountName);
                    }

                    this.setValue("bebankf7", defaultBankInfo.getBeBank());
                    this.setValue("payeecurrency", defaultBankInfo.getCurrency() == null ? null : defaultBankInfo.getCurrency().getPkValue());
                    this.setValue("supplierBankId", defaultBankInfo.getSupplierBankId());
                } else {
                    this.setValue("payeebanknum", (Object)null);
                    this.setValue("recaccbankname", basedata.getLocaleString("name").getLocaleValue());
                    this.setValue("bebankf7", (Object)null);
                    this.setValue("payeecurrency", (Object)null);
                    this.setValue("supplierBankId", (Object)null);
                }

                this.setValue("payeeaccformid", (Object)null);
                this.setValue("payeeacctbankf7", (Object)null);
            }

        }
    }

    private void userF7Changed(DynamicObject user) {
        if (!this.isSelectErpayeef7F7) {
            if (user == null) {
                this.setValue("erpayeef7", (Object)null);
            } else {
                DynamicObject erPayeeInfo = this.getErPayeeInfo(user.getLong("id"), user.getLocaleString("name").getLocaleValue());
                if (erPayeeInfo != null) {
                    this.setValue("erpayeef7", erPayeeInfo);
                } else if (this.getValue("erpayeef7") != null) {
                    this.setValue("erpayeef7", (Object)null);
                } else {
                    this.setValue("payeebanknum", (Object)null);
                    this.setValue("payeebankname", (Object)null);
                }

            }
        }
    }

    private DynamicObject getErPayeeInfo(long userId, String name) {
        DynamicObject dynamicObject = null;
        QFilter uFilter = new QFilter("payer", "=", userId);
        uFilter = uFilter.and(new QFilter("status", "=", 'C'));
        uFilter.and(new QFilter("enable", "=", "1"));
        QFilter[] qFilters = new QFilter[]{uFilter};
        String selectFields = "id,name,payer,payeraccount,payeraccount01,payeraccount02,payerbank_id,payerbank.name,payerbank.number,payeraccountname";
        String orderBys = "isdefault desc";
        DynamicObject[] defaultAccounts = BusinessDataServiceHelper.load("er_payeer", selectFields, qFilters, orderBys, 1);
        if (defaultAccounts != null && defaultAccounts.length > 0) {
            dynamicObject = defaultAccounts[0];
        }

        return dynamicObject;
    }

    private void payeeAcctBankF7Changed(DynamicObject acctBank) {
        this.setValue("supplierBankId", (Object)null);
        if (acctBank == null) {
            this.setValue("payeename", (Object)null);
            this.setValue("payeebankname", (Object)null);
            this.setValue("recaccbankname", (Object)null);
            this.setValue("recbanknumber", (Object)null);
            this.setValue("payeecurrency", (Object)null);
            this.setValue("bebankf7", (Object)null);
            this.setValue("reccountry", (Object)null);
            this.setValue("recprovince", (Object)null);
            this.setValue("reccity", (Object)null);
        } else {
            this.isSelectAcctBankF7 = true;
            this.setValue("payeeaccformid", "bd_accountbanks");
            PaymentPayeeInfo paymentPayeeInfo = PaymentPayeeInfo.createByAccountBank(acctBank);
            this.setValue("payeebanknum", paymentPayeeInfo.getAccountNumber());
            this.setValue("payeebankname", paymentPayeeInfo.getBankName());
            this.setValue("recbanknumber", paymentPayeeInfo.getBankNumber());
            this.setValue("recaccbankname", paymentPayeeInfo.getPayeeRecName());
            this.setValue("reccountry", paymentPayeeInfo.getCountryId());
            this.setValue("recprovince", paymentPayeeInfo.getProvince());
            this.setValue("reccity", paymentPayeeInfo.getCity());
            this.setValue("bebankf7", paymentPayeeInfo.getBebankId());
            if (this.isOther()) {
                this.setValue("payeename", acctBank.getString("openorg.name"));
            }

            Long defaultCurrencyId = (Long)acctBank.get("defaultcurrency.id");
            this.setValue("payeecurrency", defaultCurrencyId);
            String institutioncode = DcepConverHelper.getInstitUtionCode(acctBank);
            this.setValue("institutioncode", institutioncode);
            String institutionName = DcepConverHelper.getInstitutionName(acctBank);
            this.setValue("institutionname", institutionName);
        }
    }

    private void payeeAcctCashF7Changed(DynamicObject acctCash) {
        if (acctCash == null) {
            this.setValue("payeename", (Object)null);
            this.setValue("payeebankname", (Object)null);
            this.setValue("recaccbankname", (Object)null);
            this.setValue("recbanknumber", (Object)null);
            this.setValue("payeecurrency", (Object)null);
            this.setValue("bebankf7", (Object)null);
            this.setValue("reccountry", (Object)null);
            this.setValue("recprovince", (Object)null);
            this.setValue("reccity", (Object)null);
        } else {
            this.isSelectAcctBankF7 = true;
            this.setValue("payeeaccformid", "cas_accountcash");
            this.setValue("payeebanknum", acctCash.getString("number"));
            Long defaultCurrencyId = (Long)acctCash.get("defaultcurrency.id");
            this.setValue("payeecurrency", defaultCurrencyId);
        }
    }

    private void erPayeeF7Changed(DynamicObject erPayee) {
        if (erPayee == null) {
            this.setValue("payeebanknum", (Object)null);
            this.setValue("recaccbankname", (Object)null);
            this.setValue("bebankf7", (Object)null);
        } else {
            this.isSelectAcctBankF7 = true;
            DynamicObject user = this.getDynamicObject("payee");
            if (user == null) {
                QFilter filter = new QFilter("name", "=", erPayee.getString("name"));
                DynamicObject[] users = BusinessDataServiceHelper.load("bos_user", "id, name, number", new QFilter[]{filter});
                if (users != null && users.length > 0) {
                    this.isSelectErpayeef7F7 = true;
                    this.setValue("payee", users[0]);
                }
            }

            this.setValue("payeebanknum", erPayee.getString("payeraccount"));
            this.setValue("recaccbankname", erPayee.getString("payeraccountname"));
            this.setValue("bebankf7", erPayee.getDynamicObject("payerbank"));
        }
    }

    private void payeeBankNumChanged(String newValue) {
        if (!this.isSelectAcctBankF7) {
            if (newValue == null || StringUtils.isBlank(newValue)) {
                this.setValue("payeename", (Object)null);
                this.setValue("payeebankname", (Object)null);
                this.setValue("recaccbankname", (Object)null);
                this.setValue("supplierbankid", (Object)null);
            }

            DynamicObject payeeAccountCash;
            if (Objects.equals("bd_accountbanks", this.getString("payeeaccformid"))) {
                payeeAccountCash = this.getDynamicObject("payeeacctbankf7");
                if (payeeAccountCash == null || !Objects.equals(payeeAccountCash.getString("bankaccountnumber"), newValue)) {
                    this.getModel().beginInit();
                    this.setValue("payeeaccformid", (Object)null);
                    this.setValue("payeeacctbankf7", (Object)null);
                    this.getModel().endInit();
                    this.getView().updateView("payeeaccformid");
                    this.getView().updateView("payeeacctbankf7");
                }
            } else if (Objects.equals("cas_accountcash", this.getString("payeeaccformid"))) {
                payeeAccountCash = this.getDynamicObject("payeeacctcashf7");
                if (payeeAccountCash == null || !Objects.equals(payeeAccountCash.getString("number"), newValue)) {
                    this.getModel().beginInit();
                    this.setValue("payeeaccformid", (Object)null);
                    this.setValue("payeeacctcashf7", (Object)null);
                    this.getModel().endInit();
                    this.getView().updateView("payeeaccformid");
                    this.getView().updateView("payeeacctcashf7");
                }
            }

        }
    }

    private void beBankF7Changed(DynamicObject beBank) {
        String payeeBankName = (String)this.getModel().getValue("payeebankname");
        //todo
        if (beBank != null) {
            this.isSelectBankF7 = true;

            String cityTxt = beBank.getString("citytxt");
            String provinceTxt = beBank.getString("provincetxt");
            this.setValue("payeebankname", CasHelper.getLocalValue(beBank, "name"));
            this.setValue("recbanknumber", beBank.getString("union_number"));
            this.setValue("reccountry", beBank.get("country"));
            this.setValue("recprovince", provinceTxt);
            this.setValue("reccity", cityTxt);
        } else {
            if (StringUtils.isEmpty(payeeBankName)) {
                this.setValue("payeebankname", (Object)null);
            }

            this.setValue("recbanknumber", (Object)null);
            this.setValue("reccountry", (Object)null);
            this.setValue("recprovince", (Object)null);
            this.setValue("reccity", (Object)null);
        }

    }

    private void payeeBankNameChanged(String newValue) {
        if (!this.isSelectBankF7) {
            if (newValue != null && StringUtils.isNotBlank(newValue)) {
                QFilter qFilter = new QFilter("name", "=", newValue);
                qFilter = qFilter.and(new QFilter("enable", "=", "1"));
                qFilter = qFilter.and(new QFilter("status", "=", 'C'));
                QFilter[] qFilters = new QFilter[]{qFilter};
                DynamicObject[] colls = BusinessDataServiceHelper.load("bd_bebank", "id, name, number, basedatafield1", qFilters);
                if (colls != null && colls.length > 0) {
                    this.setValue("bebankf7", colls[0]);
                } else {
                    this.setValue("bebankf7", (Object)null);
                }
            } else {
                this.setValue("bebankf7", (Object)null);
            }

        }
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control)evt.getSource();
        BasedataEdit payeeacctcashf7;
        switch (c.getKey().toLowerCase()) {
            case "payeebanknum":
                if (this.isUser()) {
                    DynamicObject obj = this.getDynamicObject("payee");
                    if (obj == null) {
                        this.getView().showTipNotification(ResManager.loadKDString("请先选择收款人。", "PayeeInfoEdit_1", "fi-cas-formplugin", new Object[0]));
                        return;
                    }

                    BasedataEdit erpayeef7 = (BasedataEdit)this.getControl("erpayeef7");
                    erpayeef7.click();
                } else if (this.isSupplier()) {
                    this.showBankInfoF7("supplier");
                } else if (this.isCustomer()) {
                    this.showBankInfoF7("customer");
                } else if (this.isCashOut()) {
                    payeeacctcashf7 = (BasedataEdit)this.getControl("payeeacctcashf7");
                    payeeacctcashf7.click();
                } else {
                    payeeacctcashf7 = (BasedataEdit)this.getControl("payeeacctbankf7");
                    payeeacctcashf7.click();
                }
                break;
            case "payeebankname":
                payeeacctcashf7 = (BasedataEdit)this.getControl("bebankf7");
                payeeacctcashf7.click();
                break;
            case "btnok":
                this.btnOk();
        }

    }

    private void showBankInfoF7(String f7type) {
        DynamicObject obj = this.getDynamicObject("payee");
        if (obj == null) {
            this.getView().showTipNotification(ResManager.loadKDString("请先选择收款人。", "PayeeInfoEdit_1", "fi-cas-formplugin", new Object[0]));
        } else {
            DynamicObject internalOrg = BaseDataHelper.getInternalOrg(this.getDynamicObject("org"), obj);
            if (internalOrg != null) {
                BasedataEdit payeeacctbankf7 = (BasedataEdit)this.getControl("payeeacctbankf7");
                payeeacctbankf7.click();
            } else {
                SCAccountInfo defaultBankInfo = BaseDataHelper.getDefaultBankInfo(obj);
                if (defaultBankInfo == null) {
                    this.getView().showTipNotification(ResManager.loadKDString("请维护对应客商的银行信息。", "PayeeInfoEdit_2", "fi-cas-formplugin", new Object[0]));
                    return;
                }

                ListShowParameter lsp = null;
                if ("supplier".equals(f7type)) {
                    lsp = DynamicListHelper.getSupplierBankInfoShowParameter(obj.getPkValue());
                } else {
                    lsp = DynamicListHelper.getCustomerBankInfoShowParameter(obj.getPkValue());
                }

                CloseCallBack closeCallBack = new CloseCallBack(this, "payeeaccountbank");
                lsp.setCloseCallBack(closeCallBack);
                this.setIFilters(lsp.getListFilterParameter(), this.getModel().getDataEntity());
                this.getView().showForm(lsp);
            }

        }
    }

    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        String actionId = closedCallBackEvent.getActionId();
        if ("payeeaccountbank".equals(actionId)) {
            Object returnData = closedCallBackEvent.getReturnData();
            if (returnData != null) {
                ListSelectedRowCollection rowInfo = (ListSelectedRowCollection)returnData;
                Object entryKey = rowInfo.getEntryPrimaryKeyValues()[0];
                String asstActType = (String)this.getModel().getValue("asstacttype");
                SCAccountInfo scAccountInfo = BaseDataHelper.loadBankInfo(asstActType, (Long)entryKey);
                this.bindBankInfo(scAccountInfo);
            }
        }

    }

    private void bindBankInfo(SCAccountInfo scAccountInfo) {
        if (scAccountInfo == null) {
            this.setValue("payeebanknum", (Object)null);
            this.setValue("recaccbankname", (Object)null);
            this.setValue("bebankf7", (Object)null);
            this.setValue("payeecurrency", (Object)null);
            this.setValue("supplierbankid", (Object)null);
        } else {
            this.setValue("payeebanknum", scAccountInfo.getAccount());
            String accountName = scAccountInfo.getAccountName();
            if (CasHelper.isEmpty(accountName)) {
                String asstactName = null;
                if (this.isCustomer() || this.isSupplier()) {
                    asstactName = this.getDynamicObject("payee").getLocaleString("name").getLocaleValue();
                }
                this.setValue("recaccbankname", asstactName);
            } else {
                this.setValue("recaccbankname", accountName);
            }
            boolean isaccept = false;
            String asstActType = (String)this.getModel().getValue("asstacttype");
            DynamicObject settletype = (DynamicObject) this.getValue("nckd_settletype");

            if( asstActType.equals("bd_supplier") && ObjectUtil.isNotEmpty(settletype) && ACCEPT_TYPE.contains((String) settletype.get("number")) ){
                isaccept = true;
            }
            // 供应商id

            if(isaccept){
                Object supperid = ((DynamicObject)this.getModel().getValue("payee")).getPkValue();
                DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(supperid, "bd_supplier");
                // 把供应商承兑银行信息自动带出到往来银行字段
                DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");
                String eAssacct = (String) this.getModel().getValue("payeebanknum");

                for (DynamicObject dynamicObject : entryBank) {
                    if(dynamicObject.getString("bankaccount").equals(eAssacct)){
                        DynamicObject nckdAcceptingbank = dynamicObject.getDynamicObject("nckd_acceptingbank");
                        if(ObjectUtils.isNotEmpty(nckdAcceptingbank)){
                            this.setValue("bebankf7", nckdAcceptingbank);
                        }else{
                            this.setValue("bebankf7", scAccountInfo.getBeBank());
                        }
                        break;
                    }
                }
            }


//            this.setValue("bebankf7", scAccountInfo.getBeBank());
            this.setValue("payeecurrency", scAccountInfo.getCurrency() == null ? null : scAccountInfo.getCurrency().getPkValue());
            this.setValue("supplierbankid", scAccountInfo.getSupplierBankId());
        }
    }

    private void btnOk() {
        IDataModel model = this.getModel();
        this.storeFields();
        Map<String, Object> resultMap = new HashMap(50);
        resultMap.put("payeeformid", model.getValue("asstacttype"));
        DynamicObject object = (DynamicObject)model.getValue("payee");
        resultMap.put("payee", Long.valueOf((String)Optional.ofNullable(object).map((v) -> {
            return v.getString("id");
        }).orElse("0")));
        resultMap.put("payeename", model.getValue("payeename"));
        resultMap.put("payeenumber", Optional.ofNullable(object).map((o) -> {
            return o.getString("number");
        }).orElse(""));
        resultMap.put("payeeaccformid", model.getValue("payeeaccformid"));
        resultMap.put("payeeacctbank", model.getValue("payeeacctbank"));
        resultMap.put("payeeacctcash", model.getValue("payeeacctcash"));
        resultMap.put("payeebanknum", model.getValue("payeebanknum"));
        resultMap.put("institutioncode", model.getValue("institutioncode"));
        resultMap.put("institutionname", model.getValue("institutionname"));
        DynamicObject payeeacctbankf7 = this.getDynamicObject("payeeacctbankf7");
        DynamicObject bebankf7;
        DynamicObject payee;
        if (payeeacctbankf7 != null) {
            bebankf7 = payeeacctbankf7.getDynamicObject("bank");
            if (bebankf7 != null) {
                payee = BusinessDataServiceHelper.loadSingle(bebankf7.getPkValue(), "bd_finorginfo", "swift_code,routingnum,other_code,address_eng");
                if (payee != null) {
                    resultMap.put("swift_code", payee.getString("swift_code"));
                    resultMap.put("routingnum", payee.getString("routingnum"));
                    resultMap.put("other_code", payee.getString("other_code"));
                    resultMap.put("address_eng", payee.getString("address_eng"));
                }
            }
        }

        bebankf7 = this.getDynamicObject("bebankf7");
        resultMap.put("payeebank", bebankf7);
        if (bebankf7 != null) {
            resultMap.put("swift_code", bebankf7.getString("swift_code"));
            resultMap.put("routingnum", bebankf7.getString("routingnum"));
            resultMap.put("other_code", bebankf7.getString("other_code"));
            resultMap.put("address_eng", bebankf7.getString("address_eng"));
        }

        resultMap.put("payeebankname", model.getValue("payeebankname"));
        resultMap.put("recaccbankname", model.getValue("recaccbankname"));
        resultMap.put("recbanknumber", model.getValue("recbanknumber"));
        resultMap.put("reccountry", model.getValue("reccountry"));
        resultMap.put("recprovince", model.getValue("recprovince"));
        resultMap.put("reccity", model.getValue("reccity"));
        resultMap.put("payeeemail", model.getValue("payeeemail"));
        resultMap.put("payeecurrency", model.getValue("payeecurrency"));
        resultMap.put("payeeaddress", model.getValue("payeeaddress"));
        resultMap.put("supplierBankId", model.getValue("supplierBankId"));
        if (this.isSupplier()) {
            payee = (DynamicObject)model.getValue("payee");
            if (!CasHelper.isEmpty(payee)) {
                DynamicObject supplierDO = BusinessDataServiceHelper.loadSingle(payee.getPkValue(), "bd_supplier", "entry_bank.settlment,entry_bank.isdefault_bank");
                if (!CasHelper.isEmpty(supplierDO)) {
                    DynamicObjectCollection entry_banks = supplierDO.getDynamicObjectCollection("entry_bank");
                    if (!CasHelper.isEmpty(entry_banks)) {
                        for(int i = 0; i < entry_banks.size(); ++i) {
                            if (i == 0 || ((DynamicObject)entry_banks.get(i)).getBoolean("isdefault_bank")) {
                                DynamicObject entry_bank = (DynamicObject)entry_banks.get(i);
                                DynamicObject settlment = entry_bank.getDynamicObject("settlment");
                                resultMap.put("settlment", settlment);
                            }
                        }
                    }
                }
            }
        }

        this.getView().returnDataToParent(resultMap);
        this.getView().close();
    }

    private void storeFields() {
        DynamicObject payee = this.getDynamicObject("payee");
        if (!CasHelper.isEmpty(payee)) {
            this.setDefaultAddress(payee);
            this.setValue("payeename", payee.getLocaleString("name").getLocaleValue());
            if (!this.isSupplier() && !this.isCustomer()) {
                if (this.isUser()) {
                    this.setValue("payeename", payee.getLocaleString("name").getLocaleValue());
                    this.setValue("payeeemail", payee.getString("email"));
                } else if (this.isCompany()) {
                    this.setValue("payeename", payee.getLocaleString("name").getLocaleValue());
                }
            } else {
                String entityName = "";
                String addressPropName = "";
                if (this.isSupplier()) {
                    entityName = "bd_supplier";
                    addressPropName = "supplieraddress";
                } else {
                    entityName = "bd_customer";
                    addressPropName = "customeraddress";
                }

                DynamicObject supplier = BusinessDataServiceHelper.loadSingle(payee.getPkValue(), entityName, "entry_address." + addressPropName);
                DynamicObjectCollection addressls = supplier.getDynamicObjectCollection("entry_address");
                if (addressls != null && addressls.size() > 0) {
                    Set<Object> addressIds = new HashSet();
                    Iterator var7 = addressls.iterator();

                    while(var7.hasNext()) {
                        DynamicObject a = (DynamicObject)var7.next();
                        addressIds.add(a.get(addressPropName + "_id"));
                    }

                    DynamicObject[] addresses = BusinessDataServiceHelper.load("bd_address", "default,detailaddress,addemail", new QFilter[]{new QFilter("id", "in", addressIds)});
                    if (addresses != null) {
                        DynamicObject[] var14 = addresses;
                        int var9 = addresses.length;

                        for(int var10 = 0; var10 < var9; ++var10) {
                            DynamicObject address = var14[var10];
                            if (address.getBoolean("default")) {
                                this.setValue("payeeemail", address.getString("addemail"));
                                this.setValue("payeeaddress", address.getString("detailaddress"));
                                break;
                            }
                        }
                    }
                }
            }
        }

        DynamicObject acctCash;
        if (this.isUser()) {
            if (this.getValue("erpayeef7") != null) {
                this.setValue("payeeaccformid", "er_payeer");
                acctCash = this.getDynamicObject("erpayeef7");
                this.setValue("payeeacctbank", acctCash.getPkValue());
            }
        } else if (this.isCashOut()) {
            this.setValue("payeeaccformid", "cas_accountcash");
            if (this.getValue("payeeacctcashf7") != null) {
                acctCash = this.getDynamicObject("payeeacctcashf7");
                this.setValue("payeeacctcash", acctCash.getPkValue());
            }

            this.setValue("payeeacctbank", (Object)null);
        } else if (this.getValue("payeeacctbankf7") != null) {
            this.setValue("payeeaccformid", "bd_accountbanks");
            acctCash = this.getDynamicObject("payeeacctbankf7");
            this.setValue("payeeacctbank", acctCash.getPkValue());
        } else {
            this.setValue("payeeaccformid", (Object)null);
            this.setValue("payeeacctbank", (Object)null);
        }

    }

    private void setDefaultAddress(DynamicObject payee) {
        long payeeId = payee.getLong("id");
        if (!EmptyUtil.isEmpty(payeeId)) {
            String payeeType = (String)this.getValue("asstacttype");
            String address = AsstActTypeAddressEnum.getAddressByAsstActType(payeeType, payeeId);
            this.setValue("payeeaddress", address);
        }
    }

    private boolean isOther() {
        return this.isAimType(AsstActTypeEnum.OTHER);
    }

    private boolean isUser() {
        return this.isAimType(AsstActTypeEnum.EMPLOYEE);
    }

    private boolean isCompany() {
        return this.isAimType(AsstActTypeEnum.COMPANY);
    }

    private boolean isSupplier() {
        return this.isAimType(AsstActTypeEnum.SUPPLIER);
    }

    private boolean isCustomer() {
        return this.isAimType(AsstActTypeEnum.CUSTOMER);
    }

    private boolean isAimType(AsstActTypeEnum asstactType) {
        Object payeeType = this.getValue("asstacttype");
        return asstactType.getValue().equals(payeeType);
    }

    private boolean isCashOut() {
        boolean isCashOut = false;
        IFormView parentView = this.getView().getParentView();
        if (parentView != null) {
            IDataModel parentModel = parentView.getModel();
            IDataEntityProperty property = parentModel.getProperty("billtype");
            if (property == null) {
                return isCashOut;
            }

            DynamicObject billtype = (DynamicObject)parentModel.getValue("billtype");
            if (billtype != null && BillTypeConstants.PAYBILL_CASH.equals(billtype.getPkValue())) {
                String businesstype = (String)parentModel.getValue("businesstype");
                if (PayBusinessTypeEnum.CASHOUT.getValue().equals(businesstype)) {
                    isCashOut = true;
                }
            }
        }

        return isCashOut;
    }

    private void setIFilters(ListFilterParameter lfp, DynamicObject payBill) {
        LOGGER.info("开始进行二开扩展对外服务业务埋点 - kd.sdk.fi.cas.extpoint.paybill.IPayeeBankInfoFilter");
        PluginProxy<IPayeeBankInfoFilter> pluginProxy = PluginProxy.create(IPayeeBankInfoFilter.class, "fi.cas.filter.paybill.payeebankinfo");
        pluginProxy.callReplace((proxy) -> {
            proxy.setIFilters(lfp, payBill);
            return null;
        });
    }


    public  SCAccountInfo getDefaultBankInfo(DynamicObject basedata) {
        if (basedata == null) {
            return null;
        } else {
            String entity = basedata.getDataEntityType().getName();
            basedata = BusinessDataServiceHelper.loadSingleFromCache(basedata.getPkValue(), entity);
            DynamicObjectCollection bankColls = basedata.getDynamicObjectCollection("entry_bank");
            DynamicObject defaultBankAccountInfo = null;
            boolean isaccept = false;
            String asstActType = (String)this.getModel().getValue("asstacttype");
            DynamicObject settletype = (DynamicObject) this.getValue("nckd_settletype");

            if( asstActType.equals("bd_supplier") && ObjectUtil.isNotEmpty(settletype) && ACCEPT_TYPE.contains((String) settletype.get("number")) ){
                isaccept = true;
            }

            for(int i = 0; i < bankColls.size(); ++i) {
                if (i == 0 || ((DynamicObject)bankColls.get(i)).getBoolean("isdefault_bank")) {
                    defaultBankAccountInfo = (DynamicObject)bankColls.get(i);

                }
            }

            if (defaultBankAccountInfo != null) {
                SCAccountInfo scAccountInfo = new SCAccountInfo();
                scAccountInfo.setAccount(defaultBankAccountInfo.getString("bankaccount"));
                scAccountInfo.setAccountName(defaultBankAccountInfo.getLocaleString("accountname").getLocaleValue());
                scAccountInfo.setCurrency(defaultBankAccountInfo.getDynamicObject("currency"));
                scAccountInfo.setDefault(defaultBankAccountInfo.getBoolean("isdefault_bank"));
                //承兑银行
                DynamicObject nckdAcceptingbank = defaultBankAccountInfo.getDynamicObject("nckd_acceptingbank");
                if(ObjectUtils.isNotEmpty(nckdAcceptingbank) && isaccept){
                    scAccountInfo.setBeBank(nckdAcceptingbank);
                }else{
                    scAccountInfo.setBeBank(defaultBankAccountInfo.getDynamicObject("bank"));
                }
                scAccountInfo.setSupplierBankId(Long.valueOf(defaultBankAccountInfo.getPkValue().toString()));
                return scAccountInfo;
            } else {
                return null;
            }
        }
    }

}
