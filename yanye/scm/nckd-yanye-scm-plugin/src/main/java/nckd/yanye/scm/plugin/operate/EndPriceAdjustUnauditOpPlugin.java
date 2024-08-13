package nckd.yanye.scm.plugin.operate;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.cal.business.account.CloseAccountParamBuilder;
import kd.fi.cal.common.helper.AccountingSysHelper;
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
        fieldKeys.add("nckd_businessdate");
        fieldKeys.add("nckd_adjustaccountsorg");
    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                Arrays.stream(dataEntities).forEach(k -> {
                    DynamicObject dataEntity = k.getDataEntity();
                    Date bizdate = dataEntity.getDate("nckd_businessdate");

                    // 获取对应组织的上次关账日期
                    Date closedate = loadGrid(dataEntity.getDynamicObject("nckd_adjustaccountsorg").getLong("id"));

                    try {
                        if(closedate != null && getYearMonthDay(bizdate).compareTo(getYearMonthDay(closedate)) <= 0){
                            this.addErrorMessage(k, "已关账，月末调价单不允许反审核");
                        }
                    } catch (ParseException parseException) {
                        parseException.printStackTrace();
                    }

                    DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("entryentity");
                    // 获取暂估应付单编号
                    List<String> numberList = entryentity.stream().map(object -> object.getString("nckd_oddnumber").split("_")[0])
                            .distinct().collect(Collectors.toList());

                    QFilter qFilter = new QFilter("billno", QCP.in, numberList);
                    // 获取暂估应付单
                    DynamicObject[] apBusbills = BusinessDataServiceHelper.load("ap_busbill", "", qFilter.toArray());
                    Arrays.stream(apBusbills).forEach(dynamicObject -> {
                        boolean exists = QueryServiceHelper.exists("ap_finapbill", new QFilter[]{new QFilter("sourcebillid", QCP.equals, dynamicObject.getPkValue())});
                        if(exists){
                            this.addErrorMessage(k, "暂估应付单存在下游财务应付单，月末调价单不允许反审核");
                        }

                        long count = dynamicObject.getDynamicObjectCollection("entry").stream().filter(dynamic ->
                            dynamic.getBigDecimal("e_invoicedqty").compareTo(new BigDecimal(0)) != 0
                        ).count();
                        if(count > 0){
                            this.addErrorMessage(k, "暂估应付单存在确认应付的物料，月末调价单不允许反审核");
                        }
                    });
                });
            }
        });
    }

    private static Date getYearMonthDay(Date date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.parse(sdf.format(date));
    }

    /**
     * 获取对应组织的上次关账日期
     * @param orgId
     * @return
     */
    private Date loadGrid(Long orgId) {
        Date closedate = null;
        List<Long> orgList = new ArrayList<>();
        orgList.add(orgId);

        DynamicObjectCollection accSysColl = AccountingSysHelper.getAccountingSysColls(orgList, null);
        if (accSysColl.size() != 0) {
            Set<Long> ownerIdSet = new HashSet();
            Set<Long> calorgSet = new HashSet();
            Iterator var6 = accSysColl.iterator();

            while (var6.hasNext()) {
                DynamicObject accSysInfo = (DynamicObject) var6.next();
                ownerIdSet.add(accSysInfo.getLong("ownerid"));
                calorgSet.add(accSysInfo.getLong("calorgid"));
            }

            Long[] calOrgIds = AccountingSysHelper.getCalOrgIds(calorgSet);
            calorgSet.retainAll(Arrays.asList(calOrgIds));
            Map<Long, Date> calOrgIdCurPeriodMaxEndateMap = CloseAccountParamBuilder.getCalOrgCurPeriodMaxEndDateMap(calorgSet);
            Map<Long, DynamicObject> ownerIdLastCloseAccountDycMap = CloseAccountParamBuilder.getOwnerIdLastCloseAcctDycMap(ownerIdSet);
            List<DynamicObject> hasAccountAccSysDycs = new ArrayList(16);
            Iterator lastInfo = accSysColl.iterator();

            Long calorgid;
            while (lastInfo.hasNext()) {
                DynamicObject accSysDyc = (DynamicObject) lastInfo.next();
                calorgid = accSysDyc.getLong("calorgid");
                if (calorgSet.contains(calorgid)) {
                    hasAccountAccSysDycs.add(accSysDyc);
                }
            }

            accSysColl.clear();
            accSysColl.addAll(hasAccountAccSysDycs);
            if (!accSysColl.isEmpty()) {
                calorgid = accSysColl.get(0).getLong("calorgid");
                if (calorgSet.contains(calorgid)) {
                    Long ownerId = accSysColl.get(0).getLong("ownerid");
                    DynamicObject ownerIdLastClose = ownerIdLastCloseAccountDycMap.get(ownerId);

                    if (ownerIdLastClose != null) {
                        closedate = ownerIdLastClose.getDate("closedate");
                    }
                }
            }
        }

        return closedate;
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
                            object.set("e_actunitprice", dynamicObject.getBigDecimal("nckd_oldunitprice"));
                            // 税额
                            object.set("e_tax", dynamicObject.getBigDecimal("nckd_odltax"));
                            // 税额本位币
                            object.set("e_taxlocalamt", dynamicObject.getBigDecimal("nckd_odltax"));
                            // 含税单价
                            object.set("e_taxunitprice", dynamicObject.getBigDecimal("nckd_oldftaxunitprice"));
                            // 实际含税单价
                            object.set("e_acttaxunitprice", dynamicObject.getBigDecimal("nckd_oldftaxunitprice"));
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
                            // 可抵扣税额
                            object.set("curdeductibleamt", dynamicObject.getBigDecimal("nckd_odltax"));
                            // 未冲回应付金额
                            object.set("e_unwoffamt", dynamicObject.getBigDecimal("nckd_oldtotalprice"));
                            // 未冲回应付金额(本位币)
                            object.set("e_unwofflocamt", dynamicObject.getBigDecimal("nckd_oldtotalprice"));
                            // 未冲回金额
                            object.set("e_unwoffnotaxamt", dynamicObject.getBigDecimal("nckd_oldamount"));
                            // 未冲回金额(本位币)
                            object.set("e_unwoffnotaxlocamt", dynamicObject.getBigDecimal("nckd_oldamount"));
                            // 未冲回税额
                            object.set("e_unwofftax", dynamicObject.getBigDecimal("nckd_odltax"));
                            // 未冲回税额(本位币)
                            object.set("e_unwofftaxlocal", dynamicObject.getBigDecimal("nckd_odltax"));
                            // 未确认应付金额(含税)
                            object.set("e_uninvoicedamt", dynamicObject.getBigDecimal("nckd_oldtotalprice"));
                            // 未确认应付金额(含税本位币)
                            object.set("e_uninvoicedlocamt", dynamicObject.getBigDecimal("nckd_oldtotalprice"));
                            // 未确认应付金额(不含税)
                            object.set("e_uninvnotaxamt", dynamicObject.getBigDecimal("nckd_oldamount"));
                            // 未确认应付金额(不含税本位币)
                            object.set("e_uninvnotaxlocalamt", dynamicObject.getBigDecimal("nckd_oldamount"));

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
