package nckd.yanye.scm.plugin.operate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang.StringUtils;

/**
 * @author husheng
 * @date 2024-07-25 16:22
 * @description 月末调价单反审核插件
 */
public class EndPriceAdjustUnauditOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);

        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_oddnumber");
        fieldKeys.add("nckd_oldunitprice");
        fieldKeys.add("nckd_odltax");
        fieldKeys.add("nckd_oldftaxunitprice");
        fieldKeys.add("nckd_oldamount");
        fieldKeys.add("nckd_oldtotalprice");
        fieldKeys.add("nckd_oldcostamount");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);

        for (DynamicObject dataEntity : e.getDataEntities()) {
            // 获取月末调价单暂估数据页签分录
            DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("entryentity");
            // 获取暂估应付单编号
            List<String> numberList = entryentity.stream().map(object -> {
                return object.getString("nckd_oddnumber").split("_")[0];
            }).distinct().collect(Collectors.toList());
//            String oddnumber = entryentity.get(0).getString("nckd_oddnumber").split("_")[0];
            QFilter qFilter = new QFilter("billno", QCP.in, numberList);
            // 获取暂估应付单
//            QueryServiceHelper.query()
            DynamicObject[] apBusbills = BusinessDataServiceHelper.load("ap_busbill", "", qFilter.toArray());
            for (DynamicObject apBusbill : apBusbills) {
                apBusbill = BusinessDataServiceHelper.loadSingle(apBusbill.get("id"),"ap_busbill");
                // 获取暂估应付单明细分录
                DynamicObjectCollection entry = apBusbill.getDynamicObjectCollection("entry");
                // 获取采购入库单
                DynamicObject purinbill = BusinessDataServiceHelper.loadSingle(apBusbill.getString("sourcebillid"), "im_purinbill");

                List<Object> list = new ArrayList<>();
                BigDecimal pricetaxtotal = new BigDecimal(0);
                BigDecimal amount = new BigDecimal(0);
                BigDecimal tax = new BigDecimal(0);
                // 反写暂估应付单明细分录
                for (DynamicObject object : entry) {
                    for (DynamicObject dynamicObject : entryentity) {
                        if (StringUtils.equals(object.getString("nckd_billnumber"), dynamicObject.getString("nckd_oddnumber"))) {
                            // 单价
                            object.set("e_unitprice", dynamicObject.getBigDecimal("nckd_oldunitprice"));
                            // 实际单价
                            object.set("e_actunitprice", dynamicObject.getBigDecimal("nckd_newunitprice"));
                            // 税额
                            object.set("e_tax", dynamicObject.getBigDecimal("nckd_odltax"));
                            // 税额本位币
                            object.set("e_taxlocalamt", dynamicObject.getBigDecimal("nckd_odltax"));
                            // 含税单价
                            object.set("e_taxunitprice", dynamicObject.getBigDecimal("nckd_oldftaxunitprice"));
                            // 实际含税单价
                            object.set("e_acttaxunitprice", dynamicObject.getBigDecimal("nckd_newftaxunitprice"));
                            // 金额
                            object.set("e_amount", dynamicObject.getBigDecimal("nckd_oldamount"));
                            // 金额本位币
                            object.set("e_localamt", dynamicObject.getBigDecimal("nckd_oldamount"));
                            // 价税合计
                            object.set("e_pricetaxtotal", dynamicObject.getBigDecimal("nckd_oldtotalprice"));
                            // 价税合计本位币
                            object.set("e_pricetaxtotalbase", dynamicObject.getBigDecimal("nckd_oldtotalprice"));
                            // 计成本金额
                            object.set("intercostamt", dynamicObject.getBigDecimal("nckd_oldcostamount"));

                            list.add(object.getPkValue());
                        }
                    }

                    pricetaxtotal = pricetaxtotal.add(object.getBigDecimal("e_pricetaxtotal"));
                    amount = amount.add(object.getBigDecimal("e_amount"));
                    tax = tax.add(object.getBigDecimal("e_tax"));
                }
                // 反写金额信息
                apBusbill.set("pricetaxtotal", pricetaxtotal);//应付金额
                apBusbill.set("pricetaxtotalbase", pricetaxtotal);//应付金额(本位币)
                apBusbill.set("amount", amount);//金额
                apBusbill.set("localamt", amount);//金额(本位币)
                apBusbill.set("tax", tax);//税额
                apBusbill.set("taxlocamt", tax);//税额(本位币)
                apBusbill.set("unwoffamt", pricetaxtotal);//未冲回应付金额
                apBusbill.set("unwofflocamt", pricetaxtotal);//未冲回应付金额(本位币)
                apBusbill.set("unwoffnotaxamt", amount);//未冲回金额
                apBusbill.set("unwoffnotaxlocamt", amount);//未冲回金额(本位币)
                apBusbill.set("unwofftax", tax);//未冲回税额
                apBusbill.set("unwofftaxlocal", tax);//未冲回税额(本位币)
                apBusbill.set("uninvoicedamt", pricetaxtotal);//未确认应付金额(含税)
                apBusbill.set("uninvoicedlocamt", pricetaxtotal);//未确认应付金额(含税本位币)
                SaveServiceHelper.update(apBusbill);


                // 反写采购入库单物料分录
                for (DynamicObject billentry : purinbill.getDynamicObjectCollection("billentry")) {
                    for (DynamicObject dynamicObject : entry) {
                        if (Objects.equals(billentry.getPkValue(), dynamicObject.get("e_srcentryid")) && list.contains(dynamicObject.getPkValue())) {
                            // 单价
                            billentry.set("price", dynamicObject.getBigDecimal("e_unitprice"));
                            // 实际单价
                            billentry.set("actualprice", dynamicObject.getBigDecimal("e_unitprice"));
                            // 税额
                            billentry.set("taxamount", dynamicObject.getBigDecimal("e_tax"));
                            // 税额本位币
                            billentry.set("curtaxamount", dynamicObject.getBigDecimal("e_taxlocalamt"));
                            // 含税单价
                            billentry.set("priceandtax", dynamicObject.getBigDecimal("e_taxunitprice"));
                            // 实际含税单价
                            billentry.set("actualtaxprice", dynamicObject.getBigDecimal("e_taxunitprice"));
                            // 金额
                            billentry.set("amount", dynamicObject.getBigDecimal("e_amount"));
                            // 金额本位币
                            billentry.set("curamount", dynamicObject.getBigDecimal("e_localamt"));
                            // 价税合计
                            billentry.set("amountandtax", dynamicObject.getBigDecimal("e_pricetaxtotal"));
                            // 价税合计本位币
                            billentry.set("curamountandtax", dynamicObject.getBigDecimal("e_pricetaxtotalbase"));
                            // 计成本金额
                            billentry.set("intercostamt", dynamicObject.getBigDecimal("intercostamt"));
                        }
                    }
                }
                SaveServiceHelper.update(purinbill);

                QFilter qf = new QFilter("billno", QCP.equals, purinbill.getString("billno"));
                DynamicObject dynamicObjects = BusinessDataServiceHelper.loadSingle("cal_costrecord", qf.toArray());
                DynamicObject[] objects = {dynamicObjects};
                OperationResult resync = OperationServiceHelper.executeOperate("resync", "cal_costrecord", objects, OperateOption.create());
                if (!resync.isSuccess()) {
                    throw new KDBizException("核算成本记录同步服务失败：" + resync.getMessage());
                }
            }
        }
    }
}
