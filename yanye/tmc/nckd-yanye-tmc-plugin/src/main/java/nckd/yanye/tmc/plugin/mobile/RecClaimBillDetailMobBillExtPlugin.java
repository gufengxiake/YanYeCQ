package nckd.yanye.tmc.plugin.mobile;

import kd.bos.bill.AbstractMobBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.CloseCallBack;
import kd.bos.form.IFormView;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.MobileListShowParameter;
import kd.bos.orm.query.QFilter;
import kd.bos.orm.util.CollectionUtils;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.cas.formplugin.mobile.recclaim.utils.CasToolKit;
import kd.fi.cas.formplugin.mobile.recclaim.utils.EmptyUtil;

import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

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
        } /*else {
            if ("corebillentrybtn".equals(key)) {
                this.showCoreBillEntry();
            }
        }*/
    }

    /*@Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        IDataModel model = this.getModel();
        IFormView view = this.getView();
        IDataEntityProperty iDataEntityProperty = e.getProperty();
        String propertyName = iDataEntityProperty.getName();
        Object newValueObj;
        Object coreBillTypeObj = this.getModel().getValue("e_corebilltype");
        if (EmptyUtil.isEmpty(coreBillTypeObj)) {
            model.beginInit();
            model.setValue("e_corebillno", (Object) null);
            model.setValue("e_corebillid", (Object) null);
            model.endInit();
            view.showTipNotification(ResManager.loadKDString("请先指定核心单据类型。", "RecClaimBillDetailMobBillPlugin_0", "fi-cas-mobile", new Object[0]));
            return;
        }
        String coreBillType = (String) coreBillTypeObj;
        if (StringUtils.equals(propertyName, "e_corebillno") && "ocbsoc_saleorder".equals(coreBillType)) {
            model.setValue("e_corebillid", (Object) null);
            model.setValue("e_corebillentryseq", (Object) null);
            model.setValue("e_corebillentryid", (Object) null);
            newValueObj = e.getChangeSet()[0].getNewValue();
            if (EmptyUtil.isNoEmpty(newValueObj)) {
                String coreBillNo = (String) newValueObj;
                if ("ocbsoc_saleorder".equals(coreBillType)) {
                    List<Object> coreBillCollIds = QueryServiceHelper.queryPrimaryKeys(coreBillType, new QFilter[]{new QFilter("billno", "=", coreBillNo)}, (String) null, 1);
                    if (!CollectionUtils.isEmpty(coreBillCollIds)) {
                        model.setValue("e_corebillid", coreBillCollIds.get(0));
                    } else {
                        model.beginInit();
                        model.setValue("e_corebillno", (Object) null);
                        model.setValue("e_corebillid", (Object) null);
                        model.endInit();
                        view.showTipNotification(ResManager.loadKDString("对应核心单据类型不存在此核心单据编号。", "RecClaimBillDetailMobBillPlugin_2", "fi-cas-mobile", new Object[0]));
                    }
                }
            }

        }
    }*/

    @Override
    public void closedCallBack(ClosedCallBackEvent evt) {
        super.closedCallBack(evt);
        String actionId = evt.getActionId();
        if ("e_corebillno_ext".equals(actionId)) {
            this.setCoreBillNo(evt);
        } /*else {
            if ("e_corebillentry".equals(actionId)) {
                this.setCoreBillEntry(evt);
            }

        }*/
    }

    private void showCoreBillF7() {
        String coreBillType = (String) this.getModel().getValue("e_corebilltype");

        if ("ocbsoc_saleorder".equals(coreBillType)) {
            DynamicObject settleOrgDy = (DynamicObject) this.getModel().getValue("e_settleorg");
            Object settleOrgId = settleOrgDy != null ? settleOrgDy.getPkValue() : null;
            if (settleOrgId == null) {
                this.getView().showTipNotification(ResManager.loadKDString("请先指定结算组织。", "RecClaimBillDetailMobBillPlugin_1", "fi-cas-mobile", new Object[0]));
            } else {
                ListFilterParameter lfp = new ListFilterParameter();

                lfp.setFilter(new QFilter("saleorgid", "=", settleOrgId));
                //lfp.setFilter(new QFilter("billstatus", "=", "C"));

                MobileListShowParameter lsp = new MobileListShowParameter();
                CloseCallBack closeCallBack = new CloseCallBack(this, "e_corebillno_ext");
                lsp.setFormId("bos_moblisttabf7");
                lsp.setBillFormId("nckd_ocbsoc_saleorderbase");
                lsp.setCustomParam("ismergerows", Boolean.FALSE);
                lsp.setCaption(ResManager.loadKDString("核心单据编号", "RecClaimBillDetailMobBillPlugin_3", "fi-cas-mobile", new Object[0]));
                lsp.getOpenStyle().setShowType(ShowType.Floating);
                lsp.setLookUp(true);
                lsp.setListFilterParameter(lfp);
                lsp.setCloseCallBack(closeCallBack);
                this.getView().showForm(lsp);
            }
        }
    }

    /*private void showCoreBillEntry() {
        String coreBillType = (String) this.getModel().getValue("e_corebilltype");
        if (StringUtils.isEmpty(coreBillType)) {
            this.getView().showTipNotification(ResManager.loadKDString("请先指定核心单据类型。", "RecClaimBillDetailMobBillPlugin_0", "fi-cas-mobile", new Object[0]));
        } else {
            ClaimCoreBillTypeEnum coreBillTypeEnum = ClaimCoreBillTypeEnum.getEnum(coreBillType);
            if (coreBillTypeEnum != null) {
                if (coreBillTypeEnum == ClaimCoreBillTypeEnum.SALORDER || coreBillTypeEnum == ClaimCoreBillTypeEnum.SALCONTRACT) {
                    String coreBillId = (String) this.getModel().getValue("e_corebillid");
                    if (coreBillId != null && !StringUtils.isEmpty(coreBillId)) {
                        Object settleOrgId = ((DynamicObject) this.getModel().getValue("e_settleorg")).getPkValue();
                        if (settleOrgId == null) {
                            this.getView().showTipNotification(ResManager.loadKDString("请先指定结算组织。", "RecClaimBillDetailMobBillPlugin_1", "fi-cas-mobile", new Object[0]));
                        } else {
                            String formId = coreBillTypeEnum.getMobCode() == null ? coreBillTypeEnum.getCode() : coreBillTypeEnum.getMobCode();
                            MobileBillShowParameter bsp = new MobileBillShowParameter();
                            CloseCallBack closeCallBack = new CloseCallBack(this, "e_corebillentry");
                            bsp.setFormId(formId);
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
        }
    }*/

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
                    Object primaryKeyValue = listSelectedRow.getPrimaryKeyValue();
                    DynamicObject ocbsocSaleorder = BusinessDataServiceHelper.loadSingle(primaryKeyValue, "ocbsoc_saleorder");
                    this.getModel().setValue("e_corebillno", ocbsocSaleorder.getString("billno"));
                    this.getModel().setValue("e_corebillid", ocbsocSaleorder.getPkValue());
                }

            }
        }
    }

    /*private void setCoreBillEntry(ClosedCallBackEvent evt) {
        Object returnData = evt.getReturnData();
        if (!CasToolKit.isEmpty(returnData)) {
            DynamicObject returnDataDy = (DynamicObject) returnData;
            this.getModel().setValue("e_corebillentryseq", returnDataDy.get("entryseq"));
        }
    }*/

}
