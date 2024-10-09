package nckd.yanye.occ.plugin.task;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Module           :系统服务云-调度中心-调度执行程序
 * Description      :定时更新精品客户和渠道档案
 * @author : wgq
 * @date : 2024/8/23
 */
public class SetVipChannelCustomerTask extends AbstractTask {
    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        Set orderchannelidList = new HashSet();
        Set customeridList = new HashSet();
        //查找要货订单单笔超过2000元的订货渠道和客户
        QFilter qFilter = new QFilter("sumtaxamount", QCP.large_equals, 2000);
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
