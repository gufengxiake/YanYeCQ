package nckd.yanye.fi.plugin.form;

import kd.bos.bill.BillOperationStatus;
import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.validate.BillStatus;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.events.*;
import kd.bos.form.field.events.BeforeFilterF7SelectEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.IListView;
import kd.bos.list.ListShowParameter;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.fa.business.enums.lease.LeaseContractBizStatus;
import kd.fi.fa.common.util.Fa;
import kd.fi.fa.common.util.ThrowableHelper;
import kd.fi.fa.utils.FaFormPermissionUtil;
import kd.fi.fa.utils.FaFormUtils;
import nckd.yanye.fi.plugin.validator.SalaryLeaseContractValidator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Module           :财务云-租赁管理-退养人员工资
 * Description      :退休人员工资 列表组件
 *
 *
 * @author guozhiwei
 * @date  2024/8/8 9:37
 *
 *
 */


public class SalaryLeaseContractListPlugin extends AbstractListPlugin {

    private static final String KEY_PUSH = "push";
    private static final String KEY_RENT_SETTLE = "rentsettle";
    private static final String KEY_QUERY_RENT_SETTLE = "queryrentsettle";
    private static final String KEY_QUERY_INTEREST_DETAIL = "queryinterestdetail";
    public static final String PARAM_CONTRACT_ID = "param_contract_id";
    private static final String KEY_FULL_TERMINATION = "fulltermination";
    private static final String KEY_UNDO_TERMINATION = "undotermination";
    private static final int LEASE_TERMINATION_MAX_DEAL_NUM = 200;
    private static final String CALL_BACK_REFRESH = "call_back_refresh";
    private static Log logger = LogFactory.getLog(SalaryLeaseContractListPlugin.class);

    public SalaryLeaseContractListPlugin() {
    }

    public void filterContainerInit(FilterContainerInitArgs args) {
        super.filterContainerInit(args);

        try {
            String jsonString = FaFormUtils.linkOrgContainerInit(this.getPageCache(), this.getView(), "assetunit");
            this.getPageCache().put("assetunits", jsonString);
        } catch (IOException var3) {
            logger.error(ThrowableHelper.toString(var3));
        }

        FaFormPermissionUtil.filterContainerInitV2(args, this.getPageCache(), this.getView().getPageId());
    }

    public void filterContainerSearchClick(FilterContainerSearchClickArgs args) {
        super.filterContainerSearchClick(args);
        FaFormPermissionUtil.filterContainerSearchClick(args, this.getPageCache());
    }

    public void filterContainerBeforeF7Select(BeforeFilterF7SelectEvent e) {
        super.filterContainerBeforeF7Select(e);
        FaFormPermissionUtil.filterContainerBeforeF7SelectV2(e, this.getView().getPageId(), "fa_lease_contract");
    }

    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate formOperate = (FormOperate)args.getSource();
        String operateKey = formOperate.getOperateKey();
        List<String> errorInfo = new ArrayList(10);
        switch (operateKey) {
            case "push":
                errorInfo = this.validate4Push();
                break;
            case "queryrentsettle":
                errorInfo = this.validate4LinkQuery();
                break;
            case "queryinterestdetail":
                ListSelectedRowCollection selectedRows = ((IListView)this.getView()).getSelectedRows();
                Set<Object> selectContractIds = (Set)selectedRows.stream().map(ListSelectedRow::getPrimaryKeyValue).collect(Collectors.toSet());
                if (selectContractIds.size() > 1) {
                    ((List)errorInfo).add(ResManager.loadKDString("请选择单个合同进行联查。", "LeaseContractListPlugin_0", "fi-fa-formplugin", new Object[0]));
                } else {
                    errorInfo = this.validate4LinkQuery();
                }
        }

