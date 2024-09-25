package nckd.yanye.fi.plugin.report;

import kd.bos.algo.*;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.AbstractReportListDataPluginExt;
import kd.bos.entity.report.FilterInfo;
import kd.bos.event.AfterQueryEvent;
import kd.bos.orm.ORM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * Module           :财务云-总账-凭证汇总
 * Description      :凭证汇总表报表插件
 * nckd_gl_vouchersummar_ext
 * @author : yaosijie
 * @date : 2024/9/25
 */
public class VoucherSummaryQueryRptExt extends AbstractReportListDataPluginExt {

    @Override
    public void afterQuery(AfterQueryEvent event) {
        // 获取标品报表数据结果集
        DataSet dataSet = event.getDataSet();
        // 获取表单过滤条件参数对象
        FilterInfo filter = event.getReportQueryParam().getFilter();
        String currency = (String) filter.getValue("currency");// 币别
        dataSet =dataSet.addField("name","nckd_accountname");
        dataSet =dataSet.addField("number","nckd_accountnumber");

        Field[] rowFields = dataSet.getRowMeta().getFields();
        //dataSet转换为DynamicObjectCollection,通过DynamicObjectCollection去塞值
        DynamicObjectCollection dynamicObjectCollection = ORM.create().toPlainDynamicObjectCollection(dataSet);
        for (DynamicObject dynamicObject : dynamicObjectCollection){
            dynamicObject.set("nckd_accountname",dynamicObject.getString("name"));
            dynamicObject.set("nckd_accountnumber",dynamicObject.getString("number"));
        }
        if (currency.equals("allcurrency")) {
            Map<Long, List<DynamicObject>> map = dynamicObjectCollection.stream().collect(Collectors.groupingBy(t->t.getLong("number")));
            List<Long> longList = new ArrayList<>();
            for (Map.Entry<Long,List<DynamicObject>> entry : map.entrySet()){
                //判断是否为多币种的数据
                if (entry.getValue().stream().filter(t-> !"0".equals(t.getString("currencyid"))).map(t->t.getLong("currencyid")).collect(Collectors.toSet()).size() > 1){
                    longList.add(entry.getKey());
                }
                //合计数据
                if ("0".equals(entry.getKey().toString())){
                    longList.add(entry.getKey());
                }
            }
            DynamicObjectCollection resultCollection = new DynamicObjectCollection();
            resultCollection.addAll(dynamicObjectCollection.stream().filter(t->longList.contains(t.getLong("number")) || "0".equals(t.getString("currencyid"))).collect(Collectors.toList()));
            DataSet retuenDataSet = buildDataByObjCollection("algoKey", rowFields, resultCollection);
            event.setDataSet(retuenDataSet);
        }else {
            DataSet retuenDataSet = buildDataByObjCollection("algoKey", rowFields, dynamicObjectCollection);
            event.setDataSet(retuenDataSet);
        }
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
}
