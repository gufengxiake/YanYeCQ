package nckd.yanye.occ.plugin.operate;

import kd.bos.algo.sql.tree.Query;
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
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * 派车信息单审核执行销售订单下推到发货通知单
 * 表单标识：nckd_im_transdirbill_ext
 * author:吴国强 2024-08-12
 */
public class VehicledispAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_plateno");//车牌
        e.getFieldKeys().add("nckd_drivername");//司机姓名
        e.getFieldKeys().add("nckd_idcardno");//身份证号
        e.getFieldKeys().add("nckd_telephone");//手机号
        e.getFieldKeys().add("nckd_saleorderno");//销售订单号
        e.getFieldKeys().add("nckd_qty");//派车数量
        e.getFieldKeys().add("billno");//单据编号
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if (deliverRecords != null) {
            String sourceBill = "sm_salorder";//销售订单
            String targetBill = "sm_delivernotice";//发货通知单
            for (DynamicObject dataObject : deliverRecords) {
                String billno=dataObject.getString("billno");
                //销售订单编码
                String saleOrderNo = dataObject.getString("nckd_saleorderno");
                //查找编码对应的Id
                QFilter filter = new QFilter("billno", QCP.equals, saleOrderNo)
                        .and("billstatus", QCP.equals, "C");
                DynamicObjectCollection saleCollection = QueryServiceHelper.query(sourceBill, "id", filter.toArray(), "");
                if (!saleCollection.isEmpty()) {
                    DynamicObject saleDataObject = saleCollection.get(0);
                    Object pkId = saleDataObject.get("id");
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
                    //pushArgs.setRuleId(ruleId);
                    //自动保存
                    //pushArgs.setAutoSave(true);
                    // 可选，是否输出详细错误报告
                    pushArgs.setBuildConvReport(true);
                    // 必填，设置需要下推的单据，或分录行
                    List<ListSelectedRow> selectedRows = new ArrayList<>();
                    ListSelectedRow row = new ListSelectedRow();
                    //必填，设置源单单据id
                    row.setPrimaryKeyValue(pkId);
                    //可选，设置源单分录标识
                    //row.setEntryEntityKey("billentry");
                    //可选，设置源单分录id
                    //row.setEntryPrimaryKeyValue(entryId);
                    selectedRows.add(row);
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
                        HashSet<Object> Ids = new HashSet<>();
                        //修改数量
                        for (DynamicObject obj : saveDynamicObject) {
                            Object targetBillpkId = obj.getPkValue();

                            mode.setPKValue(targetBillpkId);
                            mode.load(targetBillpkId);
                            //DynamicObject dataObj = mode.getDataEntity();
                            //根据车牌号获取车辆的Id
                            Object vehicleId=null;
                            String plateno=dataObject.getString("nckd_plateno");
                            QFilter vfilter = new QFilter("name", QCP.equals, plateno).and("status", QCP.equals, "C");
                            DynamicObjectCollection vehicle = QueryServiceHelper.query("nckd_vehicle",
                                    "id", vfilter.toArray(), "");
                            if (!vehicle.isEmpty()) {
                                DynamicObject vdataObject = vehicle.get(0);
                                vehicleId = vdataObject.get("id");
                            }
                            //根据身份证获取司机信息基础资料
                            Object driverId=null;
                            String idcardno=dataObject.getString("nckd_idcardno");
                            QFilter dFilter = new QFilter("nckd_idcardno", QCP.equals, idcardno).and("status", QCP.equals, "C");
                            DynamicObjectCollection driver = QueryServiceHelper.query("nckd_driver",
                                    "id", dFilter.toArray(), "");
                            if (!driver.isEmpty()) {
                                DynamicObject ddataObject = driver.get(0);
                                driverId = ddataObject.get("id");
                            }
                            BigDecimal qty=dataObject.getBigDecimal("nckd_qty");

                            mode.beginInit();
                            mode.setValue("nckd_vehicledisp",billno);
                            mode.setItemValueByID("nckd_vehicle",vehicleId);
                            mode.setItemValueByID("nckd_driver",driverId);
                            mode.endInit();
                            mode.setValue("qty",qty,0);
                            OperationResult saveOp = formView.invokeOperation("save");
                            if (saveOp.isSuccess()) {
                                Ids.add(pkId);
                            }

                        }
                        formView.close();
//                            if (Ids.size() > 0) {
//                                OperateOption auditOption = OperateOption.create();
//                                auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
//                                auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
//                                //提交
//                                OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, Ids.toArray(), auditOption);
//                                if (subResult.isSuccess()) {
//                                    //审核
//                                    OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, Ids.toArray(), auditOption);
//                                }
//                            }

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


