package nckd.yanye.hr.plugin.form;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.BizDataEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.exception.KDBizException;
import kd.bos.form.*;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.ListShowParameter;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import nckd.yanye.hr.common.AppflgConstant;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
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


    private static Log logger = LogFactory.getLog(SalaryRetirCountEditPlugin.class);

    // hspm_ermanfile:人事业务档案
//    private static List<String> UINLIST = new ArrayList<>();


    private static  List<String> WAGES_LIST = new ArrayList<>();

    private static  List<String> STAFF_LIST = new ArrayList<>();


    @Override
    public void createNewData(BizDataEventArgs e) {
        super.createNewData(e);
    }

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);

        //  获取定调薪档案的类型编码
        String wagesTypeCode = AppflgConstant.HSBS_STANDARDITEM_NUM;
        WAGES_LIST = new ArrayList<>();
        STAFF_LIST = new ArrayList<>();

        if(StringUtils.isNotEmpty(wagesTypeCode)){
            String[] split = wagesTypeCode.split(",");
            for(String s : split){
                WAGES_LIST.add(s);
            }
        }
        if(StringUtils.isNotEmpty(AppflgConstant.HR_ZHIJI_STAFF)){
            String[] split = AppflgConstant.HR_ZHIJI_STAFF.split(",");
            for(String s : split){
                STAFF_LIST.add(s);
            }
        }
    }

    public List<String> getUINLIST() {
        //  进入获取到员工离岗退养工资统计单据体中存在的人员信息的工号放到uinList中
        QFilter qFilter = new QFilter("billno", QCP.not_equals, this.getModel().getValue("billno"));
        List<String> UINLIST = new ArrayList<>();
        DynamicObject[] nckdStaffretiresalacounts = BusinessDataServiceHelper.load("nckd_staffretiresalacount", "id,billno,billstatus,entryentity,entryentity.nckd_name,entryentity.nckd_name.number", new QFilter[]{qFilter});
        if (ObjectUtils.isNotEmpty(nckdStaffretiresalacounts)) {
            for (DynamicObject nckdStaffretiresalacount : nckdStaffretiresalacounts) {
                DynamicObjectCollection entryentity = nckdStaffretiresalacount.getDynamicObjectCollection("entryentity");
                if (ObjectUtils.isNotEmpty(entryentity)) {
                    for (DynamicObject dynamicObject : entryentity) {
                        String string = dynamicObject.getDynamicObject("nckd_name").getString("number");
                        UINLIST.add(string);
                    }
                }
            }
        }
        return UINLIST;
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
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate formOperate = (FormOperate)args.getSource();
        String operateKey = formOperate.getOperateKey();
        switch (operateKey){
            case "save":
                saveValicationValidator(args);
                break;
            default:
                break;
        }
    }

    private void saveValicationValidator(BeforeDoOperationEventArgs args) {
        String isDealed = this.getView().getPageCache().get("isDealed");

        List<String> uinlist = getUINLIST();
        DynamicObject dataEntity = this.getModel().getDataEntity();
        DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("entryentity");
        String errMessage = "";
        if(ObjectUtils.isNotEmpty(entryentity)){
            for(DynamicObject dynamicObject : entryentity){
                String string = dynamicObject.getString("nckd_name.number");
                if(uinlist.contains(string)){
                    errMessage= errMessage.concat(dynamicObject.getString("nckd_name.name")+",");
                }
            }
            if(StringUtils.isNotEmpty(errMessage)){
                errMessage = errMessage.concat("已存在于离岗退养工资统计单据中，是否继续保存?");
            }
        }
        if (!"true".equals(isDealed) && StringUtils.isNotEmpty(errMessage)) {
            // 判断是否已经点击过
            // 取消原来的操作
            args.setCancel(true);
            // 在用户点击确认框上的按钮后，系统会调用confirmCallBack方法
            ConfirmCallBackListener confirmCallBackListener = new ConfirmCallBackListener("isExceed", this);
            // 设置页面确认框，参数为：标题，选项框类型，回调监听
            this.getView().showConfirm(errMessage, MessageBoxOptions.YesNo, confirmCallBackListener);
            // 只执行一次
            this.getView().getPageCache().put("isDealed", "true");
        }

    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        //判断回调参数id
        if ("isExceed".equals(messageBoxClosedEvent.getCallBackId())) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                this.getView().invokeOperation("save");
            } else if (MessageBoxResult.No.equals(messageBoxClosedEvent.getResult())) {
                // 点击否也清除
                this.getView().getPageCache().remove("isDealed");
            }
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
                amountstandardChange(newValue,iRow);
                break;
            case "nckd_highfee":
                // 高温费修改
            case "nckd_welfareamount":
                // 福利金额修改
            case "nckd_welfareamount1":
                // 福利金额1修改
            case "nckd_welfareamount2":
                // 福利金额2修改
            case "nckd_welfareamount3":
                // 福利金额3修改
                countAmount(iRow);
                break;
            default:
                break;
        }

    }
    private void amountstandardChange(Object newValue,int iRow) {
        // 工资标准修改
        BigDecimal amountstandard = (BigDecimal) newValue;
        // 发放比例
        BigDecimal grantproportion = (BigDecimal) this.getModel().getValue("nckd_grantproportion",iRow);

        // 两年以上
        BigDecimal twoupmonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_twoupmonth",iRow));
        BigDecimal istwoupmonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_twoupmonth", iRow)).compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal twoupgrantpro = (BigDecimal)this.getModel().getValue("nckd_twoupgrantpro",iRow);
        // 工资标准
        BigDecimal multiply3 = amountstandard.multiply(grantproportion).multiply(twoupgrantpro).multiply(istwoupmonth);
        this.getModel().setValue("nckd_twoupsum",multiply3,iRow);
        // 工资合计
        BigDecimal multiply = multiply3.multiply(twoupmonth);

        // 一年以上
        BigDecimal oneupmonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_oneupmonth",iRow));
        BigDecimal isoneupmonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_oneupmonth", iRow)).compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal oneupgrantpro = (BigDecimal)this.getModel().getValue("nckd_oneupgrantpro",iRow);

        BigDecimal multiply5 = amountstandard.multiply(grantproportion).multiply(oneupgrantpro).multiply(isoneupmonth);
        this.getModel().setValue("nckd_oneupsum",multiply5,iRow);
        BigDecimal multiply1 = multiply5.multiply(oneupmonth);

        // 一年之内
        BigDecimal onemonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_onemonth",iRow));
        BigDecimal isonemonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_onemonth", iRow)).compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal onegrantpro = (BigDecimal)this.getModel().getValue("nckd_onegrantpro",iRow);

        BigDecimal multiply4 = amountstandard.multiply(grantproportion).multiply(onegrantpro).multiply(isonemonth);
        this.getModel().setValue("nckd_onesum",multiply4,iRow);
        BigDecimal multiply2 = onemonth.multiply(multiply4);

        // 总计
        BigDecimal nckdHighfee = (BigDecimal) this.getModel().getValue("nckd_highfee",iRow);
        BigDecimal nckdWelfareamount = (BigDecimal) this.getModel().getValue("nckd_welfareamount",iRow);
        BigDecimal nckdWelfareamount1 = (BigDecimal) this.getModel().getValue("nckd_welfareamount1",iRow);
        BigDecimal nckdWelfareamount2 = (BigDecimal) this.getModel().getValue("nckd_welfareamount2",iRow);
        BigDecimal nckdWelfareamount3 = (BigDecimal) this.getModel().getValue("nckd_welfareamount3",iRow);


        BigDecimal sum = multiply.add(multiply1)
                .add(multiply2)
                .add(nckdHighfee)
                .add(nckdWelfareamount)
                .add(nckdWelfareamount1)
                .add(nckdWelfareamount2)
                .add(nckdWelfareamount3);
        this.getModel().setValue("nckd_subtotals",sum,iRow);


    }

    private void retiredateChange(Object newValue,int iRow) {
        //  退休日期修改后，需要重新计算工资和高温费
        if(ObjectUtils.isNotEmpty(newValue)){
            Date retiredate = (Date)newValue;
            // 计算月份间隔
            Date nckdTaskeffect = (Date) this.getModel().getValue("nckd_taskeffect");
            // 转换为 LocalDate

            int totalMonths = getDiffMonthsByLocalDate(nckdTaskeffect, retiredate, false, true);

            // 计算各个时间段的月份
            int monthsInOneYear = Math.min(totalMonths, 12); // 一年以内的月份
            int monthsInTwoYears = (totalMonths >= 12) ? Math.min(totalMonths - 12, 12) : 0; // 一年到两年的月份
            int monthsInTwoToThreeYears = (totalMonths >= 24) ? totalMonths - 24 : 0; // 两年到三年的月份

            this.getModel().setValue("nckd_twoupmonth",monthsInTwoToThreeYears,iRow);
            this.getModel().setValue("nckd_oneupmonth",monthsInTwoYears,iRow);
            this.getModel().setValue("nckd_onemonth",monthsInOneYear,iRow);

            // 统计各个时间段内的 6、7、8、9 月的个数
            int countOneYear = 0;
            int countTwoYears = 0;
            int countTwoToThreeYears = 0;

            // 计算开始日期和结束日期的 LocalDate
            LocalDate startDate = nckdTaskeffect.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = retiredate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            // 按照月份统计
            for (int i = 0; i < totalMonths; i++) {
                LocalDate currentMonth = startDate.plusMonths(i);

                // 判断当前月份是否在 6、7、8、9 之间
                if (currentMonth.getMonthValue() >= 6 && currentMonth.getMonthValue() <= 9) {
                    countOneYear++;
                }
            }
            // 高温费标准
            BigDecimal nckdHighfee = BigDecimal.valueOf(countOneYear).multiply((BigDecimal) this.getModel().getValue("nckd_highfeestand"));
            this.getModel().setValue("nckd_highfee",nckdHighfee,iRow);
            this.getModel().setValue("nckd_highfeemonth",countOneYear,iRow);

            // 发放比例
            BigDecimal nckdGrantproportion = (BigDecimal) this.getModel().getValue("nckd_grantproportion");

            // 工资标准
            BigDecimal nckdAmountstandard = (BigDecimal) this.getModel().getValue("nckd_amountstandard");

            // 工资标准  =  工资标准*发放比例*比例 ：月份为0则为0
            // 离法定退休年龄一年内
            // 比例
            BigDecimal oneyeardrop = (BigDecimal) this.getModel().getValue("nckd_onegrantpro");
            BigDecimal wages = BigDecimal.ZERO;
            if(monthsInOneYear == 0){
                wages = BigDecimal.ZERO;
            }else{
                wages = nckdAmountstandard.multiply(nckdGrantproportion).multiply(oneyeardrop);
            }
            this.getModel().setValue("nckd_onesum",wages,iRow);

            // 离法定退休年龄两年内，一年以上
            // 比例
            BigDecimal nckdOneupgrantpro = (BigDecimal) this.getModel().getValue("nckd_oneupgrantpro");
            if(monthsInTwoYears == 0){
                wages = BigDecimal.ZERO;
            }else{
                wages = nckdAmountstandard.multiply(nckdGrantproportion).multiply(nckdOneupgrantpro);
            }
            this.getModel().setValue("nckd_oneupsum",wages,iRow);

            // 离法定退休年龄三年内，两年以上
            // 比例
            BigDecimal nckdTwoupgrantpro = (BigDecimal) this.getModel().getValue("nckd_twoupgrantpro");
            if(monthsInTwoToThreeYears == 0){
                wages = BigDecimal.ZERO;
            }else{
                wages = nckdAmountstandard.multiply(nckdGrantproportion).multiply(nckdTwoupgrantpro);
            }
            this.getModel().setValue("nckd_twoupsum",wages,iRow);

        }else{
            // 为null 工资和高温费重置为0
            this.getModel().beginInit();
            this.getModel().setValue("nckd_twoupmonth",0,iRow);
            this.getModel().setValue("nckd_twoupsum",0,iRow);
            this.getModel().setValue("nckd_highfeemonth",0,iRow);
            this.getModel().setValue("nckd_oneupmonth",0,iRow);
            this.getModel().setValue("nckd_oneupsum",0,iRow);

            this.getModel().setValue("nckd_onemonth",0,iRow);
            this.getModel().setValue("nckd_onesum",0,iRow);


            this.getModel().endInit();

        }
        countAmount(iRow);
        this.getView().updateView("entryentity");

    }

    private void countAmount(int iRow) {

        BigDecimal nckdTwoupmonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_twoupmonth",iRow));
        BigDecimal nckdTwoupsum = (BigDecimal) this.getModel().getValue("nckd_twoupsum",iRow);
        BigDecimal multiplytwoup = nckdTwoupmonth.multiply(nckdTwoupsum);

        BigDecimal nckdOneupmonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_oneupmonth",iRow));
        BigDecimal nckdOneupsum = (BigDecimal) this.getModel().getValue("nckd_oneupsum",iRow);
        BigDecimal multiplyoneup = nckdOneupmonth.multiply(nckdOneupsum);

        BigDecimal nckdOnemonth = BigDecimal.valueOf((int) this.getModel().getValue("nckd_onemonth",iRow));
        BigDecimal nckdOnesum = (BigDecimal) this.getModel().getValue("nckd_onesum",iRow);
        BigDecimal multiplyone = nckdOnemonth.multiply(nckdOnesum);

        BigDecimal nckdHighfee = (BigDecimal) this.getModel().getValue("nckd_highfee",iRow);
        BigDecimal nckdWelfareamount = (BigDecimal) this.getModel().getValue("nckd_welfareamount",iRow);
        BigDecimal nckdWelfareamount1 = (BigDecimal) this.getModel().getValue("nckd_welfareamount1",iRow);
        BigDecimal nckdWelfareamount2 = (BigDecimal) this.getModel().getValue("nckd_welfareamount2",iRow);
        BigDecimal nckdWelfareamount3 = (BigDecimal) this.getModel().getValue("nckd_welfareamount3",iRow);
        BigDecimal count = multiplytwoup.add(multiplyoneup)
                .add(multiplyone)
                .add(nckdHighfee)
                .add(nckdWelfareamount)
                .add(nckdWelfareamount1)
                .add(nckdWelfareamount2)
                .add(nckdWelfareamount3);
        this.getModel().setValue("nckd_subtotals",count,iRow);
    }



    // 构建人员信息列表
    private void showUnitList() {
        ListShowParameter lsp = ShowFormHelper.createShowListForm("hspm_ermanfile", true, 2);

//        Date date = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String dateStr = sdf.format(date);
//        Date date2 = DateUtils.stringToDate(dateStr, DatePattern.YYYY_MM_DD_HH_MM_SS);

        // 过滤出属于自己组织的
        DynamicObject nckdRecruitcompany = (DynamicObject)this.getModel().getValue("org");
        List<Long> longs = new ArrayList<Long>();
        Long pkValue = (Long) nckdRecruitcompany.getPkValue();
        longs.add(pkValue);
        List<Long> allSubordinateOrgIds = OrgUnitServiceHelper.getAllSubordinateOrgs("29", longs, true);
        QFilter qFilter1 = new QFilter("org.id", QCP.equals, pkValue);


        lsp.setFormId("hspm_ermanfiletreelistf7");
        lsp.setCloseCallBack(new CloseCallBack(this, "selectMUs"));
        QFilter qFilter = new QFilter("empposrel.isprimary", QCP.equals, "1")
                .and(qFilter1)
                .and("empentrel.laborrelstatus.number", QCP.equals, "1190_S") // 1190_S 用工关系状态：离岗退养
                .and("iscurrentversion", QCP.equals, "1");// 是否主任职
//                .and("number", QCP.not_in, UINLIST); // 已经维护离岗退养统计的不再重复使用
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
//            this.getModel().batchCreateNewEntryRow("entryentity", col.size());
//            int index = this.getModel().getEntryCurrentRowIndex("entryentity");
            Object[] primaryKeyValues = col.getPrimaryKeyValues();
            List<String> numbers = new ArrayList<String>();
            col.getBillListSelectedRowCollection().forEach(row -> numbers.add(row.getNumber()));
            DynamicObject org =(DynamicObject) this.getModel().getValue("org");

            QFilter qFilerman = new QFilter("id", QCP.in, primaryKeyValues);
            DynamicObject[] hspmErmanfiles = BusinessDataServiceHelper.load("hspm_ermanfile", "id,empposrel,person.id,person.number", new QFilter[]{qFilerman});
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
            Set<Long> zhjiIds = new HashSet<>();
            Map<Long, DynamicObject> hrpiEmpposorgrelMap = Arrays.stream(hrpiEmpposorgrels)
                    .collect(Collectors.toMap(
                            detail -> (Long) detail.get("id"), // 获取 person.id 作为键
                            detail -> detail, // 整个 DynamicObject 作为值
                            (existing, replacement) -> existing // 保留第一个遇到的值
                    ));
            hrpiEmpposorgrelMap.values().forEach(detail -> {
                // 假设 nckd_zhiji 是一个 DynamicObject，并且它有一个 id 属性
                DynamicObject zhiji = (DynamicObject) detail.get("nckd_zhiji");
                if (zhiji != null) {
                    Long zhijiId = (Long) zhiji.get("id");
                    zhjiIds.add(zhijiId);
                }
            });
            List<Long> zhjiIdList = zhjiIds.stream().collect(Collectors.toList());
            QFilter postQFilter = new QFilter("nckd_post.id", QCP.in, zhjiIdList)
                    .and("nckd_org.id",QCP.equals,org.getPkValue());

            // 职级工资标准
            DynamicObject[] nckdRanksalarystandards = BusinessDataServiceHelper.load("nckd_ranksalarystandard", "id,nckd_post,nckd_twoupdecimalprop,nckd_oneupdecimalprop,nckd_onedecimalprop,nckd_grantproportion,nckd_highfeestand,nckd_salarystandards", new QFilter[]{postQFilter});
            Map<Long, DynamicObject> ranksalarystandardMap = Arrays.stream(nckdRanksalarystandards)
                    .collect(Collectors.toMap(
                            detail -> (Long) detail.get("nckd_post.id"), // 获取 person.id 作为键
                            detail -> detail, // 整个 DynamicObject 作为值
                            (existing, replacement) -> existing // 保留第一个遇到的值
                    ));

            // 证件信息
            DynamicObject[] hspmPercres = BusinessDataServiceHelper.load("hspm_percre", "id,number,person.number,credentialstype,", new QFilter[]{status,idQFilter,personQFilter});
            Map<Long, DynamicObject> hspmPercreMap = Arrays.stream(hspmPercres)
                    .collect(Collectors.toMap(
                            detail -> (Long) detail.get("person.id"), // 获取 person.id 作为键
                            detail -> detail, // 整个 DynamicObject 作为值
                            (existing, replacement) -> existing // 保留第一个遇到的值
                    ));

            DynamicObjectCollection entryentity = this.getModel().getDataEntity(true).getDynamicObjectCollection("entryentity");

            for (int i = 0; i < hspmErmanfiles.length; i++) {
//                int rowIndex = index + i;
                // 单据体新增行数据
                DynamicObject dynamicObject = new DynamicObject(entryentity.getDynamicObjectType());
                Long personId = hspmErmanfiles[i].getLong("person.id");
                Long empposrelId =(Long)hspmErmanfiles[i].getDynamicObject("empposrel").getPkValue();

                DynamicObject person = hrpiPeopleMap.get(personId);
                DynamicObject hspmPersoninfo = personInfoMap.get(personId);
                DynamicObject hspmPercre = hspmPercreMap.get(personId);
                DynamicObject hrpiEmpposorgrel = hrpiEmpposorgrelMap.get(empposrelId);
                DynamicObject zhiji = ranksalarystandardMap.get((Long) hrpiEmpposorgrel.getDynamicObject("nckd_zhiji").getPkValue());
                if(ObjectUtils.isNotEmpty(zhiji)){
                    String number = hrpiEmpposorgrel.getDynamicObject("nckd_zhiji").getString("number");
                    if(STAFF_LIST.contains(number)){
                        // 如果是员工,或其他的工资标准要从 工资标准从人员信息-定调薪档案中获取，定调薪项目为绩效工资基数和岗位工资基数，金额字段相加
                        // 定调薪档案 hcdm_adjfileinfo
                        QFilter qFilter = new QFilter("person.id", QCP.equals, personId)
                                .and("isprimary",QCP.equals,"1");
                        DynamicObject adjfileinfo = BusinessDataServiceHelper.loadSingle("hcdm_adjfileinfo", "id,personfield", new QFilter[]{qFilter, status});
                        // 定调薪信息 hcdm_salaryadjrecord
                        if(ObjectUtils.isNotEmpty(adjfileinfo)){
                            QFilter qFilter1 = new QFilter("salaryadjfile.id", "=", adjfileinfo.getPkValue())
                                    .and("standarditem.number",QCP.in,WAGES_LIST);
                            DynamicObject[] hcdmAdjfileinfos = BusinessDataServiceHelper.load("hcdm_salaryadjrecord", "id,personfield,amount,standarditem,standarditem.number", new QFilter[]{qFilter1,status});
                            BigDecimal wages = new BigDecimal(0);
                            if(ObjectUtils.isNotEmpty(hcdmAdjfileinfos)){
                                for (DynamicObject hcdmAdjfileinfo : hcdmAdjfileinfos) {
                                    wages = wages.add(hcdmAdjfileinfo.getBigDecimal("amount"));
                                }
                                dynamicObject.set("nckd_amountstandard",wages);
                            }

                        }

                    }else{
                        // 中层工资标准
                        dynamicObject.set("nckd_amountstandard",zhiji.get("nckd_salarystandards"));
                    }
                    // 发放比例
                    dynamicObject.set("nckd_grantproportion",zhiji.get("nckd_grantproportion"));
                    zhiji.get("nckd_grantproportion");
                    // 高温费标准
//                    zhiji.get("nckd_highfeestand");
                    dynamicObject.set("nckd_highfeestand",zhiji.get("nckd_highfeestand"));
                    dynamicObject.set("nckd_twoupgrantpro",zhiji.get("nckd_twoupdecimalprop"));
                    dynamicObject.set("nckd_oneupgrantpro",zhiji.get("nckd_oneupdecimalprop"));
                    dynamicObject.set("nckd_onegrantpro",zhiji.get("nckd_onedecimalprop"));

                }


                Date startdate = hrpiEmpposorgrel.getDate("startdate");
                // 姓名
                dynamicObject.set("nckd_name",person);
                // 开始日期
                dynamicObject.set("nckd_taskeffect",startdate);
                // 性别
                dynamicObject.set("nckd_sex",hspmPersoninfo.get("gender"));
                // 年龄
                dynamicObject.set("nckd_age",hspmPersoninfo.get("age"));
                // 出生日期
                dynamicObject.set("nckd_birthday",hspmPersoninfo.get("birthday"));
                // 身份证号
                dynamicObject.set("nckd_certificatesnum",hspmPercre.get("number"));
                // 职务
                dynamicObject.set("nckd_post",hrpiEmpposorgrel.get("nckd_zhiji"));

                entryentity.add(dynamicObject);

            }
            this.getView().updateView("entryentity");

        }
    }


    // 标品财务使用的计算月份数
    public static int getDiffMonthsByLocalDate(Date beginDate, Date endDate, boolean includeBeginDate, boolean monthRoundUp) {
        if (compareDate(beginDate, endDate) >= 0) {
            return 0;
        } else {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String beginDateStr = format.format(beginDate);
            String endDateStr = format.format(endDate);
            LocalDate beginLocalDate = LocalDate.parse(beginDateStr);
            LocalDate endLocalDate = LocalDate.parse(endDateStr);
            if (includeBeginDate) {
                endLocalDate = endLocalDate.plusDays(1L);
            }

            Period period = Period.between(beginLocalDate, endLocalDate);
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();
            int diffMonth = years * 12 + months;
            if (monthRoundUp && days > 0) {
                ++diffMonth;
            }

            return diffMonth;
        }
    }

    public static int compareDate(Date d1, Date d2) {
        if (d1 != null && d2 != null) {
            long t1 = d1.getTime();
            long t2 = d2.getTime();
            return Long.compare(t1, t2);
        } else {
            KDBizException exception = new KDBizException(ResManager.loadKDString("比较的日期为空，请联系管理员。", "DateUtil_0", "fi-fa-common", new Object[0]));
            logger.error(String.format("日期比较错误：date1:[%s], date2:[%s]", d1, d2), exception);
            throw exception;
        }
    }


}
