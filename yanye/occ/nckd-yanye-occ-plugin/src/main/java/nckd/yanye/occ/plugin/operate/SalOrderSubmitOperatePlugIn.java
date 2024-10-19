package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperateErrorInfo;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.entity.validate.ErrorLevel;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.util.*;

/**
 * 销售订单提交服务插件 更新表头预收金额
 * 根据物料辅助属性是否启用判断表体辅助属性字段是否需要填写
 * 表单标识：nckd_sm_salorder_ext
 * author:吴国强 2024-07-16
 */
public class SalOrderSubmitOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("r_needrecadvance");//是否预收
        e.getFieldKeys().add("nckd_amountfieldys");//预收金额
        e.getFieldKeys().add("recplanentry.r_unremainamount");//未关联收款金额
        e.getFieldKeys().add("billentry.material");//物料
        e.getFieldKeys().add("billentry.auxpty");//辅助属性
        e.getFieldKeys().add("billentry.priceandtax");//含税单价
        e.getFieldKeys().add("nckd_salecontractno");//销售合同
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] salOrders = e.getDataEntities();
        this.checkSalOrderAuxPty(salOrders);
//        this.checkSalOrderPriceAndTax(salOrders);
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        for (DynamicObject dataObject : deliverRecords) {
            DynamicObjectCollection recplanentry = dataObject.getDynamicObjectCollection("recplanentry");
            BigDecimal count = BigDecimal.ZERO;
            for (DynamicObject recplanRow : recplanentry) {
                Boolean ys=recplanRow.getBoolean("r_needrecadvance");
                if(ys){
                    BigDecimal r_remainamount = recplanRow.getBigDecimal("r_unremainamount");
                    count = count.add(r_remainamount);
                }
            }
            dataObject.set("nckd_amountfieldys", count);
            //
            SaveServiceHelper.update(new DynamicObject[]{dataObject});
        }
    }

    /**
     * 校验销售订单物料辅助属性是否需要填写
     * @param salOrders
     */
    public void checkSalOrderAuxPty(DynamicObject[] salOrders){
        Map<Long, Boolean> materialAuxPty = this.getMaterialAuxPty(salOrders);
        if (materialAuxPty.isEmpty()){
            return;
        }
        List<Object> seq = new ArrayList<>();
        for (DynamicObject dataObject : salOrders) {
            DynamicObjectCollection billentry = dataObject.getDynamicObjectCollection("billentry");
            for (DynamicObject row : billentry) {
                if(row.getDynamicObject("material") == null) {
                    continue;
                }
                long key = (long) row.getDynamicObject("material").getDynamicObject("masterid").getPkValue();
                if (materialAuxPty.containsKey(key) && materialAuxPty.get(key) && row.get("auxpty") == null){
                    seq.add(row.get("Seq"));
                }
            }
        }
        if (!seq.isEmpty()){
            throw new KDBizException("第"+ seq +"行物料启用辅助属性[等级]但是未录入,请填写!");
        }
    }
    /**
     * 获取物料是否启用辅助属性
     * @param salOrders
     * @return
     */
    public Map<Long,Boolean> getMaterialAuxPty(DynamicObject[] salOrders){
        Map<Long,Boolean> isAuxPty = new HashMap<>();
        List<Long> materialIds = new ArrayList<>();
        for (DynamicObject dataObject : salOrders) {
            DynamicObjectCollection billentry = dataObject.getDynamicObjectCollection("billentry");
            billentry.forEach((row) ->{
                if(row.getDynamicObject("material") != null){
                    materialIds.add((Long) row.getDynamicObject("material").getDynamicObject("masterid").getPkValue());
                }
            });
        }
        if(materialIds.isEmpty()){
            return isAuxPty;
        }
        QFilter mFilter = new QFilter("id", QCP.in,materialIds.toArray(new Long[0]));
        DynamicObjectCollection bdMaterial = QueryServiceHelper.query("bd_material", "id,isuseauxpty", new QFilter[]{mFilter});
        if (bdMaterial != null) {
            bdMaterial.forEach((e)->{
                long key = e.getLong("id");
                if (e.getBoolean("isuseauxpty")){
                    isAuxPty.put(key,true);
                }else{
                    isAuxPty.put(key,false);
                }
            });
        }
        return isAuxPty;
    }

    public void checkSalOrderPriceAndTax(DynamicObject[] salOrders){
        Map<String,Map<Long,BigDecimal>> checkMaps = this.getSalContract(salOrders);
        for (DynamicObject salOrder : salOrders) {
            String nckdSalecontractno = salOrder.getString("nckd_salecontractno");
            if(nckdSalecontractno == null || checkMaps.containsKey(nckdSalecontractno)){
                continue;
            }
            Map<Long, BigDecimal> longBigDecimalMap = checkMaps.get(nckdSalecontractno);
            DynamicObjectCollection billentry = salOrder.getDynamicObjectCollection("billentry");
            for (DynamicObject dynamicObject : billentry) {
                long pkValue = (long) dynamicObject.getDynamicObject("material").getPkValue();
                BigDecimal priceandtax = dynamicObject.getBigDecimal("priceandtax");
                if(longBigDecimalMap.containsKey(pkValue) && priceandtax.compareTo(longBigDecimalMap.get(pkValue)) != 0){
                    OperateErrorInfo operateErrorInfo = new OperateErrorInfo("物料含税单价与销售合同不一致", ErrorLevel.Error,dynamicObject.getPkValue());
                    operateErrorInfo.setMessage("物料含税单价与销售合同不一致");
                    this.operationResult.addErrorInfo(operateErrorInfo);
                }
            }
        }

    }

    public Map<String,Map<Long,BigDecimal>> getSalContract(DynamicObject[] salOrders){
        Map<String,Map<Long,BigDecimal>> checkMaps = new HashMap<>();
        Set<String> contactNo = new HashSet<>();
        for (DynamicObject salOrder : salOrders) {
            if(salOrder.get("nckd_salecontractno") != null){
                contactNo.add(salOrder.getString("nckd_salecontractno"));
            }
        }
        if(contactNo.isEmpty()){
            return checkMaps;
        }
        QFilter qFilter = new QFilter("billno",QCP.in,checkMaps.keySet().toArray(new String[0]));
        DynamicObjectCollection conmSalcontract = QueryServiceHelper.query("conm_salcontract", "billno,billentry.material as material,billentry.priceandtax as priceandtax", new QFilter[]{qFilter});
        if (conmSalcontract == null){
            return checkMaps;
        }
        conmSalcontract.forEach((e)->{
            String billno = e.getString("billno");
            Map<Long,BigDecimal> map = new HashMap<>();
            map.put(e.getLong("material"),e.getBigDecimal("priceandtax"));
            checkMaps.put(billno,map);

        });
        return checkMaps;
    }


}
