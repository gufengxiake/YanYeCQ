package nckd.yanye.occ.plugin.form;


import com.alibaba.fastjson.JSONArray;
import com.alipay.api.domain.Datas;
import com.google.type.Decimal;
import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.algo.Row;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import scala.math.BigInt;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class monthPlanBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        // 获取当前页面的FormShowParameter对象
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        // 获取自定义参数
        Object orgId = formShowParameter.getCustomParam("orgId");
        Object date = formShowParameter.getCustomParam("date");
        JSONArray groupId = formShowParameter.getCustomParam("groupId");
        BigInt[] stringArray = new BigInt[groupId.size()];
        for (int i = 0; i < groupId.size(); i++) {
            stringArray[i] = BigInt.javaBigInteger2bigInt(groupId.getBigInteger(i));
        }
        // 把参数值赋值到页面过滤字段上
        this.getModel().setValue("nckd_orgld", orgId);//需求组织
        this.getModel().setValue("nckd_date", date);//需求月份
        this.getModel().setValue("nckd_mulmatgroup", stringArray);//物料分类
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date1 = sdf.parse(date.toString());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date1);

            // 获取当月第一天
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date firstDayOfMonth = calendar.getTime();

            // 获取当月最后一天
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date lastDayOfMonth = calendar.getTime();
            this.queryMonthPlan(orgId, firstDayOfMonth, lastDayOfMonth, stringArray);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }


    }

    //查询
    private void queryMonthPlan(Object orgId, Date startdate, Date enddate, BigInt[] groupIds) {
        //表单标识
        String number = "nckd_pm_monthpurapply";//月度需求申请单
        //查询字段
        String fieldkey = "org.id orgid,entryentity.nckd_matgroup.id groupid,entryentity.nckd_qty qty";
        //过滤条件
        QFilter qFilter = new QFilter("org.id", QCP.equals, orgId)
                .and("billstatus", QCP.equals, "C")
                .and("entryentity.nckd_matgroup.id", QCP.in, groupIds)
                .and("nckd_needdate", QCP.large_equals, startdate)
                .and("nckd_needdate", QCP.less_equals, enddate);
        //查询统计数据
        DataSet DBSet = QueryServiceHelper.queryDataSet("getGroupPlanQty", number, fieldkey, new QFilter[]{qFilter}, "auditdate desc");
        //设置group by
        GroupbyDataSet groupby = DBSet.groupBy(new String[]{"orgid", "groupid"});
        groupby = groupby.sum("qty");
        DataSet groupDb = groupby.finish();

        //查询采购订单已采购数量
        //表单标识
        String billNumber = "pm_purorderbill";//采购订单
        //查询字段
        String billfieldkey = "org.id orgid,billentry.material.masterid.group.id groupid,billentry.qty qty";
        //过滤条件
        QFilter billQFilter = new QFilter("org.id", QCP.equals, orgId)
                .and("billstatus", QCP.equals, "C")
                .and("billentry.material.masterid.group.id", QCP.in, groupIds)
                .and("biztime", QCP.large_equals, startdate)
                .and("biztime", QCP.less_equals, enddate);
        //查询统计数据
        DataSet billDBSet = QueryServiceHelper.queryDataSet("getPurOrderQty", billNumber, billfieldkey, new QFilter[]{billQFilter}, "auditdate desc");
        //设置group by
        GroupbyDataSet billgroupby = billDBSet.groupBy(new String[]{"orgid", "groupid"});
        billgroupby = billgroupby.sum("qty");
        DataSet billgroupDb = billgroupby.finish();
        Map<String, BigDecimal> purQtyMap = new HashMap<>();
        for (Row item : billgroupDb) {
            String groupId = item.getString("groupid");
            BigDecimal qty = item.getBigDecimal("qty");
            purQtyMap.put(groupId, qty);
        }

        int row = 0;
        this.getModel().deleteEntryData("nckd_entryentity");
        for (Row item : groupDb) {
            this.getModel().createNewEntryRow("nckd_entryentity");
            String groupId = item.getString("groupid");
            BigDecimal qty = item.getBigDecimal("qty");
            BigDecimal purQty = BigDecimal.valueOf(0);
            if (purQtyMap.containsKey(groupId)) {
                purQty = purQtyMap.get(groupId);
            }

            this.getModel().setValue("nckd_entrydate", startdate, row);
            this.getModel().setValue("nckd_entryorg", orgId, row);
            this.getModel().setValue("nckd_entrymatgroup", groupId, row);
            this.getModel().setValue("nckd_needqty", qty, row);
            this.getModel().setValue("nckd_purqty", purQty, row);
            row++;
        }


    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 注册按钮点击监听（注意itemClick和click的区别）
        Button button = this.getControl("btnok");
        button.addClickListener(this);

    }

    @Override
    public void click(EventObject e) {
        super.click(e);
        // 如果是确定按钮，则取到人员的数据，返回给父页面
        Control control = (Control) e.getSource();
        if ("btnok".equalsIgnoreCase(control.getKey())) {
            DynamicObject org = (DynamicObject) this.getModel().getValue("nckd_orgld");
            Object orgId = org.getPkValue();
            Date date = (Date) this.getModel().getValue("nckd_date");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // 获取当月第一天
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date firstDayOfMonth = calendar.getTime();

            // 获取当月最后一天
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date lastDayOfMonth = calendar.getTime();

            DynamicObjectCollection groupIds = (DynamicObjectCollection) this.getModel().getValue("nckd_mulmatgroup");
            BigInt[] stringArray = new BigInt[groupIds.size()];
            for (int i = 0; i < groupIds.size(); i++) {
                DynamicObject item = groupIds.get(i);
                DynamicObject groupobj = item.getDynamicObject("fbasedataid");
                BigInteger bigInt = BigInteger.valueOf((Long) groupobj.getPkValue());
                stringArray[i] = BigInt.javaBigInteger2bigInt(bigInt);
            }

            this.queryMonthPlan(orgId, firstDayOfMonth, lastDayOfMonth, stringArray);
        }
    }

}
