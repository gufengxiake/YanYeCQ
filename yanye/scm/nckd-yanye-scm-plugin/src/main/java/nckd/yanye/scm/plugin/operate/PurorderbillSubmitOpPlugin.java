package nckd.yanye.scm.plugin.operate;

import cn.hutool.core.util.ObjectUtil;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :供应链云-采购管理-采购订单
 * Description      :采购订单提交操作插件
 *
 * @author : zhujintao
 * @date : 2024/7/16
 */
public class PurorderbillSubmitOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("conbillid");
        fieldKeys.add("material");
    }

    /**
     * 采购订单如已匹配采购合同，则采购订单提交时，将该采购订单明细行的 “数量”及“价税合计”以累计增加的方式反写至采购合同对应行的“已下达订单数”及“已下达订单金额”，
     * 同时触发采购合同财务信息“已执行价税合计”字段进行累计汇总，如果采购订单撤销，则进行扣减对应字段；
     */
    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        // 提交时校验
        DynamicObject[] entities = e.getDataEntities();
        Set<Long> conbillid = new HashSet<>();
        Arrays.stream(entities).forEach(k -> {
            DynamicObjectCollection billentry = k.getDynamicObjectCollection("billentry");
            Set<Long> conbillidSet = billentry.stream().map(h -> h.getLong("conbillid")).collect(Collectors.toSet());
            conbillid.addAll(conbillidSet);
        });
        //根据提交的采购订单去查询采购合同
        QFilter qFilter = new QFilter("id", QCP.in, conbillid)
                .and("billstatus", QCP.equals, "C")
                .and("validstatus", QCP.equals, "B")
                .and("closestatus", QCP.equals, "A");
        //String entityName, String selectProperties, QFilter[] filters
        DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("conm_purcontract", "id,billno,totalallamount,nckd_totalallamount,nckd_totalallamountrat,billentry.id,billentry.material,billentry.qty,billentry.nckd_qty,billentry.nckd_qtyratio,billentry.amountandtax,billentry.nckd_amount,billentry.nckd_amountratio,billentry.seq", qFilter.toArray());
        //转成key id value DynamicObject采购合同
        Map<Long, DynamicObject> map = Arrays.stream(purcontractArr).collect(Collectors.toMap(k -> k.getLong("id"), v -> v));
        //遍历提交的采购订单单据 进行逻辑处理
        for (DynamicObject purorderbill : entities) {
            DynamicObjectCollection billentry = purorderbill.getDynamicObjectCollection("billentry");
            for (DynamicObject entry : billentry) {
                //获取分录行的合同id
                long key = entry.getLong("conbillid");
                //获取采购合同
                DynamicObject purcontractBill = map.get(key);
                //获取到了才进行下面逻辑，否则下一条分录
                if (ObjectUtil.isNotEmpty(purcontractBill)) {
                    DynamicObjectCollection purcontractBillEntry = purcontractBill.getDynamicObjectCollection("billentry");
                    //转换为key 为物料 value 为该行数据
                    Map<Object, DynamicObject> purcontractBillEntryMap = purcontractBillEntry.stream().filter(h -> ObjectUtil.isNotEmpty(h.getDynamicObject("material"))).collect(Collectors.toMap(k -> k.getDynamicObject("material").getPkValue(), v -> v));
                    //当前单据分录的物料
                    Object materialId = entry.getDynamicObject("material").getPkValue();
                    //以下字段来源于采购合同
                    DynamicObject purcontractBillEntryDy = purcontractBillEntryMap.get(materialId);
                    BigDecimal totalallamount = purcontractBill.getBigDecimal("totalallamount");
                    BigDecimal nckdTtotalallamount = purcontractBill.getBigDecimal("nckd_totalallamount");
                    BigDecimal nckdTotalallamountrat = purcontractBill.getBigDecimal("nckd_totalallamountrat");
                    BigDecimal qty = purcontractBillEntryDy.getBigDecimal("qty");
                    BigDecimal nckdQty = purcontractBillEntryDy.getBigDecimal("nckd_qty");
                    BigDecimal nckdQtyratio = purcontractBillEntryDy.getBigDecimal("nckd_qtyratio");
                    BigDecimal amountandtax = purcontractBillEntryDy.getBigDecimal("amountandtax");
                    BigDecimal nckdAmount = purcontractBillEntryDy.getBigDecimal("nckd_amount");
                    BigDecimal nckdAmountratio = purcontractBillEntryDy.getBigDecimal("nckd_amountratio");
                    //以下字段来源于采购订单
                    BigDecimal qty1 = entry.getBigDecimal("qty");
                    BigDecimal amountandtax1 = entry.getBigDecimal("amountandtax");
                    //采购合同已下单数量=采购合同的已下单数量+采购订单的数量
                    purcontractBillEntryDy.set("nckd_qty", nckdQty.add(qty1));
                    //采购合同已下单数量比例=采购合同的已下单数量/采购合同的数量
                    purcontractBillEntryDy.set("nckd_qtyratio", nckdQty.add(qty1).divide(qty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));
                    //采购合同已下单金额=采购合同的已下单金额+采购订单的价税合计
                    purcontractBillEntryDy.set("nckd_amount", nckdAmount.add(amountandtax1));
                    //采购合同已下单金额比例=采购合同的已下单金额/采购合同的价税合计
                    purcontractBillEntryDy.set("nckd_amountratio", nckdAmount.add(amountandtax1).divide(amountandtax, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));
                    //已执行价税合计=采购合同的已下单金额求和
                    BigDecimal sumNckdAmount = purcontractBillEntry.stream().map(k -> k.getBigDecimal("nckd_amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
                    purcontractBill.set("nckd_totalallamount", sumNckdAmount);
                    //已执行价税合计比例=采购合同的已下单金额求和/财务信息的价税合计
                    purcontractBill.set("nckd_totalallamountrat", sumNckdAmount.divide(totalallamount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));
                    SaveServiceHelper.save(purcontractArr);
                }
            }
        }
    }
}
