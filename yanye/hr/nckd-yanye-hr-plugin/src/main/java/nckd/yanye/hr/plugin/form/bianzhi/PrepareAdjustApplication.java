package nckd.yanye.hr.plugin.form.bianzhi;

import com.alibaba.druid.util.StringUtils;
import kd.bos.algo.DataSet;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.DBRoute;
import kd.bos.entity.datamodel.AbstractFormDataModel;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.DispatchServiceHelper;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.hr.hbp.common.constants.newhismodel.EventOperateEnums;
import kd.hr.hbp.common.model.org.staff.StaffResponse;
import kd.hr.hbp.common.util.HRDBUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Module           :HR中控服务云-HR基础组织-人力编制-编制调整申请
 * Description      :编制调整申请单据插件
 *
 * @author guozhiwei
 * @date  2024/9/4 9：40
 * 标识 nckd_preadjustapplic
 */


public class PrepareAdjustApplication extends AbstractBillPlugIn implements BeforeF7SelectListener {



    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        // 监听 nckd_haos_staff 的变化
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        IDataModel model = this.getModel();
        switch (key) {
            case "nckd_haos_staff":
                this.getModel().deleteEntryData("nckd_centryentity");
                this.getModel().deleteEntryData("nckd_bentryentity");
                this.initEntry(model.getValue("org"),newValue);
                break;
            default:
                break;
        }

    }
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
            // 组织id
            Long orgid = (Long) ((DynamicObject) org).getPkValue();
            // 编制维护id
            Long pkValue = (Long) staff1.getPkValue();
            Date date = new Date();

