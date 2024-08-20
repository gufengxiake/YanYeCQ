package nckd.yanye.fi.plugin.validator;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.db.SqlBuilder;
import kd.bos.entity.validate.BillStatus;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.bos.util.StringUtils;
import kd.fi.fa.business.enums.lease.InvoiceType;
import kd.fi.fa.business.enums.lease.LeaseContractSourceType;
import kd.fi.fa.business.enums.lease.PayPoint;
import kd.fi.fa.business.enums.lease.TransitionPlan;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.business.utils.SystemParamHelper;
import kd.fi.fa.common.util.DateUtil;
import kd.fi.fa.common.util.Fa;
import kd.fi.fa.common.util.Tuple;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author guozhiwei
 * @date  2024/8/8 15:18
 * @description  退养人员工资列表操作校验
 *  标识:nckd_fa_salary_retir
 *
 */


public class SalaryLeaseContractValidator {
    private static final int AMOUNT_MAX_VALID_NUMBER = 19;
    private static final String ENABLE = "1";

    public SalaryLeaseContractValidator() {
    }

    public static List<String> validateContractForImport(DynamicObject leaseContract) {
        List<String> errorInfoList = validateMustInput4Submit(leaseContract);
        if (!errorInfoList.isEmpty()) {
            return errorInfoList;
        } else {
            errorInfoList = validateForGeneratePayPlan(leaseContract);
            return !errorInfoList.isEmpty() ? errorInfoList : validateContractData(leaseContract);
        }
    }

    public static List<String> validateContractForSave(DynamicObject leaseContract) {
        return validateMustInput4Save(leaseContract);
    }

    public static List<String> validateContractForSubmit(DynamicObject leaseContract) {
        List<String> errorInfoList = validateMustInput4Submit(leaseContract);
        if (!errorInfoList.isEmpty()) {
            return errorInfoList;
        } else {
            errorInfoList = validateForGeneratePayPlan(leaseContract);
            if (!errorInfoList.isEmpty()) {
                return errorInfoList;
            } else {
                errorInfoList = validateContractData(leaseContract);
                return !errorInfoList.isEmpty() ? errorInfoList : validateContractAmountMaxValue(leaseContract);
            }
        }
    }

    public static List<String> validateContractForAudit(DynamicObject leaseContract) {
        return new ArrayList(0);
    }

    public static List<String> validateInitContractForImport(DynamicObject leaseContract) {
        List<String> errorInfo = validateContractForImport(leaseContract);
        if (!errorInfo.isEmpty()) {
            return errorInfo;
        } else {
            errorInfo.addAll(validateInitContractForSubmit(leaseContract));
            return errorInfo;
        }
    }

    public static List<String> validateInitContractForSubmit(DynamicObject leaseContract) {
        return validateInitContractData(leaseContract);
    }

    public static List<String> validateInitContractForAudit(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(2);
        errorInfo.addAll(validateInitContractLeaseLiabOriAndPayPlan(leaseContract));
        errorInfo.addAll(validateLeaseLiabAndLeaseLiabOri(leaseContract));
        return errorInfo;
    }

    public static List<String> validateInitContractForUnAudit(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(1);
        DynamicObject org = leaseContract.getDynamicObject("org");
        long orgId = org.getLong("id");
        QFilter[] filters = new QFilter[]{new QFilter("org", "=", orgId)};
        DynamicObject mainBook = BusinessDataServiceHelper.loadSingleFromCache("fa_lease_init", "status", filters);
        String status = mainBook.getString("status");
        if (BillStatus.C.name().equals(status)) {
            errorInfo.add(ResManager.loadKDString("对应组织已结束初始化，反审核失败。", "LeaseContractValidator_0", "fi-fa-business", new Object[0]));
        }

        return errorInfo;
    }

    public static List<String> validateForGeneratePayPlan(DynamicObject leaseContract) {
        List<String> errorInfo = validateMustInput4GenPayPlan(leaseContract);
        if (!errorInfo.isEmpty()) {
            return errorInfo;
        } else {
            errorInfo = validatePayRuleDate(leaseContract);
            return (List)(!errorInfo.isEmpty() ? errorInfo : new ArrayList(0));
        }
    }

    public static List<String> validateForPush(List<DynamicObject> leaseContracts) {
        List<String> errInitNumber = new ArrayList(8);
        List<String> errorInfo = new ArrayList(4);
        List<String> errNumber = new ArrayList(10);
        Set<Long> orgIds = new HashSet(2);
        Map<Long, String> id2Number = new HashMap(leaseContracts.size(), 1.0F);

        DynamicObject contract;
        for(Iterator var6 = leaseContracts.iterator(); var6.hasNext(); orgIds.add(contract.getLong(Fa.id("org")))) {
            contract = (DynamicObject)var6.next();
            long leaseContractId = contract.getLong("id");
            String number = contract.getString("number");
            id2Number.put(leaseContractId, number);
            if ("B".equals(contract.getString("sourcetype"))) {
                errInitNumber.add(number);
            }
        }

        checkLeaseInitPeriod2BookPeriod(errorInfo, orgIds);
        if (!errInitNumber.isEmpty()) {
            String errInitNumbers = String.join(", ", errInitNumber);
            errorInfo.add(String.format(ResManager.loadKDString("合同号：%s 为初始化合同，请通过初始化合同下推。", "LeaseContractValidator_1", "fi-fa-business", new Object[0]), errInitNumbers));
        }

        DynamicObjectCollection realCards = QueryServiceHelper.query("fa_card_real", "srcbillid", new QFilter[]{new QFilter("srcbillid", "in", id2Number.keySet())});
        Iterator var13 = realCards.iterator();

        while(var13.hasNext()) {
            DynamicObject realCard = (DynamicObject)var13.next();
            long srcBillId = realCard.getLong("srcbillid");
            if (id2Number.containsKey(srcBillId)) {
                errNumber.add(id2Number.get(srcBillId));
            }
        }

        if (!errNumber.isEmpty()) {
            String errNumbers = String.join(", ", errNumber);
            errorInfo.add(String.format(ResManager.loadKDString("合同号：%s 存在后续实物卡片，下推失败。", "LeaseContractValidator_2", "fi-fa-business", new Object[0]), errNumbers));
        }

        return errorInfo;
    }

