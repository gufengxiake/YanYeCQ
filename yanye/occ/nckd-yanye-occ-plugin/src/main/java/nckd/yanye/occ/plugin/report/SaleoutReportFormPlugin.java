package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static kd.bos.script.jsengine.def.typemap.JSEngineInnerType.HashMap;

/**
 * 销售出库报表查询-报表界面插件
 * 表单标识：nckd_saleoutreport_rpt
 * author:zhangzhilong
 * date:2024/09/07
 */
public class SaleoutReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
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
        Map<String, String> userNameAndPhone = getUserNameAndPhone(rowData);
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();
            //实出数量
            BigDecimal outQty = row.getBigDecimal("out_qty");
            //实收数量
            BigDecimal outNckdSignqty = row.getBigDecimal("out_nckd_signqty");
            //已开票数量
            BigDecimal simIssuednum = row.getBigDecimal("sim_issuednum");

            //累计途损数量 = 实出数量 - 实收数量
            row.set("out_tusun", outQty.subtract(outNckdSignqty));
            //未开票数量 = 实收数量 - 已开票数量
            row.set("sim_nosuednum", outNckdSignqty.subtract(simIssuednum));

            if (!userNameAndPhone.isEmpty() && row.getDynamicObject("out_bizoperator") != null) {
                String operatornumber = row.getDynamicObject("out_bizoperator").getString("operatornumber");
                if(userNameAndPhone.containsKey(operatornumber)){
                    row.set("out_bizoperatorandphone",userNameAndPhone.get(operatornumber));
                }
            }

        }


    }

    public Map<String, String> getUserNameAndPhone(DynamicObjectCollection rowData) {
        Iterator<DynamicObject> iterator = rowData.iterator();
        ArrayList<String> operatorIds = new ArrayList<>();
        Map<String, String> nameAndPhone = new HashMap<>();
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();
            //获取业务员编码
            if (row.getDynamicObject("out_bizoperator") != null) {
                operatorIds.add(row.getDynamicObject("out_bizoperator").getString("operatornumber"));
            }
        }
        if (operatorIds.isEmpty()) {
            return nameAndPhone;
        }
        //根据业务员编码获取人员编码
        DataSet bosUser = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "bos_user", "phone,number,name", new QFilter[]{new QFilter("number", QCP.in, operatorIds.toArray(new String[0]))}, null);
        while (bosUser.hasNext()) {
            Row next = bosUser.next();
            String key = next.getString("number");
            //获取用户名加电话
            String value = next.getString("name") + next.getString("phone");
            nameAndPhone.put(key, value);
        }
        return nameAndPhone;
    }
}