//            StringBuilder sql = new StringBuilder("SELECT a.fid AS id, a.fboid AS boid, A.fparentid as parentorg, t.flevel as level1,  t.fstructlongnumber as structlongnumber FROM T_HAOS_ADMINORG A LEFT JOIN T_HAOS_ADMINSTRUCT T ON A.fboid = T.fadminorgid AND T.fiscurrentversion = '0' AND T.fdatastatus = '1' AND T.fstructprojectid = 1010 AND T.finitstatus = '2' AND T.fbsed <= ? AND T.fbsled >= ? AND T.fenable = '1' LEFT JOIN T_HAOS_ORGSORTCODE S ON S.FADMINORGID = A.fboid AND S.fiscurrentversion = '0' AND S.fdatastatus = '1' AND S.finitstatus = '2' AND S.fbsed <= ? AND S.fbsled >= ? AND S.fenable = '1' WHERE A.fiscurrentversion = '0' AND A.fdatastatus = '1' AND A.finitstatus = '2' AND A.fbsed <= ? AND A.fbsled >= ? AND A.fenable = '1' AND (A.forgid = ? OR A.fboid = ?)");


            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(date);

            // 拼接 SQL 查询
            StringBuilder sql = new StringBuilder("SELECT a.fid AS id, a.fboid AS boid, A.fparentid as parentorg, "
                    + "t.flevel as level1, t.fstructlongnumber as structlongnumber, "
                    + "(select top 1 N.fstaffcount  from t_haos_adminorg M left join t_haos_dutyorgdetail N on N.fdutyorgid = M.fid "
                    + "where M.fenable ='1'  and M.fboid  =a.fboid "
                    + "AND M.fbsed <= '" + dateStr + "' AND M.fbsled >= '" + dateStr + "'  "
                    + "order by M.fhisversion desc) as staffcount "
                    + "FROM T_HAOS_ADMINORG A "
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
            Object[] param = new Object[]{(Long) orgid};
            DataSet dataSet = HRDBUtil.queryDataSet("haos_adminOrgHisSearch", new DBRoute("hr"), sql.toString(), param);

            ORM orm = ORM.create();
            DynamicObjectCollection retDynCol = orm.toPlainDynamicObjectCollection(dataSet);
            List<Long> collect2 = new ArrayList<Long>();
            for (int i = 0; i < retDynCol.size(); i++) {
                int index = this.getModel().insertEntryRow("nckd_bentryentity", i);
                this.getModel().setValue("nckd_adminorg",retDynCol.get(i).get("BOID"),index);
                this.getModel().setValue("nckd_parentorg",retDynCol.get(i).get("parentorg"),index);
                this.getModel().setValue("nckd_brealnum",retDynCol.get(i).get("staffcount"),index);
                collect2.add((Long) retDynCol.get(i).get("BOID"));
            }
            Object[] objects = new Object[2];
            objects[0] = new Date();
            objects[1] = collect2;
            StaffResponse<Map<String, Map<String, Object>>> staffResponse = (StaffResponse<Map<String, Map<String, Object>>>) DispatchServiceHelper.invokeService("kd.hrmp.haos.servicehelper","haos","IHAOSStaffService","queryUseStaffInfo",objects);

            this.initTree(staffResponse);
            this.getView().updateView("nckd_bentryentity");
            this.getView().updateView("nckd_centryentity");

            System.out.println("初始化申请单据成功");

            //sql2.append( "AND T.finitstatus = '2' AND T.fenable = '1' AND T.fbsed <= " + dateStr + " AND T.fbsled >= " + dateStr +" ");

        }
    }


    @Override
    public void itemClick(ItemClickEvent evt) {
        String itemKey = evt.getItemKey();
        if (StringUtils.equalsIgnoreCase("bentryentity", itemKey)) {
//            this.subEntryEntitySetVal1();
            // 点击事件之后需更新单据体&子单据体控件视图
            this.getView().updateView("nckd_bentryentity");
            this.getView().updateView("nckd_centryentity");
        }
        super.itemClick(evt);
    }


    // 分配树形结构
    private void initTree(StaffResponse<Map<String, Map<String, Object>>> staffResponse) {
        // 获取他的单据体，循环单据体
        DynamicObjectCollection entryentityCols = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_bentryentity");
        for (int i = 0; i < entryentityCols.size(); i++) {
            // todo 给单据体添加子单据体数据

//
            DynamicObject enObj = entryentityCols.get(i);
            // 子单据体标识nckd_centryentity
            DynamicObjectCollection cntryEntity = enObj.getDynamicObjectCollection("nckd_centryentity");
            DynamicObject dynamicObject = new DynamicObject(cntryEntity.getDynamicObjectType());
//            DynamicObject dynamicObject = cntryEntity.addNew();
            dynamicObject.set("nckd_postadjustapplic","测试");
            cntryEntity.add(dynamicObject);

            // 使用组织上级id

            Long aLong = (Long) entryentityCols.get(i).get("nckd_parentorg");
            DynamicObject centrydynamicObject = entryentityCols.get(i);
            Map<String, Object> nckdAdminorg = staffResponse.getData().get(centrydynamicObject.getDynamicObject("nckd_adminorg").getPkValue());
            if(ObjectUtils.isNotEmpty(nckdAdminorg)){
                int staffNum = (int)nckdAdminorg.get("staffNum");
                if( staffNum != 0){
                    centrydynamicObject.set("nckd_brealnum",staffNum);
                }
            }


            if(null == aLong){
                centrydynamicObject.set("pid",0);
//                entryentityCols.get(i).set("pid",0);
                continue;
            }
            Optional<DynamicObject> matchingObject = entryentityCols.stream()
                    .filter(obj -> {
                        long id = obj.getDynamicObject("nckd_adminorg").getLong("id");
                        return id == aLong;
                    })
                    .findFirst();  // 找到第一个匹配的对象

            // 处理匹配的结果
            if (matchingObject.isPresent()) {
                // 处理匹配的 DynamicObject
                DynamicObject result = matchingObject.get();
                // 执行所需操作
                centrydynamicObject.set("pid",result.getPkValue());
//                entryentityCols.get(i).set("pid",result.getPkValue());
            } else {
                // 处理未找到匹配的情况
                centrydynamicObject.set("pid",0);
            }
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
