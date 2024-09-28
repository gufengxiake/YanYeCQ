package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.DispatchServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.math.BigDecimal;
import java.util.*;

/**
 * 要货订单提交时,计算分摊金额
 * 表单标识：nckd_ocbsoc_saleorder_ext
 * author:吴国强 2024-07-16
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
        e.getFieldKeys().add("itemid");//商品
        e.getFieldKeys().add("orderchannelid");//订货渠道
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
            //商品明细单据体
            DynamicObjectCollection itemEntry = dataEntity.getDynamicObjectCollection("itementry");
            if (!orderEntryIdList.isEmpty()) {
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
                            if (entryRow != null) {
                                objectList.add(entryRow);
                                BigDecimal taxAmount = entryRow.getBigDecimal("taxamount");
                                taxAmountCount = taxAmountCount.add(taxAmount);
                                BigDecimal amount = entryRow.getBigDecimal("standardamount");
                                amountCount = amountCount.add(amount);
                            }
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

            //携带赠品分录物料对应的税率----开始
            this.setTaxrate(itemEntry);
            //携带赠品分录物料对应的税率----结束

            //携带订货渠道地址联系信息--开始
            this.setAddresSpane(dataEntity, itemEntry);
            //携带订货渠道地址联系信息--结束


            //携带仓库信息---开始
            this.setStock(dataEntity);
            //携带仓库信息---结束

            //设置是否发货记录自动签收--开始
            //this.setAutoSign(dataEntity);
            //设置是否发货记录自动签收--结束

            //SaveServiceHelper.update(dataEntity);

        }
    }


    private void setAddresSpane(DynamicObject dataEntity, DynamicObjectCollection itemEntry) {
        DynamicObject orderchannelid = dataEntity.getDynamicObject("orderchannelid");
        if (orderchannelid != null && !itemEntry.isEmpty()) {
            //省市区
            Object area = orderchannelid.get("area");
            //详细地址
            String address = orderchannelid.getString("address");
            //联系人
            String contact = orderchannelid.getString("contact");
            //联系人电话
            String contactphone = orderchannelid.getString("contactphone");
            for (DynamicObject itemEntryRow : itemEntry) {
                if (itemEntryRow.get("entryaddressid") == null) {
                    itemEntryRow.set("entryaddressid", area);
                }
                if (Objects.equals(itemEntryRow.get("entrydetailaddress").toString(), "")) {
                    itemEntryRow.set("entrydetailaddress", address);
                }
                if (Objects.equals(itemEntryRow.get("entrycontactname").toString(), "")) {
                    itemEntryRow.set("entrycontactname", contact);
                }
                if (Objects.equals(itemEntryRow.get("entrytelephone").toString(), "")) {
                    itemEntryRow.set("entrytelephone", contactphone);
                }
            }

        }
    }

    private void setTaxrate(DynamicObjectCollection itemEntry) {
        for (DynamicObject entryRow : itemEntry) {
            //是否赠品
            boolean ispresent = entryRow.getBoolean("ispresent");
            if (ispresent) {
                DynamicObject item = entryRow.getDynamicObject("itemid");//商品
                Object materialId = item.get("material.id");//物料Id
                DynamicObject material = BusinessDataServiceHelper.loadSingle(materialId, "bd_material");
                DynamicObject taxrate = material.getDynamicObject("taxrate");
                BigDecimal taxratede = BigDecimal.ZERO;
                if (taxrate != null) {
                    taxratede = taxrate.getBigDecimal("taxrate");
                }
                entryRow.set("taxrateid", taxrate);
                entryRow.set("taxrate", taxratede);
            }
        }
    }


    private void setStock(DynamicObject dataEntity) {
        DynamicObject billType = dataEntity.getDynamicObject("billtypeid");//单据类型
        if (billType != null) {
            String number = billType.getString("number");
            //Object id = billType.getPkValue();

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

                boolean pqSelect=false;
                //订货渠道
                DynamicObject orderchannelid = dataEntity.getDynamicObject("orderchannelid");
                if (orderchannelid != null) {
                    //销售片区
                    DynamicObject pq=orderchannelid.getDynamicObject("nckd_regiongroup");
                    if(pq!=null){
                        Object pqPkId=pq.getPkValue();
                        qFilter.and("nckd_regiongroup",QCP.equals,pqPkId);
                        pqSelect=true;
                    }
                    else {
                        qFilter.and("nckd_regiongroup", QCP.equals, 0L);
                    }
                }

                //车销订单
                if(number.equalsIgnoreCase("ocbsoc_saleorder_sys001")){
                    qFilter.and("nckd_isjh",QCP.equals,"1");//借货仓
                }else {
                    qFilter.and("nckd_isjh",QCP.equals,"0");//借货仓
                }
                //查找部门对应仓库
                DynamicObjectCollection collections = QueryServiceHelper.query("nckd_bmcksz",
                        "id,nckd_ck.id stockId", qFilter.toArray(), "modifytime");
                String stockId="";
                if (!collections.isEmpty()) {
                    DynamicObject stockItem = collections.get(0);
                     stockId = stockItem.getString("stockId");

                }else if(pqSelect){
                    // 构造QFilter
                    QFilter nFilter = new QFilter("createorg", QCP.equals, orgId)
                            .and("status", QCP.equals, "C")
                            .and("nckd_bm", QCP.equals, deptId)
                            .and("nckd_regiongroup", QCP.equals, 0L);
                    //车销订单
                    if(number.equalsIgnoreCase("ocbsoc_saleorder_sys001")){
                        nFilter.and("nckd_isjh",QCP.equals,"1");//借货仓
                    }else {
                        qFilter.and("nckd_isjh",QCP.equals,"0");//借货仓
                    }
                    //查找部门对应仓库
                    DynamicObjectCollection query = QueryServiceHelper.query("nckd_bmcksz",
                            "id,nckd_ck.id stockId", nFilter.toArray(), "modifytime");
                    if (!query.isEmpty()) {
                        DynamicObject stockItem = query.get(0);
                         stockId = stockItem.getString("stockId");
                    }

                }
                if(stockId!=""){
                    DynamicObject stock = BusinessDataServiceHelper.loadSingle(stockId, "bd_warehouse");
                    DynamicObjectCollection plane_entry = dataEntity.getDynamicObjectCollection("plane_entry");
                    for (DynamicObject planRow : plane_entry) {
                        planRow.set("sub_warehouseid1", stock);
                    }
                    DynamicObjectCollection itementry = dataEntity.getDynamicObjectCollection("itementry");
                    for (DynamicObject itemObj : itementry) {
                        DynamicObjectCollection subentryentity = itemObj.getDynamicObjectCollection("subentryentity");
                        for (DynamicObject sub : subentryentity) {
                            sub.set("sub_warehouseid", stock);
                        }
                    }
                }

            }

        }
    }

//    private void setAutoSign(DynamicObject dataEntity) {
//        DynamicObject billType = dataEntity.getDynamicObject("billtypeid");//单据类型
//        if (billType != null) {
//            String name = billType.getString("name");
//            Object id = billType.getPkValue();
//            if ("车销订单".equalsIgnoreCase(name) || "自提订单".equalsIgnoreCase(name) || "企业寄售订单".equalsIgnoreCase(name)) {
//                dataEntity.set("nckd_autosign", true);
//            } else {
//                dataEntity.set("nckd_autosign", false);
//            }
//        }
//    }


}