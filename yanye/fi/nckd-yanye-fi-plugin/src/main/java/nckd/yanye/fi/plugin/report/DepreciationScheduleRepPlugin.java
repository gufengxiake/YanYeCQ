package nckd.yanye.fi.plugin.report;

import cn.hutool.core.util.ObjectUtil;
import com.netflix.discovery.converters.Auto;
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
import kd.fi.fa.report.query.FaDepreciationListQuery;
import nckd.yanye.fi.common.AppflgConstant;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Module           :财务云-固定资产-报表
 * Description      :折旧表报表筛选插件
 *
 * @author : yaosijie
 * @date : 2024/8/2
 */
public class DepreciationScheduleRepPlugin extends FaDepreciationListQuery {

    @Override
    public DataSet query(ReportQueryParam queryParam, Object select) throws Throwable {
        DepreciationAllocationRepPlugin depreciation = new DepreciationAllocationRepPlugin();
        List<QFilter> filters = depreciation.assemble(queryParam);
        if (CollectionUtils.isNotEmpty(filters)){
            DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("fa_card_real_base", "number", filters.stream().toArray(QFilter[]::new));
            //构造查询参数
            List<String> query = Arrays.asList(purcontractArr).stream().map(t->t.getString("number")).collect(Collectors.toList());
            QFilter numberQFilter = new QFilter("number" , QCP.in , query);
            QFilter qFilter = queryParam.getFilter().getCommFilter().get("fa_card_fin");
            if (ObjectUtil.isNotNull(qFilter)){
                numberQFilter.and(qFilter);
            }
            queryParam.getFilter().getCommFilter().put("fa_card_fin",numberQFilter);
        }
        DataSet dataSet = super.query(queryParam, select);
        queryParam.getFilter().getCommFilter().clear();
        return dataSet;
    }

}