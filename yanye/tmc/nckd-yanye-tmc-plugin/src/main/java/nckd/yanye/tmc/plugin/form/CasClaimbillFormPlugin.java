package nckd.yanye.tmc.plugin.form;

import cn.hutool.core.util.ObjectUtil;
import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.ObjectUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.extplugin.PluginProxy;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.cas.enums.ClaimCoreBillTypeEnum;
import kd.fi.cas.helper.CasHelper;
import kd.fi.cas.util.EmptyUtil;
import kd.sdk.fi.cas.extpoint.claimbill.IClaimHandlePluginSDK;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :财务云-出纳-收款认领
 * Description      :1.认领处理单，增加核心单据可选要货订单
 *
 * @author : zhujintao
 * @date : 2024/8/6
 */
public class CasClaimbillFormPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        if (StringUtils.equals("e_receivableamt", name)) {
            ChangeData changeData = e.getChangeSet()[0]; //修改值所在行
            DynamicObject dataEntity = changeData.getDataEntity(); //修改值所在行数据
            Object newValue = changeData.getNewValue();//新值
            int rowIndex = changeData.getRowIndex(); //修改行所在行行号
            String billType = dataEntity.getString("e_corebilltype");
            String billNo = dataEntity.getString("e_corebillno");
            int eCorebillentryseq = dataEntity.getInt("e_corebillentryseq");
            BigDecimal eReceivableamt = dataEntity.getBigDecimal("e_receivableamt");
            //必须是销售订单且核心单据编号不为空
            if ("sm_salorder".equals(billType) && ObjectUtil.isNotEmpty(billNo) && ObjectUtil.isNotEmpty(newValue)) {
                QFilter qFilter = new QFilter("billno", QCP.equals, billNo);
                DynamicObject smSalorder = BusinessDataServiceHelper.loadSingle("sm_salorder", "id,recplanentry.seq,recplanentry.r_unremainamount", qFilter.toArray());
                DynamicObjectCollection recplanentryColl = smSalorder.getDynamicObjectCollection("recplanentry");
                DynamicObject recplanentry = recplanentryColl.get(eCorebillentryseq - 1);
                BigDecimal rUnremainamount = recplanentry.getBigDecimal("r_unremainamount");
                if (eReceivableamt.compareTo(rUnremainamount) > 0) {
                    this.getView().showErrorNotification("应收金额不能大于销售订单" + billNo + "收款计划第" + eCorebillentryseq + "行的未关联收款金额");
                    this.getModel().setValue("e_actamt", 0, rowIndex);
                    this.getModel().setValue("e_receivableamt", 0, rowIndex);
                }
            }
        }
    }

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
        Object settleorg = this.getModel().getValue("e_settleorg", iRow);
        if ("ocbsoc_saleorder".equals(corebilltype)) {
            ListShowParameter lsp = ShowFormHelper.createShowListForm(String.valueOf(corebilltype), false, 2);
            List<QFilter> qFilters = lsp.getListFilterParameter().getQFilters();
            qFilters.add(new QFilter("saleorgid", QCP.equals, settleorg != null ? ((DynamicObject) settleorg).getPkValue() : null));
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
                int currentRowIndex = model.getEntryCurrentRowIndex("entryentity");
                String coreBillType = (String) model.getValue("e_corebilltype", currentRowIndex);

                Boolean claimComtrol = (Boolean) this.getModel().getValue("claimcomtrol", currentRowIndex);
                String claimDimenSion = (String) this.getModel().getValue("claimdimension", currentRowIndex);
                ListSelectedRowCollection coreBillCollection = (ListSelectedRowCollection) returnData;
                int entryRowCount = model.getEntryRowCount("entryentity");
                int followingRowCount = entryRowCount - currentRowIndex - 1;
                if (coreBillCollection.size() - 1 > followingRowCount) {
                    model.appendEntryRow("entryentity", entryRowCount - 1, coreBillCollection.size() - 1 - followingRowCount);
                }

                DynamicObject settleorg = (DynamicObject) model.getValue("e_settleorg", currentRowIndex);
                FormShowParameter formShowParameter = this.getView().getFormShowParameter();
                formShowParameter.setCustomParam("isSelectCoreBillNo", "true");
                Map<Integer, Long> actMap = new HashMap(coreBillCollection.size());
                Set<Long> actSet = new HashSet(coreBillCollection.size());
                Set<Long> entryIdSet = new HashSet();
                String entryEntityKey = "";

                for (int i = 0; i < coreBillCollection.size(); ++i) {
                    ListSelectedRow listSelectedRow = coreBillCollection.get(i);
                    entryEntityKey = listSelectedRow.getEntryEntityKey();
                    Object entryPKValue = listSelectedRow.getEntryPrimaryKeyValue();
                    if (entryPKValue != null) {
                        entryIdSet.add((Long) entryPKValue);
                    }

                    actMap.put(currentRowIndex + i, (Long) listSelectedRow.getPrimaryKeyValue());
                    actSet.add((Long) listSelectedRow.getPrimaryKeyValue());
                }

                PluginProxy<IClaimHandlePluginSDK> pluginProxy = PluginProxy.create(IClaimHandlePluginSDK.class, "kd.sdk.fi.cas.extpoint.claimbill.IClaimHandlePluginSDK");
                Map<Long, Map<String, Object>> entryId_Seq = new HashMap();
                if (entryIdSet.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(entryEntityKey).append(".id,").append(entryEntityKey).append(".seq");
                    String finalSelectField = sb.toString();
                    Set<String> extFieldSet = new HashSet(10);
                    List<Set<String>> extField = pluginProxy.callReplace((p) -> {
                        return null;
                        //return p.getSelectFieldWithEntry(coreBillType, entryEntityKey, finalSelectField);
                    });
                    if (EmptyUtil.isNoEmpty(extField) && extField.size() > 0 && EmptyUtil.isNoEmpty(extField.get(0))) {
                        extFieldSet.addAll((Collection) extField.get(0));
                        Iterator var24 = ((Set) extField.get(0)).iterator();

                        while (var24.hasNext()) {
                            String oneField = (String) var24.next();
                            sb.append(',').append(oneField);
                        }
                    }

                    QFilter[] filters = new QFilter[]{new QFilter(entryEntityKey + ".id", "in", entryIdSet)};
                    DataSet ds = QueryServiceHelper.queryDataSet("queryEntry", coreBillType, sb.toString(), filters, (String) null);
                    Throwable var26 = null;

                    try {
                        while (ds.hasNext()) {
                            Row row = ds.next();
                            Long entryId = row.getLong(entryEntityKey + ".id");
                            Map<String, Object> value = new HashMap(2);
                            value.put(entryEntityKey + ".seq", row.getString(entryEntityKey + ".seq"));
                            Iterator var30 = extFieldSet.iterator();

                            while (var30.hasNext()) {
                                String field = (String) var30.next();
                                value.put(field, row.get(field));
                            }

                            entryId_Seq.put(entryId, value);
                        }
                    } catch (Throwable var39) {
                        var26 = var39;
                        throw var39;
                    } finally {
                        if (ds != null) {
                            if (var26 != null) {
                                try {
                                    ds.close();
                                } catch (Throwable var38) {
                                    var26.addSuppressed(var38);
                                }
                            } else {
                                ds.close();
                            }
                        }

                    }
                }

                pluginProxy.callReplace((p) -> {
                    p.fillResult(coreBillType, actSet, entryId_Seq);
                    return null;
                });

                for (int i = 0; i < coreBillCollection.size(); ++i) {
                    ListSelectedRow listSelectedRow = coreBillCollection.get(i);
                    Long entryPKValue = (Long) listSelectedRow.getEntryPrimaryKeyValue();
                    DynamicObject rowData = (DynamicObject) model.getEntryEntity("entryentity").get(currentRowIndex + i);
                    rowData.set("e_settleorg", settleorg);
                    rowData.set("e_corebilltype", coreBillType);
                    model.setValue("e_corebillno", listSelectedRow.getBillNo(), currentRowIndex + i);
                    model.setValue("e_corebillnoinput", listSelectedRow.getBillNo(), currentRowIndex + i);
                    model.setValue("e_corebillid", listSelectedRow.getPrimaryKeyValue(), currentRowIndex + i);
                    rowData.set("e_corebillno", listSelectedRow.getBillNo());
                    rowData.set("e_corebillnoinput", listSelectedRow.getBillNo());
                    rowData.set("e_corebillid", listSelectedRow.getPrimaryKeyValue());
                    rowData.set("claimcomtrol", claimComtrol);
                    rowData.set("claimdimension", claimDimenSion);
                    if (EmptyUtil.isNoEmpty(entryId_Seq.get(entryPKValue))) {
                        String seq = (String) ((Map) entryId_Seq.get(entryPKValue)).get(entryEntityKey + ".seq");
                        if (EmptyUtil.isNoEmpty(seq)) {
                            model.setValue("e_corebillentryseq", seq, currentRowIndex + i);
                            rowData.set("e_corebillentryseq", seq);
                        }
                    }

                    boolean isSelectEntry = !kd.bos.dataentity.utils.StringUtils.isEmpty(entryEntityKey) && !ObjectUtils.isEmpty(entryPKValue);
                    if (isSelectEntry) {
                        rowData.set("e_corebillentryid", entryPKValue);
                    } else {
                        rowData.set("e_corebillentryid", (Object) null);
                        rowData.set("e_corebillentryseq", (Object) null);
                    }

                    if (ClaimCoreBillTypeEnum.SALORDER.getValue().equals(coreBillType) || ClaimCoreBillTypeEnum.SALCONTRACT.getValue().equals(coreBillType)) {
                        boolean isSalOrder = ClaimCoreBillTypeEnum.SALORDER.getValue().equals(coreBillType);
                        boolean isMaterialEntry = !kd.bos.dataentity.utils.StringUtils.isEmpty(entryEntityKey) && "billentry".equals(entryEntityKey);
                        List conGroupFields;
                        DynamicObject queryInfo;
                        if (isSelectEntry && isMaterialEntry) {
                            conGroupFields = isSalOrder ? Arrays.asList(entryEntityKey + ".conbillnumber", entryEntityKey + ".conbillrownum", entryEntityKey + ".conbillid", entryEntityKey + ".conbillentryid") : Arrays.asList("id", "billno", entryEntityKey + ".lineno", entryEntityKey + ".id");
                            QFilter[] filters = new QFilter[]{new QFilter(entryEntityKey + ".id", "=", entryPKValue)};
                            queryInfo = QueryServiceHelper.queryOne(coreBillType, String.join(",", conGroupFields), filters);
                            if (queryInfo != null) {
                                rowData.set("conbillentity", "conm_salcontract");
                                rowData.set("conbillnumber", isSalOrder ? queryInfo.getString(entryEntityKey + ".conbillnumber") : queryInfo.getString("billno"));
                                rowData.set("conbillrownum", isSalOrder ? queryInfo.getString(entryEntityKey + ".conbillrownum") : queryInfo.getString(entryEntityKey + ".lineno"));
                                rowData.set("conbillid", isSalOrder ? queryInfo.getLong(entryEntityKey + ".conbillid") : queryInfo.getLong("id"));
                                rowData.set("conbillentryid", isSalOrder ? queryInfo.getLong(entryEntityKey + ".conbillentryid") : queryInfo.getLong(entryEntityKey + ".id"));
                            }
                        } else {
                            conGroupFields = isSalOrder ? Arrays.asList("billentry.conbillnumber", "billentry.conbillrownum", "billentry.conbillid", "billentry.conbillentryid") : Arrays.asList("id", "billno", "billentry.id");
                            QFilter filter = new QFilter("id", "=", listSelectedRow.getPrimaryKeyValue());
                            queryInfo = QueryServiceHelper.queryOne(coreBillType, String.join(",", conGroupFields), new QFilter[]{filter});
                            if (queryInfo != null) {
                                rowData.set("conbillnumber", isSalOrder ? queryInfo.getString("billentry.conbillnumber") : queryInfo.getString("billno"));
                                rowData.set("conbillid", isSalOrder ? queryInfo.getLong("billentry.conbillid") : queryInfo.getLong("id"));
                                rowData.set("conbillentity", "conm_salcontract");
                                rowData.set("conbillrownum", "");
                                if (!isSalOrder) {
                                    rowData.set("conbillentryid", entryPKValue);
                                }
                            }
                        }
                    }

                    pluginProxy.callReplace((p) -> {
                        p.dealResult(entryId_Seq, rowData);
                        return null;
                    });
                    model.getEntryEntity("entryentity").set(currentRowIndex + i, rowData);
                }

                this.getModel().updateEntryCache(model.getEntryEntity("entryentity"));
                this.getView().updateView("entryentity");
                if (kd.bos.dataentity.utils.StringUtils.equals(coreBillType, ClaimCoreBillTypeEnum.PAYBILL.getValue())) {
                    QFilter qFilter = new QFilter("id", "in", actSet);
                    Map<Long, BigDecimal> actpayamt = (Map) QueryServiceHelper.query("cas_paybill", "id , actpayamt", new QFilter[]{qFilter}).stream().collect(Collectors.toMap((dy) -> {
                        return dy.getLong("id");
                    }, (dy) -> {
                        return dy.getBigDecimal("actpayamt");
                    }));
                    Iterator var49 = actMap.entrySet().iterator();

                    while (var49.hasNext()) {
                        Map.Entry<Integer, Long> act = (Map.Entry) var49.next();
                        this.getModel().setValue("e_receivableamt", actpayamt.get(act.getValue()), (Integer) act.getKey());
                    }
                }
            }
        }
    }
}
