package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.algo.DataType;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.db.SqlBuilder;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 报表取数插件
 */
public class ImTransdirbillReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {
    String algoKey = "nckd.yanye.occ.plugin.report.ImTransdirbillReportListDataPlugin";
    DBRoute faRoute = DBRoute.of("scm");

    private static String [] FIELDS ={"nckd_forg","nckd_ywy","nckd_material","nckd_materialname",
            "nckd_materialmodelnum","nckd_unit","nckd_jhqty",
            "nckd_xsqty","nckd_jchhqty","nckd_jhyeqty"};
    private static DataType[] DATATYPES = {DataType.LongType, DataType.LongType,
            DataType.LongType,DataType.StringType,DataType.StringType,DataType.LongType,
            DataType.LongType, DataType.LongType,DataType.LongType, DataType.LongType
    };


    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        DataSet im = this.getImTransDirBill().union(this.getImSaleOutBill());

        //关联物料库存信息取物料masterid
        im = im.leftJoin(this.getMaterial(im)).on("material","fid")
                .select(new String[]{"nckd_forg","nckd_ywy","nckd_material","nckd_unit","fqty","fbilltypeid","fbiztime"})
                .finish();

        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        Long nckd_forg_q = null;
        String nckd_biztime_q_start = null;
        String nckd_biztime_q_end = null;
        Long nckd_ywy_q = null;
        Long nckd_materiel_q = null;
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件库存组织,标识如不一致,请修改
                case "nckd_forg_q":
                    nckd_forg_q = (filterItem.getValue() == null) ? null
                            : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "nckd_biztime_q_start":
                    nckd_biztime_q_start  = (filterItem.getDate() == null) ? null
                            : new SimpleDateFormat("yyyy-MM-dd").format(filterItem.getDate());
                    break;
                case "nckd_biztime_q_end":
                    nckd_biztime_q_end  = (filterItem.getDate() == null) ? null
                            : new SimpleDateFormat("yyyy-MM-dd").format(filterItem.getDate());
                    break;
                // 查询条件业务员,标识如不一致,请修改
                case "nckd_ywy_q":
                     nckd_ywy_q = (filterItem.getValue() == null) ? null
                             : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();

                    break;
                // 查询条件物料,标识如不一致,请修改
                case "nckd_materiel_q":
                    nckd_materiel_q = (filterItem.getValue() == null) ? null
                            : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
            }
        }
        if (nckd_forg_q != null){
            im = im.filter("nckd_forg = " + nckd_forg_q);
        }
        if (nckd_biztime_q_start != null && nckd_biztime_q_end != null){
            im = im.filter("fbiztime >=  to_date('" +  nckd_biztime_q_start + "','yyyy-MM-dd')"  )
                    .filter("fbiztime <= to_date('" +  nckd_biztime_q_end + "','yyyy-MM-dd')" );
        }
        if (nckd_ywy_q != null){
            im = im.filter("nckd_ywy = " + nckd_ywy_q);

        }
        if (nckd_materiel_q != null){
            im = im.filter("nckd_material = " + nckd_materiel_q);
        }


        im = im.groupBy(new String[]{"nckd_forg","nckd_ywy","nckd_material","nckd_unit"})
                .sum("CASE WHEN fbilltypeid = 1980435041267748864L THEN fqty ELSE 0 END ","nckd_jhqty")
                .sum("CASE WHEN fbilltypeid = 1980435141796826112L THEN fqty ELSE 0 END ","nckd_xsqty")
                .sum("CASE WHEN fbilltypeid = 1980511903113284608L THEN fqty ELSE 0 END ","nckd_jchhqty")
                .finish();

        im.select(new String[]{"nckd_forg","nckd_ywy","nckd_material","nckd_unit","nckd_jhqty","nckd_xsqty","nckd_jchhqty"});

        return im;
    }

    public DataSet getImTransDirBill(){
        String selectFields = "outorg AS nckd_forg,nckd_ywy AS nckd_ywy,billentry.material AS material," +
                "billentry.unit AS nckd_unit,billentry.qty AS fqty,billtype AS fbilltypeid,biztime as fbiztime";
        QFilter qFilter = new QFilter("billtype", QCP.in,new Long[]{1980435141796826112L,1980435041267748864L});
        DataSet im_transdirbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_transdirbill", selectFields, new QFilter[]{qFilter},null);
        return im_transdirbill;
    }

    public DataSet getImSaleOutBill(){
        String selectFields = "org AS nckd_forg,bizoperator AS nckd_ywy,billentry.material AS material," +
                "billentry.unit AS nckd_unit,billentry.qty AS fqty,billtype AS fbilltypeid,biztime as fbiztime";
        QFilter qFilter = new QFilter("billtype", QCP.equals,1980511903113284608L);
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", selectFields, new QFilter[]{qFilter},null);
        return im_saloutbill;
    }

    public DataSet getMaterial(DataSet im){
        DataSet copy = im.copy();
        List<Long> bill_materialId = new ArrayList<>();
        while (copy.hasNext()){
            Row row = copy.next();
            bill_materialId.add( row.getLong("material"));
        }
        QFilter qFilter = new QFilter("id", QCP.in,bill_materialId.toArray(new Long[0]));
        DataSet bd_materialinven = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "bd_materialinventoryinfo", "id AS fid,masterid as nckd_material", new QFilter[]{qFilter},null);
        return bd_materialinven;
    }


}