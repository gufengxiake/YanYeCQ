package nckd.yanye.fi.plugin.report;

import dm.jdbc.util.StringUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.CollectionUtils;
import kd.fi.fa.report.query.FaCardListQuery;

import java.util.*;
import java.util.stream.Collectors;

public class AssetListPlugin extends FaCardListQuery {

    private Map<String,String> map = new HashMap<>(6);

    public Map<String, String> getMap() {
        map.put("01","安全生产设备");
        map.put("02","环保设备");
        map.put("03","电子专用设备");
        map.put("04","检测设备");
        map.put("05","专用仪器仪表");
        map.put("06","非专用设备");
        return map;
    }

    @Override
    public DataSet query(ReportQueryParam queryParam, Set<String> otherRealFields) {
        Map<String,String> map = getMap();
//        String[] array = {"number","realcard"};
        String[] array = {"fid", "assetbook", "assetcat", "assetcat.number", "number", "realcard", "finaccountdate", "depremethod", "originalval", "preusingamount", "depredamount", "accumdepre", "preresidualval", "networth", "decval", "netamount", "summarytype", "incometax", "depreusename", "currency", "assetbookcurperiod"};
        List<FilterItemInfo> filterItemInfos = queryParam.getFilter().getTableHeadFilterItems();
        List<String> valueList = new ArrayList<>();
        DataSet c = super.query(queryParam, otherRealFields);
        if (CollectionUtils.isNotEmpty(filterItemInfos)){
            for (Map.Entry<String,String> entry : map.entrySet()){
                if ("LIKE".equals(filterItemInfos.get(0).getCompareType()) && entry.getValue().indexOf(filterItemInfos.get(0).getValue().toString()) != -1){
                    valueList.add(entry.getKey());
                }else if ("NOT LIKE".equals(filterItemInfos.get(0).getCompareType()) && entry.getValue().indexOf(filterItemInfos.get(0).getValue().toString()) == -1){
                    valueList.add(entry.getKey());
                }else if ("=".equals(filterItemInfos.get(0).getCompareType()) && entry.getValue().equals(filterItemInfos.get(0).getValue().toString())){
                    valueList.add(entry.getKey());
                }else if ("<>".equals(filterItemInfos.get(0).getCompareType()) && !entry.getValue().equals(filterItemInfos.get(0).getValue().toString())){
                    valueList.add(entry.getKey());
                }else if ("lIKE".equals(filterItemInfos.get(0).getCompareType()) && entry.getValue().startsWith(filterItemInfos.get(0).getValue().toString())){
                    valueList.add(entry.getKey());
                }else if ("like".equals(filterItemInfos.get(0).getCompareType()) && entry.getValue().endsWith(filterItemInfos.get(0).getValue().toString())){
                    valueList.add(entry.getKey());
                }else if ("NOTISNULL".equals(filterItemInfos.get(0).getCompareType()) && StringUtil.isNotEmpty(entry.getValue())){
                    valueList.add(entry.getKey());
                }
            }
            QFilter qFilter = new QFilter("nckd_specialequipmen",QCP.in,valueList);
            if ("ISNULL".equals(filterItemInfos.get(0).getCompareType())){
                qFilter = new QFilter("nckd_specialequipmen",QCP.equals," ");
            }
            DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("fa_card_real_base", "number", qFilter.toArray());
            //构造查询参数
            String query = Arrays.asList(purcontractArr).stream().map(t->t.getString("number")).collect(Collectors.joining("','"));
            DataSet c1 = c.filter("number in ('"+query+"')").groupBy(array).finish();
            return c1;
        }
        return c;
    }

}
