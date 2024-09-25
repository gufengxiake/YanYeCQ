package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.util.*;

/**
 * 销售出库保存更新配送费单价-单据操作插件
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
    }


    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);

        DynamicObject[] dataEntities = e.getDataEntities();
        for (DynamicObject dataEntity : dataEntities) {
            Long bizorg = (Long) dataEntity.getDynamicObject("bizorg").getPkValue();
            Long deliverchannel = (Long) dataEntity.getDynamicObject("nckd_deliverchannel").getPkValue();
            Date biztime = dataEntity.getDate("biztime");
            Long supplierId = this.getSupplierId(deliverchannel);
            BigDecimal purContractPrice = this.getPurContractPrice(bizorg, supplierId,biztime);
            if(purContractPrice.compareTo(BigDecimal.ZERO) > 0){
                DynamicObjectCollection billentry = dataEntity.getDynamicObjectCollection("billentry");
                for (DynamicObject entryRow : billentry) {
                    entryRow.set("nckd_pricefielpsf",purContractPrice);
                }
            }

        }

    }
    public BigDecimal getPurContractPrice(Long org, Long supplierId,Date biztime) {
        QFilter qFilter = new QFilter("org", QCP.equals, org)
                .and("supplier", QCP.equals, supplierId)//供应商;
                .and("validstatus",QCP.equals,"B")
                .and("billstatus", QCP.equals, "C")
                .and("closestatus", QCP.equals, "A")
                .and("biztimeend",QCP.large_equals,biztime)
                .and("biztimebegin",QCP.less_equals,biztime);
        DynamicObjectCollection conm_purcontract = QueryServiceHelper.query("conm_purcontract", "billentry.priceandtax as priceandtax,org,supplier"
                , new QFilter[]{qFilter}, "biztimebegin desc");
        if (conm_purcontract == null || conm_purcontract.isEmpty()){
            return BigDecimal.ZERO;
        }
        return conm_purcontract.get(0).getBigDecimal("priceandtax");

    }

    public Long getSupplierId(Long channelId) {
        DynamicObject ocdbd_channel = QueryServiceHelper.queryOne("ocdbd_channel", "customer",
                new QFilter[]{new QFilter("id", QCP.equals, channelId)});
        if (ocdbd_channel == null) {
            return 0L;
        }
        long customer = ocdbd_channel.getLong("customer");
        DynamicObject bd_customer = QueryServiceHelper.queryOne("bd_customer", "bizpartner",
                new QFilter[]{new QFilter("id", QCP.equals, customer)});
        if(bd_customer == null){
            return 0L;
        }
        long bizpartner = bd_customer.getLong("bizpartner");
        DynamicObject bd_bizpartner = QueryServiceHelper.queryOne("bd_supplier", "id",
                new QFilter[]{new QFilter("bizpartner", QCP.equals, bizpartner)});
        if (bd_bizpartner == null){
            return 0L;
        }
        return bd_bizpartner.getLong("id");

    }
}