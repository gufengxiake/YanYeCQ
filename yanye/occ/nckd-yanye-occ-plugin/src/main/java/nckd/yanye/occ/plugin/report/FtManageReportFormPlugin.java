package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.util.Iterator;

/**
 * 销售管理报表-报表界面插件
 * 表单标识：nckd_salemanage_rpt
 * author:zhangzhilong
 * date:2024/08/29
 */
public class FtManageReportFormPlugin extends AbstractReportFormPlugin implements Plugin {

    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给组织默认值
        filter.addFilterItem("nckd_org_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //运价
            BigDecimal nckdPricefieldyf1 = next.getBigDecimal("nckd_pricefieldyf1") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_pricefieldyf1");
            //客户签收数量
            BigDecimal nckdSignqty = next.getBigDecimal("nckd_signqty") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_signqty");
           //计算价税合计
            BigDecimal multiply = nckdSignqty.multiply(nckdPricefieldyf1);
            next.set("nckd_e_pricetaxtotal",multiply);
            //组织名称需要改变
            if (next.getString("nckd_bizorg").equals("晶昊本部")){
                next.set("nckd_bizorg","江西晶昊盐化有限公司");
            }
            //途损数小于0的直接清空
            if(next.getBigDecimal("nckd_damageqty").compareTo(BigDecimal.ZERO) <= 0){
                next.set("nckd_damageqty",BigDecimal.ZERO);
            }
            //签收数量为空则赋值为基本数量
            if(next.getBigDecimal("nckd_signqty").compareTo(BigDecimal.ZERO) == 0){
                next.set("nckd_signqty",next.getBigDecimal("nckd_quantity"));

            }
        }

    }
}