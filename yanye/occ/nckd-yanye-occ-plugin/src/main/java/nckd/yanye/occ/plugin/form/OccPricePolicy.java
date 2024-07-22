package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 价格政策表单取价
 * 表单插件
 * author:吴国强 2024-07-12
 */
public class OccPricePolicy extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {

        String propName = e.getProperty().getName();
        //商品
        if (propName.equals("item") || propName.equals("unit")) {
            Date billDate = DateTime.now().toDate();
            //获取当前行
            int index = e.getChangeSet()[0].getRowIndex();
            DynamicObject item = (DynamicObject) this.getModel().getValue("item", index);
            if (item == null) return;
            //商品Id
            Object itemId = item.getPkValue();
            DynamicObject unit = (DynamicObject) this.getModel().getValue("unit", index);
            if (unit == null) return;
            //单位Id
            Object unitId = unit.getPkValue();
            //表单标识
            String number = "ocdbd_pricepolicy";
            //查询字段
            String fieldkey = "id,number,priceentry.id,priceentry.item.id,priceentry.price price";
            Object priceC = this.getModel().getValue("nckd_pricescontrol");
            //价格管控
            if (this.getModel().getValue("nckd_pricescontrol") == null || priceC.equals("")) {
                DynamicObject saleOrg = (DynamicObject) this.getModel().getValue("saleorg");
                Object saleOrgId = saleOrg.getPkValue();
                //过滤信息 先找本组织的价格
                QFilter qWhole = this.getFilter(billDate, itemId, unitId, saleOrgId);
                QFilter[] filters = new QFilter[]{qWhole};
                DynamicObjectCollection groupBrandDs = QueryServiceHelper.query(number, fieldkey, filters, "auditdate desc");
                if (groupBrandDs.size() > 0) {
                    BigDecimal prices = groupBrandDs.get(0).getBigDecimal("price");
                    this.getModel().setValue("lowestprice", prices, index);
                } else {
                    //过滤信息 再找集团的价格
                    qWhole = this.getFilter(billDate, itemId, unitId, 100000);
                    filters = new QFilter[]{qWhole};
                    groupBrandDs = QueryServiceHelper.query(number, fieldkey, filters, "auditdate desc");
                    if (groupBrandDs.size() > 0) {
                        BigDecimal prices = groupBrandDs.get(0).getBigDecimal("price");
                        this.getModel().setValue("lowestprice", prices, index);
                    }
                }
            } else {
                String pricesType = this.getModel().getValue("nckd_pricescontrol").toString();
                //子公司
                if (pricesType.equals("B")) {
                    //过滤信息 再找集团的价格
                    QFilter qWhole = this.getFilter(billDate, itemId, unitId, 100000);
                    QFilter[] filters = new QFilter[]{qWhole};
                    DynamicObjectCollection groupBrandDs = QueryServiceHelper.query(number, fieldkey, filters, "auditdate desc");
                    if (groupBrandDs.size() > 0) {
                        BigDecimal prices = groupBrandDs.get(0).getBigDecimal("price");
                        this.getModel().setValue("lowestprice", prices, index);
                    }
                }
            }


        } else if (propName.equals("nckd_pricescontrol")) {
            //价格管控
            String pricesType = e.getChangeSet()[0].getNewValue().toString();
            Date billDate = DateTime.now().toDate();
            //表单标识
            String number = "ocdbd_pricepolicy";
            //查询字段
            String fieldkey = "id,number,priceentry.id,priceentry.item.id,priceentry.price price";
            int rows = this.getModel().getEntryRowCount("priceentry");
            if (pricesType.equals("B")) {

                for (int i = 0; i < rows; i++) {
                    DynamicObject item = (DynamicObject) this.getModel().getValue("item", i);
                    if (item == null) continue;
                    //商品Id
                    Object itemId = item.getPkValue();
                    DynamicObject unit = (DynamicObject) this.getModel().getValue("unit", i);
                    if (unit == null) continue;
                    //单位Id
                    Object unitId = unit.getPkValue();
                    //商品
                    QFilter qWhole = this.getFilter(billDate, itemId, unitId, 100000);
                    QFilter[] filters = new QFilter[]{qWhole};
                    DynamicObjectCollection groupBrandDs = QueryServiceHelper.query(number, fieldkey, filters, "auditdate desc");
                    if (groupBrandDs.size() > 0) {
                        BigDecimal prices = groupBrandDs.get(0).getBigDecimal("price");
                        this.getModel().setValue("lowestprice", prices, i);
                    }

                }
            } else if (pricesType.equals("")) {
                DynamicObject saleOrg = (DynamicObject) this.getModel().getValue("saleorg");
                Object saleOrgId = saleOrg.getPkValue();
                for (int i = 0; i < rows; i++) {
                    DynamicObject item = (DynamicObject) this.getModel().getValue("item", i);
                    if (item == null) continue;
                    //商品Id
                    Object itemId = item.getPkValue();
                    DynamicObject unit = (DynamicObject) this.getModel().getValue("unit", i);
                    if (unit == null) continue;
                    //单位Id
                    Object unitId = unit.getPkValue();
                    //商品
                    QFilter qWhole = this.getFilter(billDate, itemId, unitId, saleOrgId);
                    QFilter[] filters = new QFilter[]{qWhole};
                    DynamicObjectCollection groupBrandDs = QueryServiceHelper.query(number, fieldkey, filters, "auditdate desc");
                    if (groupBrandDs.size() > 0) {
                        BigDecimal prices = groupBrandDs.get(0).getBigDecimal("price");
                        this.getModel().setValue("lowestprice", prices, i);
                    } else {
                        //过滤信息 再找集团的价格
                        qWhole = this.getFilter(billDate, itemId, unitId, 100000);
                        filters = new QFilter[]{qWhole};
                        groupBrandDs = QueryServiceHelper.query(number, fieldkey, filters, "auditdate desc");
                        if (groupBrandDs.size() > 0) {
                            BigDecimal prices = groupBrandDs.get(0).getBigDecimal("price");
                            this.getModel().setValue("lowestprice", prices, i);
                        }
                    }

                }
            } else {
                for (int i = 0; i < rows; i++) {
                    this.getModel().setValue("lowestprice", 0, i);
                }
            }

        }

    }

    private QFilter getFilter(Date billDate, Object itemId, Object unitId, Object saleOrgId) {
        //已审核
        QFilter qWhole = new QFilter("status", QCP.equals, 'C');
        //已启用
        QFilter qEnable = new QFilter("enable", QCP.equals, '1');
        qEnable.and("priceentry.entryenable", "=", '1');
        qWhole.and(qEnable);
        //生效时间<业务时间<失效时间
        QFilter qTime = QFilter.of("(begindate <= ? and enddate > ?)", billDate, billDate);
        qWhole.and(qTime);
        //商品失效时间
        QFilter qTimeEntry = QFilter.of("(priceentry.begindt <= ? and priceentry.enddt > ?)", billDate, billDate);
        qWhole.and(qTimeEntry);
        //销售组织
        QFilter qOrg = new QFilter("saleorg.id", QCP.equals, saleOrgId);
        qWhole.and(qOrg);
        //商品
        QFilter qItem = new QFilter("priceentry.item.id", QCP.equals, itemId);
        qWhole.and(qItem);
        //单位
        QFilter qUint = new QFilter("priceentry.unit.id", QCP.equals, unitId);
        qWhole.and(qUint);
        return qWhole;
    }
}
