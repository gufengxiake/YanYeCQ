package nckd.yanye.hr.report.shebao;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.ReportQueryParam;
import kd.wtc.wtte.report.RecordDayListReport;

import java.util.Iterator;
import java.util.Map;

/**
 * Module           :工时假勤云-考勤核算-考勤记录-日报表
 * Description      : 日报表二开扩展插件
 * 标识 wtte_dailydetailslist
 *
 *
 */

public class RecordDayListDataReport extends RecordDayListReport {

    @Override
    public DataSet query(ReportQueryParam queryParam, Object select) {


        DataSet queryDataSet = super.query(queryParam, select);
        // todo 获取到标品的查询内容，然后添加字段内容
        DataSet copy = queryDataSet.copy();

        return queryDataSet;
    }


}
