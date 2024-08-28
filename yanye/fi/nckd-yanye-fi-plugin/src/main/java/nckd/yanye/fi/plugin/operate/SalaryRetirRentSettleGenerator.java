package nckd.yanye.fi.plugin.operate;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.validate.BillStatus;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.fa.business.depretask.DepreSplitSumUtils;
import kd.fi.fa.business.enums.lease.InterestDetailSourceType;
import kd.fi.fa.business.enums.lease.RentSettleSourceType;
import kd.fi.fa.business.lease.InterestDetailGenerator;
import kd.fi.fa.common.util.ContextUtil;
import kd.fi.fa.common.util.Tuple;
import kd.fi.fa.po.GenInterestDetailParamPo;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 *
 * Module           :财务云-租赁管理-退养人员工资
 * @author guozhiwei
 * @date  2024/8/5 15:18
 * @description  退养人员生成摊销与计息表数据
 *  标识:nckd_fa_lease_rent_settle
 *
 */


public class SalaryRetirRentSettleGenerator {
    private final List<GenInterestDetailParamPo> paramPos;
    protected final String BILL_NO_CONNECTOR = "-";

    public SalaryRetirRentSettleGenerator(List<GenInterestDetailParamPo> paramPos) {
        this.paramPos = paramPos;
    }

    public void generate() {
        // 改为退让人员单据
        SalaryInterstDetailGenerator generator = new SalaryInterstDetailGenerator(this.paramPos);
        List<DynamicObject> interestDetails = generator.generate();
        List<DynamicObject> rentSettles = new ArrayList(this.paramPos.size());
        Iterator var4 = interestDetails.iterator();

        while(var4.hasNext()) {
            DynamicObject interestDetail = (DynamicObject)var4.next();
            DynamicObject leaseContract = interestDetail.getDynamicObject("leasecontract");
            rentSettles.addAll(this.generateByInterestDetail(interestDetail, leaseContract));
        }

        SaveServiceHelper.save((DynamicObject[])interestDetails.toArray(new DynamicObject[0]));
        saveRentSettle((DynamicObject[])rentSettles.toArray(new DynamicObject[0]), true);
//        LeaseUtil.saveRentSettle((DynamicObject[])rentSettles.toArray(new DynamicObject[0]), true);
    }

