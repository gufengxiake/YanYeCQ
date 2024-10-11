package nckd.yanye.hr.plugin.form.zhaoping;


import kd.bos.algo.DataSet;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.DBRoute;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.LoadDataEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.hr.hbp.common.util.HRDBUtil;
import org.apache.commons.lang3.ObjectUtils;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Module           :人才供应云-招聘直通车-首页-年度招聘计划
 * Description      :年度招聘计划单据插件
 *
 * @author guozhiwei
 * @date  2024/9/19 15：12
 * 标识 nckd_yearcasreplan
 */



public class YearcrapplyPlanFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {

    // 公司类型
    private final List<String> COMPANY_LIST = Arrays.asList(new String[]{"1020_S","1050_S","1060_S","1070_S"});

    private final List<String> COMPANY_LIST2 = Arrays.asList(new String[]{"Orgform01","Orgform01-100","Orgform02","Orgform03"});

    // 定义日期格式
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");



    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
    }

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        DynamicObject org = (DynamicObject) this.getModel().getValue("org");
        //  组织id
        Long pkValue = (Long) org.getPkValue();
        /*
        List<Long> longs = new ArrayList<Long>();
        longs.add(pkValue);
        QFilter qFilter = new QFilter("boid", QCP.equals, pkValue);
        // 获取组织历史查询
        DynamicObjectCollection query = QueryServiceHelper.query("haos_adminorgdetail", "id,boid,hisversion", new QFilter[]{qFilter}, "hisversion desc");
        if(ObjectUtils.isNotEmpty(query)){
            long boid = query.get(0).getLong("id");
            QFilter qFilter1 = new QFilter("dutyorg.id", QCP.equals, boid);
            DynamicObject haosDutyorgdetail = BusinessDataServiceHelper.loadSingle( "haos_dutyorgdetail","id,dutyorg,staffcount",new QFilter[]{qFilter1});
            int staffcount = ObjectUtils.isNotEmpty(haosDutyorgdetail) ? haosDutyorgdetail.getInt("staffcount"):0;
            this.getModel().setValue("nckd_sftaffcount",staffcount);

//            QFilter qFilter1 = new QFilter("useorgbo", QCP.equals, boid);
//            DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("haos_useorgdetail", "id,useorgbo,useorg.id,yearstaff", new QFilter[]{qFilter1});
//            if(ObjectUtils.isNotEmpty(dynamicObject)){
//                this.getModel().setValue("nckd_sftaffcount",dynamicObject.get("yearstaff"));
//            }
        }
        */
        updateStaffCount();
        this.getModel().setValue("nckd_relnum",getStaffCount(org.getPkValue()));

    }
    // 获取组织编制人数
    private void updateStaffCount(){

        DynamicObject org = (DynamicObject) this.getModel().getValue("org");
        //  组织id
        Long pkValue = (Long) org.getPkValue();
        List<Long> longs = new ArrayList<Long>();
        longs.add(pkValue);
        QFilter qFilter = new QFilter("boid", QCP.equals, pkValue);
        // 获取组织历史查询
        DynamicObjectCollection query = QueryServiceHelper.query("haos_adminorgdetail", "id,boid,hisversion", new QFilter[]{qFilter}, "hisversion desc");
        if(ObjectUtils.isNotEmpty(query)){
            long boid = query.get(0).getLong("id");
            // 获取填写的年度 nckd_year
            Object nckdYear =  this.getModel().getValue("nckd_year");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");

            if(ObjectUtils.isEmpty(nckdYear)){
                nckdYear = new Date();
            }else{
                nckdYear = (Date) nckdYear;
            }
            String dateStr = sdf.format(nckdYear);
//            QFilter qFilter2 = new QFilter("staff.year", QCP.like, dateStr + "%");
            QFilter qFilter1 = new QFilter("dutyorg.id", QCP.equals, boid);

            DynamicObject[] haosDutyorgdetail = BusinessDataServiceHelper.load( "haos_dutyorgdetail","id,dutyorg,staff,staffcount",new QFilter[]{qFilter1});
            if(ObjectUtils.isNotEmpty(haosDutyorgdetail)){
                for (DynamicObject dynamicObject : haosDutyorgdetail) {
                    String format = sdf.format(dynamicObject.getDate("staff.year"));
                    if(dateStr.equals(format)){
                        int staffcount = dynamicObject.getInt("staffcount");
                        this.getModel().setValue("nckd_sftaffcount",staffcount);
                        return;
                    }
                }
            }
            this.getModel().setValue("nckd_sftaffcount",null);

//            int staffcount = ObjectUtils.isNotEmpty(haosDutyorgdetail) ? haosDutyorgdetail.getInt("staffcount"):null;
//            this.getModel().setValue("nckd_sftaffcount",staffcount);

        }

    }

    @Override
    public void loadData(LoadDataEventArgs e) {
        super.loadData(e);
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        int iRow = changeData[0].getRowIndex();
        switch (key) {
            case "nckd_recruitnum":
                // 人数字段统计
                setSumRecruitnum(newValue,iRow);
                break;
            default:
                break;
        }

    }

    // 公司，部门，岗位选择过滤
    @Override
    public void beforeF7Select(BeforeF7SelectEvent e) {
        String fieldKey = e.getProperty().getName();
        switch (fieldKey){
            case "org":
                ListShowParameter showParameter4 = (ListShowParameter)e.getFormShowParameter();
                // 去除部门
                QFilter qFilter4 = new QFilter("orgpattern.number", "in", COMPANY_LIST2);
                showParameter4.getListFilterParameter().setFilter(qFilter4);
                break;
            default:
                break;
        }
    }



    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 添加对分录按钮的监听
        this.addItemClickListeners("advcontoolbarap");
        BasedataEdit fieldEdit = this.getView().getControl("org");
        fieldEdit.addBeforeF7SelectListener(this);
    }


    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        String key = evt.getItemKey();
        //监听分录刷新招聘申请按钮
        if (key.equals("nckd_advconbaritemap1")) {
            DynamicObjectCollection entryentity = this.getModel().getDataEntity(true).getDynamicObjectCollection("entryentity");
            // 获取年度
            Object nckdYear = this.getModel().getValue("nckd_year");
            if(ObjectUtils.isEmpty(nckdYear)){
                this.getView().showErrorNotification("请先填写年度");
                return;
            }
            entryentity.clear();
            // 删除分录数据，然后使用表单组织去拉去年度招聘计划中的数据，然后获取他的下级分录，然后添加到本单据分录中，然后刷新本单分录
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            QFilter nckdYear1 = new QFilter("nckd_year", QCP.equals, nckdYear);
            QFilter qBillstatus = new QFilter("billstatus", QCP.equals, "C");
            List<Long> longs = new ArrayList<Long>();
            longs.add((Long) org.getPkValue());
            // 获取组织历史查询
            List<Long> allSubordinateOrgs = OrgUnitServiceHelper.getAllSubordinateOrgs("01", longs, true);
            QFilter qFilter = new QFilter("org.id", QCP.in, allSubordinateOrgs);
            // 年度招聘计划数据
            DynamicObject[] loads = BusinessDataServiceHelper.load("nckd_yearapply", "id,org,org.id,billstatus,entryentity,entryentity.nckd_recruitorg," +
                    "entryentity.nckd_recruitpost,entryentity.nckd_recruitnum,entryentity.nckd_majortype,entryentity.nckd_qualification," +
                    "entryentity.nckd_payrange,entryentity.nckd_employcategory,entryentity.nckd_recruittype,nckd_year", new QFilter[]{nckdYear1,qFilter,qBillstatus});
            if(ObjectUtils.isEmpty(loads)){
                return;
            }
            ORM orm = ORM.create();
            for (DynamicObject dynamicObject : loads) {
                // 获取招聘计划的下级分录
                DynamicObjectCollection entryentity1 = dynamicObject.getDynamicObjectCollection("entryentity");
                if(ObjectUtils.isEmpty(entryentity1)){
                    continue;
                }
                for (DynamicObject object : entryentity1) {
                    DynamicObject dynamicObjentryrow = entryentity.addNew();
                    // 招聘单位
                    dynamicObjentryrow.set("nckd_recruitcompany",dynamicObject.getDynamicObject("org"));
                    dynamicObjentryrow.set("nckd_recruitorg",object.getDynamicObject("nckd_recruitorg"));
                    dynamicObjentryrow.set("nckd_recruitpost",object.get("nckd_recruitpost"));
                    dynamicObjentryrow.set("nckd_recruitnum",object.get("nckd_recruitnum"));
                    StringBuilder sqlBuilder = new StringBuilder("SELECT m.fbasedataid FROM tk_nckd_yearcrapp_major m WHERE m.fentryid = ?");

                    Object[] param = new Object[]{object.getPkValue()};
                    DataSet rows = HRDBUtil.queryDataSet("tk_nckd_yearcrapp_major", new DBRoute("tsc"), sqlBuilder.toString(), param);
                    // 多选基础资料key
                    DynamicObjectCollection retDynCol = orm.toPlainDynamicObjectCollection(rows);
                    MainEntityType type = EntityMetadataCache.getDataEntityType("nckd_specialityclass");
                    DynamicObjectCollection newColList = dynamicObjentryrow.getDynamicObjectCollection("nckd_majortype");

                    if(ObjectUtils.isNotEmpty(retDynCol)){
                        // 创建多选基础资料，
                        for (DynamicObject dynamicObject1 : retDynCol) {
                            DynamicObject dynamicObject2 = newColList.addNew();
                            dynamicObject2.set("fbasedataId", BusinessDataServiceHelper.loadSingle(dynamicObject1.get("fbasedataid"),type));
                        }
                    }
                    dynamicObjentryrow.set("nckd_qualification",object.get("nckd_qualification"));
                    dynamicObjentryrow.set("nckd_payrange",object.get("nckd_payrange"));
                    dynamicObjentryrow.set("nckd_employcategory",object.get("nckd_employcategory"));
                    dynamicObjentryrow.set("nckd_recruittype",object.get("nckd_recruittype"));
                    dynamicObjentryrow.set("nckd_yearapplyid",dynamicObject.getPkValue());

                }
            }
            // 计算人数
            setSumRecruitnum(0,-1);
            getView().updateView("entryentity");

        }
    }


    // 校验
    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate opreate = (FormOperate) args.getSource();
        switch (opreate.getOperateKey()) {
            case "save":
                if(!planValidator((DynamicObject) this.getModel().getValue("org"))){
                    args.setCancel(true);
                }
                break;
            default:
                break;
        }
    }

    private boolean planValidator(DynamicObject org){
        Object nckdYear = this.getModel().getValue("nckd_year");
        QFilter nckdYear1 = new QFilter("nckd_year", QCP.equals, nckdYear);
        QFilter qBillstatus = new QFilter("billstatus", QCP.equals, "C");
        // 获取所有下级组织 org.getPkValue()
        String selectFields = "id,org,org.id,org.name,longnumber,parent,orgpattern";
        QFilter numberFilter = new QFilter("number", "=", "01");
        DynamicObject viewObj = BusinessDataServiceHelper.loadSingleFromCache("bos_org_viewschema", "id", new QFilter[]{numberFilter});
        QFilter viewFilter = new QFilter("view", "=", viewObj.getPkValue());
        QFilter viewFilter2 = new QFilter("parent.id", "=", org.getPkValue());
        QFilter[] filters = new QFilter[]{viewFilter,viewFilter2};
        // 获取组织所有直属下级组织
        DynamicObject[] bosOrgStructures = BusinessDataServiceHelper.load("bos_org_structure", selectFields, filters);
        if(ObjectUtils.isNotEmpty(bosOrgStructures)){
            // 存在下级组织，使用下级组织查询
            Set<Long> allSubOrgIds = new HashSet(10000);
            for (int i = 0; i < bosOrgStructures.length; i++) {
                allSubOrgIds.add((Long) bosOrgStructures[i].get("org.id"));
            }
            QFilter qFilter4 = new QFilter("orgpattern.number", "in", COMPANY_LIST2).and("id", QCP.in, allSubOrgIds);
            DynamicObject[] bosOrgs = BusinessDataServiceHelper.load("bos_org", "id,name,orgpattern", new QFilter[]{qFilter4});
            Set<Long> allSubOrgIds2 = new HashSet(10000);
            for (DynamicObject bosOrg : bosOrgs) {
                allSubOrgIds2.add((Long) bosOrg.get("id"));
            }
            // 获取所有下级组织的招聘计划
            QFilter bosOrgFilter = new QFilter("org.id", QCP.in, allSubOrgIds2);
            StringBuilder errBuilder = new StringBuilder();
            DynamicObject[] nckdYearcasreplans = BusinessDataServiceHelper.load("nckd_yearcasreplan", "id,org,", new QFilter[]{nckdYear1,bosOrgFilter, qBillstatus});
            if(ObjectUtils.isEmpty(nckdYearcasreplans)){
                errBuilder.append("直属下级组织未配置完年度招聘计划");
            }else{
                Map<Object, DynamicObject> resultMap = Arrays.stream(nckdYearcasreplans)
                        .collect(Collectors.toMap(
                                obj -> obj.get("org.id"), // 使用 org.id 作为 key
                                obj -> obj, // 使用 DynamicObject 本身作为 value
                                (existing, replacement) -> existing // 如果键冲突，保留现有值
                        ));
                for (DynamicObject bosOrgStructure : bosOrgStructures) {
                    DynamicObject dynamicObject = resultMap.get(bosOrgStructure.get("org.id"));
                    if(ObjectUtils.isEmpty(dynamicObject)){
                        errBuilder.append("组织["+bosOrgStructure.get("org.name")+"]未配置完年度招聘计划");
                    }
                }
            }
            if(ObjectUtils.isNotEmpty(errBuilder)){
                // 未通过校验
                this.getView().showErrorNotification(errBuilder.toString());
                return false;
            }

        }

        return true;
    }



    public static boolean isNotEmpty(Object  key) {
        // 基础资料判空
        if(ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(((DynamicObject)key).getDataStorage())){
            return false;
        }
        return true;
    }


    // 获取组织对应人员编制总数（包含下级）
    public static int getStaffCount(Object orgId){

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(date);

        // 拼接 SQL 查询
        // 拼接 SQL 查询 haos_stafforgempcount
        StringBuilder sql = new StringBuilder("SELECT DISTINCT T.fstructlongnumber,ST.fcount as count "
                + "FROM T_HAOS_ADMINORG A "
                + "LEFT JOIN T_HAOS_ADMINSTRUCT T ON a.fboid = T.fadminorgid "
                + "left join T_HAOS_STAFFORGEMPCOUNT ST on A.fboid = ST.fuseorgboid "
                + "WHERE A.fiscurrentversion = '0' AND A.fdatastatus = '1' AND A.finitstatus = '2' "
                + "AND A.fbsed <= '" + dateStr + "' AND A.fbsled >= '" + dateStr + "' "
                + "AND A.fenable = '1' "
                + "AND ( T.fstructlongnumber LIKE ( select top 1 concat(F.fstructlongnumber,'%')  from  T_HAOS_ADMINSTRUCT F where  F.fadminorgid = ? "
                + "AND F.fiscurrentversion = '0' AND F.fdatastatus = '1' AND F.fstructprojectid = 1010 "
                + "AND F.finitstatus = '2' AND F.fbsed <= '" + dateStr + "' "
                + "AND F.fbsled >= '" + dateStr + "' AND F.fenable = '1' "
                +") " +") "
        );
        Object[] param = new Object[]{(Long) orgId};
        DataSet dataSet = HRDBUtil.queryDataSet("haos_adminOrgHisSearch", new DBRoute("hr"), sql.toString(), param);

        ORM orm = ORM.create();
        DynamicObjectCollection retDynCol = orm.toPlainDynamicObjectCollection(dataSet);
        // 获取实际人数
        AtomicInteger nckdRellownum2 = new AtomicInteger(0);
        retDynCol.forEach(dynObj -> {
            int count = dynObj.getInt("count");
            if(ObjectUtils.isNotEmpty(count)){
                nckdRellownum2.addAndGet(count);
            }
        });
        return nckdRellownum2.get();
    }

    public void setSumRecruitnum(Object newValue,int row){
        // 计算招聘人数,获取出所有分录，然后统计所有分录的招聘人数累加
        DynamicObjectCollection entryEntity = this.getModel().getEntryEntity("entryentity");
        int num = 0;
        for (int i = 0; i < entryEntity.size(); i++) {
            if(row == i){
                num = num + (Integer) newValue;
            }else{
                num = num + (Integer) entryEntity.get(i).get("nckd_recruitnum");
            }
        }
        this.getModel().setValue("nckd_applynum", num);
    }

}
