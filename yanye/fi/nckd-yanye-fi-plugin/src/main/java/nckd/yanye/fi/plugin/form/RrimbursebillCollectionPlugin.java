package nckd.yanye.fi.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
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

    private final List<String> NAME_LIST = Arrays.asList(new String[]{"paymode", "payertype", "supplier"});

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
                payeraccountStr = newValuetest.toString();
            }

            //获取改变的行号
            int rowIndex = changeData.getRowIndex();

            DynamicObjectCollection collection = this.getModel().getEntryEntity("accountentry");

            DynamicObject dynamicObject = collection.get(rowIndex);
            // 定义出支付方式，收款人类型，收款人
            String paymode = name.equals("paymode")?newValue.toString(): dynamicObject.getString("paymode");

            String payertype = name.equals("payertype")?newValue.toString(): dynamicObject.getString("payertype");

            DynamicObject supplier = name.equals("supplier")?newValue: dynamicObject.getDynamicObject("supplier");

            if(paymode!= null && (paymode.equals(BANK_ACCEP) || paymode.equals(TRADE_ACCEP))){

                if(StringUtils.isNotEmpty(payertype) && (payertype.equals("bd_supplier"))){
                    if(ObjectUtils.isNotEmpty(supplier)){
                        Object masterid = supplier.get("masterid");
                        DynamicObject eAsstactObject = BusinessDataServiceHelper.loadSingle(masterid, "bd_supplier");
                        // 把供应商承兑银行信息自动带出到往来银行字段
                        DynamicObjectCollection entryBank = eAsstactObject.getDynamicObjectCollection("entry_bank");

                        String eAssacct = name.equals("e_assacct") ? payeraccountStr:dynamicObject.getString("e_assacct");

                        if (ObjectUtils.isNotEmpty(entryBank)) {
                            for (DynamicObject object : entryBank) {
                                if (eAssacct.equals(object.getString("bankaccount"))) {
                                    dynamicObject.set("payerbank", object.getDynamicObject("nckd_acceptingbank"));
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
