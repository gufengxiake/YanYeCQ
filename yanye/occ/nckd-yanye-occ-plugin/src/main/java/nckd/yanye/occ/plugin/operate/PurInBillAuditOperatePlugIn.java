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
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/*
采购入库单审核服务插件
 */
public class PurInBillAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("lot");//批号主档
        e.getFieldKeys().add("lotnumber");//批号主档
        e.getFieldKeys().add("producedate");//生产日期
        e.getFieldKeys().add("expirydate");//到期日期
        e.getFieldKeys().add("nckd_soentryid");//要货订单分录Id

    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        // 获取当前单据（下游单据）的主实体编码、单据内码

        String targetEntityNumber = this.billEntityType.getName();

        Set<Object> billIds = new HashSet<>();

//        for (ExtendedDataEntity dataEntity : e.getValidExtDataEntities()) {
//
//            billIds.add(dataEntity.getBillPkId());
//
//        }
        DynamicObject[] deliverRecords = e.getDataEntities();
        if (deliverRecords != null) {
            //逐单处理
            for (DynamicObject dataObject : deliverRecords) {
                //分录Id集合
                HashSet<Object> soEntryIdList = new HashSet<>();
                Map<Object, DynamicObject> lotMap = new HashMap<>();
                Map<Object, String> lotNumberMap = new HashMap<>();
                Map<Object, Object> producedateMap = new HashMap<>();
                Map<Object, Object> expirydateMap = new HashMap<>();
                //物料明细单据体
                DynamicObjectCollection billentry = dataObject.getDynamicObjectCollection("billentry");
                for (DynamicObject entryRow : billentry) {
                    //批号主档
                    DynamicObject lot = entryRow.getDynamicObject("lot");
                    //批号
                    String lotNumber = entryRow.getString("lotNumber");
                    //生产日期
                    Object producedate = entryRow.getDate("producedate");
                    //到期日期
                    Object expirydate = entryRow.getDate("expirydate");
                    Object soEntryId = entryRow.get("nckd_soentryid");
                    if (soEntryId != null && !soEntryId.toString().equalsIgnoreCase("0")) {
                        soEntryIdList.add(soEntryId);
                        lotMap.put(soEntryId, lot);
                        lotNumberMap.put(soEntryId, lotNumber);
                        producedateMap.put(soEntryId, producedate);
                        expirydateMap.put(soEntryId, expirydate);
                    }
                }
                //查询销售出库单
                //表单标识
                String number = "im_saloutbill";
                //查询字段
                String fieldkey = "id";
                QFilter qFilter = new QFilter("billentry.mainbillentryid", QCP.in, soEntryIdList);
                QFilter[] filters = new QFilter[]{qFilter};
                DynamicObjectCollection saloutDycollec = QueryServiceHelper.query(number, fieldkey, filters);
                if (saloutDycollec.size() > 0) {
                    //HashSet<Object>Ids=new HashSet<>();
                    for (DynamicObject saloutData : saloutDycollec) {
                        Object id = saloutData.get("id");
                        DynamicObject saloutDyna = BusinessDataServiceHelper.loadSingle(id, number);
                        DynamicObjectCollection saloutbillentry = saloutDyna.getDynamicObjectCollection("billentry");
                        HashSet<DynamicObject> datalist = new HashSet<>();
                        for (DynamicObject salentryRow : saloutbillentry) {
                            Object mainbillentryid = salentryRow.get("mainbillentryid");
                            if (soEntryIdList.contains(mainbillentryid)) {
                                salentryRow.set("lot", lotMap.get(mainbillentryid));
                                salentryRow.set("lotnumber", lotNumberMap.get(mainbillentryid));
                                salentryRow.set("producedate", producedateMap.get(mainbillentryid));
                                salentryRow.set("expirydate", expirydateMap.get(mainbillentryid));
                                datalist.add(saloutDyna);
                            }

                        }
                        if (datalist.size() == 0) {
                            return;
                        }
                        DynamicObject[] saveDynamicObject = datalist.toArray(new DynamicObject[datalist.size()]);
                        //保存
                        OperationResult operationResult1 = SaveServiceHelper.saveOperate(number, saveDynamicObject, OperateOption.create());
                        if (operationResult1.isSuccess()) {
                            OperateOption auditOption = OperateOption.create();
                            auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
                            auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
                            //提交
                            OperationResult subResult = OperationServiceHelper.executeOperate("submit", number, saveDynamicObject, auditOption);
                            if (subResult.isSuccess()) {
                                //审核
                                OperationResult auditResult = OperationServiceHelper.executeOperate("audit", number, saveDynamicObject, auditOption);
                            }
                        }
                        //Ids.add(id);
                    }
//                        for(Object pk:Ids){
//                            DynamicObject saloutDyna= BusinessDataServiceHelper.loadSingle(pk,number);
//                        }

                }

            }

        }
    }

}

