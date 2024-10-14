package nckd.yanye.scm.plugin.operate;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.DBServiceHelper;
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
        fieldKeys.add("seq");
        fieldKeys.add("materialname");
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
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                List<DynamicObject> purorderbillList = Arrays.stream(dataEntities).map(e -> e.getDataEntity()).collect(Collectors.toList());
                Set<Long> conbillids = new HashSet<>();
                purorderbillList.forEach(k -> {
                    DynamicObjectCollection billentry = k.getDynamicObjectCollection("billentry");
                    Set<Long> conbillidSet = billentry.stream().map(h -> h.getLong("conbillid")).collect(Collectors.toSet());
                    conbillids.addAll(conbillidSet);
                });
                //根据提交的采购订单去查询采购合同
                QFilter qFilter = new QFilter("id", QCP.in, conbillids)
                        .and("billstatus", QCP.equals, "C")
                        .and("validstatus", QCP.equals, "B")
                        .and("closestatus", QCP.equals, "A");
                //String entityName, String selectProperties, QFilter[] filters
                DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("conm_purcontract", "id,billno,type,totalallamount,nckd_totalallamount,nckd_cebl,billentry.id,billentry.material,billentry.nckd_priceandtaxup,billentry.nckd_priceandtaxlow" +
                        ",billentry.qty,billentry.nckd_qty,billentry.amountandtax,billentry.nckd_amount,billentry,billentry.seq", qFilter.toArray());
                //转成key id value DynamicObject采购合同
                Map<Long, DynamicObject> map = Arrays.stream(purcontractArr).collect(Collectors.toMap(k -> k.getLong("id"), v -> v));

                //提交时校验可能存在多条
                for (ExtendedDataEntity rowDataEntity : this.getDataEntities()) {
                    //获取采购订单
                    DynamicObject purorderbill = rowDataEntity.getDataEntity();
                    //获取采购订单的分录
                    DynamicObjectCollection purorderbillEntryColl = purorderbill.getDynamicObjectCollection("billentry");
                    //查询采购合同，获取“合同类型”字段
                    for (DynamicObject purorderbillEntry : purorderbillEntryColl) {
                        Object conbillid = purorderbillEntry.get("conbillid");
                        //根据合同id为key找到采购合同
                        DynamicObject purcontract = map.get(conbillid);
                        if (ObjectUtil.isNotEmpty(purcontract)) {
                            DynamicObject type = purcontract.getDynamicObject("type");
                            String excutecontrol = type.getString("excutecontrol");
                            //根据采购合同的合同类型，找到合同控制 来判断是单个控制还是组合控制
                            Set<String> actions = new HashSet<>();
                            getCheckType(actions, excutecontrol);
                            checkDataByCheckType(actions, purorderbillEntry, purcontract, rowDataEntity, purorderbill);
                        } /*else {
                            int materialSeq = purorderbillEntry.getInt("seq");
                            String materialName = purorderbillEntry.getString("materialname");
                            this.addErrorMessage(rowDataEntity, String.format("第(%s)行的物料名称 (%s) 未关联合同id(conbillid)", materialSeq, materialName));
                        }*/
                    }
                }
            }

            /**
             * 获得“合同类型”的合同控制 进行数据校验
             *
             * @param actions
             * @param purorderbillEntry
             * @param purcontract
             * @param rowDataEntity
             * @param purorderbill
             */
            private void checkDataByCheckType(Set<String> actions, DynamicObject purorderbillEntry, DynamicObject purcontract, ExtendedDataEntity rowDataEntity, DynamicObject purorderbill) {
                //判断 “合同类型”的合同控制字段 是什么
                //总金额	MON
                //数量	NUM
                //单价	PRI
                //总金额、数量	M&N
                //总金额、单价	M&P
                //总金额、数量、单价	M&N&P
                //不控制	NAN
                for (String action : actions) {
                    if (action.equals("qty")) {
                        qtyCheck(purorderbillEntry, purcontract, rowDataEntity, purorderbill);
                    } else if (action.equals("amount")) {
                        amountCheck(purorderbillEntry, purcontract, rowDataEntity, purorderbill);
                    } else if (action.equals("price")) {
                        priceCheck(purorderbillEntry, purcontract, rowDataEntity, purorderbill);
                    }
                }
            }

            /*
             * ResManager.loadKDString("物料明细第%1$d行，合同：[%2$s] 的物料明细第%3$d行，已订货数量超额", "PerformDataValidator_1", "scmc-conm-business", new Object[0]);
             *
             * @param purorderbillEntry
             * @param purcontract
             */
            private void qtyCheck(DynamicObject purorderbillEntry, DynamicObject purcontract, ExtendedDataEntity rowDataEntity, DynamicObject purorderbill) {
                //获取采购订单分录的物料
                DynamicObject material = purorderbillEntry.getDynamicObject("material");
                DynamicObjectCollection purcontractEntryColl = purcontract.getDynamicObjectCollection("billentry");
                Map<Object, DynamicObject> purcontractEntryMap = purcontractEntryColl.stream().collect(Collectors.toMap(e -> e.getDynamicObject("material").getPkValue(), v -> v));
                DynamicObject purcontractEntry = purcontractEntryMap.get(material.getPkValue());
                //b）如果控制数量，则采购订单中物料明细行的数量≤采购合同对应行的数量-采购合同对应行的采购合同已下达订单数量；
                //采购订单数据
                BigDecimal qty = purorderbillEntry.getBigDecimal("qty");
                //采购合同数量
                BigDecimal qty1 = purcontractEntry.getBigDecimal("qty");
                BigDecimal nckdQty = purcontractEntry.getBigDecimal("nckd_qty");
                BigDecimal subtract = qty1.subtract(nckdQty);
                if (qty.compareTo(subtract) > 0) {
                    //"第(%s)行的物料名称 (%s) 与采购合同(%s)的第(%s)行存在重复物料"
                    String billno = purorderbill.getString("billno");
                    int seq = purorderbillEntry.getInt("seq");
                    String purcontractBillno = purcontract.getString("billno");
                    int purcontractEntrySeq = purcontractEntry.getInt("seq");
                    this.addErrorMessage(rowDataEntity, String.format("%s 物料明细第(%s)行，对应合同：(%s) 的物料明细第(%s)行，已订货数量超额",
                            billno, seq, purcontractBillno, purcontractEntrySeq));
                }
            }

            /*
             * ResManager.loadKDString("合同：[%s] 的已订货金额超额", "PerformDataValidator_3", "scmc-conm-business", new Object[0]);
             *
             * @param purorderbillEntry
             * @param purcontract
             */
            private void amountCheck(DynamicObject purorderbillEntry, DynamicObject purcontract, ExtendedDataEntity rowDataEntity, DynamicObject purorderbill) {
                //获取采购订单分录
                DynamicObjectCollection purorderbillEntryColl = purorderbill.getDynamicObjectCollection("billentry");
                //根据合同id分组并sum价税合计
                Map<Long, BigDecimal> purorderbillMap = purorderbillEntryColl.stream().collect(Collectors.groupingBy(k -> k.getLong("conbillid"), Collectors.reducing(BigDecimal.ZERO,
                        k -> k.getBigDecimal("amountandtax"),
                        BigDecimal::add)));
                //DynamicObject material = purorderbillEntry.getDynamicObject("material");
                //DynamicObjectCollection purcontractEntryColl = purcontract.getDynamicObjectCollection("billentry");
                //Map<Object, DynamicObject> purcontractEntryMap = purcontractEntryColl.stream().collect(Collectors.toMap(e -> e.getDynamicObject("material").getPkValue(), v -> v));
                //DynamicObject purcontractEntry = purcontractEntryMap.get(material.getPkValue());
                //c）如果控制总金额，则采购订单中相同采购合同的物料明细行的“价税合计”的合计≤采购合同财务信息“已执行价税合计”，存在一个采购订单多行物料对应一个采购合同，故需要先将采购订单中相同采购合同的金额进行汇总后，再与采购合同进行比较判断；
                //采购订单数据
                BigDecimal groupAmountandtax = purorderbillMap.get(purorderbillEntry.get("conbillid"));
                //采购合同数量 modify 这里新增超额设置（采购合同上已执行价税合计+本次采购订单执行价税合计）小于等于价税合计*（1+允许超额比例）
                BigDecimal totalallamount = purcontract.getBigDecimal("totalallamount");
                BigDecimal nckdCebl = purcontract.getBigDecimal("nckd_cebl");
                BigDecimal total = totalallamount.add(totalallamount.multiply(nckdCebl).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
                BigDecimal nckdTotalallamount = purcontract.getBigDecimal("nckd_totalallamount");
                BigDecimal subtract = total.subtract(nckdTotalallamount);
                if (groupAmountandtax.compareTo(subtract) > 0) {
                    //%s 物料明细第(%s)行，对应合同：[%s] 的已订货金额超额
                    String billno = purorderbill.getString("billno");
                    int seq = purorderbillEntry.getInt("seq");
                    String purcontractBillno = purcontract.getString("billno");
                    this.addErrorMessage(rowDataEntity, String.format("%s 物料明细第(%s)行，对应合同：[%s] 的已订货金额超额",
                            billno, seq, purcontractBillno));
                }
            }

            /*
             * ResManager.loadKDString("物料明细第%1$d行，含税单价与合同：[%2$s] 的物料明细第%3$d行含税单价不一致", "PerformDataValidator_4", "scmc-conm-business", new Object[0]);
             *
             * @param purorderbillEntry
             * @param purcontract
             */
            private void priceCheck(DynamicObject purorderbillEntry, DynamicObject purcontract, ExtendedDataEntity rowDataEntity, DynamicObject purorderbill) {
                //获取采购订单分录的物料
                DynamicObject material = purorderbillEntry.getDynamicObject("material");
                DynamicObjectCollection purcontractEntryColl = purcontract.getDynamicObjectCollection("billentry");
                Map<Object, DynamicObject> purcontractEntryMap = purcontractEntryColl.stream().collect(Collectors.toMap(e -> e.getDynamicObject("material").getPkValue(), v -> v));
                DynamicObject purcontractEntry = purcontractEntryMap.get(material.getPkValue());
                //a）如果控制单价，则采购订单中物料明细行对应的含税单价必须在采购合同中上下限含税单价；
                //采购订单数据
                BigDecimal priceandtax = purorderbillEntry.getBigDecimal("priceandtax");
                //采购合同数量
                BigDecimal nckdPriceandtaxup = purcontractEntry.getBigDecimal("nckd_priceandtaxup");
                BigDecimal nckdPriceandtaxlow = purcontractEntry.getBigDecimal("nckd_priceandtaxlow");
                if (priceandtax.compareTo(nckdPriceandtaxup) > 0 || priceandtax.compareTo(nckdPriceandtaxlow) < 0) {
                    //"第(%s)行的物料名称 (%s) 与采购合同(%s)的第(%s)行存在重复物料"
                    String billno = purorderbill.getString("billno");
                    int seq = purorderbillEntry.getInt("seq");
                    String purcontractBillno = purcontract.getString("billno");
                    int purcontractEntrySeq = purcontractEntry.getInt("seq");
                    this.addErrorMessage(rowDataEntity, String.format("%s 物料明细第(%s)行，含税单价不在合同：(%s) 的物料明细第(%s)行含税单价上下限区间",
                            billno, seq, purcontractBillno, purcontractEntrySeq));
                }
            }

            /**
             * 根据规则获取需要校验的字段
             * @param actions
             * @param excutecontrol
             */
            private void getCheckType(Set<String> actions, String excutecontrol) {
                switch (excutecontrol) {
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
                        break;
                }
            }
        });
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
        Set<Long> conbillids = new HashSet<>();
        Arrays.stream(entities).forEach(k -> {
            DynamicObjectCollection billentry = k.getDynamicObjectCollection("billentry");
            Set<Long> conbillidSet = billentry.stream().map(h -> h.getLong("conbillid")).collect(Collectors.toSet());
            conbillids.addAll(conbillidSet);
        });
        //根据提交的采购订单去查询采购合同
        QFilter qFilter = new QFilter("id", QCP.in, conbillids)
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
                    if (qty.compareTo(new BigDecimal(0)) > 0) {
                        purcontractBillEntryDy.set("nckd_qtyratio", nckdQty.add(qty1).divide(qty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));
                    }
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

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        // 提交时校验
        DynamicObject[] entities = e.getDataEntities();
        Set<Long> conbillids = new HashSet<>();
        Arrays.stream(entities).forEach(k -> {
            DynamicObjectCollection billentry = k.getDynamicObjectCollection("billentry");
            Set<Long> conbillidSet = billentry.stream().map(h -> h.getLong("conbillid")).collect(Collectors.toSet());
            conbillids.addAll(conbillidSet);
        });
        //根据提交的采购订单去查询采购合同
        QFilter qFilter = new QFilter("id", QCP.in, conbillids)
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
                    // 带参数
                    insertTBotpBilltracker(purcontractBill.getPkValue(), purorderbill.getPkValue());

                    insertTPmPurorderbillTc(purcontractBill.getPkValue(), purcontractBillEntryDy.getPkValue(), purorderbill.getPkValue(), entry.getPkValue());

                }
            }
        }
    }

    /**
     * 插入规则表
     */
    private static void insertTPmPurorderbillTc(Object sbillid, Object sid, Object tbillid, Object tid) {
        String purorderBillTcSql = "INSERT INTO t_pm_purorderbill_tc (fid, ftbillid, fttableid, ftid, fsbillid, fstableid,fsid) VALUES (?, ?, ?, ?, ?, ?,?);";
        Long fid = DBServiceHelper.genGlobalLongId();
        Long ftbillid = Convert.toLong(tbillid);
        Long fttableid = 602924326097811460L;
        Long ftid = Convert.toLong(tid);
        Long fsbillid = Convert.toLong(sbillid);
        Long fstableid = 719529409035381761L;
        Long fsid = Convert.toLong(sid);
        //先判断有没有这个botp关系，有就不再添加
        String queryPurorderBillTcSql = "SELECT fid FROM t_pm_purorderbill_tc WHERE ftbillid = ? AND ftid = ? AND fsbillid = ? AND fsid = ?);";
        List<Long> query = DB.query(DBRoute.of("scm"), queryPurorderBillTcSql, new Object[]{ftbillid, ftid, fsbillid, fsid}, resultSet -> {
            List<Long> valuetemp = new ArrayList<Long>();
            while (resultSet.next()) {
                valuetemp.add(resultSet.getLong("fid"));
            }
            return valuetemp;
        });

        if (query.size() == 0) {
            DB.execute(DBRoute.of("scm"), purorderBillTcSql, new Object[]{fid, ftbillid, fttableid, ftid, fsbillid, fstableid, fsid});
        }
    }

    /**
     * 插入botp规则表
     */
    private static void insertTBotpBilltracker(Object sbillid, Object tbillid) {
        String botpSql = "INSERT INTO t_botp_billtracker (fid, fstableid, fsbillid, fttableid, ftbillid, fcreatetime) VALUES (?, ?, ?, ?, ?, ?);";
        Long fid = DBServiceHelper.genGlobalLongId();
        Long fstableid = 719529409035381761L;
        Long fsbillid = Convert.toLong(sbillid);
        Long fttableid = 602924326097811460L;
        Long ftbillid = Convert.toLong(tbillid);
        Date fcreatetime = new Date();
        //先判断有没有这个botp关系，有就不再添加
        QFilter qFilter = new QFilter("sbillid", QCP.equals, sbillid).and("tbillid", QCP.equals, tbillid);
        DynamicObject botpBilltracker = BusinessDataServiceHelper.loadSingle("botp_billtracker", "id,stableid,sbillid,ttableid,tbillid,createtime", qFilter.toArray());
        if (ObjectUtil.isNotEmpty(botpBilltracker)) {
            DB.execute(DBRoute.basedata, botpSql, new Object[]{fid, fstableid, fsbillid, fttableid, ftbillid, fcreatetime});
        }
    }
}
