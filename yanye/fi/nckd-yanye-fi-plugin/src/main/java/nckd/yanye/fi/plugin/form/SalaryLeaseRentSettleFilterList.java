package nckd.yanye.fi.plugin.form;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kd.bos.bill.BillShowParameter;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.filter.CommonFilterColumn;
import kd.bos.filter.FilterColumn;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BillListHyperLinkClickEvent;
import kd.bos.form.events.FilterContainerInitArgs;
import kd.bos.form.events.HyperLinkClickArgs;
import kd.bos.form.events.HyperLinkClickEvent;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.form.field.events.BeforeFilterF7SelectEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.fa.business.lease.utils.LeaseUtil;
import kd.fi.fa.common.util.Fa;
import kd.fi.fa.utils.FaFormPermissionUtil;


/**
 * @author guozhiwei
 * @date  2024/8/2 11:01
 * @description  退休人员摊息与计息 列表组件
 *  标识:nckd_fa_lease_rent_settle
 *
 */


public class SalaryLeaseRentSettleFilterList extends AbstractListPlugin {
    private static final String SHOW_TERMINATION_DATA = "showTerminationData";
    private static final String STR_TRUE = "true";

    public SalaryLeaseRentSettleFilterList() {
    }

    public void filterContainerInit(FilterContainerInitArgs filtercontainerinitargs) {
        super.filterContainerInit(filtercontainerinitargs);
        FaFormPermissionUtil.setOrgDefaultValuesForSelectV2(this.getView().getPageId(), filtercontainerinitargs);
        List<FilterColumn> listFilterColumns = filtercontainerinitargs.getFilterContainerInitEvent().getCommonFilterColumns();
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        Map<String, Object> paramMap = formShowParameter.getCustomParams();
        Iterator var5 = listFilterColumns.iterator();

        while(var5.hasNext()) {
            FilterColumn listFilter = (FilterColumn)var5.next();
            CommonFilterColumn commFilter = (CommonFilterColumn)listFilter;
            switch (commFilter.getFieldName()) {
                case "org.name":
                    List<Object> orgIdList = (List)paramMap.get("leaseContractOrgIdList");
                    if (orgIdList != null && orgIdList.size() > 0) {
                        commFilter.setDefaultValues(orgIdList);
                    }
                    break;
                case "settledate":
                    if (paramMap.containsKey("leaseContractOrgIdList")) {
                        commFilter.setDefaultValue("");
                    }
            }
        }

    }

    public void filterContainerBeforeF7Select(BeforeFilterF7SelectEvent e) {
        super.filterContainerBeforeF7Select(e);
        if (StringUtils.equals("org.id", e.getFieldName())) {
            FaFormPermissionUtil.filterContainerBeforeF7SelectOnlyOrgV2(e, "nckd_fa_lease_rent_settle", this.getView().getPageId());
        }

        if (StringUtils.equals("leasecontract.number", e.getFieldName())) {
            FaFormPermissionUtil.filterLeaseContractBeforeF7SelectByOrgV2(e, "nckd_fa_lease_rent_settle", this.getView().getPageId());
        }

    }

    public void setFilter(SetFilterEvent e) {
        super.setFilter(e);
        List<QFilter> filters = e.getQFilters();
        filters.add(new QFilter(Fa.dot(new String[]{"leasecontract", "isbak"}), "=", Boolean.FALSE));
        String contractIdListStr = (String)this.getView().getFormShowParameter().getCustomParam("contractIdList");
        if (StringUtils.isNotBlank(contractIdListStr)) {
            List<Long> contractIdList = (List<Long>) SerializationUtils.fromJsonStringToList(contractIdListStr, Long.class);
            filters.add(new QFilter("leasecontract", "in", contractIdList));
        }

        this.processDataAfterTerminationPeriod(e);
        String orderBy = "leasecontract asc,billno asc";
        e.setOrderBy(orderBy);
    }

    public void billListHyperLinkClick(HyperLinkClickArgs args) {
        String fieldName = args.getFieldName();
        if (Fa.join("_", new String[]{"leasecontract", "number"}).equals(fieldName)) {
            this.showLeaseContract(args);
        }

    }

    public void afterDoOperation(AfterDoOperationEventArgs args) {
        super.afterDoOperation(args);
        String operateKey = args.getOperateKey();
        if ("genvoucher_dn".equals(operateKey) || "deletevoucher_dn".equals(operateKey)) {
            this.getView().updateView("billlistap");
        }

    }

    private void showLeaseContract(HyperLinkClickArgs args) {
        args.setCancel(true);
        HyperLinkClickEvent evt = args.getHyperLinkClickEvent();
        ListSelectedRow row = ((BillListHyperLinkClickEvent)evt).getCurrentRow();
        Object rentSettlePk = row.getPrimaryKeyValue();
        DynamicObject rentSettle = BusinessDataServiceHelper.loadSingleFromCache(rentSettlePk, "nckd_fa_lease_rent_settle");
        DynamicObject leaseContract = rentSettle.getDynamicObject("leasecontract");
        Object leaseContractPk = leaseContract.getPkValue();
        BillShowParameter param = new BillShowParameter();
        param.setFormId("nckd_fa_salary_retir");
        param.setPkId(leaseContractPk);
        param.getOpenStyle().setShowType(ShowType.MainNewTabPage);
        this.getView().showForm(param);
    }

    private void processDataAfterTerminationPeriod(SetFilterEvent e) {
        QFilter[] filters = new QFilter[]{new QFilter("entity", "=", "nckd_fa_lease_rent_settle"), new QFilter("param", "=", "showTerminationData"), new QFilter("enable", "=", Boolean.TRUE)};
        DynamicObject billParam = QueryServiceHelper.queryOne("fa_billparam", "value", filters);
        if (billParam != null) {
            String paramValue = billParam.getString("value");
            if (!"true".equals(paramValue)) {
                List<QFilter> mergeFilters = e.getMergeQFilters();
                String selectFields = Fa.comma(new String[]{"id", "leasecontract", "amortizationperiod"});
                DynamicObjectCollection preRentSettleList = QueryServiceHelper.query("nckd_fa_lease_rent_settle", selectFields, (QFilter[])mergeFilters.toArray(new QFilter[0]));
                Set<Long> preContractIdSet = (Set)preRentSettleList.stream().map((v) -> {
                    return v.getLong("leasecontract");
                }).collect(Collectors.toSet());
                Map<Long, Long> amortizationPeriodIdMap = LeaseUtil.calTerminationAmortizationPeriodId(preContractIdSet);
                List<Long> rentSettleIdList = new ArrayList(preRentSettleList.size());
                Iterator var11 = preRentSettleList.iterator();

                while(var11.hasNext()) {
                    DynamicObject rentSettle = (DynamicObject)var11.next();
                    long rentSettleId = rentSettle.getLong("id");
                    long leaseContractId = rentSettle.getLong("leasecontract");
                    Long termAmortizationPeriodId = (Long)amortizationPeriodIdMap.get(leaseContractId);
                    if (termAmortizationPeriodId == null) {
                        rentSettleIdList.add(rentSettleId);
                    } else {
                        long amortizationPeriodId = rentSettle.getLong("amortizationperiod");
                        if (amortizationPeriodId <= termAmortizationPeriodId) {
                            rentSettleIdList.add(rentSettleId);
                        }
                    }
                }

                List<QFilter> qFilters = e.getQFilters();
                qFilters.add(new QFilter("id", "in", rentSettleIdList));
            }
        }
    }
}
