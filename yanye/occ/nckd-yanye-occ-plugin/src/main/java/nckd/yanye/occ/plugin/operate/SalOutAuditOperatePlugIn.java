package nckd.yanye.occ.plugin.operate;

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
import kd.bos.metadata.dao.MetadataDao;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.mvc.FormConfigFactory;
import kd.bos.mvc.SessionManager;
import kd.bos.form.IFormView;
import kd.bos.mvc.bill.BillModel;

import java.math.BigDecimal;
import java.util.*;

/**
 * 销售出库单审核自动执行采购订单下推收料通知
 * 服务插件
 * author:吴国强 2024-07-12
 */
public class SalOutAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("mainbillentryid");//核心单据行Id
        e.getFieldKeys().add("qty");//数量

    }
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        if (e.getValidExtDataEntities().isEmpty()) {

            return;

        }


        // 获取当前单据的主实体编码、单据内码

        String targetEntityNumber = this.billEntityType.getName();

        Set<Object> billIds = new HashSet<>();

        for (ExtendedDataEntity dataEntity : e.getValidExtDataEntities()) {

            billIds.add(dataEntity.getBillPkId());

        }
        Map<String, BigDecimal> outQty = new HashMap<>();
        for (DynamicObject dataObject : e.getDataEntities()) {
            //获取单据体数据的集合
            DynamicObjectCollection billentry = dataObject.getDynamicObjectCollection("billentry");
            for (DynamicObject entryObj : billentry) {
                String mainbillentryid = entryObj.getString("mainbillentryid");//核心单据行Id
                BigDecimal qty = entryObj.getBigDecimal("qty");//核心单据行Id
                if (!outQty.containsKey(mainbillentryid)) {
                    outQty.put(mainbillentryid, qty);
                } else {

                    outQty.put(mainbillentryid, outQty.get(mainbillentryid).add(qty));
                }
            }
        }

        // 调用平台的服务，获取所有源单及其内码

        Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, billIds.toArray(new Long[0]));


        // 从所有源单中寻找需要的demo_botpbill1

        HashSet<Long> botpbill1_Ids = new HashSet<>();

        String botpbill1_EntityNumber = "pm_purorderbill";//采购订单

        if (sourceBillIds.containsKey(botpbill1_EntityNumber)) {

            botpbill1_Ids = sourceBillIds.get(botpbill1_EntityNumber);

        }

        //获取上游销售订单对应的采购订单分录
        HashSet<Long> saloutBillIds = new HashSet<>();
        if (sourceBillIds.containsKey("sm_salorder")) {
            saloutBillIds = sourceBillIds.get("sm_salorder");
        }
        if (!saloutBillIds.isEmpty()) {
            Map<String, BigDecimal> srcQty = new HashMap<>();
            for (Object salOutPk : saloutBillIds) {
                DynamicObject saloutBill = BusinessDataServiceHelper.loadSingle(salOutPk, "sm_salorder");
                //获取单据体数据的集合
                DynamicObjectCollection goodsEntities = saloutBill.getDynamicObjectCollection("billentry");
                for (DynamicObject entryObj : goodsEntities) {
                    //获取某行数据的id
                    Object entryId = entryObj.getPkValue();
                    String srcbillentryid = entryObj.getString("srcbillentryid");//来源单据行Id
                    if (outQty.containsKey(entryId.toString())) {
                        srcQty.put(srcbillentryid, outQty.get(entryId.toString()));
                    }
                }
            }
            if (!botpbill1_Ids.isEmpty()) {
                String sourceBill = "pm_purorderbill";//采购订单
                String targetBill = "pm_receiptnotice";//收货通知单
                String ruleId = "1946321539372681216";//单据转换Id
                // TODO 已经获取到了源头的demo_botpbill1单据内码，可以进行后续处理
                for (Object pk : botpbill1_Ids) {

                    //根据Id获取采购订单单实体
                    DynamicObject saloutBill = BusinessDataServiceHelper.loadSingle(pk, sourceBill);
                    //执行销售出库下推财务应付
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
                        ListSelectedRow row = new ListSelectedRow();
                        //必填，设置源单单据id
                        row.setPrimaryKeyValue(pk);
                        //可选，设置源单分录标识
                        row.setEntryEntityKey("billentry");
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

                            formView.updateView();

                        }
                        BillModel mode = (BillModel) formView.getModel();
                        HashSet<Object>Ids=new HashSet<>();
                        //修改数量
                        for (DynamicObject obj : saveDynamicObject) {
                            Object pkId=obj.getPkValue();

                            mode.setPKValue(pkId);
                            mode.load(pkId);
                            DynamicObject dataObj=mode.getDataEntity();
                            DynamicObjectCollection entry=dataObj.getDynamicObjectCollection("billentry");
                            int row=0;
                            for (DynamicObject entryRow : entry) {
                                String mainbillentryid = entryRow.getString("mainbillentryid");//核心单据行Id
                                BigDecimal qty = srcQty.get(mainbillentryid);
                                mode.setValue("qty",qty,row);
                                row++;
                            }
                            OperationResult saveOp= formView.invokeOperation("save");
                            if(saveOp.isSuccess()){
                                Ids.add(pkId);
                            }

                        }
                        formView.close();
                        if(Ids.size()>0){
                            OperateOption auditOption = OperateOption.create();
                            auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
                            auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
                            //提交
                            OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, Ids.toArray(), auditOption);
                            if (subResult.isSuccess()) {
                                //审核
                                OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, Ids.toArray(), auditOption);
                            }
                        }

                    }


                }
            }
        }
        //e.cancel=true;


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
