package nckd.yanye.fi.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.validate.BillStatus;
import kd.bos.exception.KDBizException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.fa.business.enums.lease.InterestAmortizeSchema;
import kd.fi.fa.business.enums.lease.InterestDetailSourceType;
import kd.fi.fa.business.enums.lease.LeaseContractSourceType;
import kd.fi.fa.business.enums.lease.PayFrequency;
import kd.fi.fa.business.lease.RealDailyIntRateCalculator;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.business.utils.FaBigDecimalUtil;
import kd.fi.fa.business.utils.FaFindPeriodHelper;
import kd.fi.fa.business.utils.SystemParamHelper;
import kd.fi.fa.common.util.DateUtil;
import kd.fi.fa.common.util.Fa;
import kd.fi.fa.po.GenInterestDetailParamPo;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * Module           :财务云-租赁管理-退养人员工资
 * Description      :构建退养人员生成摊销与计息数据
 * @author guozhiwei
 * @date  2024/8/5 15:18
 *  标识:nckd_fa_salary_retir
 *
 */


public class SalaryInterstDetailGenerator {
    private static final Log logger = LogFactory.getLog(SalaryInterstDetailGenerator.class);
    private static final String DATE_FORMAT = "yyyyMMdd";
    private final List<GenInterestDetailParamPo> paramPos;
    private Map<Object, DynamicObject> interestDetailMap;
    private final Map<Long, DynamicObject> leaseInitMap = new HashMap(32);
    private final Map<Long, FaFindPeriodHelper> findPeriodHelperMap = new HashMap(32);

    public SalaryInterstDetailGenerator(List<GenInterestDetailParamPo> paramPos) {
        this.paramPos = paramPos;
    }

    public List<DynamicObject> generate() {
        Object[] leaseContractIds = this.paramPos.stream().map(GenInterestDetailParamPo::getLeaseContractId).toArray();
        Map<Object, DynamicObject> leaseContractMap = BusinessDataServiceHelper.loadFromCache(leaseContractIds, "nckd_fa_salary_retir");
        List<DynamicObject> interestDetailList = new ArrayList(this.paramPos.size());
        Iterator var4 = this.paramPos.iterator();

        while(var4.hasNext()) {
            GenInterestDetailParamPo param = (GenInterestDetailParamPo)var4.next();
            DynamicObject leaseContract = (DynamicObject)leaseContractMap.get(param.getLeaseContractId());
            interestDetailList.add(this.generateByLeaseContract(leaseContract));
        }

        return interestDetailList;
    }

    public List<DynamicObject> reverse() {
        Object[] leaseContractIds = this.paramPos.stream().map(GenInterestDetailParamPo::getLeaseContractId).toArray();
        String selectFields = Fa.comma(new String[]{"org", "leasecontract", Fa.dot(new String[]{"detailentry", "seq"}), Fa.dot(new String[]{"detailentry", "date"}), Fa.dot(new String[]{"detailentry", "leaseliabpay"}), Fa.dot(new String[]{"detailentry", "beginbalance"}), Fa.dot(new String[]{"detailentry", "leaseliabint"}), Fa.dot(new String[]{"detailentry", "endbalance"}), Fa.dot(new String[]{"detailentry", "realdailyrate"}), Fa.dot(new String[]{"detailentry", "sourcetype"}), Fa.dot(new String[]{"detailentry", "latestdata"}), Fa.dot(new String[]{"detailentry", "amortizationperiod"}), Fa.dot(new String[]{"detailentry", "contractversion"}), "dailyrate", "currency"});
        QFilter filter = new QFilter("leasecontract", "in", leaseContractIds);
        DynamicObject[] interestDetails = BusinessDataServiceHelper.load("fa_interest_detail", selectFields, filter.toArray());
        this.interestDetailMap = (Map) Stream.of(interestDetails).collect(Collectors.toMap((v) -> {
            return v.getDynamicObject("leasecontract").getPkValue();
        }, (v) -> {
            return v;
        }));
        Iterator var5 = this.paramPos.iterator();

        while(var5.hasNext()) {
            GenInterestDetailParamPo param = (GenInterestDetailParamPo)var5.next();
            this.generateReversal(param);
        }

        return new ArrayList(this.interestDetailMap.values());
    }

