package nckd.yanye.scm.plugin.form;

import com.alibaba.fastjson.JSONObject;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillShowParameter;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.events.HyperLinkClickEvent;
import kd.bos.form.events.HyperLinkClickListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :供应链云-采购管理-采购订单
 * Description      :采购订单表单插件
 *
 * @author : zhujintao
 * @date : 2024/7/12
 */
public class PurorderbillBillPlugin extends AbstractBillPlugIn implements HyperLinkClickListener {

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        //采购订单编辑时，选择供应商、物料，系统根据 供应商、物料、订单日期，自动匹配有效采购合同，匹配成功后，将对应的 合同编号 及 合同行号 进行记录；
        String name = e.getProperty().getName();
        //如果供应商变为空 ，清空合同编号及合同行号
        if (StringUtils.equals("supplier", name)) {
            Object org = this.getModel().getValue("org");
            Object supplier = this.getModel().getValue("supplier");
            DynamicObjectCollection billentry = this.getModel().getEntryEntity("billentry");
            if (ObjectUtils.isEmpty(supplier)) {
                dealPurcontractEntry(billentry);
            } else {
                Object bizTime = this.getModel().getValue("biztime");
                if (ObjectUtils.isNotEmpty(billentry)) {
                    List<Object> list = billentry.stream().filter(h -> ObjectUtils.isNotEmpty(h.getDynamicObject("material")))
                            .map(k -> k.getDynamicObject("material").getPkValue()).collect(Collectors.toList());
                    if (list.size() == 0) {
                        return;
                    }
                    //构造查询条件
                    DynamicObject orgdy = (DynamicObject) org;
                    DynamicObject supplierDy = (DynamicObject) supplier;
                    Date bizDate = ObjectUtils.isNotEmpty(bizTime) ? (Date) bizTime : new Date();
                    QFilter qFilter = new QFilter("org", QCP.equals, orgdy.getPkValue())
                            .and("supplier", QCP.equals, supplierDy.getPkValue())
                            .and("billstatus", QCP.equals, "C")
                            .and("validstatus", QCP.equals, "B")
                            .and("closestatus", QCP.equals, "A")
                            .and("billentry.material", QCP.in, list)
                            .and("biztimebegin", QCP.less_equals, bizDate)
                            .and("biztimeend", QCP.large_equals, bizDate);
                    //String entityName, String selectProperties, QFilter[] filters
                    DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("conm_purcontract", "id,billno,billentry.id,billentry.material,billentry.lineno,billentry.price,billentry.priceandtax,billentry.taxrateid,billentry.seq", qFilter.toArray());
                    //直接没查到，那就置空
                    if (purcontractArr.length == 0) {
                        dealPurcontractEntry(billentry);
                        return;
                    }
                    //purcontractArr 转换为key material value JSONObject(billno,seq)
                    Map<Object, JSONObject> map = new HashMap<>();
                    for (DynamicObject purcontractBill : purcontractArr) {
                        DynamicObjectCollection purcontractEntryList = purcontractBill.getDynamicObjectCollection("billentry");
                        for (DynamicObject entry : purcontractEntryList) {
                            JSONObject json = new JSONObject();
                            json.put("conbillentity",purcontractBill.getPkValue());
                            json.put("id", purcontractBill.getString("id"));
                            json.put("billno", purcontractBill.getString("billno"));
                            json.put("entryid", entry.getString("id"));
                            json.put("lineno", entry.getInt("lineno"));
                            json.put("seq", entry.getInt("seq"));
                            json.put("price", entry.getBigDecimal("price"));
                            json.put("priceandtax", entry.getBigDecimal("priceandtax"));
                            json.put("taxrateid", entry.getDynamicObject("taxrateid"));
                            map.put(entry.getDynamicObject("material").getPkValue(), json);
                        }
                    }

                    //遍历当前单据分录，自动匹配有效采购合同，匹配成功后，将对应的合同编号及合同行号进行记录；
                    for (DynamicObject dy : billentry) {
                        //int seq = dy.getInt("seq");
                        JSONObject json = map.get(dy.getDynamicObject("material").getPkValue());
                        if (json != null) {
                            dy.set("conbillentity",json.get("conbillentity"));
                            dy.set("conbillid", json.get("id"));
                            dy.set("conbillnumber", json.get("billno"));
                            dy.set("conbillentryid", json.get("entryid"));
                            dy.set("conbillrownum", json.get("lineno"));
                            dy.set("conbillentryseq", json.getString("seq"));
                            BigDecimal price = json.getBigDecimal("price");
                            dy.set("price", price);
                            BigDecimal priceandtax = json.getBigDecimal("priceandtax");
                            dy.set("priceandtax", priceandtax);
                            dy.set("taxrateid", json.get("taxrateid"));
                            //价税合计（含本位币）=含税单价*数量---保留两位小数
                            //金额（含本位币）=单价*数量--------保留两位小数
                            //税额（含本位币）=价税合计-金额---------保留两位小数
                            BigDecimal qty = dy.getBigDecimal("qty");
                            BigDecimal amountandtax = priceandtax.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                            dy.set("amountandtax", amountandtax);
                            dy.set("curamountandtax", amountandtax);
                            BigDecimal amount = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                            dy.set("amount", amount);
                            dy.set("curamount", amount);
                            BigDecimal taxamount = amountandtax.subtract(amount);
                            dy.set("taxamount", taxamount);
                            dy.set("curtaxamount", taxamount);
                        } else {
                            dy.set("conbillentity",null);
                            dy.set("conbillid", null);
                            dy.set("conbillnumber", null);
                            dy.set("conbillentryid", null);
                            dy.set("conbillrownum", null);
                            dy.set("conbillentryseq", null);
                            dy.set("price", null);
                            dy.set("priceandtax", null);
                            dy.set("taxrateid", null);
                            dy.set("amountandtax", null);
                            dy.set("curamountandtax", null);
                            dy.set("amount", null);
                            dy.set("curamount", null);
                            dy.set("taxamount", null);
                            dy.set("curtaxamount", null);
                        }
                    }
                }
            }
        }
        //如果物料变化为空 ，清空对应行合同编号及合同行号
        if (StringUtils.equals("material", name)) {
            ChangeData changeData = e.getChangeSet()[0]; //修改值所在行
            DynamicObject dataEntity = changeData.getDataEntity(); //修改值所在行数据
            Object newValue = changeData.getNewValue();//新值
            int rowIndex = changeData.getRowIndex(); //修改行所在行行号
            if (ObjectUtils.isEmpty(newValue)) {
                dealPurcontractEntryBySeq(rowIndex);
            } else {
                Object org = this.getModel().getValue("org");
                Object bizTime = this.getModel().getValue("biztime");
                Object supplier = this.getModel().getValue("supplier");
                if (ObjectUtils.isNotEmpty(supplier)) {
                    //构造查询条件
                    DynamicObject orgdy = (DynamicObject) org;
                    DynamicObject supplierDy = (DynamicObject) supplier;
                    Date bizDate = ObjectUtils.isNotEmpty(bizTime) ? (Date) bizTime : new Date();
                    DynamicObject material = (DynamicObject) newValue;
                    QFilter qFilter = new QFilter("org", QCP.equals, orgdy.getPkValue())
                            .and("supplier", QCP.equals, supplierDy.getPkValue())
                            .and("billstatus", QCP.equals, "C")
                            .and("validstatus", QCP.equals, "B")
                            .and("closestatus", QCP.equals, "A")
                            .and("billentry.material", QCP.equals, material.getPkValue())
                            .and("biztimebegin", QCP.less_equals, bizDate)
                            .and("biztimeend", QCP.large_equals, bizDate);
                    //String entityName, String selectProperties, QFilter[] filters
                    DynamicObject purcontractBill = BusinessDataServiceHelper.loadSingle("conm_purcontract", "id,billno,billentry.id,billentry.lineno,billentry.material,billentry.price,billentry.priceandtax,billentry.taxrateid,billentry.seq", qFilter.toArray());
                    if (ObjectUtils.isEmpty(purcontractBill)) {
                        dealPurcontractEntryBySeq(rowIndex);
                        return;
                    } else {
                        //自动匹配有效采购合同，匹配成功后，将对应的合同编号及合同行号进行记录；
                        DynamicObjectCollection purcontractEntryList = purcontractBill.getDynamicObjectCollection("billentry");
                        Map<Object, DynamicObject> purcontractEntryMap = purcontractEntryList.stream().collect(Collectors.toMap(k -> k.getDynamicObject("material").getPkValue(), v -> v));
                        DynamicObject purcontractEntry = purcontractEntryMap.get(material.getPkValue());
                        this.getModel().setValue("conbillentity", purcontractBill.getPkValue(), rowIndex);
                        this.getModel().setValue("conbillid", purcontractBill.get("id"), rowIndex);
                        this.getModel().setValue("conbillnumber", purcontractBill.getString("billno"), rowIndex);
                        this.getModel().setValue("conbillentryid", purcontractEntry.get("id"), rowIndex);
                        this.getModel().setValue("conbillrownum", purcontractEntry.getInt("lineno"), rowIndex);
                        this.getModel().setValue("conbillentryseq", purcontractEntry.getInt("seq"), rowIndex);
                        BigDecimal price = purcontractEntry.getBigDecimal("price");
                        this.getModel().setValue("price", price, rowIndex);
                        BigDecimal priceandtax = purcontractEntry.getBigDecimal("priceandtax");
                        this.getModel().setValue("priceandtax", priceandtax, rowIndex);
                        this.getModel().setValue("taxrateid", purcontractEntry.getDynamicObject("taxrateid"), rowIndex);
                        //价税合计（含本位币）=含税单价*数量---保留两位小数
                        //金额（含本位币）=单价*数量--------保留两位小数
                        //税额（含本位币）=价税合计-金额---------保留两位小数
                        BigDecimal qty = dataEntity.getBigDecimal("qty");
                        BigDecimal amountandtax = priceandtax.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                        this.getModel().setValue("amountandtax", amountandtax, rowIndex);
                        this.getModel().setValue("curamountandtax", amountandtax, rowIndex);
                        BigDecimal amount = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                        this.getModel().setValue("amount", amount, rowIndex);
                        this.getModel().setValue("curamount", amount, rowIndex);
                        //税额（含本位币）=价税合计-金额---------保留两位小数
                        BigDecimal taxamount = amountandtax.subtract(amount);
                        this.getModel().setValue("taxamount", taxamount, rowIndex);
                        this.getModel().setValue("curtaxamount", taxamount, rowIndex);
                    }
                }
            }
        }
        this.getView().updateView();
    }

    /**
     * 将采购订单的分录 合同编号 及 合同行号 全部清空
     *
     * @param billentry
     */
    private void dealPurcontractEntry(DynamicObjectCollection billentry) {
        for (DynamicObject entry : billentry) {
            entry.set("conbillentity",null);
            entry.set("conbillid", null);
            entry.set("conbillnumber", null);
            entry.set("conbillentryid", null);
            entry.set("conbillrownum", null);
            entry.set("conbillentryseq", null);
            entry.set("price", null);
            entry.set("priceandtax", null);
            entry.set("taxrateid", null);

            entry.set("amountandtax", null);
            entry.set("curamountandtax", null);
            entry.set("amount", null);
            entry.set("curamount", null);
            entry.set("taxamount", null);
            entry.set("curtaxamount", null);
        }
        this.getView().updateView();
    }

    /**
     * 将采购订单的分录 合同编号 及 合同行号 全部清空
     *
     * @param rowIndex
     */
    private void dealPurcontractEntryBySeq(int rowIndex) {
        this.getModel().setValue("conbillentity", null, rowIndex);
        this.getModel().setValue("conbillid", null, rowIndex);
        this.getModel().setValue("conbillnumber", null, rowIndex);
        this.getModel().setValue("conbillentryid", null, rowIndex);
        this.getModel().setValue("conbillrownum", null, rowIndex);
        this.getModel().setValue("conbillentryseq", null, rowIndex);
        this.getModel().setValue("price", null, rowIndex);
        this.getModel().setValue("priceandtax", null, rowIndex);
        this.getModel().setValue("taxrateid", null, rowIndex);

        this.getModel().setValue("amountandtax", null, rowIndex);
        this.getModel().setValue("curamountandtax", null, rowIndex);
        this.getModel().setValue("amount", null, rowIndex);
        this.getModel().setValue("curamount", null, rowIndex);
        this.getModel().setValue("taxamount", null, rowIndex);
        this.getModel().setValue("curtaxamount", null, rowIndex);
        this.getView().updateView();
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //监听物料明细
        EntryGrid billentry = this.getView().getControl("billentry");
        billentry.addHyperClickListener(this);
    }

    @Override
    public void hyperLinkClick(HyperLinkClickEvent hyperLinkClickEvent) {
        String key = hyperLinkClickEvent.getFieldName();
        //采购订单匹配到采购合同后，将采购合同号进行记录，且点击该字段可直接跳转至采购合同
        if(StringUtils.equals("conbillnumber",key)){
            int rowIndex = hyperLinkClickEvent.getRowIndex();
            BillShowParameter billShowParameter = new BillShowParameter();
            billShowParameter.setFormId("conm_purcontract");
            DynamicObject billentry = this.getModel().getEntryRowEntity("billentry", rowIndex);
            Object conbillid = billentry.get("conbillid");
            billShowParameter.setPkId(conbillid);
            billShowParameter.setCaption(new LocaleString("采购合同").toString());
            billShowParameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
            this.getView().showForm(billShowParameter);
        }
    }
}