    protected List<DynamicObject> generateByInterestDetail(DynamicObject interestDetail, DynamicObject leaseContract) {
        List<DynamicObject> rentSettleList = new ArrayList(32);
        boolean isExempt = leaseContract.getBoolean("isexempt");
        DynamicObject org = interestDetail.getDynamicObject("org");
        String contractNumber = leaseContract.getString("number");
        DynamicObject currency = interestDetail.getDynamicObject("currency");
        DynamicObjectCollection detailEntry = interestDetail.getDynamicObjectCollection("detailentry");
        int seq = this.getBeginIndex4Generate();
        BigDecimal rent = BigDecimal.ZERO;
        int interestDays = 0;
        BigDecimal interestSum = BigDecimal.ZERO;
        long lastAmortizationPeriodId = 0L;
        DynamicObject lastAmortizationPeriod = null;
        BigDecimal lastEndBalance = BigDecimal.ZERO;
        BigDecimal lastRealDailyRate = BigDecimal.ZERO;
        Date lastData = null;
        Iterator var19 = detailEntry.iterator();

        while(var19.hasNext()) {
            DynamicObject detail = (DynamicObject)var19.next();
            DynamicObject amortizationPeriod = detail.getDynamicObject("amortizationperiod");
            if (amortizationPeriod == null) {
                throw new KDBizException(String.format(ResManager.loadKDString("生成摊销与计息发生异常：计息明细表第[%s]行摊销期间为空。", "RentSettleGenerator_0", "fi-fa-business", new Object[0]), detail.getInt("seq")));
            }

            long amortizationPeriodId = amortizationPeriod.getLong("id");

            if (this.isBeginGenerate(amortizationPeriodId)) {
                if (lastAmortizationPeriodId != 0L && amortizationPeriodId != lastAmortizationPeriodId) {
                    String suffix = this.formatRentSettleNo(seq++);
                    String billNo = contractNumber + "-" + suffix;
                    Tuple<Integer, Integer> tupleMonth = this.getFormatMonthAndNextMonth(lastData);
                    DynamicObject rentSettle = this.generateRentSettle(org, leaseContract, billNo, lastAmortizationPeriod, rent, interestDays, lastRealDailyRate, interestSum, lastEndBalance, (Integer)tupleMonth.item1, (Integer)tupleMonth.item2, currency);
                    rentSettleList.add(rentSettle);
                    rent = BigDecimal.ZERO;
                    interestDays = 0;
                    interestSum = BigDecimal.ZERO;
                }

                BigDecimal leaseLiabPay = detail.getBigDecimal("leaseliabpay");
                rent = rent.add(leaseLiabPay);
                BigDecimal leaseLiabInt = detail.getBigDecimal("leaseliabint");
                interestSum = interestSum.add(leaseLiabInt);
                boolean latestData = detail.getBoolean("latestdata");
                if (latestData && !isExempt) {
                    ++interestDays;
                }

                lastAmortizationPeriod = amortizationPeriod;
                lastAmortizationPeriodId = amortizationPeriodId;
                lastEndBalance = detail.getBigDecimal("endbalance");
                lastRealDailyRate = detail.getBigDecimal("realdailyrate");
                lastData = detail.getDate("date");
            }
        }

        String suffix = this.formatRentSettleNo(seq);
        String billNo = contractNumber + "-" + suffix;
        Tuple<Integer, Integer> tupleMonth = this.getFormatMonthAndNextMonth(lastData);
        DynamicObject rentSettle = this.generateRentSettle(org, leaseContract, billNo, lastAmortizationPeriod, rent, interestDays, lastRealDailyRate, interestSum, lastEndBalance, (Integer)tupleMonth.item1, (Integer)tupleMonth.item2, currency);
        rentSettleList.add(rentSettle);
        return rentSettleList;
    }

    protected int getBeginIndex4Generate() {
        return 1;
    }

    protected boolean isBeginGenerate(long amortizationPeriodId) {
        return true;
    }

    public void reverse() {
        InterestDetailGenerator generator = new InterestDetailGenerator(this.paramPos);
        List<DynamicObject> interestDetails = generator.reverse();
        List<DynamicObject> rentSettles = new ArrayList(this.paramPos.size());
        Map<Long, Integer> rentSettleNumMap = this.queryRentSettleNum();
        Iterator var5 = interestDetails.iterator();

        while(var5.hasNext()) {
            DynamicObject detail = (DynamicObject)var5.next();
            DynamicObject leaseContract = detail.getDynamicObject("leasecontract");
            long leaseContractId = leaseContract.getLong("id");
            Integer rentSettleCnt = (Integer)rentSettleNumMap.get(leaseContractId);
            List<DynamicObject> reverseRentSettleList = this.generate4TerminationReverse(detail, rentSettleCnt + 1);
            rentSettles.addAll(reverseRentSettleList);
        }

        SaveServiceHelper.save((DynamicObject[])interestDetails.toArray(new DynamicObject[0]));
        saveRentSettle((DynamicObject[])rentSettles.toArray(new DynamicObject[0]), true);
//        LeaseUtil.saveRentSettle((DynamicObject[])rentSettles.toArray(new DynamicObject[0]), true);
    }

