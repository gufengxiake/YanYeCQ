package nckd.yanye.hr.plugin.form;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.BizDataEventArgs;
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
import kd.hr.hbp.common.util.DatePattern;
import kd.hr.hbp.common.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.*;

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

    private void showUintInfo(ClosedCallBackEvent e) {
        ListSelectedRowCollection col = (ListSelectedRowCollection)e.getReturnData();
        if (col != null && col.size() != 0) {
            this.getModel().batchCreateNewEntryRow("entryentity",1);
            int index = this.getModel().getEntryCurrentRowIndex("entryentity");

            ListSelectedRow listSelectedRow = col.get(0);
            listSelectedRow.getNumber(); // 工号
            Object primaryKeyValue = listSelectedRow.getPrimaryKeyValue();//
            // 人事业务档案
            DynamicObject hspmErmanfile = BusinessDataServiceHelper.loadSingle(primaryKeyValue, "hspm_ermanfile");
            long aLong = hspmErmanfile.getLong("person.id");// hrID
            // 基本信息表单
            QFilter qFilter = new QFilter("person.id", QFilter.equals, aLong);
            // 人员 性别年龄出生日期
            DynamicObject hspmPersoninfo = BusinessDataServiceHelper.loadSingle("hspm_personinfo","id,name,person.number,age,birthday,gender.name,gender.number",new QFilter[]{qFilter});


            QFilter hrPersion = new QFilter("number", "=", listSelectedRow.getNumber())
                    .and("datastatus", QCP.equals, "1")
                    .and("iscurrentversion", QCP.equals, "1");

            DynamicObject hrpiPeople = BusinessDataServiceHelper.loadSingle("hrpi_person", "id,number,name", new QFilter[]{hrPersion});
            // 职级信息
            QFilter qFilter1 = new QFilter("empposrel.isprimary", QCP.equals, "1")
                    .and("iscurrentversion",QCP.equals,"1")
                    .and("datastatus",QCP.equals,"1");

//            DynamicObject hspmEmpposrel = BusinessDataServiceHelper.loadSingle("hspm_empjobrel", "id,empposrel.position.name,joblevel,empposrel.position.number,empposrel.isprimary", new QFilter[]{qFilter1});

//            hspmEmpposrel.get("joblevel");



            // 任职经历基础页面
            DynamicObject hrpiEmpposorgrel = BusinessDataServiceHelper.loadSingle(hspmErmanfile.getDynamicObject("empposrel").getPkValue(), "hrpi_empposorgrel");
            // 生效日期
            Date startdate = hrpiEmpposorgrel.getDate("startdate");
            // 姓名
            this.getModel().setValue("nckd_name",hrpiPeople,index);
            // nckd_taskeffect 生效日期
            this.getModel().setValue("nckd_taskeffect",startdate,index);
            this.getModel().setValue("nckd_sex",hspmPersoninfo.get("gender"),index);
            this.getModel().setValue("nckd_age",hspmPersoninfo.get("age"),index);
            this.getModel().setValue("nckd_birthday",hspmPersoninfo.get("birthday"),index);


//            this.getModel().setValue("nckd_post",hspmPersoninfo,index);
        }
    }


}
