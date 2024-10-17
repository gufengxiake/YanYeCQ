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
        DataSet im = this.getImTransDirBill("im_transdirbill_BT","'借货'")
                .union(this.getImTransDirBill("im_transdirbill_BT2","'借货归还'"))
                .union(this.getImSaleOutBill());

        //获取过滤条件
        List<FilterItemInfo> filters = reportQueryParam.getFilter().getFilterItems();
        Long nckdForgQ = null;
        String nckdBiztimeQStart = null;
        String nckdBiztimeQEnd = null;
        Long nckdYwyQ = null;
        Long nckdMaterielQ = null;
        for (FilterItemInfo filterItem : filters) {
            switch (filterItem.getPropName()) {
                // 查询条件库存组织,标识如不一致,请修改
                case "nckd_forg_q":
                    nckdForgQ = (filterItem.getValue() == null) ? null
                            : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
                // 查询条件单据日期,标识如不一致,请修改
                case "nckd_biztime_q_start":
                    nckdBiztimeQStart  = (filterItem.getDate() == null) ? null
                            : new SimpleDateFormat("yyyy-MM-dd").format(filterItem.getDate());
                    break;
                case "nckd_biztime_q_end":
                    nckdBiztimeQEnd  = (filterItem.getDate() == null) ? null
                            : new SimpleDateFormat("yyyy-MM-dd").format(filterItem.getDate());
                    break;
                // 查询条件业务员,标识如不一致,请修改
                case "nckd_ywy_q":
                    nckdYwyQ = (filterItem.getValue() == null) ? null
                             : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();

                    break;
                // 查询条件物料,标识如不一致,请修改
                case "nckd_materiel_q":
                    nckdMaterielQ = (filterItem.getValue() == null) ? null
                            : (Long) ((DynamicObject) filterItem.getValue()).getPkValue();
                    break;
            }
        }
        if (im.isEmpty()) {
            return im;
        }
        if (nckdForgQ != null){
            im = im.filter("nckd_forg = " + nckdForgQ);
        }
        if (nckdBiztimeQStart != null && nckdBiztimeQEnd != null){
            im = im.filter("fbiztime >=  to_date('" +  nckdBiztimeQStart + "','yyyy-MM-dd')"  )
                    .filter("fbiztime <= to_date('" +  nckdBiztimeQEnd + "','yyyy-MM-dd')" );
        }
        if (nckdYwyQ != null){
            im = im.filter("nckd_ywy = " + nckdYwyQ);

        }
        if (nckdMaterielQ != null){
            im = im.filter("nckd_material = " + nckdMaterielQ);
        }


        //根据组织，业务员，物料，批号，单位汇总不同单据类型的数量
        im = im.groupBy(new String[]{"nckd_forg","nckd_ywy","nckd_material","nckd_lotnum","nckd_unit"})
                .sum("CASE WHEN btname = '借货' THEN fqty ELSE 0 END ","nckd_jhqty")
                .sum("CASE WHEN btname = '车销出库' THEN fqty ELSE 0 END ","nckd_xsqty")
                .sum("CASE WHEN btname = '借货归还' THEN fqty ELSE 0 END ","nckd_jchhqty")
                .finish();

        return im.orderBy(new String[]{"nckd_forg","nckd_material"});
    }

    /**
     * 获取直接调拨单
     * @param billTypeNumber
     * @param billTypeName
     * @return
     */
    public DataSet getImTransDirBill(String billTypeNumber,String billTypeName){
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
//        QFilter qFilter = new QFilter("billtype", QCP.in,new Long[]{1980435141796826112L,1980435041267748864L});
        QFilter qFilter = new QFilter("billtype", QCP.equals,this.getBillTypeId(billTypeNumber));
        qFilter.and("billstatus", QCP.equals ,"C");
        return QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_transdirbill", selectFields, new QFilter[]{qFilter},null).addField(billTypeName,"btname");
    }

    /**
     * 获取销售出库单
     * @return
     */
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
//        QFilter qFilter = new QFilter("billtype", QCP.equals,1980511903113284608L);
        QFilter qFilter = new QFilter("billtype", QCP.equals,this.getBillTypeId("im_saloutbill_BT1"));
        qFilter.and("billstatus", QCP.equals ,"C");
        return QueryServiceHelper.queryDataSet(this.getClass().getName(), "im_saloutbill", selectFields, new QFilter[]{qFilter},null).addField("'车销出库'","btname");
    }

    /**
     * 根据单据类型编码获取单据类型主键
     * @param billTypeNumber
     * @return
     */
    public Long getBillTypeId(String billTypeNumber){
        DynamicObject dynamicObject = QueryServiceHelper.queryOne("bos_billtype", "id", new QFilter[]{new QFilter("number", QCP.equals, billTypeNumber)});
        if (dynamicObject == null) {
            return 0L;
        }
        return dynamicObject.getLong("id");
    }

}