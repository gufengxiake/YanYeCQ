package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.formula.RowDataModel;
import kd.bos.entity.operate.result.OperateErrorInfo;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.entity.validate.AbstractValidator;
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

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new MaterialAuxPtyCheckValidator());
    }
    /**
     * 自定义操作校验器
     *
     * @author
     */
    static class MaterialAuxPtyCheckValidator extends AbstractValidator {

        /**
         * 返回校验器的主实体：系统将自动对此实体数据，逐行进行校验
         */
        @Override
        public String getEntityKey() {
            return this.entityKey;
        }

        /**
         * 给校验器传入上下文环境及单据数据包之后，调用此方法；
         *
         * @remark 自定义校验器，可以在此事件进行本地变量初始化：如确认需要校验的主实体
         */
        @Override
        public void initializeConfiguration() {
            super.initializeConfiguration();
            this.entityKey = "billentry";
        }

        /**
         * 校验器初始化完毕，从单据数据包中，提取出了主实体数据行，开始校验前，调用此方法；
         *
         * @remark 此方法，比initializeConfiguration更晚调用；
         * 在此方法调用this.getDataEntities()，可以获取到需校验的主实体数据行
         * 不能在本方法中，确认需要校验的主实体
         */
        @Override
        public void initialize() {
            super.initialize();
        }


        /**
         * 执行自定义校验
         */
        @Override
        public void validate() {
            Map<Long, Boolean> materialAuxPty = this.getMaterialAuxPty();
            if (materialAuxPty.isEmpty()){
                return;
            }
            RowDataModel rowDataModel = new RowDataModel(this.entityKey, this.getValidateContext().getSubEntityType());
            for (ExtendedDataEntity rowDataEntity : this.getDataEntities()) {
                rowDataModel.setRowContext(rowDataEntity.getDataEntity());
                if(rowDataModel.getValue("material") == null) {
                    continue;
                }
                DynamicObject material = ((DynamicObject) rowDataModel.getValue("material")).getDynamicObject("masterid");
                long key = (long) material.getPkValue();
                if (materialAuxPty.containsKey(key) && materialAuxPty.get(key) && rowDataModel.getValue("auxpty") == null){
                    this.addErrorMessage(rowDataEntity, "物料["+material.getString("number")+"]启用辅助属性[等级]但是未录入,请填写!");
                }
            }

        }
        /**
         * 获取物料是否启用辅助属性
         * @return
         */
        public Map<Long,Boolean> getMaterialAuxPty(){
            Map<Long,Boolean> isAuxPty = new HashMap<>();
            RowDataModel rowDataModel = new RowDataModel(this.entityKey, this.getValidateContext().getSubEntityType());
            List<Long> materialIds = new ArrayList<>();
            for (ExtendedDataEntity dataEntity : this.getDataEntities()) {
                rowDataModel.setRowContext(dataEntity.getDataEntity());
                if(rowDataModel.getValue("material") != null){
                    DynamicObject material = ((DynamicObject) rowDataModel.getValue("material")).getDynamicObject("masterid");
                    materialIds.add((Long) material.getPkValue());
                }
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
}