    protected DynamicObject generateByLeaseContract(DynamicObject leaseContract) {
        boolean isExempt = leaseContract.getBoolean("isexempt");
        return isExempt ? this.generateForExemptContract(leaseContract) : this.generateForNotExemptContract(leaseContract);
    }

    private DynamicObject generateForNotExemptContract(DynamicObject leaseContract) {
        DynamicObject org = leaseContract.getDynamicObject("org");
        String contractVersion = leaseContract.getString("version");
        DynamicObject currency = leaseContract.getDynamicObject("currency");
        int amtPrecision = currency.getInt("amtprecision");
        Map<String, BigDecimal> amountMap = new HashMap(128);
        this.getLeaseLiabPayAmountMap(leaseContract, amountMap);
        long orgId = org.getLong("id");
//        Date curPeriodEndDate = leaseContract.getDate("leaseenddate");
        DynamicObject leaseInit = this.getLeaseInitFromLocalCache(orgId);
        DynamicObject curPeriod = leaseInit.getDynamicObject("curperiod");

        Date curPeriodEndDate = curPeriod.getDate("enddate");

        FaFindPeriodHelper findPeriodHelper = this.getFindPeriodHelperFromLocalCache(orgId);

        DynamicObject interestDetail = this.createInterestDetail();
        interestDetail.set("org", org);
        interestDetail.set("leasecontract", leaseContract);
        interestDetail.set("currency", currency);
        DynamicObjectCollection detailEntry = interestDetail.getDynamicObjectCollection("detailentry");
        detailEntry.clear();
        Date detailBeginDate = this.getDetailBeginDate(leaseContract);
        Date detailEndDate = this.getDetailEndDate(leaseContract, amountMap);
        BigDecimal beginBalance = this.getDetailBeginBalance(leaseContract);
        BigDecimal dailyRate = this.getDailyRate(leaseContract, amountMap);
        BigDecimal dailyRate4Show = dailyRate.multiply(FaBigDecimalUtil.HUNDRED).setScale(6, LeaseUtil.getRoundingMode4CalAmount());
        interestDetail.set("dailyrate", dailyRate4Show);
        BigDecimal sumInterest = BigDecimal.ZERO;
        BigDecimal notConfirmFinCost = this.getNotConfirmFinCost(leaseContract);
        String dealTailDiffDate = this.getDealTailDiffDate(amountMap);
        int seq = 1;

        for(Date rowDate = detailBeginDate; DateUtil.compareDate(rowDate, detailEndDate) <= 0; rowDate = DateUtil.addDay(rowDate, 1)) {
            DynamicObject row = detailEntry.addNew();
            row.set("seq", seq++);
            row.set("date", rowDate);
            row.set("beginbalance", beginBalance);
            String dateStr = this.formatDate(rowDate);
            BigDecimal leaseLiabPay = (BigDecimal)amountMap.get(dateStr);
            leaseLiabPay = leaseLiabPay == null ? BigDecimal.ZERO : leaseLiabPay;
            row.set("leaseliabpay", leaseLiabPay);
            BigDecimal leaseLiabInt;
            if (dealTailDiffDate.equals(this.formatDate(rowDate))) {
                leaseLiabInt = notConfirmFinCost.subtract(sumInterest);
            } else {
                leaseLiabInt = beginBalance.subtract(leaseLiabPay).multiply(dailyRate);
            }

            leaseLiabInt = leaseLiabInt.setScale(amtPrecision, LeaseUtil.getRoundingMode4CalAmount());
            row.set("leaseliabint", leaseLiabInt);
            sumInterest = sumInterest.add(leaseLiabInt);
            BigDecimal endBalance = beginBalance.subtract(leaseLiabPay).add(leaseLiabInt);
            row.set("endbalance", endBalance);
            if (BigDecimal.ZERO.compareTo(leaseLiabInt) == 0) {
                row.set("realdailyrate", BigDecimal.ZERO);
            } else {
                row.set("realdailyrate", dailyRate4Show);
            }

            DynamicObject amortizationPeriod;
            if (DateUtil.compareDate(rowDate, curPeriodEndDate) <= 0) {
                amortizationPeriod = curPeriod;
            } else {
                amortizationPeriod = findPeriodHelper.findPeriodObjByDate(rowDate);
                if (amortizationPeriod == null) {
                    throw new KDBizException(String.format(ResManager.loadKDString("租赁结束日/最晚计划付款日[%s]对应会计期间未维护，请先维护对应会计期间。", "InterestDetailGenerator_0", "fi-fa-business", new Object[0]), DateUtil.formatToString(detailEndDate)));
                }
            }
//            amortizationPeriod = findPeriodHelper.findPeriodObjByDate(rowDate);
//            if (amortizationPeriod == null) {
//                throw new KDBizException(String.format(ResManager.loadKDString("租赁结束日/最晚计划付款日[%s]对应会计期间未维护，请先维护对应会计期间。", "InterestDetailGenerator_0", "fi-fa-business", new Object[0]), DateUtil.formatToString(detailEndDate)));
//            }
//            if (amortizationPeriod == null) {
//                throw new KDBizException(String.format(ResManager.loadKDString("租赁结束日/最晚计划付款日[%s]对应会计期间未维护，请先维护对应会计期间。", "InterestDetailGenerator_0", "fi-fa-business", new Object[0]), DateUtil.formatToString(detailEndDate)));
//            }
//
            row.set("amortizationperiod", amortizationPeriod);
            row.set("sourcetype", InterestDetailSourceType.A.name());
            row.set("latestdata", Boolean.TRUE);
            row.set("contractversion", contractVersion);
            beginBalance = endBalance;
        }

        return interestDetail;
    }

