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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 重点盐种价格情况表-报表界面插件
 * 表单标识：nckd_keysaltprice_rpt
 * author:zhangzhilong
 * date:2024/09/05
 */
public class KeySaltPriceReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
    }
    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        QFilter materialFilter = new QFilter("nckd_iskey", QCP.equals, "1");
        DataSet bdMaterial = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "bd_material", "id,name", new QFilter[]{materialFilter}, null);

        Map<Long,String> material = new HashMap<>();
        while (bdMaterial.hasNext()) {
            Row next = bdMaterial.next();
            material.put(next.getLong("id"),next.getString("name"));
        }


        Iterator<DynamicObject> iterator = rowData.iterator();
        Long[] id = material.keySet().toArray(new Long[0]);
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            BigDecimal sumQty = BigDecimal.ZERO;
            BigDecimal sumAmount = BigDecimal.ZERO;
            for (int i = 0; i < material.size(); i++) {
                String materialName = String.valueOf(id[i]);
                //计算含税单价
                BigDecimal materialQty = BigDecimal.ZERO;
                BigDecimal materialAmount = BigDecimal.ZERO;
                if (next.containsProperty(materialName+"qty")) {
                    materialQty = next.getBigDecimal(materialName+"qty");
                    sumQty = sumQty.add(materialQty);
                }
                if (next.containsProperty(materialName+"amount")) {
                    materialAmount = next.getBigDecimal(materialName+"amount");
                    sumAmount = sumAmount.add(materialAmount);
                }
                if(materialQty.compareTo(BigDecimal.ZERO) == 0 || materialAmount.compareTo(BigDecimal.ZERO) == 0){
                    continue;
                }
                BigDecimal price = materialAmount.divide(materialQty, RoundingMode.CEILING);
                next.set(materialName+"amount",price);
            }
            next.set("sumqty",sumQty);
            if (next.getString("nckd_bizorgname").equals("小计间隔")){
                next.set("nckd_bizorgname","");
                next.set("nckd_qj","小计间隔");
            }

        }
        bdMaterial.close();
    }


}