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
 * Module           :质量云-来料质量管理-不合格品处理
 * Description      :来料不良品处理单反审核插件
 *
 * @author : yaosijie
 * @date : 2024/7/22
 */
public class BadDealbillAuditOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
//        fieldKeys.add("conbillnumber");
        fieldKeys.add("srcbillid");
        fieldKeys.add("nckd_discount_amount");
        fieldKeys.add("materielid");
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        DynamicObject[] entities = e.getDataEntities();
        //获取来料不良品处理单全部分录信息
        DynamicObjectCollection billentry = new DynamicObjectCollection();
        Arrays.stream(entities).forEach(k -> {
            billentry.addAll(k.getDynamicObjectCollection("materialentry"));
        });
        //获取来料不良品处理单全部分录信息的srcbillid
        Set<Long> srcbillid = billentry.stream().filter(t -> t.getBigDecimal("nckd_discount_amount") != null
                && t.getBigDecimal("nckd_discount_amount").compareTo(BigDecimal.ZERO) > 0).map(t -> t.getLong("srcbillid")).collect(Collectors.toSet());
        //采购收货单id
        Set<Long> procureIds = new HashSet<>();
        //构造检验单查询条件
        QFilter qFilter = new QFilter("id", QCP.in, srcbillid);
        DynamicObject[] incominginspctArr = BusinessDataServiceHelper.load
                ("qcp_incominginspct", "id,matintoentity.srcbillid,matintoentity.sourcebillno,matintoentity.materialid", qFilter.toArray());