    private DynamicObject generateForExemptContract(DynamicObject leaseContract) {
        DynamicObject org = leaseContract.getDynamicObject("org");
        String contractVersion = leaseContract.getString("version");
        DynamicObject currency = leaseContract.getDynamicObject("currency");
        Map<String, BigDecimal> amountMap = new HashMap(128);
        this.getLeaseLiabPayAmountMap(leaseContract, amountMap);
        long orgId = org.getLong("id");
//        Date curPeriodEndDate = leaseContract.getDate("leaseenddate");
        DynamicObject leaseInit = this.getLeaseInitFromLocalCache(orgId);
        DynamicObject curPeriod = leaseInit.getDynamicObject("curperiod");
        Date curPeriodEndDate = curPeriod.getDate("enddate");

        FaFindPeriodHelper findPeriodHelper = this.getFindPeriodHelperFromLocalCache(orgId);
        DynamicObject interestDetail = this.createInterestDetail();
        interestDetail.set("org", org);
        interestDetail.set("leasecontract", leaseContract);
        interestDetail.set("currency", currency);
        DynamicObjectCollection detailEntry = interestDetail.getDynamicObjectCollection("detailentry");
        detailEntry.clear();
        Date detailBeginDate = this.getDetailBeginDate(leaseContract);
        // 生成计息明细
        Date detailEndDate = this.getDetailEndDate(leaseContract, amountMap);
        interestDetail.set("dailyrate", BigDecimal.ZERO);
        int seq = 1;

        for(Date rowDate = detailBeginDate; DateUtil.compareDate(rowDate, detailEndDate) <= 0; rowDate = DateUtil.addDay(rowDate, 1)) {
            String dateStr = this.formatDate(rowDate);
            if (amountMap.containsKey(dateStr)) {
                DynamicObject row = detailEntry.addNew();
                row.set("seq", seq++);
                row.set("date", rowDate);
                row.set("beginbalance", BigDecimal.ZERO);
                BigDecimal leaseLiabPay = (BigDecimal)amountMap.get(dateStr);
                leaseLiabPay = leaseLiabPay == null ? BigDecimal.ZERO : leaseLiabPay;
                row.set("leaseliabpay", leaseLiabPay);
                row.set("leaseliabint", BigDecimal.ZERO);
                row.set("endbalance", BigDecimal.ZERO);
                row.set("realdailyrate", BigDecimal.ZERO);
                DynamicObject amortizationPeriod;
                if (DateUtil.compareDate(rowDate, curPeriodEndDate) <= 0) {
                    amortizationPeriod = curPeriod;
                } else {
                    amortizationPeriod = findPeriodHelper.findPeriodObjByDate(rowDate);
                    if (amortizationPeriod == null) {
                        throw new KDBizException(String.format(ResManager.loadKDString("租赁结束日/最晚计划付款日[%s]对应会计期间未维护，请先维护对应会计期间。", "InterestDetailGenerator_0", "fi-fa-business", new Object[0]), DateUtil.formatToString(detailEndDate)));
                    }
                }
                amortizationPeriod = findPeriodHelper.findPeriodObjByDate(rowDate);
                if (amortizationPeriod == null) {
                    throw new KDBizException(String.format(ResManager.loadKDString("租赁结束日/最晚计划付款日[%s]对应会计期间未维护，请先维护对应会计期间。", "InterestDetailGenerator_0", "fi-fa-business", new Object[0]), DateUtil.formatToString(detailEndDate)));
                }

                row.set("amortizationperiod", amortizationPeriod);
                row.set("sourcetype", InterestDetailSourceType.A.name());
                row.set("latestdata", Boolean.TRUE);
                row.set("contractversion", contractVersion);
            }
        }

        return interestDetail;
    }

