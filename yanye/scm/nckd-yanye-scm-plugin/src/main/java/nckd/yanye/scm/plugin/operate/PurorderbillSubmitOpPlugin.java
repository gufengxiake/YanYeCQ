package nckd.yanye.scm.plugin.operate;

import cn.hutool.core.util.ObjectUtil;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.validate.AbstractValidator;
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
     * 5、采购订单根据匹配采购合同对应的“合同类型”字段对应的控制策略进行控制，在提交时进行校验，控制逻辑如下
     * a）如果控制单价，则采购订单中物料明细行对应的含税单价必须在采购合同中上下限含税单价；
     * b）如果控制数量，则采购订单中物料明细行的数量≤采购合同对应行的数量-采购合同对应行的采购合同已下达订单数量；
     * c）如果控制总金额，则采购订单中相同采购合同的物料明细行的“价税合计”的合计≤采购合同财务信息“已执行价税合计”，存在一个采购订单多行物料对应一个采购合同，故需要先将采购订单中相同采购合同的金额进行汇总后，再与采购合同进行比较判断；
     * d）如果选择组合控制，例如数量+单价，则进行组合控制，多种控制逻辑均生效；
     *
     * @param e
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                //提交时校验可能存在多条
                for (ExtendedDataEntity rowDataEntity : this.getDataEntities()) {
                    //获取采购订单
                    DynamicObject purorderbill = rowDataEntity.getDataEntity();
                    //获取采购订单的分录

                    //查询采购合同，获取“合同类型”字段

                    //判断 “合同类型”字段 是什么
                    //总金额	MON
                    //数量	NUM
                    //单价	PRI
                    //总金额、数量	M&N
                    //总金额、单价	M&P
                    //总金额、数量、单价	M&N&P
                    //不控制	NAN
                    Set<String> checkTypes = getCheckType("合同类型");
                    for (String action : checkTypes) {
                        if (action.equals("qty")) {
                            qtyCheck(purorderbill);
                        } else if (action.equals("amount")) {
                            amountCheck(purorderbill);
                        } else if (action.equals("price")) {
                            priceCheck(purorderbill);
                        }
                    }
                }
            }
        });
    }

    /**
     * 根据规则获取需要校验的字段
     *
     * @param checkType
     * @return
     */
    private Set<String> getCheckType(String checkType) {
        Set<String> actions = new LinkedHashSet();
        switch (checkType) {
            case "MON":
                actions.add("amount");
                break;
            case "NUM":
                actions.add("qty");
                break;
            case "PRI":
                actions.add("price");
                break;
            case "M&N":
                actions.add("amount");
                actions.add("qty");
                break;
            case "M&P":
                actions.add("amount");
                actions.add("price");
                break;
            case "M&N&P":
                actions.add("amount");
                actions.add("qty");
                actions.add("price");
                break;
            case "NAN":
                return actions;
        }
        return actions;
    }

    private void qtyCheck(DynamicObject purorderbill) {

    }

    private void amountCheck(DynamicObject purorderbill) {

    }

    private void priceCheck(DynamicObject purorderbill) {
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