    private static void checkLeaseInitPeriod2BookPeriod(List<String> errorInfo, Set<Long> orgIds) {
        SqlBuilder sql = new SqlBuilder();
        sql.append("select a.fname from t_fa_lease_init a inner join t_fa_assetbook b on a.forgid = b.forgid and b.fismainbook = '1'", new Object[0]);
        sql.append(" where a.fenableperiodid > b.fcurrentperiodid", new Object[0]);
        sql.appendIn(" and a.forgid", orgIds.toArray());
        DataSet dataSet = DB.queryDataSet("leaseContractPushCard", DBRoute.of("fa"), sql);

        StringBuffer sb;
        String orgName;
        for(sb = new StringBuffer(); dataSet.hasNext(); sb.append(orgName)) {
            Row next = dataSet.next();
            orgName = next.getString("fname");
            if (sb.length() > 0) {
                sb.append("，");
            }
        }

        if (sb.length() > 0) {
            errorInfo.add(String.format(ResManager.loadKDString("下推失败，以下核算组织账簿当前期间早于租赁初始化的启用期间：%s", "LeaseContractValidator_3", "fi-fa-business", new Object[0]), sb));
        }

    }

    public static List<String> validateForPushByInit(List<DynamicObject> leaseContracts) {
        List<String> errorInfo = new ArrayList(4);
        List<String> errNumber = new ArrayList(10);
        Iterator var3 = leaseContracts.iterator();

        while(var3.hasNext()) {
            DynamicObject contract = (DynamicObject)var3.next();
            long masterId = contract.getLong("masterid");
            boolean push = BFTrackerServiceHelper.isPush("fa_lease_contract_init", masterId);
            if (push) {
                errNumber.add(contract.getString("number"));
            }
        }

        if (!errNumber.isEmpty()) {
            String errNumbers = String.join(", ", errNumber);
            errorInfo.add(String.format(ResManager.loadKDString("合同号：%s 存在后续实物卡片，下推失败。", "LeaseContractValidator_2", "fi-fa-business", new Object[0]), errNumbers));
        }

        return errorInfo;
    }

    public static List<String> validateForLinkQuery(List<DynamicObject> leaseContracts) {
        List<String> errorInfo = new ArrayList(4);
        List<Long> contractIds = new ArrayList(leaseContracts.size());
        Iterator var3 = leaseContracts.iterator();

        while(var3.hasNext()) {
            DynamicObject contract = (DynamicObject)var3.next();
            long settleShareSrcId = contract.getLong("settlesharesrcid");
            if (settleShareSrcId != 0L) {
                contractIds.add(settleShareSrcId);
            } else {
                contractIds.add(contract.getLong("id"));
            }
        }

        QFilter filter = new QFilter("leasecontract", "in", contractIds);
        DynamicObjectCollection rentSettle = QueryServiceHelper.query("nckd_fa_lease_rent_settle", "leasecontract", new QFilter[]{filter});
        Set<Long> existContractId = (Set)rentSettle.stream().map((v) -> {
            return v.getLong("leasecontract");
        }).collect(Collectors.toSet());
        List<String> errNumber = new ArrayList(10);
        Iterator var7 = leaseContracts.iterator();

        while(var7.hasNext()) {
            DynamicObject contract = (DynamicObject)var7.next();
            long id = contract.getLong("settlesharesrcid");
            if (id == 0L) {
                id = contract.getLong("id");
            }

            if (!existContractId.contains(id)) {
                String number = contract.getString("number");
                errNumber.add(number);
            }
        }

        if (!errNumber.isEmpty()) {
            String errNumbers = String.join(", ", errNumber);
            errorInfo.add(String.format(ResManager.loadKDString("合同号：%s 没有关联数据。", "LeaseContractValidator_4", "fi-fa-business", new Object[0]), errNumbers));
        }

        return errorInfo;
    }

    private static List<String> validateContractAmountMaxValue(DynamicObject leaseContract) {
        List<String> errorInfoList = new ArrayList(2);
        BigDecimal leaseLiab = leaseContract.getBigDecimal("leaseliab");
        String leaseLiabStr = leaseLiab.toPlainString();
        int leaseLiabValidNum = leaseLiabStr.replace(".", StringUtils.getEmpty()).length();
        if (leaseLiabValidNum > 19) {
            errorInfoList.add(ResManager.loadKDString("“租赁负债现值”超过最大值。", "LeaseContractValidator_5", "fi-fa-business", new Object[0]));
        }

        BigDecimal leaseAssets = leaseContract.getBigDecimal("leaseassets");
        String leaseAssetsStr = leaseAssets.toPlainString();
        int leaseAssetsValidNum = leaseAssetsStr.replace(".", StringUtils.getEmpty()).length();
        if (leaseAssetsValidNum > 19) {
            errorInfoList.add(ResManager.loadKDString("“使用权资产原值”超过最大值。", "LeaseContractValidator_6", "fi-fa-business", new Object[0]));
        }

        return errorInfoList;
    }

    private static List<String> validateMustInput4Save(DynamicObject leaseContract) {
        List<String> errorInfoList = new ArrayList(17);
        String number = leaseContract.getString("number");
        if (StringUtils.isEmpty(number)) {
            errorInfoList.add(ResManager.loadKDString("“合同号”不能为空。", "LeaseContractValidator_7", "fi-fa-business", new Object[0]));
        }

        return !errorInfoList.isEmpty() ? errorInfoList : new ArrayList(0);
    }

