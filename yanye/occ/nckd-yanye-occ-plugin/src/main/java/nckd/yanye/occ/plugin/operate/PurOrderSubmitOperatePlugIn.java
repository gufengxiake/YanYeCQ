package nckd.yanye.occ.plugin.operate;

import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

public class PurOrderSubmitOperatePlugIn extends AbstractOperationServicePlugIn {


    /**
     * 操作执行前，准备加载单据数据之前，触发此事件
     * 插件可以在此事件中，指定需要加载的字段
     */
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("material");//物料
        e.getFieldKeys().add("qty");//数量
        e.getFieldKeys().add("biztime");//订单日期
        e.getFieldKeys().add("org");//组织
    }

    /**
     * 操作校验执行完毕，开启事务保存单据之前，触发此事件
     * 可以在此事件，对单据数据包进行整理、取消操作
     */
    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);

        DynamicObject[] entities = e.getDataEntities();
        // 逐单处理
        for (DynamicObject dataEntity : entities) {
            DynamicObject org = dataEntity.getDynamicObject("org");
            Object orgId = org.getPkValue();
            Date billDate = dataEntity.getDate("biztime");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(billDate);

            // 获取年月的第一天
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            SimpleDateFormat sdfFirstDay = new SimpleDateFormat("yyyy-MM-dd");
            String firstDayOfMonth = sdfFirstDay.format(calendar.getTime());

            // 获取年月的最后一天
            calendar.add(Calendar.MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 0);
            SimpleDateFormat sdfLastDay = new SimpleDateFormat("yyyy-MM-dd");
            String lastDayOfMonth = sdfLastDay.format(calendar.getTime());

            //物料明细
            DynamicObjectCollection billentry = dataEntity.getDynamicObjectCollection("billentry");
            if (!billentry.isEmpty()) {
                for (DynamicObject entryData : billentry) {
                    DynamicObject mat = entryData.getDynamicObject("material");
                    if (mat != null) {
                        //物料分组编码
                        Object groupNum = mat.getString("masterid.group.number");
                        BigDecimal qty = entryData.getBigDecimal("qty");
                        BigDecimal monthPlanQty=this.getMonthPlanQty(orgId,firstDayOfMonth,lastDayOfMonth,groupNum);
                        if(qty.compareTo(monthPlanQty)<0){
                            //
                        }
                    }
                }
            }

        }
    }

    private BigDecimal getMonthPlanQty(Object orgId, String startdate, String enddate, Object groupNum) {
        BigDecimal planQty = BigDecimal.ZERO;
        //查找当前物料分组的所有下级分组
        QFilter gFilter = new QFilter("longnumber", QCP.like, "%" + groupNum + "%");
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
                    .and("closestatus", QCP.equals, "A")//整单关闭
                    .and("billentry.rowclosestatus", QCP.equals, "A")//行关闭
                    //行中止
                    .and("billentry.rowterminatestatus", QCP.equals, "A");
            //查询统计数据
            DataSet billDBSet = QueryServiceHelper.queryDataSet("getPurOrderQty", billNumber, billfieldkey, new QFilter[]{billQFilter}, "auditdate desc");
            //设置group by
            GroupbyDataSet billgroupby = billDBSet.groupBy(new String[]{"orgid"});
            billgroupby = billgroupby.sum("qty");
            DataSet billgroupDb = billgroupby.finish();
            BigDecimal purQty = BigDecimal.valueOf(0);
            if (billgroupDb.hasNext()) {
                Row billItem = billgroupDb.next();
                purQty = purQty.add(billItem.getBigDecimal("qty"));
            }

            //采购订单已关闭或已终止  取采购订单的已入库数量
            String closeFieldkey = "org.id orgid,billentry.invqty qty";
            QFilter closeFilter = new QFilter("org.id", QCP.equals, orgId)
                    .and("billstatus", QCP.equals, "C")
                    .and("billentry.material.masterid.group.id", QCP.in, groupIds)
                    .and("biztime", QCP.large_equals, startdate)
                    .and("biztime", QCP.less_equals, enddate);
            QFilter orFilter = new QFilter("closestatus", QCP.equals, "B")
                    .or("billentry.rowclosestatus", QCP.equals, "B")
                    .or("billentry.rowterminatestatus", QCP.equals, "B");
            closeFilter = closeFilter.and(orFilter);
            DataSet closeBillDBSet = QueryServiceHelper.queryDataSet("getPurOrderQty1", billNumber, closeFieldkey, new QFilter[]{closeFilter}, "auditdate desc");
            //设置group by
            GroupbyDataSet closebillgroupby = closeBillDBSet.groupBy(new String[]{"orgid"});
            closebillgroupby = closebillgroupby.sum("qty");
            DataSet closeBillgroupDb = closebillgroupby.finish();
            if (closeBillgroupDb.hasNext()) {
                Row billItem = closeBillgroupDb.next();
                purQty = purQty.add(billItem.getBigDecimal("qty"));
            }
            //需求数量-已采购数量
            planQty=qty.subtract(purQty);
        }
        return planQty;
    }
}
