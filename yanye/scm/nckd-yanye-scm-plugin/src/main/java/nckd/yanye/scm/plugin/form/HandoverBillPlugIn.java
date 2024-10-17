package nckd.yanye.scm.plugin.form;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

/**
 * 生产交接单表单插件
 * 表单标识：nckd_im_mdc_handover
 * author:黄文波 2024-10-15
 */
public class HandoverBillPlugIn extends AbstractBillPlugIn {

    private final static String IM_INV_REALBALAN= "im_inv_realbalance";//即时库存余额表
    private final static String TQSD_IM_INV_REALBALAN_EXT_SEQ = "org,warehouse,location,ownertype,owner,invstatus,invtype,material,auxpty,lotnum,project,baseunit,unit,unit2nd,baseqty,"
            + ",qty,qty2nd";
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 工具栏注册监听（注意这里是把整个工具栏注册监听，工具栏项是没有运行时控件模型的）
        Toolbar toolbar = this.getControl("advcontoolbarap");
        if (toolbar != null) {
            // 注意itemClick和click的区别
            toolbar.addItemClickListener(this);
        }

    }


    // 注意itemClick和click的区别
    @Override
    public void itemClick(ItemClickEvent e) {
        super.itemClick(e);

        String itemKey = e.getItemKey();
        if ("nckd_updatekcsl".equalsIgnoreCase(itemKey)) {

            DynamicObjectCollection entryentities =this.getModel().getDataEntity().getDynamicObjectCollection("entryentity");//循环单据体每一行
            for (DynamicObject entryobj: entryentities) {
                DynamicObject material = entryobj.getDynamicObject("nckd_material");//物料
                DynamicObject warehouse = entryobj.getDynamicObject("nckd_warehouse");//仓库
                DynamicObject org = entryobj.getDynamicObject("nckd_orgfield");//组织

                String stocktype = "110";//库存类型
                Long materialid=(long) material.getPkValue();
                Long stockid = (long) warehouse.getPkValue();//仓库内码
                Long orgid = (long) org.getPkValue();//库存组织内码

                DynamicObject[] record= new DynamicObject[0];
                try {  record = lotnumberQuery(materialid, stocktype,stockid,orgid);
                } catch (Exception ex) { throw new RuntimeException(ex);  }

                BigDecimal lockqty= BigDecimal.valueOf(0);
                BigDecimal lockqtyhj= BigDecimal.valueOf(0);
                for (int j = 0; j < record.length; j++) {
                    DynamicObject recordobj=record[j];
                    lockqty = BigDecimal.valueOf(recordobj.getBigDecimal("qty").intValue());

                    lockqtyhj=lockqty.add(lockqtyhj);
                }
                entryobj.set("nckd_inventoryqty",lockqtyhj);
            }

            this.getView().updateView("entryentity");


        }




//        if ("nckd_test".equalsIgnoreCase(itemKey)) {
//
//            Set orderchannelidList = new HashSet();
//            Set customeridList = new HashSet();
//
////            LocalDate firstDayOfYear = LocalDate.now().withDayOfYear(1);
////            LocalDate yesterday = LocalDate.now().minusDays(1);
////            QFilter dateFilter = new QFilter("nckd_date", QCP.equals, yesterday);
//
//            QFilter Filterywzt = new QFilter("nckd_statu", QCP.equals, "A");//业务状态为未完成
//
//
//            DataSet orderCollections = QueryServiceHelper.queryDataSet("CGSYSSH", "nckd_eleweighing",
//                    "id,nckd_waterid,nckd_bucket,nckd_qygys.id nckd_qygys,nckd_date", Filterywzt.toArray(), "nckd_qygys,nckd_date");
//            DataSet countSql = orderCollections.groupBy(new String[]{"nckd_date", "nckd_qygys"}).count("count").finish();
//            while (countSql.hasNext()) {
//                Row countItem = countSql.next();
//                int count = countItem.getInteger("count");
//                if (count >= 0) {
//                    //业务日期只取年月，然后比较上次是否一致，一致不加到list中
//
//
//
//
//                    Object customerid = countItem.get("nckd_qygys");
//                    customeridList.add(customerid);
//                }
//            }
//
//            if (!customeridList.isEmpty()) {
//                DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_supplier");
//                DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(customeridList.toArray(), newDynamicObject.getDynamicObjectType());
//                for(DynamicObject obj:dynamicObjects){
//
//                    //2、循环供应商找到业务日期当月的供应商对应的采购订单。
//
//
//                    QFilter qFilterbeg = new QFilter("biztime", QCP.large_equals , "2024-10-01");
//                    QFilter qFilterend = new QFilter("biztime", QCP.less_equals , "2024-10-01");
//
//
//
//                    QFilter qFiltergys = new QFilter("supplier", QCP.less_equals , obj.getPkValue());
//
//
//                    DynamicObjectCollection collections = QueryServiceHelper.query("pm_purorderbill",
//                            "id,orderchannelid.id  orderchannelid,customerid.id customerid", qFilter.toArray(), "");
//                    if (!collections.isEmpty()) {
//                        for (DynamicObject saleData : collections) {
//                            Object orderchannelid = saleData.get("orderchannelid");
//                            orderchannelidList.add(orderchannelid);
//                            Object customerid = saleData.get("customerid");
//                            customeridList.add(customerid);
//                        }
//                    }
//
//
//                    //精品
//                    obj.set("nckd_boutiquecustomer",true);
//                }
//                SaveServiceHelper.update(dynamicObjects);
//            }
//
//
//        }
    }
    public DynamicObject[] lotnumberQuery(Long number,String stocktype,Long stockid,Long orgid) throws Exception {
        QFilter filter4 = new QFilter("org.id", QCP.equals, orgid);//库存组织
        QFilter filter5 = new QFilter("warehouse.id", QCP.equals, stockid);//仓库
        QFilter filter = new QFilter("material.id", QCP.equals, number);// 物料
        QFilter filter1 = new QFilter("invtype.number", QCP.equals, stocktype);//库存类型
        QFilter filter2 = new QFilter("baseqty", QCP.large_than, 0);//数量大于0
        QFilter filter3 = new QFilter("invstatus.number", QCP.equals, "110");//库存状态为可用
        filter.and(filter1).and(filter3).and(filter2).and(filter4).and(filter5);
        DynamicObject[] recordDynamicObjectList = BusinessDataServiceHelper.load(IM_INV_REALBALAN,TQSD_IM_INV_REALBALAN_EXT_SEQ, filter.toArray());// 获取批号集合
        return recordDynamicObjectList;

    }
}