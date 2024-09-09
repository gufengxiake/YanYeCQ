package nckd.yanye.hr.report.shebao;

import cn.hutool.core.date.DateUtil;
import kd.bos.algo.*;
import kd.bos.algo.input.CollectionInput;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.util.CollectionUtils;

import java.util.*;

/**
 * 工时假勤云-日常考勤-出差申请单
 * 报表标识：nckd_attendancereport
 * 考勤异常报表插件
 * @author yaosijie
 * @since 2024-09-04
 */
public class AttendanceReportPlugin extends AbstractReportListDataPlugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) {
        boolean flag = false;
        Collection<Object[]> coll = new ArrayList<>();
        String[] fields = new String[]{"key1"};
        DataType[] dataTypes = new DataType[]{DataType.StringType};
        RowMeta rowMeta = RowMetaFactory.createRowMeta(fields,dataTypes);
        CollectionInput inputs = new CollectionInput(rowMeta, coll);
        DataSet returnDataSet = Algo.create(this.getClass().getName()).createDataSet(inputs);
        //考勤管理组织
        List<FilterItemInfo> orgFilters =  reportQueryParam.getFilter().getFilterItems("nckd_orgfield");
        //异常分类
        List<FilterItemInfo> exceptiontypeFilters =  reportQueryParam.getFilter().getFilterItems("nckd_exceptiontype");
        //自定义日期(开始日期)
        List<FilterItemInfo> startDateFilters =  reportQueryParam.getFilter().getFilterItems("startdate");
        //自定义日期(结束日期)
        List<FilterItemInfo> endDateFilters =  reportQueryParam.getFilter().getFilterItems("enddate");
        //异常日期
        List<FilterItemInfo> exceptiondateFilters =  reportQueryParam.getFilter().getFilterItems("nckd_exceptiondate");
        Object orgId = orgFilters.get(0).getValue() != null ? ((DynamicObject) orgFilters.get(0).getValue()).getPkValue() : null;
        String exceptiontype = exceptiontypeFilters.get(0).getValue() != null ? String.valueOf(exceptiontypeFilters.get(0).getValue()) : null;
        Date startDate = startDateFilters.get(0).getValue() != null ? (Date) startDateFilters.get(0).getValue() : null;
        Date endDate = endDateFilters.get(0).getValue() != null ? (Date) endDateFilters.get(0).getValue() : null;
        /**
         * 本月	A
         * 本周	B
         * 上月	C
         * 自定义 D
         */
        if (CollectionUtils.isNotEmpty(exceptiondateFilters)){
            switch ((String)exceptiondateFilters.get(0).getValue()){
                case "A":
                    //获取当月的开始结束日期
                    startDate = DateUtil.beginOfMonth(DateUtil.date());
                    endDate = DateUtil.endOfMonth(DateUtil.date());
                    break;
                case "B":
                    startDate = DateUtil.beginOfWeek(new Date());
                    endDate = DateUtil.endOfWeek(new Date());
                    break;
                case "C":
                    startDate = DateUtil.beginOfMonth(DateUtil.offsetMonth(new Date(), -1));
                    endDate = DateUtil.endOfMonth(DateUtil.offsetMonth(new Date(), -1));
                    break;
            }
        }

        //异常记录
//        DataSet wtteexrecord = Algo.create(this.getClass().getName()).createDataSet(inputs);;
        if (exceptiontype.indexOf("4") != -1){
            QFilter wtteexrecordQFilter = new QFilter("org.id", QCP.equals, orgId);
            if (startDate != null && endDate != null) {
                wtteexrecordQFilter.and("shiftdate", QCP.large_equals, startDate)
                        .and("shiftdate", QCP.less_equals, endDate);
            }
            String wtteexrecordSql = "'' billno,personid.name nckd_name,attfileid.personnum nckd_number,org.name nckd_orgname," +
                    "companyvid.name nckd_companyname,adminorgvid.name nckd_adminorgname,shiftdate nckd_startdate," +
                    "shiftdate nckd_enddate,4 nckd_excepclassify,null nckd_signaturedate," +
                    "attitemvalue nckd_exceptionduration,attitemvid.unit nckd_unit,exattributeid.name nckd_exceptype";
            DataSet wtteexrecord = QueryServiceHelper.queryDataSet(this.getClass().getName(), "wtte_exrecord", wtteexrecordSql, wtteexrecordQFilter.toArray(), null);
            wtteexrecord = wtteexrecord.select("billno,nckd_name,nckd_number,nckd_orgname," +
                    "nckd_companyname,nckd_adminorgname,nckd_startdate," +
                    "nckd_enddate,nckd_excepclassify,nckd_signaturedate," +
                    "CAST(nckd_exceptionduration AS STRING) nckd_exceptionduration,nckd_unit,nckd_exceptype");
            returnDataSet = wtteexrecord;
            flag = true;
        }