    private static List<String> validateMustInput4Submit(DynamicObject leaseContract) {
        List<String> errorInfoList = new ArrayList(17);
        DynamicObject assetUnit = leaseContract.getDynamicObject("assetunit");
        if (assetUnit == null) {
            errorInfoList.add(ResManager.loadKDString("“资产组织”不能为空。", "LeaseContractValidator_8", "fi-fa-business", new Object[0]));
        }

        DynamicObject org = leaseContract.getDynamicObject("org");
        if (org == null) {
            errorInfoList.add(ResManager.loadKDString("“核算组织”不能为空。", "LeaseContractValidator_9", "fi-fa-business", new Object[0]));
        }

        String number = leaseContract.getString("number");
        if (StringUtils.isEmpty(number)) {
            errorInfoList.add(ResManager.loadKDString("“合同号”不能为空。", "LeaseContractValidator_7", "fi-fa-business", new Object[0]));
        }

//        String name = leaseContract.getString("name");
//        if (StringUtils.isEmpty(name)) {
//            errorInfoList.add(ResManager.loadKDString("“合同名称”不能为空。", "LeaseContractValidator_10", "fi-fa-business", new Object[0]));
//        }

        DynamicObject leaser = leaseContract.getDynamicObject("leaser");
        if (leaser == null) {
            errorInfoList.add(ResManager.loadKDString("“退养人员”不能为空。", "LeaseContractValidator_11", "fi-fa-business", new Object[0]));
        }

//        DynamicObject assetCat = leaseContract.getDynamicObject("assetcat");
//        if (assetCat == null) {
//            errorInfoList.add(ResManager.loadKDString("“资产类别”不能为空。", "LeaseContractValidator_12", "fi-fa-business", new Object[0]));
//        }

//        String assetName = leaseContract.getString("assetname");
//        if (StringUtils.isEmpty(assetName)) {
//            errorInfoList.add(ResManager.loadKDString("“资产名称”不能为空。", "LeaseContractValidator_13", "fi-fa-business", new Object[0]));
//        }

//        DynamicObject storePlace = leaseContract.getDynamicObject("storeplace");
//        if (storePlace == null) {
//            errorInfoList.add(ResManager.loadKDString("“存放地点”不能为空。", "LeaseContractValidator_14", "fi-fa-business", new Object[0]));
//        }

//        DynamicObject unit = leaseContract.getDynamicObject("unit");
//        if (unit == null) {
//            errorInfoList.add(ResManager.loadKDString("“计量单位”不能为空。", "LeaseContractValidator_15", "fi-fa-business", new Object[0]));
//        }

//        BigDecimal assetAmount = leaseContract.getBigDecimal("assetamount");
//        if (assetAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            errorInfoList.add(ResManager.loadKDString("“数量”必须大于0。", "LeaseContractValidator_16", "fi-fa-business", new Object[0]));
//        }

        Date leaseStartDate = leaseContract.getDate("leasestartdate");
        if (leaseStartDate == null) {
            errorInfoList.add(ResManager.loadKDString("“退养起始日”不能为空。", "LeaseContractValidator_17", "fi-fa-business", new Object[0]));
        }

        Date leaseEndDate = leaseContract.getDate("leaseenddate");
        if (leaseEndDate == null) {
            errorInfoList.add(ResManager.loadKDString("“退养终止日”不能为空。", "LeaseContractValidator_18", "fi-fa-business", new Object[0]));
        }

        return errorInfoList;
    }

    private static List<String> validateMustInput4GenPayPlan(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(10);
        Date leaseStartDate = leaseContract.getDate("leasestartdate");
        if (leaseStartDate == null) {
            errorInfo.add(ResManager.loadKDString("“退养起始日”不能为空。", "LeaseContractValidator_17", "fi-fa-business", new Object[0]));
        }

        Date leaseEndDate = leaseContract.getDate("leaseenddate");
        if (leaseEndDate == null) {
            errorInfo.add(ResManager.loadKDString("“退养终止日”不能为空。", "LeaseContractValidator_18", "fi-fa-business", new Object[0]));
        }

        DynamicObject currency = leaseContract.getDynamicObject("currency");
        if (currency == null) {
            errorInfo.add(ResManager.loadKDString("获取核算组织本位币失败，请检查核算组织是否为空，或设置“租赁初始化”。", "LeaseContractValidator_19", "fi-fa-business", new Object[0]));
        }

        DynamicObjectCollection ruleEntry = leaseContract.getDynamicObjectCollection("payruleentryentity");

        for(int i = 0; i < ruleEntry.size(); ++i) {
            DynamicObject row = (DynamicObject)ruleEntry.get(i);
            DynamicObject payItem = row.getDynamicObject("rule_payitem");
            if (payItem == null) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“付款项目”不能为空。", "LeaseContractValidator_20", "fi-fa-business", new Object[0]), i + 1));
            }

            String frequency = row.getString("frequency");
            if (StringUtils.isEmpty(frequency)) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“频率”不能为空。", "LeaseContractValidator_21", "fi-fa-business", new Object[0]), i + 1));
            }

