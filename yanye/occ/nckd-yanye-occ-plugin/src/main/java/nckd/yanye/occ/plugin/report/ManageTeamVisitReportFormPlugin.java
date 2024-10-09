package nckd.yanye.occ.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.text.DecimalFormat;
import java.util.Iterator;

/**
 * 经营团队情况一览表（拜访）-报表界面插件
 * 表单标识：nckd_manageteamvisit_rpt
 * author:zhangzhilong
 * date:2024/09/14
 */
public class ManageTeamVisitReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            //登记客商数
            double ocdbdSumks = next.getBigDecimal("ocdbd_sumks").doubleValue();
            //当月拜访
            double sumbf = next.getBigDecimal("sumbf").doubleValue();
            //月度拜访目标
            double yearHkYdbfkh = next.getBigDecimal("year_hk_ydbfkh").doubleValue();
            //计算零拜访客户数 = 登记客商数 - 当月拜访
            next.set("lbfkhs",ocdbdSumks-sumbf);
            //计算月拜访覆盖率 = 当月拜访/登记客商数
            if(ocdbdSumks != 0){
                next.set("nckd_ybffgl",new DecimalFormat("0.00%").format(sumbf/ocdbdSumks));
            }
            //计算月拜访完成率 = 当月拜访/月度拜访目标
            if(yearHkYdbfkh != 0){
                next.set("nckd_ybfwcl",new DecimalFormat("0.00%").format(sumbf/yearHkYdbfkh));
            }
        }
    }
}