        // 为他人申请补签wtpm_supsigninfo
//        DataSet wtpmsupsignpcquery = Algo.create(this.getClass().getName()).createDataSet(inputs);;
        if (exceptiontype.indexOf("3") != -1){
            QFilter wtpmsupsignpcqueryQFilter = new QFilter("org.id", QCP.equals, orgId).and("billstatus",QCP.equals,"C");;
            if (startDate != null && endDate != null) {
                wtpmsupsignpcqueryQFilter.and("entryentity.signdate", QCP.large_equals, startDate)
                        .and("entryentity.signdate", QCP.less_equals, endDate);
            }
            String wtpmsupsignpcquerySql = "'' billno,personid.name nckd_name,attfile.personnum nckd_number,org.name nckd_orgname," +
                    "attfilebasef7.company.name nckd_companyname,attfilebasef7.adminorg.name nckd_adminorgname,entryentity.signdate nckd_startdate," +
                    "entryentity.signdate nckd_enddate,3 nckd_excepclassify,entryentity.suppleworktime nckd_signaturedate," +
                    "'' nckd_exceptionduration,'W' nckd_unit,entryentity.applyreason.name nckd_exceptype";
            DataSet wtpmsupsignpcquery = QueryServiceHelper.queryDataSet(this.getClass().getName(), "wtpm_supsignpc", wtpmsupsignpcquerySql, wtpmsupsignpcqueryQFilter.toArray(), null);
            wtpmsupsignpcquery = wtpmsupsignpcquery.select("billno,nckd_name,nckd_number,nckd_orgname," +
                    "nckd_companyname,nckd_adminorgname,nckd_startdate," +
                    "nckd_enddate,nckd_excepclassify,nckd_signaturedate," +
                    "CAST(nckd_exceptionduration AS STRING) nckd_exceptionduration,nckd_unit,nckd_exceptype");
            if (flag){
                returnDataSet = returnDataSet.union(wtpmsupsignpcquery);
            }else {
                returnDataSet = wtpmsupsignpcquery;
                flag = true;
            }
        }