            Date startDate = row.getDate("rule_startdate");
            if (startDate == null) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“受益期_起”不能为空。", "LeaseContractValidator_22", "fi-fa-business", new Object[0]), i + 1));
            }

            Date endDate = row.getDate("rule_enddate");
            if (endDate == null) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“受益期_止”不能为空。", "LeaseContractValidator_23", "fi-fa-business", new Object[0]), i + 1));
            }

            String payPoint = row.getString("paypoint");
            if (!PayPoint.A.name().equals(payPoint) && !PayPoint.B.name().equals(payPoint)) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“支付起点”不能为空。", "LeaseContractValidator_24", "fi-fa-business", new Object[0]), i + 1));
            }

            int relativePayDate = row.getInt("relativepaydate");
            if (relativePayDate == 0) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“第几天支付”不能为0。", "LeaseContractValidator_25", "fi-fa-business", new Object[0]), i + 1));
            }

            String invoiceType = row.getString("rule_invoicetype");
            if (StringUtils.isEmpty(invoiceType)) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“发票类型”不能为空。", "LeaseContractValidator_26", "fi-fa-business", new Object[0]), i + 1));
            }

            BigDecimal amount = row.getBigDecimal("amount");
            if (amount == null || BigDecimal.ZERO.compareTo(amount) >= 0) {
                errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“金额”必须大于0。", "LeaseContractValidator_27", "fi-fa-business", new Object[0]), i + 1));
            }
        }

        return errorInfo;
    }

    private static List<String> validateContractData(DynamicObject leaseContract) {
        List<String> errorInfoList = new ArrayList(13);
        BigDecimal assetAmount = leaseContract.getBigDecimal("assetamount");
        Date leaseStartDate = leaseContract.getDate("leasestartdate");
        Date leaseEndDate = leaseContract.getDate("leaseenddate");
        Date freeLeaseStartDate = leaseContract.getDate("freeleasestartdate");
        Date leaseTermStartDate = leaseContract.getDate("leasetermstartdate");
        boolean isExempt = leaseContract.getBoolean("isexempt");
        boolean isStock = LeaseUtil.isStockContract(leaseTermStartDate, isExempt);
        String transitionPlan = leaseContract.getString("transitionplan");
        BigDecimal discountRate = leaseContract.getBigDecimal("discountrate");
        DynamicObjectCollection ruleEntry = leaseContract.getDynamicObjectCollection("payruleentryentity");
        errorInfoList.addAll(validateOrgRelation(leaseContract));
//        if (assetAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            errorInfoList.add(ResManager.loadKDString("“数量”必须大于0。", "LeaseContractValidator_16", "fi-fa-business", new Object[0]));
//        }

        if (leaseStartDate.compareTo(leaseEndDate) >= 0) {
            errorInfoList.add(ResManager.loadKDString("“退养终止日”必须晚于“退养起始日”。", "LeaseContractValidator_28", "fi-fa-business", new Object[0]));
        }

        if (freeLeaseStartDate != null && freeLeaseStartDate.compareTo(leaseStartDate) >= 0) {
            errorInfoList.add(ResManager.loadKDString("“免租期开始日”必须早于“起租日”。", "LeaseContractValidator_29", "fi-fa-business", new Object[0]));
        }

        if (!isExempt && discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            errorInfoList.add(ResManager.loadKDString("“年折现率”必须大于0。", "LeaseContractValidator_30", "fi-fa-business", new Object[0]));
        }

        if (isStock && StringUtils.isEmpty(transitionPlan)) {
            errorInfoList.add(ResManager.loadKDString("存量合同的“过渡方案”不能为空。", "LeaseContractValidator_31", "fi-fa-business", new Object[0]));
        }

        if (!isStock && StringUtils.isNotEmpty(transitionPlan)) {
            errorInfoList.add(ResManager.loadKDString("非存量合同不能录入“过渡方案”。", "LeaseContractValidator_32", "fi-fa-business", new Object[0]));
        }

        if (ruleEntry.isEmpty()) {
            errorInfoList.add(ResManager.loadKDString("请录入至少一行“使用权类”或“租赁负债类”付款规则。", "LeaseContractValidator_33", "fi-fa-business", new Object[0]));
        } else {
            boolean hasItem = false;
            Iterator var13 = ruleEntry.iterator();

            label53: {
                String acctClass;
                do {
                    DynamicObject payItem;
                    do {
                        if (!var13.hasNext()) {
                            break label53;
                        }

                        DynamicObject rule = (DynamicObject)var13.next();
                        payItem = rule.getDynamicObject("rule_payitem");
                    } while(payItem == null);

                    acctClass = payItem.getString("accountingclass");
                } while(!"A".equals(acctClass) && !"B".equals(acctClass));

                hasItem = true;
            }

            if (!hasItem) {
                errorInfoList.add(ResManager.loadKDString("请录入至少一行“使用权类”或“租赁负债类”付款规则。", "LeaseContractValidator_33", "fi-fa-business", new Object[0]));
            }
        }

        return errorInfoList;
    }

    private static List<String> validateInitContractData(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(13);
        DynamicObject leaseInit = getLeaseInitFromCache(leaseContract);
        if (leaseInit == null) {
            errorInfo.add(ResManager.loadKDString("请先创建核算组织的租赁初始化。", "LeaseContractValidator_34", "fi-fa-business", new Object[0]));
            return errorInfo;
        } else {
            String status = leaseInit.getString("status");
            if (BillStatus.C.name().equals(status)) {
                errorInfo.add(ResManager.loadKDString("对应组织租赁初始化已结束初始化，不可以新增初始化合同。", "LeaseContractValidator_35", "fi-fa-business", new Object[0]));
            }

//            Date sysSwitchDate = leaseContract.getDate("sysswitchdate");
//            if (sysSwitchDate == null) {
//                errorInfo.add(ResManager.loadKDString("系统切换日不能为空，请先创建核算组织的资产主账簿。", "LeaseContractValidator_36", "fi-fa-business", new Object[0]));
//            } else if (sysSwitchDate.compareTo(leaseInit.getDate("systemswitchday")) != 0) {
//                errorInfo.add(ResManager.loadKDString("初始化租赁合同系统切换日与租赁初始化系统切换日不一致。", "LeaseContractValidator_37", "fi-fa-business", new Object[0]));
//            }
//
//            Date initConfirmDate = leaseContract.getDate("initconfirmdate");
//            if (initConfirmDate != null && DateUtil.compareDate(initConfirmDate, sysSwitchDate) >= 0) {
//                errorInfo.add(ResManager.loadKDString("初始确认日大于或等于系统切换日，请通过新增合同录入。", "LeaseContractValidator_38", "fi-fa-business", new Object[0]));
//            }

            boolean isExempt = leaseContract.getBoolean("isexempt");
            int depreMonths = getDepreMonths(leaseContract);
            if (isExempt && depreMonths > 12) {
                errorInfo.add(ResManager.loadKDString("“适用租赁期（月）”大于12个月，不能设为豁免合同。", "LeaseContractValidator_39", "fi-fa-business", new Object[0]));
            }

            errorInfo.addAll(validateInitContractFinInfo(leaseContract, leaseInit));
            return errorInfo;
        }
    }

    private static int getDepreMonths(DynamicObject leaseContract) {
        String sourceType = leaseContract.getString("sourcetype");
        Date leaseEndDate = leaseContract.getDate("leaseenddate");
        String transitionPlan = leaseContract.getString("transitionplan");
        Date tempDate = leaseContract.getDate("initconfirmdate");
        if (LeaseContractSourceType.A.name().equals(sourceType) && (TransitionPlan.A.name().equals(transitionPlan) || TransitionPlan.C.name().equals(transitionPlan))) {
            tempDate = leaseContract.getDate("leasetermstartdate");
        }

        if (tempDate != null && leaseEndDate != null) {
            boolean monthRoundUp = getLeaseMonthRoundUpFromSysParam(leaseContract);
            int depreMonths = DateUtil.getDiffMonthsByLocalDate(tempDate, leaseEndDate, true, monthRoundUp);
            return depreMonths;
        } else {
            return 0;
        }
    }

    private static boolean getLeaseMonthRoundUpFromSysParam(DynamicObject leaseContract) {
        DynamicObject org = leaseContract.getDynamicObject("org");
        long orgId = 0L;
        if (org != null) {
            orgId = org.getLong("id");
        }

        return SystemParamHelper.getBooleanParam("leasemonthroundup", orgId, false);
    }

    private static List<String> validateInitContractFinInfo(DynamicObject leaseContract, DynamicObject leaseInit) {
        List<String> errorInfo = new ArrayList(10);
        boolean isExempt = leaseContract.getBoolean("isexempt");
        if (isExempt) {
            errorInfo.addAll(validateLockedFields4Exempt(leaseContract));
            return errorInfo;
        } else {
            BigDecimal leaseLiabOri = leaseContract.getBigDecimal("leaseliabori");
            BigDecimal leaseLiab = leaseContract.getBigDecimal("leaseliab");
            if (BigDecimal.ZERO.compareTo(leaseLiab) > 0) {
                errorInfo.add(ResManager.loadKDString("“租赁负债现值”不能为负数。", "LeaseContractValidator_40", "fi-fa-business", new Object[0]));
            }

            if (BigDecimal.ZERO.compareTo(leaseLiabOri) < 0 ^ BigDecimal.ZERO.compareTo(leaseLiab) < 0) {
                errorInfo.add(ResManager.loadKDString("“租赁负债现值”与“租赁负债原值”必须同时大于0或同时等于0。", "LeaseContractValidator_41", "fi-fa-business", new Object[0]));
            }

            BigDecimal leaseAssets = leaseContract.getBigDecimal("leaseassets");
            if (BigDecimal.ZERO.compareTo(leaseAssets) > 0) {
                errorInfo.add(ResManager.loadKDString("“使用权资产原值”不能为负数。", "LeaseContractValidator_42", "fi-fa-business", new Object[0]));
            }

            BigDecimal assetsAccumDepre = leaseContract.getBigDecimal("assetsaccumdepre");
            if (BigDecimal.ZERO.compareTo(assetsAccumDepre) > 0) {
                errorInfo.add(ResManager.loadKDString("“使用权资产累计折旧”不能为负数。", "LeaseContractValidator_43", "fi-fa-business", new Object[0]));
            }

            if (assetsAccumDepre.compareTo(leaseAssets) > 0) {
                errorInfo.add(ResManager.loadKDString("“使用权资产累计折旧”不能大于“使用权资产原值”。", "LeaseContractValidator_44", "fi-fa-business", new Object[0]));
            }

            BigDecimal assetsYearDepre = leaseContract.getBigDecimal("assetsaddupyeardepre");
            if (BigDecimal.ZERO.compareTo(assetsYearDepre) > 0) {
                errorInfo.add(ResManager.loadKDString("“使用权资产本年累计折旧”不能为负数。", "LeaseContractValidator_45", "fi-fa-business", new Object[0]));
            }

            if (assetsYearDepre.compareTo(assetsAccumDepre) > 0) {
                errorInfo.add(ResManager.loadKDString("“使用权资产本年累计折旧”不能大于“使用权资产累计折旧”。", "LeaseContractValidator_46", "fi-fa-business", new Object[0]));
            }

            int depreMonths = leaseContract.getInt("depremonths");
            int hasDepreMonths = leaseContract.getInt("hasdepremonths");
            if (hasDepreMonths < 0) {
                errorInfo.add(ResManager.loadKDString("“已折旧期间（月）”不能为负数。", "LeaseContractValidator_47", "fi-fa-business", new Object[0]));
            }

            if (hasDepreMonths > depreMonths) {
                errorInfo.add(ResManager.loadKDString("“已折旧期间（月）”不能大于“适用租赁期（月）”。", "LeaseContractValidator_48", "fi-fa-business", new Object[0]));
            }

            if (BigDecimal.ZERO.compareTo(assetsAccumDepre) < 0 ^ hasDepreMonths > 0) {
                errorInfo.add(ResManager.loadKDString("“使用权资产累计折旧”与“已折旧期间（月）”必须同时大于0或同时等于0。", "LeaseContractValidator_49", "fi-fa-business", new Object[0]));
            }

            BigDecimal accumRent = leaseContract.getBigDecimal("accumrent");
            if (BigDecimal.ZERO.compareTo(accumRent) > 0) {
                errorInfo.add(ResManager.loadKDString("“累计租金”不能为负数。", "LeaseContractValidator_50", "fi-fa-business", new Object[0]));
            }

            if (BigDecimal.ZERO.compareTo(accumRent) < 0 && leaseAssets.compareTo(leaseLiab) < 0) {
                errorInfo.add(ResManager.loadKDString("累计租金大于0，“使用权资产原值”不能小于“租赁负债现值”。", "LeaseContractValidator_51", "fi-fa-business", new Object[0]));
            }

            BigDecimal addUpYearRent = leaseContract.getBigDecimal("addupyearrent");
            if (BigDecimal.ZERO.compareTo(addUpYearRent) > 0) {
                errorInfo.add(ResManager.loadKDString("“本年累计租金”不能为负数。", "LeaseContractValidator_52", "fi-fa-business", new Object[0]));
            }

            if (addUpYearRent.compareTo(accumRent) > 0) {
                errorInfo.add(ResManager.loadKDString("“本年累计租金”不能大于“累计租金”。", "LeaseContractValidator_53", "fi-fa-business", new Object[0]));
            }

            BigDecimal accumInterest = leaseContract.getBigDecimal("accuminterest");
            if (BigDecimal.ZERO.compareTo(accumInterest) > 0) {
                errorInfo.add(ResManager.loadKDString("“累计利息”不能为负数。", "LeaseContractValidator_54", "fi-fa-business", new Object[0]));
            }

            BigDecimal addUpYearInterest = leaseContract.getBigDecimal("addupyearinterest");
            if (BigDecimal.ZERO.compareTo(addUpYearInterest) > 0) {
                errorInfo.add(ResManager.loadKDString("“本年累计利息”不能为负数。", "LeaseContractValidator_55", "fi-fa-business", new Object[0]));
            }

            if (addUpYearInterest.compareTo(accumInterest) > 0) {
                errorInfo.add(ResManager.loadKDString("“本年累计利息”不能大于“累计利息”。", "LeaseContractValidator_56", "fi-fa-business", new Object[0]));
            }

            int periodNumber = leaseInit.getInt("curperiod.periodnumber");
            if (periodNumber == 1 && BigDecimal.ZERO.compareTo(assetsYearDepre) < 0) {
                errorInfo.add(ResManager.loadKDString("租赁初始化当前在1期，不允许录入“使用权资产本年累计折旧”。", "LeaseContractValidator_57", "fi-fa-business", new Object[0]));
            }

            if (periodNumber == 1 && BigDecimal.ZERO.compareTo(addUpYearRent) < 0) {
                errorInfo.add(ResManager.loadKDString("租赁初始化当前在1期，不允许录入“本年累计租金”。", "LeaseContractValidator_58", "fi-fa-business", new Object[0]));
            }

            if (periodNumber == 1 && BigDecimal.ZERO.compareTo(addUpYearInterest) < 0) {
                errorInfo.add(ResManager.loadKDString("租赁初始化当前在1期，不允许录入“本年累计利息”。", "LeaseContractValidator_59", "fi-fa-business", new Object[0]));
            }

            return errorInfo;
        }
    }

    private static List<String> validateLockedFields4Exempt(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(12);
        boolean isExempt = leaseContract.getBoolean("isexempt");
        if (!isExempt) {
            return errorInfo;
        } else {
            BigDecimal discountRate = leaseContract.getBigDecimal("discountrate");
            if (BigDecimal.ZERO.compareTo(discountRate) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“年折现率”。", "LeaseContractValidator_60", "fi-fa-business", new Object[0]));
            }

            BigDecimal dailyDiscountRate = leaseContract.getBigDecimal("dailydiscountrate");
            if (BigDecimal.ZERO.compareTo(dailyDiscountRate) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“日折现率”。", "LeaseContractValidator_61", "fi-fa-business", new Object[0]));
            }

            BigDecimal leaseLiabOri = leaseContract.getBigDecimal("leaseliabori");
            if (BigDecimal.ZERO.compareTo(leaseLiabOri) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“租赁负债原值”。", "LeaseContractValidator_62", "fi-fa-business", new Object[0]));
            }

            BigDecimal leaseLiab = leaseContract.getBigDecimal("leaseliab");
            if (BigDecimal.ZERO.compareTo(leaseLiab) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“租赁负债现值”。", "LeaseContractValidator_63", "fi-fa-business", new Object[0]));
            }

            BigDecimal leaseAssets = leaseContract.getBigDecimal("leaseassets");
            if (BigDecimal.ZERO.compareTo(leaseAssets) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“使用权资产原值”。", "LeaseContractValidator_64", "fi-fa-business", new Object[0]));
            }

            BigDecimal assetsAccumDepre = leaseContract.getBigDecimal("assetsaccumdepre");
            if (BigDecimal.ZERO.compareTo(assetsAccumDepre) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“使用权资产累计折旧”。", "LeaseContractValidator_65", "fi-fa-business", new Object[0]));
            }

            BigDecimal assetsAddUpYearDepre = leaseContract.getBigDecimal("assetsaddupyeardepre");
            if (BigDecimal.ZERO.compareTo(assetsAddUpYearDepre) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“使用权资产本年累计折旧”。", "LeaseContractValidator_66", "fi-fa-business", new Object[0]));
            }

            int hasDepreMonths = leaseContract.getInt("hasdepremonths");
            if (hasDepreMonths != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“已折旧期间（月）”。", "LeaseContractValidator_67", "fi-fa-business", new Object[0]));
            }

            BigDecimal accumRent = leaseContract.getBigDecimal("accumrent");
            if (BigDecimal.ZERO.compareTo(accumRent) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“累计租金”。", "LeaseContractValidator_68", "fi-fa-business", new Object[0]));
            }

            BigDecimal addUpYearRent = leaseContract.getBigDecimal("addupyearrent");
            if (BigDecimal.ZERO.compareTo(addUpYearRent) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“本年累计租金”。", "LeaseContractValidator_69", "fi-fa-business", new Object[0]));
            }

            BigDecimal accumInterest = leaseContract.getBigDecimal("accuminterest");
            if (BigDecimal.ZERO.compareTo(accumInterest) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“累计利息”。", "LeaseContractValidator_70", "fi-fa-business", new Object[0]));
            }

            BigDecimal addUpYearInterest = leaseContract.getBigDecimal("addupyearinterest");
            if (BigDecimal.ZERO.compareTo(addUpYearInterest) != 0) {
                errorInfo.add(ResManager.loadKDString("豁免合同不允许录入“本年累计利息”。", "LeaseContractValidator_71", "fi-fa-business", new Object[0]));
            }

            return errorInfo;
        }
    }

    private static List<String> validatePayRuleDate(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(10);
        boolean isExempt = leaseContract.getBoolean("isexempt");
        DynamicObjectCollection payRuleEntry = leaseContract.getDynamicObjectCollection("payruleentryentity");
        if (!isExempt && payRuleEntry.isEmpty()) {
            errorInfo.add(ResManager.loadKDString("付款规则不能为空。", "LeaseContractValidator_72", "fi-fa-business", new Object[0]));
            return errorInfo;
        } else {
            long orgId = leaseContract.getDynamicObject("org").getLong("id");
            Date leaseStartDate = leaseContract.getDate("leasestartdate");
            Date leaseEndDate = leaseContract.getDate("leaseenddate");
            Date initConfirmDate = leaseContract.getDate("initconfirmdate");
            Date leaseTermStartDate = leaseContract.getDate("leasetermstartdate");
            boolean isStock = LeaseUtil.isStockContract(leaseTermStartDate, isExempt);
            String transitionPlan = leaseContract.getString("transitionplan");
            String sourceType = leaseContract.getString("sourcetype");
            Map<String, List<Tuple<Date, Date>>> payItemDateRange = new HashMap(16);

            for(int i = 0; i < payRuleEntry.size(); ++i) {
                DynamicObject row = (DynamicObject)payRuleEntry.get(i);
                String invoiceType = row.getString("rule_invoicetype");
                boolean deductible = row.getBoolean("rule_deductible");
                if (InvoiceType.COMMON.getValue().equals(invoiceType) && deductible) {
                    errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“发票类型”为普通发票，不可抵扣。", "LeaseContractValidator_73", "fi-fa-business", new Object[0]), i + 1));
                }

                BigDecimal taxRate = row.getBigDecimal("rule_taxrate");
                BigDecimal tax = row.getBigDecimal("rule_tax");
                if (!deductible) {
                    if (BigDecimal.ZERO.compareTo(taxRate) < 0 ^ BigDecimal.ZERO.compareTo(tax) < 0) {
                        errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“可抵扣”为否，“税率”、“税额”必须同时大于0或同时等于0。", "LeaseContractValidator_74", "fi-fa-business", new Object[0]), i + 1));
                    }
                } else {
                    if (BigDecimal.ZERO.compareTo(taxRate) >= 0) {
                        errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“可抵扣”为是，“税率”必须大于0。", "LeaseContractValidator_75", "fi-fa-business", new Object[0]), i + 1));
                    }

                    if (BigDecimal.ZERO.compareTo(tax) >= 0) {
                        errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“可抵扣”为是，“税额”必须大于0。", "LeaseContractValidator_76", "fi-fa-business", new Object[0]), i + 1));
                    }
                }

                BigDecimal amount = row.getBigDecimal("amount");
                if (tax.compareTo(amount) > 0) {
                    errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“税额”不能大于“含税金额”。", "LeaseContractValidator_77", "fi-fa-business", new Object[0]), i + 1));
                }

                DynamicObject payItem = row.getDynamicObject("rule_payitem");
                String acctClass = payItem.getString("accountingclass");
                Date startDate = row.getDate("rule_startdate");
                if ("A".equals(acctClass)) {
                    if (DateUtil.compareDate(leaseStartDate, startDate) > 0) {
                        errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“受益期_起”必须大于等于“起租日”。", "LeaseContractValidator_78", "fi-fa-business", new Object[0]), i + 1));
                    }
                } else {
                    Set<String> exceptTransitionPlan = new HashSet(Arrays.asList(TransitionPlan.A.name(), TransitionPlan.C.name()));
                    if (DateUtil.compareDate(initConfirmDate, startDate) > 0 && !exceptTransitionPlan.contains(transitionPlan)) {
                        errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“受益期_起”必须大于等于“初始确认日”。", "LeaseContractValidator_79", "fi-fa-business", new Object[0]), i + 1));
                    }
                }

                String payItemNumber;
                Date endDate;
                if (LeaseContractSourceType.A.name().equals(sourceType)) {
                    endDate = LeaseUtil.getFirstExecDateFromSysParam(orgId);
                    if (isStock && DateUtil.compareDate(startDate, endDate) < 0 && TransitionPlan.B.name().equals(transitionPlan)) {
                        payItemNumber = DateUtil.getShortDate().format(endDate);
                        errorInfo.add(String.format(ResManager.loadKDString("付款规则第%1$s行：当前组织采用“简化追溯法2”作为存量合同过渡方案，付款规则的“受益期_起”须大于等于首次执行日[%2$s]。", "LeaseContractValidator_80", "fi-fa-business", new Object[0]), i + 1, payItemNumber));
                    }
                }

                endDate = row.getDate("rule_enddate");
                if (DateUtil.compareDate(leaseEndDate, endDate) < 0) {
                    errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“受益期_止”必须小于等于“租赁结束日”。", "LeaseContractValidator_81", "fi-fa-business", new Object[0]), i + 1));
                }

                if (DateUtil.compareDate(startDate, endDate) >= 0) {
                    errorInfo.add(String.format(ResManager.loadKDString("付款规则第%s行：“受益期_起”必须小于“受益期_止”。", "LeaseContractValidator_82", "fi-fa-business", new Object[0]), i + 1));
                } else {
                    payItemNumber = payItem.getString("number");
                    if (!payItemDateRange.containsKey(payItemNumber)) {
                        payItemDateRange.put(payItemNumber, new ArrayList());
                    }

                    List<Tuple<Date, Date>> dateRangeList = (List)payItemDateRange.get(payItemNumber);
                    dateRangeList.add(new Tuple(startDate, endDate));
                }
            }

            Iterator var27 = payItemDateRange.entrySet().iterator();

            while(var27.hasNext()) {
                Map.Entry<String, List<Tuple<Date, Date>>> entry = (Map.Entry)var27.next();
                boolean isOverlap = DateUtil.isOverlap((List)entry.getValue());
                if (isOverlap) {
                    errorInfo.add(String.format(ResManager.loadKDString("付款规则中，付款项目[%s]存在重叠的受益期，请检查。", "LeaseContractValidator_83", "fi-fa-business", new Object[0]), entry.getKey()));
                }
            }

            return errorInfo;
        }
    }

    private static List<String> validateOrgRelation(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(1);
        long assetUnitId = leaseContract.getLong(Fa.id("assetunit"));
        List<Long> fromOrgIds = OrgUnitServiceHelper.getFromOrgs("09", assetUnitId, "10", true);
        long orgId = leaseContract.getLong(Fa.id("org"));
        if (!fromOrgIds.contains(orgId)) {
            errorInfo.add(ResManager.loadKDString("核算组织不在当前资产组织的可用组织范围内，请先配置业务单元间协作。", "LeaseContractValidator_84", "fi-fa-business", new Object[0]));
        }

        return errorInfo;
    }

    private static List<String> validateInitContractLeaseLiabOriAndPayPlan(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(2);
        boolean isExempt = leaseContract.getBoolean("isexempt");
        if (isExempt) {
            return errorInfo;
        } else {
            Date sysSwitchDate = leaseContract.getDate("sysswitchdate");
            DynamicObjectCollection planEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
            BigDecimal totalUnpaidRent = BigDecimal.ZERO;
            boolean existPayPlanAfterSysSwitchDate = false;
            Iterator var7 = planEntry.iterator();

            while(var7.hasNext()) {
                DynamicObject row = (DynamicObject)var7.next();
                Date planPayDate = row.getDate("planpaydate");
                if (DateUtil.compareDate(planPayDate, sysSwitchDate) >= 0) {
                    DynamicObject payItem = row.getDynamicObject("plan_payitem");
                    String acctClass = payItem.getString("accountingclass");
                    if ("A".equals(acctClass)) {
                        if (DateUtil.compareDate(planPayDate, sysSwitchDate) > 0) {
                            existPayPlanAfterSysSwitchDate = true;
                        }

                        BigDecimal unpaidRent = row.getBigDecimal("unpaidrent");
                        totalUnpaidRent = totalUnpaidRent.add(unpaidRent);
                    }
                }
            }

            BigDecimal leaseLiabOri = leaseContract.getBigDecimal("leaseliabori");
            if (leaseLiabOri.compareTo(totalUnpaidRent) != 0) {
                errorInfo.add(String.format(ResManager.loadKDString("“租赁负债原值”与付款计划中系统切换日之后的未付租金总额[%s]不一致，请调整“租赁负债原值”或者“付款规则”。", "LeaseContractValidator_85", "fi-fa-business", new Object[0]), totalUnpaidRent.stripTrailingZeros().toPlainString()));
            }

            BigDecimal leaseLiab = leaseContract.getBigDecimal("leaseliab");
            if (leaseLiab.compareTo(leaseLiabOri) != 0 && !existPayPlanAfterSysSwitchDate) {
                errorInfo.add(ResManager.loadKDString("“租赁负债原值”与“租赁负债现值”不相等时，至少要有一笔租赁负债类付款计划的“计划付款日”晚于“系统切换日”。", "LeaseContractValidator_86", "fi-fa-business", new Object[0]));
            }

            return errorInfo;
        }
    }

    private static List<String> validateLeaseLiabAndLeaseLiabOri(DynamicObject leaseContract) {
        List<String> errorInfo = new ArrayList(1);
        BigDecimal leaseLiab = leaseContract.getBigDecimal("leaseliab");
        BigDecimal leaseLiabOri = leaseContract.getBigDecimal("leaseliabori");
        if (leaseLiab.compareTo(leaseLiabOri) < 0) {
            return errorInfo;
        } else if (BigDecimal.ZERO.compareTo(leaseLiab) == 0 && BigDecimal.ZERO.compareTo(leaseLiabOri) == 0) {
            return errorInfo;
        } else if (leaseLiab.compareTo(leaseLiabOri) > 0) {
            errorInfo.add(ResManager.loadKDString("“租赁负债现值”不能大于“租赁负债原值”，请检查数据是否有误。", "LeaseContractValidator_87", "fi-fa-business", new Object[0]));
            return errorInfo;
        } else {
            Date sysSwitchDate = leaseContract.getDate("sysswitchdate");
            DynamicObjectCollection payPlanEntry = leaseContract.getDynamicObjectCollection("payplanentryentity");
            Iterator var6 = payPlanEntry.iterator();

            while(var6.hasNext()) {
                DynamicObject row = (DynamicObject)var6.next();
                DynamicObject payItem = row.getDynamicObject("plan_payitem");
                String acctClass = payItem.getString("accountingclass");
                if ("A".equals(acctClass)) {
                    Date startDate = row.getDate("plan_startdate");
                    DynamicObject payRuleRow = LeaseUtil.getPayRuleRow(leaseContract, payItem.getLong("id"), startDate);
                    String payPoint = payRuleRow.getString("paypoint");
                    Date planPayDate = row.getDate("planpaydate");
                    if (DateUtil.compareDate(planPayDate, sysSwitchDate) > 0) {
                        if (PayPoint.A.name().equals(payPoint)) {
                            Date firstDayOfPayMonth = DateUtil.getMinDateOfMonth(planPayDate);
                            if (DateUtil.compareDate(sysSwitchDate, firstDayOfPayMonth) < 0) {
                                errorInfo.add(String.format(ResManager.loadKDString("付款计划[%s]期初付款，且“计划付款日”在“系统切换日”所在月份之后，“租赁负债现值”不能等于“租赁负债原值”。", "LeaseContractValidator_88", "fi-fa-business", new Object[0]), row.getString("plannumber")));
                            }
                        } else {
                            errorInfo.add(String.format(ResManager.loadKDString("付款计划[%s]期末付款，且“计划付款日”不等于“系统切换日”，“租赁负债现值”不能等于“租赁负债原值”。", "LeaseContractValidator_89", "fi-fa-business", new Object[0]), row.getString("plannumber")));
                        }
                    }
                }
            }

            return errorInfo;
        }
    }

    private static DynamicObject getLeaseInitFromCache(DynamicObject leaseContract) {
        long orgId = leaseContract.getLong(Fa.id("org"));
        QFilter[] mainBookFilters = new QFilter[]{new QFilter("org", "=", orgId)};
        String selectFields = Fa.comma(new String[]{"status", Fa.dot(new String[]{"curperiod", "periodnumber"}), "systemswitchday"});
        return BusinessDataServiceHelper.loadSingleFromCache("fa_lease_init", selectFields, mainBookFilters);
    }
}
