package nckd.yanye.tmc.plugin.form;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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

    private final List<String> NAME_LIST = Arrays.asList(new String[]{"e_asstacttype", "e_settlementtype", "e_asstact" , "nckd_e_assacct"});

    private final List<String> ACCEP_LIST = Arrays.asList(new String[]{"JSFS06", "JSFS07"});

    private final List<String> PAYMENT_REQUEST_LIST = Arrays.asList(new String[]{"ap_payapply_BT_S", "ap_payapply_oth_BT_S"});

    private final String BD_SUPPER ="bd_supplier";
    private final String NUMBER ="number";//编号


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
            if (StringUtils.isNotEmpty(billtypeNumber) && PAYMENT_REQUEST_LIST.contains(billtypeNumber)) {
                // 更新单条分录
                ChangeData changeData = e.getChangeSet()[0];
                Object newValuetest = changeData.getNewValue();
                DynamicObject newValue = new DynamicObject();
                //获取改变的行号
                int rowIndex = changeData.getRowIndex();
                DynamicObjectCollection collection = this.getModel().getEntryEntity("entry");
                DynamicObject dynamicObject = collection.get(rowIndex);
                String eAssacctStr = "";
                if (newValuetest instanceof DynamicObject) {
                    newValue = (DynamicObject) changeData.getNewValue();
                }else if(newValuetest != null){
                    eAssacctStr = newValuetest.toString();
                }


                // 获取结算方式
                DynamicObject eSettlementtype = "e_settlementtype".equals(name) ? newValue:dynamicObject.getDynamicObject("e_settlementtype");
                if(ObjectUtils.isNotEmpty(eSettlementtype)){
                    eSettlementtype = ObjectUtils.isNotEmpty(eSettlementtype.getDataStorage())? eSettlementtype :null;
                }

                // 获取e_asstact 往来户信息
                DynamicObject eAsstact = "e_asstact".equals(name) ? newValue:dynamicObject.getDynamicObject("e_asstact");

                if (ObjectUtils.isNotEmpty(eAsstact) && ObjectUtils.isNotEmpty(eAsstact.getDataStorage()) ) {
                    Object masterid = eAsstact.get("masterid");

                    String eAsstacttype = "e_asstacttype".equals(name) ? newValue.toString():dynamicObject.getString("e_asstacttype");
                    if(BD_SUPPER.equals(eAsstacttype)){

                            if (ObjectUtils.isNotEmpty(eSettlementtype) && ACCEP_LIST.contains(eSettlementtype.getString(NUMBER)) ) {
                                String eSettlementtypeName = (String) eSettlementtype.get(NUMBER);
                                //银行账户
                                String eAssacct = "nckd_e_assacct".equals(name) ? eAssacctStr:dynamicObject.getString("nckd_e_assacct");

                                DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");

                                DynamicObject o =  eAsstactObject.getDynamicObject("internal_company");
                                if(ObjectUtils.isNotEmpty(o)){
                                    QFilter qFilter3 = new QFilter("account.bankaccountnumber", "=", eAssacct);
                                    // 查询供应商的银行账户信息
                                    // 根据银行账户去查询对应票据开户行信息
                                    DynamicObject billbank = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","account,billbank",new QFilter[]{qFilter3});
                                    if(ObjectUtils.isNotEmpty(billbank)){
                                        // 合作金融机构信息
                                        DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(billbank.getLong("billbank.id"),"bd_finorginfo");
                                        dynamicObject.set("e_bebank", bdFinorginfo.getDynamicObject("bebank"));
                                        this.getView().updateView();
                                        return;
                                    }
                                }
                                // 把供应商承兑银行信息自动带出到往来银行字段
                                DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");
                                if (ObjectUtils.isNotEmpty(entryBank)) {

                                    for (DynamicObject object : entryBank) {
                                        if (object.getString("bankaccount").equals(eAssacct)) {
                                            dynamicObject.set("e_bebank", object.getDynamicObject("nckd_acceptingbank"));
                                            break; // 找到符合条件的记录后退出循环
                                        }
                                    }
                                    // 刷新页面
                                    this.getView().updateView();

                                }
                            }else{
                                String eAssacct = "nckd_e_assacct".equals(name) ? eAssacctStr:dynamicObject.getString("nckd_e_assacct");
                                //  处理内部供应商情况
                                DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");
                                DynamicObject o = eAsstactObject.getDynamicObject("internal_company");
                                if(ObjectUtils.isNotEmpty(o)){
                                    QFilter qFilter = new QFilter("openorg.masterid", "=", o.getPkValue());
                                    QFilter qFilter2 = new QFilter("bankaccountnumber", "=", eAssacct);
                                    // 查询供应商的银行账户信息
                                    DynamicObject amAccountbank = BusinessDataServiceHelper.loadSingle("am_accountbank", "bank,bankaccountnumber,currency", new QFilter[]{qFilter,qFilter2});
                                    if(ObjectUtils.isNotEmpty(amAccountbank)){
                                        long aLong = amAccountbank.getLong("bank.id");
                                        // 行名行号
                                        DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(aLong, "bd_finorginfo");
                                        dynamicObject.set("e_bebank", bdFinorginfo.getDynamicObject("bebank"));
                                        this.getView().updateView();
                                        return;
                                    }
                                }

                                // 不是承兑银行带出正常银行到往来银行字段
                                DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");
                                if (ObjectUtils.isNotEmpty(entryBank)) {

                                    for (DynamicObject object : entryBank) {
                                        if (object.getString("bankaccount").equals(eAssacct)) {
                                            dynamicObject.set("e_bebank", object.getDynamicObject("bank"));
                                            break; // 找到符合条件的记录后退出循环
                                        }
                                    }
                                    // 刷新页面
                                    this.getView().updateView();
                                }

                            }

                    }
                }
            }
        }
    }

}
