package nckd.yanye.hr.plugin.form.empout;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.util.CollectionUtils;
import kd.hr.hspm.opplugin.infoclassify.empproexp.EmpproexpValidator;
import kd.sdk.hr.hspm.common.result.HrpiServiceOperateResult;
import kd.sdk.hr.hspm.common.utils.DynamicPropValidateUtil;
import kd.sdk.hr.hspm.opplugin.InfoclassifySaveOp;
import nckd.yanye.hr.plugin.form.yearkaohe.IEmyearkaoheService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 核心人力云->人员信息->分类维护表单
 * 年度考核信息，nckd_hspm_yearkaohe
 * 操作插件
 * author：程超华
 */
public class EmporgreloutSaveOp extends InfoclassifySaveOp {
    private static final Log LOGGER = LogFactory.getLog(EmporgreloutSaveOp.class);
    private final IEmporgreloutService empproexpService = IEmporgreloutService.getInstance();
    private final Map<Long, HrpiServiceOperateResult> operateResultMap = new HashMap(16);

    public EmporgreloutSaveOp() {
    }

    public void onAddValidators(AddValidatorsEventArgs args) {
        super.onAddValidators(args);
        args.addValidator(new EmpproexpValidator());
    }

    protected void saveNew(BeginOperationTransactionArgs args, DynamicObject[] dataEntities) {
        DynamicObject[] var3 = dataEntities;
        int var4 = dataEntities.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject dataEntity = var3[var5];
            DynamicPropValidateUtil.trim(dataEntity);
            HrpiServiceOperateResult operateResult = this.empproexpService.insertEmchufeninfo(dataEntity);
            this.validateOperateResult(args, operateResult);
        }

    }

    protected void saveOverride(BeginOperationTransactionArgs args, DynamicObject[] dataEntities) {
        DynamicObject[] var3 = dataEntities;
        int var4 = dataEntities.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject dataEntity = var3[var5];
            DynamicPropValidateUtil.trim(dataEntity);
            Long pkId = (Long)dataEntity.getPkValue();
            DynamicObject dbDy = this.empproexpService.getEmchufeninfoByPkId(pkId);
            boolean compareResult = DynamicPropValidateUtil.checkChanged(dataEntity, dbDy);
            if (!this.isNoDataChanged(args, compareResult)) {
                HrpiServiceOperateResult operateResult = this.empproexpService.updateEmchufeninfo(pkId, dataEntity);
                this.validateOperateResult(args, operateResult);
            }
        }

    }

    protected void delete(BeginOperationTransactionArgs args, DynamicObject[] dataEntities) {
        List<Long> pkIdList = (List) Arrays.stream(dataEntities).map((listSelectedRow) -> {
            return (Long)listSelectedRow.getPkValue();
        }).collect(Collectors.toList());
        HrpiServiceOperateResult operateResult = this.empproexpService.deleteEmchufeninfo(pkIdList);
        this.validateOperateResult(args, operateResult);
    }

    protected List<Long> checkDelete(AfterOperationArgs args, DynamicObject[] dataEntities) {
        List<Long> pkIdList = (List)Arrays.stream(dataEntities).map((listSelectedRow) -> {
            return (Long)listSelectedRow.getPkValue();
        }).collect(Collectors.toList());
        return this.empproexpService.queryExistsIdByPkIdList(pkIdList);
    }

    protected void afterDelete(AfterOperationArgs args, DynamicObject[] dataEntities) {
        OperationResult operationResult = this.getOperationResult();
        List<Object> successPkIds = operationResult.getSuccessPkIds();
        if (!CollectionUtils.isEmpty(successPkIds)) {
            List<Long> pkIdList = (List)successPkIds.stream().map((pkId) -> {
                return (Long)pkId;
            }).collect(Collectors.toList());
            // HR中台服务云->员工信息中心->人员附表  系统外任职经历基础页面 nckd_hrpi_emporgrelout
            this.empproexpService.removeAttachment("nckd_hrpi_emporgrelout", pkIdList);
        }

    }

    protected void saveImportNew(BeginOperationTransactionArgs args, DynamicObject[] dataEntities) {
        DynamicObject[] var3 = dataEntities;
        int var4 = dataEntities.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject dataEntity = var3[var5];
            HrpiServiceOperateResult operateResult = this.empproexpService.saveImportEmchufeninfo("new", new DynamicObject[]{dataEntity});
            this.validateOperateResult(args, operateResult, false);
        }

    }

    protected void saveImportOverride(BeginOperationTransactionArgs args, DynamicObject[] dataEntities) {
        DynamicObject[] var3 = dataEntities;
        int var4 = dataEntities.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject dataEntity = var3[var5];
            Long pkId = (Long)dataEntity.getPkValue();
            DynamicObject dbDy = this.empproexpService.getEmchufeninfoByPkId(pkId);
            boolean compareResult = DynamicPropValidateUtil.checkChanged(dataEntity, dbDy);
            if (!compareResult) {
                this.getOperationResult().setMessage("the entity has not changed.");
                LOGGER.info("the entity has not changed.");
            } else {
                HrpiServiceOperateResult operateResult = this.empproexpService.saveImportEmchufeninfo("override", new DynamicObject[]{dataEntity});
                this.validateOperateResult(args, operateResult, false);
                this.operateResultMap.put(dataEntity.getLong("person.id"), operateResult);
            }
        }

    }
}
