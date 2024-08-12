package nckd.yanye.fi.plugin.operate;

import kd.bos.bill.BillOperationStatus;
import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.ShowType;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import nckd.yanye.fi.plugin.validator.SalaryLeaseContractValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Module           :财务云-租赁管理-退养人员工资列表
 * Description      :退养人员工资列表操作插件
 *
 * @author : guozhiwei
 * @date : 2024/8/7
 */

public class SalaryLeaseContractOperPlugin extends AbstractFormPlugin {
    private static final String KEY_PUSH = "push";
    private static final String KEY_RENT_SETTLE = "rentsettle";
    private static final String KEY_QUERY_RENT_SETTLE = "queryrentsettle";
    private static final String KEY_QUERY_INTEREST_DETAIL = "queryinterestdetail";

    public SalaryLeaseContractOperPlugin() {
    }

    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate formOperate = (FormOperate)args.getSource();
        String operateKey = formOperate.getOperateKey();
        DynamicObject leaseContract = this.getModel().getDataEntity();
        List<DynamicObject> leaseContracts = new ArrayList(4);
        leaseContracts.add(leaseContract);
        List<String> errorInfo = null;
        switch (operateKey) {
            case "push":
                errorInfo = SalaryLeaseContractValidator.validateForPush(leaseContracts);
                break;
            case "queryrentsettle":
            case "queryinterestdetail":
                errorInfo = SalaryLeaseContractValidator.validateForLinkQuery(leaseContracts);
        }

        if (errorInfo != null && !errorInfo.isEmpty()) {
            args.setCancel(true);
            this.getView().showTipNotification(String.join(" ", errorInfo));
        }

    }

    public void afterDoOperation(AfterDoOperationEventArgs args) {
        super.afterDoOperation(args);
        switch (args.getOperateKey()) {
            case "rentsettle":
                OperationResult opResult = args.getOperationResult();
                if (opResult != null && opResult.isSuccess()) {
                    this.showRentSettleList();
                }
                break;
            case "queryrentsettle":
                this.showRentSettleList();
                break;
            case "queryinterestdetail":
                this.showInterestDetail();
        }

    }

    private void showRentSettleList() {
        ListShowParameter parameter = new ListShowParameter();
        parameter.setFormId("bos_list");
        parameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
        DynamicObject leaseContract = this.getModel().getDataEntity();
        List<Long> contractIdList = new ArrayList(1);
        long leaseContractId = leaseContract.getLong("id");
        contractIdList.add(leaseContractId);
        parameter.getCustomParams().put("contractIdList", SerializationUtils.toJsonString(contractIdList));
        List<String> orgIdList = new ArrayList(1);
        long orgId = leaseContract.getLong("org_id");
        orgIdList.add(Long.toString(orgId));
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
        param.setHasRight(Boolean.TRUE);
        long settleShareSrcId = (Long)this.getModel().getValue("settlesharesrcid");
        long contractId = settleShareSrcId != 0L ? settleShareSrcId : (Long)this.getModel().getDataEntity().getPkValue();
        QFilter filter = new QFilter("leasecontract", "=", contractId);
        DynamicObject interestDetail = QueryServiceHelper.queryOne("fa_interest_detail", "id", new QFilter[]{filter});
        param.setPkId(interestDetail.getLong("id"));
        this.getView().showForm(param);
    }
}
