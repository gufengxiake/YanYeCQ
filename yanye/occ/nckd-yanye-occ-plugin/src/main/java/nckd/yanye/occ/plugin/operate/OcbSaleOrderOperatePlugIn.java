package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;

import java.math.BigDecimal;
import java.util.*;


/**
 * 单据操作插件
 *  要货订单提交时,计算分摊金额
 */
public class OcbSaleOrderOperatePlugIn extends AbstractOperationServicePlugIn  {



    /**
     * 操作执行前，准备加载单据数据之前，触发此事件
     * 插件可以在此事件中，指定需要加载的字段
     */
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("resultseq");//促销结果组号
        e.getFieldKeys().add("orderentryid");//要货订单分录Id
        e.getFieldKeys().add("approveqty");//批准数量
        e.getFieldKeys().add("nckd_amount");//测试金额
        e.getFieldKeys().add("nckd_apportionamount");//分摊金额
        e.getFieldKeys().add("taxamount");//价税合计
    }

    /**
     * 操作校验执行完毕，开启事务保存单据之前，触发此事件
     * 可以在此事件，对单据数据包进行整理、取消操作
     */
    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);

        DynamicObject[] entities = e.getDataEntities();
        // 逐单处理
        for(DynamicObject dataEntity : entities){
            //按促销明细的促销结果组号分组,获取商品明细的分录Id集合
            Map<Integer, HashSet<Object>> orderEntryIdList=new HashMap<>();
            //促销明细单据体
            DynamicObjectCollection promotion_entry=dataEntity.getDynamicObjectCollection("promotion_entry");
            for(DynamicObject proRow:promotion_entry){
                int resultSeq=proRow.getInt("resultseq");//促销结果组号
                Object orderEntryId=proRow.get("orderentryid");//要货订单Id
                if(!orderEntryIdList.containsKey(resultSeq)){
                    orderEntryIdList.put(resultSeq,new HashSet<>());
                }
                orderEntryIdList.get(resultSeq).add(orderEntryId);
            }
            if(!orderEntryIdList.isEmpty()){
                //商品明细单据体
                DynamicObjectCollection itemEntry=dataEntity.getDynamicObjectCollection("itementry");
                //遍历分录Id集合
                for(HashSet<Object>  entryIds:orderEntryIdList.values()){
                    if(!entryIds.isEmpty()){
                        BigDecimal taxAmountCount=BigDecimal.ZERO;//价税合计
                        BigDecimal amountCount=BigDecimal.ZERO;//测试金额合计
                        List<DynamicObject>objectList=new ArrayList<>();
                        for(Object entryId:entryIds){
                            //商品明细分录
                            DynamicObject entryRow=itemEntry.stream()
                                    .filter(dynamicObject -> entryId.equals(dynamicObject.getPkValue()))
                                    .findFirst()
                                    .orElse(null);
                            objectList.add(entryRow);
                            BigDecimal taxAmount=entryRow.getBigDecimal("taxamount");
                            taxAmountCount=taxAmountCount.add(taxAmount);
                            BigDecimal amount=entryRow.getBigDecimal("nckd_amount");
                            amountCount=amountCount.add(amount);
                        }
                        if(!amountCount.equals(BigDecimal.ZERO)){
                            //再次遍历计算分摊金额
                            for(DynamicObject entryRow:objectList){
                                BigDecimal amount=entryRow.getBigDecimal("nckd_amount");
                                BigDecimal ratio=amount.divide(amountCount,10,BigDecimal.ROUND_HALF_UP);
                                BigDecimal apportionAmount=taxAmountCount.multiply(ratio).setScale(6,BigDecimal.ROUND_HALF_UP);
                                entryRow.set("nckd_apportionamount",apportionAmount);
                            }
                        }


                    }
                }
            }

        }

    }


}