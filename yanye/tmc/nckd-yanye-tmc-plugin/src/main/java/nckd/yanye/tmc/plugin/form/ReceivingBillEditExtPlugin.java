package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.utils.ObjectUtils;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.cas.enums.ClaimCoreBillTypeEnum;
import kd.fi.cas.helper.CasHelper;
import kd.fi.cas.util.EmptyUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           资金云-收款单
 * Description      :因为要货订单的聚合支付暂时不能整合，先提供备选方案，客户直接在收款单上选择要货订单
 *
 * @author : zhujintao
 * @date : 2024/8/26
 */
public class ReceivingBillEditExtPlugin extends AbstractBillPlugIn {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"e_corebillno"});
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control) evt.getSource();
        switch (c.getKey().toLowerCase()) {
            case "e_corebillno":
                this.getCorebilltypeToChange();
                break;
            default:
                break;
        }
    }

    private void getCorebilltypeToChange() {
        int iRow = this.getModel().getEntryCurrentRowIndex("entry");
        Object corebilltype = this.getModel().getValue("e_corebilltype", iRow);
        DynamicObject value = (DynamicObject)this.getModel().getValue("org");//收款人id
        if ("ocbsoc_saleorder".equals(corebilltype)) {
            ListShowParameter lsp = ShowFormHelper.createShowListForm(String.valueOf(corebilltype), false, 2);
            List<QFilter> qFilters = lsp.getListFilterParameter().getQFilters();
            qFilters.add(new QFilter("saleorgid", QCP.equals, value.getPkValue()));
            lsp.setCustomParam("ismergerows", Boolean.FALSE);
            CloseCallBack closeCallBack = new CloseCallBack(this, "e_corebillno_ext");
            lsp.setCloseCallBack(closeCallBack);
            this.getView().showForm(lsp);
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        super.closedCallBack(e);
        if ("e_corebillno_ext".equals(e.getActionId())) {
            Object returnData = e.getReturnData();
            if (!CasHelper.isEmpty(returnData)) {
                IDataModel model = this.getModel();
                int currentRowIndex = model.getEntryCurrentRowIndex("entry");
                String coreBillType = (String) model.getValue("e_corebilltype", currentRowIndex);
                ListSelectedRowCollection coreBillCollection = (ListSelectedRowCollection) returnData;
                int entryRowCount = model.getEntryRowCount("entry");
                int followingRowCount = entryRowCount - currentRowIndex - 1;
                if (coreBillCollection.size() - 1 > followingRowCount) {
                    model.appendEntryRow("entry", entryRowCount - 1, coreBillCollection.size() - 1 - followingRowCount);
                }

                DynamicObject settleorg = (DynamicObject) model.getValue("e_settleorg", currentRowIndex);
                FormShowParameter formShowParameter = this.getView().getFormShowParameter();
                formShowParameter.setCustomParam("isSelectCoreBillNo", "true");
                Map<Integer, Long> actMap = new HashMap(coreBillCollection.size());

                String entryEntityKey;
                for (int i = 0; i < coreBillCollection.size(); ++i) {
                    ListSelectedRow listSelectedRow = coreBillCollection.get(i);
                    model.setValue("e_settleorg", settleorg, currentRowIndex + i);
                    this.getModel().beginInit();
                    model.setValue("e_corebilltype", coreBillType, currentRowIndex + i);
                    this.getModel().endInit();
                    this.getView().updateView("e_corebilltype", currentRowIndex + i);
                    model.setValue("e_corebillno", listSelectedRow.getBillNo(), currentRowIndex + i);
                    //已匹配核心单据标识
                    model.setValue("e_matchselltag", "1", currentRowIndex + i);
                    //TODO 抄产品的，应该可以删除这一段
                    if (StringUtils.equals(coreBillType, ClaimCoreBillTypeEnum.REPAYMENTBILL.getValue())) {
                        model.setValue("e_sourcebillid", listSelectedRow.getPrimaryKeyValue(), currentRowIndex + i);
                        model.setValue("e_sourcebillentryid", listSelectedRow.getEntryPrimaryKeyValue(), currentRowIndex + i);
                        model.setValue("e_corebillid", listSelectedRow.getEntryPrimaryKeyValue(), currentRowIndex + i);
                        actMap.put(currentRowIndex + i, (Long) listSelectedRow.getEntryPrimaryKeyValue());
                    }

                    entryEntityKey = listSelectedRow.getEntryEntityKey();
                    Object entryPKValue = listSelectedRow.getEntryPrimaryKeyValue();
                    if (!kd.fi.cas.util.StringUtils.isEmpty(entryEntityKey) && !ObjectUtils.isEmpty(entryPKValue)) {
                        QFilter[] filters = new QFilter[]{new QFilter(entryEntityKey + ".id", "=", entryPKValue)};
                        DynamicObject coreBill = QueryServiceHelper.queryOne(coreBillType, entryEntityKey + ".seq", filters);
                        if (coreBill != null) {
                            model.setValue("e_corebillentryseq", coreBill.getString(entryEntityKey + ".seq"), currentRowIndex + i);
                            model.setValue("e_corebillentryid", entryPKValue, currentRowIndex + i);
                        }
                    } else {
                        model.setValue("e_corebillentryseq", (Object) null, currentRowIndex + i);
                        model.setValue("e_corebillentryid", (Object) null, currentRowIndex + i);
                    }

                    this.getModel().beginInit();
                    //TODO 抄产品的，应该可以删除这一段
                    if (ClaimCoreBillTypeEnum.SALORDER.getValue().equals(coreBillType) || ClaimCoreBillTypeEnum.SALCONTRACT.getValue().equals(coreBillType)) {
                        boolean isSalOrder = ClaimCoreBillTypeEnum.SALORDER.getValue().equals(coreBillType);
                        boolean isSelectEntry = !kd.fi.cas.util.StringUtils.isEmpty(entryEntityKey) && !ObjectUtils.isEmpty(entryPKValue);
                        boolean isMaterialEntry = !kd.fi.cas.util.StringUtils.isEmpty(entryEntityKey) && "billentry".equals(entryEntityKey);
                        List conGroupFields;
                        DynamicObject queryInfo;
                        if (isSelectEntry && isMaterialEntry) {
                            conGroupFields = isSalOrder ? Arrays.asList(entryEntityKey + ".conbillnumber", entryEntityKey + ".conbillrownum", entryEntityKey + ".conbillid", entryEntityKey + ".conbillentryid") : Arrays.asList("id", "billno", entryEntityKey + ".lineno", entryEntityKey + ".id");
                            QFilter[] filters = new QFilter[]{new QFilter(entryEntityKey + ".id", "=", entryPKValue)};
                            queryInfo = QueryServiceHelper.queryOne(coreBillType, String.join(",", conGroupFields), filters);
                            if (queryInfo != null) {
                                model.setValue("conbillentity", "conm_salcontract", currentRowIndex + i);
                                model.setValue("conbillnumber", isSalOrder ? queryInfo.getString(entryEntityKey + ".conbillnumber") : queryInfo.getString("billno"), currentRowIndex + i);
                                model.setValue("conbillrownum", isSalOrder ? queryInfo.getString(entryEntityKey + ".conbillrownum") : queryInfo.getString(entryEntityKey + ".lineno"), currentRowIndex + i);
                            }
                        } else {
                            if (isSalOrder) {
                                conGroupFields = Arrays.asList("billentry.conbillnumber", "billentry.conbillrownum", "billentry.conbillid", "billentry.conbillentryid");
                                QFilter filter = new QFilter("id", "=", listSelectedRow.getPrimaryKeyValue());
                                queryInfo = QueryServiceHelper.queryOne(coreBillType, String.join(",", conGroupFields), new QFilter[]{filter});
                                if (queryInfo != null) {
                                    model.setValue("conbillnumber", queryInfo.getString("billentry.conbillnumber"), currentRowIndex + i);
                                }
                            }

                            model.setValue("conbillentity", "conm_salcontract", currentRowIndex + i);
                            model.setValue("conbillrownum", "", currentRowIndex + i);
                        }
                    }

                    this.getModel().endInit();
                    this.getView().updateView("conbillentity", currentRowIndex + i);
                    this.getView().updateView("conbillnumber", currentRowIndex + i);
                    this.getView().updateView("conbillrownum", currentRowIndex + i);
                }

                QFilter qFilter;
                Map actpayamt;
                if (StringUtils.equals(coreBillType, ClaimCoreBillTypeEnum.PAYBILL.getValue())) {
                    qFilter = new QFilter("id", "in", actMap.keySet());
                    actpayamt = (Map) QueryServiceHelper.query("cas_paybill", "id , actpayamt", new QFilter[]{qFilter}).stream().collect(Collectors.toMap((dy) -> {
                        return dy.getLong("id");
                    }, (dy) -> {
                        return dy.getBigDecimal("actpayamt");
                    }));
                    Iterator var32 = actMap.entrySet().iterator();

                    while (var32.hasNext()) {
                        Map.Entry<Integer, Long> act = (Map.Entry) var32.next();
                        this.getModel().setValue("e_receivableamt", actpayamt.get(act.getValue()), (Integer) act.getKey());
                    }
                } else if (StringUtils.equals(coreBillType, ClaimCoreBillTypeEnum.REPAYMENTBILL.getValue())) {
                    qFilter = new QFilter("er_repaymentbill.repaymententry.id", "in", actMap.values());
                    actpayamt = (Map) QueryServiceHelper.query("er_repaymentbill", "er_repaymentbill.repaymententry.id id, er_repaymentbill.repaymententry.orirepayamount orirepayamount, er_repaymentbill.repaymententry.orirecamount orirecamount,er_repaymentbill.repaymententry.remarks remarks,er_repaymentbill.repaymententry.sourceexpenseitem.id expenseitem", new QFilter[]{qFilter}).stream().collect(Collectors.toMap((dy) -> {
                        return dy.getLong("id");
                    }, (dy) -> {
                        return dy;
                    }, (m1, m2) -> {
                        return m1;
                    }));
                    entryEntityKey = null;
                    Iterator var33 = actMap.entrySet().iterator();

                    while (var33.hasNext()) {
                        Map.Entry<Integer, Long> act = (Map.Entry) var33.next();
                        DynamicObject repayDy = (DynamicObject) actpayamt.get(act.getValue());
                        if (EmptyUtil.isNoEmpty(repayDy)) {
                            this.getModel().setValue("e_receivableamt", repayDy.getBigDecimal("orirepayamount").subtract(repayDy.getBigDecimal("orirecamount")), (Integer) act.getKey());
                            this.getModel().setValue("e_actamt", repayDy.getBigDecimal("orirepayamount").subtract(repayDy.getBigDecimal("orirecamount")), (Integer) act.getKey());
                            this.getModel().setValue("e_remark", repayDy.get("remarks"), (Integer) act.getKey());
                            this.getModel().setValue("e_expenseitem", repayDy.get("expenseitem"), (Integer) act.getKey());
                        } else {
                            this.getModel().setValue("e_receivableamt", (Object) null, (Integer) act.getKey());
                            this.getModel().setValue("e_actamt", (Object) null, (Integer) act.getKey());
                            this.getModel().setValue("e_remark", (Object) null, (Integer) act.getKey());
                            this.getModel().setValue("e_expenseitem", (Object) null, (Integer) act.getKey());
                        }
                    }
                }

                formShowParameter.setCustomParam("isSelectCoreBillNo", "false");
            }
        }
    }
}
