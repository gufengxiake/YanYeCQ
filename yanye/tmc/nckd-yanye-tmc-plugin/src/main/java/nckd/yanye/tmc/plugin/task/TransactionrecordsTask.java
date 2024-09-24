package nckd.yanye.tmc.plugin.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.AppInfo;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.param.AppParam;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;

/**
 * @author husheng
 * @date 2024-09-24 9:43
 * @description  交易汇总记录
 */
public class TransactionrecordsTask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        Set<Long> orgIds = new HashSet<>(16);

        LocalDate currentDate = LocalDate.now();
        // 上个月第一天
        LocalDate firstDayOfCurrentMonth = currentDate.withDayOfMonth(1);
        LocalDate firstDayOfLastMonth = firstDayOfCurrentMonth.minusMonths(1);
        // 上个月最后一天
        int lastDayOfLastMonth = firstDayOfCurrentMonth.minusDays(1).getDayOfMonth();
        LocalDate lastDayOfLastMonthDate = firstDayOfLastMonth.withDayOfMonth(lastDayOfLastMonth);

        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C")
                .and("bizdate", QCP.large_equals, firstDayOfLastMonth)
                .and("bizdate", QCP.less_equals, lastDayOfLastMonthDate);

        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("cdm_receivablebill", "company", qFilter.toArray());
        Arrays.stream(dynamicObjects).forEach(t -> orgIds.add(t.getDynamicObject("company").getLong("id")));
        orgIds.forEach(t -> {
            DynamicObject dynamicObject = BusinessDataServiceHelper.newDynamicObject("nckd_transactionrecords");

            // ----------------本期间收取---------------
            QFilter qFilter1 = this.buildCdmReceivablebillFilter(t, firstDayOfLastMonth, lastDayOfLastMonthDate, "1");
            // ”6+9“银票
            BigDecimal nckdamount1 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter1(qFilter1).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 非”6+9“银票
            BigDecimal nckdamount2 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter2(qFilter1).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 商票
            BigDecimal nckdamount3 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter3(qFilter1).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 小计
            BigDecimal nckd_amountsum1 = nckdamount1.add(nckdamount2).add(nckdamount3);

            // 到期收款
            QFilter qFilter2 = this.buildCdmReceivablebillFilter(t, firstDayOfLastMonth, lastDayOfLastMonthDate, "2")
                    .and("draftbillstatus", QCP.equals, "collected");
            BigDecimal nckdduecollection = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", qFilter2.toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);

            // ----------------背书---------------
            QFilter qFilter3 = this.buildCdmReceivablebillFilter(t, firstDayOfLastMonth, lastDayOfLastMonthDate, "2")
                    .and("draftbillstatus", QCP.equals, "endorsed");
            // ”6+9“银票
            BigDecimal nckdamount4 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter1(qFilter3).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 非”6+9“银票
            BigDecimal nckdamount5 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter2(qFilter3).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 商票
            BigDecimal nckdamount6 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter3(qFilter3).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 小计
            BigDecimal nckd_amountsum2 = nckdamount4.add(nckdamount5).add(nckdamount6);

            // ----------------贴现---------------
            QFilter qFilter4 = this.buildCdmReceivablebillFilter(t, firstDayOfLastMonth, lastDayOfLastMonthDate, "2")
                    .and("draftbillstatus", QCP.equals, "discounted");
            // ”6+9“银票
            BigDecimal nckdamount7 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter1(qFilter4).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 非”6+9“银票
            BigDecimal nckdamount8 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter2(qFilter4).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 商票
            BigDecimal nckdamount9 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter3(qFilter4).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 小计
            BigDecimal nckd_amountsum3 = nckdamount7.add(nckdamount8).add(nckdamount9);

            // ----------------本期间---------------
            QFilter qFilter5 = this.buildCdmReceivablebillFilter(t, firstDayOfLastMonth, lastDayOfLastMonthDate, "3");
            // ”6+9“银票
            BigDecimal nckdamount10 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter1(qFilter5).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 非”6+9“银票
            BigDecimal nckdamount11 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter2(qFilter5).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 商票
            BigDecimal nckdamount12 = Arrays.stream(BusinessDataServiceHelper.load("cdm_receivablebill", "amount", this.buildQFilter3(qFilter5).toArray())).map(t1 -> t1.getBigDecimal("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 小计
            BigDecimal nckd_amountsum4 = nckdamount10.add(nckdamount11).add(nckdamount12);

            // 本期间应收票据余额
            Boolean parameter = (Boolean) getSysCtrlParameter("cdm", "nckd_endorsementtransfer", t);
            BigDecimal nckdreceivablebalance = nckd_amountsum4.add(nckdamount5).add(nckdamount8);
            if (parameter) {
                nckdreceivablebalance = nckdreceivablebalance.subtract(nckdamount10);
            }

            dynamicObject.set("nckd_org", t);
            dynamicObject.set("nckd_amount1", nckdamount1);
            dynamicObject.set("nckd_amount2", nckdamount2);
            dynamicObject.set("nckd_amount3", nckdamount3);
            dynamicObject.set("nckd_amountsum1", nckd_amountsum1);
            dynamicObject.set("nckd_duecollection", nckdduecollection);
            dynamicObject.set("nckd_amount4", nckdamount4);
            dynamicObject.set("nckd_amount5", nckdamount5);
            dynamicObject.set("nckd_amount6", nckdamount6);
            dynamicObject.set("nckd_amountsum2", nckd_amountsum2);
            dynamicObject.set("nckd_amount7", nckdamount7);
            dynamicObject.set("nckd_amount8", nckdamount8);
            dynamicObject.set("nckd_amount9", nckdamount9);
            dynamicObject.set("nckd_amountsum3", nckd_amountsum3);
            dynamicObject.set("nckd_amount10", nckdamount10);
            dynamicObject.set("nckd_amount11", nckdamount11);
            dynamicObject.set("nckd_amount12", nckdamount12);
            dynamicObject.set("nckd_amountsum4", nckd_amountsum4);
            dynamicObject.set("nckd_receivablebalance", nckdreceivablebalance);
            SaveServiceHelper.save(new DynamicObject[]{dynamicObject});
        });
    }

    // 获取系统参数
    public Object getSysCtrlParameter(String appNumber, String paramPro, Long orgId) {
        AppParam appParam = new AppParam();
        AppInfo appInfo = AppMetadataCache.getAppInfo(appNumber);
        String appId = appInfo.getId();
        appParam.setAppId(appId);
        appParam.setOrgId(orgId);
        Map<String, Object> map = SystemParamServiceHelper.loadAppParameterFromCache(appParam);
        return map == null ? null : map.get(paramPro);
    }

    /**
     * 构建”6+9“银票过滤条件
     *
     * @param qFilter
     * @return
     */
    private QFilter buildQFilter1(QFilter qFilter) {
        return qFilter.copy().and("draftbilltype.number", QCP.in, new String[]{"101", "102"})
                .and("accepterbebank.nckd_bankcredit_type", QCP.equals, "A");
    }

    /**
     * 构建非”6+9“银票过滤条件
     *
     * @param qFilter
     * @return
     */
    private QFilter buildQFilter2(QFilter qFilter) {
        return qFilter.copy().and("draftbilltype.number", QCP.in, new String[]{"101", "102"})
                .and("accepterbebank.nckd_bankcredit_type", QCP.in, new String[]{"B", "C"});
    }

    /**
     * 构建商票过滤条件
     *
     * @param qFilter
     * @return
     */
    private QFilter buildQFilter3(QFilter qFilter) {
        return qFilter.copy().and("draftbilltype.number", QCP.in, new String[]{"103", "104"});
    }

    /**
     * 构建票据过滤条件
     *
     * @param orgId               组织id
     * @param firstDayOfLastMonth 上个月第一天
     * @param lastDayOfLastMonth  上个月最后一天
     * @param type
     * @return
     */
    private QFilter buildCdmReceivablebillFilter(Long orgId, LocalDate firstDayOfLastMonth, LocalDate lastDayOfLastMonth, String type) {
        QFilter qFilter = new QFilter("billstatus", QCP.equals, "C")
                .and("company", QCP.equals, orgId);

        if ("1".equals(type)) {
            // 开始日期
            qFilter.and(new QFilter("bizdate", QCP.large_equals, firstDayOfLastMonth));
            // 结束日期
            qFilter.and(new QFilter("bizdate", QCP.less_equals, lastDayOfLastMonth));
        } else if ("2".equals(type)) {
            // 开始日期
            qFilter.and(new QFilter("bizfinishdate", QCP.large_equals, firstDayOfLastMonth));
            // 结束日期
            qFilter.and(new QFilter("bizfinishdate", QCP.less_equals, lastDayOfLastMonth));
        } else if ("3".equals(type)) {
            qFilter.and(new QFilter("bizdate", QCP.large_equals, firstDayOfLastMonth)
                    .and("bizdate", QCP.less_equals, lastDayOfLastMonth)
                    .and("bizfinishdate", QCP.large_than, lastDayOfLastMonth)
                    .or(new QFilter("bizdate", QCP.less_than, firstDayOfLastMonth)
                            .or("bizdate", QCP.large_than, lastDayOfLastMonth)
                            .and("draftbillstatus", QCP.equals, "registered")));
        }

        return qFilter;
    }
}