    private DynamicObject createInterestDetail() {
        DynamicObject interestDetail = BusinessDataServiceHelper.newDynamicObject("fa_interest_detail");
        interestDetail.set("billstatus", BillStatus.C);
        return interestDetail;
    }

    protected BigDecimal getDailyRate(DynamicObject leaseContract, Map<String, BigDecimal> amountMap) {
        DynamicObject org = leaseContract.getDynamicObject("org");
        String interestAmortizeSchema = SystemParamHelper.getStringParam("interest_amortize_schema", (Long)org.getPkValue(), InterestAmortizeSchema.A.name());
        String sourceType = leaseContract.getString("sourcetype");
        if (InterestAmortizeSchema.A.name().equals(interestAmortizeSchema) && LeaseContractSourceType.A.name().equals(sourceType)) {
            BigDecimal dailyDiscountRate = leaseContract.getBigDecimal("dailydiscountrate");
            return dailyDiscountRate.divide(FaBigDecimalUtil.HUNDRED, 8, LeaseUtil.getRoundingMode4CalAmount());
        } else {
            return (new RealDailyIntRateCalculator(leaseContract, this.getDetailBeginDate(leaseContract), amountMap)).calculate();
        }
    }

    private String getDealTailDiffDate(Map<String, BigDecimal> amountMap) {
        if (amountMap.isEmpty()) {
            logger.info("amountMap为空，无需处理尾差，返回空串。");
            return "";
        } else {
            String lastPayDateStr = (String)amountMap.keySet().stream().max(Comparator.naturalOrder()).get();

            Date lastPayDate;
            try {
                lastPayDate = (new SimpleDateFormat("yyyyMMdd")).parse(lastPayDateStr);
            } catch (ParseException var5) {
                throw new KDBizException(String.format(ResManager.loadKDString("转换处理尾差的日期时出现错误“%s”。", "InterestDetailGenerator_1", "fi-fa-business", new Object[0]), lastPayDateStr));
            }

            Date dealTailDate = DateUtil.addDay(lastPayDate, -1);
            return this.formatDate(dealTailDate);
        }
    }

