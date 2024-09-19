package nckd.yanye.occ.plugin.task;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicObjectType;
import kd.bos.exception.KDException;
import kd.bos.metadata.form.FormMetadata;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.occ.plugin.report.DataSetToList;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SetVipChannelCustomerTaask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        Set orderchannelidList = new HashSet();
        Set customeridList = new HashSet();
        //查找要货订单单笔超过2000元的订货渠道和客户
        QFilter qFilter = new QFilter("sumtax", QCP.large_equals, 2000);
        DynamicObjectCollection collections = QueryServiceHelper.query("ocbsoc_saleorder",
                "id,orderchannelid.id  orderchannelid,customerid.id customerid", qFilter.toArray(), "");
        if (!collections.isEmpty()) {
            for (DynamicObject saleData : collections) {
                Object orderchannelid = saleData.get("orderchannelid");
                orderchannelidList.add(orderchannelid);
                Object customerid = saleData.get("customerid");
                customeridList.add(customerid);
            }
        }
        //查找要货订单数量大于等于6次的记录
        LocalDate firstDayOfYear = LocalDate.now().withDayOfYear(1);
        QFilter dateFilter = new QFilter("orderdate", QCP.large_equals, firstDayOfYear);
        DataSet orderCollections = QueryServiceHelper.queryDataSet("DATE", "ocbsoc_saleorder",
                "id,orderchannelid.id  orderchannelid,customerid.id customerid", dateFilter.toArray(), null);
        DataSet countSql = orderCollections.groupBy(new String[]{"orderchannelid", "customerid"}).count("count").finish();
        while (countSql.hasNext()) {
            Row countItem = countSql.next();
            int count = countItem.getInteger("count");
            if (count >= 6) {
                Object orderchannelid = countItem.get("orderchannelid");
                orderchannelidList.add(orderchannelid);
                Object customerid = countItem.get("customerid");
                customeridList.add(customerid);
            }
        }
        if (!orderchannelidList.isEmpty()) {
            DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("ocdbd_channel");
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(orderchannelidList.toArray(), newDynamicObject.getDynamicObjectType());
            for(DynamicObject obj:dynamicObjects){
                //精品
                obj.set("nckd_boutiquecustomer",true);
            }
            SaveServiceHelper.update(dynamicObjects);
        }
        if (!customeridList.isEmpty()) {
            DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_customer");
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(customeridList.toArray(), newDynamicObject.getDynamicObjectType());
            for(DynamicObject obj:dynamicObjects){
                //精品
                obj.set("nckd_boutiquecustomer",true);
            }
            SaveServiceHelper.update(dynamicObjects);
        }


    }
}
