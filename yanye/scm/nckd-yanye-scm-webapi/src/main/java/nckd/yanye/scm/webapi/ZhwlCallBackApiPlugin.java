package nckd.yanye.scm.webapi;

import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.tx.TX;
import kd.bos.db.tx.TXHandle;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.openapi.common.custom.annotation.*;
import kd.bos.openapi.common.result.CustomApiResult;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.PurapplybillConst;
import nckd.yanye.scm.common.dto.Content;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 对接智慧物流系统api
 * api编码：back/getStatus
 *
 * @author xiaoxiaopeng
 * @since 2024/09/10
 */
@ApiController(value = "zhwl", desc = "对接智慧物流")
@ApiMapping("/back")
public class ZhwlCallBackApiPlugin implements Serializable {

    @ApiPostMapping(value = "/getStatus", desc = "派车单实时状态获取")
    public CustomApiResult<Object> getCarStatus(
            @ApiParam(value = "单据编号", required = true) String billno,
            @ApiParam(value = "业务状态", required = true) String nckd_statu,
            @ApiParam(value = "业务类型", required = true) String billnotype
    ) {
        if (StringUtils.isEmpty(billnotype)){
            return CustomApiResult.fail("false","传入单据编号错误");
        }
        if (StringUtils.isEmpty(billno)){
            return CustomApiResult.fail("false","传入单据编号错误");
        }
        if (StringUtils.isEmpty(nckd_statu)){
            return CustomApiResult.fail("false","传入业务状态错误");
        }
        if ("1".equals(billnotype)){
            //销售发货单
            DynamicObject deliver = BusinessDataServiceHelper.loadSingle("sm_delivernotice", "id,billno,nckd_erpstatus",
                    new QFilter[]{new QFilter("billno", QCP.equals, billno)});
            if (deliver == null) {
                return CustomApiResult.fail("false","未找到对应单据");
            }
            deliver.set("nckd_erpstatus",nckd_statu);
            try {
                SaveServiceHelper.update(deliver);
                return CustomApiResult.success("success");
            }catch (Exception e) {
                return CustomApiResult.fail("false",e.getMessage());
            }
        } else if ("2".equals(billnotype)) {
            //调拨申请单
            DynamicObject tranSapply = BusinessDataServiceHelper.loadSingle("im_transapply", "id,billno,nckd_erpstatus",
                    new QFilter[]{new QFilter("billno", QCP.equals, billno)});
            if (tranSapply == null) {
                return CustomApiResult.fail("false","未找到对应单据");
            }
            tranSapply.set("nckd_erpstatus",nckd_statu);
            try {
                SaveServiceHelper.update(tranSapply);
                return CustomApiResult.success("success");
            }catch (Exception e) {
                return CustomApiResult.fail("false",e.getMessage());
            }
        }else {
            return CustomApiResult.fail("false","业务类型传值错误");
        }
    }

    private String setErpStatus(String nckdStatu) {
        switch (nckdStatu) {
            //未执行
            case "1":
                nckdStatu = "1";
                break;
                //进场
            case "2":
                //第一次过磅/第二次过磅
            case "3":
                //出厂
            case "4":
                nckdStatu = "2";
                break;
                //已完成
            case "5":
                nckdStatu = "3";
                break;
                //空车出厂
            case "6":
                nckdStatu = "4";
                break;
        }
        return nckdStatu;
    }

