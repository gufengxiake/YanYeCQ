package nckd.yanye.occ.plugin.report;

import cn.hutool.core.date.DateUtil;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Iterator;

/**
 * 客户交易情况表-报表界面插件
 * 表单标识：nckd_customertrade_rpt
 * author:zhangzhilong
 * date:2024/09/02
 */
public class CustomerTradeReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
            //数量
            BigDecimal nckdReqbaseqty = next.getBigDecimal("nckd_reqbaseqty") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_reqbaseqty");
            //金额
            BigDecimal nckdSumtaxamount = next.getBigDecimal("nckd_sumtaxamount") == null ? BigDecimal.ZERO : next.getBigDecimal("nckd_sumtaxamount");
            if(nckdReqbaseqty.compareTo(BigDecimal.ZERO) != 0){
                //计算含税单价
                BigDecimal divide = nckdSumtaxamount.divide(nckdReqbaseqty, RoundingMode.CEILING);
                next.set("nckd_pricetax",divide);
            }
            //计算无交易天数
            long maxOrderdate = DateUtil.endOfDay(next.getDate("max_orderdate")).getTime();
            long time = DateUtil.endOfDay(new Date()).getTime();
            long days = (time - maxOrderdate)/24/60/60/1000;
            next.set("notradedays",days);

        }
    }
}