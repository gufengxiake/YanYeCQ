package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

import java.math.BigDecimal;
import java.util.*;


/**
 * 单据操作插件
 * 要货订单提交时,计算分摊金额
 */
public class OcbSaleOrderOperatePlugIn extends AbstractOperationServicePlugIn {


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
        e.getFieldKeys().add("standardamount");//标准金额
        e.getFieldKeys().add("nckd_price");//分摊单价
        //e.getFieldKeys().add("nckd_apportionamount");//分摊金额
        e.getFieldKeys().add("taxamount");//价税合计
        e.getFieldKeys().add("billtypeid");//单据类型
        e.getFieldKeys().add("saleorgid");//销售组织
        e.getFieldKeys().add("departmentid");//销售部门
        e.getFieldKeys().add("sub_warehouseid1");//发货仓库
        e.getFieldKeys().add("sub_warehouseid");//
        e.getFieldKeys().add("nckd_autosign");//自动签收
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
        for (DynamicObject dataEntity : entities) {

            //计算分摊金额------开始
            //按促销明细的促销结果组号分组,获取商品明细的分录Id集合
            Map<Integer, HashSet<Object>> orderEntryIdList = new HashMap<>();
            //促销明细单据体
            DynamicObjectCollection promotion_entry = dataEntity.getDynamicObjectCollection("promotion_entry");
            for (DynamicObject proRow : promotion_entry) {
                int resultSeq = proRow.getInt("resultseq");//促销结果组号
                Object orderEntryId = proRow.get("orderentryid");//要货订单Id
                if (!orderEntryIdList.containsKey(resultSeq)) {
                    orderEntryIdList.put(resultSeq, new HashSet<>());
                }
                orderEntryIdList.get(resultSeq).add(orderEntryId);
            }
            if (!orderEntryIdList.isEmpty()) {
                //商品明细单据体
                DynamicObjectCollection itemEntry = dataEntity.getDynamicObjectCollection("itementry");
                //遍历分录Id集合
                for (HashSet<Object> entryIds : orderEntryIdList.values()) {
                    if (!entryIds.isEmpty()) {
                        BigDecimal taxAmountCount = BigDecimal.ZERO;//价税合计
                        BigDecimal amountCount = BigDecimal.ZERO;//测试金额合计
                        List<DynamicObject> objectList = new ArrayList<>();
                        for (Object entryId : entryIds) {
                            //商品明细分录
                            DynamicObject entryRow = itemEntry.stream()
                                    .filter(dynamicObject -> entryId.equals(dynamicObject.getPkValue()))
                                    .findFirst()
                                    .orElse(null);
                            objectList.add(entryRow);
                            BigDecimal taxAmount = entryRow.getBigDecimal("taxamount");
                            taxAmountCount = taxAmountCount.add(taxAmount);
                            BigDecimal amount = entryRow.getBigDecimal("standardamount");
                            amountCount = amountCount.add(amount);
                        }
                        if (!amountCount.equals(BigDecimal.ZERO)) {
                            //再次遍历计算分摊金额
                            for (DynamicObject entryRow : objectList) {
                                BigDecimal amount = entryRow.getBigDecimal("standardamount");
                                BigDecimal qty = entryRow.getBigDecimal("approveqty");//批准数量
                                BigDecimal ratio = amount.divide(amountCount, 10, BigDecimal.ROUND_HALF_UP);
                                BigDecimal apportionAmount = taxAmountCount.multiply(ratio).setScale(6, BigDecimal.ROUND_HALF_UP);
                                entryRow.set("nckd_apportionamount", apportionAmount);//分摊金额
                                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                                    BigDecimal apportionPrice = apportionAmount.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
                                    entryRow.set("nckd_price", apportionPrice);//分摊单价
                                }
                            }
                        }


                    }
                }
            }
            //计算分摊金额-----结束

            //携带仓库信息---开始
            this.setStock(dataEntity);
            //携带仓库信息---结束