    private boolean getLeaseLiabPayAmountMap(DynamicObject leaseContract, Map<String, BigDecimal> allAmountMap) {
        boolean sameDateExistedMultiPay = false;
        DynamicObject org = leaseContract.getDynamicObject("org");
        DynamicObject currency = leaseContract.getDynamicObject("currency");
        int amtPrecision = currency.getInt("amtprecision");
        String interestAmortizeSchema = SystemParamHelper.getStringParam("interest_amortize_schema", (Long)org.getPkValue(), InterestAmortizeSchema.A.name());
        DynamicObjectCollection planEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
        Date detailBeginDate = this.getDetailBeginDate(leaseContract);
        Iterator var10 = planEntry.iterator();

        while(true) {
            while(true) {
                while(true) {
                    DynamicObject row;
                    DynamicObject payItem;
                    Date planPayDate;
                    do {
                        String acctClass;
                        do {
                            if (!var10.hasNext()) {
                                return sameDateExistedMultiPay;
                            }

                            row = (DynamicObject)var10.next();
                            payItem = row.getDynamicObject("plan_payitem");
                            acctClass = payItem.getString("accountingclass");
                        } while(!"A".equals(acctClass));

                        planPayDate = row.getDate("planpaydate");
                    } while(DateUtil.compareDate(planPayDate, detailBeginDate) < 0);

                    BigDecimal amount = row.getBigDecimal("unpaidrent");
                    Date planStartDate = row.getDate("plan_startdate");
                    Date planEndDate = row.getDate("plan_enddate");
                    DynamicObject payRuleRow = LeaseUtil.getPayRuleRow(leaseContract, payItem.getLong("id"), planStartDate);
                    String frequency = payRuleRow.getString("frequency");
                    Map<String, BigDecimal> amMap = new HashMap(8);
                    String payPoint;
                    if (!InterestAmortizeSchema.A.name().equals(interestAmortizeSchema) && !PayFrequency.F.name().equals(frequency)) {
                        payPoint = payRuleRow.getString("paypoint");
                        int relativePayDate = payRuleRow.getInt("relativepaydate");
                        Date startDate = planStartDate;
                        List<String> payDateList = new ArrayList();

                        while(DateUtil.compareDate(startDate, planEndDate) <= 0) {
                            Date endDate = DateUtil.addNaturalMonth(startDate, 1);
                            endDate = DateUtil.addDay(endDate, -1);
                            if (DateUtil.compareDate(endDate, planEndDate) > 0) {
                                endDate = planEndDate;
                            }

                            Date payDate = LeaseUtil.calPlanPayDate(startDate, endDate, payPoint, relativePayDate);
                            startDate = DateUtil.addDay(endDate, 1);
                            if (DateUtil.compareDate(payDate, detailBeginDate) >= 0) {
                                String payDateStr = this.formatDate(payDate);
                                payDateList.add(payDateStr);
                            }
                        }

                        int payTimes = payDateList.size();
                        if (payTimes == 0) {
                            logger.info(String.format("付款计划[%s]按月摊销时，未找到付款日期。", row.getString("plannumber")));
                        } else {
                            BigDecimal tempAmount = amount.divide(new BigDecimal(payTimes), amtPrecision, LeaseUtil.getRoundingMode4CalAmount());
                            BigDecimal sumAmount = tempAmount.multiply(new BigDecimal(payTimes - 1)).setScale(amtPrecision, LeaseUtil.getRoundingMode4CalAmount());
                            BigDecimal tailDiff = amount.subtract(sumAmount);

                            for(int i = 0; i < payTimes; ++i) {
                                String payDateStr = (String)payDateList.get(i);
                                if (i == payTimes - 1) {
                                    amMap.put(payDateStr, tailDiff);
                                } else {
                                    amMap.put(payDateStr, tempAmount);
                                }
                            }

                            boolean sameDateExistedMultiPayTemp = this.amMapMergeAllAmountMap(amMap, allAmountMap);
                            if (sameDateExistedMultiPayTemp && !sameDateExistedMultiPay) {
                                sameDateExistedMultiPay = true;
                            }
                        }
                    } else {
                        payPoint = this.formatDate(planPayDate);
                        amMap.put(payPoint, amount);
                        boolean sameDateExistedMultiPayTemp = this.amMapMergeAllAmountMap(amMap, allAmountMap);
                        if (sameDateExistedMultiPayTemp && !sameDateExistedMultiPay) {
                            sameDateExistedMultiPay = true;
                        }
                    }
                }
            }
        }
    }

    private boolean amMapMergeAllAmountMap(Map<String, BigDecimal> amountMap, Map<String, BigDecimal> allAmountMap) {
        boolean sameDateExistedMultiPay = false;
        Iterator var4 = amountMap.entrySet().iterator();

        while(var4.hasNext()) {
            Map.Entry<String, BigDecimal> amountMapEntry = (Map.Entry)var4.next();
            BigDecimal exitAmnout = (BigDecimal)allAmountMap.get(amountMapEntry.getKey());
            if (exitAmnout == null) {
                allAmountMap.put(amountMapEntry.getKey(), amountMapEntry.getValue());
            } else {
                BigDecimal exitAmnoutAddNew = exitAmnout.add((BigDecimal)amountMapEntry.getValue());
                allAmountMap.put(amountMapEntry.getKey(), exitAmnoutAddNew);
                sameDateExistedMultiPay = true;
            }
        }

        return sameDateExistedMultiPay;
    }

