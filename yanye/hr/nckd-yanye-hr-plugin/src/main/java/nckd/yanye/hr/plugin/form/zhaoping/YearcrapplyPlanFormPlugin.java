package nckd.yanye.hr.plugin.form.zhaoping;


import kd.bos.algo.DataSet;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.DBRoute;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.LoadDataEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.hr.hbp.common.util.HRDBUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
//        DispatchServiceHelper.invokeService("kd.hrmp.haos.servicehelper","haos","IHAOSStaffService","queryUseStaffInfo",objects);
    }

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
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
            QFilter qFilter1 = new QFilter("dutyorg.id", QCP.equals, boid);
            DynamicObject haosDutyorgdetail = BusinessDataServiceHelper.loadSingle( "haos_dutyorgdetail","id,dutyorg,staffcount",new QFilter[]{qFilter1});
            this.getModel().setValue("nckd_sftaffcount",haosDutyorgdetail.get("staffcount"));
        }
        this.getModel().setValue("nckd_relnum",getStaffCount(org.getPkValue()));

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
            QFilter nckdYear1 = new QFilter("org.id", QCP.equals, org.getPkValue()).and("nckd_year", QCP.equals, nckdYear);
            // 年度招聘计划数据
            DynamicObject[] loads = BusinessDataServiceHelper.load("nckd_yearapply", "id,org,org.id,entryentity,entryentity.nckd_recruitorg,entryentity.nckd_recruitpost,entryentity.nckd_recruitnum,entryentity.nckd_majortype,entryentity.nckd_qualification,entryentity.nckd_payrange,entryentity.nckd_employcategory,entryentity.nckd_recruittype,nckd_year", new QFilter[]{nckdYear1});
            if(ObjectUtils.isEmpty(loads)){
                return;
            }
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
                    dynamicObjentryrow.set("nckd_majortype",object.get("nckd_majortype"));
                    dynamicObjentryrow.set("nckd_qualification",object.get("nckd_qualification"));
                    dynamicObjentryrow.set("nckd_payrange",object.get("nckd_payrange"));
                    dynamicObjentryrow.set("nckd_employcategory",object.get("nckd_employcategory"));
                    dynamicObjentryrow.set("nckd_recruittype",object.get("nckd_recruittype"));
//                    entryentity.add(dynamicObjentryrow);
                }
            }
//            getModel().endInit();
            // 计算人数
            setSumRecruitnum(0,-1);
            getView().updateView("entryentity");

        }
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
        StringBuilder sql = new StringBuilder("SELECT a.fid AS id, a.fboid AS boid, A.fparentid as parentorg, "
                + "t.flevel as level1, t.fstructlongnumber as structlongnumber, ST.fcount as count,ST.fcontainsubcount as containsubcount, "
//                    + "(select top 1 N.fstaffcount  from t_haos_adminorg M where a.fparentid = M.fboid ) as "
                + "(select top 1 N.fstaffcount  from t_haos_adminorg M left join t_haos_dutyorgdetail N on N.fdutyorgid = M.fid "
                + "where M.fenable ='1'  and M.fboid  =a.fboid "
                + "AND M.fbsed <= '" + dateStr + "' AND M.fbsled >= '" + dateStr + "'  "
                + "order by M.fhisversion desc) as staffcount "
                + "FROM T_HAOS_ADMINORG A "
                + "LEFT JOIN T_HAOS_STAFFORGEMPCOUNT ST on A.fboid = ST.fuseorgboid "
                + "LEFT JOIN T_HAOS_ADMINSTRUCT T ON A.fboid = T.fadminorgid "
                + "AND T.fiscurrentversion = '0' AND T.fdatastatus = '1' AND T.fstructprojectid = 1010 "
                + "AND T.finitstatus = '2' AND T.fbsed <= '" + dateStr + "' "
                + "AND T.fbsled >= '" + dateStr + "' AND T.fenable = '1' "
                + "LEFT JOIN T_HAOS_ORGSORTCODE S ON S.FADMINORGID = A.fboid "
                + "AND S.fiscurrentversion = '0' AND S.fdatastatus = '1' AND S.finitstatus = '2' "
                + "AND S.fbsed <= '" + dateStr + "' AND S.fbsled >= '" + dateStr + "' "
                + "AND S.fenable = '1' "
                + "LEFT JOIN T_HAOS_DUTYORGDETAIL M ON M.fdutyorgid = A.fboid "
                + "WHERE A.fiscurrentversion = '0' AND A.fdatastatus = '1' AND A.finitstatus = '2' "
                + "AND A.fbsed <= '" + dateStr + "' AND A.fbsled >= '" + dateStr + "' "
                + "AND A.fenable = '1' "
                + "AND ( T.fstructlongnumber LIKE ( select top 1 concat(F.fstructlongnumber,'%')  from  T_HAOS_ADMINSTRUCT F where  F.fadminorgid = ?) " +") "
                + "ORDER BY S.fsortcode");
        Object[] param = new Object[]{(Long) orgId};
        DataSet dataSet = HRDBUtil.queryDataSet("haos_adminOrgHisSearch", new DBRoute("hr"), sql.toString(), param);

        ORM orm = ORM.create();
        DynamicObjectCollection retDynCol = orm.toPlainDynamicObjectCollection(dataSet);
        // 获取实际人数
        AtomicInteger nckdRellownum2 = new AtomicInteger(0);
        retDynCol.forEach(dynObj -> {
            nckdRellownum2.addAndGet(dynObj.getInt("count"));
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
