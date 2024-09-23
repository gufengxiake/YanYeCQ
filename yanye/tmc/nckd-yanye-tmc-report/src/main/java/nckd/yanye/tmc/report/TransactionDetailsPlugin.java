package nckd.yanye.tmc.report;

import kd.bos.algo.*;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.fa.common.util.DateUtil;
import kd.taxc.tdm.common.util.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TransactionDetailsPlugin extends AbstractReportListDataPlugin {

    private final List<String> ACCEP_LIST = Arrays.asList(new String[]{"101", "102"});
    private final static SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {

        QFilter qFilter1 = null;
        FilterInfo filterInfo=reportQueryParam.getFilter();
        List<FilterItemInfo> itemInfos=filterInfo.getFilterItems();

        for (FilterItemInfo item:itemInfos) {
            if(item.getPropName().equals("nckd_company")){
                item.getValue();
                if(ObjectUtils.isNotEmpty(item.getValue())){
                    qFilter1 = new QFilter("company.id", QCP.equals, ((DynamicObject)item.getValue()).getPkValue());
                }

            }
        }
        String select =  "id,company,bizdate,draftbillno,subbillrange,drawername,issuedate,draftbillexpiredate,amount," +
                "draftbilltype,draftbilltype.number,receivername,delivername,issplit,istransfer,eledraftstatusnew," +
                "elccirculatestatus,accepterbebank,acceptername,'否' as nckd_istransferred,accepterbebank.nckd_bankcredit_type," +
                "draftbillstatus,bizfinishdate";
        DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), "cdm_receivablebill", select, new QFilter[]{qFilter1}, null);
        ORM orm = ORM.create();
        dataSet = dataSet.addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"nckd_issueticket")
               .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"nckd_paymentnature")
               .addField(ResManager.loadKDString("0", "ReportListHelper_2", "wtc-wtte-business"),"nckd_ending_balance")
               .addField(ResManager.loadKDString("0", "ReportListHelper_2", "wtc-wtte-business"),"nckd_dis_interest")
               .addField(ResManager.loadKDString("0", "ReportListHelper_2", "wtc-wtte-business"),"nckd_prepaymentamount")
               .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"nckd_prepaymentdate")
               .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"nckd_beendorsortext")
               .addField(ResManager.loadKDString("0", "ReportListHelper_2", "wtc-wtte-business"),"nckd_endorseamount")
               .addField(ResManager.loadKDString("''", "ReportListHelper_2", "wtc-wtte-business"),"nckd_paymentdate")
               .addField(ResManager.loadKDString("0", "ReportListHelper_2", "wtc-wtte-business"),"nckd_dueacceptamount")
               .addField(ResManager.loadKDString("0", "ReportListHelper_2", "wtc-wtte-business"),"nckd_dis_rate");


        DynamicObjectCollection dynamicObjects = orm.toPlainDynamicObjectCollection(dataSet.copy());

        for (DynamicObject dynamicObject : dynamicObjects) {
//            BusinessDataServiceHelper.loadSingle(dynamicObject.get("draftbilltype"),"")
            if(ACCEP_LIST.contains(dynamicObject.getString("draftbilltype.number"))){
                // 票据类型未银行承兑汇票，出票银行取“承兑人全称
                dynamicObject.set("nckd_issueticket",dynamicObject.get("acceptername"));
            }
            // 判断是否6+9
            if(StringUtils.equals(dynamicObject.getString("draftbilltype.nckd_bankcredit_type"),"A")){
                dynamicObject.set("nckd_istransferred","是");
            }
            // 票据状态
            String draftbillstatus = dynamicObject.getString("draftbillstatus");
            // 获取此票据关联的业务处理单信息
            QFilter qFilter = new QFilter("entrys.draftbill.id", QCP.equals, dynamicObject.get("id"));
            DynamicObject cdmDraftTradeBill = BusinessDataServiceHelper.loadSingle("cdm_drafttradebill", "id,tradetype,rate,description,entrys,entrys.draftbill,discountentry,discountentry.dis_interest,discountentry.dis_selectbillid", new QFilter[]{qFilter});
            boolean isflag = false;
            if(ObjectUtils.isNotEmpty(cdmDraftTradeBill)){
                isflag = true;
                // 业务处理
                String tradetype = cdmDraftTradeBill.getString("tradetype");
                if(StringUtils.equals(tradetype,"endorse")){
                    // 已登记，设置款项性质，取用途字段
                    dynamicObject.set("nckd_paymentnature",cdmDraftTradeBill.getString("description"));
                }
            }
            Date bizfinishdate = dynamicObject.getDate("bizfinishdate");
            Date parse = null;
            if(ObjectUtils.isNotEmpty(bizfinishdate)){
                parse = simpleFormat.parse(simpleFormat.format(bizfinishdate));
            }

            if(StringUtils.equals(draftbillstatus,"registered")){
                // 已登记,设置期末余额
                dynamicObject.set("nckd_ending_balance",dynamicObject.get("amount"));
            }else if(StringUtils.equals(draftbillstatus,"collected")){
                // 已托收，设置到期承兑金额,收款日期
                dynamicObject.set("nckd_dueacceptamount",dynamicObject.get("amount"));
//                dynamicObject.set("nckd_paymentdate",dynamicObject.getDate("bizfinishdate"));
                dynamicObject.set("nckd_paymentdate",parse);
            }else if(StringUtils.equals(draftbillstatus,"ebdorsed")){
                //已背书，设置金额，背书日期，被背书人
                dynamicObject.set("nckd_endorseamount",dynamicObject.get("amount"));
//                dynamicObject.set("nckd_endorsedate",dynamicObject.getDate("bizfinishdate"));
                dynamicObject.set("nckd_endorsedate",parse);
                if(isflag){
                    dynamicObject.set("nckd_beendorsortext",cdmDraftTradeBill.get("beendorsortext"));
                }
            }else if (StringUtils.equals(draftbillstatus,"discounted")){
                // 已贴现，设置贴现金额，贴现日期，贴现率，贴现息
                dynamicObject.set("nckd_prepaymentamount",dynamicObject.get("amount"));
                dynamicObject.set("nckd_prepaymentdate",parse);
                if(isflag){
//                    cdmDraftTradeBill.get("beendorsortext");
                    DynamicObjectCollection entrys = cdmDraftTradeBill.getDynamicObjectCollection("discountentry");
                    for (DynamicObject entry : entrys) {
                        if(entry.getDynamicObject("dis_selectbillid").getPkValue().equals(dynamicObject.get("id"))){
                            dynamicObject.set("nckd_dis_interest",entry.get("dis_interest"));
                            dynamicObject.set("nckd_dis_rate",cdmDraftTradeBill.get("rate"));
                        }
                    }
                }
            }

        }
        Field[] rowFields = dataSet.getRowMeta().getFields();
        //DynamicObjectCollection 转换为 DataSet
        DataSet retuenDataSet = buildDataByObjCollection("algoKey", rowFields, dynamicObjects);




        return retuenDataSet;
    }

    //DynamicObjectCollection 转换为 DataSet 方法
    public DataSet buildDataByObjCollection(String algoKey, Field[] rowFields, DynamicObjectCollection objCollection) {
        DataSetBuilder dataSetBuilder = Algo.create(algoKey + ".emptyFields")
                .createDataSetBuilder(new RowMeta(rowFields));
        for (DynamicObject arObj : objCollection) {
            Object[] rowData = new Object[rowFields.length];
            for (int i = 0; i < rowFields.length; i++) {
                Field field = rowFields[i];
                rowData[i] = arObj.get(field.getName());
            }
            dataSetBuilder.append(rowData);
        }
        return dataSetBuilder.build();
    }
}
