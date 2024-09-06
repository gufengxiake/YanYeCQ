package nckd.yanye.scm.plugin.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperateErrorInfo;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.validate.ValidateResult;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.util.CollectionUtils;
import nckd.yanye.scm.common.utils.FactoryApiUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :制造云-生产任务管理-生产工单(新)
 * Description      :定时任务通过物料-业务处理对应单生成负库存物料检查单
 *
 * @author : yaosijie
 * @date : 2024/8/27
 */
public class BussprocessorderTask extends AbstractTask {

    private static Map<String,String> map = new HashMap<>();

    private static Map<String,String> getMap(){
        map.put("1","早班");
        map.put("2","中班");
        map.put("3","晚班");
        map.put("4","不分班次");
        return map;
    }

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        QFilter qFilter = new QFilter("nckd_datasources", QCP.equals,"2")
                .and("billstatus",QCP.equals,"C");
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("nckd_bussprocessorder", "id,billno,org,nckd_inventoryclosedate," +
                "nckd_invcountschemeno,nckd_datasources,nckd_bussinessentries.nckd_materielfield,nckd_bussinessentries.nckd_teamsgroups,nckd_bussinessentries.nckd_iscumulative" +
                ",nckd_bussinessentries.nckd_quantity,nckd_bussinessentries.nckd_parameter,nckd_bussinessentries.nckd_isinventory," +
                "nckd_bussinessentries.nckd_inventoryorg,nckd_bussinessentries.nckd_warehouse,nckd_bussinessentries.nckd_businessdocument," +
                "nckd_bussinessentries.nckd_sideproduct,nckd_bussinessentries.nckd_mainproduce,nckd_bussinessentries.nckd_useworkshop," +
                "nckd_bussinessentries.nckd_wareorderworkshop,nckd_bussinessentries.nckd_illustrate,nckd_datefield", new QFilter[]{qFilter});
        //构建map,key:物料生产信息id，value:5G工厂返回数量
//        Map<Object,BigDecimal> bigDecimalMap = new HashMap<>();
        for (DynamicObject dynamicObject : dynamicObjects){
            Date date = new Date();
            DynamicObjectCollection dynamicObjectCollection = dynamicObject.getDynamicObjectCollection("nckd_bussinessentries");
            //构造调用5G接口的参数
            List<String> stringList = new ArrayList<>();
            //构造是否累计值为true的参数
            List<String> cumList = new ArrayList<>();
            /**
             *  "1"：早班;
             *  "2"：中班;
             *  "3"：晚班;
             *  "4"：不分班次;
             */
            //构造map,key:物料生产信息id,value：5G智能工厂系统入参
//            Map<Object,String> stringMap = new HashMap<>();
            for (DynamicObject dynamic : dynamicObjectCollection){
                String parmeter = dynamic.getString("nckd_parameter").replaceFirst("#","");
                if (!"4".equals(dynamic.getString("nckd_teamsgroups"))){
                    parmeter = parmeter + "," + getMap().get(dynamic.getString("nckd_teamsgroups"));
                }
                if (dynamic.getBoolean("nckd_iscumulative")){
                    String parameter = dynamic.getString("nckd_parameter").replaceFirst("#","");
                    //如果班组是早班并且是否为累计值为true时，需要构造当前数据班组为晚班的数据
                    if (ObjectUtil.equal("1",dynamic.getString("nckd_teamsgroups"))){
                        String culparameter = parameter+ "," + getMap().get("3");
                        cumList.add(culparameter);
                        //班组为不分班次并且是否为累计值为true时，需要构造当前数据班组为不分班次的数据
                    }else if (ObjectUtil.equal("4",dynamic.getString("nckd_teamsgroups"))){
//                        String culparameter = parameter;
                        cumList.add(parameter);
                        //如果班组是晚班并且是否为累计值为true时，需要构造当前数据班组为晚班的数据
                    }else if (ObjectUtil.equal("3",dynamic.getString("nckd_teamsgroups"))){
                        String culparameter = parameter+","+getMap().get("3");
                        cumList.add(culparameter);
                    }

                }
                stringList.add(parmeter);
//                stringMap.put(dynamic.getDynamicObject("nckd_materielfield").getPkValue(),dynamic.getString("nckd_parameter").replace("#","")
//                        +","+getMap().get(dynamic.getString("nckd_teamsgroups")));
            }
            //昨天的日期，用于调用5G工厂接口昨天晚班的数据
            Date datefield = dynamicObject.getDate("nckd_datefield");
            if (null == datefield){
                datefield = new Date();
            }
            Date dateTime = DateUtil.offsetDay(datefield,-1);
            Date yesterday = DateUtil.offsetDay(datefield,-2);
            //调用5G工厂接口：所有物料
            JSONObject resultJson = FactoryApiUtil.getFactoryInfo(stringList,dateTime);
            JSONArray jsonArray = resultJson.getJSONArray("data");
            Map<String, BigDecimal> dataMap = new HashMap<>();
            Map<String, BigDecimal> cumdataMap = new HashMap<>();
            for (int i = 0;i<jsonArray.size();i++){
                dataMap.put(jsonArray.getJSONObject(i).getString("indexName"),jsonArray.getJSONObject(i).getBigDecimal("dataValue"));
            }
            //判断返回的参数是否有为空的数据，有则记录日志
            List<String> msg = new ArrayList<>();
            for (Map.Entry<String,BigDecimal> entry: dataMap.entrySet()){
                if (null == entry.getValue()){
                    msg.add(entry.getKey());
                }
            }
            if (CollectionUtils.isNotEmpty(msg)){
                OperationResult operation = new OperationResult();
                operation.setSuccess(false);
                operation.setMessage("分录中5G智能工厂系统入参字段："
                        +msg.stream().collect(Collectors.joining(","))+"调用5G工厂接口查询参数dataValue为null");
                saveLog(operation,dynamicObject);
                continue;
            }
            //调用5G工厂接口：是否累计值为true的物料
            if (CollectionUtils.isNotEmpty(cumList)){
                JSONObject cumresultJson = FactoryApiUtil.getFactoryInfo(cumList,yesterday);
                JSONArray cumjsonArray = cumresultJson.getJSONArray("data");
                for (int i = 0;i<cumjsonArray.size();i++){
                    cumdataMap.put(cumjsonArray.getJSONObject(i).getString("indexName"),cumjsonArray.getJSONObject(i).getBigDecimal("dataValue"));
                }
            }
            //判断返回的参数是否有为空的数据，有则记录日志
            List<String> yestodayMsg = new ArrayList<>();
            for (Map.Entry<String,BigDecimal> entry: cumdataMap.entrySet()){
                if (null == entry.getValue()){
                    yestodayMsg.add(entry.getKey());
                }
            }
            if (CollectionUtils.isNotEmpty(yestodayMsg)){
                OperationResult operation = new OperationResult();
                operation.setSuccess(false);
                operation.setMessage("分录中5G智能工厂系统入参字段："
                        + yestodayMsg.stream().collect(Collectors.joining(","))+"调用5G工厂接口查询返回昨日参数dataValue为null");
                saveLog(operation,dynamicObject);
                continue;
            }

            /**
             * 计算早班的累计值，早班的值减去前一天晚班的值
             * 计算中班的累计值，当天中班的值减去当天早班班的值
             * 计算晚班的累计值，当天晚班的值减去当天中班的值
             */
            //回写物料中的数量
            dynamicObjectCollection.forEach(t-> {
                String parameter = t.getString("nckd_parameter").replaceFirst("#","");
                if (t.getBoolean("nckd_iscumulative") && ObjectUtil.equal("1",t.getString("nckd_teamsgroups"))){
                    //班组  3:晚班   1:早班   2:中班
                    BigDecimal decimal = cumdataMap.get(parameter + "," + getMap().get("3"));
                    t.set("nckd_quantity", dataMap.get(parameter + "," + getMap().get("1")).subtract(decimal));
                } else if (t.getBoolean("nckd_iscumulative") && ObjectUtil.equal("2",t.getString("nckd_teamsgroups"))) {
                    BigDecimal decimal = dataMap.get(parameter + "," + getMap().get("1"));
                    t.set("nckd_quantity", dataMap.get(parameter + "," + getMap().get("2")).subtract(decimal));
                } else if (t.getBoolean("nckd_iscumulative") && ObjectUtil.equal("3",t.getString("nckd_teamsgroups"))) {
                    BigDecimal decimal = dataMap.get(parameter + "," + getMap().get("2"));
                    if (decimal == null){
                        decimal = cumdataMap.get(parameter + "," + getMap().get("3"));
                    }
                    t.set("nckd_quantity", dataMap.get(parameter + "," + getMap().get("3")).subtract(decimal));
                }else if (t.getBoolean("nckd_iscumulative") && ObjectUtil.equal("4",t.getString("nckd_teamsgroups"))){
                    BigDecimal decimal = cumdataMap.get(parameter);
                    t.set("nckd_quantity", dataMap.get(parameter).subtract(decimal));
                }else {
                    t.set("nckd_quantity", dataMap.get(parameter));
                }
            });
//            stringMap.forEach((key,value)->{
//                bigDecimalMap.put(key,dataMap.get(value));
//            });
            //更新
            SaveServiceHelper.update(dynamicObject);
            //调用生产负库存的操作(负库存需要判断是5G工厂类型还是默认类型)  操作按钮：generate
            OperationResult operationResult = saveNegainventoryOrder(date,dynamicObject);
            //插入日志表
            saveLog(operationResult,dynamicObject);

        }

    }

    public OperationResult saveNegainventoryOrder(Date date,DynamicObject dynamicObject) {
        //新增负库存物料检查单
        DynamicObject negainventoryOrder = BusinessDataServiceHelper.newDynamicObject("nckd_negainventoryorder");

        String yyyyMMdd = DateUtil.format(date, "yyyyMMdd");
        int i = RandomUtil.randomInt(10000);
        String code = String.format("%04d", ++i);
        negainventoryOrder.set("billno", "FKCWLJCD-" + yyyyMMdd + "-" + code);
        int month = DateUtil.month(date) + 1;
        negainventoryOrder.set("nckd_periodnumber", month);
        negainventoryOrder.set("nckd_inventoryclosedate", date);
//        negainventoryOrder.set("nckd_invcountschemeno", schemenumber);
        negainventoryOrder.set("billstatus", "A");
        long currUserId = RequestContext.get().getCurrUserId();
        negainventoryOrder.set("creator", currUserId);
        negainventoryOrder.set("createtime", new Date());
        negainventoryOrder.set("modifier", currUserId);
        negainventoryOrder.set("modifytime", new Date());
        negainventoryOrder.set("nckd_datasources", dynamicObject.getString("nckd_datasources"));

        //分录赋值
        DynamicObjectCollection negainventoryOrderEntryColl = negainventoryOrder.getDynamicObjectCollection("nckd_negainentries");
        DynamicObjectCollection bussProcessOrderEntryColl = dynamicObject.getDynamicObjectCollection("nckd_bussinessentries");
        bussProcessOrderEntryColl.forEach(e -> {
            DynamicObject negainventoryOrderEntry = negainventoryOrderEntryColl.addNew();
            //这一部分直接由物料-业务处理对应单 分录带过来
            DynamicObject nckdMaterielfield = e.getDynamicObject("nckd_materielfield");
            negainventoryOrderEntry.set("nckd_teamsgroups", e.getString("nckd_teamsgroups"));
            negainventoryOrderEntry.set("nckd_materielfield", nckdMaterielfield);
            negainventoryOrderEntry.set("nckd_materiel", nckdMaterielfield.getDynamicObject("masterid"));
            negainventoryOrderEntry.set("nckd_isinventory", e.getBoolean("nckd_isinventory"));
            negainventoryOrderEntry.set("nckd_inventoryorg", e.getDynamicObject("nckd_inventoryorg"));
            DynamicObject nckdWarehouse = e.getDynamicObject("nckd_warehouse");
            negainventoryOrderEntry.set("nckd_warehouse", nckdWarehouse);
            negainventoryOrderEntry.set("nckd_businessdocument", e.getString("nckd_businessdocument"));
            negainventoryOrderEntry.set("nckd_sideproduct", e.getString("nckd_sideproduct"));
            negainventoryOrderEntry.set("nckd_mainproduce", e.getDynamicObject("nckd_mainproduce"));
            negainventoryOrderEntry.set("nckd_useworkshop", e.getDynamicObject("nckd_useworkshop"));
            negainventoryOrderEntry.set("nckd_wareorderworkshop", e.getDynamicObject("nckd_wareorderworkshop"));
            negainventoryOrderEntry.set("nckd_illustrate", e.getString("nckd_illustrate"));
            //根据物料生产信息查询物料信息
            DynamicObject material = BusinessDataServiceHelper.loadSingle(nckdMaterielfield.getDynamicObject("masterid").getPkValue(),"bd_material");
            negainventoryOrderEntry.set("nckd_number", e.get("nckd_quantity"));
            negainventoryOrderEntry.set("nckd_unitofmeasurement", material.get("baseunit"));
            negainventoryOrderEntry.set("nckd_basicunitnumber", e.get("nckd_quantity"));
            negainventoryOrderEntry.set("nckd_basicunit", material.getDynamicObject("baseunit"));
        });
        //调用保存操作
        OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "nckd_negainventoryorder", new DynamicObject[]{negainventoryOrder}, OperateOption.create());
        if (!saveOperationResult.isSuccess()) {
            return saveOperationResult;
        } else {
            //提交审批
            OperationResult submit = OperationServiceHelper.executeOperate("submit", "nckd_negainventoryorder", new Object[]{negainventoryOrder.getPkValue()}, OperateOption.create());
            if(!submit.isSuccess()){
                OperationServiceHelper.executeOperate("delete", "nckd_negainventoryorder", new Object[]{negainventoryOrder.getPkValue()}, OperateOption.create());
                return submit;
            }
            OperationResult audit = OperationServiceHelper.executeOperate("audit", "nckd_negainventoryorder", new Object[]{negainventoryOrder.getPkValue()}, OperateOption.create());
            if(!audit.isSuccess()){
                //已提交的数据需要先撤销提交再执行删除操作
                OperationServiceHelper.executeOperate("unsubmit", "nckd_negainventoryorder", new Object[]{negainventoryOrder.getPkValue()}, OperateOption.create());
                OperationServiceHelper.executeOperate("delete", "nckd_negainventoryorder", new Object[]{negainventoryOrder.getPkValue()}, OperateOption.create());
                return audit;
            }
        }
        return saveOperationResult;
    }

    public void saveLog(OperationResult operationResult,DynamicObject dynamicObject){
        DynamicObject log = BusinessDataServiceHelper.newDynamicObject("nckd_log");
        log.set("nckd_iserror",!operationResult.isSuccess());//是否异常
        log.set("nckd_upstream_doc",dynamicObject.getString("billno"));//上游单据编号
        log.set("creator", RequestContext.get().getCurrUserId());
        log.set("createtime", new Date());
        log.set("modifier", RequestContext.get().getCurrUserId());
        log.set("modifytime", new Date());
        String msg = operationResult.getMessage();
        List<String> errorMsgList = new ArrayList<>();
        if (!operationResult.isSuccess()){
            //获取分录返回异常列表，需要把列表中的数据整合成string
            List<ValidateResult> validateResults = operationResult.getValidateResult().getValidateErrors();
            for (ValidateResult validateResult : validateResults){
                String key = validateResult.getValidatorKey();//操作类型
                String errorMsg = validateResult.getAllErrorInfo().stream().map(OperateErrorInfo::getMessage).collect(Collectors.joining(","));
                errorMsgList.add("操作类型"+key+"，异常信息："+errorMsg);
            }
        }
        log.set("nckd_errorremark",msg+errorMsgList.stream().collect(Collectors.joining(",")));//描述+分录异常描述
        OperationServiceHelper.executeOperate("save", "nckd_log", new DynamicObject[]{log}, OperateOption.create());

    }
}
