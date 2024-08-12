package nckd.yanye.fi.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.exception.KDBizException;
import kd.bos.form.ShowType;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.events.HyperLinkClickEvent;
import kd.bos.form.events.HyperLinkClickListener;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.DateEdit;
import kd.bos.form.field.OrgEdit;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.fa.business.calc.DataModelWrapper;
import kd.fi.fa.business.calc.IObjWrapper;
import kd.fi.fa.business.enums.lease.InvoiceType;
import kd.fi.fa.business.enums.lease.LeaseContractSourceType;
import kd.fi.fa.business.enums.lease.PayFrequency;
import kd.fi.fa.business.lease.LeaseContractCal;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.business.utils.FaBigDecimalUtil;
import kd.fi.fa.common.util.DateUtil;
import kd.fi.fa.common.util.Fa;
import kd.fi.fa.formplugin.lease.LeaseBizRecordTabHandler;
import kd.fi.fa.formplugin.lease.LeaseContractEditPlugin;
import kd.fi.fa.utils.FaF7DeptUtils;
import kd.fi.fa.utils.FaFormPermissionUtil;
import kd.fi.fa.utils.FaShowFormUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;
import java.util.Iterator;

/**
 *
 * Module           :财务云-租赁管理-退养人员工资
 * Description      :退休人员工资编辑插件
 *
 * @author guozhiwei
 * @date  2024/8/2 11:01
 *  标识:nckd_fa_salary_retir
 *
 */


public class SalaryRetirEditPlugin extends AbstractBillPlugIn implements HyperLinkClickListener {

    private static final Log logger = LogFactory.getLog(LeaseContractEditPlugin.class);
    private static final String FA_LEASE_CONTRACT = "fa_lease_contract";
    private static final String FA_LEASE_CONTRACT_INIT = "fa_lease_contract_init";
    private static final String[] EXEMPT_AFFECTED_FIELDS = new String[]{"leaseliabori", "leaseliab", "leaseassets", "assetsaccumdepre", "assetsaddupyeardepre", "hasdepremonths", "accumrent", "addupyearrent", "accuminterest", "addupyearinterest"};

    public SalaryRetirEditPlugin() {
    }

    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit assetUnit = (BasedataEdit)this.getView().getControl("assetunit");
        assetUnit.addBeforeF7SelectListener((evt) -> {
            FaFormPermissionUtil.beforeAssetUnitSelectV2(this.getView().getPageId(), evt, "nckd_fa_salary_retir");
        });
        OrgEdit org = (OrgEdit)this.getView().getControl("org");
        if (org != null) {
            org.setIsOnlyDisplayOrgLeaves(true);
            org.addBeforeF7SelectListener((listener) -> {
                FaF7DeptUtils.orgDelegateAssetUnit(listener, this.getModel());
            });
        }