    private void generateReversal(GenInterestDetailParamPo param) {
        DynamicObject interestDetail = (DynamicObject)this.interestDetailMap.get(param.getLeaseContractId());
        Date terminationDate = param.getTerminationDate();
        DynamicObject leaseContract = interestDetail.getDynamicObject("leasecontract");
        leaseContract = BusinessDataServiceHelper.loadSingleFromCache(leaseContract.getPkValue(), "nckd_fa_salary_retir");
        Date leaseStartDate = leaseContract.getDate("leasestartdate");
        boolean invalidContract = DateUtil.isSameDay(terminationDate, leaseStartDate);
        Date leaseTermStartDate = leaseContract.getDate("leasetermstartdate");
        DynamicObjectCollection detailEntry = interestDetail.getDynamicObjectCollection("detailentry");
        detailEntry.sort(Comparator.comparingInt((v) -> {
            return v.getInt("seq");
        }));
        Date lastPayDate = null;

        for(int i = detailEntry.size() - 1; i >= 0; --i) {
            DynamicObject row = (DynamicObject)detailEntry.get(i);
            boolean latestData = row.getBoolean("latestdata");
            if (latestData) {
                BigDecimal leaseLiabPay = row.getBigDecimal("leaseliabpay");
                if (BigDecimal.ZERO.compareTo(leaseLiabPay) < 0) {
                    lastPayDate = row.getDate("date");
                    break;
                }
            }
        }

        if (lastPayDate != null && DateUtil.compareShortDate(param.getTerminationDate(), lastPayDate) < 0) {
            Date reversalBeginDate = invalidContract ? leaseTermStartDate : param.getTerminationDate();
            Date detailBeginDate = this.getDetailBeginDate(leaseContract);
            if (DateUtil.compareDate(reversalBeginDate, detailBeginDate) < 0) {
                reversalBeginDate = detailBeginDate;
            }

            String contractVersion = leaseContract.getString("version");
            long orgId = leaseContract.getLong(Fa.id("org"));
//            Date curPeriodEndDate = leaseContract.getDate("leaseenddate");

            DynamicObject leaseInit = this.getLeaseInitFromLocalCache(orgId);
            DynamicObject curPeriod = leaseInit.getDynamicObject("curperiod");
            Date curPeriodEndDate = curPeriod.getDate("enddate");

            FaFindPeriodHelper findPeriodHelper = this.getFindPeriodHelperFromLocalCache(orgId);
            int entrySize = detailEntry.size();
            int seq = entrySize + 1;

            for(int i = 0; i < entrySize; ++i) {
                DynamicObject row = (DynamicObject)detailEntry.get(i);
                boolean latestData = row.getBoolean("latestdata");
                if (latestData) {
                    Date date = row.getDate("date");
                    if (DateUtil.compareDate(date, reversalBeginDate) >= 0) {
                        if (DateUtil.compareDate(date, lastPayDate) > 0) {
                            break;
                        }

                        BigDecimal beginBalance = row.getBigDecimal("beginbalance");
                        BigDecimal leaseLiabPay = row.getBigDecimal("leaseliabpay");
                        BigDecimal leaseLiabInt = row.getBigDecimal("leaseliabint");
                        BigDecimal endBalance = row.getBigDecimal("endbalance");
                        BigDecimal realDailyRate = row.getBigDecimal("realdailyrate");
                        DynamicObject reversalDetail = detailEntry.addNew();
                        reversalDetail.set("seq", seq++);
                        reversalDetail.set("date", date);
                        reversalDetail.set("beginbalance", beginBalance.negate());
                        reversalDetail.set("leaseliabpay", leaseLiabPay.negate());
                        reversalDetail.set("leaseliabint", leaseLiabInt.negate());
                        reversalDetail.set("endbalance", endBalance.negate());
                        reversalDetail.set("realdailyrate", realDailyRate);
                        reversalDetail.set("amortizationperiod", null);
                        if (DateUtil.compareDate(date, curPeriodEndDate) <= 0) {
                            reversalDetail.set("amortizationperiod", null);
                        } else {
                            reversalDetail.set("amortizationperiod", findPeriodHelper.findPeriodObjByDate(date));
                        }

                        reversalDetail.set("sourcetype", InterestDetailSourceType.B.name());
                        reversalDetail.set("latestdata", Boolean.TRUE);
                        reversalDetail.set("contractversion", contractVersion);
                    }
                }
            }

        }
    }