        // 部门休假申请
        if (exceptiontype.indexOf("1") != -1){
            QFilter wtabmvaapplyQFilter = new QFilter("org.id", QCP.equals, orgId).and("billstatus",QCP.equals,"C");
            if (startDate != null && endDate != null) {
                wtabmvaapplyQFilter
                        .and(new QFilter("startdate", QCP.large_equals, startDate).and("startdate", QCP.less_equals, endDate)
                                .or(new QFilter("enddate", QCP.large_equals, startDate).and("enddate", QCP.less_equals, endDate)));
            }
            String wtabmvaapplySql = "billno,personid.name nckd_name,attfile.personnum nckd_number,org.name nckd_orgname," +
                    "attfilebasef7.company.name nckd_companyname,attfilebasef7.adminorg.name nckd_adminorgname,startdate nckd_startdate," +
                    "enddate nckd_enddate,1 nckd_excepclassify,entryentity.entryvacationtype.id nckd_exceptypeid,null nckd_signaturedate," +
                    "applytime nckd_exceptionduration,unit nckd_unit";
            //查询休假类型
            DataSet regionDataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), "wtbd_vacationtype",
                    "id,name nckd_exceptype,number", null, null);

            DataSet wtabmvaapplyquery = QueryServiceHelper.queryDataSet(this.getClass().getName(), "wtabm_vaapply", wtabmvaapplySql, wtabmvaapplyQFilter.toArray(), null);

            // join连接2个数据
            DataSet resultDataSet = wtabmvaapplyquery.copy().leftJoin(regionDataSet).on("nckd_exceptypeid", "id")
                    .select(wtabmvaapplyquery.getRowMeta().getFieldNames(), new String[]{"nckd_exceptype"}).finish();
            GroupbyDataSet groupbyDataSet = resultDataSet.groupBy(new String[]{"billno"});
            resultDataSet = groupbyDataSet.groupConcat("nckd_exceptype",null,",").finish();
            DataSet dataSet = wtabmvaapplyquery.copy().groupBy(new String[]{"billno","nckd_name","nckd_number","nckd_orgname","nckd_companyname","nckd_adminorgname"
                    ,"nckd_startdate","nckd_enddate","nckd_excepclassify",
                    "nckd_signaturedate","nckd_exceptionduration","nckd_unit"}).finish();;

            DataSet vaapplyDataSet = dataSet.leftJoin(resultDataSet).on("billno", "billno")
                    .select(dataSet.getRowMeta().getFieldNames(), new String[]{"nckd_exceptype"}).finish();
            vaapplyDataSet = vaapplyDataSet.select("billno,nckd_name,nckd_number,nckd_orgname," +
                    "nckd_companyname,nckd_adminorgname,nckd_startdate," +
                    "nckd_enddate,nckd_excepclassify,nckd_signaturedate," +
                    "CAST(nckd_exceptionduration AS STRING) nckd_exceptionduration,nckd_unit,nckd_exceptype");

            if (flag){
                returnDataSet = returnDataSet.union(vaapplyDataSet);
            }else {
                returnDataSet = vaapplyDataSet;
                flag = true;
            }
        }

        // 部门出差申请
        if (Objects.equals("2",exceptiontype)){
            QFilter wtambusitripbillQFilter = new QFilter("org.id", QCP.equals, orgId).and("billstatus",QCP.equals,"C");;
            if (startDate != null && endDate != null) {
                wtambusitripbillQFilter
                        .and(new QFilter("sdate", QCP.large_equals, startDate).and("sdate", QCP.less_equals, endDate)
                                .or(new QFilter("edate", QCP.large_equals, startDate).and("edate", QCP.less_equals, endDate)));
            }
            String wtambusitripbillSql = "billno,personid.name nckd_name,attfile.personnum nckd_number,org.name nckd_orgname," +
                    "attfilebasef7.company.name nckd_companyname,attfilebasef7.adminorg.name nckd_adminorgname,sdate nckd_startdate," +
                    "edate nckd_enddate,2 nckd_excepclassify,entryentity.busitriptype.id nckd_exceptypeid,null nckd_signaturedate," +
                    "sumtriptime nckd_exceptionduration,sumunit nckd_unit";
            //查询出差类型
            DataSet businessDataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), "wtbd_traveltype",
                    "id,name nckd_exceptype,number", null, null);

            DataSet wtambusitripbillquery = QueryServiceHelper.queryDataSet(this.getClass().getName(), "wtam_busitripbill", wtambusitripbillSql, wtambusitripbillQFilter.toArray(), null);

            // join连接2个数据
            DataSet wtamburesultDataSet = wtambusitripbillquery.copy().leftJoin(businessDataSet).on("nckd_exceptypeid", "id")
                    .select(wtambusitripbillquery.getRowMeta().getFieldNames(), new String[]{"nckd_exceptype"}).finish();
            GroupbyDataSet wtambugroupbyDataSet = wtamburesultDataSet.groupBy(new String[]{"billno"});
            wtamburesultDataSet = wtambugroupbyDataSet.groupConcat("nckd_exceptype",null,",").finish();
            DataSet wtambudataSet = wtambusitripbillquery.copy().groupBy(new String[]{"billno","nckd_name","nckd_number","nckd_orgname","nckd_companyname","nckd_adminorgname"
                    ,"nckd_startdate","nckd_enddate","nckd_excepclassify",
                    "nckd_signaturedate","nckd_exceptionduration","nckd_unit"}).finish();

            DataSet rewtambudataSet = wtambudataSet.leftJoin(wtamburesultDataSet).on("billno", "billno")
                    .select(wtambudataSet.getRowMeta().getFieldNames(), new String[]{"nckd_exceptype"}).finish();
            rewtambudataSet = rewtambudataSet.select("billno,nckd_name,nckd_number,nckd_orgname," +
                    "nckd_companyname,nckd_adminorgname,nckd_startdate," +
                    "nckd_enddate,nckd_excepclassify,nckd_signaturedate," +
                    "CAST(nckd_exceptionduration AS STRING) nckd_exceptionduration,nckd_unit,nckd_exceptype");

            if (flag){
                returnDataSet = returnDataSet.union(rewtambudataSet);
            }else {
                returnDataSet = rewtambudataSet;
            }
        }
        return returnDataSet;
    }
}