        EntryGrid srcContractEntry = (EntryGrid)this.getControl("srccontractentity");
        srcContractEntry.addHyperClickListener(this);
        EntryGrid terminationEntry = (EntryGrid)this.getControl("terminationentry");
        terminationEntry.addHyperClickListener(this);
        EntryGrid leaseChangeEntry = (EntryGrid)this.getControl("leasechangeentry");
        leaseChangeEntry.addHyperClickListener(this);
    }

    public void afterCreateNewData(EventObject e) {
        this.setCurrencyAndSysSwitchDate();
    }

    public void afterLoadData(EventObject e) {
        this.setDiscountRateEnable();
        this.setRuleDeductibleEnable4LoadData();
        String isClickBefChgContract = this.getView().getParentView().getPageCache().get("isClickBefChgContract");
        if ("true".equals(isClickBefChgContract)) {
            this.getView().setVisible(Boolean.FALSE, new String[]{"queryrentsettle", "viewbillrelation"});
        } else {
            this.getView().setVisible(Boolean.TRUE, new String[]{"queryrentsettle", "viewbillrelation"});
        }

    }

    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        (new LeaseBizRecordTabHandler(this.getView())).handle();
    }

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        this.setLeaseAssetsGroupVisible();
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
        if (status == OperationStatus.ADDNEW) {
            DynamicObject org = (DynamicObject)this.getModel().getValue("org");
            if (org == null) {
                return;
            }

            QFilter[] filters = new QFilter[]{new QFilter("org", "=", org.getPkValue())};
            DynamicObject leaseInit = QueryServiceHelper.queryOne("gl_accountbook", Fa.comma(new String[]{"basecurrency", "status"}), filters);
            if (leaseInit == null) {
                this.getView().showErrorNotification(ResManager.loadKDString("会计账簿初始化未设置，不允许新增合同。", "LeaseContractEditPlugin_0", "fi-fa-formplugin", new Object[0]));
                this.getView().setEnable(Boolean.FALSE, new String[]{"conentpanel"});
                return;
            }
            if(leaseInit == null){
                return;
            }
            this.initLeaseContractCheck(leaseInit);
        }

        this.setFreeLeaseStartDateRange();
    }

    public void propertyChanged(PropertyChangedArgs e) {
        String propName = e.getProperty().getName();
        int index = e.getChangeSet()[0].getRowIndex();
        logger.info(String.format("退养人员工资[%s]变化字段：%s，行号(从0开始)：%s，[%s] -> [%s]。", this.getModel().getValue("number"), propName, index, e.getChangeSet()[0].getOldValue(), e.getChangeSet()[0].getNewValue()));
        switch (propName) {
            case "org":
                this.setCurrencyAndSysSwitchDate();
                this.setInitConfirmDate();
                this.setTransitionPlan();
                this.setDailyDiscountRate();
//                this.setIsExempt();
                this.setLeaseMonths();
                this.setDepreMonths();
                this.setFreeLeaseMonths();
                break;
            case "currency":
                this.setDiscountRate();
                break;
            case "assetcat":
                this.setUnit();
                break;
            case "assetamount":
                this.checkAssetAmountValid(e);
                break;
            case "leasestartdate":
                this.setLeaseEndDateRange();
                this.setFreeLeaseStartDateRange();
                this.setLeaseMonths();
                this.setFreeLeaseMonths();
                this.setInitConfirmDate();
                this.setLeaseTermStartDate();
                break;
            case "leaseenddate":
//                this.setIsExempt();
                this.setLeaseMonths();
                this.setDepreMonths();
                break;
            case "freeleasestartdate":
                this.setFreeLeaseMonths();
                this.setInitConfirmDate();
                this.setLeaseTermStartDate();
                break;
            case "initconfirmdate":
//                this.setIsExempt();
                this.setDepreMonths();
                this.setDiscountRate();
                break;
            case "leasetermstartdate":
                this.setTransitionPlan();
                this.setDepreMonths();
                break;
            case "isexempt":
                this.setDiscountRateEnable();
                this.clearDataIfExempt();
                this.setTransitionPlan();
                break;
            case "depremonths":
                this.setDiscountRate();
                break;
            case "discountrate":
                this.setDailyDiscountRate();
                break;
            case "transitionplan":
                this.setDepreMonths();
                this.setDiscountRate();
                break;
            case "rule_payitem":
                this.setPayRuleFrequency(index);
                break;
            case "rule_invoicetype":
                this.setRuleDeductibleRange(index);
                break;
            case "rule_deductible":
                this.checkRuleDeductibleValid(e);
                break;
            case "rule_taxrate":
            case "amount":
                this.setRuleTax(index);
        }

    }

    public void afterCopyData(EventObject e) {
        this.getModel().deleteEntryData("payplanentryentity");
        this.setDiscountRateEnable();
    }

    public void hyperLinkClick(HyperLinkClickEvent evt) {
        String fieldName = evt.getFieldName();
        int rowIndex = evt.getRowIndex();
        switch (fieldName) {
            case "srccontractshow":
                this.showSrcContractForm();
                break;
            case "clearbillno":
                this.showClearBillForm();
                break;
            case "renewalcontractshow":
                this.showRenewalContractForm();
                break;
            case "leasechangebillno":
                this.showLeaseChangeBillForm(rowIndex);
                break;
            case "befchangeversion":
                this.showBefChangeContractForm(rowIndex);
        }

    }

    private IObjWrapper buildObjWrapper() {
        return new DataModelWrapper(this.getModel());
    }

    private void setCurrencyAndSysSwitchDate() {
        DynamicObject org = (DynamicObject)this.getModel().getValue("org");
        if (org != null) {
            QFilter[] filters = new QFilter[]{new QFilter("org", "=", org.getPkValue())};
            DynamicObject leaseInit = QueryServiceHelper.queryOne("gl_accountbook", Fa.comma(new String[]{"basecurrency", "status"}), filters);
            if (leaseInit == null) {
                this.getView().showErrorNotification(ResManager.loadKDString("会计账簿初始化未设置，不允许新增合同。", "LeaseContractEditPlugin_0", "fi-fa-formplugin", new Object[0]));
                this.getView().setEnable(Boolean.FALSE, new String[]{"conentpanel"});
                return;
            }
            if(leaseInit == null){
                return;
            }
            long baseCurrencyId = leaseInit.getLong("basecurrency");
            this.getModel().setValue("currency", baseCurrencyId);

        } else {
            this.getView().showErrorNotification(ResManager.loadKDString("核算组织为空。", "LeaseContractEditPlugin_1", "fi-fa-formplugin", new Object[0]));
        }

    }

    private void initLeaseContractCheck(DynamicObject leaseInit) {
        String entityName = this.getModel().getDataEntityType().getName();
        if ("fa_lease_contract_init".equals(entityName)) {
//            Date sysSwitchDate = leaseInit.getDate("systemswitchday");
//            if (sysSwitchDate == null) {
//                this.getView().showErrorNotification(ResManager.loadKDString("系统切换日未维护，请先维护租赁初始化中数据。", "LeaseContractEditPlugin_2", "fi-fa-formplugin", new Object[0]));
//                this.getView().setEnable(Boolean.FALSE, new String[]{"conentpanel"});
//            }

//            this.getModel().setValue("sysswitchdate", sysSwitchDate);
            String status = leaseInit.getString("status");
//            if ("C".equals(status)) {
//                this.getView().showErrorNotification(ResManager.loadKDString("租赁初始化已启用，不能新增初始化租赁合同。", "LeaseContractEditPlugin_3", "fi-fa-formplugin", new Object[0]));
//                this.getView().setEnable(Boolean.FALSE, new String[]{"conentpanel"});
//            }
        }

    }

    private void setUnit() {
        DynamicObject assetCat = (DynamicObject)this.getModel().getValue("assetcat");
        if (assetCat != null) {
            DynamicObject unit = assetCat.getDynamicObject("unit");
            DynamicObject curUnit = (DynamicObject)this.getModel().getValue("unit");
            if (curUnit == null && unit != null) {
                this.getModel().setValue("unit", unit.getPkValue());
            }
        }

    }

    private void setLeaseEndDateRange() {
        Date leaseStartDate = (Date)this.getModel().getValue("leasestartdate");
        if (leaseStartDate != null) {
            DateEdit leaseEndDateEdit = (DateEdit)this.getView().getControl("leaseenddate");
            leaseEndDateEdit.setMinDate(DateUtil.addDay(leaseStartDate, 1));
            Date leaseEndDate = (Date)this.getModel().getValue("leaseenddate");
            if (leaseEndDate != null && DateUtil.compareShortDate(leaseStartDate, leaseEndDate) >= 0) {
                this.getModel().setValue("leaseenddate", (Object)null);
            }

        }
    }

    private void setFreeLeaseStartDateRange() {
        Date leaseStartDate = (Date)this.getModel().getValue("leasestartdate");
        if (leaseStartDate != null) {
            DateEdit freeLeaseStartDateEdit = (DateEdit)this.getView().getControl("freeleasestartdate");
            freeLeaseStartDateEdit.setMaxDate(DateUtil.addDay(leaseStartDate, -1));
            Date freeLeaseStartDate = (Date)this.getModel().getValue("freeleasestartdate");
            if (freeLeaseStartDate != null && DateUtil.compareShortDate(freeLeaseStartDate, leaseStartDate) > 0) {
                this.getModel().setValue("freeleasestartdate", (Object)null);
            }

        }
    }

    private void setLeaseMonths() {
        LeaseContractCal.setLeaseMonths(this.buildObjWrapper());
    }

    private void setFreeLeaseMonths() {
        LeaseContractCal.setFreeLeaseMonths(this.buildObjWrapper());
    }

    private void setRuleDeductibleRange(int index) {
        String invoiceType = (String)this.getModel().getValue("rule_invoicetype", index);
        if (InvoiceType.DEDICATED.getValue().equals(invoiceType)) {
            this.getView().setEnable(Boolean.TRUE, index, new String[]{"rule_deductible"});
            this.getModel().setValue("rule_deductible", Boolean.TRUE, index);
        } else if (InvoiceType.COMMON.getValue().equals(invoiceType)) {
            this.getModel().setValue("rule_deductible", Boolean.FALSE, index);
            this.getView().setEnable(Boolean.FALSE, index, new String[]{"rule_deductible"});
        }

    }

    private void setRuleTax(int index) {
        BigDecimal ruleTaxRate = (BigDecimal)this.getModel().getValue("rule_taxrate", index);
        BigDecimal ruleAmount = (BigDecimal)this.getModel().getValue("amount", index);
        if (BigDecimal.ZERO.compareTo(ruleTaxRate) != 0 && BigDecimal.ZERO.compareTo(ruleAmount) != 0) {
            ruleTaxRate = ruleTaxRate.divide(FaBigDecimalUtil.HUNDRED, 4, LeaseUtil.getRoundingMode4CalAmount());
            BigDecimal ruleTax = ruleAmount.subtract(ruleAmount.divide(BigDecimal.ONE.add(ruleTaxRate), this.getCurrencyAmtPrecision(), LeaseUtil.getRoundingMode4CalAmount()));
            this.getModel().setValue("rule_tax", ruleTax, index);
        }
    }

    private void setInitConfirmDate() {
        LeaseContractCal.setInitConfirmDate(this.buildObjWrapper());
    }

    private void setIsExempt() {
        String parentFormId = this.getView().getFormShowParameter().getParentFormId();
        if (!parentFormId.equals("fa_lease_change_bill")) {
            LeaseContractCal.setIsExempt(this.buildObjWrapper());
        }

    }

    private void setDiscountRate() {
        String formId = this.getView().getFormShowParameter().getFormId();
        if (!"fa_lease_contract".equalsIgnoreCase(formId) && !"fa_lease_contract_init".equalsIgnoreCase(formId)) {
            BigDecimal discountRate = (BigDecimal)this.getModel().getValue("discountrate");
            if (discountRate == null || BigDecimal.ZERO.compareTo(discountRate) == 0) {
                LeaseContractCal.setDiscountRate(this.buildObjWrapper());
            }
        } else {
            LeaseContractCal.setDiscountRate(this.buildObjWrapper());
        }

    }

    private void setDailyDiscountRate() {
        LeaseContractCal.setDailyDiscountRate(this.buildObjWrapper());
    }

    private void setDepreMonths() {
        LeaseContractCal.setDepreMonths(this.buildObjWrapper());
    }

    private void setPayRuleFrequency(int index) {
        DynamicObject payItem = (DynamicObject)this.getModel().getValue("rule_payitem", index);
        if (payItem == null) {
            this.getModel().setValue("frequency", (Object)null, index);
        } else {
            String frequency = payItem.getString("frequency");
            if (StringUtils.isBlank(frequency)) {
                this.getModel().setValue("frequency", PayFrequency.A, index);
            } else {
                this.getModel().setValue("frequency", frequency, index);
            }

        }
    }

    private void setDiscountRateEnable() {
        boolean isExempt = (Boolean)this.getModel().getValue("isexempt");
        if (isExempt) {
            this.getModel().setValue("discountrate", BigDecimal.ZERO);
            this.getView().setEnable(Boolean.FALSE, new String[]{"discountrate"});
        } else {
            this.getView().setEnable(Boolean.TRUE, new String[]{"discountrate"});
        }

    }

    private void setRuleDeductibleEnable4LoadData() {
        DynamicObjectCollection ruleEntry = this.getModel().getEntryEntity("payruleentryentity");
        Iterator var2 = ruleEntry.iterator();

        while(var2.hasNext()) {
            DynamicObject row = (DynamicObject)var2.next();
            int seq = row.getInt("seq");
            String ruleInvoiceType = row.getString("rule_invoicetype");
            if (StringUtils.isNotBlank(ruleInvoiceType) && InvoiceType.COMMON.getValue().equals(ruleInvoiceType)) {
                this.getView().setEnable(Boolean.FALSE, seq - 1, new String[]{"rule_deductible"});
            }
        }

    }

    private void setLeaseTermStartDate() {
        LeaseContractCal.setLeaseTermStartDate(this.buildObjWrapper());
    }

    private void setTransitionPlan() {
        LeaseContractCal.setTransitionPlan(this.buildObjWrapper());
    }

    private int getCurrencyAmtPrecision() {
        DynamicObject currency = (DynamicObject)this.getModel().getValue("currency");
        if (currency == null) {
            QFilter qFilter = new QFilter("number" , QCP.equals,"CNY");
            DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bd_currency", "id,amtprecision", new QFilter[]{qFilter});
            // 初始化币别信息
            if(ObjectUtils.isNotEmpty(dynamicObject)){
                this.getModel().setValue("currency",dynamicObject.getPkValue());
//                this.getModel().setValue("currency", dynamicObject.getInt("amtprecision"));
                return dynamicObject.getInt("amtprecision");
            }
            throw new KDBizException(ResManager.loadKDString("获取核算组织本位币失败，请检查核算组织是否为空，或设置“会计账簿初始化”。", "LeaseContractEditPlugin_4", "fi-fa-formplugin", new Object[0]));
        } else {
            return currency.getInt("amtprecision");
        }
    }

    private void checkAssetAmountValid(PropertyChangedArgs e) {

        //去除资产数量限制

//        BigDecimal oldValue = (BigDecimal)e.getChangeSet()[0].getOldValue();
//        BigDecimal newValue = (BigDecimal)e.getChangeSet()[0].getNewValue();
//        if (newValue.compareTo(BigDecimal.ZERO) < 0) {
//            this.getView().showTipNotification(ResManager.loadKDString("“资产数量”不能小于零。", "LeaseContractEditPlugin_5", "fi-fa-formplugin", new Object[0]));
//            this.getModel().setValue("assetamount", oldValue);
//        }

    }

    private void checkRuleDeductibleValid(PropertyChangedArgs e) {
        Object oldValue = e.getChangeSet()[0].getOldValue();
        boolean newValue = (Boolean)e.getChangeSet()[0].getNewValue();
        int rowIndex = e.getChangeSet()[0].getRowIndex();
        String ruleInvoiceType = (String)this.getModel().getValue("rule_invoicetype", rowIndex);
        if (newValue && StringUtils.isNotBlank(ruleInvoiceType) && InvoiceType.COMMON.getValue().equals(ruleInvoiceType)) {
            this.getView().showTipNotification(String.format(ResManager.loadKDString("付款计划第%s行：“发票类型”为“普通发票”，不可抵扣。", "LeaseContractEditPlugin_6", "fi-fa-formplugin", new Object[0]), rowIndex + 1));
            this.getModel().setValue("rule_deductible", oldValue, rowIndex);
        }

    }

    private void clearDataIfExempt() {
        boolean isExempt = (Boolean)this.getModel().getValue("isexempt");
        if (isExempt) {
            this.getModel().deleteEntryData("payplanentryentity");
            this.getModel().setValue("leaseliabori", BigDecimal.ZERO);
            this.getModel().setValue("leaseliab", BigDecimal.ZERO);
            this.getModel().setValue("leaseassets", BigDecimal.ZERO);
            this.getModel().setValue("assetsaccumdepre", BigDecimal.ZERO);
            this.getModel().setValue("assetsaddupyeardepre", BigDecimal.ZERO);
            this.getModel().setValue("hasdepremonths", 0);
            this.getModel().setValue("accumrent", BigDecimal.ZERO);
            this.getModel().setValue("addupyearrent", BigDecimal.ZERO);
            this.getModel().setValue("accuminterest", BigDecimal.ZERO);
            this.getModel().setValue("addupyearinterest", BigDecimal.ZERO);
            this.getView().setEnable(Boolean.FALSE, EXEMPT_AFFECTED_FIELDS);
        } else {
            String sourceType = (String)this.getModel().getValue("sourcetype");
            this.getView().setEnable(LeaseContractSourceType.B.name().equals(sourceType), EXEMPT_AFFECTED_FIELDS);
        }

    }

    private void showClearBillForm() {
        DynamicObject clearBill = (DynamicObject)this.getModel().getValue("clearbill");
        if (clearBill != null) {
            FaShowFormUtils.showSingleBillForm(this.getView(), "fa_clearbill", clearBill.getPkValue(), ShowType.MainNewTabPage);
        }
    }

    private void showRenewalContractForm() {
        long renewalContractId = (Long)this.getModel().getValue("renewalcontractid");
        if (renewalContractId != 0L) {
            FaShowFormUtils.showSingleBillForm(this.getView(), "fa_lease_contract", renewalContractId, ShowType.MainNewTabPage);
        }
    }

    private void showSrcContractForm() {
        DynamicObject srcContract = (DynamicObject)this.getModel().getValue("srccontract");
        if (srcContract != null) {
            FaShowFormUtils.showSingleBillForm(this.getView(), "fa_lease_contract", srcContract.getPkValue(), ShowType.MainNewTabPage);
        }
    }

    private void showLeaseChangeBillForm(int rowIndex) {
        long leaseChangeBillId = (Long)this.getModel().getValue("leasechangebillid", rowIndex);
        FaShowFormUtils.showSingleBillForm(this.getView(), "fa_lease_change_bill", leaseChangeBillId, ShowType.MainNewTabPage, OperationStatus.VIEW);
    }

    private void showBefChangeContractForm(int rowIndex) {
        long befChangeContractId = (Long)this.getModel().getValue("befchgcontractid", rowIndex);
        this.getView().getPageCache().put("isClickBefChgContract", "true");
        FaShowFormUtils.showSingleBillForm(this.getView(), "fa_lease_contract", befChangeContractId, ShowType.MainNewTabPage, OperationStatus.VIEW);
    }

    private void setLeaseAssetsGroupVisible() {
        String transitionPlan = (String)this.getModel().getValue("transitionplan");
        boolean showLeaseAssets = LeaseUtil.isCalLeaseAssetsForPayPlan(transitionPlan);
        this.getView().setVisible(showLeaseAssets, new String[]{"leaseassetsgroup"});
    }

}
