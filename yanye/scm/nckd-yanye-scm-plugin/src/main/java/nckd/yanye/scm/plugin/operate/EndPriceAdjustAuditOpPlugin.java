package nckd.yanye.scm.plugin.operate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang.StringUtils;

/**
 * @author husheng
 * @date 2024-07-25 16:16
 * @description 月末调价单审核插件
 */
public class EndPriceAdjustAuditOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);

        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_oddnumber");
        fieldKeys.add("nckd_newunitprice");
        fieldKeys.add("nckd_newtax");
        fieldKeys.add("nckd_newftaxunitprice");
        fieldKeys.add("nckd_newamount");
        fieldKeys.add("nckd_newtotalprice");
        fieldKeys.add("nckd_newcostamount");
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);

        for (DynamicObject dataEntity : e.getDataEntities()) {
            // 获取月末调价单暂估数据页签分录
            DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("entryentity");
            // 获取暂估应付单编号
            String oddnumber = entryentity.get(0).getString("nckd_oddnumber").split("_")[0];
            QFilter qFilter = new QFilter("billno", QCP.equals,oddnumber);
            // 获取暂估应付单
            DynamicObject apBusbill = BusinessDataServiceHelper.loadSingle("ap_busbill", qFilter.toArray());
            // 获取暂估应付单明细分录
            DynamicObjectCollection entry = apBusbill.getDynamicObjectCollection("entry");
            // 获取采购入库单
            DynamicObject purinbill = BusinessDataServiceHelper.loadSingle(apBusbill.getString("sourcebillid"), "im_purinbill");

            BigDecimal pricetaxtotal = new BigDecimal(0);
            BigDecimal amount = new BigDecimal(0);
            BigDecimal tax = new BigDecimal(0);
            // 反写暂估应付单明细分录
            for (DynamicObject object : entry) {
                for (DynamicObject dynamicObject : entryentity) {
                    if(StringUtils.equals(object.getString("nckd_billnumber"),dynamicObject.getString("nckd_oddnumber"))){
                        // 单价
                        object.set("e_unitprice",dynamicObject.getBigDecimal("nckd_newunitprice"));
                        // 税额
                        object.set("e_tax",dynamicObject.getBigDecimal("nckd_newtax"));
                        // 税额本位币
                        object.set("e_taxlocalamt",dynamicObject.getBigDecimal("nckd_newtax"));
                        // 含税单价
                        object.set("e_taxunitprice",dynamicObject.getBigDecimal("nckd_newftaxunitprice"));
                        // 金额
                        object.set("e_amount",dynamicObject.getBigDecimal("nckd_newamount"));
                        // 金额本位币
                        object.set("e_localamt",dynamicObject.getBigDecimal("nckd_newamount"));
                        // 价税合计
                        object.set("e_pricetaxtotal",dynamicObject.getBigDecimal("nckd_newtotalprice"));
                        // 价税合计本位币
                        object.set("e_pricetaxtotalbase",dynamicObject.getBigDecimal("nckd_newtotalprice"));
                        // 计成本金额
                        object.set("intercostamt",dynamicObject.getBigDecimal("nckd_newcostamount"));

                        pricetaxtotal = pricetaxtotal.add(dynamicObject.getBigDecimal("nckd_newtotalprice"));
                        amount = amount.add(dynamicObject.getBigDecimal("nckd_newamount"));
                        tax = tax.add(dynamicObject.getBigDecimal("nckd_newtax"));
                    }
                }
            }
            // 反写金额信息
            apBusbill.set("pricetaxtotal",pricetaxtotal);//应付金额
            apBusbill.set("pricetaxtotalbase",pricetaxtotal);//应付金额(本位币)
            apBusbill.set("amount",amount);//金额
            apBusbill.set("localamt",amount);//金额(本位币)
            apBusbill.set("tax",tax);//税额
            apBusbill.set("taxlocamt",tax);//税额(本位币)
            SaveServiceHelper.update(apBusbill);


            // 反写采购入库单物料分录
            for (DynamicObject billentry : purinbill.getDynamicObjectCollection("billentry")) {
                for (DynamicObject dynamicObject : entry) {
                    if(Objects.equals(billentry.getPkValue(),dynamicObject.get("e_srcentryid"))){
                        // 单价
                        billentry.set("price",dynamicObject.getBigDecimal("e_unitprice"));
                        // 税额
                        billentry.set("taxamount",dynamicObject.getBigDecimal("e_tax"));
                        // 税额本位币
                        billentry.set("curtaxamount",dynamicObject.getBigDecimal("e_taxlocalamt"));
                        // 含税单价
                        billentry.set("priceandtax",dynamicObject.getBigDecimal("e_taxunitprice"));
                        // 金额
                        billentry.set("amount",dynamicObject.getBigDecimal("e_amount"));
                        // 金额本位币
                        billentry.set("curamount",dynamicObject.getBigDecimal("e_localamt"));
                        // 价税合计
                        billentry.set("amountandtax",dynamicObject.getBigDecimal("e_pricetaxtotal"));
                        // 价税合计本位币
                        billentry.set("curamountandtax",dynamicObject.getBigDecimal("e_pricetaxtotalbase"));
                        // 计成本金额
                        billentry.set("intercostamt",dynamicObject.getBigDecimal("intercostamt"));
                    }
                }
            }
            SaveServiceHelper.update(purinbill);

            QFilter qf = new QFilter("billno", QCP.equals,purinbill.getString("billno"));
            DynamicObject dynamicObjects = BusinessDataServiceHelper.loadSingle("cal_costrecord", qf.toArray());
            DynamicObject[] objects = {dynamicObjects};
            OperationResult resync = OperationServiceHelper.executeOperate("resync", "cal_costrecord", objects, OperateOption.create());
            if(!resync.isSuccess()){
                throw new KDBizException("核算成本记录同步服务失败：" + resync.getMessage());
            }
        }
    }
}
