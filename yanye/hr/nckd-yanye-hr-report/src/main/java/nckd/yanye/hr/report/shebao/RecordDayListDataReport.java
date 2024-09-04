package nckd.yanye.hr.report.shebao;

import kd.bos.algo.*;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.report.*;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.CollectionUtils;
import kd.imsc.dmw.utils.DateUtils;
import kd.wtc.wtte.report.RecordDayListReport;

import java.util.*;

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
        List<FilterItemInfo> itemInfos = queryParam.getFilter().getFilterItems();
        Date startDate = new Date();
        Date endDate = new Date();
        //考勤期间
        if (itemInfos.get(1).getValue() != null){
            DynamicObject dynamicObject = (DynamicObject) itemInfos.get(1).getValue();
            startDate = dynamicObject.getDate("begindate");
            endDate = dynamicObject.getDate("enddate");
            //归属日期(开始时间，结束时间)
        }else if (itemInfos.get(2).getValue() != null && itemInfos.get(3).getValue() != null){
            startDate = (Date)itemInfos.get(2).getValue();
            endDate = (Date)itemInfos.get(3).getValue();
        }
        DataSet queryDataSet = super.query(queryParam, select);
        //赋值dateSet数据
        DataSet copyDataSet = queryDataSet.copy();
        Set<String> numList = new HashSet<>();
        Iterator<Row> iterator = copyDataSet.iterator();
        while (iterator.hasNext()){
            Row row = iterator.next();
            numList.add(row.getString("personid.number"));
        }
        //构造多次卡记录查询条件（attperson.number：工号，shiftdate：班次日期）
        QFilter qFilter = new QFilter("attperson.number", QCP.in,numList)
                .and("shiftdate",QCP.large_equals,startDate)
                .and("shiftdate",QCP.less_equals,endDate);
        //查询多次卡记录wtpm_multicard
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("wtpm_multicard", "id,attperson,shiftdate,attfilebo,entryentity.effectivepoint,entryentity.pointdesc", qFilter.toArray());

        //构造上班打卡时间map（key：工号+日期，value：上班有效卡点）
        Map<String,String> WcMap = new HashMap<>();
        //构造下班打卡时间map（key：工号+日期，value：下班有效卡点）
        Map<String,String> AcMap = new HashMap<>();
        Arrays.stream(dynamicObjects).forEach(t->{
            DynamicObjectCollection collection = t.getDynamicObjectCollection("entryentity");
            for (DynamicObject object : collection){
                String number = object.getString("pointdesc.number");
                if (object.getDate("effectivepoint") != null){
                    if (number.indexOf("WC") != -1){
                        WcMap.put(t.getString("attperson.number")+ DateUtils.format(t.getDate("shiftdate"),"yyyy-MM-dd")
                                ,DateUtils.format(object.getDate("effectivepoint"),"yyyy-MM-dd HH:mm:ss"));
                    }else if (number.indexOf("AC") != -1){
                        AcMap.put(t.getString("attperson.number")+ DateUtils.format(t.getDate("shiftdate"),"yyyy-MM-dd")
                                ,DateUtils.format(object.getDate("effectivepoint"),"yyyy-MM-dd HH:mm:ss"));
                    }
                }
            }
        });
        //DateSet 数据新增 打卡起始时间、打卡结束时间
        DataSet dataSet = queryDataSet.addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"startTime")
                .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"endTime");//打卡起始时、打卡结束时间
        //dataSet 转换为DynamicObjectCollection
        DynamicObjectCollection dynamicObjectCollection = ORM.create().toPlainDynamicObjectCollection(dataSet);
        if (CollectionUtils.isNotEmpty(dynamicObjectCollection)){
            dynamicObjectCollection.forEach(t->{
                if (t.getDate("owndate") != null){
                    //打卡起始时间、打卡结束时间 重新赋值
                    t.set("startTime",WcMap.get(t.getString("personid.number") + DateUtils.format(t.getDate("owndate"),"yyyy-MM-dd")));
                    t.set("endTime",AcMap.get(t.getString("personid.number") + DateUtils.format(t.getDate("owndate"),"yyyy-MM-dd") ));
                }
            });
        }
        Field[] rowFields = dataSet.getRowMeta().getFields();
        //DynamicObjectCollection 转换为 DataSet
        DataSet retuenDataSet = buildDataByObjCollection("algoKey", rowFields, dynamicObjectCollection);
        return retuenDataSet;
    }

    //DynamicObjectCollection 转换为 DataSet 方法
    public DataSet buildDataByObjCollection(String algoKey, Field[] rowFields, DynamicObjectCollection objCollection) {
        DataSetBuilder dataSetBuilder = Algo.create(algoKey + ".emptyFields")
                .createDataSetBuilder(new RowMeta(rowFields));
        for (DynamicObject arObj : objCollection) {
            Object[] rowData = new Object[rowFields.length];
            for (int i = 0; i < rowFields.length; i++) {
                Field field = rowFields[i];
                rowData[i] = arObj.get(field.getName());
            }
            dataSetBuilder.append(rowData);
        }
        return dataSetBuilder.build();
    }

    /**
     * 报表列增加打卡起始时间、打卡结束时间
     * @param columns
     * @return
     * @throws Throwable
     */
    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        ReportColumn startTimeColumn = new ReportColumn();
        ColumnStyle style = new ColumnStyle();
        style.setTextAlign("center");
        startTimeColumn.setStyle(style);
        startTimeColumn.setFieldType("text");
        startTimeColumn.setFieldKey("startTime");
        LocaleString startlocaleString = new LocaleString("打卡起始时间");
        startTimeColumn.setCaption(startlocaleString);

        ReportColumn endTimeColumn = new ReportColumn();
        endTimeColumn.setStyle(style);
        endTimeColumn.setFieldType("text");
        endTimeColumn.setFieldKey("endTime");
        LocaleString endTimelocaleString = new LocaleString("打卡结束时间");
        endTimeColumn.setCaption(endTimelocaleString);

        List<AbstractReportColumn> columnList = super.getColumns(columns);
        columnList.add(startTimeColumn);
        columnList.add(endTimeColumn);
        return columnList;
    }
}
