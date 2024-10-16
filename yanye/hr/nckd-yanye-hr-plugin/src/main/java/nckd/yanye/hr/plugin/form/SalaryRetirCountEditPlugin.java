package nckd.yanye.hr.plugin.form;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.BizDataEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.fa.common.util.DateUtil;
import kd.hr.hbp.common.util.DatePattern;
import kd.hr.hbp.common.util.DateUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Module           :薪酬福利云-薪资数据集成-员工离岗退养工资统计
 * Description      :员工离岗退养工资统计编辑插件
 *
 * @author guozhiwei
 * @date  2024/10/14 9:59
 *  标识:nckd_staffretiresalacount
 *
 */


public class SalaryRetirCountEditPlugin extends AbstractBillPlugIn   {

    // hspm_ermanfile:人事业务档案
    private static final List<String> UINLIST = new ArrayList<>();


    @Override
    public void createNewData(BizDataEventArgs e) {
        super.createNewData(e);
        // todo 进入获取到员工离岗退养工资统计单据体中存在的人员信息的工号放到uinList中


    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("advcontoolbarap");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        String itemKey = evt.getItemKey();
        if (itemKey.equals("nckd_retirperson")) {
            showUnitList();

        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        int iRow = changeData[0].getRowIndex();
        switch (key){
            case "nckd_retiredate":
                // 到退休日期修改
                retiredateChange(newValue,iRow);
                break;
            case "nckd_amountstandard":
                // 工资标准修改
                break;
            case "nckd_highfee":
                // 高温费修改
                break;
            case "nckd_welfareamount":
                // 福利金额修改
                break;
            case "nckd_welfareamount1":
                // 福利金额修改
                break;
            case "nckd_welfareamount2":
                // 福利金额修改
                break;
            case "nckd_welfareamount3":
                // 福利金额修改
                break;
            default:
                break;
        }

    }

    private void retiredateChange(Object newValue,int iRow) {
        // todo 退休日期修改后，需要重新计算工资和高温费
        if(ObjectUtils.isNotEmpty(newValue)){
            Date retiredate = (Date)newValue;
            // 计算月份间隔
            Date nckdTaskeffect = (Date) this.getModel().getValue("nckd_taskeffect");
            // 转换为 LocalDate
//            LocalDate retireDate = convertToLocalDate(retiredate);
//            LocalDate taskEffectDate = convertToLocalDate(nckdTaskeffect);
//            // 计算两个日期之间的年月差
//            Period period = Period.between(taskEffectDate, retireDate);
            int totalMonths = DateUtil.getDiffMonthsByLocalDate(nckdTaskeffect, retiredate, false, true);


            // 计算各个时间段的月份
            int monthsInOneYear = Math.min(totalMonths, 12); // 一年以内的月份
            int monthsInTwoYears = (totalMonths >= 12) ? Math.min(totalMonths - 12, 12) : 0; // 一年到两年的月份
            int monthsInTwoToThreeYears = (totalMonths >= 24) ? Math.min(totalMonths - 24, 12) : 0; // 两年到三年的月份

            this.getModel().setValue("nckd_twoupmonth",monthsInTwoToThreeYears,iRow);
            this.getModel().setValue("nckd_oneupmonth",monthsInTwoYears,iRow);
            this.getModel().setValue("nckd_onemonth",monthsInOneYear,iRow);

        }else{
            // 为null 工资和高温费重置为0
            this.getModel().beginInit();
            this.getModel().setValue("nckd_twoupmonth",0,iRow);
            this.getModel().setValue("nckd_twoupsum",0,iRow);

            this.getModel().setValue("nckd_oneupmonth",0,iRow);
            this.getModel().setValue("nckd_oneupsum",0,iRow);

            this.getModel().setValue("nckd_onemonth",0,iRow);
            this.getModel().setValue("nckd_onesum",0,iRow);

            this.getModel().setValue("nckd_highfee",0,iRow);

            this.getModel().endInit();

        }
        this.getView().updateView("entryentity");




    }





    // 构建人员信息列表
    private void showUnitList() {
        ListShowParameter lsp = ShowFormHelper.createShowListForm("hspm_ermanfile", true, 2);

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(date);
        Date date2 = DateUtils.stringToDate(dateStr, DatePattern.YYYY_MM_DD_HH_MM_SS);


        lsp.setFormId("hspm_ermanfiletreelistf7");
        lsp.setCloseCallBack(new CloseCallBack(this, "selectMUs"));
        QFilter qFilter = new QFilter("empposrel.isprimary", QCP.equals, "1")
                .and("empentrel.laborrelstatus.number", QCP.equals, "1190_S") // 1190_S 用工关系状态：离岗退养
                .and("iscurrentversion", QCP.equals, "1")// 是否主任职
                .and("number", QCP.not_in, UINLIST); // 已经维护离岗退养统计的不再重复使用
        lsp.setCloseCallBack(new CloseCallBack(this, "selectMUs"));
        lsp.getListFilterParameter().getQFilters().add(qFilter);
        this.getView().showForm(lsp);
    }


    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        String actionId = e.getActionId();
        if ("selectMUs".equals(actionId)) {
            showUintInfo(e);

        }

    }

    // 辅助方法，将 Date 转换为 LocalDate
    private static LocalDate convertToLocalDate(Date date) {
        Instant instant = date.toInstant(); // 将 Date 转换为 Instant
        return instant.atZone(ZoneId.systemDefault()).toLocalDate(); // 转换为 LocalDate
    }


