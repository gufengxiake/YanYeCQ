package nckd.yanye.occ.plugin.report;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;

/**
 * 发出商品报表-报表界面插件
 * 表单标识：nckd_sendoutproduct_rpt
 * author:zhangzhilong
 * date:2024/09/18
 */
public class SendOutProductReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
        for (DynamicObject next : rowData) {
            if (next.getBigDecimal("out_signbaseqty") == null
                    || next.getBigDecimal("out_signbaseqty").compareTo(BigDecimal.ZERO) == 0) {
                //签收数量为空则用基本数量赋值
                next.set("out_signbaseqty", next.getBigDecimal("out_baseqty"));
            }
            //获取行政区划的长地址中的省份
            DynamicObject outAdmindivision = next.getDynamicObject("out_admindivision");
            if (outAdmindivision == null) {
                continue;
            }
            String fullname = outAdmindivision.getString("fullname");
            String[] s = fullname.split("_");
            if (s.length > 0) {
                next.set("admindivision" ,s[0]);
            }

        }
    }
}