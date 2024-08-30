package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;
import cn.hutool.core.date.DateUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 业绩统计-报表界面插件
 * 表单标识：nckd_hmua_sfa_perform_ext
 * author:zzl
 * date:2024/08/30
 */
public class PerformanceExtFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Map<String, BigDecimal> orderComplete = null;
        try {
            orderComplete = this.getOrderComplete(rowData);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (orderComplete.isEmpty()) {
            return;
        }
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //获取用户编码
            String hmuaFlowUser = ((DynamicObject) next.get("hmua_flow_user")).getString("number");
            String hmuaTeam = ((DynamicObject) next.get("hmua_team")).getPkValue().toString();
            if (orderComplete.containsKey(hmuaFlowUser+hmuaTeam)) {
                BigDecimal complete = orderComplete.get(hmuaFlowUser + hmuaTeam);
                //给完成情况-已完成赋值
                next.set("PL-LJ-0001t",new DecimalFormat("0.00").format(complete));
                BigDecimal target = new BigDecimal(String.valueOf(next.get("hmua_target")));
                target = complete.divide(target,BigDecimal.ROUND_CEILING);
                //给完成情况-完成率赋值
                next.set("PL-LJ-0001f",new DecimalFormat("0.00%").format(target));
            }
        }


    }

    public Map<String, BigDecimal> getOrderComplete(DynamicObjectCollection rowData) throws ParseException {
        Iterator<DynamicObject> iterator = rowData.iterator();
        Map<String, BigDecimal> orderComplete = new HashMap<>();
        ArrayList<String> hmuaKeyUser = new ArrayList<>();
        ArrayList<Long> hmuaKeyTeam = new ArrayList<>();
        String hmuaYear = null;
        String hmuaMonth = null;
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //获取用户编码
            hmuaKeyUser.add((String) ((DynamicObject)next.get("hmua_flow_user")).getString("number"));
            hmuaKeyTeam.add((Long) ((DynamicObject)next.get("hmua_team")).getPkValue());
            if(hmuaYear == null){
                hmuaYear = next.getString("hmua_year_field");
            }
            if(hmuaMonth == null){
                hmuaMonth = next.getString("hmua_month_field");
            }
        }

        if (hmuaYear == null) {
            return orderComplete;
        }
        String yearMonth = hmuaYear + "-" + hmuaMonth;
        Date begin = DateUtil.beginOfMonth(new SimpleDateFormat("yyyy-MM").parse(yearMonth));
        Date end = DateUtil.endOfMonth(new SimpleDateFormat("yyyy-MM").parse(yearMonth));
//        查询条件根据取出的年月进行过来
        QFilter qFilter = new QFilter("orderdate", QCP.large_equals , begin).and("orderdate", QCP.less_equals , end);
        if(!hmuaKeyUser.isEmpty()){
//            业务员编码与用户编码进行匹配
            qFilter.and("nckd_salerid.operatornumber",QCP.in,hmuaKeyUser);
        }
        if(!hmuaKeyTeam.isEmpty()){
//            销售部门与团队进行匹配
            qFilter.and("departmentid",QCP.in,hmuaKeyTeam);
        }

        //取要货订单业务员，
        String sFields = "nckd_salerid.operatornumber as nckd_salerid , " +
                //销售部门，
                "departmentid , " +
                //累计出库基本数量 - 累计退货基本数量 = 订单实际出库数量，
                "itementry.totaloutstockbaseqty - itementry.totalreturnbaseqty as complete";
        DataSet saleOrder = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "ocbsoc_saleorder", sFields, new QFilter[]{qFilter},null)
                .groupBy(new String[]{"nckd_salerid","departmentid"}).sum("complete").finish();


        if (saleOrder.isEmpty()) {
            return orderComplete;
        }
        while (saleOrder.hasNext()) {
            Row row = saleOrder.next();
            String key = row.getString("nckd_salerid") + row.getLong("departmentid").toString();
            BigDecimal complete = row.getBigDecimal("complete");
            orderComplete.put(key,complete);
        }
        return orderComplete;
    }
}