    private void showUintInfo(ClosedCallBackEvent e) {
        ListSelectedRowCollection col = (ListSelectedRowCollection)e.getReturnData();
        if (col != null && col.size() != 0) {
            this.getModel().batchCreateNewEntryRow("entryentity", col.size());
            int index = this.getModel().getEntryCurrentRowIndex("entryentity");
            Object[] primaryKeyValues = col.getPrimaryKeyValues();
            List<String> numbers = new ArrayList<String>();
            col.getBillListSelectedRowCollection().forEach(row -> numbers.add(row.getNumber()));

            QFilter qFilerman = new QFilter("id", QCP.in, primaryKeyValues);
            DynamicObject[] hspmErmanfiles = BusinessDataServiceHelper.load("hspm_ermanfile", "id,empposrel,person.id", new QFilter[]{qFilerman});
            DynamicObject[] hspmEmpposrelscopy = hspmErmanfiles;
            // 使用流循环获取并构建 List<Object> 集合

            List<Object> empposrelIds =new ArrayList<>();
            List<Object> personIds = Arrays.stream(hspmErmanfiles)
                    .map(detail -> {
                        // 在这里处理 DynamicObject，提取需要的字段并构建对象
                        // 例如，假设 detail.getValue() 是要提取的值
                        empposrelIds.add(detail.getDynamicObject("empposrel").getPkValue());
                        return detail.get("person.id"); // 将提取出的值返回
                    })
                    .collect(Collectors.toList());

            QFilter personQFilter = new QFilter("person.id", QFilter.in, personIds);

            QFilter empposrelQFilter = new QFilter("id", QCP.in, empposrelIds);

            QFilter status = new QFilter("iscurrentversion", QCP.equals, "1")
                    .and("datastatus", QCP.equals, "1");

            QFilter hrPersion = new QFilter("id", QCP.in, personIds);

            // 证件信息 身份证号
            QFilter idQFilter = new QFilter("credentialstype.number",QCP.equals,"1010_S");
            // 人员 基本信息表单
            DynamicObject[] hspmPersoninfos = BusinessDataServiceHelper.load("hspm_personinfo", "id,name,person.id,person.number,age,birthday,gender.name,gender.number", new QFilter[]{personQFilter});

            Map<Long, DynamicObject> personInfoMap = Arrays.stream(hspmPersoninfos)
                    .collect(Collectors.toMap(
                            detail -> (Long) detail.get("person.id"), // 获取 person.id 作为键
                            detail -> detail, // 整个 DynamicObject 作为值
                            (existing, replacement) -> existing // 保留第一个遇到的值
                    ));

            // HR人员
            DynamicObject[] hrpiPeoples = BusinessDataServiceHelper.load("hrpi_person", "id,number,name", new QFilter[]{status, hrPersion});
            Map<Long, DynamicObject> hrpiPeopleMap = Arrays.stream(hrpiPeoples)
                    .collect(Collectors.toMap(
                            detail -> (Long) detail.get("id"), // 获取 person.id 作为键
                            detail -> detail, // 整个 DynamicObject 作为值
                            (existing, replacement) -> existing // 保留第一个遇到的值
                    ));

            // 任职经历基础页面
            DynamicObject[] hrpiEmpposorgrels = BusinessDataServiceHelper.load("hrpi_empposorgrel", "id,startdate,nckd_zhiji", new QFilter[]{empposrelQFilter});
            Map<Long, DynamicObject> hrpiEmpposorgrelMap = Arrays.stream(hrpiEmpposorgrels)
                    .collect(Collectors.toMap(
                            detail -> (Long) detail.get("id"), // 获取 person.id 作为键
                            detail -> detail, // 整个 DynamicObject 作为值
                            (existing, replacement) -> existing // 保留第一个遇到的值
                    ));
            // 证件信息
            DynamicObject[] hspmPercres = BusinessDataServiceHelper.load("hspm_percre", "id,number,person.number,credentialstype,", new QFilter[]{status,idQFilter});
            Map<Long, DynamicObject> hspmPercreMap = Arrays.stream(hspmPercres)
                    .collect(Collectors.toMap(
                            detail -> (Long) detail.get("person.id"), // 获取 person.id 作为键
                            detail -> detail, // 整个 DynamicObject 作为值
                            (existing, replacement) -> existing // 保留第一个遇到的值
                    ));
            for (int i = 0; i < hspmErmanfiles.length; i++) {
                int rowIndex = index + i;
                Long personId = hspmErmanfiles[i].getLong("person.id");
                Long empposrelId =(Long)hspmErmanfiles[i].getDynamicObject("empposrel").getPkValue();
                DynamicObject person = hrpiPeopleMap.get(personId);
                DynamicObject hspmPersoninfo = personInfoMap.get(personId);
                DynamicObject hspmPercre = hspmPercreMap.get(personId);
                DynamicObject hrpiEmpposorgrel = hrpiEmpposorgrelMap.get(empposrelId);

                Date startdate = hrpiEmpposorgrel.getDate("startdate");
                this.getModel().setValue("nckd_name",person,rowIndex);
                this.getModel().setValue("nckd_taskeffect",startdate,rowIndex);
                this.getModel().setValue("nckd_sex",hspmPersoninfo.get("gender"),rowIndex);
                this.getModel().setValue("nckd_age",hspmPersoninfo.get("age"),rowIndex);
                this.getModel().setValue("nckd_birthday",hspmPersoninfo.get("birthday"),rowIndex);

                this.getModel().setValue("nckd_certificatesnum",hspmPercre.get("number"),rowIndex);
                this.getModel().setValue("nckd_post",hrpiEmpposorgrel.get("nckd_zhiji"),rowIndex);


            }

        }
    }


}
