package nckd.yanye.tmc.plugin.form;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.datamodel.IBillModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.RowDataEntity;
import kd.bos.entity.datamodel.events.*;
import kd.bos.entity.operate.PushAndSave;
import kd.bos.entity.property.EntryProp;
import kd.bos.form.*;
import kd.bos.form.control.Control;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Label;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.ItemClassEdit;
import kd.bos.form.operate.AbstractOperate;
import kd.bos.form.operate.botp.Push;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.ap.enums.BillStatusEnum;
import kd.fi.ap.formplugin.ApBaseEdit;
import kd.fi.ap.formplugin.formservice.payapply.ApplyPayBillImportHelper;
import kd.fi.ap.helper.BaseDataHelper;
import kd.fi.arapcommon.enums.BillSrcTypeEnum;
import kd.fi.arapcommon.excecontrol.ExecCtrlHelper;
import kd.fi.arapcommon.form.FormServiceHelper;
import kd.fi.arapcommon.helper.*;
import kd.fi.arapcommon.service.BillStatusCtrlService;
import kd.fi.arapcommon.service.log.LogUtil;
import kd.fi.arapcommon.util.DateUtils;
import kd.fi.arapcommon.util.EmptyUtils;
import nckd.yanye.tmc.plugin.operate.AsstactHelperPlugin;
import nckd.yanye.tmc.plugin.operate.AsstactHelperShow;
import org.apache.commons.lang3.ObjectUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Module           :财务云-应付-付款申请-付款申请单
 * Description      :付款申请单-明细分录-选择银行账户信息，筛选出申请公司id喝内部业务单元id一致的账号，添加监听字段，nckd_e_assacct
 *
 *
 *
 * @author guozhiwei
 * @date  2024/8/19 15:05
 * 标识 nckd_ap_payapply_ext
 *
 *
 */


public class ApplyPayBillEdit extends ApBaseEdit {

    private ApplyPayBillImportHelper importHelper;
    private BillStatusCtrlService billStatusCtrlService = new BillStatusCtrlService();
    private boolean isCopyEntryRow = false;
    private Map<String, Object> splitRowMap = new HashMap(16);
    private DynamicObject settlementType;

    private final String BANK_ACCEP = "JSFS06"; // 银行承兑汇票

    private final String TRADE_ACCEP = "JSFS07";//  商业承兑汇票

    public ApplyPayBillEdit() {
    }

