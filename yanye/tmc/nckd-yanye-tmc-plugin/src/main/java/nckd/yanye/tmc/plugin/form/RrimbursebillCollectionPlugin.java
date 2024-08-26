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

import java.util.Arrays;
import java.util.List;


/**
 * Module           :财务云-费用核算-对公费用单据-对公报销单
 * Description      :对公报销单-收款信息分录
 *
 *
 * @author guozhiwei
 * @date  2024/8/12 17:05
 * 标识 er_publicreimbursebill
 *
 */

public class RrimbursebillCollectionPlugin extends AbstractBillPlugIn {

    private final List<String> NAME_LIST = Arrays.asList(new String[]{"paymode", "payertype", "supplier","payeraccount"});

    private final String BANK_ACCEP = "JSFS06"; // 银行承兑汇票

    private final String TRADE_ACCEP = "JSFS07";//  商业承兑汇票


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        // 支付方式的类别为 银行承兑汇票 或 商业承兑汇票，收款人类型为供应商，并且选了收款人，然后自动带出承兑银行到开户银行字段

        String name = e.getProperty().getName();
        if(NAME_LIST.contains(name)){

            ChangeData changeData = e.getChangeSet()[0];
            String payeraccountStr = "";
            Object newValuetest = changeData.getNewValue();
            DynamicObject newValue = new DynamicObject();

            if (newValuetest instanceof DynamicObject) {
                newValue = (DynamicObject) changeData.getNewValue();
            }else{
                payeraccountStr = ObjectUtils.isNotEmpty(newValuetest)?newValuetest.toString():"";
            }

            //获取改变的行号
            int rowIndex = changeData.getRowIndex();

            DynamicObjectCollection collection = this.getModel().getEntryEntity("accountentry");

            DynamicObject dynamicObject = collection.get(rowIndex);
            // 定义出支付方式，收款人类型，收款人
            String paymode = name.equals("paymode")?newValue.getString("number"): ObjectUtils.isNotEmpty(dynamicObject.getDynamicObject("paymode")) ? dynamicObject.getDynamicObject("paymode").getString("number"): null;

            String payertype = name.equals("payertype")?newValue.toString(): dynamicObject.getString("payertype");

            DynamicObject supplier = name.equals("supplier")?newValue: dynamicObject.getDynamicObject("supplier");

            if(StringUtils.isNotEmpty(payertype) && (payertype.equals("bd_supplier")) ){

                String eAssacct = name.equals("payeraccount") ? payeraccountStr:dynamicObject.getString("payeraccount");

                if(ObjectUtils.isNotEmpty(supplier)){
                    Object masterid = supplier.get("masterid");
                    // 内部供应商获取
//                    DynamicObject innerSupplier = isInnerSupplier(masterid);
//                    if(innerSupplier != null){
//                        dynamicObject.set("payeraccount", innerSupplier.getDynamicObject("account").getString("number"));
////                        dynamicObject.set("nckd_e_assacct", innerSupplier.getDynamicObject("account").getString("number"));
//                        dynamicObject.set("payerbank", innerSupplier.getDynamicObject("bank"));
//                        this.getView().updateView();
//                        return;
//                    }
                    if(StringUtils.isNotEmpty(paymode)  && (paymode.equals(BANK_ACCEP) || paymode.equals(TRADE_ACCEP))){

                        DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");
                        // 把供应商承兑银行信息自动带出到往来银行字段
                        DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");

                        if (ObjectUtils.isNotEmpty(entryBank)) {
                            for (DynamicObject object : entryBank) {
                                if (eAssacct.equals(object.getString("bankaccount"))) {
                                    dynamicObject.set("payerbank", object.getDynamicObject("nckd_acceptingbank"));
                                    // 刷新页面
                                    break; // 找到符合条件的记录后退出循环
                                }
                            }

                        }
                        this.getView().updateView();
                    }
                }
            }
        }
    }

    // 获取内部供应商信息
    public DynamicObject isInnerSupplier(Object supplierid) {
        if (ObjectUtils.isNotEmpty(supplierid)) {
            // 查询是否存在内部公司
            DynamicObject internalCompany = BusinessDataServiceHelper.loadSingle(supplierid, "bd_supplier").getDynamicObject("internal_company");
            if (ObjectUtils.isNotEmpty(internalCompany)) {
                // 获取票据账号开户行维护信息
                QFilter qFilter = new QFilter("company.masterid", "=", internalCompany.getPkValue());
                DynamicObject load = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","bank,openorg,account,billbank,bankname", new QFilter[]{qFilter} );
                if (ObjectUtils.isNotEmpty(load)) {
                    // 存在票据账号开户行维护信息
                    return load;
                }
            }
        }
        return null;
    }


}
