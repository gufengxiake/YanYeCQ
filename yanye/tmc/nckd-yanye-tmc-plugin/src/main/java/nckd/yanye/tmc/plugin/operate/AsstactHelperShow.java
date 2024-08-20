package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;

import kd.bos.form.CloseCallBack;
import kd.bos.form.IFormView;
import kd.bos.form.ShowFormHelper;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.arapcommon.helper.BizExtendHelper;
import kd.fi.arapcommon.helper.DynamicListHelper;
import kd.fi.arapcommon.helper.LspWapper;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Module           :财务云-应付-付款申请-付款申请,往来账号选择修改
 * Description      :付款申请单-明细分录-选择银行账户信息 nckd_e_assacct
 *
 * @author guozhiwei
 * @date  2024/8/19 16:11
 * 标识 nckd_ap_payapply_ext
 *
 */


public class AsstactHelperShow {

    public AsstactHelperShow() {
    }

    public static void assacctShowF7(DynamicObject basedata, IFormView view, String pluginName) {
        if (ObjectUtils.isEmpty(basedata)) {
            view.showTipNotification(ResManager.loadKDString("请先选择往来户。", "AsstactHelper_0", "fi-arapcommon", new Object[0]));
        } else {
            String asstactType = basedata.getDataEntityType().getName();
            ListShowParameter lsp;
            ListFilterParameter lspfileter;
            CloseCallBack closeCallBack;
            if ("bos_user".equals(asstactType)) {
                lsp = ShowFormHelper.createShowListForm("er_payeer", false, 2);
                lspfileter = new ListFilterParameter();
                lspfileter.setFilter(new QFilter("payer", "=", basedata.getPkValue()));
                lspfileter.setFilter(new QFilter("enable", "=", Boolean.TRUE));
                lsp.setListFilterParameter(lspfileter);
                closeCallBack = new CloseCallBack(pluginName, "assaccount");
                lsp.setCloseCallBack(closeCallBack);
                view.showForm(lsp);
            } else {
                if (!"bos_org".equals(asstactType)) {
                    DynamicObjectCollection coll = DynamicListHelper.getBankCollInfo(basedata.getPkValue(), asstactType);
                    if (coll == null || coll.isEmpty()) {
                        view.showTipNotification(ResManager.loadKDString("请维护对应客商的银行信息。", "AsstactHelper_1", "fi-arapcommon", new Object[0]));
                        return;
                    }

                    lsp = null;
                    lspfileter  = null;
                    if ("bd_supplier".equals(asstactType)) {
                        boolean flag = true;
                        // 查询是否存在内部公司
                        DynamicObject o = (DynamicObject) BusinessDataServiceHelper.loadSingle(basedata.getPkValue(), "bd_supplier").get("internal_company");
                        if (ObjectUtils.isNotEmpty(o)) {
                            // 获取票据账号开户行维护信息
                            QFilter qFilter = new QFilter("company.masterid", "=", o.getPkValue());
                            DynamicObject[] load = BusinessDataServiceHelper.load("am_accountmaintenance", "bank",new QFilter[]{qFilter},null );
                            if (ObjectUtils.isNotEmpty(load)) {
                                // 存在票据账号开户行维护信息
                                lsp= getSupplierBankInfoShowParameter(o);
                                flag = false;
                            }
//                            lsp = DynamicListHelper.getSupplierBankInfoShowParameter(o.getPkValue());
                        }
                        if(flag){
                           lsp = DynamicListHelper.getSupplierBankInfoShowParameter(basedata.getPkValue());
                        }
                    } else {
                        lsp = DynamicListHelper.getCustomerBankInfoShowParameter(basedata.getPkValue());
                    }

                    closeCallBack = new CloseCallBack(pluginName, "assaccount");
                    lsp.setCloseCallBack(closeCallBack);
                    BizExtendHelper.payeeBankInfoFilter(lsp.getListFilterParameter(), view);
                } else {
                    lsp = ShowFormHelper.createShowListForm("bd_accountbanks", false, 2);
                    lspfileter = new ListFilterParameter();
                    List<QFilter> filterList = new ArrayList(2);
                    filterList.add(new QFilter("company", "=", basedata.getPkValue()));
                    filterList.add(new QFilter("acctstatus", "=", "normal"));
                    lspfileter.setQFilters(filterList);
                    lsp.setListFilterParameter(lspfileter);
                    closeCallBack = new CloseCallBack(pluginName, "assaccount");
                    lsp.setCloseCallBack(closeCallBack);
                }
                view.showForm(lsp);
            }

        }
    }

    public static ListShowParameter getSupplierBankInfoShowParameter(DynamicObject pk) {
        List<String> showFields = new ArrayList();
        showFields.add("bank.name");
        showFields.add("account.number");
        showFields.add("bankname");
        showFields.add("currency.name");
        showFields.add("isdefault_bank");
        ListShowParameter lsp = createDynamicListShowParameter("am_accountmaintenance", null, showFields);
        ListFilterParameter lfp = new ListFilterParameter();
        lfp.setFilter(new QFilter("company.masterid", "=", pk.getPkValue()));
        lsp.setListFilterParameter(lfp);
        lsp.setCaption(ResManager.loadKDString("供应商-银行信息", "DynamicListHelper_0", "fi-arapcommon", new Object[0]));
        return lsp;
    }

    public static ListShowParameter createDynamicListShowParameter(String entity, String entry, List<String> showFields) {
        ListShowParameter lsp = ShowFormHelper.createShowListForm(entity, true, 2);
        lsp.setCustomParam("entity", entity);
        lsp.setCustomParam("entry", entry);
        lsp.setCustomParam("isEntryMain", Boolean.TRUE);
        lsp.setCustomParam("showFields", showFields);
        LspWapper lspWapper = new LspWapper(lsp);
        lspWapper.clearPlugins();
        lspWapper.registerScript("kingdee.fi.ap.mainpage.arapdynamiclistscriptplugin");
        lspWapper.setMergeRow(false);
        lsp.setAppId("ap");
        return lsp;
    }

}