    private ApplyPayBillImportHelper getImportHelper() {
        if (this.importHelper == null) {
            this.importHelper = new ApplyPayBillImportHelper(this.getModel(), this.getPageCache());
        }

        return this.importHelper;
    }

    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"nckd_e_assacct"});
        this.filterMaterialVersion();
    }

    public void initImportData(InitImportDataEventArgs e) {
        super.initImportData(e);
        this.getImportHelper().initImportData(e);
    }

    public void beforeImportData(BeforeImportDataEventArgs e) {
        super.beforeImportData(e);
        this.getImportHelper().beforeImportData(e);
    }

    public void afterImportData(ImportDataEventArgs e) {
        this.getImportHelper().afterImportData(e);
    }

    private void setCalculatorAmt(IBillModel m) {
        BigDecimal exchangeRate = (BigDecimal)m.getValue("exchangerate");
        String quotation = (String)m.getValue("quotation");
        if (exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            quotation = "0";
        }

        DynamicObject destCurrency = (DynamicObject)m.getValue("settlecurrency");
        int localprecision = destCurrency.getInt("amtprecision");
        DynamicObjectCollection entry = m.getEntryEntity("entry");

        for(int i = 0; i < entry.size(); ++i) {
            BigDecimal e_applyAmount = (BigDecimal)m.getValue("e_applyamount", i);
            BigDecimal e_approvedAmt = (BigDecimal)m.getValue("e_approvedamt", i);
            if (e_approvedAmt.compareTo(BigDecimal.ZERO) == 0) {
                e_approvedAmt = e_applyAmount;
                m.setValue("e_approvedamt", e_applyAmount, i);
            }

            BigDecimal localApplyAmount;
            BigDecimal LocalApprovedAmt;
            if ("1".equals(quotation)) {
                localApplyAmount = e_applyAmount.divide(exchangeRate, localprecision, RoundingMode.HALF_UP);
                LocalApprovedAmt = e_approvedAmt.divide(exchangeRate, localprecision, RoundingMode.HALF_UP);
            } else {
                localApplyAmount = e_applyAmount.multiply(exchangeRate).setScale(localprecision, RoundingMode.HALF_UP);
                LocalApprovedAmt = e_approvedAmt.multiply(exchangeRate).setScale(localprecision, RoundingMode.HALF_UP);
            }

            m.setValue("e_appseleamount", localApplyAmount, i);
            m.setValue("e_approvedseleamt", LocalApprovedAmt, i);
        }

    }

    private void setHeadAmt(IBillModel m) {
        DynamicObjectCollection entry = m.getEntryEntity("entry");
        BigDecimal applyamount = BigDecimal.ZERO;
        BigDecimal appseleamount = BigDecimal.ZERO;
        BigDecimal approvalamount = BigDecimal.ZERO;
        BigDecimal aprseleamount = BigDecimal.ZERO;

        for(int i = 0; i < entry.size(); ++i) {
            applyamount = applyamount.add((BigDecimal)this.getModel().getValue("e_applyamount", i));
            appseleamount = appseleamount.add((BigDecimal)this.getModel().getValue("e_appseleamount", i));
            approvalamount = approvalamount.add((BigDecimal)this.getModel().getValue("e_approvedamt", i));
            aprseleamount = aprseleamount.add((BigDecimal)this.getModel().getValue("e_approvedseleamt", i));
            Object settlementtype = this.getModel().getValue("e_settlementtype", i);
            if (ObjectUtils.isEmpty(settlementtype) && !m.isFromImport()) {
                this.getModel().setValue("e_settlementtype", BaseDataHelper.getDefaultSettleType(), i);
            }
        }

        this.getModel().setValue("applyamount", applyamount);
        this.getModel().setValue("appseleamount", appseleamount);
        this.getModel().setValue("approvalamount", approvalamount);
        this.getModel().setValue("aprseleamount", aprseleamount);
    }

    public void confirmCallBack(MessageBoxClosedEvent e) {
        super.confirmCallBack(e);
        String callBackID = e.getCallBackId();
        boolean isOK = Objects.equals(MessageBoxResult.Yes, e.getResult());
        if (Objects.equals(callBackID, "opcheck") && isOK) {
            String operateKey = e.getCustomVaule();
            this.getPageCache().put("ignoreCheck", "true");
            this.getView().invokeOperation(operateKey);
            Object billNo = this.getModel().getValue("billno");
            LogUtil.addOpLog("ap_payapply", billNo, operateKey, "billnos：" + billNo + "execute non-standard operations：" + operateKey, true);
        } else if (Objects.equals(callBackID, "executeClose") && isOK) {
            OperateOption option = OperateOption.create();
            String customValue = e.getCustomVaule();
            if (!ObjectUtils.isEmpty(customValue)) {
                option.setVariableValue("selectrows", customValue);
            }

            this.getView().invokeOperation("closepay", option);
        }

    }

    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        String ignoreCheck = this.getPageCache().get("ignoreCheck");
        if (Objects.equals("true", ignoreCheck)) {
            this.getPageCache().put("ignoreCheck", (String)null);
        } else {
            ArApOperateCheckHelper.operateCheck(this.getView(), args, "SZJK-PRE-0057");
        }

        AbstractOperate op = (AbstractOperate)args.getSource();
        if (op instanceof Push || op instanceof PushAndSave) {
            Long pkValue = this.getModel().getDataEntity().getLong("id");
            Set<Long> billIds = new HashSet(1);
            billIds.add(pkValue);
            ExecCtrlHelper.execCustomizeCtrlService("SZJK-PRE-0025", (Object)null, new Object[]{billIds});
        }

        String key = op.getOperateKey();
        if ("submit".equals(key) || "audit".equals(key)) {
            this.checkPayhold(key, args);
        }

        if ("push".equals(key)) {
            this.checkPayhold(args);
        }

        EntryGrid grid;
        if ("copyentryrow".equals(key)) {
            grid = (EntryGrid)this.getControl("entry");
            int length = grid.getSelectRows().length;
            if (length != 1) {
                this.getView().showTipNotification(ResManager.loadKDString("请选择单行分录复制。", "ApplyPayBillEdit_0", "fi-ap-formplugin", new Object[0]));
                args.setCancel(true);
                return;
            }

            this.isCopyEntryRow = true;
        } else if ("splitentryrow".equals(key)) {
            grid = (EntryGrid)this.getControl("entry");
            int[] selectRows = grid.getSelectRows();
            if (selectRows.length != 1) {
                this.getView().showTipNotification(ResManager.loadKDString("请选择单行分录拆分。", "ApplyPayBillEdit_1", "fi-ap-formplugin", new Object[0]));
                args.setCancel(true);
                return;
            }

            this.isCopyEntryRow = true;
            this.splitRowMap.put("e_corebilltype", this.getModel().getValue("e_corebilltype", selectRows[0]));
            this.splitRowMap.put("e_corebillno", this.getModel().getValue("e_corebillno", selectRows[0]));
            this.splitRowMap.put("e_corebillentryseq", this.getModel().getValue("e_corebillentryseq", selectRows[0]));
            this.splitRowMap.put("e_corebillid", this.getModel().getValue("e_corebillid", selectRows[0]));
            this.splitRowMap.put("e_corebillentryid", this.getModel().getValue("e_corebillentryid", selectRows[0]));
            this.splitRowMap.put("e_conbillentity", this.getModel().getValue("e_conbillentity", selectRows[0]));
            this.splitRowMap.put("e_conbillnumber", this.getModel().getValue("e_conbillnumber", selectRows[0]));
            this.splitRowMap.put("e_conbillrownum", this.getModel().getValue("e_conbillrownum", selectRows[0]));
            this.splitRowMap.put("e_conbillid", this.getModel().getValue("e_conbillid", selectRows[0]));
            this.splitRowMap.put("e_conbillentryid", this.getModel().getValue("e_conbillentryid", selectRows[0]));
        } else if ("closepay".equals(key)) {
            this.getView().invokeOperation("refresh");
        }

    }

    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
    }

    public void afterDoOperation(AfterDoOperationEventArgs args) {
        super.afterDoOperation(args);

    }

    private void fillToolBar() {
        this.getView().setVisible(Boolean.FALSE, new String[]{"bar_new", "bar_del", "bar_save", "bar_submit", "bar_audit", "bar_submitandnew", "baritemap1", "bar_businesspro", "bar_more", "barmore"});
        this.getView().setEnable(Boolean.TRUE, new String[]{"bar_new", "bar_del", "bar_save", "bar_submit", "bar_audit", "bar_submitandnew", "baritemap1", "bar_businesspro", "bar_more", "barmore", "bar_unaudit"});
        String billStatus = this.getModel().getDataEntity().getString("billstatus");
        if (billStatus.equals(BillStatusEnum.SAVE.getValue())) {
            this.getView().setVisible(Boolean.TRUE, new String[]{"bar_new", "bar_del", "bar_save", "bar_submit", "bar_submitandnew", "bar_more", "barmore"});
            this.getView().setVisible(Boolean.FALSE, new String[]{"bar_apgeneratevoucher"});
        } else if (billStatus.equals(BillStatusEnum.SUBMIT.getValue())) {
            this.getView().setVisible(Boolean.TRUE, new String[]{"bar_new", "bar_submit", "bar_audit", "bar_more", "barmore"});
            this.getView().setEnable(Boolean.FALSE, new String[]{"bar_submit"});
            this.getView().setVisible(Boolean.FALSE, new String[]{"bar_apgeneratevoucher"});
        } else if (!billStatus.equals(BillStatusEnum.AUDIT.getValue()) && !billStatus.equals("E")) {
            if (billStatus.equals("D")) {
                this.getView().setVisible(Boolean.TRUE, new String[]{"bar_new", "bar_audit", "baritemap1", "bar_businesspro", "bar_more", "barmore"});
                this.getView().setEnable(Boolean.FALSE, new String[]{"bar_audit", "bar_unaudit", "baritemap1"});
                this.getView().setVisible(Boolean.FALSE, new String[]{"bar_apgeneratevoucher"});
            }
        } else {
            this.getView().setVisible(Boolean.TRUE, new String[]{"bar_new", "bar_audit", "baritemap1", "bar_businesspro", "bar_more", "barmore", "bar_apgeneratevoucher"});
            this.getView().setEnable(Boolean.FALSE, new String[]{"bar_audit"});
        }

        if (!billStatus.equals("E") && !billStatus.equals("D")) {
            this.getView().setEnable(Boolean.TRUE, new String[]{"bar_assign", "bar_antiassign"});
        } else {
            this.getView().setEnable(Boolean.FALSE, new String[]{"bar_assign", "bar_antiassign"});
        }

    }

    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control)evt.getSource();
        switch (c.getKey().toLowerCase()) {
            case "nckd_e_assacct":
                this.assacctShowF7();
                break;
        }

    }

    private Map<Object, List<DynamicObject>> getRefundWarnMap() {
        Map<Object, List<DynamicObject>> entryFroupMap = new HashMap(8);
        DynamicObjectCollection entrys = this.getModel().getEntryEntity("entry");
        if (entrys != null && entrys.size() > 0) {
            List<Long> asstactIdList = new ArrayList(entrys.size());
            List<Long> bankIdList = new ArrayList(entrys.size());
            List<String> assacctNameList = new ArrayList(entrys.size());
            List<DynamicObject> isPushCasList = new ArrayList(entrys.size());
            Iterator var7 = entrys.iterator();

            Long PayAsstactId;
            while(var7.hasNext()) {
                DynamicObject entry = (DynamicObject)var7.next();
                BigDecimal ePaidAmt = entry.getBigDecimal("e_paidamt");
                BigDecimal eApproveAmt = entry.getBigDecimal("e_approvedamt");
                if (ePaidAmt.abs().compareTo(eApproveAmt.abs()) < 0) {
                    Long asstactId = entry.getLong("e_asstact.id");
                    String assacctName = entry.getString("e_assacct");
                    PayAsstactId = entry.getLong("e_bebank.id");
                    asstactIdList.add(asstactId);
                    bankIdList.add(PayAsstactId);
                    assacctNameList.add(assacctName);
                    isPushCasList.add(entry);
                }
            }

            QFilter filter = (new QFilter("billstatus", "=", "F")).and("bizdate", ">", DateUtils.getLastDay(new Date(), 180)).and("payeebanknum", "in", assacctNameList).and("payeebank", "in", bankIdList).and("payee", "in", asstactIdList);
            DynamicObjectCollection payBills = QueryServiceHelper.query("cas_paybill", "payeebanknum,payeebank,payee", new QFilter[]{filter});
            Map<Object, List<DynamicObject>> payGroupMap = (Map)payBills.stream().collect(Collectors.groupingBy((p) -> {
                return p.getLong("payee") + p.getString("payeebanknum") + p.getLong("payeebank");
            }));
            Iterator var26 = payGroupMap.entrySet().iterator();

            while(var26.hasNext()) {
                Map.Entry<Object, List<DynamicObject>> payEntry = (Map.Entry)var26.next();
                DynamicObject paybill = (DynamicObject)((List)payEntry.getValue()).get(0);
                PayAsstactId = paybill.getLong("payee");
                Long payBankId = paybill.getLong("payeebank");
                String payAssacctName = paybill.getString("payeebanknum");
                Iterator var16 = isPushCasList.iterator();

                while(var16.hasNext()) {
                    DynamicObject entry = (DynamicObject)var16.next();
                    Long asstactId = entry.getLong("e_asstact.id");
                    String assacctName = entry.getString("e_assacct");
                    Long bankId = entry.getLong("e_bebank.id");
                    if (asstactId.equals(PayAsstactId) && bankId.equals(payBankId) && assacctName.equals(payAssacctName)) {
                        String groupKey = asstactId + assacctName + bankId;
                        List<DynamicObject> groupEntrys = (List)entryFroupMap.get(groupKey);
                        if (groupEntrys != null) {
                            ((List)groupEntrys).add(entry);
                        } else {
                            groupEntrys = new ArrayList(1);
                            ((List)groupEntrys).add(entry);
                        }

                        entryFroupMap.put(groupKey, groupEntrys);
                    }
                }
            }
        }

        return entryFroupMap;
    }

    public void closedCallBack(ClosedCallBackEvent e) {
        super.closedCallBack(e);
        String id = e.getActionId();
        Object returnData = e.getReturnData();
        switch (id) {
            case "assaccount":
                this.closeassaccountF7(returnData);
                break;
            case "nckd_assaccount":
                this.closeassaccountF72(returnData);
                break;
            case "coreBill":
                this.closecoreBill(returnData);
                break;
            case "bar_submit":
                if (returnData != null) {
                    this.getView().invokeOperation("submit");
                }
                break;
            case "bar_audit":
                if (returnData != null) {
                    this.getView().invokeOperation("audit");
                }
                break;
            case "bar_submitandnew":
                if (returnData != null) {
                    this.getView().invokeOperation("submitandnew");
                }
        }

    }

    // 新回调
    private void closeassaccountF72(Object returnData) {
        int curentrow = this.getModel().getEntryCurrentRowIndex("entry");
        if(ObjectUtils.isEmpty(returnData)){
            return;
        }
        // 结算方式
        boolean isAccept = false;
        DynamicObject paymode = (DynamicObject) this.getModel().getValue("e_settlementtype", curentrow);
        if(ObjectUtils.isNotEmpty(paymode) && (BANK_ACCEP.equals(paymode.getString("number")) || TRADE_ACCEP.equals(paymode.getString("number")))){
            // 结算方式为承兑汇票
            isAccept = true;
        }
        ListSelectedRow listSelectedRow = ((ListSelectedRowCollection) returnData).get(0);
        // 银行账户号
        String number = listSelectedRow.getNumber();
        // 银行账户的key
        Object primaryKeyValue = listSelectedRow.getPrimaryKeyValue();
        DynamicObject amAccountbank = BusinessDataServiceHelper.loadSingle(primaryKeyValue, "am_accountbank");

        String assacttype = this.getModel().getValue("e_asstacttype", curentrow).toString();
        this.getModel().setValue("e_assacct", number, curentrow);
        this.getModel().setValue("nckd_e_assacct", number, curentrow);
        //  查询银行账户是否有对应票据开户行信息，如果有，则设置到e_bebank
        QFilter qFilter = new QFilter("account.masterid", "=", primaryKeyValue);
        // 合作金融机构
        Object cooperationId = null;
        DynamicObject billbank = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","billbank.id",new QFilter[]{qFilter});
        if(ObjectUtils.isNotEmpty(billbank) && isAccept){
//            this.getModel().setValue("e_bebank", amAccountbank.getLong("bank.id"), curentrow);
            cooperationId = billbank.getLong("bebank.id");
        }else{
            cooperationId = amAccountbank.getLong("bank.id");
        }
        // 查询合作金融机构对应的行名行号信息
        DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(cooperationId, "bd_finorginfo");
        this.getModel().setValue("e_bebank", bdFinorginfo.getDynamicObject("bebank"), curentrow);
    }


    private void closecoreBill(Object returnData) {
        ArApCorebillHelper.closeCoreBillF7(this.getModel(), "entry", returnData);
    }

    private void showCloseConfirm(String selectRows) {
        DynamicObjectCollection invEntry = this.getModel().getEntryEntity("inventry");
        String confirmMessage = ResManager.loadKDString("关闭操作不可逆，是否继续？", "ApplyPayBillEdit_23", "fi-ap-formplugin", new Object[0]);
        if (!invEntry.isEmpty()) {
            DynamicObjectCollection entry = this.getModel().getEntryEntity("entry");
            int payRowNumber = 0;
            int unCloseRowNum = 0;
            Iterator var7 = entry.iterator();

            while(var7.hasNext()) {
                DynamicObject applyRow = (DynamicObject)var7.next();
                if (applyRow.getBigDecimal("lockedamt").compareTo(BigDecimal.ZERO) != 0) {
                    ++payRowNumber;
                }

                if ("A".equals(applyRow.getString("e_closestatus"))) {
                    ++unCloseRowNum;
                }
            }

            if ((long)payRowNumber == 0L) {
                Set<Long> invoiceIds = (Set)invEntry.stream().map((inv) -> {
                    return inv.getLong("invid");
                }).collect(Collectors.toSet());
                DataSet invoiceDatSet = QueryServiceHelper.queryDataSet("query_invoice", "ap_invoice", "isreffin,businesssource,unrelatedamt,pricetaxtotal,isvoucher,ismatched", new QFilter[]{new QFilter("id", "in", invoiceIds)}, "");
                int matchInvoiceNumber = 0;
                Iterator var10 = invoiceDatSet.iterator();

                while(var10.hasNext()) {
                    Row invRow = (Row)var10.next();
                    if ("ap_payapply".equals(invRow.getString("businesssource")) && invRow.getBigDecimal("unrelatedamt").compareTo(invRow.getBigDecimal("pricetaxtotal")) == 0 && !invRow.getBoolean("isvoucher") && !invRow.getBoolean("ismatched")) {
                        ++matchInvoiceNumber;
                    }
                }

                boolean isAllClose = true;
                if (!ObjectUtils.isEmpty(selectRows)) {
                    List<Long> selectRowList = (List)SerializationUtils.fromJsonString(selectRows, List.class);
                    if (selectRowList.size() != unCloseRowNum) {
                        isAllClose = false;
                    }
                }

                if (isAllClose && matchInvoiceNumber > 0) {
                    confirmMessage = ResManager.loadKDString("关闭操作不可逆，且关闭后将释放发票，是否继续？", "ApplyPayBillEdit_24", "fi-ap-formplugin", new Object[0]);
                }
            }
        }

        this.getView().showConfirm(confirmMessage, (String)null, MessageBoxOptions.OKCancel, ConfirmTypes.Default, new ConfirmCallBackListener("executeClose"), (Map)null, selectRows);
    }

    private void closeassaccountF7(Object returnData) {
        int curentrow = this.getModel().getEntryCurrentRowIndex("entry");
        String assacttype = this.getModel().getValue("e_asstacttype", curentrow).toString();
        ListSelectedRowCollection returnColl = (ListSelectedRowCollection)returnData;
        if (!ObjectUtils.isEmpty(returnColl)) {
            ListSelectedRow rowInfo = returnColl.get(0);
            Object entryKey = rowInfo.getEntryPrimaryKeyValue();
            Object pk = rowInfo.getPrimaryKeyValue();
            DynamicObject account;
            if (!"bd_customer".equals(assacttype) && !"bd_supplier".equals(assacttype)) {
                if ("bos_user".equals(assacttype)) {
                    account = BusinessDataServiceHelper.loadSingleFromCache(pk, "er_payeer", "id,payerbank,payeraccount");
                    if (!ObjectUtils.isEmpty(account)) {
                        this.getModel().setValue("e_assacct", account.getString("payeraccount"), curentrow);
                        this.getModel().setValue("nckd_e_assacct", account.getString("payeraccount"), curentrow);
                        this.getModel().setValue("e_bebank", account.getLong("payerbank.id"), curentrow);
                    }
                } else {
                    account = BusinessDataServiceHelper.loadSingleFromCache(pk, "bd_accountbanks", "id,bankaccountnumber,bank.id,bank.bebank");
                    if (!ObjectUtils.isEmpty(account)) {
                        this.getModel().setValue("e_assacct", account.getString("bankaccountnumber"), curentrow);
                        this.getModel().setValue("nckd_e_assacct", account.getString("bankaccountnumber"), curentrow);
                    }

                    if (!ObjectUtils.isEmpty(account) && !ObjectUtils.isEmpty(account.getDynamicObject("bank.bebank"))) {
                        this.getModel().setValue("e_bebank", account.getDynamicObject("bank.bebank").getPkValue(), curentrow);
                    }
                }
            } else {
                account = BusinessDataServiceHelper.loadSingleFromCache(pk, assacttype, "bankaccount,accountname,bank,name,entry_bank");
                DynamicObjectCollection bankEntry = account.getDynamicObjectCollection("entry_bank");
                Iterator var10 = bankEntry.iterator();

                while(var10.hasNext()) {
                    DynamicObject row = (DynamicObject)var10.next();
                    if (row.getPkValue().equals(entryKey)) {
                        this.getModel().setValue("e_assacct", row.getString("bankaccount"), curentrow);
                        this.getModel().setValue("nckd_e_assacct", row.getString("bankaccount"), curentrow);
                        this.getModel().setValue("e_bebank", row.getLong("bank.id"), curentrow);
                        break;
                    }
                }
            }
        }

    }

    private void assacctShowF7() {
        int currrow = this.getModel().getEntryCurrentRowIndex("entry");
        DynamicObject basedata = (DynamicObject)this.getModel().getValue("e_asstact", currrow);
//        AsstactHelper.assacctShowF7(basedata, this.getView(), this.getPluginName());
        AsstactHelperShow.assacctShowF7(basedata, this.getView(), this.getPluginName());
    }

    private void showCoreBillF7() {
        int currrow = this.getModel().getEntryCurrentRowIndex("entry");
        String corebilltype = (String)this.getModel().getValue("e_corebilltype", currrow);
        ArApCorebillHelper.showCoreBillF7(corebilltype, this.getView(), this.getPluginName());
    }

    public void afterBindData(EventObject e) {
        super.afterBindData(e);
    }

    private void setAsstactCaption() {
        DynamicObject billType = (DynamicObject)this.getModel().getValue("billtype");
        if (!ObjectUtils.isEmpty(billType)) {
            String customerCaption = ResManager.loadKDString("收款供应商", "ApplyPayBillEdit_5", "fi-ap-formplugin", new Object[0]);
            String asstactCaption = ResManager.loadKDString("往来户", "ApplyPayBillEdit_6", "fi-ap-formplugin", new Object[0]);
            ItemClassEdit asstact = (ItemClassEdit)this.getControl("e_asstact");
            String asstName = (String)asstact.getProperty().getDisplayName().get("zh_CN");
            if ("往来户".equals(asstName)) {
                if ("ap_payapply_BT_S".equals(billType.getString("number"))) {
                    asstact.setCaption(new LocaleString(customerCaption));
                } else {
                    asstact.setCaption(new LocaleString(asstactCaption));
                }

            }
        }
    }

    public void propertyChanged(PropertyChangedArgs e) {
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        int iRow = changeData[0].getRowIndex();
        String billStatus = (String)this.getModel().getValue("billstatus");
        if (this.billStatusCtrlService.isCanChange(billStatus)) {
            switch (key) {
                case "settleorg":
                    this.settleOrgChanged(newValue);
                    break;
                case "billtype":
                    this.setAsstactCaption();
                    if (newValue == null) {
                        this.getModel().beginInit();
                        this.getModel().setValue("billtype", oldValue);
                        this.getModel().endInit();
                        this.getView().updateView("billtype");
                        return;
                    }
                    break;
                case "applydate":
                    if (ObjectUtils.isEmpty(newValue)) {
                        FormServiceHelper.recoverOldValue(this.getView(), "applydate", oldValue);
                    }
                    break;
                case "exratedate":
                    if (Objects.isNull(newValue)) {
                        this.getModel().setValue("exratedate", new Date());
                        return;
                    }
                    break;
                case "exratetable":
                    if (ObjectUtils.isEmpty(newValue)) {
                        this.getModel().setValue("exratetable", oldValue);
                    }
                    break;
                case "settlecurrency":
                    DynamicObjectCollection invEntries = this.getModel().getEntryEntity("inventry");
                    if (!invEntries.isEmpty() && (ObjectUtils.isEmpty(newValue) || ((DynamicObject)newValue).getLong("id") != ((DynamicObject)invEntries.get(0)).getLong("i_currency.id"))) {
                        this.getView().showTipNotification(ResManager.loadKDString("付款申请单结算币与发票币种不一致，请修改。", "ApplyPayBillEdit4Inv_27", "fi-ap-formplugin", new Object[0]));
                        this.getModel().setValue("settlecurrency", oldValue);
                        break;
                    }
                case "paycurrency":
                    if (oldValue != null && newValue != null) {
                        int oldPrecision = ((DynamicObject)oldValue).getInt("amtprecision");
                        int newPrecision = ((DynamicObject)newValue).getInt("amtprecision");
                        BigDecimal amount = (BigDecimal)this.getModel().getValue("applyamount");
                        if (oldPrecision != newPrecision && amount.compareTo(BigDecimal.ZERO) != 0) {
                            IBillModel model = (IBillModel)this.getModel();
                            this.setCalculatorAmt(model);
                            this.setHeadAmt(model);
                        }
                    }
                    break;
                case "e_asstact":
                    this.asstactChanged(changeData);
                    break;
//                case "nckd_e_assacct":
//                    this.asstactChanged(changeData);
//                    break;
                case "e_applyamount":
                    DynamicObject settleCurrency = (DynamicObject)this.getModel().getValue("settlecurrency");
                    if (ObjectUtils.isEmpty(settleCurrency)) {
                        this.getModel().setValue("e_applyamount", BigDecimal.ZERO, iRow);
                        this.getModel().setValue("e_payamount", BigDecimal.ZERO, iRow);
                        if (((BigDecimal)newValue).compareTo(BigDecimal.ZERO) != 0) {
                            this.getView().showTipNotification(ResManager.loadKDString("请先录入结算币。", "ApplyPayBillEdit_3", "fi-ap-formplugin", new Object[0]));
                        }

                        return;
                    }

                    BigDecimal applyamount = (BigDecimal)newValue;
                    this.reserictApplyAmount(applyamount, iRow);
                    Map<String, String> map = new HashMap();
                    map.put("e_applyamount", "applyamount");
                    map.put("e_appseleamount", "appseleamount");
                    this.applyamountChanged(map);
                    this.getModel().setValue("e_approvedamt", applyamount, iRow);
                    this.setCalculatorAmt((IBillModel)this.getModel());
                    this.setHeadAmt((IBillModel)this.getModel());
                    break;
                case "e_approvedamt":
                    BigDecimal approvedAmt = (BigDecimal)this.getModel().getValue("e_approvedamt", iRow);
                    BigDecimal exchangeRate = (BigDecimal)this.getModel().getValue("exchangerate");
                    DynamicObject settleCurrency_1 = (DynamicObject)this.getModel().getValue("settlecurrency");
                    String quotation = (String)this.getModel().getValue("quotation");
                    if ("1".equals(quotation) && exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
                        quotation = "0";
                    }

                    if (ObjectUtils.isEmpty(settleCurrency_1)) {
                        this.getModel().setValue("e_applyamount", BigDecimal.ZERO, iRow);
                        this.getModel().setValue("e_payamount", BigDecimal.ZERO, iRow);
                        this.getView().showTipNotification(ResManager.loadKDString("请先录入结算币。", "ApplyPayBillEdit_3", "fi-ap-formplugin", new Object[0]));
                        return;
                    }

                    int amtPrecision = settleCurrency_1.getInt("amtprecision");
                    if ("1".equals(quotation)) {
                        this.getModel().setValue("e_approvedseleamt", approvedAmt.divide(exchangeRate, amtPrecision, RoundingMode.HALF_UP), iRow);
                    } else {
                        this.getModel().setValue("e_approvedseleamt", approvedAmt.multiply(exchangeRate).setScale(amtPrecision, RoundingMode.HALF_UP), iRow);
                    }

                    Map<String, String> entrymap = new HashMap();
                    entrymap.put("e_approvedamt", "approvalamount");
                    entrymap.put("e_approvedseleamt", "aprseleamount");
                    this.applyamountChanged(entrymap);
                    break;
                case "exchangerate":
                    if (EmptyUtils.isEmpty(newValue)) {
                        this.getView().showErrorNotification(ResManager.loadKDString("请补充该结算币的“汇率”。", "ApplyPayBillEdit_4", "fi-ar-formplugin", new Object[0]));
                        return;
                    }

                    this.setCalculatorAmt((IBillModel)this.getModel());
                    this.setHeadAmt((IBillModel)this.getModel());
                    break;
                case "e_payamount":
                    this.getModel().setValue("e_applyamount", newValue, iRow);
                    break;
                case "e_material":
                    this.getModel().setValue("e_materialversion", (Object)null, iRow);
                    this.materialChanged(changeData);
                    break;
                case "e_corebilltype":
                    this.getModel().setValue("e_corebillno", (Object)null, iRow);
                    this.getModel().setValue("e_corebillentryseq", (Object)null, iRow);
            }

        }
    }

    private void settleOrgChanged(Object newValue) {
        if (newValue == null) {
            this.getModel().setValue("settlecurrency", (Object)null);
        } else {
            InitHelper initHelper = new InitHelper(((DynamicObject)newValue).getLong("id"), "ap_init");
            DynamicObject currency = initHelper.getStandardCurrency();
            if (ObjectUtils.isEmpty(currency)) {
                this.getModel().setValue("settlecurrency", (Object)null);
            } else {
                this.getModel().setValue("settlecurrency", currency.getPkValue());
            }

            DynamicObject exrateTable = initHelper.getExrateTable();
            if (!ObjectUtils.isEmpty(exrateTable)) {
                this.getModel().setValue("exratetable", exrateTable.getPkValue());
            }
        }

    }

    public void afterDeleteRow(AfterDeleteRowEventArgs e) {
        Map<String, String> map = new HashMap();
        map.put("e_applyamount", "applyamount");
        map.put("e_approvedamt", "approvalamount");
        map.put("e_approvedseleamt", "aprseleamount");
        map.put("e_appseleamount", "appseleamount");
        this.applyamountChanged(map);
    }

    public void afterAddRow(AfterAddRowEventArgs e) {
        EntryProp entryProp = e.getEntryProp();
        Map<String, String> map = new HashMap();
        map.put("e_applyamount", "applyamount");
        map.put("e_approvedamt", "approvalamount");
        map.put("e_approvedseleamt", "aprseleamount");
        map.put("e_appseleamount", "appseleamount");
        this.applyamountChanged(map);
        if ("entry".equals(entryProp.getName())) {
            RowDataEntity[] rowDataEntities = e.getRowDataEntities();
            RowDataEntity[] var5 = rowDataEntities;
            int var6 = rowDataEntities.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                RowDataEntity row = var5[var7];
                if (!this.isCopyEntryRow) {
                    if (this.settlementType == null) {
                        this.settlementType = BaseDataHelper.getDefaultSettleType();
                    }

                    this.getModel().setValue("e_settlementtype", this.settlementType, row.getRowIndex());
                } else {
                    BigDecimal applyAmount = (BigDecimal)this.getModel().getValue("e_applyamount", row.getRowIndex());
                    this.getModel().setValue("e_approvedamt", applyAmount, row.getRowIndex());
                }

                if (!this.splitRowMap.isEmpty()) {
                    this.getModel().setValue("e_corebilltype", this.splitRowMap.get("e_corebilltype"), row.getRowIndex());
                    this.getModel().setValue("e_corebillno", this.splitRowMap.get("e_corebillno"), row.getRowIndex());
                    this.getModel().setValue("e_corebillentryseq", this.splitRowMap.get("e_corebillentryseq"), row.getRowIndex());
                    this.getModel().setValue("e_corebillid", this.splitRowMap.get("e_corebillid"), row.getRowIndex());
                    this.getModel().setValue("e_corebillentryid", this.splitRowMap.get("e_corebillentryid"), row.getRowIndex());
                    this.getModel().setValue("e_conbillentity", this.splitRowMap.get("e_conbillentity"), row.getRowIndex());
                    this.getModel().setValue("e_conbillnumber", this.splitRowMap.get("e_conbillnumber"), row.getRowIndex());
                    this.getModel().setValue("e_conbillrownum", this.splitRowMap.get("e_conbillrownum"), row.getRowIndex());
                    this.getModel().setValue("e_conbillid", this.splitRowMap.get("e_conbillid"), row.getRowIndex());
                    this.getModel().setValue("e_conbillentryid", this.splitRowMap.get("e_conbillentryid"), row.getRowIndex());
                    this.splitRowMap.clear();
                }
            }
        }

    }

    private void applyamountChanged(Map<String, String> map) {
        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry)iterator.next();
            int count = this.getModel().getEntryRowCount("entry");
            BigDecimal amount = BigDecimal.ZERO;

            for(int i = 0; i < count; ++i) {
                amount = amount.add((BigDecimal)this.getModel().getValue((String)entry.getKey(), i));
            }

            this.getModel().setValue((String)entry.getValue(), amount);
        }

    }

    private void asstactChanged(ChangeData[] data) {
        int row = data[0].getRowIndex();
        Object propValue = data[0].getNewValue();
        if (!ObjectUtils.isEmpty(propValue)) {
            DynamicObject dydata = (DynamicObject)propValue;
            Map<Object, Object> map = AsstactHelperPlugin.getaccbebankMap(dydata);
            this.getModel().setValue("nckd_e_assacct", map.get("account"), row);
            this.getModel().setValue("e_assacct", map.get("account"), row);
            this.getModel().setValue("e_bebank", map.get("bebank"), row);
            if (map.get("settlementtypeid") != null) {
                this.getModel().setValue("e_settlementtype", map.get("settlementtypeid"), row);
            } else {
                long settleTypeId = ArApSettleTypeHelper.getDefaultSettleType();
                this.getModel().setValue("e_settlementtype", settleTypeId, row);
            }
        } else {
            this.getModel().setValue("e_assacct", (Object)null, row);
            this.getModel().setValue("nckd_e_assacct", (Object)null, row);
            this.getModel().setValue("e_bebank", (Object)null, row);
            this.getModel().setValue("e_settlementtype", ArApSettleTypeHelper.getDefaultSettleType(), row);
        }
        this.getView().updateView();

    }

    public void afterCopyData(EventObject e) {
        super.afterCopyData(e);
        IBillModel model = (IBillModel)this.getModel();
        int count = model.getEntryRowCount("entry");

        for(int i = 0; i < count; ++i) {
            model.setValue("e_approvedamt", model.getValue("e_applyamount", i), i);
            model.setValue("e_approvedseleamt", model.getValue("e_appseleamount", i), i);
        }

        model.setValue("approvalamount", model.getValue("applyamount"));
        model.setValue("aprseleamount", model.getValue("appseleamount"));
        this.getModel().setValue("billsrctype", BillSrcTypeEnum.MANUAL.getValue());
    }

    private void setCurrencyByApplyOrg() {
        DynamicObject org = (DynamicObject)this.getModel().getValue("settleorg");
        if (!ObjectUtils.isEmpty(org)) {
            this.init = new InitHelper(org.getLong("id"), "ap_init");
            DynamicObject currency = this.init.getStandardCurrency();
            this.getModel().setValue("paycurrency", currency);
            this.getModel().setValue("settlecurrency", currency);
            DynamicObject exrateTable = this.init.getExrateTable();
            if (!ObjectUtils.isEmpty(exrateTable)) {
                this.getModel().setValue("exratetable", exrateTable.getPkValue());
                this.getModel().setValue("exchangerate", 1);
            }
        }

    }

    private void reserictApplyAmount(BigDecimal eApplyAmount, int iRow) {
        BigDecimal ePayAmount = (BigDecimal)this.getModel().getValue("e_payamount", iRow);
        if (ePayAmount.compareTo(BigDecimal.ZERO) > 0 && ePayAmount.compareTo(eApplyAmount) < 0) {
            this.getModel().setValue("e_applyamount", ePayAmount, iRow);
        }

    }

    public void afterLoadData(EventObject e) {
        super.afterLoadData(e);
        this.fillToolBar();
        this.getView().setVisible(Boolean.FALSE, new String[]{"sameinfoflex"});
        Object billStatus = this.getModel().getValue("billstatus");
        if ("B".equals(billStatus) || "A".equals(billStatus)) {
            Map<String, List<String>> sameBillInfo = (Map)ExecCtrlHelper.execCustomizeCtrlService("SZJK-PRE-0024", new HashMap(2), new Object[]{this.getModel().getDataEntity(true)});
            this.sameBillWarn(sameBillInfo);
        }

    }

    private void setOrgByUser(DynamicObject org) {
        long orgId = org.getLong("id");
        if (EmptyUtils.isEmpty(this.getModel().getValue("settleorg")) && org.getBoolean("fisaccounting")) {
            this.getModel().setValue("settleorg", orgId);
        }

        if (EmptyUtils.isEmpty(this.getModel().getValue("purorg")) && org.getBoolean("fispurchase")) {
            this.getModel().setValue("purorg", orgId);
        }

        if (EmptyUtils.isEmpty(this.getModel().getValue("payorg")) && org.getBoolean("fisbankroll")) {
            this.getModel().setValue("payorg", orgId);
        }

    }

    private void materialChanged(ChangeData[] data) {
        int row = data[0].getRowIndex();
        Object propValue = data[0].getNewValue();
        if (propValue != null) {
            DynamicObject material = (DynamicObject)propValue;
            this.getModel().setValue("e_spectype", material.getString("modelnum"), row);
        } else {
            this.getModel().setValue("e_spectype", (Object)null, row);
        }

    }

    private void filterMaterialVersion() {
        BasedataEdit materialVersionF7 = (BasedataEdit)this.getControl("e_materialversion");
        materialVersionF7.addBeforeF7SelectListener((beforeF7SelectEvent) -> {
            int currentRow = this.getModel().getEntryCurrentRowIndex("entry");
            DynamicObject material = (DynamicObject)this.getModel().getValue("e_material", currentRow);
            if (EmptyUtils.isNotEmpty(material)) {
                long materialId = material.getLong("id");
                QFilter filter = new QFilter("material", "=", materialId);
                ListShowParameter showParameter = (ListShowParameter)beforeF7SelectEvent.getFormShowParameter();
                ListFilterParameter listFilterParameter = showParameter.getListFilterParameter();
                listFilterParameter.setFilter(filter);
            }

        });
    }

    private void sameBillWarn(Map<String, List<String>> sameBillInfo) {
        if (sameBillInfo != null && !sameBillInfo.isEmpty()) {
            this.getView().setVisible(Boolean.TRUE, new String[]{"sameinfoflex"});
            List<List<String>> sameBillNos = new ArrayList(sameBillInfo.values());
            Label label = (Label)this.getView().getControl("sameinfo_labelap");
            String message = ResManager.loadKDString("重复付款风险提醒：", "FinApBillEdit_31", "fi-ap-formplugin", new Object[0]);
            label.setText(message.concat(ExecCtrlHelper.getSameBillMessage((List)sameBillNos.get(0), true)));
        }
    }

    private void checkPayhold(String key, BeforeDoOperationEventArgs args) {
        String operateKey = ResManager.loadKDString("提交", "ApplyPayBillEdit_7", "fi-ap-formplugin", new Object[0]);
        String errorTitle = ResManager.loadKDString("提交失败", "ApplyPayBillEdit_9", "fi-ap-formplugin", new Object[0]);
        String errorMessageTemplate = ResManager.loadKDString("单据编号%1$s：第%2$d行，收款供应商“%3$s”已付款冻结，不允许%4$s。", "ApplyPayBillEdit_11", "fi-ap-formplugin", new Object[0]);
        if ("audit".equals(key)) {
            operateKey = ResManager.loadKDString("审核", "ApplyPayBillEdit_8", "fi-ap-formplugin", new Object[0]);
            errorTitle = ResManager.loadKDString("审核失败", "ApplyPayBillEdit_10", "fi-ap-formplugin", new Object[0]);
        }

        DynamicObjectCollection entries = this.getModel().getEntryEntity("entry");
        List<Long> asstactList = new ArrayList(8);

        for(int i = 0; i < entries.size(); ++i) {
            DynamicObject entry = (DynamicObject)entries.get(i);
            if ("bd_supplier".equals(entry.getString("e_asstacttype"))) {
                DynamicObject asstact = entry.getDynamicObject("e_asstact");
                if (!ObjectUtils.isEmpty(asstact)) {
                    Long id = asstact.getLong("id");
                    asstactList.add(id);
                }
            }
        }

        Map<Object, DynamicObject> asstactRes = BusinessDataServiceHelper.loadFromCache("bd_supplier", "id", new QFilter[]{new QFilter("id", "in", asstactList), new QFilter("payhold", "=", Boolean.TRUE)});
        if (!ObjectUtils.isEmpty(asstactRes)) {
            List<String> errorMsgs = new ArrayList(8);

            for(int i = 0; i < entries.size(); ++i) {
                DynamicObject entry = (DynamicObject)entries.get(i);
                if ("bd_supplier".equals(entry.getString("e_asstacttype"))) {
                    DynamicObject asstact = entry.getDynamicObject("e_asstact");
                    if (!ObjectUtils.isEmpty(asstact)) {
                        Long id = asstact.getLong("id");
                        if (asstactRes.containsKey(id)) {
                            String billno = (String)this.getModel().getValue("billno");
                            String asstactName = asstact.getString("name");
                            String message = String.format(errorMessageTemplate, billno, i + 1, asstactName, operateKey);
                            errorMsgs.add(message);
                        }
                    }
                }
            }

            if (errorMsgs.size() > 1) {
                Map<String, Object> customParam = new HashMap(2);
                customParam.put("title", errorTitle);
                customParam.put("errorMsg", errorMsgs);
                FormShowParameter parameter = new FormShowParameter();
                parameter.setFormId("bos_operationresult");
                parameter.getOpenStyle().setShowType(ShowType.Modal);
                parameter.getCustomParams().putAll(customParam);
                parameter.setShowTitle(false);
                this.getView().showForm(parameter);
            } else {
                this.getView().showErrorNotification((String)errorMsgs.get(0));
            }

            args.setCancel(true);
        }

    }

    private void checkPayhold(BeforeDoOperationEventArgs args) {
        String operateKey = ResManager.loadKDString("付款", "ApplyPayBillEdit_12", "fi-ap-formplugin", new Object[0]);
        String errorTitle = ResManager.loadKDString("下推失败", "ApplyPayBillEdit_13", "fi-ap-formplugin", new Object[0]);
        String errorMessageTemplate = ResManager.loadKDString("单据编号%1$s：第%2$d行，收款供应商“%3$s”已付款冻结，不允许下推%4$s。", "ApplyPayBillEdit_14", "fi-ap-formplugin", new Object[0]);
        DynamicObjectCollection entries = this.getModel().getEntryEntity("entry");
        List<Long> asstactList = new ArrayList(8);

        for(int i = 0; i < entries.size(); ++i) {
            DynamicObject entry = (DynamicObject)entries.get(i);
            if (!"bd_supplier".equals(entry.getString("e_asstacttype"))) {
                return;
            }

            DynamicObject asstact = entry.getDynamicObject("e_asstact");
            if (!ObjectUtils.isEmpty(asstact)) {
                Long id = asstact.getLong("id");
                asstactList.add(id);
            }
        }

        if (!ObjectUtils.isEmpty(asstactList)) {
            Map<Object, DynamicObject> asstactRes = BusinessDataServiceHelper.loadFromCache("bd_supplier", "id", new QFilter[]{new QFilter("id", "in", asstactList), new QFilter("payhold", "=", Boolean.FALSE)});
            if (ObjectUtils.isEmpty(asstactRes)) {
                List<String> errorMsgs = new ArrayList(8);

                for(int i = 0; i < entries.size(); ++i) {
                    DynamicObject entry = (DynamicObject)entries.get(i);
                    DynamicObject asstact = entry.getDynamicObject("e_asstact");
                    if (!ObjectUtils.isEmpty(asstact)) {
                        String billno = (String)this.getModel().getValue("billno");
                        String asstactName = asstact.getString("name");
                        String message = String.format(errorMessageTemplate, billno, i + 1, asstactName, operateKey);
                        errorMsgs.add(message);
                    }
                }

                if (errorMsgs.size() > 1) {
                    Map<String, Object> customParam = new HashMap(2);
                    customParam.put("title", errorTitle);
                    customParam.put("errorMsg", errorMsgs);
                    FormShowParameter parameter = new FormShowParameter();
                    parameter.setFormId("bos_operationresult");
                    parameter.getOpenStyle().setShowType(ShowType.Modal);
                    parameter.getCustomParams().putAll(customParam);
                    parameter.setShowTitle(false);
                    this.getView().showForm(parameter);
                } else {
                    this.getView().showErrorNotification((String)errorMsgs.get(0));
                }

                args.setCancel(true);
            }
        }
    }
}

