package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        DynamicObject[] salOrders = e.getDataEntities();
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
}