    private List<DynamicObject> generate4TerminationReverse(DynamicObject interestDetail, int rentSettleBeginSeq) {
        List<DynamicObject> reverseRentSettleList = new ArrayList(10);
        DynamicObject org = interestDetail.getDynamicObject("org");
        DynamicObject leaseContract = interestDetail.getDynamicObject("leasecontract");
        String contractNumber = leaseContract.getString("number");
        DynamicObject currency = interestDetail.getDynamicObject("currency");
        DynamicObjectCollection detailEntry = interestDetail.getDynamicObjectCollection("detailentry");
        int seq = rentSettleBeginSeq;
        BigDecimal rent = BigDecimal.ZERO;
        int interestDays = 0;
        BigDecimal interestSum = BigDecimal.ZERO;
        long lastAmortizationPeriodId = 0L;
        DynamicObject lastAmortizationPeriod = null;
        BigDecimal lastRealDailyRate = BigDecimal.ZERO;
        boolean hasReversalDetail = false;
        Iterator var18 = detailEntry.iterator();

        while(var18.hasNext()) {
            DynamicObject detail = (DynamicObject)var18.next();
            String sourceType = detail.getString("sourcetype");
            if (InterestDetailSourceType.B.name().equals(sourceType)) {
                hasReversalDetail = true;
                DynamicObject amortizationPeriod = detail.getDynamicObject("amortizationperiod");
                long amortizationPeriodId = amortizationPeriod.getLong("id");
                if (lastAmortizationPeriodId != 0L && amortizationPeriodId != lastAmortizationPeriodId) {
                    String suffix = this.formatRentSettleNo(seq++);
                    String billNo = contractNumber + "-" + suffix;
                    DynamicObject rentSettle = this.generateReverseRentSettle(org, leaseContract, billNo, lastAmortizationPeriod, rent, interestDays, lastRealDailyRate, interestSum, currency);
                    reverseRentSettleList.add(rentSettle);
                    rent = BigDecimal.ZERO;
                    interestDays = 0;
                    interestSum = BigDecimal.ZERO;
                }

                BigDecimal leaseLiabPay = detail.getBigDecimal("leaseliabpay");
                rent = rent.add(leaseLiabPay);
                BigDecimal leaseLiabInt = detail.getBigDecimal("leaseliabint");
                interestSum = interestSum.add(leaseLiabInt);
                if (BigDecimal.ZERO.compareTo(leaseLiabInt) != 0) {
                    ++interestDays;
                }

                lastAmortizationPeriod = amortizationPeriod;
                lastAmortizationPeriodId = amortizationPeriodId;
                lastRealDailyRate = detail.getBigDecimal("realdailyrate");
            }
        }

        if (hasReversalDetail) {
            String suffix = this.formatRentSettleNo(seq);
            String billNo = contractNumber + "-" + suffix;
            DynamicObject rentSettle = this.generateReverseRentSettle(org, leaseContract, billNo, lastAmortizationPeriod, rent, interestDays, lastRealDailyRate, interestSum, currency);
            reverseRentSettleList.add(rentSettle);
        }

        return reverseRentSettleList;
    }

    private Map<Long, Integer> queryRentSettleNum() {
        List<Long> contractIds = (List)this.paramPos.stream().map(GenInterestDetailParamPo::getLeaseContractId).collect(Collectors.toList());
        QFilter filter = new QFilter("leasecontract", "in", contractIds);
        DataSet rentSettleDataSet = QueryServiceHelper.queryDataSet(kd.fi.fa.business.lease.RentSettleGenerator.class.toString(), "nckd_fa_lease_rent_settle", "leasecontract", filter.toArray(), (String)null);
        rentSettleDataSet = rentSettleDataSet.groupBy(new String[]{"leasecontract"}).count("cnt").finish();
        Map<Long, Integer> rentSettleNumMap = new HashMap(this.paramPos.size());
        Iterator var5 = rentSettleDataSet.iterator();

        while(var5.hasNext()) {
            Row row = (Row)var5.next();
            Long contractId = row.getLong("leasecontract");
            Integer cnt = row.getInteger("cnt");
            rentSettleNumMap.put(contractId, cnt);
        }

        return rentSettleNumMap;
    }

