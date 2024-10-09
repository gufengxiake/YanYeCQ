package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.sdk.plugin.Plugin;
import nckd.yanye.occ.plugin.form.SaleOrderBillPlugIn;

import java.math.BigDecimal;
import java.util.*;

/**
 * 销售出库提交更新配送费单价，溢价金额-单据操作插件
 * 提交修改要货订单单据状态，签收状态
 * 表单标识：nckd_im_saloutbill_ext
 * author:zhangzhilong
 * date:2024/09/25
 */
public class SalOutSaveOperationPlugIn extends AbstractOperationServicePlugIn implements Plugin {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("bizorg");//销售组织
        e.getFieldKeys().add("nckd_deliverchannel");//配送渠道
        e.getFieldKeys().add("biztime");//业务日期
        e.getFieldKeys().add("billentry.nckd_pricefielpsf");//配送费单价
        e.getFieldKeys().add("billentry.qty");//数量
        e.getFieldKeys().add("billentry.nckd_pricefield3");//标准基价
        e.getFieldKeys().add("billentry.amountandtax");//价税合计
        e.getFieldKeys().add("billentry.nckd_remainpayamount");//剩余应付金额
        e.getFieldKeys().add("billentry.nckd_premiumamount");//溢价金额
        e.getFieldKeys().add("billentry.nckd_pricefielpsf");//配送费单价
        e.getFieldKeys().add("billentry.nckd_basicqtyps");//配送结算基本数量
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            Long bizorg = (Long) dataEntity.getDynamicObject("bizorg").getPkValue();
            Long deliverchannel=0L;
            if(dataEntity.getDynamicObject("nckd_deliverchannel")!=null){
                 deliverchannel = (Long) dataEntity.getDynamicObject("nckd_deliverchannel").getPkValue();
            }
            Date biztime = dataEntity.getDate("biztime");
            Long supplierId = this.getSupplierId(deliverchannel);
            BigDecimal purContractPrice = this.getPurContractPrice(bizorg, supplierId, biztime);//配送费金额
            DynamicObjectCollection billentry = dataEntity.getDynamicObjectCollection("billentry");
            for (DynamicObject entryRow : billentry) {
                entryRow.set("nckd_pricefielpsf", purContractPrice);
                BigDecimal qty = entryRow.getBigDecimal("qty");
                BigDecimal price = entryRow.getBigDecimal("nckd_pricefield3");
                BigDecimal amountandtax = entryRow.getBigDecimal("amountandtax");
                //溢价金额=价税合计-（数量*标准基价）
                BigDecimal premiumamount = amountandtax.subtract(qty.multiply(price));
                if (premiumamount.compareTo(BigDecimal.ZERO) > 0) {
                    entryRow.set("nckd_premiumamount", premiumamount);
                    entryRow.set("nckd_remainpayamount", premiumamount);
                    //配送结算基本数量=溢价金额/配送单价
                    entryRow.set("nckd_basicqtyps", purContractPrice.compareTo(BigDecimal.ZERO) > 0 ? premiumamount.divide(purContractPrice) : BigDecimal.ZERO);
                } else {
                    entryRow.set("nckd_premiumamount", 1);
                    entryRow.set("nckd_remainpayamount", 1);
                    entryRow.set("nckd_basicqtyps", 1);
                }
            }
        }

        //SaveServiceHelper.update(dataEntities);
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        // 获取当前单据的主实体编码、单据内码
        String targetEntityNumber = this.billEntityType.getName();
        Set<Object> billIds = new HashSet<>();
        for (DynamicObject dataEntity : e.getDataEntities()) {
            billIds.add(dataEntity.getPkValue());
        }
        // 调用平台的服务，获取所有源单及其内码
        Map<String, HashSet<Long>> sourceBillIds = BFTrackerServiceHelper.findSourceBills(targetEntityNumber, billIds.toArray(new Long[0]));
        // 从所有源单中寻找需要的demo_botpbill1
        HashSet<Long> botpBillIds = new HashSet<>();
        String sourceBill = "ocbsoc_saleorder";//要货订单
        if (sourceBillIds.containsKey(sourceBill)) {
            botpBillIds = sourceBillIds.get(sourceBill);
        }
        if(botpBillIds.isEmpty()){
            return;
        }
        List<DynamicObject> saveValue = new ArrayList(16);
        for (Object pk : botpBillIds) {
            //根据Id获取要货订单单实体
            DynamicObject saleBill = BusinessDataServiceHelper.loadSingle(pk, sourceBill);
            saleBill.set("billstatus","E");//订单状态已发货
            saleBill.set("signstatus","B");//签收状态待签收
            saveValue.add(saleBill);
        }
        DynamicObject[] saveValues = saveValue.toArray(new DynamicObject[saveValue.size()]);
        SaveServiceHelper.save(saveValues);

    }


    public BigDecimal getPurContractPrice(Long org, Long supplierId, Date biztime) {
        QFilter qFilter = new QFilter("org", QCP.equals, org)
                .and("supplier", QCP.equals, supplierId)//供应商;
                .and("validstatus", QCP.equals, "B")
                .and("billstatus", QCP.equals, "C")
                .and("closestatus", QCP.equals, "A")
                .and("biztimeend", QCP.large_equals, biztime)
                .and("biztimebegin", QCP.less_equals, biztime);
        DynamicObjectCollection conmPurcontract = QueryServiceHelper.query("conm_purcontract", "billentry.priceandtax as priceandtax,org,supplier"
                , new QFilter[]{qFilter}, "biztimebegin desc");
        if (conmPurcontract == null || conmPurcontract.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return conmPurcontract.get(0).getBigDecimal("priceandtax");

    }

    public Long getSupplierId(Long channelId) {
        DynamicObject ocdbdChannel = QueryServiceHelper.queryOne("ocdbd_channel", "customer",
                new QFilter[]{new QFilter("id", QCP.equals, channelId)});
        if (ocdbdChannel == null) {
            return 0L;
        }
        long customer = ocdbdChannel.getLong("customer");
        DynamicObject bdCustomer = QueryServiceHelper.queryOne("bd_customer", "bizpartner",
                new QFilter[]{new QFilter("id", QCP.equals, customer)});
        if (bdCustomer == null) {
            return 0L;
        }
        long bizpartner = bdCustomer.getLong("bizpartner");
        DynamicObject bdBizpartner = QueryServiceHelper.queryOne("bd_supplier", "id",
                new QFilter[]{new QFilter("bizpartner", QCP.equals, bizpartner)});
        if (bdBizpartner == null) {
            return 0L;
        }
        return bdBizpartner.getLong("id");

    }
}