package nckd.yanye.scm.plugin.form;

import com.google.type.Decimal;
import kd.bos.bill.BillShowParameter;
import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityType;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.botp.runtime.BFRow;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.botp.runtime.SourceBillReport;
import kd.bos.entity.datamodel.IRefrencedataProvider;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.metadata.dao.MetadataDao;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.mvc.FormConfigFactory;
import kd.bos.mvc.SessionManager;
import kd.bos.form.IFormView;
import kd.bos.mvc.bill.BillModel;
import kd.occ.ocbase.common.util.DynamicObjectUtils;
import java.math.BigDecimal;
import java.util.*;

/**
 * 采购订单审核服务插件  自动生成采购合同（审核状态） 同时更新采购订单合同相关信息
 * 表单标识：nckd_pm_purorderbill_ext
 * author:黄文波 2024-09-19
 */
public class PurorderAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("mainbillentryid");//核心单据行Id
        e.getFieldKeys().add("qty");//数量
    }
    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
      super.endOperationTransaction(e);

        DynamicObject[] deliverRecords = e.getDataEntities();
        String targetEntityNumber =this.billEntityType.getName();

        for(DynamicObject dataEntity :deliverRecords)
        {
            String sourceBill = "pm_purorderbill";//采购订单
            String targetBill = "conm_purcontract";//采购合同
            String ruleId = "2043580320552337408";//单据转换Id

            Object pkId=dataEntity.getPkValue();

            //根据Id获取采购订单单实体
            DynamicObject saloutBill = BusinessDataServiceHelper.loadSingle(pkId, sourceBill);
            //获取单据体数据的集合
            DynamicObjectCollection goodsEntities = saloutBill.getDynamicObjectCollection("billentry");

            // 创建下推参数
            PushArgs pushArgs = new PushArgs();
            // 必填，源单标识
            pushArgs.setSourceEntityNumber(sourceBill);
            // 必填，目标单标识
            pushArgs.setTargetEntityNumber(targetBill);
            // 可选，传入true，不检查目标单新增权
            pushArgs.setHasRight(true);
            // 可选，传入目标单验权使用的应用编码
            //pushArgs.setAppId("");
            // 可选，传入目标单主组织默认值
            //pushArgs.setDefOrgId(orgId);
            //可选，转换规则id
            pushArgs.setRuleId(ruleId);
            //自动保存
            //pushArgs.setAutoSave(true);
            // 可选，是否输出详细错误报告
            pushArgs.setBuildConvReport(true);
            // 必填，设置需要下推的单据，或分录行
            List<ListSelectedRow> selectedRows = new ArrayList<>();
            for (DynamicObject entryObj : goodsEntities) {
                //获取某行数据的id
                Object entryId = entryObj.getPkValue();
                BigDecimal qty = entryObj.getBigDecimal("joinqty");
                if (qty.compareTo(BigDecimal.ZERO) == 0) {
                    ListSelectedRow row = new ListSelectedRow();
                    //必填，设置源单单据id
                    row.setPrimaryKeyValue(pkId);
                    //可选，设置源单分录标识
                    row.setEntryEntityKey("billentry");
                    //可选，设置源单分录id
                    row.setEntryPrimaryKeyValue(entryId);
                    selectedRows.add(row);
                }
            }

            if (selectedRows.size() > 0) {

                // 必选，设置需要下推的源单及分录内码
                pushArgs.setSelectedRows(selectedRows);
                // 调用下推引擎，下推目标单
                ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
                // 判断下推是否成功，如果失败，提取失败消息
                if (!pushResult.isSuccess()) {
                    String errMessage = pushResult.getMessage();    // 错误信息
                    for (SourceBillReport billReport : pushResult.getBillReports()) {
                        // 提取各单错误报告
                        if (!billReport.isSuccess()) {
                            String billMessage = billReport.getFailMessage();
                        }
                    }
                    throw new KDBizException("下推失败:" + errMessage);
                }
                // 获取生成的目标单数据包
                MainEntityType targetMainType = EntityMetadataCache.getDataEntityType(targetBill);
                List<DynamicObject> targetBillObjs = pushResult.loadTargetDataObjects(new IRefrencedataProvider() {
                    @Override
                    public void fillReferenceData(Object[] objs, IDataEntityType dType) {
                        BusinessDataReader.loadRefence(objs, dType);
                    }
                }, targetMainType);
                DynamicObject[] saveDynamicObject = targetBillObjs.toArray(new DynamicObject[targetBillObjs.size()]);
                //保存
                OperationResult operationResult1 = SaveServiceHelper.saveOperate(targetBill, saveDynamicObject, OperateOption.create());

                if (operationResult1.isSuccess()) {
                    OperateOption auditOption = OperateOption.create();
                    auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
                    auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
                    //提交
                    OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, saveDynamicObject, auditOption);
                    if (subResult.isSuccess()) {
                        //审核
                        OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, saveDynamicObject, auditOption);
                    }
                }

            }
        }

    }

    private static String getAppId(String entityNumber, MainEntityType dt) {
        String appId = dt.getAppId();
        if (!"bos".equals(appId)) {
            return appId;
        } else {
            String bizAppNumber = dt.getBizAppNumber();
            if (StringUtils.isBlank(bizAppNumber)) {
                bizAppNumber = MetadataDao.getAppNumberByEntityNumber(entityNumber);
            }

            if (StringUtils.isNotBlank(bizAppNumber)) {
                appId = String.format("%s.%s", "bos", bizAppNumber);
            }

            return appId;
        }
    }
}
