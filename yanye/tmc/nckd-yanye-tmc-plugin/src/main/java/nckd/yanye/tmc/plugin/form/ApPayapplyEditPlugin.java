package nckd.yanye.tmc.plugin.form;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;

import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;

import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

/**
 * Module           :财务云-应付-付款申请-付款申请单
 * Description      :付款申请单-明细分录-承兑银行带出到往来银行字段
 *
 *
 * @author guozhiwei
 * @date  2024/8/12 14:20
 * 标识 nckd_ap_payapply_ext
 *
 *
 */


public class ApPayapplyEditPlugin extends AbstractBillPlugIn {

    private final List<String> NAME_LIST = Arrays.asList(new String[]{"e_asstacttype", "e_settlementtype", "e_asstact" , "e_assacct"});

    private final String BANK_ACCEP = "JSFS06";
    private final String TRADE_ACCEP = "JSFS07";

    private final String PURCHASE_PAYMENT_REQUEST = "ap_payapply_BT_S";

    private final String OTHER_PAYMENT_REQUEST = "ap_payapply_oth_BT_S";



    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        if(NAME_LIST.contains(name)) {

            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype");
            String billtypeNumber = (String) billtype.get("number");
            if (StringUtils.isNotEmpty(billtypeNumber) && (billtypeNumber.equals(PURCHASE_PAYMENT_REQUEST) || billtypeNumber.equals(OTHER_PAYMENT_REQUEST))) {
                // 更新单条分录
                ChangeData changeData = e.getChangeSet()[0];
                Object newValuetest = changeData.getNewValue();
                DynamicObject newValue = new DynamicObject();
                String eAssacctStr = "";
                if (newValuetest instanceof DynamicObject) {
                    newValue = (DynamicObject) changeData.getNewValue();
                }else if(newValuetest != null){
                    eAssacctStr = newValuetest.toString();
                }
//                else{
//                    return;
//                }
//                DynamicObject newValue = (DynamicObject) changeData.getNewValue();
                //获取改变的行号
                int rowIndex = changeData.getRowIndex();

                DynamicObjectCollection collection = this.getModel().getEntryEntity("entry");

                DynamicObject dynamicObject = collection.get(rowIndex);
                // 获取结算方式
                DynamicObject eSettlementtype = name.equals("e_settlementtype") ? newValue:dynamicObject.getDynamicObject("e_settlementtype");


                // 获取e_asstact 往来户信息
                DynamicObject eAsstact = name.equals("e_asstact") ? newValue:dynamicObject.getDynamicObject("e_asstact");
                if (ObjectUtils.isNotEmpty(eAsstact)) {
                    Object masterid = eAsstact.get("masterid");

                    String eAsstacttype = name.equals("e_asstacttype") ? newValue.toString():dynamicObject.getString("e_asstacttype");
                    if("bd_supplier".equals(eAsstacttype)){
                        if(name.equals("e_assacct")){
                            Map<String,Object> innerSupplier = isInnerSupplier(eAsstact.get("masterid"));
                            if(innerSupplier != null){
                                dynamicObject.set("e_assacct", innerSupplier.get("number"));
                                dynamicObject.set("nckd_e_assacct", innerSupplier.get("number"));

                                dynamicObject.set("e_bebank", innerSupplier.get("bankid"));
                                this.getView().updateView();
                                return;
                            }
                        }
                        if(ObjectUtils.isNotEmpty(eSettlementtype)){
                            String eSettlementtypeName = (String) eSettlementtype.get("number");
                            if (BANK_ACCEP.equals(eSettlementtypeName) || TRADE_ACCEP.equals(eSettlementtypeName) ) {

                                //银行账户
                                String eAssacct = name.equals("e_assacct") ? eAssacctStr:dynamicObject.getString("e_assacct");

                                DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");

                                // 把供应商承兑银行信息自动带出到往来银行字段
                                DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");
                                if (ObjectUtils.isNotEmpty(entryBank)) {

                                    for (DynamicObject object : entryBank) {
                                        if (eAssacct.equals(object.getString("bankaccount"))) {
                                            dynamicObject.set("e_bebank", object.getDynamicObject("nckd_acceptingbank"));
                                            // 刷新页面
                                            this.getView().updateView();
                                            break; // 找到符合条件的记录后退出循环
                                        }
                                    }

                                }
                            }else{
                                //银行账户
                                String eAssacct = name.equals("e_assacct") ? eAssacctStr:dynamicObject.getString("e_assacct");

                                DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");

                                // 不是承兑银行带出正常银行到往来银行字段
                                DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");
                                if (ObjectUtils.isNotEmpty(entryBank)) {

                                    for (DynamicObject object : entryBank) {
                                        if (eAssacct.equals(object.getString("bankaccount"))) {
                                            dynamicObject.set("e_bebank", object.getDynamicObject("bank"));
                                            // 刷新页面
                                            this.getView().updateView();
                                            break; // 找到符合条件的记录后退出循环
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

//     校验是否是内部供应商
    public Map<String, Object> isInnerSupplier(Object supplierid) {
        Map<String, Object> map = new HashMap<>();

        // 查询是否存在内部公司
        DynamicObject o = (DynamicObject) BusinessDataServiceHelper.loadSingle(supplierid, "bd_supplier").get("internal_company");
        if (ObjectUtils.isNotEmpty(o)) {
            QFilter qFilter = new QFilter("openorg.masterid", "=", o.getPkValue());
            // 查询供应商的银行账户信息
            DynamicObject amAccountbank = BusinessDataServiceHelper.loadSingle("am_accountbank", "bank,bankaccountnumber,currency", new QFilter[]{qFilter});
            if (ObjectUtils.isNotEmpty(amAccountbank)) {

                map.put("number", amAccountbank.getString("bankaccountnumber"));
                amAccountbank.getLong("bank.id");
                // todo 查询银行账户是否有对应票据开户行信息，如果有，则设置到e_bebank
                QFilter qFilter2 = new QFilter("account.masterid", "=", amAccountbank.getPkValue());
                // 合作金融机构
                Object cooperationId = null;
                DynamicObject billbank = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","billbank.id",new QFilter[]{qFilter2});
                if (ObjectUtils.isNotEmpty(billbank)) {
                    cooperationId = amAccountbank.get("bank.id");
                }else{
                    cooperationId = billbank.getLong("bebank.id");
                }
                // 合作金融机构信息
                DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(cooperationId, "bd_finorginfo");
                map.put("bankid",bdFinorginfo.getLong("bebank.id"));
                return map;
            }
        }
        return null;
    }

}
