package nckd.yanye.occ.plugin.operate;

import cn.hutool.core.date.DateUtil;
import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityType;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.IRefrencedataProvider;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.util.*;

/**
 * 签收单审核审核服务插件
 * 表单标识：nckd_signaturebill
 * author:吴国强 2024-07-12
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
        e.getFieldKeys().add("nckd_materiel");
        e.getFieldKeys().add("nckd_signdate");
        e.getFieldKeys().add("org");
    }

    /**
     * 操作校验执行完毕，开启事务保存单据之前，触发此事件
     * 可以在此事件，对单据数据包进行整理、取消操作
     */
    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);

        DynamicObject[] entities = e.getDataEntities();
        Map<Long, Boolean> materialIsMoreSalt = this.getMaterialIsMoreSalt(entities);
        // 逐单处理
        for (DynamicObject dataEntity : entities) {
            DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("entryentity");
            if (!entryentity.isEmpty()) {
                //构造需要下推的数据 当签收数量大于出库数量 下推其他入库单
                List<ListSelectedRow> selectedRows = new ArrayList<>();
                List<ListSelectedRow> bhlselectedRows = new ArrayList<>();
                //Map<Object, BigDecimal> entryQtyMap = new HashMap<>();
                boolean isMoreSalt = false;
                for (DynamicObject entryRowData : entryentity) {
                    String srcbillentity = entryRowData.getString("nckd_srcbillentity");//源单实体
                    if (!"im_saloutbill".equals(srcbillentity)) {
                        return;
                    }
                    BigDecimal outQty = entryRowData.getBigDecimal("nckd_outstockqty");//出库数量
                    BigDecimal signQty = entryRowData.getBigDecimal("nckd_signqty");//签收数量
                    BigDecimal unableQty = entryRowData.getBigDecimal("nckd_unableqty");//签收数量
                    if (signQty.compareTo(outQty) > 0) {
                        Object sourcebillId = entryRowData.get("nckd_sourcebillid");
                        Object sourceentryId = entryRowData.get("nckd_sourceentryid");
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
                        Object sourcebillId = entryRowData.get("nckd_sourcebillid");
                        Object sourceentryId = entryRowData.get("nckd_sourceentryid");
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
                    DynamicObject nckdMateriel = entryRowData.getDynamicObject("nckd_materiel");
                    if (nckdMateriel != null && !materialIsMoreSalt.isEmpty()
                            && materialIsMoreSalt.containsKey((Long)nckdMateriel.getPkValue())
                            && materialIsMoreSalt.get((Long)nckdMateriel.getPkValue())){
                        isMoreSalt = true;
                    }
                }
                StringBuilder errMessage = new StringBuilder();
                //物料勾选长吨盐走推完工入库逻辑
                if(!selectedRows.isEmpty() && isMoreSalt){
                    this.pushProductIn(dataEntity,errMessage);
                } else if (!selectedRows.isEmpty() ) { //下推其他入库单
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
                //入库出问题则不允许生成出库单
                if (errMessage.length() > 0) {
                    throw new KDBizException(errMessage.toString());
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
                if (errMessage.length() > 0) {
                    throw new KDBizException(errMessage.toString());
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
                    if (pushResult.isSuccess()) {
                        // 获取生成的目标单数据包
                        MainEntityType targetMainType = EntityMetadataCache.getDataEntityType(targetBill);
                        List<DynamicObject> targetBillObjs = pushResult.loadTargetDataObjects(new IRefrencedataProvider() {
                            @Override
                            public void fillReferenceData(Object[] objs, IDataEntityType dType) {
                                BusinessDataReader.loadRefence(objs, dType);
                            }
                        }, targetMainType);
                        DynamicObject[] saveDynamicObject = targetBillObjs.toArray(new DynamicObject[targetBillObjs.size()]);
                        //将承运商转成客户赋值到出库单的收货客户
                        for (DynamicObject data : saveDynamicObject) {
                            //承运商
                            DynamicObject supplier = data.getDynamicObject("nckd_carcustomer");
                            if (supplier != null) {
                                //商务伙伴
                                DynamicObject bizpartner = supplier.getDynamicObject("bizpartner");
                                if (bizpartner != null) {
                                    Object bizpartnerId = bizpartner.getPkValue();
                                    //根据商务伙伴查找对应的客户
                                    // 构造QFilter
                                    QFilter qFilter = new QFilter("bizpartner.id", QCP.equals, bizpartnerId).and("status", QCP.equals, "C");
                                    // 将选中的id对应的数据从数据库加载出来
                                    DynamicObjectCollection collections = QueryServiceHelper.query("bd_customer",
                                            "id", qFilter.toArray(), "");
                                    if (!collections.isEmpty()) {
                                        DynamicObject customer = collections.get(0);
                                        String customerId = customer.getString(("id"));
                                        DynamicObject customerDyna = BusinessDataServiceHelper.loadSingle(customerId, "bd_customer");
                                        data.set("customer", customerDyna);
                                    }
                                }
                            }
                        }
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
                            } else {
                                errMessage.append("提交销售出库(承运商)出错：" + subResult.getMessage());
                            }
                        }
                    } else {
                        errMessage.append("下推销售出库单（承运商）出错:" + pushResult.getMessage());    // 错误信息
//                        for (SourceBillReport billReport : pushResult.getBillReports()) {
//                            // 提取各单错误报告
//                            if (!billReport.isSuccess()) {
//                                String billMessage = billReport.getFailMessage();
//                            }
//                        }
                    }

                }
                if (errMessage.length() > 0) {
                    throw new KDBizException(errMessage.toString());
                }
            }
        }
    }


    /**
     * 推完工入库单
     * @param dataEntity
     * @param errMessage
     */
    public void pushProductIn(DynamicObject dataEntity,StringBuilder errMessage){
        DynamicObject mftOrder = this.getMFTOrder(dataEntity);
        if (mftOrder == null) {
            errMessage.append("未找到对应生产工单!");
            return;
        }
        List<ListSelectedRow> selectedRows = new ArrayList<>();
        Object sourcebillId = mftOrder.get("id");
        Object sourceentryId = mftOrder.get("treeentryentity.id");
        ListSelectedRow row = new ListSelectedRow();
        //必填，设置源单单据id
        row.setPrimaryKeyValue(sourcebillId);
        //可选，设置源单分录标识
        row.setEntryEntityKey("treeentryentity");
        //可选，设置源单分录id
        row.setEntryPrimaryKeyValue(sourceentryId);
        selectedRows.add(row);
        String sourceBill = "pom_mftorder";//生产工单
        String targetBill = "im_mdc_mftmanuinbill";//完工入库
        String ruleId = "2027130505631116288";//单据转换Id
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
        // 必选，设置需要下推的源单及分录内码
        pushArgs.setSelectedRows(selectedRows);
        // 调用下推引擎，下推目标单
        ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
        // 判断下推是否成功，如果失败，提取失败消息
        if (!pushResult.isSuccess()) {
            errMessage.append("下推完工入库失败:" + pushResult.getMessage());    // 错误信息
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
        DynamicObject dynamicObject = dataEntity.getDynamicObjectCollection("entryentity").get(0);
        long nckdSourceentryid = dynamicObject.getLong("nckd_sourcebillid");
        //根据签收单来源单据id找销售出库单
        DynamicObject imSaloutbill = BusinessDataServiceHelper.loadSingle(nckdSourceentryid,"im_saloutbill");
        //获取班次信息
        DynamicObject workshifts = BusinessDataServiceHelper.loadSingle("2066082303343345664","mpdm_workshifts");
        if (imSaloutbill != null) {
            DynamicObject salEntry = imSaloutbill.getDynamicObjectCollection("billentry").get(0);
            for (DynamicObject save : saveDynamicObject) {
                DynamicObjectCollection billentry = save.getDynamicObjectCollection("billentry");
                for (DynamicObject object : billentry) {
                    object.set("qty",salEntry.getBigDecimal("nckd_signqty").subtract(salEntry.getBigDecimal("qty")));
                    object.set("baseqty",salEntry.getBigDecimal("nckd_signbaseqty").subtract(salEntry.getBigDecimal("baseqty")));
                    object.set("receivalqty",salEntry.getBigDecimal("nckd_signbaseqty").subtract(salEntry.getBigDecimal("baseqty")));
                    object.set("lot",salEntry.get("lot"));
                    object.set("warehouse",salEntry.get("warehouse"));
                    object.set("location",salEntry.get("location"));
                    object.set("lotnumber",salEntry.getString("lotnumber"));
                    object.set("auxpty",salEntry.get("auxpty"));
                    object.set("shift",workshifts);
                }
                save.set("comment","长吨入库");
            }
        }
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
                for (DynamicObject object : saveDynamicObject) {
                    this.operationResult.setMessage("已生成单据号为: "+object.get("billno")+" 完工入库单");
                }

            }

        }

    }


    /**
     * 获取生产订单
     * @param dataEntity
     * @return
     */
    public DynamicObject getMFTOrder(DynamicObject dataEntity){
        DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("entryentity");
        DynamicObject dynamicObject = entryentity.get(0);
        DynamicObject nckdMateriel = dynamicObject.getDynamicObject("nckd_materiel");
        DynamicObject org = dataEntity.getDynamicObject("org");
        Date nckdSigndate = dataEntity.getDate("nckd_signdate");
        QFilter qFilter = new QFilter("treeentryentity.material.masterid",QCP.equals,nckdMateriel.getPkValue())
                .and("org",QCP.equals,org.getPkValue())
                .and("billdate",QCP.large_equals, DateUtil.beginOfMonth(nckdSigndate))
                .and("billdate",QCP.less_equals,DateUtil.endOfMonth(nckdSigndate))
                .and("billstatus",QCP.equals,"C");
        DynamicObject pomMftorder = QueryServiceHelper.queryOne("pom_mftorder", "id,treeentryentity.id", new QFilter[]{qFilter});
        if(pomMftorder == null){
            QFilter qFilter2 = new QFilter("treeentryentity.material.masterid",QCP.equals,nckdMateriel.getPkValue())
                    .and("org",QCP.equals,org.getPkValue())
//                    .and("billdate",QCP.not_equals2, null)
//                    .and("billdate",QCP.less_equals,DateUtil.endOfMonth(nckdSigndate))
                    .and("billstatus",QCP.equals,"C");
            DynamicObjectCollection query = QueryServiceHelper.query("pom_mftorder", "id,treeentryentity.id", new QFilter[]{qFilter2}, "billdate desc", 1);
            return query.isEmpty() ? null : (DynamicObject)query.get(0);
        }
        return pomMftorder;
    }

    /**
     * 获取物料是否为长吨盐
     * @param entities
     * @return
     */
    public Map<Long,Boolean> getMaterialIsMoreSalt(DynamicObject[] entities){
        Set<Long> materialIds = new HashSet<>();
        Map<Long,Boolean> isMoreSalt = new HashMap<>();
        for (DynamicObject entity : entities) {
            DynamicObjectCollection entryentity = entity.getDynamicObjectCollection("entryentity");
            if(entryentity.isEmpty()){
                continue;
            }
            entryentity.forEach((row)->{
                if (row.getDynamicObject("nckd_materiel") != null){
                    materialIds.add((Long) row.getDynamicObject("nckd_materiel").getPkValue());
                }
            });
        }
        if (materialIds.isEmpty()){
            return isMoreSalt;
        }
        QFilter qFilter = new QFilter("id",QCP.in,materialIds.toArray(new Long[0]));
        DynamicObjectCollection bdMaterial = QueryServiceHelper.query("bd_material", "id,nckd_ismoresalt", new QFilter[]{qFilter});
        bdMaterial.forEach((m)->{
            if(m.get("nckd_ismoresalt") != null){
                isMoreSalt.put(m.getLong("id"),m.getBoolean("nckd_ismoresalt"));
            }
        });
        return isMoreSalt;
    }

}