    @ApiPostMapping(value = "/setEleWeigh", desc = "同步电子磅单数据")
    public CustomApiResult<Object> setEleWeigh(
            @ApiParam(value = "业务类型", required = true) String nckd_billtype,
            @ApiParam(value = "派车单号") String nckd_carsysno,
            @ApiParam(value = "来源单据实体") String nckd_srcbillentity,
            @ApiParam(value = "来源单据ID") String nckd_orderid,
            @ApiParam(value = "单据日期") String nckd_date,
            @ApiParam(value = "司机") String nckd_driver,
            @ApiParam(value = "车辆号") String carno,
            @ApiParam(value = "来源单据ID(表体)") String nckd_srcbillid,
            @ApiParam(value = "发货仓库编码") String nckd_warehouse,
            @ApiParam(value = "入库仓库编码") String nckd_inwarehouse,
            @ApiParam(value = "毛重") BigDecimal nckd_grossweight,
            @ApiParam(value = "皮重") BigDecimal nckd_tare,
            @ApiParam(value = "净重") BigDecimal nckd_netweight,
            @ApiParam(value = "发货数量") BigDecimal nckd_qty,
            @ApiParam(value = "车皮号") String nckd_railwaywagon,
            @ApiParam(value = "客户名称") String nckd_customer,
            @ApiParam(value = "采样流水id") String nckd_waterid,
            @ApiParam(value = "采样桶号") String nckd_bucket,
            @ApiParam(value = "毛重时间") String nckd_grosstime
    ) {
        //业务类型
        if (Objects.isNull(nckd_billtype)){
            return CustomApiResult.fail("false","传入业务类型数据错误");
        }
        if("1".equals(nckd_billtype) || "2".equals(nckd_billtype)){
            //派车单号
            if (Objects.isNull(nckd_carsysno)){
                return CustomApiResult.fail("false","传入派车单号数据错误");
            }
            //来源单据ID
            if (Objects.isNull(nckd_orderid)){
                return CustomApiResult.fail("false","传入来源单据ID数据错误");
            }
            //来源单据实体
            if (Objects.isNull(nckd_srcbillentity)){
                return CustomApiResult.fail("false","传入来源单据实体数据错误");
            }
            //单据日期
            if (Objects.isNull(nckd_date)){
                return CustomApiResult.fail("false","传入单据日期数据错误");
            }
            //来源单据ID(表体)
            if (Objects.isNull(nckd_srcbillid)){
                return CustomApiResult.fail("false","传入来源单据ID(表体)数据错误");
            }
            //发货仓库编码
            if (Objects.isNull(nckd_warehouse)){
                return CustomApiResult.fail("false","传入发货仓库编码数据错误");
            }
            //入库仓库编码
            if (Objects.isNull(nckd_inwarehouse)){
                return CustomApiResult.fail("false","传入入库仓库编码数据错误");
            }
            //皮重
            if (Objects.isNull(nckd_tare)){
                return CustomApiResult.fail("false","传入皮重数据错误");
            }
            //净重
            if (Objects.isNull(nckd_netweight)){
                return CustomApiResult.fail("false","传入净重数据错误");
            }
            //发货数量
            if (Objects.isNull(nckd_qty)){
                return CustomApiResult.fail("false","传入发货数量数据错误");
            }
        }
        //司机
        if (Objects.isNull(nckd_driver)){
            return CustomApiResult.fail("false","传入司机数据错误");
        }
        //车辆号
        if (Objects.isNull(carno)){
            return CustomApiResult.fail("false","传入车辆号数据错误");
        }
        //毛重
        if (Objects.isNull(nckd_grossweight)){
            return CustomApiResult.fail("false","传入毛重数据错误");
        }

        //加事务
        try (TXHandle h = TX.required("setEleWeigh")) {
            try {
                //根据业务类型区分来源单据是发货还是转运1发货2转运
                if ("1".equals(nckd_billtype) && "发货通知单".equals(nckd_srcbillentity)){
                    DynamicObject deliver = BusinessDataServiceHelper.loadSingle(nckd_orderid, "sm_delivernotice");
                    if (deliver == null) {
                        return CustomApiResult.fail("false","传入来源单据ID数据错误,系统未查询到相关单据");
                    }
                    //判断是否重复生成电子磅单
                    DynamicObject pushEleweigh = BusinessDataServiceHelper.loadSingle("nckd_eleweighing", "id,nckd_orderid",
                            new QFilter[]{new QFilter("nckd_orderid", QCP.equals, nckd_orderid)});
                    if (pushEleweigh != null) {
                        return CustomApiResult.fail("false","传入来源单据ID数据重复,请勿重复下推");
                    }
                    //构建下推参数，下推电子磅单
                    PushArgs pushArgs = getPushArgs(deliver, "nckd_eleweighing");
                    // 调用下推引擎，下推目标单并保存
                    ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
                    if (pushResult.isSuccess()) {
                        MainEntityType mainEntityType = EntityMetadataCache.getDataEntityType(pushArgs.getTargetEntityNumber());
                        List<DynamicObject> targetDos = pushResult.loadTargetDataObjects(BusinessDataServiceHelper::loadRefence, mainEntityType);
                        DynamicObject eleweigh = targetDos.get(0);
                        DynamicObject driver = BusinessDataServiceHelper.loadSingle("nckd_driver","id,name",new QFilter[]{new QFilter("name",QCP.equals,nckd_driver)});
                        DynamicObject vehicle = BusinessDataServiceHelper.loadSingle( "nckd_vehicle","id,name",new QFilter[]{new QFilter("name",QCP.equals,carno)});
                        eleweigh.set("nckd_carsysno",nckd_carsysno);//派车单号
                        eleweigh.set("nckd_driver",driver);//司机
                        eleweigh.set("nckd_vehicle",vehicle);//车号
                        DynamicObjectCollection entryentity = eleweigh.getDynamicObjectCollection("entryentity");
                        DynamicObject entry = entryentity.get(0);
                        entry.set("nckd_srcbillid",nckd_srcbillid);//来源单据ID
                        entry.set("nckd_srcbillentity",nckd_srcbillentity);//来源单据主实体

                        OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "nckd_eleweighing", new DynamicObject[]{eleweigh}, OperateOption.create());
                        if (!saveOperationResult.isSuccess()){
                            return CustomApiResult.fail("false","生成电子磅单失败");
                        }
                        //SaveServiceHelper.update(eleweigh);
                        //构建下推参数，下推销售出库单
                        PushArgs pushArgs_a = getPushArgs(eleweigh, "im_saloutbill");
                        // 调用下推引擎，下推目标单并保存
                        ConvertOperationResult pushResult_a = ConvertServiceHelper.push(pushArgs_a);
                        if (!pushResult_a.isSuccess()){
                            return CustomApiResult.fail("false","生成销售出库单失败");
                        }
                        MainEntityType mainEntityType_a = EntityMetadataCache.getDataEntityType(pushResult_a.getTargetEntityNumber());
                        List<DynamicObject> targetDos_a = pushResult_a.loadTargetDataObjects(BusinessDataServiceHelper::loadRefence, mainEntityType_a);
                        DynamicObject saloutbill = targetDos_a.get(0);
                        OperationResult saveOperationResult_a = OperationServiceHelper.executeOperate("save", "im_saloutbill", new DynamicObject[]{saloutbill}, OperateOption.create());
                        if (!saveOperationResult_a.isSuccess()){
                            return CustomApiResult.fail("false","生成销售出库单失败");
                        }
                        return CustomApiResult.success("success");

                    }else {
                        return CustomApiResult.fail("false","生成电子磅单失败");
                    }

                } else if ("2".equals(nckd_billtype) && "调拨申请单".equals(nckd_srcbillentity)) {
                    DynamicObject tranSapply = BusinessDataServiceHelper.loadSingle(nckd_orderid, "im_transapply");
                    if (tranSapply == null) {
                        return CustomApiResult.fail("false","传入来源单据ID数据错误,系统未查询到相关单据");
                    }
                    //构建下推参数，下推电子磅单
                    PushArgs pushArgs = getPushArgs(tranSapply, "nckd_eleweighing");
                    // 调用下推引擎，下推目标单并保存
                    ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
                    if (pushResult.isSuccess()) {
                        MainEntityType mainEntityType = EntityMetadataCache.getDataEntityType(pushArgs.getTargetEntityNumber());
                        List<DynamicObject> targetDos = pushResult.loadTargetDataObjects(BusinessDataServiceHelper::loadRefence, mainEntityType);
                        DynamicObject eleweigh = targetDos.get(0);
                        DynamicObject driver = BusinessDataServiceHelper.loadSingle("nckd_driver","id,name",new QFilter[]{new QFilter("name",QCP.equals,nckd_driver)});
                        DynamicObject vehicle = BusinessDataServiceHelper.loadSingle( "nckd_vehicle","id,name",new QFilter[]{new QFilter("name",QCP.equals,carno)});
                        eleweigh.set("nckd_carsysno",nckd_carsysno);//派车单号
                        eleweigh.set("nckd_driver",driver);//司机
                        eleweigh.set("nckd_vehicle",vehicle);//车号
                        eleweigh.set("nckd_railwaywagon",nckd_railwaywagon);//车皮号
                        DynamicObjectCollection entryentity = eleweigh.getDynamicObjectCollection("entryentity");
                        DynamicObject entry = entryentity.get(0);
                        entry.set("nckd_srcbillid",nckd_srcbillid);//来源单据ID
                        entry.set("nckd_srcbillentity",nckd_srcbillentity);//来源单据主实体

                        //SaveServiceHelper.update(eleweigh);
                        OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "nckd_eleweighing", new DynamicObject[]{eleweigh}, OperateOption.create());
                        if (!saveOperationResult.isSuccess()){
                            return CustomApiResult.fail("false","生成电子磅单失败");
                        }
                        //构建下推参数，下推销售出库单
                        PushArgs pushArgs_a = getPushArgs(eleweigh, "im_transdirbill");
                        // 调用下推引擎，下推目标单并保存
                        ConvertOperationResult pushResult_a = ConvertServiceHelper.push(pushArgs_a);
                        if (!pushResult_a.isSuccess()){
                            return CustomApiResult.fail("false","生成直接调拨单失败");
                        }
                        MainEntityType mainEntityType_a = EntityMetadataCache.getDataEntityType(pushResult_a.getTargetEntityNumber());
                        List<DynamicObject> targetDos_a = pushResult_a.loadTargetDataObjects(BusinessDataServiceHelper::loadRefence, mainEntityType_a);
                        DynamicObject saloutbill = targetDos_a.get(0);
                        OperationResult saveOperationResult_a = OperationServiceHelper.executeOperate("save", "im_transdirbill", new DynamicObject[]{saloutbill}, OperateOption.create());
                        if (!saveOperationResult_a.isSuccess()){
                            return CustomApiResult.fail("false","生成直接调拨单失败");
                        }
                        return CustomApiResult.success("success");

                    }else {
                        return CustomApiResult.fail("false","生成电子磅单失败");
                    }

                } else if ("3".equals(nckd_billtype)) {
                    //客户名称
                    if (Objects.isNull(nckd_customer)){
                        return CustomApiResult.fail("false","业务类型为采购时，客户名称必填");
                    }
                    //采样流水id
                    if (Objects.isNull(nckd_waterid)){
                        return CustomApiResult.fail("false","业务类型为采购时，采样流水id必填");
                    }
                    //采样桶号
                    if (Objects.isNull(nckd_bucket)){
                        return CustomApiResult.fail("false","业务类型为采购时，采样桶号必填");
                    }
                    //毛重时间
                    if (Objects.isNull(nckd_grosstime)){
                        return CustomApiResult.fail("false","业务类型为采购时，毛重时间必填");
                    }
                    //生成电子磅单
                    DynamicObject eleweighing = BusinessDataServiceHelper.newDynamicObject("nckd_eleweighing");
                    CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(eleweighing.getDataEntityType().getName(), eleweighing, null);
                    String number = CodeRuleServiceHelper.getNumber(codeRule, eleweighing);
                    //DynamicObject billType = BusinessDataServiceHelper.loadSingle("bos_billtype", "id", new QFilter[]{new QFilter("id", QCP.equals, "2057962852278348800")});
                    //DynamicObject supplier = BusinessDataServiceHelper.loadSingle("bd_supplier", "id,name", new QFilter[]{new QFilter("name", QCP.equals, nckd_customer)});
                    DynamicObject vehicle = BusinessDataServiceHelper.loadSingle( "nckd_vehicle","id,name",new QFilter[]{new QFilter("name",QCP.equals,carno)});
                    DynamicObject driver = BusinessDataServiceHelper.loadSingle("nckd_driver","id,name",new QFilter[]{new QFilter("name",QCP.equals,nckd_driver)});
                    DynamicObject material = BusinessDataServiceHelper.loadSingle("bd_material", "id,name", new QFilter[]{new QFilter("name", QCP.equals, "石灰石（60一120mm）")});
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    DynamicObject supplierControl = BusinessDataServiceHelper.loadSingle("nckd_supplier_control", "id,nckd_entryentity,nckd_entryentity.nckd_suppliername,nckd_entryentity.nckd_supplier", new QFilter[]{new QFilter("nckd_entryentity.nckd_suppliername", QCP.equals, nckd_customer)});

                    eleweighing.set("billno",number);
                    eleweighing.set("billstatus","A");
                    eleweighing.set("org",1994936229107338240L);
                    eleweighing.set("nckd_stockorg",1994936229107338240L);
                    eleweighing.set("nckd_billtype",2057962852278348800L);
                    eleweighing.set("nckd_currency",1);
                    eleweighing.set("nckd_settlecurrency",1);
                    eleweighing.set("nckd_paymode","CREDIT");
                    eleweighing.set("nckd_bucket",nckd_bucket);
                    eleweighing.set("nckd_waterid",nckd_waterid);
                    eleweighing.set("nckd_vehicle",vehicle);//车号
                    eleweighing.set("nckd_driver",driver);//司机
                    eleweighing.set("nckd_statu","A");
                    eleweighing.set("nckd_remarks",carno);
                    Date date = null;
                    try {
                        date =  simpleDateFormat.parse(nckd_grosstime);
                        eleweighing.set("nckd_date",date);
                    }catch (Exception e){
                        eleweighing.set("nckd_date",date);
                    }
                    if (supplierControl == null){
                        eleweighing.set("nckd_qygys",null);
                    }else {
                        DynamicObject supplier = supplierControl.getDynamicObjectCollection("nckd_entryentity").size() > 0 ? supplierControl.getDynamicObjectCollection("nckd_entryentity").get(0).getDynamicObject("nckd_supplier") : null;
                        eleweighing.set("nckd_supplier",supplier);
                    }
                    DynamicObjectCollection entryentity = eleweighing.getDynamicObjectCollection("entryentity");
                    DynamicObject entry = entryentity.addNew();
                    entry.set("nckd_materiel",material);
                    entry.set("nckd_unit",307797538013076480L);
                    entry.set("nckd_baseunit",307797538013076480L);
                    entry.set("nckd_qty",nckd_grossweight);
                    OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "nckd_eleweighing", new DynamicObject[]{eleweighing}, OperateOption.create());
                    if (!saveOperationResult.isSuccess()){
                        return CustomApiResult.fail("false","生成电子磅单失败");
                    }
                    return CustomApiResult.success("success");
                } else {
                    return CustomApiResult.fail("false","传入业务类型和来源单据实体数据错误,系统未查询到相关单据");
                }

            } catch (Throwable e) {
                h.markRollback();
                throw e;
            }
        }
    }

    private PushArgs getPushArgs(DynamicObject srcObj, String formbillid) {
        Long pkid = (long) srcObj.getPkValue();

        List<ListSelectedRow> selectedRows = new ArrayList<>();
        ListSelectedRow selectedRow = new ListSelectedRow(pkid);
        selectedRows.add(selectedRow);

        // 生成下推参数PushArgs
        PushArgs pushArgs = new PushArgs();
        // 必选，源单标识
        pushArgs.setSourceEntityNumber(srcObj.getDataEntityType().getName());
        // 必选，目标单标识
        pushArgs.setTargetEntityNumber(formbillid);
        // 可选，自动保存
        pushArgs.setAutoSave(false);
        // 可选，设置单据转换规则的id，如果没有设置，会自动匹配一个规则进行转换
//        pushArgs.setRuleId("1134727974310918144");
        // 是否输出详细错误报告
        pushArgs.setBuildConvReport(true);
        // 必选，设置需要下推的源单及分录内码
        pushArgs.setSelectedRows(selectedRows);
        return pushArgs;
    }
}