        if (!((List)errorInfo).isEmpty()) {
            args.setCancel(true);
            this.getView().showTipNotification(String.join(" ", (Iterable)errorInfo));
        }

    }

    public void afterDoOperation(AfterDoOperationEventArgs args) {
        super.afterDoOperation(args);
        String operateKey = args.getOperateKey();
        OperationResult opResult = args.getOperationResult();
        switch (operateKey) {
            case "rentsettle":
                if (opResult != null && opResult.isSuccess()) {
                    this.showRentSettleList();
                }
                break;
            case "queryrentsettle":
                this.showRentSettleList();
                break;
            case "queryinterestdetail":
                this.showInterestDetail();
                break;
            case "fulltermination":
                if (opResult != null && opResult.isSuccess()) {
                    this.showFullTerminationForm();
                }
                break;
            case "undotermination":
                if (opResult != null && opResult.isSuccess()) {
                    this.showUndoTerminationForm();
                }
        }

    }

    public void closedCallBack(ClosedCallBackEvent evt) {
        super.closedCallBack(evt);
        String actionId = evt.getActionId();
        if ("call_back_refresh".equals(actionId)) {
            this.getView().invokeOperation("refresh");
        }

    }

    private List<String> validate4Push() {
        List<String> errorInfo = new ArrayList(10);
        ListSelectedRowCollection selectedRows = ((IListView)this.getView()).getSelectedRows();
        Stream<Object> idStream = selectedRows.stream().map(ListSelectedRow::getPrimaryKeyValue);
        Map<Object, DynamicObject> contractFromCache = BusinessDataServiceHelper.loadFromCache(idStream.toArray((x$0) -> {
            return new Object[x$0];
        }), "nckd_fa_salary_retir");
        List<DynamicObject> contracts = new ArrayList(contractFromCache.values());
//        List<String> res = LeaseContractValidator.validateForPush(contracts);
        List<String> res = SalaryLeaseContractValidator.validateForPush(contracts);
        if (!res.isEmpty()) {
            errorInfo.addAll(res);
        }

        return errorInfo;
    }

    private List<String> validate4LinkQuery() {
        List<String> errorInfo = new ArrayList(10);
        ListSelectedRowCollection selectedRows = ((IListView)this.getView()).getSelectedRows();
        Stream<Object> idStream = selectedRows.stream().map(ListSelectedRow::getPrimaryKeyValue);
        Map<Object, DynamicObject> contractFromCache = BusinessDataServiceHelper.loadFromCache(idStream.toArray((x$0) -> {
            return new Object[x$0];
        }), "nckd_fa_salary_retir");
        List<DynamicObject> contracts = new ArrayList(contractFromCache.values());
//        List<String> res = LeaseContractValidator.validateForLinkQuery(contracts);
        List<String> res = SalaryLeaseContractValidator.validateForLinkQuery(contracts);
        if (!res.isEmpty()) {
            errorInfo.addAll(res);
        }

        return errorInfo;
    }

    private void showRentSettleList() {
        ListShowParameter parameter = new ListShowParameter();
        parameter.setFormId("bos_list");
        parameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
        List<Long> contractIdList = new ArrayList(1);
        List<String> orgIdList = new ArrayList(1);
        ListSelectedRowCollection selectedRows = ((IListView)this.getView()).getSelectedRows();
        Iterator var5 = selectedRows.iterator();

        while(var5.hasNext()) {
            ListSelectedRow selectedRow = (ListSelectedRow)var5.next();
            long contractId = (Long)selectedRow.getPrimaryKeyValue();
            contractIdList.add(contractId);
            List<QFilter> filterList = new ArrayList(1);
            filterList.add(new QFilter("id", "in", contractId));
            DynamicObject leaseContract = QueryServiceHelper.queryOne("nckd_fa_salary_retir", "id,org", (QFilter[])filterList.toArray(new QFilter[0]));
            String orgId = leaseContract.getString("org");
            orgIdList.add(orgId);
        }

        parameter.getCustomParams().put("contractIdList", SerializationUtils.toJsonString(contractIdList));
        parameter.getCustomParams().put("leaseContractOrgIdList", orgIdList);
        parameter.setBillFormId("nckd_fa_lease_rent_settle");
        parameter.setHasRight(Boolean.TRUE);
        this.getView().showForm(parameter);
    }

    private void showInterestDetail() {
        BillShowParameter param = new BillShowParameter();
        param.setFormId("fa_interest_detail");
        param.getOpenStyle().setShowType(ShowType.MainNewTabPage);
        param.setStatus(OperationStatus.VIEW);
        param.setBillStatus(BillOperationStatus.VIEW);
        ListSelectedRowCollection selectedRows = ((IListView)this.getView()).getSelectedRows();
        QFilter filter = new QFilter("leasecontract", "=", selectedRows.get(0).getPrimaryKeyValue());
        DynamicObject interestDetail = QueryServiceHelper.queryOne("nckd_fa_salary_retir", "id", new QFilter[]{filter});
        param.setPkId(interestDetail.getLong("id"));
        param.setHasRight(Boolean.TRUE);
        this.getView().showForm(param);
    }

    private void showFullTerminationForm() {
        ListSelectedRowCollection selectedRows = ((IListView)this.getView()).getSelectedRows();
        List<Object> contractIdList = (List)selectedRows.stream().map(ListSelectedRow::getPrimaryKeyValue).collect(Collectors.toList());
        QFilter[] filters = new QFilter[]{new QFilter("id", "in", contractIdList), new QFilter("status", "=", BillStatus.C.name()), new QFilter("bizstatus", "=", LeaseContractBizStatus.A.name())};
        String selectFields = Fa.comma(new String[]{"id", "leaseenddate"});
        DynamicObjectCollection contracts = QueryServiceHelper.query("nckd_fa_salary_retir", selectFields, filters, (String)null, 200);
        Map<Long, Date> contractMap = (Map)contracts.stream().collect(Collectors.toMap((v) -> {
            return v.getLong("id");
        }, (v) -> {
            return v.getDate("leaseenddate");
        }));
        String contractMapStr = SerializationUtils.toJsonString(contractMap);
        FormShowParameter param = new FormShowParameter();
        param.setFormId("fa_lease_termination");
        param.getOpenStyle().setShowType(ShowType.Modal);
        param.setCustomParam("param_contract_id", contractMapStr);
        param.setCloseCallBack(new CloseCallBack(this, "call_back_refresh"));
        this.getView().showForm(param);
    }

    private void showUndoTerminationForm() {
        ListSelectedRowCollection selectedRows = ((IListView)this.getView()).getSelectedRows();
        List<Object> contractIdList = (List)selectedRows.stream().map(ListSelectedRow::getPrimaryKeyValue).collect(Collectors.toList());
        QFilter[] filters = new QFilter[]{new QFilter("id", "in", contractIdList), new QFilter("bizstatus", "=", LeaseContractBizStatus.B.name())};
        String selectFields = Fa.comma(new String[]{"id", "renewalcontractid"});
        DynamicObjectCollection contracts = QueryServiceHelper.query("nckd_fa_salary_retir", selectFields, filters, (String)null, 200);
        Map<Long, Long> contractMap = (Map)contracts.stream().collect(Collectors.toMap((v) -> {
            return v.getLong("id");
        }, (v) -> {
            return v.getLong("renewalcontractid");
        }));
        String contractMapStr = SerializationUtils.toJsonString(contractMap);
        FormShowParameter param = new FormShowParameter();
        param.setFormId("fa_lease_undo_termination");
        param.getOpenStyle().setShowType(ShowType.Modal);
        param.setCustomParam("param_contract_id", contractMapStr);
        param.setCloseCallBack(new CloseCallBack(this, "call_back_refresh"));
        this.getView().showForm(param);
    }
}
