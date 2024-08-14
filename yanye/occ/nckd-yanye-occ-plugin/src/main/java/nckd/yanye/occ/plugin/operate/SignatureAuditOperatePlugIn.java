package nckd.yanye.occ.plugin.operate;

import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityType;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
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
import kd.bos.exception.KDBizException;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
签收单审核
 */

public class SignatureAuditOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_outstockqty");//出库数量
        e.getFieldKeys().add("nckd_signqty");//签收数量
        e.getFieldKeys().add("nckd_unableqty");//非合理途损数量
        e.getFieldKeys().add("nckd_srcbillentity");//来源单据实体
        e.getFieldKeys().add("nckd_sourcebillid");//来源单据id
        e.getFieldKeys().add("nckd_sourceentryid");//来源单据分录Id
    }

    /**
     * 操作校验执行完毕，开启事务保存单据之前，触发此事件
     * 可以在此事件，对单据数据包进行整理、取消操作
     */
    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);

        DynamicObject[] entities = e.getDataEntities();
        // 逐单处理
        for (DynamicObject dataEntity : entities) {
            DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("entryentity");
            if (!entryentity.isEmpty()) {
                //构造需要下推的数据 当签收数量大于出库数量 下推其他入库单
                List<ListSelectedRow> selectedRows = new ArrayList<>();
                List<ListSelectedRow> bhlselectedRows = new ArrayList<>();
                //Map<Object, BigDecimal> entryQtyMap = new HashMap<>();
                for (DynamicObject entryRowData : entryentity) {
                    String srcbillentity = entryRowData.getString("nckd_srcbillentity");//源单实体
                    if (!"im_saloutbill".equals(srcbillentity)) {
                        return;
                    }
                    BigDecimal outQty = entryRowData.getBigDecimal("nckd_outstockqty");//出库数量
                    BigDecimal signQty = entryRowData.getBigDecimal("nckd_signqty");//签收数量
                    BigDecimal unableQty = entryRowData.getBigDecimal("nckd_unableqty");//签收数量
                    if (signQty.compareTo(outQty) > 0) {
                        BigDecimal sourcebillId = entryRowData.getBigDecimal("nckd_sourcebillid");
                        BigDecimal sourceentryId = entryRowData.getBigDecimal("nckd_sourceentryid");
                        ListSelectedRow row = new ListSelectedRow();
                        //必填，设置源单单据id
                        row.setPrimaryKeyValue(sourcebillId);
                        //可选，设置源单分录标识
                        row.setEntryEntityKey("billentry");
                        //可选，设置源单分录id
                        row.setEntryPrimaryKeyValue(sourceentryId);
                        selectedRows.add(row);
                        //entryQtyMap.put(sourceentryId, signQty.subtract(outQty));
                    }
                    if (unableQty.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal sourcebillId = entryRowData.getBigDecimal("nckd_sourcebillid");
                        BigDecimal sourceentryId = entryRowData.getBigDecimal("nckd_sourceentryid");
                        ListSelectedRow row = new ListSelectedRow();
                        //必填，设置源单单据id
                        row.setPrimaryKeyValue(sourcebillId);
                        //可选，设置源单分录标识
                        row.setEntryEntityKey("billentry");
                        //可选，设置源单分录id
                        row.setEntryPrimaryKeyValue(sourceentryId);
                        bhlselectedRows.add(row);
                        //entryQtyMap.put(sourceentryId,signQty.subtract(outQty));
                    }
                }
                StringBuilder errMessage = new StringBuilder();
                //下推其他入库单
                if (!selectedRows.isEmpty()) {
                    String sourceBill = "im_saloutbill";//销售出库单
                    String targetBill = "im_otherinbill";//其他入库单
                    String ruleId = "2008890965632225280";//单据转换Id
                    // 创建下推参数
                    PushArgs pushArgs = new PushArgs();
                    // 必填，源单标识
                    pushArgs.setSourceEntityNumber(sourceBill);//销售出库单
                    // 必填，目标单标识
                    pushArgs.setTargetEntityNumber(targetBill);//其他入库单
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
                    // 必选，设置需要下推的源单及分录内码
                    pushArgs.setSelectedRows(selectedRows);
                    // 调用下推引擎，下推目标单
                    ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
                    // 判断下推是否成功，如果失败，提取失败消息
                    if (!pushResult.isSuccess()) {
                        errMessage.append("下推其他入库失败:" + pushResult.getMessage());    // 错误信息
//                        for (SourceBillReport billReport : pushResult.getBillReports()) {
//                            // 提取各单错误报告
//                            if (!billReport.isSuccess()) {
//                                String billMessage = billReport.getFailMessage();
//                            }
//                        }

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
                //下推销售出库单
                if (!selectedRows.isEmpty()) {
                    String sourceBill = "im_saloutbill";//销售出库
                    String targetBill = "im_saloutbill";//销售出库
                    String ruleId = "2008896141185254400";//单据转换Id
                    // 创建下推参数
                    PushArgs pushArgs = new PushArgs();
                    // 必填，源单标识
                    pushArgs.setSourceEntityNumber(sourceBill);//销售出库单
                    // 必填，目标单标识
                    pushArgs.setTargetEntityNumber(targetBill);//其他入库单
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
                    // 必选，设置需要下推的源单及分录内码
                    pushArgs.setSelectedRows(selectedRows);
                    // 调用下推引擎，下推目标单
                    ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
                    // 判断下推是否成功，如果失败，提取失败消息
                    if (!pushResult.isSuccess()) {
                        errMessage.append("下推销售出库出错:" + pushResult.getMessage());    // 错误信息
//                        for (SourceBillReport billReport : pushResult.getBillReports()) {
//                            // 提取各单错误报告
//                            if (!billReport.isSuccess()) {
//                                String billMessage = billReport.getFailMessage();
//                            }
//                        }

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
                //下推销售退货
                if (!bhlselectedRows.isEmpty()) {
                    String sourceBill = "im_saloutbill";//销售出库
                    String targetBill = "im_saloutbill";//销售出库
                    String ruleId = "2010828900610867200";//单据转换Id
                    // 创建下推参数
                    PushArgs pushArgs = new PushArgs();
                    // 必填，源单标识
                    pushArgs.setSourceEntityNumber(sourceBill);//销售出库单
                    // 必填，目标单标识
                    pushArgs.setTargetEntityNumber(targetBill);//其他入库单
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
                    // 必选，设置需要下推的源单及分录内码
                    pushArgs.setSelectedRows(bhlselectedRows);
                    // 调用下推引擎，下推目标单
                    ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
                    // 判断下推是否成功，如果失败，提取失败消息
                    if (!pushResult.isSuccess()) {
                        errMessage.append("下推销售退货出错:" + pushResult.getMessage());   // 错误信息
//                        for (SourceBillReport billReport : pushResult.getBillReports()) {
//                            // 提取各单错误报告
//                            if (!billReport.isSuccess()) {
//                                String billMessage = billReport.getFailMessage();
//                            }
//                        }

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
                //销售出库单（承运商）
                if (!bhlselectedRows.isEmpty()) {
                    String sourceBill = "im_saloutbill";//销售出库
                    String targetBill = "im_saloutbill";//销售出库
                    String ruleId = "2008902063978713088";//单据转换Id
                    // 创建下推参数
                    PushArgs pushArgs = new PushArgs();
                    // 必填，源单标识
                    pushArgs.setSourceEntityNumber(sourceBill);//销售出库单
                    // 必填，目标单标识
                    pushArgs.setTargetEntityNumber(targetBill);//其他入库单
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
                    // 必选，设置需要下推的源单及分录内码
                    pushArgs.setSelectedRows(bhlselectedRows);
                    // 调用下推引擎，下推目标单
                    ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
                    // 判断下推是否成功，如果失败，提取失败消息
                    if (!pushResult.isSuccess()) {
                        errMessage.append("下推销售出库单（承运商）出错:" + pushResult.getMessage());    // 错误信息
//                        for (SourceBillReport billReport : pushResult.getBillReports()) {
//                            // 提取各单错误报告
//                            if (!billReport.isSuccess()) {
//                                String billMessage = billReport.getFailMessage();
//                            }
//                        }
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
                        else {
                            errMessage.append("提交销售出库(承运商)出错："+subResult.getMessage());
                        }
                    }
                }
                if (errMessage.length() > 0) {
                    throw new KDBizException(errMessage.toString());
                }
            }
        }
    }
}
