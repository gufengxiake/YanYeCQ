package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.context.RequestContext;
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
import java.text.DecimalFormat;
import java.util.*;

import static com.ibm.db2.jcc.am.ao.ds;

/**
 * 销售情况明细表-报表界面插件
 * 表单标识：nckd_saledetailrpt
 * author:zzl
 * date:2024/08/22
 */
public class SaledetailReportFormPlugin extends AbstractReportFormPlugin implements Plugin {

    public void afterCreateNewData(EventObject e) {
        Long curLoginOrg = RequestContext.get().getOrgId();
        this.getModel().setValue("nckd_bizorg_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
        Iterator<DynamicObject> iterator = rowData.iterator();
        Map<Long, String> invoiceNo = this.getInvoiceNo(rowData);
        while (iterator.hasNext()) {
            DynamicObject row = iterator.next();
            BigDecimal nckd_amount =  row.getBigDecimal("nckd_amount") == null
                    ? BigDecimal.ZERO :  row.getBigDecimal("nckd_amount");
            if( nckd_amount.compareTo(BigDecimal.ZERO) != 0){
                BigDecimal nckd_mll = BigDecimal.ZERO;
                //计算毛利率 = 金额-结算成本/金额
                nckd_mll = nckd_amount.subtract(row.getBigDecimal("nckd_cbj") == null
                        ? BigDecimal.ZERO : row.getBigDecimal("nckd_cbj"))  ;
                nckd_mll = nckd_mll.divide(nckd_amount, RoundingMode.CEILING);
                DecimalFormat df = new DecimalFormat("0.00%");
                String percent=df.format(nckd_mll);
                row.set("nckd_mll", percent);
            }

            if(row.getBigDecimal("nckd_thsl").compareTo(BigDecimal.ZERO) == 0){
                row.set("nckd_thbs", "N");
            }else{
                row.set("nckd_thbs", "Y");
            }

            //获取发票编号
            if (row.getString("nckd_mainbillentity").equals("ocbsoc_saleorder")) {
                Long key = row.getLong("nckd_mainbillentryid");
                if (invoiceNo.isEmpty() || key == 0L) continue;
                if (invoiceNo.containsKey(key)) {
                    row.set("nckd_invoiceno", invoiceNo.get(key));
                }
            }else{
                row.set("nckd_mainbillnumber",null);
            }

        }
    }

    //获取发票编号
    public Map<Long,String> getInvoiceNo(DynamicObjectCollection rowData){
        Iterator<DynamicObject> iterator = rowData.iterator();
        List<Long> mainbillentryid = new ArrayList<>();
        while (iterator.hasNext()) {
            DynamicObject next = iterator.next();
            if ( next.getLong("nckd_mainbillentryid")!= 0L) {
                mainbillentryid.add(next.getLong("nckd_mainbillentryid"));
            }

        }
        QFilter qFilter = new QFilter("sim_original_bill_item.corebillentryid" , QCP.in , mainbillentryid.toArray(new Long[0]));
        DataSet originalBill = QueryServiceHelper.queryDataSet(this.getClass().getName(),
                "sim_original_bill", "sim_original_bill_item.corebillentryid as corebillentryid, invoiceno as nckd_invoiceno",
                new QFilter[]{qFilter},null);
        Map<Long,String> invoiceNo = new HashMap<>();
        while (originalBill.hasNext()) {
            Row row = originalBill.next();
            Long corebillentryid = row.getLong("corebillentryid");
            String nckdInvoiceno = row.getString("nckd_invoiceno");
            invoiceNo.put(corebillentryid,nckdInvoiceno);
        }

        return invoiceNo;
    }

}