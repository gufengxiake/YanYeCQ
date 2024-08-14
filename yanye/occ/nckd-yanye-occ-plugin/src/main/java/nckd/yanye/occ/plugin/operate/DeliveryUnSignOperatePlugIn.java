package nckd.yanye.occ.plugin.operate;

import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityType;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.botp.runtime.SourceBillReport;
import kd.bos.entity.datamodel.IRefrencedataProvider;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.exception.KDBizException;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.*;

/**
 * 发货记录拒收
 * 服务插件
 * author:吴国强 2024-07-12
 */
public class DeliveryUnSignOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        if (e.getValidExtDataEntities().isEmpty()) {

            return;

        }

        // 获取当前单据（下游单据）的主实体编码、单据内码

        String targetEntityNumber = this.billEntityType.getName();

        Set<Object> billIds = new HashSet<>();

        for (ExtendedDataEntity dataEntity : e.getValidExtDataEntities()) {
            billIds.add(dataEntity.getBillPkId());
        }


        // 调用平台的服务，获取所有源单及其内码

        Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, billIds.toArray(new Long[0]));


        // 从所有源单中寻找需要的demo_botpbill1

        HashSet<Long> botpbill1_Ids = new HashSet<>();
        String botpbill1_EntityNumber = "ocbsoc_saleorder";//要货订单

        if (sourceBillIds.containsKey(botpbill1_EntityNumber)) {

            botpbill1_Ids = sourceBillIds.get(botpbill1_EntityNumber);

        }

        //获取上游销售出库单的仓库信息
        HashSet<Long> saloutBillIds = new HashSet<>();
        Map<String, DynamicObject> stockIdMap = new HashMap<>();
        if (sourceBillIds.containsKey("im_saloutbill")) {
            saloutBillIds = sourceBillIds.get("im_saloutbill");
        }
        if (!saloutBillIds.isEmpty()) {
            for (Object salOutPk : saloutBillIds) {
                DynamicObject saloutBill = BusinessDataServiceHelper.loadSingle(salOutPk, "im_saloutbill");
                //获取单据体数据的集合
                DynamicObjectCollection goodsEntities = saloutBill.getDynamicObjectCollection("billentry");
                for (DynamicObject entryObj : goodsEntities) {
                    //获取某行数据的id
                    Object entryId = entryObj.getPkValue();
                    String mainbillentryid = entryObj.getString("mainbillentryid");//核心单据行Id
                    DynamicObject stock = entryObj.getDynamicObject("warehouse");
                    if (stock != null) {
                        stockIdMap.put(mainbillentryid, stock);
                    }

                }
            }
        }

        if (!botpbill1_Ids.isEmpty()) {
            String sourceBill = "ocbsoc_saleorder";//要货订单
            String targetBill = "ocbsoc_returnorder";//退货申请单
            String ruleId = "1985077720819698688";//单据转换Id
            //HashSet<DynamicObject>billDycoll=new HashSet<>();
            // TODO 已经获取到了源头的demo_botpbill1单据内码，可以进行后续处理
            for (Object pk : botpbill1_Ids) {

                //根据Id获取销售出库单实体
                DynamicObject saloutBill = BusinessDataServiceHelper.loadSingle(pk, botpbill1_EntityNumber);
                //执行销售出库下推财务应付
                //获取单据体数据的集合
                DynamicObjectCollection goodsEntities = saloutBill.getDynamicObjectCollection("itementry");

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
                    ListSelectedRow row = new ListSelectedRow();
                    //必填，设置源单单据id
                    row.setPrimaryKeyValue(pk);
                    //可选，设置源单分录标识
                    row.setEntryEntityKey("itementry");
                    //可选，设置源单分录id
                    row.setEntryPrimaryKeyValue(entryId);
                    selectedRows.add(row);
                }

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
                //修改仓库信息
                for (DynamicObject obj : saveDynamicObject) {
                    obj.set("nckd_unsign",true);
                    DynamicObjectCollection entry = obj.getDynamicObjectCollection("itementry");
                    for (DynamicObject entryRow : entry) {
                        String mainbillentryid = entryRow.getString("mainbillentryid");//核心单据行Id
                        DynamicObject stock = stockIdMap.get(mainbillentryid);
                        entryRow.set("stockid", stock);
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
                    }
                }

                //订单状态 拒签
                saloutBill.set("billstatus", "G");
                //签收状态 拒签
                saloutBill.set("signstatus", "E");
                SaveServiceHelper.save(new DynamicObject[]{saloutBill});
            }

        }
        for (DynamicObject dataEntity : e.getDataEntities()) {
            dataEntity.set("billstatus", "E");
            SaveServiceHelper.save(new DynamicObject[]{dataEntity});
        }
        if (!saloutBillIds.isEmpty()) {
            for (Object salOutPk : saloutBillIds) {
                DynamicObject saloutBill = BusinessDataServiceHelper.loadSingle(salOutPk, "im_saloutbill");
                saloutBill.set("nckd_unsign",true);
                SaveServiceHelper.save(new DynamicObject[]{saloutBill});
            }
        }


    }

}
