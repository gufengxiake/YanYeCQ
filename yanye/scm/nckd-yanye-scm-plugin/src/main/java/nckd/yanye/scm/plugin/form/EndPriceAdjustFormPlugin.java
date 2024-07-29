package nckd.yanye.scm.plugin.form;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;

/**
 * @author husheng
 * @date 2024-07-25 9:17
 * @description 月末调价单-表单插件
 */
public class EndPriceAdjustFormPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);

        String name = e.getProperty().getName();
        // 根据供应商和物料计算新金额
        if("nckd_adjustunitprice".equals(name)){
            ChangeData changeData = e.getChangeSet()[0];
            DynamicObject dynamicObject = changeData.getDataEntity();
            Object supplieradjust = dynamicObject.getDynamicObject("nckd_supplieradjust").getPkValue();
            Object material = dynamicObject.getDynamicObject("nckd_material").getPkValue();

            DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("entryentity");
            for (DynamicObject object : entryEntity) {
                Object nckdSupplier = object.getDynamicObject("nckd_supplier").getPkValue();
                Object materialcode = object.getDynamicObject("nckd_materialcode").getPkValue();

                if(Objects.equals(supplieradjust,nckdSupplier) && Objects.equals(material,materialcode)){
                    //税率
                    DynamicObject nckdTaxrate = object.getDynamicObject("nckd_taxrate");
                    BigDecimal taxrate = new BigDecimal(0);
                    if(nckdTaxrate != null){
                        taxrate = nckdTaxrate.getBigDecimal("taxrate").divide(new BigDecimal(100));
                    }
                    //数量
                    BigDecimal quantity = object.getBigDecimal("nckd_quantity");
                    //新含税单价
                    BigDecimal newftaxunitprice = ((BigDecimal) changeData.getNewValue()).setScale(2,BigDecimal.ROUND_HALF_UP);
                    //新单价
                    BigDecimal newunitprice = newftaxunitprice.divide(new BigDecimal(1).add(taxrate), 6, BigDecimal.ROUND_HALF_UP);
                    //新价税合计
                    BigDecimal newtotalprice = newftaxunitprice.multiply(quantity).setScale(2, BigDecimal.ROUND_HALF_UP);
                    //新金额
                    BigDecimal newamount = newunitprice.multiply(quantity).setScale(2, BigDecimal.ROUND_HALF_UP);
                    //新税额
                    BigDecimal newtax = newtotalprice.subtract(newamount);
                    //新计成本金额
                    BigDecimal newcostamount = newamount;

                    object.set("nckd_newunitprice",newunitprice);
                    object.set("nckd_newtax",newtax);
                    object.set("nckd_newftaxunitprice",newftaxunitprice);
                    object.set("nckd_newamount",newamount);
                    object.set("nckd_newtotalprice",newtotalprice);
                    object.set("nckd_newcostamount",newcostamount);
                }
            }
            this.getView().updateView();
        }
    }
}
