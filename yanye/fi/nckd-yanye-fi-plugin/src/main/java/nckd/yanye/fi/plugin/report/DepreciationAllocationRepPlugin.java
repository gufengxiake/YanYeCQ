package nckd.yanye.fi.plugin.report;

import cn.hutool.core.util.ObjectUtil;
import dm.jdbc.util.StringUtil;
import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.CollectionUtils;
import kd.fi.fa.report.query.FaAssignListQuery;
import nckd.yanye.fi.common.AppflgConstant;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Module           :财务云-固定资产-资产卡片
 * Description      :折旧分摊表报表筛选插件
 *
 * @author : yaosijie
 * @date : 2024/78/12
 */
public class DepreciationAllocationRepPlugin extends FaAssignListQuery {

    @Override
    public DataSet query(ReportQueryParam queryParam, Object selectedObj) throws Throwable {
        List<QFilter> filters = assemble(queryParam);
        if (CollectionUtils.isNotEmpty(filters)){
            DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("fa_card_real_base", "number", filters.stream().toArray(QFilter[]::new));
            //构造查询参数
            List<String> query = Arrays.asList(purcontractArr).stream().map(t->t.getString("number")).collect(Collectors.toList());
            QFilter numberQFilter = new QFilter("assentry.copyrealcard.number" , QCP.in , query);
            QFilter qFilter = queryParam.getFilter().getCommFilter().get("fa_depresplitdetail");
            if (ObjectUtil.isNotNull(qFilter)){
                numberQFilter.and(qFilter);
            }
            queryParam.getFilter().getCommFilter().put("fa_depresplitdetail",numberQFilter);
        }
        DataSet dataSet = super.query(queryParam, selectedObj);
        queryParam.getFilter().getCommFilter().clear();
        return dataSet;
    }

