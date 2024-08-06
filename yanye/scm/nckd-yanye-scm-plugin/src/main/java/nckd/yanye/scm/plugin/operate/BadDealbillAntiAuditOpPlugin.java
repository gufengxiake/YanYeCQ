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
import java.util.*;
import java.util.stream.Collectors;
/**
 * Module           :质量云-来料质量管理-不合格品处理
 * Description      :来料不良品处理单审核插件
 *
 * @author : yaosijie
 * @date : 2024/7/22
 */
public class BadDealbillAntiAuditOpPlugin extends AbstractOperationServicePlugIn {



    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
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
                        "billentry.nckd_amountand,billentry.nckd_amountand_current,billentry.srcbillid,billentry.materialmasterid,billentry.price,billentry.priceandtax" +
                        ",billentry.nckd_pricecurrent,billentry.nckd_taxpricecurrent,billentry.nckd_amount,billentry.nckd_amountcurrency" +
                        ",billentry.nckd_taxamount,billentry.nckd_taxamountcurrent,billentry.price,billentry.priceandtax,billentry.amount" +
                        ",billentry.curamount,billentry.taxamount,billentry.curtaxamount", qFilter1.toArray());

        Map<Long, DynamicObject> purreceivebillMap = Arrays.stream(procureMaterials).collect(Collectors.toMap(t -> t.getLong("id"), t -> t));
        //遍历来料不良品处理单全部分录
        billentry.stream().filter(t -> t.getBigDecimal("nckd_discount_amount") != null
                && t.getBigDecimal("nckd_discount_amount").compareTo(BigDecimal.ZERO) > 0).forEach(k -> {
            //这是不良品物料id
            Object materielid = k.getDynamicObject("materielid").getPkValue();
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
                //获取采购收货单分录
                DynamicObjectCollection billentryDynamicObjectC = procureDynamicObject.getDynamicObjectCollection("billentry");
                //构造采购收货单map
                Map<Object, DynamicObject> materialmasterMap = billentryDynamicObjectC.stream().collect(Collectors.toMap(t -> t.getDynamicObject("materialmasterid").getPkValue(), t -> t));
                //采购收货单的对应物料的分录
                DynamicObject dynamic = materialmasterMap.get(materielid);
                //采购收货单的对应物料的分录
                dynamic.set("amountandtax",dynamic.getBigDecimal("nckd_amountand"));//价税合计
                dynamic.set("curamountandtax",dynamic.getBigDecimal("nckd_amountand_current"));//价税合计(本位币)
                dynamic.set("actualprice",dynamic.getBigDecimal("nckd_pricecurrent"));//实际单价
                dynamic.set("actualtaxprice",dynamic.getBigDecimal("nckd_taxpricecurrent"));//实际含税单价

                dynamic.set("price",dynamic.getBigDecimal("nckd_pricecurrent"));//单价
                dynamic.set("priceandtax",dynamic.getBigDecimal("nckd_taxpricecurrent"));// 含税单价
                dynamic.set("amount",dynamic.getBigDecimal("nckd_amount"));// 金额
                dynamic.set("curamount",dynamic.getBigDecimal("nckd_amountcurrency"));// 金额（本币位）
                dynamic.set("taxamount",dynamic.getBigDecimal("nckd_taxamount"));// 税额
                dynamic.set("curtaxamount",dynamic.getBigDecimal("nckd_taxamountcurrent"));// 税额（本币位）
            }
        });
        SaveServiceHelper.update(procureMaterials);
    }
}