            //设置是否发货记录自动签收--开始
            this.setAutoSign(dataEntity);
            //设置是否发货记录自动签收--结束


        }

    }

    private void setStock(DynamicObject dataEntity) {
        DynamicObject billType = dataEntity.getDynamicObject("billtypeid");//单据类型
        if (billType != null) {
            String name = billType.getString("name");
            Object id = billType.getPkValue();
            if (name.equalsIgnoreCase("车销订单") || id.toString().equalsIgnoreCase("100000")) {
                //车销订单默认仓库，为当前组织指定的默认借货仓
                DynamicObject org = dataEntity.getDynamicObject("saleorgid");
                Object orgId = org.getPkValue();
                // 构造QFilter
                QFilter qFilter = new QFilter("createorg", QCP.equals, orgId)
                        .and("status", QCP.equals, "C")
                        .and("nckd_isjh", QCP.equals, "1");

                //查找借货仓
                DynamicObjectCollection collections = QueryServiceHelper.query("bd_warehouse",
                        "id,number", qFilter.toArray(), "audittime");
                if (!collections.isEmpty()) {
                    DynamicObject stockItem = collections.get(0);
                    String stockId = stockItem.getString("id");
                    DynamicObject stock= BusinessDataServiceHelper.loadSingle(stockId,"bd_warehouse");
                    DynamicObjectCollection plane_entry=dataEntity.getDynamicObjectCollection("plane_entry");
                    for(DynamicObject planRow:plane_entry){
                        planRow.set("sub_warehouseid1",stock);
                    }
                    DynamicObjectCollection itementry=dataEntity.getDynamicObjectCollection("itementry");
                    for(DynamicObject itemObj:itementry){
                        DynamicObjectCollection subentryentity=itemObj.getDynamicObjectCollection("subentryentity");
                        for(DynamicObject sub:subentryentity){
                            sub.set("sub_warehouseid",stock);
                        }
                    }
                }

            } else if (name.equalsIgnoreCase("赊销订单") || name.equalsIgnoreCase("访销订单") || name.equalsIgnoreCase("自提订单")) {
                // 赊销订单、访销订单、自提订单 按部门设置对应仓库
                DynamicObject org = dataEntity.getDynamicObject("saleorgid");
                Object orgId = org.getPkValue();
                //销售部门
                DynamicObject dept = dataEntity.getDynamicObject("departmentid");
                if (dept != null) {
                    Object deptId = dept.getPkValue();

                    //从部门 仓库设置基础资料中获取对应仓库
                    // 构造QFilter
                    QFilter qFilter = new QFilter("createorg", QCP.equals, orgId)
                            .and("status", QCP.equals, "C")
                            .and("nckd_bm", QCP.equals, deptId);

                    //查找部门对应仓库
                    DynamicObjectCollection collections = QueryServiceHelper.query("nckd_bmcksz",
                            "id,nckd_ck.id stockId", qFilter.toArray(), "modifytime");
                    if (!collections.isEmpty()) {
                        DynamicObject stockItem = collections.get(0);
                        String stockId = stockItem.getString("stockId");
                        DynamicObject stock= BusinessDataServiceHelper.loadSingle(stockId,"bd_warehouse");
                        DynamicObjectCollection plane_entry=dataEntity.getDynamicObjectCollection("plane_entry");
                        for(DynamicObject planRow:plane_entry){
                            planRow.set("sub_warehouseid1",stock);
                        }
                        DynamicObjectCollection itementry=dataEntity.getDynamicObjectCollection("itementry");
                        for(DynamicObject itemObj:itementry){
                            DynamicObjectCollection subentryentity=itemObj.getDynamicObjectCollection("subentryentity");
                            for(DynamicObject sub:subentryentity){
                                sub.set("sub_warehouseid",stock);
                            }
                        }
                    }
                }
            }
        }
    }

    private void setAutoSign(DynamicObject dataEntity){
        DynamicObject billType = dataEntity.getDynamicObject("billtypeid");//单据类型
        if (billType != null) {
            String name = billType.getString("name");
            Object id = billType.getPkValue();
            if("车销订单".equalsIgnoreCase(name)||"自提订单".equalsIgnoreCase(name)||"企业寄售订单".equalsIgnoreCase(name)){
                dataEntity.set("nckd_autosign",true);
            }
        }
    }


}