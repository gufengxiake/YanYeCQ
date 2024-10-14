package nckd.yanye.occ.plugin.operate;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.List;

/**
 * 派车信息单保存新增车辆和司机
 * 表单标识：nckd_im_transdirbill_ext
 * author:吴国强 2024-09-12
 */
public class VehicledispSaveOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("nckd_plateno");//车牌
        e.getFieldKeys().add("nckd_drivername");//司机姓名
        e.getFieldKeys().add("nckd_idcardno");//身份证号
        e.getFieldKeys().add("nckd_telephone");//手机号
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if (deliverRecords != null) {
            for (DynamicObject dataObject : deliverRecords) {
                //车牌
                String plateno=dataObject.getString("nckd_plateno");
                String drivername=dataObject.getString("nckd_drivername");
                String idcardno=dataObject.getString("nckd_idcardno");
                String telephone=dataObject.getString("nckd_telephone");
                if(!plateno.trim().equalsIgnoreCase("")){
                    //查找当前车牌是否存在
                    // 构造QFilter
                    QFilter qFilter = new QFilter("name", QCP.equals, plateno);
                    DynamicObjectCollection collections = QueryServiceHelper.query("nckd_vehicle",
                            "id", qFilter.toArray(), "");
                    if(collections.isEmpty()){
                        //不存在，新增
                        this.createVehicle(plateno);
                    }
                }
                if(!idcardno.trim().equalsIgnoreCase("")){
                    //查找当前司机身份证是否存在
                    // 构造QFilter
                    QFilter qFilter = new QFilter("nckd_idcardno", QCP.equals, idcardno);
                    DynamicObjectCollection collections = QueryServiceHelper.query("nckd_driver",
                            "id", qFilter.toArray(), "");
                    if(collections.isEmpty()){
                        //不存在，新增
                        this.createDriver(drivername,idcardno,telephone);
                    }
                }
            }
        }
    }

    private void createVehicle(String plateno){
        String targetBill="nckd_vehicle";
        DynamicObject vehicle = BusinessDataServiceHelper.newDynamicObject(targetBill);
        DynamicObject org=BusinessDataServiceHelper.loadSingle(RequestContext.get().getOrgId(),"bos_org");
        vehicle.set("createorg",org);
        //vehicle.set("org",org);
        vehicle.set("name",plateno);
        //数据状态
        vehicle.set("status","A");
        //可用状态
        vehicle.set("enable","1");

        //保存
        OperationResult operationResult1 = SaveServiceHelper.saveOperate(targetBill,new DynamicObject[]{vehicle} , OperateOption.create());
        if (operationResult1.isSuccess()) {
            List<Object> idList= operationResult1.getSuccessPkIds();
            OperateOption auditOption = OperateOption.create();
            auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
            auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
            //提交

            OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, idList.toArray(new Object[0]), auditOption);
            if (subResult.isSuccess()) {
                //审核
                OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, idList.toArray(new Object[0]), auditOption);
                if(auditResult.isSuccess()){

                }
            }
        }
    }

    private void createDriver(String drivername,String idcardno,String telephone){
        String targetBill="nckd_driver";
        DynamicObject driver = BusinessDataServiceHelper.newDynamicObject(targetBill);
        driver.set("name",drivername);
        driver.set("nckd_idcardno",idcardno);
        driver.set("nckd_phonenumber",telephone);
        driver.set("status","A");
        driver.set("enable","1");
        //保存
        OperationResult operationResult1 = SaveServiceHelper.saveOperate(targetBill,new DynamicObject[]{driver} , OperateOption.create());
        if (operationResult1.isSuccess()) {
            List<Object> idList= operationResult1.getSuccessPkIds();
            OperateOption auditOption = OperateOption.create();
            auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
            auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
            //提交
            OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, idList.toArray(new Object[0]), auditOption);
            if (subResult.isSuccess()) {
                //审核
                OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, idList.toArray(new Object[0]), auditOption);
                if(auditResult.isSuccess()){

                }
            }
        }
    }
}
