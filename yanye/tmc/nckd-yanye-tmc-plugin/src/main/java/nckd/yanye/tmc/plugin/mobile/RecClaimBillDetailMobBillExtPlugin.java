package nckd.yanye.tmc.plugin.mobile;

import kd.bos.bill.AbstractMobBillPlugIn;
import kd.bos.bill.MobileBillShowParameter;
import kd.bos.bill.MobileFormPosition;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormConfig;
import kd.bos.form.FormMetadataCache;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.MobileListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.fi.cas.formplugin.mobile.recclaim.utils.CasToolKit;

import java.util.EventObject;
import java.util.Iterator;

/**
 * Module           :财务云-出纳-收款认领
 * Description      :1.认领处理单，增加核心单据可选要货订单
 *
 * @author : zhujintao
 * @date : 2024/9/9
 */
public class RecClaimBillDetailMobBillExtPlugin extends AbstractMobBillPlugIn {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"corebillnobtn", "corebillentrybtn"});
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control control = (Control) evt.getSource();
        String key = control.getKey();
        if ("corebillnobtn".equals(key)) {
            this.showCoreBillF7();
        } else {
            if ("corebillentrybtn".equals(key)) {
                this.showCoreBillEntry();
            }
        }
    }

    private void showCoreBillF7() {
        String coreBillType = (String) this.getModel().getValue("e_corebilltype");
        if ("ocbsoc_saleorder".equals(coreBillType)) {
            Object settleorg = this.getModel().getValue("e_settleorg");
            ListFilterParameter lfp = new ListFilterParameter();
            lfp.setFilter(new QFilter("saleorgid", QCP.equals, settleorg != null ? ((DynamicObject) settleorg).getPkValue() : null));
            //MobileListShowParameter lsp = new MobileListShowParameter();
            MobileListShowParameter lsp = createShowMobileF7ListForm("nckd_ocbsoc_saleorderbase", false);//第二个参数为是否支持多选;
            CloseCallBack closeCallBack = new CloseCallBack(this, "e_corebillno_ext");
            //lsp.setFormId("bos_moblisttabf7");
            //lsp.setBillFormId("nckd_ocbsoc_saleorderbase");
            //lsp.setCustomParam("ismergerows", Boolean.FALSE);
            //lsp.setCaption(ResManager.loadKDString("核心单据编号", "RecClaimBillDetailMobBillPlugin_3", "fi-cas-mobile", new Object[0]));
            //lsp.getOpenStyle().setShowType(ShowType.Floating);
            //lsp.setLookUp(true);
            //lsp.setListFilterParameter(lfp);
            lsp.setCloseCallBack(closeCallBack);
            this.getView().showForm(lsp);
        }
    }

    /*
    创建移动列表
     */
    private static MobileListShowParameter createShowMobileF7ListForm(String formId, boolean isMultiSelect) {
        MobileListShowParameter para = new MobileListShowParameter();
        FormConfig formConfig = FormMetadataCache.getMobListFormConfig(formId);
        para.setCaption(formConfig.getCaption().toString());
        para.setLookUp(true);
        para.setBillFormId(formId);
        ShowType showType;
        if (formConfig.getShowType() == ShowType.MainNewTabPage) {
            showType = ShowType.Floating;
        } else {
            showType = ShowType.Modal;
            para.setPosition(MobileFormPosition.Bottom);
        }
        para.getOpenStyle().setShowType(showType);
        para.setMultiSelect(isMultiSelect);

        String f7ListFormId = formConfig.getF7ListFormId();
        if (StringUtils.isNotBlank(f7ListFormId)) {
            para.setFormId(f7ListFormId);
        }

        return para;
    }

    private void showCoreBillEntry() {
        String coreBillType = (String) this.getModel().getValue("e_corebilltype");
        if ("ocbsoc_saleorder".equals(coreBillType)) {
            String coreBillId = (String) this.getModel().getValue("e_corebillid");
            if (coreBillId != null && !StringUtils.isEmpty(coreBillId)) {
                Object settleOrgId = ((DynamicObject) this.getModel().getValue("e_settleorg")).getPkValue();
                if (settleOrgId == null) {
                    this.getView().showTipNotification(ResManager.loadKDString("请先指定结算组织。", "RecClaimBillDetailMobBillPlugin_1", "fi-cas-mobile", new Object[0]));
                } else {
                    MobileBillShowParameter bsp = new MobileBillShowParameter();
                    CloseCallBack closeCallBack = new CloseCallBack(this, "e_corebillentry");
                    bsp.setFormId("ocbsoc_saleorder");
                    bsp.setPkId(coreBillId);
                    bsp.setCaption(ResManager.loadKDString("核心单据行号", "RecClaimBillDetailMobBillPlugin_6", "fi-cas-mobile", new Object[0]));
                    bsp.setStatus(OperationStatus.VIEW);
                    bsp.setCustomParam("org", settleOrgId);
                    bsp.getOpenStyle().setShowType(ShowType.Floating);
                    bsp.setCloseCallBack(closeCallBack);
                    this.getView().showForm(bsp);
                }
            } else {
                this.getView().showTipNotification(ResManager.loadKDString("请先指定核心单据编号。", "RecClaimBillDetailMobBillPlugin_5", "fi-cas-mobile", new Object[0]));
            }

        }

    }

    public void closedCallBack(ClosedCallBackEvent evt) {
        super.closedCallBack(evt);
        String actionId = evt.getActionId();
        if ("e_corebillno".equals(actionId)) {
            this.setCoreBillNo(evt);
        } else {
            if ("e_corebillentry".equals(actionId)) {
                this.setCoreBillEntry(evt);
            }

        }
    }

    private void setCoreBillNo(ClosedCallBackEvent evt) {
        Object returnData = evt.getReturnData();
        if (!CasToolKit.isEmpty(returnData)) {
            ListSelectedRowCollection coreBillCollection = (ListSelectedRowCollection) returnData;
            if (coreBillCollection.isClearFlag()) {
                this.getModel().setValue("e_corebillno", (Object) null);
            } else {
                Iterator var4 = coreBillCollection.iterator();

                while (var4.hasNext()) {
                    ListSelectedRow listSelectedRow = (ListSelectedRow) var4.next();
                    this.getModel().setValue("e_corebillno", listSelectedRow.getBillNo());
                }

            }
        }
    }

    private void setCoreBillEntry(ClosedCallBackEvent evt) {
        Object returnData = evt.getReturnData();
        if (!CasToolKit.isEmpty(returnData)) {
            DynamicObject returnDataDy = (DynamicObject) returnData;
            this.getModel().setValue("e_corebillentryseq", returnDataDy.get("entryseq"));
        }
    }

}