    public List<QFilter> assemble(ReportQueryParam queryParam){
        Map<String,String> map = AppflgConstant.getMap();
        Map<String,String> resdevdeviceMap = AppflgConstant.getResdevdeviceMap();
        List<FilterItemInfo> filterItemInfos = queryParam.getFilter().getTableHeadFilterItems();
        //
        List<String> valueList = new ArrayList<>();
        List<String> resdevdeviceList = new ArrayList<>();
        List<QFilter> filters = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filterItemInfos)){

            for (FilterItemInfo filterItemInfo : filterItemInfos){
                if ("nckd_specializedequipmen".equals(filterItemInfo.getPropName())){
                    for (Map.Entry<String,String> entry : map.entrySet()){
                        if ("LIKE".equals(filterItemInfo.getCompareType()) && entry.getValue().indexOf(filterItemInfo.getValue().toString()) != -1){
                            valueList.add(entry.getKey());
                        }else if ("NOT LIKE".equals(filterItemInfo.getCompareType()) && entry.getValue().indexOf(filterItemInfo.getValue().toString()) == -1){
                            valueList.add(entry.getKey());
                        }else if ("=".equals(filterItemInfo.getCompareType()) && entry.getValue().equals(filterItemInfo.getValue().toString())){
                            valueList.add(entry.getKey());
                        }else if ("<>".equals(filterItemInfo.getCompareType()) && !entry.getValue().equals(filterItemInfo.getValue().toString())){
                            valueList.add(entry.getKey());
                        }else if ("lIKE".equals(filterItemInfo.getCompareType()) && entry.getValue().startsWith(filterItemInfo.getValue().toString())){
                            valueList.add(entry.getKey());
                        }else if ("like".equals(filterItemInfo.getCompareType()) && entry.getValue().endsWith(filterItemInfo.getValue().toString())){
                            valueList.add(entry.getKey());
                        }else if ("NOTISNULL".equals(filterItemInfo.getCompareType()) && StringUtil.isNotEmpty(entry.getValue())){
                            valueList.add(entry.getKey());
                        }
                    }
                    QFilter qFilter = new QFilter("nckd_specialequipmen", QCP.in,valueList);
                    if ("ISNULL".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_specialequipmen",QCP.equals," ");
                    }
                    filters.add(qFilter);
                }else if ("nckd_resdevdevice".equals(filterItemInfo.getPropName())){
                    for (Map.Entry<String,String> entry : resdevdeviceMap.entrySet()){
                        if ("LIKE".equals(filterItemInfo.getCompareType()) && entry.getValue().indexOf(filterItemInfo.getValue().toString()) != -1){
                            resdevdeviceList.add(entry.getKey());
                        }else if ("NOT LIKE".equals(filterItemInfo.getCompareType()) && entry.getValue().indexOf(filterItemInfo.getValue().toString()) == -1){
                            resdevdeviceList.add(entry.getKey());
                        }else if ("=".equals(filterItemInfo.getCompareType()) && entry.getValue().equals(filterItemInfo.getValue().toString())){
                            resdevdeviceList.add(entry.getKey());
                        }else if ("<>".equals(filterItemInfo.getCompareType()) && !entry.getValue().equals(filterItemInfo.getValue().toString())){
                            resdevdeviceList.add(entry.getKey());
                        }else if ("lIKE".equals(filterItemInfo.getCompareType()) && entry.getValue().startsWith(filterItemInfo.getValue().toString())){
                            resdevdeviceList.add(entry.getKey());
                        }else if ("like".equals(filterItemInfo.getCompareType()) && entry.getValue().endsWith(filterItemInfo.getValue().toString())){
                            resdevdeviceList.add(entry.getKey());
                        }else if ("NOTISNULL".equals(filterItemInfo.getCompareType()) && StringUtil.isNotEmpty(entry.getValue())){
                            resdevdeviceList.add(entry.getKey());
                        }
                    }
                    QFilter qFilter = new QFilter("nckd_checkboxfield", QCP.in, resdevdeviceList);
                    if ("ISNULL".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_checkboxfield",QCP.equals," ");
                    }
                    filters.add(qFilter);

                }else if ("nckd_resdevduration".equals(filterItemInfo.getPropName()) && filterItemInfo.getValue() instanceof BigDecimal){
                    QFilter qFilter = new QFilter("nckd_decimalfield",QCP.like,filterItemInfo.getValue());
//                    if ("LIKE".equals(filterItemInfo.getCompareType()) && entry.getValue().indexOf(filterItemInfo.getValue().toString()) != -1){
//                        resdevdeviceList.add(entry.getKey());
//                    }else
                    if ("<".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_decimalfield",QCP.less_than,new BigDecimal(filterItemInfo.getValue().toString()));
                    }else if ("=".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_decimalfield",QCP.equals,new BigDecimal(filterItemInfo.getValue().toString()));
                    }else if ("<>".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_decimalfield",QCP.not_equals2,new BigDecimal(filterItemInfo.getValue().toString()));
                    }else if (">".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_decimalfield",QCP.large_than,new BigDecimal(filterItemInfo.getValue().toString()));
                    }else if ("<=".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_decimalfield",QCP.less_equals,new BigDecimal(filterItemInfo.getValue().toString()));
                    }else if (">=".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("nckd_decimalfield",QCP.large_equals,new BigDecimal(filterItemInfo.getValue().toString()));
                    }
                    filters.add(qFilter);
                }else if ("nckd_resdevproject".equals(filterItemInfo.getPropName())){
                    QFilter qFilter = new QFilter("name",QCP.like,"%"+filterItemInfo.getValue()+"%");
                    if ("NOT LIKE".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("name",QCP.not_like,"%"+filterItemInfo.getValue()+"%");
                    }else if ("=".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("name",QCP.equals,filterItemInfo.getValue());
                    }else if ("<>".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("name",QCP.not_equals,filterItemInfo.getValue());
                    }else if ("lIKE".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("name",QCP.ILIKE,filterItemInfo.getValue());
                    }else if ("like".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("name",QCP.NOT_ILIKE,filterItemInfo.getValue());
                    }else if ("NOTISNULL".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("name",QCP.is_notnull,filterItemInfo.getValue());
                    }else if ("ISNULL".equals(filterItemInfo.getCompareType())){
                        qFilter = new QFilter("name",QCP.is_null,null);
                    }
                    DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("bd_project", "id", qFilter.toArray());
                    List<Long> ids = Arrays.asList(purcontractArr).stream().map(t->t.getLong("id")).collect(Collectors.toList());
                    QFilter checkboxQFilter = new QFilter("nckd_basedatafield", QCP.in, ids);
                    filters.add(checkboxQFilter);
                }
            }
        }
        return filters;
    }
}
