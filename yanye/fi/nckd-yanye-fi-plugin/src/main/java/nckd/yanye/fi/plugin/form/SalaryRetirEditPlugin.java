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
import kd.bos.form.control.events.ItemClickEvent;
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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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

    private static  Map<String, DynamicObject> FAPAYMENTITEMSMAP = new HashMap<>();
    private static final String[] EXEMPT_AFFECTED_FIELDS = new String[]{"leaseliabori", "leaseliab", "leaseassets", "assetsaccumdepre", "assetsaddupyeardepre", "hasdepremonths", "accumrent", "addupyearrent", "accuminterest", "addupyearinterest"};

    public SalaryRetirEditPlugin() {
    }

    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("advcontoolbarap");
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

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        // 刷新付款规则
        if (itemKey.equals("nckd_advconbaritemap")) {
            setAdvconbaritemap();
        }
    }

    // 刷新付款规则
    private void setAdvconbaritemap(){
        // todo 获取人员，去查询 hr nckd_staffretiresalacount 筛选出
        // 获取项目
        QFilter qFilter1 = new QFilter("enable", QCP.equals, "1");
        DynamicObject[] faPaymentItems = BusinessDataServiceHelper.load("fa_payment_item", "id,number,name,accountingclass,frequency,enable", new QFilter[]{qFilter1});
        FAPAYMENTITEMSMAP =
                Arrays.stream(faPaymentItems)
                        .collect(Collectors.toMap(
                                detail -> detail.getString("number"),
                                detail -> detail, // 整个 DynamicObject 作为 value
                                (existing, replacement) -> existing // 保留前面的值
                        ));


        // 退养人员
        DynamicObject leaser = (DynamicObject) this.getModel().getValue("leaser");
        if(ObjectUtils.isEmpty(leaser)){
            this.getView().showErrorNotification("请选择退养人员");
            return;
        }
        QFilter qFilter = new QFilter("entryentity.nckd_name.number",QCP.equals,leaser.getString("number"));
//                .and("billstatus", QCP.equals, "C");

        DynamicObject[] nckdStaffretiresalacounts = BusinessDataServiceHelper.load("nckd_staffretiresalacount", "id,entryentity,entryentity.nckd_name,entryentity.nckd_name.number" +
                "entryentity.nckd_twoupmonth,entryentity.nckd_twoupsum,entryentity.nckd_oneupmonth,entryentity.nckd_oneupsum,entryentity.nckd_onemonth,entryentity.nckd_onesum," +
                "entryentity.nckd_highfee,entryentity.nckd_welfareamount,entryentity.nckd_welfareamount1,entryentity.nckd_welfareamount2,entryentity.nckd_welfareamount3," +
                "entryentity.nckd_retiredate,entryentity.nckd_taskeffect,entryentity.nckd_amountstandard,entryentity.nckd_highfeemonth,entryentity.nckd_highfeestand", new QFilter[]{qFilter});
        if(ObjectUtils.isEmpty(nckdStaffretiresalacounts)){
            return;
        }else{
            Date nckdstartDate = null;
            Date nckdendDate = null;
            this.getModel().deleteEntryData("payruleentryentity");
            DynamicObjectCollection payruleentryentity = this.getModel().getDataEntity(true).getDynamicObjectCollection("payruleentryentity");
            payruleentryentity.clear();
            for (DynamicObject nckdStaffretiresalacount : nckdStaffretiresalacounts) {
                DynamicObjectCollection entryentity = nckdStaffretiresalacount.getDynamicObjectCollection("entryentity");

                if(ObjectUtils.isNotEmpty(entryentity)){
                    for (DynamicObject dynamicObject : entryentity) {
                        String string = dynamicObject.getString("nckd_name.number");
                        if(StringUtils.equals(string,leaser.getString("number"))){
                            // 结束时间
                            Date nckdRetiredate = dynamicObject.getDate("nckd_retiredate");
                            // 开始时间
                            Date nckdTaskeffect = dynamicObject.getDate("nckd_taskeffect");

                            // 比较并赋值
                            if (nckdstartDate == null) {
                                nckdstartDate = nckdTaskeffect; // 如果开始时间为 null，赋值为 nckdTaskeffect
                            } else if (nckdTaskeffect != null) {
                                nckdstartDate = nckdstartDate.before(nckdTaskeffect) ? nckdstartDate : nckdTaskeffect; // 取较小的时间
                            }

                            if (nckdendDate == null) {
                                nckdendDate = nckdRetiredate; // 如果结束时间为 null，赋值为 nckdRetiredate
                            } else if (nckdRetiredate != null) {
                                nckdendDate = nckdendDate.after(nckdRetiredate) ? nckdendDate : nckdRetiredate; // 取较大的时间
                            }

                            if(nckdRetiredate != null ){
                                // 相差的月份
                                int totalMonths = getDiffMonthsByLocalDate(nckdTaskeffect, nckdRetiredate, false, true);
                                // 获取对应用户的高温费
                                BigDecimal nckdHighfee = dynamicObject.getBigDecimal("nckd_highfee");
                                // 计算开始日期和结束日期的 LocalDate
                                LocalDate startDate = nckdTaskeffect.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                // 退养人员工资
                                DynamicObject rulePayitem = FAPAYMENTITEMSMAP.get("02");
                                Object rulePayitemFrequency = FAPAYMENTITEMSMAP.get("02").get("frequency");
                                BigDecimal nckdAmountstandard = dynamicObject.getBigDecimal("nckd_amountstandard");
                                BigDecimal nckdOnesum = dynamicObject.getBigDecimal("nckd_onesum");
                                BigDecimal nckdOneupsum = dynamicObject.getBigDecimal("nckd_oneupsum");
                                BigDecimal nckdTwoupsum = dynamicObject.getBigDecimal("nckd_twoupsum");
                                // 工资标准 不为0 的情况下，计算各个时间段的工资
                                if(nckdAmountstandard.compareTo(BigDecimal.ZERO) != 0){
                                    if(totalMonths>24){
                                        DynamicObject dynamicObject1 = payruleentryentity.addNew();
                                        dynamicObject1.set("rule_startdate",nckdTaskeffect);
                                        dynamicObject1.set("rule_enddate",Date.from(startDate.plusMonths(12).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                                        dynamicObject1.set("amount",nckdOnesum);
                                        dynamicObject1.set("paypoint","A");
                                        dynamicObject1.set("relativepaydate",1);
                                        dynamicObject1.set("rule_invoicetype","A");
                                        dynamicObject1.set("rule_payitem",rulePayitem);
                                        dynamicObject1.set("frequency",rulePayitemFrequency);

                                        DynamicObject dynamicObject2 = payruleentryentity.addNew();
                                        dynamicObject2.set("rule_startdate",Date.from(startDate.plusMonths(12).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                                        dynamicObject2.set("rule_enddate",Date.from(startDate.plusMonths(24).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                                        dynamicObject2.set("amount",nckdOneupsum);
                                        dynamicObject2.set("paypoint","A");
                                        dynamicObject2.set("relativepaydate",1);
                                        dynamicObject2.set("rule_invoicetype","A");
                                        dynamicObject2.set("rule_payitem",rulePayitem);
                                        dynamicObject2.set("frequency",rulePayitemFrequency);

                                        DynamicObject dynamicObject3 = payruleentryentity.addNew();
                                        dynamicObject3.set("rule_startdate",Date.from(startDate.plusMonths(24).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                                        dynamicObject3.set("rule_enddate",nckdRetiredate);
                                        dynamicObject3.set("paypoint","A");
                                        dynamicObject3.set("relativepaydate",1);
                                        dynamicObject3.set("rule_invoicetype","A");
                                        dynamicObject3.set("amount",nckdTwoupsum);
                                        dynamicObject3.set("rule_payitem",rulePayitem);
                                        dynamicObject3.set("frequency",rulePayitemFrequency);

                                    }else if(totalMonths>12){
                                        DynamicObject dynamicObject1 = payruleentryentity.addNew();
                                        dynamicObject1.set("rule_startdate",nckdTaskeffect);
                                        dynamicObject1.set("rule_enddate",Date.from(startDate.plusMonths(12).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                                        dynamicObject1.set("amount",nckdOnesum);
                                        dynamicObject1.set("paypoint","A");
                                        dynamicObject1.set("relativepaydate",1);
                                        dynamicObject1.set("rule_invoicetype","A");
                                        dynamicObject1.set("rule_payitem",rulePayitem);
                                        dynamicObject1.set("frequency",rulePayitemFrequency);

                                        DynamicObject dynamicObject2 = payruleentryentity.addNew();
                                        dynamicObject2.set("rule_startdate",Date.from(startDate.plusMonths(12).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                                        dynamicObject2.set("rule_enddate",nckdRetiredate);
                                        dynamicObject2.set("paypoint","A");
                                        dynamicObject2.set("relativepaydate",1);
                                        dynamicObject2.set("rule_invoicetype","A");
                                        dynamicObject2.set("amount",nckdOneupsum);
                                        dynamicObject2.set("rule_payitem",rulePayitem);
                                        dynamicObject2.set("frequency",rulePayitemFrequency);
                                    }else{
                                        // 一年以内使用开始到结束日期
                                        DynamicObject dynamicObject1 = payruleentryentity.addNew();
                                        dynamicObject1.set("rule_startdate",nckdTaskeffect);
                                        dynamicObject1.set("rule_enddate",nckdRetiredate);
                                        dynamicObject1.set("paypoint","A");
                                        dynamicObject1.set("relativepaydate",1);
                                        dynamicObject1.set("rule_invoicetype","A");
                                        dynamicObject1.set("amount",nckdOnesum);
                                        dynamicObject1.set("rule_payitem",rulePayitem);
                                        dynamicObject1.set("frequency",rulePayitemFrequency);
                                    }
                                }



                                // 计算高温费
                                if(!nckdHighfee.equals(new BigDecimal("0E-10"))) {
                                    //  存在高温费
                                    int nckdHighfeemonth = dynamicObject.getInt("nckd_highfeemonth");
                                    // 高温费项目
                                    DynamicObject dynamicObject2 = FAPAYMENTITEMSMAP.get("03");
                                    boolean isHighfee = true;
                                    if(ObjectUtils.isEmpty(dynamicObject2)){
                                        isHighfee = false;
                                    }
                                    BigDecimal highfeestand = dynamicObject.getBigDecimal("nckd_highfeestand");
                                    // 判断当前月份是否处于高温费月份，判断结算日期是否处于高温费月份
                                    List<Date[]> consolidatedTimeRanges = getConsolidatedTimeRanges(nckdTaskeffect, nckdRetiredate);
                                    for (Date[] consolidatedTimeRange : consolidatedTimeRanges) {
                                        DynamicObject dynamicObject1 = payruleentryentity.addNew();
                                        dynamicObject1.set("rule_startdate", consolidatedTimeRange[0]);
                                        dynamicObject1.set("rule_enddate", consolidatedTimeRange[1]);
                                        dynamicObject1.set("amount",highfeestand);
                                        dynamicObject1.set("paypoint","A");
                                        dynamicObject1.set("relativepaydate",1);
                                        dynamicObject1.set("rule_invoicetype","A");
                                        if(isHighfee){
                                            dynamicObject1.set("rule_payitem",dynamicObject2);
                                            dynamicObject1.set("frequency",dynamicObject2.get("frequency"));
                                        }
                                    }
                                }
                            }

                            //  其他福利项

                            BigDecimal nckdWelfareamount = dynamicObject.getBigDecimal("nckd_welfareamount");
                            insertNewEntry(payruleentryentity,nckdWelfareamount);
                            BigDecimal nckdWelfareamount1 = dynamicObject.getBigDecimal("nckd_welfareamount1");
                            insertNewEntry(payruleentryentity,nckdWelfareamount1);
                            BigDecimal nckdWelfareamount2 = dynamicObject.getBigDecimal("nckd_welfareamount2");
                            insertNewEntry(payruleentryentity,nckdWelfareamount2);
                            BigDecimal nckdWelfareamount3 = dynamicObject.getBigDecimal("nckd_welfareamount3");
                            insertNewEntry(payruleentryentity,nckdWelfareamount3);
                        }
                    }
                    this.getView().updateView("payruleentryentity");
                }
            }
            this.getModel().setValue("leasestartdate",nckdstartDate);
            this.getModel().setValue("leaseenddate",nckdendDate);
        }
    }

    public static List<Date[]> getConsolidatedTimeRanges(Date startDate, Date endDate) {
        List<Date[]> ranges = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        startCal.setTime(startDate);
        endCal.setTime(endDate);

        // 确保起始日期在结束日期之前
        if (startCal.after(endCal)) {
            return ranges; // 如果起始日期在结束日期之后，返回空列表
        }

        // 移动到6月的第一天
        if (startCal.get(Calendar.MONTH) < Calendar.JUNE) {
            startCal.set(Calendar.MONTH, Calendar.JUNE);
            startCal.set(Calendar.DAY_OF_MONTH, 1);
        } else if (startCal.get(Calendar.MONTH) > Calendar.SEPTEMBER) {
            startCal.set(Calendar.MONTH, Calendar.JUNE);
            startCal.set(Calendar.DAY_OF_MONTH, 1);
        }

        // 确保起始日期不早于给定日期
        if (startCal.before(startDate)) {
            startCal.setTime(startDate);
        }

        // 迭代6, 7, 8, 9月
        while (startCal.get(Calendar.YEAR) < endCal.get(Calendar.YEAR) ||
                (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && startCal.get(Calendar.MONTH) <= Calendar.SEPTEMBER)) {

            // 设置时间段的开始日期
            Date rangeStart = startCal.getTime();

            // 设置结束日期为9月的最后一天
            startCal.set(Calendar.MONTH, Calendar.SEPTEMBER);
            startCal.set(Calendar.DAY_OF_MONTH, 30);
            Date rangeEnd = startCal.getTime();

            // 确保结束日期不超过总结束日期
            if (rangeEnd.after(endDate)) {
                rangeEnd = endDate;
            }

            // 只添加包含6-9月的时间段
            if (isInJuneToSeptember(rangeStart, rangeEnd)) {
                ranges.add(new Date[]{rangeStart, rangeEnd});
            }

            // 移动到下一个年份的6月
            startCal.add(Calendar.YEAR, 1);
            startCal.set(Calendar.MONTH, Calendar.JUNE);
            startCal.set(Calendar.DAY_OF_MONTH, 1);
        }

        return ranges;
    }

    // 检查日期范围是否在6到9月之间
    public static boolean isInJuneToSeptember(Date start, Date end) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        int startMonth = cal.get(Calendar.MONTH); // 0为1月
        cal.setTime(end);
        int endMonth = cal.get(Calendar.MONTH);

        // 检查是否在6到9月之间
        return (startMonth <= Calendar.SEPTEMBER && endMonth >= Calendar.JUNE);
    }


    private void insertNewEntry(DynamicObjectCollection entryentity,BigDecimal amount){
        if(amount.compareTo(BigDecimal.ZERO) != 0){
            DynamicObject dynamicObject = entryentity.addNew();
            dynamicObject.set("amount",amount);
            dynamicObject.set("paypoint","A");
            dynamicObject.set("relativepaydate",1);
            dynamicObject.set("rule_invoicetype","A");
            DynamicObject rulePayitem = FAPAYMENTITEMSMAP.get("04");
            if(ObjectUtils.isNotEmpty(rulePayitem)){
                dynamicObject.set("rule_payitem",rulePayitem);
                dynamicObject.set("frequency",rulePayitem.get("frequency"));
            }
        }
    }

    // 标品财务使用的计算月份数
    public static int getDiffMonthsByLocalDate(Date beginDate, Date endDate, boolean includeBeginDate, boolean monthRoundUp) {
        if (compareDate(beginDate, endDate) >= 0) {
            return 0;
        } else {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String beginDateStr = format.format(beginDate);
            String endDateStr = format.format(endDate);
            LocalDate beginLocalDate = LocalDate.parse(beginDateStr);
            LocalDate endLocalDate = LocalDate.parse(endDateStr);
            if (includeBeginDate) {
                endLocalDate = endLocalDate.plusDays(1L);
            }

            Period period = Period.between(beginLocalDate, endLocalDate);
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();
            int diffMonth = years * 12 + months;
            if (monthRoundUp && days > 0) {
                ++diffMonth;
            }

            return diffMonth;
        }
    }

    public static int compareDate(Date d1, Date d2) {
        if (d1 != null && d2 != null) {
            long t1 = d1.getTime();
            long t2 = d2.getTime();
            return Long.compare(t1, t2);
        } else {
            KDBizException exception = new KDBizException(ResManager.loadKDString("比较的日期为空，请联系管理员。", "DateUtil_0", "fi-fa-common", new Object[0]));
            logger.error(String.format("日期比较错误：date1:[%s], date2:[%s]", d1, d2), exception);
            throw exception;
        }
    }

    private static int getHighFeeCountForMonths(int year, int... months) {
        int count = 0;
        // 遍历月份，检查是否存在高温费
        for (int month : months) {
            // 这里可以根据具体业务逻辑判断该月份的高温费是否存在
            // count += checkHighFeeForMonth(year, month);
        }
        return count;
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
//            this.initLeaseContractCheck(leaseInit);
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

    // 业务页签切换战术数据
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
        // 不用校验 租赁初始化状态
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
        // 计算退养 初始确认日
        LeaseContractCal.setInitConfirmDate(this.buildObjWrapper());
    }

    private void setIsExempt() {
        String parentFormId = this.getView().getFormShowParameter().getParentFormId();
        if (!parentFormId.equals("fa_lease_change_bill")) {
            LeaseContractCal.setIsExempt(this.buildObjWrapper());
        }

    }

    private void setDiscountRate() {
        // 获取币别信息，如果不存在默认使用人名币
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
