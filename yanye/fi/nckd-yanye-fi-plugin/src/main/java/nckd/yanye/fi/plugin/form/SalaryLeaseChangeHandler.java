package nckd.yanye.fi.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicProperty;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.EntityType;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.property.EntryProp;
import kd.bos.exception.KDBizException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.business.enums.lease.*;
import kd.fi.fa.business.lease.InterestDetailGenerator4LeaseChange;
import kd.fi.fa.business.lease.RentSettleGenerator4LeaseChange;
import kd.fi.fa.business.lease.backup.LeaseContractBackupUtils;
import kd.fi.fa.business.lease.model.PayRuleCompareResult;
import kd.fi.fa.business.lease.utils.LeaseChangeUtil;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.business.utils.FaBigDecimalUtil;
import kd.fi.fa.business.utils.SystemParamHelper;
import kd.fi.fa.common.util.DateUtil;
import kd.fi.fa.common.util.Fa;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SalaryLeaseChangeHandler {
    private static final Log logger = LogFactory.getLog(SalaryLeaseChangeHandler.class);
    private static final Map<String, Set<String>> LINKAGE_FIELDS = new HashMap(4);
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private final DynamicObject[] bills;
    private final Map<Long, BackupMode> backupModeMap = new HashMap(32);
    private Map<Long, DynamicObject> contractMap = new HashMap(0);
    private Map<Long, DynamicObject> interestDetailMap = new HashMap(0);
    private final List<DynamicObject> contract4Save = new ArrayList(32);
    private final Map<Long, BigDecimal> totalAssetsAmountBefChgMap = new HashMap(32);
    private final Map<Long, BigDecimal> totalAssetsAmountAftChgMap = new HashMap(32);
    private final Map<Long, BigDecimal> effectiveDateBeginBalanceBefChgMap = new HashMap(32);
    private final Map<Long, BigDecimal> effectiveDateBeginBalanceAftChgMap = new HashMap(32);
    private final Map<Long, DynamicObject> leaseInitMap = new HashMap(32);

    public SalaryLeaseChangeHandler(DynamicObject[] bills) {
        this.bills = bills;
        this.loadContractData(bills);
        this.loadInterestDetailData(bills);
    }

    public void handle() {
        DynamicObject[] var1 = this.bills;
        int var2 = var1.length;

        int var3;
        DynamicObject bill;
        for(var3 = 0; var3 < var2; ++var3) {
            bill = var1[var3];
            this.buildBefContract(bill);
        }

        var1 = this.bills;
        var2 = var1.length;

        for(var3 = 0; var3 < var2; ++var3) {
            bill = var1[var3];
            this.doChange(bill);
        }

        this.batchUpdateData();
        var1 = this.bills;
        var2 = var1.length;

        for(var3 = 0; var3 < var2; ++var3) {
            bill = var1[var3];
            this.buildAftContract(bill);
        }

    }

    private void doChange(DynamicObject bill) {
        Set<String> changeFields = LeaseChangeUtil.getChangeFields(bill);
        DynamicObject leaseContract = bill.getDynamicObject("leasecontract");
        leaseContract = (DynamicObject)this.contractMap.get(leaseContract.getLong("id"));
        DynamicObject changeBakContract = bill.getDynamicObject("changebakcontract");
        changeBakContract = (DynamicObject)this.contractMap.get(changeBakContract.getLong("id"));
        DynamicObject befContract = bill.getDynamicObject("befcontract");
        Date effectiveDate = bill.getDate("effectivedate");
        leaseContract.set("version", this.formatDate(effectiveDate));
        leaseContract.set("isinitdata", false);
        leaseContract.set("previousbackid", befContract.getLong("id"));
        Iterator var7 = changeFields.iterator();

        while(true) {
            Set linkageFields;
            do {
                do {
                    if (!var7.hasNext()) {
                        if (LeaseChangeUtil.isEffectPayPlan(bill)) {
                            boolean isDiscountRateChanged = changeFields.contains("discountrate");
                            this.updatePayPlanAndRelatedInfo(befContract, changeBakContract, leaseContract, effectiveDate, isDiscountRateChanged);
                            boolean rentsettlestatus = LeaseUtil.isNeedRentSettle(changeBakContract);
                            if (rentsettlestatus) {
                                leaseContract.set("rentsettlestatus", LeaseContractRentSettleStatus.C.name());
                            } else {
                                leaseContract.set("rentsettlestatus", LeaseContractRentSettleStatus.B.name());
                            }
                        }

                        this.updatePlanEntrySrcContractId(befContract, leaseContract, effectiveDate);
                        this.contract4Save.add(leaseContract);
                        return;
                    }

                    String field = (String)var7.next();
                    this.copyField(changeBakContract, leaseContract, field);
                    linkageFields = (Set)LINKAGE_FIELDS.get(field);
                } while(linkageFields == null);
            } while(linkageFields.isEmpty());

            Iterator var10 = linkageFields.iterator();

            while(var10.hasNext()) {
                String linkageField = (String)var10.next();
                this.copyField(changeBakContract, leaseContract, linkageField);
            }
        }
    }

    private void copyField(DynamicObject changeBakContract, DynamicObject leaseContract, String field) {
        MainEntityType contractType = EntityMetadataCache.getDataEntityType("nckd_fa_salary_retir");
        DynamicProperty property = contractType.getProperty(field);
        if (property instanceof EntryProp) {
            this.copyEntryWithoutHisData(changeBakContract, leaseContract, field);
        } else {
            this.copyCommonField(changeBakContract, leaseContract, field);
        }

    }

    private void copyCommonField(DynamicObject fromDyn, DynamicObject toDyn, String field) {
        Object aftValue = fromDyn.get(field);
        toDyn.set(field, aftValue);
    }

    private void copyEntryWithoutHisData(DynamicObject fromDyn, DynamicObject toDyn, String field) {
        DynamicObjectCollection entry = toDyn.getDynamicObjectCollection(field);
        entry.clear();
        this.copyEntry(fromDyn, toDyn, field);
    }

    private void copyEntry(DynamicObject fromDyn, DynamicObject toDyn, String field) {
        this.copyEntry(fromDyn, toDyn, field, new ArrayList(0));
    }

    private void copyEntry(DynamicObject fromDyn, DynamicObject toDyn, String field, List<String> excludeFields) {
        if (fromDyn != null && toDyn != null) {
            if (!fromDyn.getDataEntityType().getName().equals(toDyn.getDataEntityType().getName())) {
                throw new KDBizException(String.format(ResManager.loadKDString("对象类型不一致: “被复制对象”：[%1$s], “待更新对象”：[%2$s]", "LeaseChangeHandler_1", "fi-fa-opplugin", new Object[0]), fromDyn.getDataEntityType().getName(), toDyn.getDataEntityType().getName()));
            } else {
                MainEntityType entityType = (MainEntityType)fromDyn.getDataEntityType();
                if (!(entityType.getProperty(field) instanceof EntryProp)) {
                    throw new KDBizException(String.format(ResManager.loadKDString("复制的字段[%s]不是分录类型", "LeaseChangeHandler_2", "fi-fa-opplugin", new Object[0]), field));
                } else {
                    Set<String> entryFields = ((EntityType)entityType.getAllEntities().get(field)).getFields().keySet();
                    DynamicObjectCollection fromEntry = fromDyn.getDynamicObjectCollection(field);
                    DynamicObjectCollection toEntry = toDyn.getDynamicObjectCollection(field);
                    int seq = toEntry.size() + 1;
                    Iterator var10 = fromEntry.iterator();

                    while(var10.hasNext()) {
                        DynamicObject fromRow = (DynamicObject)var10.next();
                        DynamicObject newRow = toEntry.addNew();
                        newRow.set("seq", seq++);
                        Iterator var13 = entryFields.iterator();

                        while(var13.hasNext()) {
                            String key = (String)var13.next();
                            if (!excludeFields.contains(key)) {
                                Object value = fromRow.get(key);
                                newRow.set(key, value);
                            }
                        }
                    }

                }
            }
        } else {
            throw new KDBizException(String.format(ResManager.loadKDString("“被复制对象”或“待更新对象”为空, “被复制对象”：[%1$s], “待更新对象”：[%2$s]", "LeaseChangeHandler_0", "fi-fa-opplugin", new Object[0]), fromDyn, toDyn));
        }
    }

    private void updatePayPlanAndRelatedInfo(DynamicObject befContract, DynamicObject changeBakContract, DynamicObject leaseContract, Date effectiveDate, boolean isDiscountRateChanged) {
        PayRuleCompareResult compareResult = LeaseChangeUtil.comparePayRule(befContract, changeBakContract, effectiveDate);
        this.prepareDataBefCopyPayPlan(leaseContract);
        this.copyPayPlan(changeBakContract, leaseContract, effectiveDate);
        this.calLeaseLiabAndAssetsData4PayPlan(leaseContract, effectiveDate, compareResult, isDiscountRateChanged);
        this.prepareDataAftCopyPayPlan(leaseContract);
        BackupMode backupMode = this.setBackupMode(leaseContract, compareResult, isDiscountRateChanged);
        if (backupMode == BackupMode.ALLDATA) {
            this.prepareDataBefUpdateInterestDetail(leaseContract, effectiveDate);
            this.updateInterestDetail(leaseContract, effectiveDate);
            this.prepareDataAftUpdateInterestDetail(leaseContract, effectiveDate);
            this.updateRentSettle(leaseContract);
        }

        this.updateFinInfo(leaseContract, compareResult, isDiscountRateChanged);
    }

    private void copyPayPlan(DynamicObject changeBakContract, DynamicObject leaseContract, Date effectiveDate) {
        DynamicObjectCollection chgPlanEntry = changeBakContract.getDynamicObjectCollection("payplanentryentity");
        Map<String, Date> chgNumber2ObjMap = new HashMap(chgPlanEntry.size(), 1.0F);
        Iterator var6 = chgPlanEntry.iterator();

        while(var6.hasNext()) {
            DynamicObject chgPlan = (DynamicObject)var6.next();
            DynamicObject payItem = chgPlan.getDynamicObject("plan_payitem");
            String payItemClass = payItem.getString("accountingclass");
            if ("C".equals(payItemClass)) {
                chgNumber2ObjMap.put(chgPlan.getString("plannumber"), chgPlan.getDate("plan_enddate"));
            }
        }

        DynamicObjectCollection oriPlanEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
        Map<String, Date> oriNumber2ObjMap = new HashMap(oriPlanEntry.size(), 1.0F);
        Iterator oriIterator = oriPlanEntry.iterator();

        DynamicObject row;
//        DynamicObject row;
        while(oriIterator.hasNext()) {
            row = (DynamicObject)oriIterator.next();
            row = row.getDynamicObject("plan_payitem");
            String payItemClass = row.getString("accountingclass");
            if ("C".equals(payItemClass)) {
                oriNumber2ObjMap.put(row.getString("plannumber"), row.getDate("plan_enddate"));
            }
        }

        oriIterator = oriPlanEntry.iterator();

        Date endDate;
        while(oriIterator.hasNext()) {
            row = (DynamicObject)oriIterator.next();
            Date planPayDate = row.getDate("planpaydate");
            endDate = row.getDate("plan_enddate");
            DynamicObject payItem = row.getDynamicObject("plan_payitem");
            String payItemClass = payItem.getString("accountingclass");
            if (DateUtil.compareDate(planPayDate, effectiveDate) >= 0) {
                oriIterator.remove();
            } else if ("C".contains(payItemClass)) {
                Date chgEndDate = (Date)chgNumber2ObjMap.get(row.getString("plannumber"));
                if (chgEndDate != null) {
                    String endDateStr = format.format(endDate);
                    String chgEndDateStr = format.format(chgEndDate);
                    if (!endDateStr.equals(chgEndDateStr)) {
                        oriIterator.remove();
                    }
                }
            }
        }

        Iterator<DynamicObject> chgIterator = chgPlanEntry.iterator();

        while(true) {
//            DynamicObject row;
            while(chgIterator.hasNext()) {
                row = (DynamicObject)chgIterator.next();
                endDate = row.getDate("planpaydate");
                endDate = row.getDate("plan_enddate");
                row = row.getDynamicObject("plan_payitem");
                String payItemClass = row.getString("accountingclass");
                if (DateUtil.compareDate(endDate, effectiveDate) < 0 && !"C".equals(payItemClass)) {
                    chgIterator.remove();
                } else if (DateUtil.compareDate(endDate, effectiveDate) < 0 && "C".equals(payItemClass)) {
                    Date oriEndDate = (Date)oriNumber2ObjMap.get(row.getString("plannumber"));
                    boolean toHold = false;
                    if (oriEndDate != null) {
                        String endDateStr = format.format(endDate);
                        String oriEndDateStr = format.format(oriEndDate);
                        if (!endDateStr.equals(oriEndDateStr)) {
                            toHold = true;
                        }
                    }

                    if (!toHold) {
                        chgIterator.remove();
                    }
                }
            }

            List<String> excludeFields = new ArrayList(2);
            excludeFields.add("planentrysrcid");
            excludeFields.add("contractsrcid");
            this.copyEntry(changeBakContract, leaseContract, "payplanentryentity", excludeFields);
            oriPlanEntry.sort((o1, o2) -> {
                String number1 = o1.getString("plannumber");
                String number2 = o2.getString("plannumber");
                return number1.compareTo(number2);
            });
            int seq = 1;
            Iterator var29 = oriPlanEntry.iterator();

            while(var29.hasNext()) {
                row = (DynamicObject)var29.next();
                row.set("seq", seq++);
            }

            return;
        }
    }

    private void updatePlanEntrySrcContractId(DynamicObject befContract, DynamicObject leaseContract, Date effectiveDate) {
        long leaseContractId = leaseContract.getLong("id");
        DynamicObjectCollection planEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
        Iterator var7 = planEntry.iterator();

        while(true) {
            DynamicObject row;
            long planEntrySrcContractId;
            label25:
            do {
                while(var7.hasNext()) {
                    row = (DynamicObject)var7.next();
                    Date planPayDate = row.getDate("planpaydate");
                    if (DateUtil.compareDate(planPayDate, effectiveDate) < 0) {
                        planEntrySrcContractId = row.getLong(Fa.id("contractsrcid"));
                        continue label25;
                    }

                    row.set("contractsrcid", leaseContract);
                }

                return;
            } while(planEntrySrcContractId != leaseContractId && planEntrySrcContractId != 0L);

            row.set("contractsrcid", befContract);
        }
    }

    private void calLeaseLiabAndAssetsData4PayPlan(DynamicObject leaseContract, Date effectiveDate, PayRuleCompareResult compareResult, boolean isDiscountRateChanged) {
        DynamicObject org = leaseContract.getDynamicObject("org");
        long orgId = org.getLong("id");
        DynamicObject currency = leaseContract.getDynamicObject("currency");
        int amtPrecision = currency.getInt("amtprecision");
        BigDecimal dailyDiscountRate = leaseContract.getBigDecimal("dailydiscountrate");
        BigDecimal dailyDiscountRate4Cal = dailyDiscountRate.divide(FaBigDecimalUtil.HUNDRED, 8, 4);
        String transitionPlan = leaseContract.getString("transitionplan");
        boolean isDeductVatDiscount = SystemParamHelper.getBooleanParam("is_deduct_vat_discount", orgId, false);
        Set<String> changedPayItemClass = compareResult.getChangedPayItemClass();
        DynamicObjectCollection planEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
        Iterator var16 = planEntry.iterator();

        while(true) {
            DynamicObject row;
            Date planPayDate;
            boolean deductible;
            BigDecimal rent;
            BigDecimal rentNoTax;
            String acctClass;
            do {
                DynamicObject payItem;
                do {
                    do {
                        if (!var16.hasNext()) {
                            return;
                        }

                        row = (DynamicObject)var16.next();
                        planPayDate = row.getDate("planpaydate");
                    } while(DateUtil.compareDate(planPayDate, effectiveDate) < 0);

                    payItem = row.getDynamicObject("plan_payitem");
                } while(!changedPayItemClass.contains("A") && !isDiscountRateChanged);

                deductible = row.getBoolean("plan_deductible");
                rent = row.getBigDecimal("rent");
                rentNoTax = row.getBigDecimal("rentnotax");
                BigDecimal unpaidRent = row.getBigDecimal("unpaidrent");
                int liabDiscountDays = 0;
                acctClass = payItem.getString("accountingclass");
                if ("A".equals(acctClass) && DateUtil.compareDate(planPayDate, effectiveDate) >= 0) {
                    liabDiscountDays = DateUtil.getDiffDays(effectiveDate, planPayDate);
                }

                row.set("discountdays", liabDiscountDays);
                BigDecimal liabDiscountFactor = BigDecimal.ONE.add(dailyDiscountRate4Cal).pow(liabDiscountDays).setScale(6, 4);
                row.set("discountfactor", liabDiscountFactor);
                BigDecimal liabPresentValue = BigDecimal.ZERO;
                if ("A".equals(acctClass)) {
                    liabPresentValue = unpaidRent.divide(liabDiscountFactor, amtPrecision, LeaseUtil.getRoundingMode4CalAmount());
                }

                row.set("presentvalue", liabPresentValue);
            } while(!TransitionPlan.A.name().equals(transitionPlan) && !TransitionPlan.C.name().equals(transitionPlan));

            int assetsDiscountDays = 0;
            if ("A".equals(acctClass) && DateUtil.compareDate(planPayDate, effectiveDate) >= 0) {
                assetsDiscountDays = DateUtil.getDiffDays(effectiveDate, planPayDate);
            }

            row.set("discountdays2", assetsDiscountDays);
            BigDecimal assetsDiscountFactor = BigDecimal.ONE.add(dailyDiscountRate4Cal).pow(assetsDiscountDays).setScale(6, 4);
            row.set("discountfactor2", assetsDiscountFactor);
            BigDecimal assetsPresentValue = BigDecimal.ZERO;
            if ("A".equals(acctClass)) {
                BigDecimal amountTemp = deductible && !isDeductVatDiscount ? rentNoTax : rent;
                assetsPresentValue = amountTemp.divide(assetsDiscountFactor, amtPrecision, LeaseUtil.getRoundingMode4CalAmount());
            }

            row.set("presentvalue2", assetsPresentValue);
        }
    }

    private void prepareDataBefCopyPayPlan(DynamicObject leaseContract) {
        long leaseContractId = leaseContract.getLong("id");
        this.totalAssetsAmountBefChgMap.put(leaseContractId, this.calTotalAssetsAmount(leaseContract));
    }

    private void prepareDataAftCopyPayPlan(DynamicObject leaseContract) {
        long leaseContractId = leaseContract.getLong("id");
        this.totalAssetsAmountAftChgMap.put(leaseContractId, this.calTotalAssetsAmount(leaseContract));
    }

    private BigDecimal calTotalAssetsAmount(DynamicObject leaseContract) {
        DynamicObjectCollection planEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
        BigDecimal total = BigDecimal.ZERO;
        Iterator var4 = planEntry.iterator();

        while(var4.hasNext()) {
            DynamicObject row = (DynamicObject)var4.next();
            DynamicObject payItem = row.getDynamicObject("plan_payitem");
            String acctClass = payItem.getString("accountingclass");
            if ("B".equals(acctClass)) {
                boolean deductible = row.getBoolean("plan_deductible");
                BigDecimal rentNoTax;
                if (deductible) {
                    rentNoTax = row.getBigDecimal("rentnotax");
                    total = total.add(rentNoTax);
                } else {
                    rentNoTax = row.getBigDecimal("rent");
                    total = total.add(rentNoTax);
                }
            }
        }

        return total;
    }

    private BackupMode setBackupMode(DynamicObject leaseContract, PayRuleCompareResult compareResult, boolean isDiscountRateChanged) {
        long contractId = leaseContract.getLong("id");
        if (isDiscountRateChanged) {
            this.backupModeMap.put(contractId, BackupMode.ALLDATA);
            return BackupMode.ALLDATA;
        } else {
            Set<String> changedPayItemClass = compareResult.getChangedPayItemClass();
            if (changedPayItemClass.contains("A")) {
                this.backupModeMap.put(contractId, BackupMode.ALLDATA);
                return BackupMode.ALLDATA;
            } else {
                this.backupModeMap.put(contractId, BackupMode.BASICDATA);
                return BackupMode.BASICDATA;
            }
        }
    }

    private void updateInterestDetail(DynamicObject leaseContract, Date effectiveDate) {
        long leaseContractId = leaseContract.getLong("id");
        DynamicObject interestDetail = (DynamicObject)this.interestDetailMap.get(leaseContractId);
        DynamicObjectCollection entry;
        if (interestDetail != null) {
            boolean hisPeriodEffective = this.isHisPeriodEffective(leaseContract, effectiveDate);
            if (hisPeriodEffective) {
                this.processInterestDetailIfHisPeriodEffective(leaseContract, effectiveDate, interestDetail);
            }

            entry = interestDetail.getDynamicObjectCollection("detailentry");
            Iterator<DynamicObject> iterator = entry.iterator();

            while(iterator.hasNext()) {
                DynamicObject row = (DynamicObject)iterator.next();
                Date date = row.getDate("date");
                boolean latestData = row.getBoolean("latestdata");
                if (DateUtil.compareDate(date, effectiveDate) >= 0 && latestData) {
                    iterator.remove();
                }
            }
        }

        DynamicObject tempInterestDetail = (new InterestDetailGenerator4LeaseChange(leaseContract, effectiveDate)).generate4LeaseChange();
        if (interestDetail != null) {
            this.copyEntry(tempInterestDetail, interestDetail, "detailentry");
        } else {
            interestDetail = tempInterestDetail;
            tempInterestDetail.set("leasecontract", leaseContract);
            this.interestDetailMap.put(leaseContractId, tempInterestDetail);
        }

        entry = interestDetail.getDynamicObjectCollection("detailentry");
        entry.sort((o1, o2) -> {
            String contractVersion1 = o1.getString("contractversion");
            String contractVersion2 = o2.getString("contractversion");
            if (!contractVersion1.equals(contractVersion2)) {
                return contractVersion1.compareTo(contractVersion2);
            } else {
                String sourceType1 = o1.getString("sourcetype");
                String sourceType2 = o2.getString("sourcetype");
                if (!sourceType1.equals(sourceType2)) {
                    return sourceType2.compareTo(sourceType1);
                } else {
                    Date date1 = o1.getDate("date");
                    Date date2 = o2.getDate("date");
                    return DateUtil.compareDate(date1, date2);
                }
            }
        });
        int seq = 1;
        Iterator var15 = entry.iterator();

        while(var15.hasNext()) {
            DynamicObject row = (DynamicObject)var15.next();
            row.set("seq", seq++);
        }

    }

    private void processInterestDetailIfHisPeriodEffective(DynamicObject leaseContract, Date effectiveDate, DynamicObject interestDetail) {
        String aftChgVersion = leaseContract.getString("version");
        DynamicObject leaseInit = this.getLeaseInitFromLocalCache(leaseContract);
        DynamicObject curPeriod = leaseInit.getDynamicObject("curperiod");
        Date curPeriodBeginDate = curPeriod.getDate("begindate");
        DynamicObjectCollection detailEntry = interestDetail.getDynamicObjectCollection("detailentry");
        if (DateUtil.compareDate(effectiveDate, curPeriodBeginDate) >= 0) {
            throw new KDBizException(String.format(ResManager.loadKDString("生效日不在历史期间，无需冲销计息明细，生效日[%1$s]，当期开始日[%2$s]。", "LeaseChangeHandler_3", "fi-fa-opplugin", new Object[0]), effectiveDate, curPeriodBeginDate));
        } else {
            int size = detailEntry.size();

            for(int i = 0; i < size; ++i) {
                DynamicObject row = (DynamicObject)detailEntry.get(i);
                Date date = row.getDate("date");
                if (DateUtil.compareDate(date, effectiveDate) >= 0 && DateUtil.compareDate(date, curPeriodBeginDate) < 0) {
                    boolean latestData = row.getBoolean("latestdata");
                    if (latestData) {
                        row.set("latestdata", Boolean.FALSE);
                        BigDecimal beginBalance = row.getBigDecimal("beginbalance");
                        BigDecimal leaseLiabPay = row.getBigDecimal("leaseliabpay");
                        BigDecimal leaseLiabInt = row.getBigDecimal("leaseliabint");
                        BigDecimal endBalance = row.getBigDecimal("endbalance");
                        BigDecimal realDailyRate = row.getBigDecimal("realdailyrate");
                        DynamicObject reverseData = detailEntry.addNew();
                        reverseData.set("date", date);
                        reverseData.set("beginbalance", beginBalance.negate());
                        reverseData.set("leaseliabpay", leaseLiabPay.negate());
                        reverseData.set("leaseliabint", leaseLiabInt.negate());
                        reverseData.set("endbalance", endBalance.negate());
                        reverseData.set("realdailyrate", realDailyRate);
                        reverseData.set("amortizationperiod", curPeriod);
                        reverseData.set("sourcetype", InterestDetailSourceType.C.name());
                        reverseData.set("latestdata", Boolean.FALSE);
                        reverseData.set("contractversion", aftChgVersion);
                    }
                }
            }

        }
    }

    private boolean isHisPeriodEffective(DynamicObject leaseContract, Date effectiveDate) {
        DynamicObject leaseInit = this.getLeaseInitFromLocalCache(leaseContract);
        DynamicObject curPeriod = leaseInit.getDynamicObject("curperiod");
        Date beginDate = curPeriod.getDate("begindate");
        return DateUtil.compareDate(effectiveDate, beginDate) < 0;
    }

    private void prepareDataBefUpdateInterestDetail(DynamicObject leaseContract, Date effectiveDate) {
        long contractId = leaseContract.getLong("id");
        this.effectiveDateBeginBalanceBefChgMap.put(contractId, this.getEffectiveDateBeginBalance(contractId, effectiveDate));
    }

    private void prepareDataAftUpdateInterestDetail(DynamicObject leaseContract, Date effectiveDate) {
        long contractId = leaseContract.getLong("id");
        this.effectiveDateBeginBalanceAftChgMap.put(contractId, this.getEffectiveDateBeginBalance(contractId, effectiveDate));
    }

    private BigDecimal getEffectiveDateBeginBalance(long leaseContractId, Date effectiveDate) {
        DynamicObject intDetail = (DynamicObject)this.interestDetailMap.get(leaseContractId);
        if (intDetail == null) {
            logger.info(String.format("getEffectiveDateBeginBalance -> 未找到计息明细数据，contractId：%s", leaseContractId));
            return BigDecimal.ZERO;
        } else {
            DynamicObjectCollection detailEntry = intDetail.getDynamicObjectCollection("detailentry");
            Iterator var6 = detailEntry.iterator();

            DynamicObject row;
            Date date;
            boolean latestData;
            do {
                if (!var6.hasNext()) {
                    logger.info(String.format("存在计息明细数据，但是没有变更生效日当天的分录行，contractId：%s", leaseContractId));
                    return BigDecimal.ZERO;
                }

                row = (DynamicObject)var6.next();
                date = row.getDate("date");
                latestData = row.getBoolean("latestdata");
            } while(DateUtil.compareDate(date, effectiveDate) != 0 || !latestData);

            return row.getBigDecimal("beginbalance");
        }
    }

    private void updateFinInfo(DynamicObject leaseContract, PayRuleCompareResult compareResult, boolean isDiscountRateChanged) {
        boolean isExempt = leaseContract.getBoolean("isexempt");
        if (!isExempt) {
            Set<String> changedPayItemClass = compareResult.getChangedPayItemClass();
            if (isDiscountRateChanged || changedPayItemClass.size() != 1 || !changedPayItemClass.contains("C")) {
                String sourceType = leaseContract.getString("sourcetype");
//                Date sysswitchdate = leaseContract.getDate("sysswitchdate");
                DynamicObjectCollection planEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
                BigDecimal leaseLiabOri = BigDecimal.ZERO;
                Iterator var10 = planEntry.iterator();

                while(true) {
                    DynamicObject row;
                    Date planPayDate;
                    BigDecimal unPaidRent;
                    do {
                        if (!var10.hasNext()) {
                            long leaseContractId = leaseContract.getLong("id");
                            BigDecimal leaseLiab = leaseContract.getBigDecimal("leaseliab");
                            unPaidRent = (BigDecimal)this.effectiveDateBeginBalanceAftChgMap.getOrDefault(leaseContractId, BigDecimal.ZERO);
                            BigDecimal beginBalanceBefChg = (BigDecimal)this.effectiveDateBeginBalanceBefChgMap.getOrDefault(leaseContractId, BigDecimal.ZERO);
                            leaseLiab = leaseLiab.add(unPaidRent.subtract(beginBalanceBefChg));
                            BigDecimal leaseAssets = leaseContract.getBigDecimal("leaseassets");
                            BigDecimal totalAmountAftChg = (BigDecimal)this.totalAssetsAmountAftChgMap.get(leaseContractId);
                            BigDecimal totalAmountBefChg = (BigDecimal)this.totalAssetsAmountBefChgMap.get(leaseContractId);
                            leaseAssets = leaseAssets.add(unPaidRent.subtract(beginBalanceBefChg)).add(totalAmountAftChg.subtract(totalAmountBefChg));
                            if (isDiscountRateChanged || changedPayItemClass.contains("A")) {
                                leaseContract.set("leaseliabori", leaseLiabOri);
                                leaseContract.set("leaseliab", leaseLiab);
                            }

                            if (isDiscountRateChanged || changedPayItemClass.contains("A") || changedPayItemClass.contains("B")) {
                                leaseContract.set("leaseassets", leaseAssets);
                            }

                            return;
                        }

                        row = (DynamicObject)var10.next();
                        planPayDate = row.getDate("planpaydate");
                    } while(LeaseContractSourceType.B.name().equals(sourceType) );

                    unPaidRent = row.getBigDecimal("unpaidrent");
                    leaseLiabOri = leaseLiabOri.add(unPaidRent);
                }
            }
        }
    }

    private void updateRentSettle(DynamicObject leaseContract) {
        DynamicObject interestDetail = (DynamicObject)this.interestDetailMap.get(leaseContract.getLong("id"));
        DynamicObject leaseInit = this.getLeaseInitFromLocalCache(leaseContract);
        long curPeriodId = leaseInit.getLong(Fa.id("curperiod"));
        (new RentSettleGenerator4LeaseChange(leaseContract, interestDetail, curPeriodId)).regenerate();
    }

    private void buildBefContract(DynamicObject bill) {
        DynamicObject befContract = bill.getDynamicObject("befcontract");
        if (befContract == null) {
            DynamicObject leaseContract = bill.getDynamicObject("leasecontract");
            leaseContract = (DynamicObject)this.contractMap.get(leaseContract.getLong("id"));
            QFilter[] filters = new QFilter[]{new QFilter("masterid", "=", leaseContract.getLong("masterid")), new QFilter("version", "=", leaseContract.getString("version")), new QFilter("isbak", "=", true)};
            String selectFields = Fa.comma(new String[]{"plan_payitem", "plannumber", "plan_startdate", "leaseassets", "depremonths", "plan_enddate", "planpaydate", "plan_invoicetype", "plan_deductible", "plan_taxrate", "rentnotax", "tax", "rent", "unpaidrent"});
            befContract = BusinessDataServiceHelper.loadSingleFromCache("nckd_fa_salary_retir", selectFields, filters);
            if (befContract == null) {
                befContract = LeaseContractBackupUtils.backup(leaseContract.getLong("id"), BackupMode.ALLDATA);
            }

            bill.set("befcontract", befContract);
        }
    }

    private void buildAftContract(DynamicObject bill) {
        DynamicObject leaseContract = bill.getDynamicObject("leasecontract");
        long contractId = leaseContract.getLong("id");
        BackupMode backupMode = (BackupMode)this.backupModeMap.getOrDefault(contractId, BackupMode.BASICDATA);
        DynamicObject aftContract = LeaseContractBackupUtils.backup(contractId, backupMode);
        bill.set("aftcontract", aftContract);
    }

    private void batchUpdateData() {
        Object[] saveContractResult = SaveServiceHelper.save((DynamicObject[])this.contract4Save.toArray(new DynamicObject[0]));
        if (saveContractResult.length != this.contract4Save.size()) {
            throw new KDBizException(String.format(ResManager.loadKDString("保存变更后合同失败，待保存[%1$s]条，实际保存[%2$s]条。", "LeaseChangeHandler_4", "fi-fa-opplugin", new Object[0]), this.contract4Save.size(), saveContractResult.length));
        } else {
            Object[] saveInterestDetailResult = SaveServiceHelper.save((DynamicObject[])this.interestDetailMap.values().toArray(new DynamicObject[0]));
            if (saveInterestDetailResult.length != this.interestDetailMap.size()) {
                throw new KDBizException(String.format(ResManager.loadKDString("保存变更后计息明细表失败，待保存[%1$s]条，实际保存[%2$s]条。", "LeaseChangeHandler_5", "fi-fa-opplugin", new Object[0]), this.interestDetailMap.size(), saveInterestDetailResult.length));
            }
        }
    }

    private DynamicObject getLeaseInitFromLocalCache(DynamicObject leaseContract) {
        DynamicObject org = leaseContract.getDynamicObject("org");
        long orgId = org.getLong("id");
        DynamicObject leaseInit = (DynamicObject)this.leaseInitMap.get(orgId);
        if (leaseInit == null) {
            leaseInit = BusinessDataServiceHelper.loadSingle("gl_accountbook", "curperiod", (new QFilter("org", "=", orgId)).toArray());
            if (leaseInit == null) {
                throw new KDBizException(String.format(ResManager.loadKDString("未找到核算组织[%s]的租赁初始化数据。", "LeaseChangeHandler_6", "fi-fa-opplugin", new Object[0]), org.getString("number")));
            }

            this.leaseInitMap.put(orgId, leaseInit);
        }

        return leaseInit;
    }

    private void loadContractData(DynamicObject[] bills) {
        List<Long> contractIds = new ArrayList(bills.length);
        DynamicObject[] contractArr = bills;
        int var4 = bills.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject bill = contractArr[var5];
            DynamicObject leaseContract = bill.getDynamicObject("leasecontract");
            if (leaseContract != null) {
                contractIds.add(leaseContract.getLong("id"));
            }

            DynamicObject changeBakContract = bill.getDynamicObject("changebakcontract");
            if (changeBakContract != null) {
                contractIds.add(changeBakContract.getLong("id"));
            }

            DynamicObject befContract = bill.getDynamicObject("befcontract");
            if (befContract != null) {
                contractIds.add(befContract.getLong("id"));
            }

            DynamicObject aftContract = bill.getDynamicObject("aftcontract");
            if (aftContract != null) {
                contractIds.add(aftContract.getLong("id"));
            }
        }

        contractArr = BusinessDataServiceHelper.load(contractIds.toArray(new Long[0]), EntityMetadataCache.getDataEntityType("nckd_fa_salary_retir"));
        this.contractMap = (Map)Arrays.stream(contractArr).collect(Collectors.toMap((v) -> {
            return v.getLong("id");
        }, (v) -> {
            return v;
        }));
    }

    private void loadInterestDetailData(DynamicObject[] bills) {
        List<Long> contractIds = new ArrayList(bills.length);
        DynamicObject[] var3 = bills;
        int var4 = bills.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject bill = var3[var5];
            DynamicObject leaseContract = bill.getDynamicObject("leasecontract");
            long leaseContractId = leaseContract.getLong("id");
            contractIds.add(leaseContractId);
        }

        QFilter[] filters = new QFilter[]{new QFilter("leasecontract", "in", contractIds)};
        DynamicObjectCollection intDetailsOnlyPk = QueryServiceHelper.query("nckd_fa_salary_retir", "id", filters);
        Object[] intDetailIds = intDetailsOnlyPk.stream().map((v) -> {
            return v.getLong("id");
        }).toArray();
        DynamicObject[] intDetails = BusinessDataServiceHelper.load(intDetailIds, EntityMetadataCache.getDataEntityType("fa_interest_detail"));
        this.interestDetailMap = (Map)Arrays.stream(intDetails).collect(Collectors.toMap((v) -> {
            return v.getLong(Fa.id("leasecontract"));
        }, (v) -> {
            return v;
        }));
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            return dateFormat.format(date);
        }
    }

    static {
        LINKAGE_FIELDS.put("discountrate", new HashSet<String>() {
            {
                this.add("dailydiscountrate");
            }
        });
        LINKAGE_FIELDS.put("leaseenddate", new HashSet<String>() {
            {
                this.add("leasemonths");
                this.add("depremonths");
            }
        });
    }
}
