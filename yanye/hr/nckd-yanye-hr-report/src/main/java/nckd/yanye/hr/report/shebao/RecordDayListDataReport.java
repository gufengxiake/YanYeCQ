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
import java.util.stream.Collectors;

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
        //构造原始卡记录查询条件
        QFilter filter = new QFilter("attfilebo.personnum", QCP.in,numList)
                .and("nckd_position",QCP.is_notnull,null).and("nckd_position",QCP.not_equals,"");
        //查询原始卡记录
        DynamicObject[] objects = BusinessDataServiceHelper.load("wtpd_signcard", "id,signpoint,attfilebo.personnum,nckd_position", filter.toArray());
        //原始卡记录按照key :工号+打卡时间，value：进出卡地点
        Map<String, String> locationMap = Arrays.stream(objects).collect(Collectors.toMap(k ->  k.getString("attfilebo.personnum")+DateUtils.format(k.getDate("signpoint"),"yyyy-MM-dd HH:mm:00"),
                v -> v.getString("nckd_position"),(key1,key2)->key1));

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
        //构造查询

        //DateSet 数据新增 打卡起始时间、打卡结束时间、起始打卡地点、结束打卡地点
        DataSet dataSet = queryDataSet.addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"startTime")
                .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"endTime")
                .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"startLocation")
                .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"endLocation");
        //dataSet 转换为DynamicObjectCollection
        DynamicObjectCollection dynamicObjectCollection = ORM.create().toPlainDynamicObjectCollection(dataSet);
        if (CollectionUtils.isNotEmpty(dynamicObjectCollection)){
            dynamicObjectCollection.forEach(t->{
                if (t.getDate("owndate") != null){
                    //打卡起始时间、打卡结束时间 重新赋值
                    t.set("startTime",WcMap.get(t.getString("personid.number") + DateUtils.format(t.getDate("owndate"),"yyyy-MM-dd")));
                    t.set("endTime",AcMap.get(t.getString("personid.number") + DateUtils.format(t.getDate("owndate"),"yyyy-MM-dd") ));
                    t.set("startLocation",locationMap.get(t.getString("personid.number") + t.getString("startTime")));
                    t.set("endLocation",locationMap.get(t.getString("personid.number") + t.getString("endTime")));
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

        ReportColumn startLocationColumn = new ReportColumn();
        style.setTextAlign("center");
        startLocationColumn.setStyle(style);
        startLocationColumn.setFieldType("text");
        startLocationColumn.setFieldKey("startLocation");
        LocaleString startLocationlocaleString = new LocaleString("起始打卡地点");
        startLocationColumn.setCaption(startLocationlocaleString);

        ReportColumn endLocationColumn = new ReportColumn();
        endLocationColumn.setStyle(style);
        endLocationColumn.setFieldType("text");
        endLocationColumn.setFieldKey("endLocation");
        LocaleString endLocationlocaleString = new LocaleString("结束打卡地点");
        endLocationColumn.setCaption(endLocationlocaleString);

        List<AbstractReportColumn> columnList = super.getColumns(columns);
        columnList.add(startTimeColumn);
        columnList.add(endTimeColumn);
        columnList.add(startLocationColumn);
        columnList.add(endLocationColumn);
        return columnList;
    }
}
