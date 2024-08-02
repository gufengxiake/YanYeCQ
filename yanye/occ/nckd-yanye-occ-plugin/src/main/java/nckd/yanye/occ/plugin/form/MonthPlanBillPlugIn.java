package nckd.yanye.occ.plugin.form;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

public class MonthPlanBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        // 获取当前页面的FormShowParameter对象
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        // 获取自定义参数
        Object orgId = formShowParameter.getCustomParam("orgId");
        Object date = formShowParameter.getCustomParam("date");
        //分组ID
        JSONArray groupId = formShowParameter.getCustomParam("groupIds");
        BigInt[] idArray = new BigInt[groupId.size()];
        for (int i = 0; i < groupId.size(); i++) {
            idArray[i] = BigInt.javaBigInteger2bigInt(groupId.getBigInteger(i));
        }
        //分组编码
        JSONArray groupNum = formShowParameter.getCustomParam("groupNum");
        String[] numArray = new String[groupNum.size()];
        for (int i = 0; i < groupNum.size(); i++) {
            numArray[i] = groupNum.getString(i);
        }

        // 把参数值赋值到页面过滤字段上
        this.getModel().setValue("nckd_orgld", orgId);//需求组织
        this.getModel().setValue("nckd_date", date);//需求月份
        this.getModel().setValue("nckd_mulmatgroup", idArray);//物料分类
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
            this.queryMonthPlan(orgId, firstDayOfMonth, lastDayOfMonth, numArray);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }


    }

    //查询
    private void queryMonthPlan(Object orgId, Date startdate, Date enddate, String[] groupNums) {
        if (groupNums.length > 0) {
            this.getModel().deleteEntryData("nckd_entryentity");
            int row = 0;
            for (String groupNum : groupNums) {
                //查找当前物料分组的所有下级分组
                QFilter gFilter = new QFilter("longnumber", QCP.like, "%"+groupNum+"%");
                DynamicObjectCollection groupColle = QueryServiceHelper.query("bd_materialgroup", "id", gFilter.toArray(), "");
                HashSet<Object> groupIds = new HashSet<>();
                if (!groupColle.isEmpty()) {
                    for (DynamicObject group : groupColle) {
                        Object id = group.get("id");
                        groupIds.add(id);
                    }
                    //表单标识
                    String number = "nckd_pm_monthpurapply";//月度需求申请单
                    //查询字段
                    String fieldkey = "org.id orgid,entryentity.nckd_qty qty";
                    //过滤条件
                    QFilter qFilter = new QFilter("org.id", QCP.equals, orgId)
                            .and("billstatus", QCP.equals, "C")
                            .and("entryentity.nckd_matgroup.id", QCP.in, groupIds)
                            .and("nckd_needdate", QCP.large_equals, startdate)
                            .and("nckd_needdate", QCP.less_equals, enddate);
                    //查询统计数据
                    DataSet DBSet = QueryServiceHelper.queryDataSet("getGroupPlanQty", number, fieldkey, new QFilter[]{qFilter}, "auditdate desc");
                    //设置group by
                    GroupbyDataSet groupby = DBSet.groupBy(new String[]{"orgid"});
                    groupby = groupby.sum("qty");
                    DataSet groupDb = groupby.finish();
                    BigDecimal qty = BigDecimal.valueOf(0);
                    if (groupDb.hasNext()) {
                        Row monItem = groupDb.next();
                        qty = monItem.getBigDecimal("qty");
                    }


                    //查询采购订单已采购数量
                    //表单标识
                    String billNumber = "pm_purorderbill";//采购订单
                    //查询字段
                    String billfieldkey = "org.id orgid,billentry.qty qty";
                    //过滤条件  采购订单未关闭未终止  取采购订单的采购数量
                    QFilter billQFilter = new QFilter("org.id", QCP.equals, orgId)
                            .and("billstatus", QCP.equals, "C")
                            .and("billentry.material.masterid.group.id", QCP.in, groupIds)
                            .and("biztime", QCP.large_equals, startdate)
                            .and("biztime", QCP.less_equals, enddate)
                            .and("closestatus",QCP.equals,"A")//整单关闭
                            .and("billentry.rowclosestatus",QCP.equals,"A")//行关闭
                            //行中止
                            .and("billentry.rowterminatestatus",QCP.equals,"A");
                    //查询统计数据
                    DataSet billDBSet = QueryServiceHelper.queryDataSet("getPurOrderQty", billNumber, billfieldkey, new QFilter[]{billQFilter}, "auditdate desc");
                    //设置group by
                    GroupbyDataSet billgroupby = billDBSet.groupBy(new String[]{"orgid"});
                    billgroupby = billgroupby.sum("qty");
                    DataSet billgroupDb = billgroupby.finish();
                    BigDecimal purQty = BigDecimal.valueOf(0);
                    if (billgroupDb.hasNext()) {
                        Row billItem=billgroupDb.next();
                        purQty=purQty.add(billItem.getBigDecimal("qty")) ;
                    }

                    //采购订单已关闭或已终止  取采购订单的已入库数量
                    String closeFieldkey="org.id orgid,billentry.invqty qty";
                    QFilter closeFilter=new QFilter("org.id", QCP.equals, orgId)
                            .and("billstatus", QCP.equals, "C")
                            .and("billentry.material.masterid.group.id", QCP.in, groupIds)
                            .and("biztime", QCP.large_equals, startdate)
                            .and("biztime", QCP.less_equals, enddate);
                    QFilter orFilter=new QFilter("closestatus",QCP.equals,"B")
                            .or("billentry.rowclosestatus",QCP.equals,"B")
                            .or("billentry.rowterminatestatus",QCP.equals,"B");
                    closeFilter=closeFilter.and(orFilter);
                    DataSet closeBillDBSet = QueryServiceHelper.queryDataSet("getPurOrderQty1", billNumber, closeFieldkey, new QFilter[]{closeFilter}, "auditdate desc");
                    //设置group by
                    GroupbyDataSet closebillgroupby = closeBillDBSet.groupBy(new String[]{"orgid"});
                    closebillgroupby = closebillgroupby.sum("qty");
                    DataSet closeBillgroupDb = closebillgroupby.finish();
                    if(closeBillgroupDb.hasNext()){
                        Row billItem=closeBillgroupDb.next();
                        purQty=purQty.add(billItem.getBigDecimal("qty"));
                    }

                    this.getModel().createNewEntryRow("nckd_entryentity");
                    this.getModel().setValue("nckd_entrydate", startdate, row);
                    this.getModel().setValue("nckd_entryorg", orgId, row);
                    this.getModel().setItemValueByNumber("nckd_entrymatgroup", groupNum, row);
                    this.getModel().setValue("nckd_needqty", qty, row);
                    this.getModel().setValue("nckd_purqty", purQty, row);
                    row++;

                }
            }
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
        //
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
            String[] stringArray = new String[groupIds.size()];
            for (int i = 0; i < groupIds.size(); i++) {
                DynamicObject item = groupIds.get(i);
                DynamicObject groupobj = item.getDynamicObject("fbasedataid");
                String number = groupobj.getString("number");
                stringArray[i] = number;
            }

            this.queryMonthPlan(orgId, firstDayOfMonth, lastDayOfMonth, stringArray);
        }
    }

}