    protected Date getDetailBeginDate(DynamicObject leaseContract) {
        String sourceType = leaseContract.getString("sourcetype");
        Date initConfirmDate = leaseContract.getDate("initconfirmdate");
        Date sysSwitchDate = leaseContract.getDate("sysswitchdate");
        return LeaseContractSourceType.A.name().equals(sourceType) ? initConfirmDate : sysSwitchDate;
    }

    protected BigDecimal getDetailBeginBalance(DynamicObject leaseContract) {
        return leaseContract.getBigDecimal("leaseliab");
    }

    protected BigDecimal getNotConfirmFinCost(DynamicObject leaseContract) {
        BigDecimal leaseLiabOri = leaseContract.getBigDecimal("leaseliabori");
        BigDecimal leaseLiab = leaseContract.getBigDecimal("leaseliab");
        return leaseLiabOri.subtract(leaseLiab);
    }

    // 生产计息明细
    private Date getDetailEndDate(DynamicObject leaseContract, Map<String, BigDecimal> amountMap) {
        Date leaseEndDate = leaseContract.getDate("leaseenddate");
        Date detailEndDate = leaseEndDate;
        if (!amountMap.isEmpty()) {
            String lastPayDateStr = (String)amountMap.keySet().stream().max(Comparator.naturalOrder()).get();

            try {
                Date lastPayDate = (new SimpleDateFormat("yyyyMMdd")).parse(lastPayDateStr);
                if (DateUtil.compareDate(lastPayDate, leaseEndDate) > 0) {
                    detailEndDate = lastPayDate;
                }
            } catch (ParseException var7) {
                logger.error(String.format("生成计息明细表失败，日期转换异常[%s]", lastPayDateStr), var7);
                throw new KDBizException(String.format(ResManager.loadKDString("生成计息明细表失败，日期转换异常“%s”。", "InterestDetailGenerator_2", "fi-fa-business", new Object[0]), lastPayDateStr));
            }
        }

        return detailEndDate;
    }

    private DynamicObject getLeaseInitFromLocalCache(long orgId) {

        // 获取org会计账簿信息
        DynamicObject leaseInit = (DynamicObject)this.leaseInitMap.get(orgId);
        if (leaseInit == null) {
            String select = Fa.comma(new String[]{"curperiod", "periodtype"});

            leaseInit = BusinessDataServiceHelper.loadSingle("gl_accountbook", select, (new QFilter("org", "=", orgId)).toArray());
            if (leaseInit == null) {
                throw new KDBizException(String.format(ResManager.loadKDString("未找到核算组织[id = %s]的退养初始化数据。", "InterestDetailGenerator_3", "fi-fa-business", new Object[0]), orgId));
            }

            this.leaseInitMap.put(orgId, leaseInit);
        }

        return leaseInit;
    }

    private FaFindPeriodHelper getFindPeriodHelperFromLocalCache(long orgId) {
        DynamicObject leaseInit = this.getLeaseInitFromLocalCache(orgId);
        long periodTypeId = leaseInit.getLong(Fa.id("periodtype"));
        FaFindPeriodHelper helper = (FaFindPeriodHelper)this.findPeriodHelperMap.get(periodTypeId);
        if (helper == null) {
            helper = new FaFindPeriodHelper(periodTypeId);
            this.findPeriodHelperMap.put(periodTypeId, helper);
        }

        return helper;
    }

    private String formatDate(Date date) {
        return date == null ? "" : (new SimpleDateFormat("yyyyMMdd")).format(date);
    }
}