//        DynamicObjectCollection matintoentityArrAll = new DynamicObjectCollection();
        //构造检验单的key value结构 key为单据id value为检验单对象
        Map<Long, DynamicObject> incominginspctMap = Arrays.stream(incominginspctArr).collect(Collectors.toMap(t -> t.getLong("id"), t -> t));
        //循环检验单，获取检验单全部分录信息的srcbillid
        for (int i = 0; i < incominginspctArr.length; i++) {
            DynamicObjectCollection matintoentityArr = incominginspctArr[i].getDynamicObjectCollection("matintoentity");
            procureIds.addAll(matintoentityArr.stream().map(h -> h.getLong("srcbillid")).collect(Collectors.toSet()));
        }
        //构造采收收货单查询条件
        QFilter qFilter1 = new QFilter("id", QCP.in, procureIds);
        //重复上面的操作 去查采购收货单
        DynamicObject[] procureMaterials = BusinessDataServiceHelper.load
                ("im_purreceivebill", "id,exchangerate,billentry.amountandtax,billentry.actualprice," +
                        "billentry.actualtaxprice,billentry.taxrate,billentry.qty,billentry.curamountandtax," +
                        "billentry.nckd_amountand,billentry.nckd_amountand_current,billentry.srcbillid,billentry.materialmasterid" +
                        ",billentry.amount,billentry.curamount,billentry.taxamount,billentry.curtaxamount", qFilter1.toArray());

        Map<Long, DynamicObject> purreceivebillMap = Arrays.stream(procureMaterials).collect(Collectors.toMap(t -> t.getLong("id"), t -> t));
        //遍历来料不良品处理单全部分录
        billentry.stream().filter(t -> t.getBigDecimal("nckd_discount_amount") != null
                && t.getBigDecimal("nckd_discount_amount").compareTo(BigDecimal.ZERO) > 0).forEach(k -> {
            //这是不良品物料id
            Object materielid = k.getDynamicObject("materielid").getPkValue();
            //折让金额
            BigDecimal discountAmount = k.getBigDecimal("nckd_discount_amount");
            //
            Object sourceId = k.get("srcbillid");
            //检验单
            DynamicObject incominginspct = incominginspctMap.get(sourceId);
            //判断有没有检验单，没有就结束
            if (ObjectUtil.isNotEmpty(incominginspct)) {
                //检验单分录
                DynamicObjectCollection matintoentity = incominginspct.getDynamicObjectCollection("matintoentity");
                //构造检验单分录为map结构 key为分录的物料id ，value就是当前分录
                Map<Object, DynamicObject> materialMap = matintoentity.stream().collect(Collectors.toMap(t -> t.getDynamicObject("materialid").getPkValue(), t -> t));
                //拿到了检验单分录对应的物料行数据
                DynamicObject dynamicObject = materialMap.get(materielid);
                //采购收货单的原单id
                Object procureSrcbillid = dynamicObject.get("srcbillid");
                //采购收货单
                DynamicObject procureDynamicObject = purreceivebillMap.get(procureSrcbillid);
                BigDecimal exchangerate = procureDynamicObject.getBigDecimal("exchangerate");
                //获取采购收货单分录
                DynamicObjectCollection billentryDynamicObjectC = procureDynamicObject.getDynamicObjectCollection("billentry");
                //构造采购收货单map
                Map<Object, DynamicObject> materialmasterMap = billentryDynamicObjectC.stream().collect(Collectors.toMap(t -> t.getDynamicObject("materialmasterid").getPkValue(), t -> t));
                //采购收货单的对应物料的分录
                DynamicObject dynamic = materialmasterMap.get(materielid);
                //获取采购收货单分录的收据
                //价税合计（原）
                BigDecimal originalAmount = dynamic.getBigDecimal("amountandtax");
                //税率
                BigDecimal taxrate = dynamic.getBigDecimal("taxrate");
                //数量
                BigDecimal unqualiqty = dynamic.getBigDecimal("qty");
                BigDecimal original = originalAmount.subtract(discountAmount);
                //实际含税单价
                BigDecimal actualTaxPrice = original.divide(unqualiqty, 6, RoundingMode.HALF_UP);
                BigDecimal rate = taxrate.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                //实际单价
                BigDecimal actualPrice = actualTaxPrice.divide(BigDecimal.ONE.add(rate), 6, RoundingMode.HALF_UP);
                dynamic.set("nckd_amountand_current", dynamic.getBigDecimal("curamountandtax"));//价税合计(原)(本位币)
                dynamic.set("amountandtax", original);//价税合计
                dynamic.set("curamountandtax", original.multiply(exchangerate));//价税合计(本位币)
                dynamic.set("actualprice", actualPrice);//实际单价
                dynamic.set("actualtaxprice", actualTaxPrice);//实际含税单价
                dynamic.set("nckd_amountand", originalAmount);//价税合计(原)
                dynamic.set("actualtaxprice", actualTaxPrice);//含税单价
                dynamic.set("actualprice", actualPrice);// 单价
                dynamic.set("nckd_taxpricecurrent", actualTaxPrice);//含税单价（原）
                dynamic.set("nckd_pricecurrent", actualPrice);//单价（原）
                dynamic.set("nckd_amount", dynamic.getBigDecimal("amount"));// 金额(原)
                dynamic.set("nckd_amountcurrency", dynamic.getBigDecimal("curamount"));// 金额（本币位）(原)
                dynamic.set("nckd_taxamount", dynamic.getBigDecimal("taxamount"));// 税额(原)
                dynamic.set("nckd_taxamountcurrent", dynamic.getBigDecimal("curtaxamount"));// 税额（本币位）(原)
                BigDecimal amount = actualPrice.multiply(unqualiqty);// 金额
                BigDecimal currentamount = actualPrice.multiply(unqualiqty).multiply(unqualiqty);// 金额（本币位）
                dynamic.set("amount", amount);// 金额
                dynamic.set("curamount", currentamount);// 金额（本币位）
                dynamic.set("taxamount", original.subtract(amount));// 税额
                dynamic.set("curtaxamount", original.multiply(exchangerate).subtract(currentamount));// 税额（本币位）



            }
        });
        SaveServiceHelper.update(procureMaterials);
    }
}