    private Tuple<Integer, Integer> getFormatMonthAndNextMonth(Date date) {
        if (date == null) {
            throw new KDBizException(ResManager.loadKDString("生成摊销与计息发生异常：日期不能为空。", "RentSettleGenerator_1", "fi-fa-business", new Object[0]));
        } else {
            int month = this.formatDate(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(2, 1);
            int nextMonth = this.formatDate(calendar.getTime());
            return new Tuple(month, nextMonth);
        }
    }

    private int formatDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
        String formatDateStr = format.format(date);
        return Integer.parseInt(formatDateStr + "00");
    }

    private String formatRentSettleNo(int seq) {
        DecimalFormat format = new DecimalFormat("000");
        return format.format((long)seq);
    }

    private DynamicObject generateReverseRentSettle(DynamicObject org, DynamicObject leaseContract, String billNo, DynamicObject amortizationPeriod, BigDecimal rent, int interestDays, BigDecimal dailyRate, BigDecimal interestSum, DynamicObject currency) {
        DynamicObject rentSettle = this.generateRentSettle(org, leaseContract, billNo, amortizationPeriod, rent, interestDays, dailyRate, interestSum, BigDecimal.ZERO, 0, 0, currency);
        rentSettle.set("sourcetype", RentSettleSourceType.B.name());
        return rentSettle;
    }

    private DynamicObject generateRentSettle(DynamicObject org, DynamicObject leaseContract, String billNo, DynamicObject amortizationPeriod, BigDecimal rent, int interestDays, BigDecimal dailyRate, BigDecimal interestSum, BigDecimal endLeaseLiab, int settleMonth, int nextSettleMonth, DynamicObject currency) {

//        DynamicObject rentSettle = BusinessDataServiceHelper.newDynamicObject("fa_lease_rent_settle");
        DynamicObject rentSettle = BusinessDataServiceHelper.newDynamicObject("nckd_fa_lease_rent_settle");
        rentSettle.set("org", org);
        rentSettle.set("leasecontract", leaseContract);
        rentSettle.set("billno", billNo);
        rentSettle.set("amortizationperiod", amortizationPeriod);
        if (amortizationPeriod != null) {
            Date endDate = amortizationPeriod.getDate("enddate");
            rentSettle.set("settledate", endDate);
        }

        rentSettle.set("rent", rent);
        rentSettle.set("interestdays", interestDays);
        rentSettle.set("irr", dailyRate);
        rentSettle.set("interest", interestSum);
        rentSettle.set("endleaseliab", endLeaseLiab);
        rentSettle.set("settledatemonth", settleMonth);
        rentSettle.set("nextsettledatemonth", nextSettleMonth);
        rentSettle.set("currency", currency);
        rentSettle.set("sourcetype", RentSettleSourceType.A.name());
        rentSettle.set("billstatus", BillStatus.C.name());
        rentSettle.set("createtime", new Date());
        rentSettle.set("creator", ContextUtil.getUserId());
        rentSettle.set("modifytime", new Date());
        rentSettle.set("modifier", ContextUtil.getUserId());
        rentSettle.set("auditdate", new Date());
        rentSettle.set("auditor", ContextUtil.getUserId());
        return rentSettle;
    }


    /**
     * 退养人员摊息与计息保存
     * @param rentSettleArr
     * @param syncIep
     */
    public static void saveRentSettle(DynamicObject[] rentSettleArr, boolean syncIep) {
        DynamicObject[] result = (DynamicObject[])((DynamicObject[])SaveServiceHelper.save(rentSettleArr));
        if (syncIep) {
            Object[] pks = Arrays.stream(result).map((v) -> {
                return v.getLong("id");
            }).toArray();
            DepreSplitSumUtils.saveIntelliWhitelist(pks, "nckd_fa_lease_rent_settle");
        }
    }
}
