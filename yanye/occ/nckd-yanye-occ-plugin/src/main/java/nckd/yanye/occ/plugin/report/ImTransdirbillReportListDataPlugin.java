package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
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
 * 业务员借货汇总表表取数插件
 * 表单标识：nckd_ywyjhhz_rpt
 * author:zhangzhilong
 * date:2024/08/21
 *  */

public class ImTransdirbillReportListDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
        //联合直接调拨单和销售出库单的数据
        DataSet im = this.getImTransDirBill().union(this.getImSaleOutBill());

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
        if (im.isEmpty()) {
            return im;
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


        //根据组织，业务员，物料，批号，单位汇总不同单据类型的数量
        im = im.groupBy(new String[]{"nckd_forg","nckd_ywy","nckd_material","nckd_lotnum","nckd_unit"})
                .sum("CASE WHEN fbilltypeid = 1980435041267748864L THEN fqty ELSE 0 END ","nckd_jhqty")
                .sum("CASE WHEN fbilltypeid = 1980511903113284608L THEN fqty ELSE 0 END ","nckd_xsqty")
                .sum("CASE WHEN fbilltypeid = 1980435141796826112L THEN fqty ELSE 0 END ","nckd_jchhqty")
                .finish();

        return im;
    }

    //获取直接调拨单
    public DataSet getImTransDirBill(){
        //库存组织
        String selectFields = "outorg AS nckd_forg," +
                //业务员
                "nckd_ywy AS nckd_ywy," +
                //物料编码
                "billentry.material.masterid AS nckd_material," +
                //批号
                "billentry.lotnumber as nckd_lotnum," +
                //单位
                "billentry.unit AS nckd_unit," +
                //数量
                "billentry.qty AS fqty," +
                //单据类型
                "billtype AS fbilltypeid," +
                //业务日期
                "biztime as fbiztime";
//        默认查询借出和借出还回的直接调拨单
        QFilter qFilter = new QFilter("billtype", QCP.in,new Long[]{1980435141796826112L,1980435041267748864L});
        qFilter.and("billstatus", QCP.equals ,"C");
        DataSet im_transdirbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_transdirbill", selectFields, new QFilter[]{qFilter},null);
        return im_transdirbill;
    }

    //获取销售出库单
    public DataSet getImSaleOutBill(){
                //库存组织
        String selectFields = "org AS nckd_forg," +
                //业务员
                "bizoperator AS nckd_ywy," +
                //物料编码
                "billentry.material.masterid AS nckd_material," +
                //批号
                "billentry.lotnumber as nckd_lotnum," +
                //单位
                "billentry.unit AS nckd_unit," +
                //数量
                "billentry.qty AS fqty," +
                //单据类型
                "billtype AS fbilltypeid," +
                //业务日期
                "biztime as fbiztime";
//        默认查询车销出库单据类型和已审核的销售出库单
        QFilter qFilter = new QFilter("billtype", QCP.equals,1980511903113284608L);
        qFilter.and("billstatus", QCP.equals ,"C");
        DataSet im_saloutbill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "im_saloutbill", selectFields, new QFilter[]{qFilter},null);
        return im_saloutbill;
    }

}