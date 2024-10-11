package nckd.yanye.occ.plugin.operate;

import kd.bos.bill.BillShowParameter;
import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityType;
import kd.bos.dataentity.utils.StringUtils;
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
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.form.IFormView;
import kd.bos.metadata.dao.MetadataDao;
import kd.bos.mvc.FormConfigFactory;
import kd.bos.mvc.SessionManager;
import kd.bos.mvc.bill.BillModel;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.util.*;

/*
采购入库单审核服务插件 审核时携带批号数据到销售出库单
表单标识：nckd_im_purinbill_ext
author:wgq
date:2024/08/22
 */
public class PurInBillAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("lot");//批号主档
        e.getFieldKeys().add("qty");//数量
        e.getFieldKeys().add("lotnumber");//批号
        e.getFieldKeys().add("producedate");//生产日期
        e.getFieldKeys().add("expirydate");//到期日期
        e.getFieldKeys().add("nckd_soentryid");//要货订单分录Id
        e.getFieldKeys().add("outownertype");//出库货主类型
        e.getFieldKeys().add("outowner");//出库货主
        e.getFieldKeys().add("outkeepertype");//出库保管者类型
        e.getFieldKeys().add("outkeeper");//出库保管者

    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        // 获取当前单据（下游单据）的主实体编码、单据内码

        DynamicObject[] deliverRecords = e.getDataEntities();
        if (deliverRecords != null) {
            //逐单处理
            for (DynamicObject dataObject : deliverRecords) {
                //分录Id集合
                HashSet<Object> soEntryIdList = new HashSet<>();
                //批号主档
                Map<Object, DynamicObject> lotMap = new HashMap<>();
                //数量
                Map<Object, BigDecimal> qtyMap = new HashMap<>();
                //批号
                Map<Object, String> lotNumberMap = new HashMap<>();
                //生产日期
                Map<Object, Object> producedateMap = new HashMap<>();
                //失效日期
                Map<Object, Object> expirydateMap = new HashMap<>();
                //仓库
                Map<Object,DynamicObject>stockMap=new HashMap<>();
                //出库货主类型
                Map<Object,String>outownertypeMap=new HashMap<>();
                //出库货主
                Map<Object,DynamicObject>outownerMap=new HashMap<>();
                //出库保管者类型
                Map<Object,String>outkeepertypeMap=new HashMap<>();
                //出库保管者
                Map<Object,DynamicObject>outkeeperMap=new HashMap<>();
                //物料明细单据体
                DynamicObjectCollection billentry = dataObject.getDynamicObjectCollection("billentry");
                for (DynamicObject entryRow : billentry) {
                    //数量
                    BigDecimal sl=entryRow.getBigDecimal("qty");
                    //批号主档
                    DynamicObject lot = entryRow.getDynamicObject("lot");
                    //批号
                    String lotNumber = entryRow.getString("lotNumber");
                    //生产日期
                    Object producedate = entryRow.getDate("producedate");
                    //到期日期
                    Object expirydate = entryRow.getDate("expirydate");
                    //仓库
                    DynamicObject stock=entryRow.getDynamicObject("warehouse");
                    //出库货主类型
                    String outownertype=entryRow.getString("outownertype");
                    //出库货主
                    DynamicObject outowner=entryRow.getDynamicObject("outowner");
                    //出库保管者类型
                    String outkeepertype=entryRow.getString("outkeepertype");
                    //出库保管者
                    DynamicObject outkeeper=entryRow.getDynamicObject("outkeeper");

                    //要货订单分录Id
                    Object soEntryId = entryRow.get("nckd_soentryid");
                    if (soEntryId != null && !soEntryId.toString().equalsIgnoreCase("0")) {
                        soEntryIdList.add(soEntryId);
                        qtyMap.put(soEntryId,sl);
                        lotMap.put(soEntryId, lot);
                        lotNumberMap.put(soEntryId, lotNumber);
                        producedateMap.put(soEntryId, producedate);
                        expirydateMap.put(soEntryId, expirydate);
                        stockMap.put(soEntryId,stock);
                        outownertypeMap.put(soEntryId,outownertype);
                        outownerMap.put(soEntryId,outowner);
                        outkeepertypeMap.put(soEntryId,outkeepertype);
                        outkeeperMap.put(soEntryId,outkeeper);
                    }
                }
                //查询发货单
                //表单标识
                String sourceBill = "ocococ_deliveryorder";//发货单
                String targetBill = "im_saloutbill";//销售出库单
                String ruleId = "1980516234571026432";//单据转换Id
                //查询字段
                String fieldkey = "id,entryentity.id entryId";
                //出库数量=0（未下推销售出库）
                QFilter qFilter = new QFilter("entryentity.corebillentryid", QCP.in, soEntryIdList)
                        .and("billstatus",QCP.equals,"C")
                        .and("entryentity.invqty",QCP.equals,0);
                QFilter[] filters = new QFilter[]{qFilter};
                DynamicObjectCollection saloutDycollec = QueryServiceHelper.query(sourceBill, fieldkey, filters);
                if (saloutDycollec.size() > 0) {
                    List<ListSelectedRow> selectedRows = new ArrayList<>();
                    for (DynamicObject saloutData : saloutDycollec) {
                        Object sourcebillId = saloutData.get("id");
                        Object sourceentryId = saloutData.get("entryId");
                        ListSelectedRow row = new ListSelectedRow();
                        //必填，设置源单单据id
                        row.setPrimaryKeyValue(sourcebillId);
                        //可选，设置源单分录标识
                        row.setEntryEntityKey("entryentity");
                        //可选，设置源单分录id
                        row.setEntryPrimaryKeyValue(sourceentryId);
                        selectedRows.add(row);
                    }
                    if(selectedRows.size()>0){
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
                            MainEntityType dt = EntityMetadataCache.getDataEntityType(targetBill);
                            String appId = getAppId(targetBill, dt);
                            // 设置单据显示参数
                            BillShowParameter para = new BillShowParameter();
                            para.setFormId(targetBill);
                            para.setPkId(0);
                            para.setAppId(appId);

                            // 创建单据配置
                            FormConfigFactory.createConfigInCurrentAppService(para);
                            // 获取单据页面视图
                            final SessionManager sm = SessionManager.getCurrent();
                            final IFormView formView = sm.getView(para.getPageId());
                            if (formView != null) {

                                // 设置视图应用id和数据模型
                                formView.getFormShowParameter().setAppId(appId);

                                formView.getModel().createNewData();

                                //formView.updateView();

                            }
                            BillModel mode = (BillModel) formView.getModel();
                            HashSet<Object> Ids = new HashSet<>();
                            //修改数量
                            for (DynamicObject obj : saveDynamicObject) {
                                Object pkId = obj.getPkValue();

                                mode.setPKValue(pkId);
                                mode.load(pkId);
                                DynamicObject dataObj = mode.getDataEntity();
                                DynamicObjectCollection entry = dataObj.getDynamicObjectCollection("billentry");
                                int row = 0;
                                for (DynamicObject entryRow : entry) {
                                    Object mainbillentryid = entryRow.get("mainbillentryid");//核心单据行Id
                                    BigDecimal qty = qtyMap.get(mainbillentryid);//数量
                                    String lot=lotNumberMap.get(mainbillentryid);//批号
                                    Object producedate=producedateMap.get(mainbillentryid);//生产日期
                                    Object expirydate=expirydateMap.get(mainbillentryid);//失效日期
                                    DynamicObject stock=stockMap.get(mainbillentryid);//仓库
                                    String outownertype=outownertypeMap.get(mainbillentryid);//出库货主类型
                                    DynamicObject outowner=outownerMap.get(mainbillentryid);//出库货主
                                    String outkeepertype=outkeepertypeMap.get(mainbillentryid);//出库保管者类型
                                    DynamicObject outkeeper=outkeeperMap.get(mainbillentryid);//出库保管者类型
                                    mode.setValue("qty", qty, row);
                                    mode.setValue("lotnumber", lot, row);
                                    mode.setValue("producedate", producedate, row);
                                    mode.setValue("expirydate", expirydate, row);
                                    mode.setValue("warehouse", stock, row);
                                    //mode.setValue("outownertype", outownertype, row);
                                    //mode.setValue("outowner", outowner, row);
                                    //mode.setValue("outkeepertype", outkeepertype, row);
                                    //mode.setValue("outkeeper", outkeeper, row);

                                    row++;
                                }
                                OperationResult saveOp = formView.invokeOperation("save");
                                if (saveOp.isSuccess()) {
                                    Ids.add(pkId);
                                }

                            }
                            formView.close();
                            if (Ids.size() > 0) {
                                OperateOption auditOption = OperateOption.create();
                                auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
                                auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
                                //提交
                                OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, Ids.toArray(), auditOption);
                                if (subResult.isSuccess()) {
                                    //审核
//                                    OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, Ids.toArray(), auditOption);
//                                    if(auditResult.isSuccess()){
//
//                                    }
                                }
                            }

                        }
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

