package nckd.yanye.hr.plugin.form.bianzhi;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.AbstractFormDataModel;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.hr.haos.business.service.staff.service.StaffCommonService;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.hr.hbp.common.constants.newhismodel.EventOperateEnums;
import kd.taxc.tdm.common.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.*;


/**
 * Module           :HR中控服务云-HR基础组织-人力编制-编制调整申请
 * Description      :编制调整申请单据插件
 *
 * @author guozhiwei
 * @date  2024/9/4 9：40
 * 标识 nckd_preadjustapplic
 */


public class PrepareAdjustApplication extends AbstractBillPlugIn implements BeforeF7SelectListener {

    private final List<String> ACCEP_LIST = Arrays.asList(new String[]{"org", "和nckd_haos_staff"});


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        // 监听 org 和nckd_haos_staff 的变化
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        IDataModel model = this.getModel();
        switch (key) {
            case "nckd_haos_staff":
                this.getModel().deleteEntryData("nckd_bentryentity");
                this.initEntry2(model.getValue("org"),newValue);
                break;
            default:
                break;
        }

    }
    private void initEntry2(Object org,Object staff) {
        // 初始化申请单据
        if(isNotEmpty(org) && isNotEmpty(staff)){
            // 填充申请单据数据
            DynamicObject staff1 = (DynamicObject) staff;
            BusinessDataServiceHelper.loadSingle(staff1.getPkValue(),"haos_staff");
            IFormView view = this.getView();
            AbstractFormDataModel model = (AbstractFormDataModel)this.getModel();
//            StaffInitEntryDataService.create(view, model).loadEntryData();
//            StaffFormService.create(view, model).setVisibleByUseOrg();
            List<Long> orgIds = Arrays.asList((Long) ((DynamicObject) org).getPkValue());
            // 编制维护id
            Long pkValue = (Long) staff1.getPkValue();
            List<Long> allSubordinateOrgs = OrgUnitServiceHelper.getAllSubordinateOrgs("10", orgIds, true);
            QFilter qFilter = new QFilter("useorgbo", "in", allSubordinateOrgs);

//            DynamicObject[] haosDutyorgdetails = StaffCommonService.queryStaffTempAndEffectData("haos_dutyorgdetail", pkValue, "dutyorg, staffcount");
            // 获取到当前组织和所有下级组织的使用组织明细， 然后再去获取对应组织的部门信息
            DynamicObject[] useOrgInfoArr = StaffCommonService.queryStaffTempAndEffectDataWithFilter("haos_useorgdetail",pkValue, qFilter, "id, useorg.id, useorg.enable, useorg, useorgbo, dutyorg.id, sequence, pid,staffdimension.fbasedataid, controlstrategy, elasticcontrol, elasticcount, yearstaff, halfyearstaff1, halfyearstaff2, quarterstaff1, quarterstaff2, quarterstaff3, quarterstaff4, monthstaff1, monthstaff2, monthstaff3, monthstaff4, monthstaff5, monthstaff6, monthstaff7, monthstaff8, monthstaff9, monthstaff10, monthstaff11, monthstaff12");
            List<Long> collect = Arrays.stream(useOrgInfoArr)
                    .map(dynamicObject -> (Long) dynamicObject.getPkValue()) // 使用 lambda 表达式
                    .collect(Collectors.toList());
            QFilter qFilter1 = new QFilter("dutyorg.masterid", "in", collect);
            //  先查出haos_useorgdetail 编制信息相关联的使用组织信息，把id，pid构建成map形式
            QFilter qFilter3 = new QFilter("staff.id", "=", pkValue);
//            DynamicObject[] load = BusinessDataServiceHelper.load("haos_useorgdetail", "id,pid,staff", new QFilter[]{qFilter3});

//            BusinessDataServiceHelper.load("haos_dutyorgdetail","id",new QFilter[]{qFilter1});
            // 上级主键
//            BusinessDataServiceHelper.load("haos_useorgdetail","id",new QFilter[]{new QFilter("pid", "in", collect)});
            // 编制类型为岗位的
            QFilter qFilter2 = new QFilter("staffdimension.fbasedataid.id", "=", 1010L);
            QFilter qFilter5 = new QFilter("datastatus", "=", "1");
            QFilter qFilter4 = new QFilter("pid", "in", collect);
            DynamicObject[] haosUseorgdetails = BusinessDataServiceHelper.load("haos_useorgdetail", "id,staff,useorg,bo,staffdimension,pid", new QFilter[]{qFilter5,qFilter4, qFilter2, qFilter3});
            // 插入单据体表
            // 获取使用组织id获取他下级id
            List<Long> collect2 = Arrays.stream(haosUseorgdetails)
                    .map(dynamicObject -> (Long) dynamicObject.getPkValue()) // 使用 lambda 表达式
                    .collect(Collectors.toList());
            // 下级
            List<Long> longs = collect2;
            boolean b = true;

            List<DynamicObject> combinedList = new ArrayList<>();
            combinedList.addAll(Arrays.asList(haosUseorgdetails));
            while (b){
                DynamicObject[] load1 = BusinessDataServiceHelper.load("haos_useorgdetail", "id,staff,useorg,bo,staffdimension,pid", new QFilter[]{new QFilter("pid", "in", longs),qFilter2,qFilter3,qFilter5});
                if(ObjectUtils.isEmpty(load1)){
                    b = false;
                }else{
                    combinedList.addAll(Arrays.asList(load1));
                    longs = Arrays.stream(load1)
                            .map(dynamicObject -> (Long) dynamicObject.getPkValue()) // 使用 lambda 表达式
                            .collect(Collectors.toList());
                }
            }

            this.getView().updateView("nckd_bentryentity");
            List<DynamicObject> uniqueList = combinedList.stream()
                    .distinct()
                    .collect(Collectors.toList());
            // 插入单据体信息
            for (int i = 0; i < uniqueList.size(); i++) {
//                DynamicObject dynamicObject = entryentityCols.addNew();
                int index = this.getModel().insertEntryRow("nckd_bentryentity", i);
                this.getModel().setValue("nckd_adminorg",uniqueList.get(i).getDynamicObject("useorg").get("id"),index);
                this.getModel().setValue("nckd_haos_useorgdetail",uniqueList.get(i).getPkValue(),index);
            }
            this.initTree();
            this.getView().updateView("nckd_bentryentity");
            // 刷新表单

        }
    }

    // 分配树形结构
    private void initTree() {
        // 获取他的单据体，循环单据体
        DynamicObjectCollection entryentityCols = 	this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_bentryentity");
//        DynamicObjectCollection entryCopy = this.getModel().getEntryEntity("nckd_bentryentity");
        for (int i = 0; i < entryentityCols.size(); i++) {

            // 使用组织上级id
            Long aLong = entryentityCols.get(i).getDynamicObject("nckd_haos_useorgdetail").getLong("pid");
            if(null == aLong){
                entryentityCols.get(i).set("pid",0);
                continue;
            }
            Optional<DynamicObject> matchingObject = entryentityCols.stream()
                    .filter(obj -> {
                        long id = obj.getDynamicObject("nckd_haos_useorgdetail").getLong("id");
                        return id == aLong;
                    })
                    .findFirst();  // 找到第一个匹配的对象

            // 处理匹配的结果
            if (matchingObject.isPresent()) {
                // 处理匹配的 DynamicObject
                DynamicObject result = matchingObject.get();
                // 执行所需操作
                entryentityCols.get(i).set("pid",result.getPkValue());
            } else {
                // 处理未找到匹配的情况
                entryentityCols.get(i).set("pid",0);
            }
        }
    }


    //
    private void initEntry(Object org,Object staff) {
        // 初始化申请单据
        if(isNotEmpty(org) && isNotEmpty(staff)){
            // 填充申请单据数据
            DynamicObject staff1 = (DynamicObject) staff;
            BusinessDataServiceHelper.loadSingle(staff1.getPkValue(),"haos_staff");
            IFormView view = this.getView();
            AbstractFormDataModel model = (AbstractFormDataModel)this.getModel();
//            StaffInitEntryDataService.create(view, model).loadEntryData();
//            StaffFormService.create(view, model).setVisibleByUseOrg();
            List<Long> orgIds = Arrays.asList((Long) ((DynamicObject) org).getPkValue());
            // 编制维护id
            Long pkValue = (Long) staff1.getPkValue();
            List<Long> allSubordinateOrgs = OrgUnitServiceHelper.getAllSubordinateOrgs("02", orgIds, true);
            QFilter qFilter = new QFilter("useorgbo", "in", allSubordinateOrgs);

//            DynamicObject[] haosDutyorgdetails = StaffCommonService.queryStaffTempAndEffectData("haos_dutyorgdetail", pkValue, "dutyorg, staffcount");
            // 获取到当前组织和所有下级组织的使用组织明细， 然后再去获取对应组织的部门信息
            DynamicObject[] useOrgInfoArr = queryStaffTempAndEffectDataWithFilter("haos_useorgdetail",pkValue, qFilter, "id, useorg.id, useorg.enable, useorg, useorgbo, dutyorg.id, sequence, pid,staffdimension.fbasedataid, controlstrategy, elasticcontrol, elasticcount, yearstaff, halfyearstaff1, halfyearstaff2, quarterstaff1, quarterstaff2, quarterstaff3, quarterstaff4, monthstaff1, monthstaff2, monthstaff3, monthstaff4, monthstaff5, monthstaff6, monthstaff7, monthstaff8, monthstaff9, monthstaff10, monthstaff11, monthstaff12");
            List<Long> collect = Arrays.stream(useOrgInfoArr)
                    .map(dynamicObject -> (Long) dynamicObject.getPkValue()) // 使用 lambda 表达式
                    .collect(Collectors.toList());
            QFilter qFilter1 = new QFilter("dutyorg.masterid", "in", collect);
            //  先查出haos_useorgdetail 编制信息相关联的使用组织信息，把id，pid构建成map形式
            QFilter qFilter3 = new QFilter("staff.id", "=", pkValue);
//            DynamicObject[] load = BusinessDataServiceHelper.load("haos_useorgdetail", "id,pid,staff", new QFilter[]{qFilter3});

            BusinessDataServiceHelper.load("haos_dutyorgdetail","id",new QFilter[]{qFilter1});
            // 上级主键
            BusinessDataServiceHelper.load("haos_useorgdetail","id",new QFilter[]{new QFilter("pid", "in", collect)});
            // 编制类型为岗位的
            QFilter qFilter2 = new QFilter("staffdimension.fbasedataid.id", "=", 1010L);

            QFilter qFilter4 = new QFilter("pid", "in", collect);
            DynamicObject[] haosUseorgdetails = BusinessDataServiceHelper.load("haos_useorgdetail", "id,staff,useorg,bo,staffdimension", new QFilter[]{qFilter4, qFilter2, qFilter3});
            // 插入单据体表
            IDataModel model1 = this.getModel();
            model1.deleteEntryData("nckd_bentryentity");
            this.getModel().getDataEntity(true);
//            DynamicObjectCollection nckdBentryentity1 = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_bentryentity");

            DynamicObjectCollection entryentityCols = 	this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_bentryentity");
//            DynamicObject entryCol = entryentityCols.addNew();
//            entryCol.set("nckd_adjustapplic", "方式4");
            this.getView().updateView("nckd_bentryentity");
            for (int i = 0; i < haosUseorgdetails.length; i++) {
                DynamicObject dynamicObject = entryentityCols.addNew();

//                int dynamicObject1 = model1.createNewEntryRow("nckd_bentryentity");
                int index = model1.insertEntryRow("nckd_bentryentity", i);
                model1.setValue("nckd_adminorg",haosUseorgdetails[i].getDynamicObject("useorg").get("id"),index);
                model1.setValue("nckd_haos_useorgdetail",haosUseorgdetails[i].getPkValue(),index);

//                dynamicObject.set("id")
//                dynamicObject.set("nckd_adminorg",haosUseorgdetails[i].getDynamicObject("useorg").get("id"));
//                dynamicObject.set("nckd_haos_useorgdetail",haosUseorgdetails[i].getPkValue());
//                this.getModel().setValue("nckd_adminorg",haosUseorgdetails[i].getDynamicObject("useorg").get("id"),index);
//                this.getModel().setValue("nckd_haos_useorgdetail",haosUseorgdetails[i].getPkValue(),index);

                //获取父id
//                this.getModel().getValue("nckd_bentryentity", i);
//                this.getModel().setValue("","",dynamicObject1);
//                this.getModel().setValue("","",dynamicObject1);
            }
            this.getView().updateView("nckd_bentryentity");

/*
            DynamicObject entry = 	ORM.create().newDynamicObject(this.getModel().getEntryEntity("nckd_bentryentity").getDynamicObjectType());
            entry.set("kded_textfield1","44");
            entry.set("nckd_adjustapplic","10");
            int rowindex = this.getModel().createNewEntryRow("nckd_bentryentity", entry);
            */
//            this.getView().updateView("nckd_bentryentity");
//            this.getModel().get
//            DynamicObjectCollection entryentityCols = 	this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_bentryentity");
//            for (int i = 0; i < haosUseorgdetails.length; i++) {
//                DynamicObject dynamicObject = entryentityCols.addNew();
////                dynamicObject.set("nckd_adjustapplic","1111111111111111111111");
////                this.getView().updateView("nckd_bentryentity");
//
//
////                DynamicObject dynamicObject = sdentry.addNew();
////                int dynamicObject1 = model1.createNewEntryRow("nckd_bentryentity");
////                int index = model1.insertEntryRow("nckd_bentryentity", i);
//                // 树单据体插入子表行
//
//                // 行政组织
////                dynamicObject.set("id")
//                dynamicObject.set("nckd_adminorg",haosUseorgdetails[i].getDynamicObject("useorg").get("id"));
//                dynamicObject.set("nckd_haos_useorgdetail",haosUseorgdetails[i].getPkValue());
////                this.getModel().setValue("nckd_adminorg",haosUseorgdetails[i].getDynamicObject("useorg").get("id"),index);
////                this.getModel().setValue("nckd_haos_useorgdetail",haosUseorgdetails[i].getPkValue(),index);
//
//                //获取父id
////                this.getModel().getValue("nckd_bentryentity", i);
////                this.getModel().setValue("","",dynamicObject1);
////                this.getModel().setValue("","",dynamicObject1);
//            }
//            this.getView().updateView("nckd_bentryentity");
//            DynamicObjectCollection nckdBentryentity = this.getModel().getEntryEntity("nckd_bentryentity");
//            for (int j = 0; j < nckdBentryentity.size(); j++) {
//                DynamicObject dynamicObject = nckdBentryentity.get(j);
//                // 获取父节点key
////                Object pid = dynamicObject.getPkValue();
//                Object haospid = dynamicObject.getDynamicObject("nckd_haos_useorgdetail").getPkValue();
//                boolean flag = true;
//
//                // 获取下级数据
//                DynamicObject[] load2 = BusinessDataServiceHelper.load("haos_useorgdetail", "id,staff,useorg,bo,staffdimension,pid", new QFilter[]{new QFilter("pid", "=", haospid),qFilter2});
//                if(ObjectUtils.isEmpty(load2)){
//                    continue;
//                }
//                for (int n = 0; n < load2.length; n++) {
//                    int index = model1.insertEntryRow("nckd_bentryentity", j);
//                    this.getModel().setValue("nckd_adminorg",load2[n].getDynamicObject("useorg").get("id"),index);
//                    this.getModel().setValue("nckd_haos_useorgdetail",load2[n].getPkValue(),index);
//
//                }
//                // 循环后需要筛选出当前节点的所有子节点，然后循环插入
//
//
//            }


            // 获取使用组织id获取他下级id
            List<Long> collect2 = Arrays.stream(haosUseorgdetails)
                    .map(dynamicObject -> (Long) dynamicObject.getPkValue()) // 使用 lambda 表达式
                    .collect(Collectors.toList());
            // 下级
            List<Long> longs = collect2;
            boolean b = true;

            List<DynamicObject> combinedList = new ArrayList<>();
//            combinedList.addAll(Arrays.asList(haosUseorgdetails));
            while (b){
                DynamicObject[] load1 = BusinessDataServiceHelper.load("haos_useorgdetail", "id,staff,useorg,bo,staffdimension,pid", new QFilter[]{new QFilter("pid", "in", longs)});
                if(ObjectUtils.isEmpty(load1)){
                    b = false;
                }else{
                    combinedList.addAll(Arrays.asList(load1));
                    longs = Arrays.stream(load1)
                            .map(dynamicObject -> (Long) dynamicObject.getPkValue()) // 使用 lambda 表达式
                            .collect(Collectors.toList());
                }
            }
            // 获取所有下级目录
            DynamicObject[] combinedArray = combinedList.toArray(new DynamicObject[0]);


            // 构建属性单据体
//            this.getModel().deleteEntryData("bentryentity");



            //haosUseorgdetails[1].getDynamicObject("useorg");
//            haosUseorgdetails[0].get("bo");
        }else{
            // 清理单据体和子单据体数据
            this.getModel().deleteEntryData("nckd_bentryentity");
            this.getView().updateView("nckd_bentryentity");
        }
    }



    public static boolean isNotEmpty(Object  key) {
        // 基础资料判空
        if(ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(((DynamicObject)key).getDataStorage())){
            return false;
        }
        return true;
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent e) {

        String fieldKey = e.getProperty().getName();
        if (StringUtils.equals(fieldKey, "nckd_haos_staff")){
            ListShowParameter showParameter = (ListShowParameter)e.getFormShowParameter();
            //是否展示审核的改为false
            showParameter.setShowApproved(false);
        }


    }

    @Override
    public void registerListener(EventObject e) {
        BasedataEdit fieldEdit = this.getView().getControl("nckd_haos_staff");//基础资料字段标识
        fieldEdit.addBeforeF7SelectListener(this);
    }



    public static DynamicObject[] queryStaffTempAndEffectDataWithFilter(String entryEntityName, Long staffId, QFilter otherFilter, String selectFields) {
        HRBaseServiceHelper serviceHelper = new HRBaseServiceHelper(entryEntityName);
        QFilter staffIdFilter = new QFilter("staff", "=", staffId);
        QFilter dataStatusFilter = new QFilter("datastatus", "=", EventOperateEnums.CHANGE_BEFORE_STATUS_EFFECTING.getValue() );
        return serviceHelper.query(selectFields, new QFilter[]{staffIdFilter, dataStatusFilter, otherFilter}, "sequence asc");
    }


}
