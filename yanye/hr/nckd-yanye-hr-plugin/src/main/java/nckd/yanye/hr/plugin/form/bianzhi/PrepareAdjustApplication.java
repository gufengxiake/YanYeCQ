package nckd.yanye.hr.plugin.form.bianzhi;


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
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.control.events.RowClickEvent;
import kd.bos.form.control.events.RowClickEventListener;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.DispatchServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.hr.haos.business.servicehelper.OrgBatchBillHelper;
import kd.hr.haos.common.constants.masterdata.AdminOrgConstants;
import kd.hr.hbp.common.model.org.staff.StaffResponse;
import kd.hr.hbp.common.util.DatePattern;
import kd.hr.hbp.common.util.DateUtils;
import kd.hr.hbp.common.util.HRDBUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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


public class PrepareAdjustApplication extends AbstractBillPlugIn implements BeforeF7SelectListener, RowClickEventListener {


    private static List<String> approval = new ArrayList<>();

    private final List<String> COMPANY_LIST = Arrays.asList(new String[]{"Orgform01","Orgform01-100","Orgform02","Orgform03"});

    private static final String ADJUSTTYPE_ADD = "A";
    private static final String ADJUSTTYPE_MINUS = "B";

    // 编制信息单
    private static final String NCKD_HAOS_STAFF = "nckd_haos_staff";
    // 组织 直属调整人数
    private static final String NCKD_ADJUSTDIRE = "nckd_adjustdire";
    // 组织 直属调整后人数
    private static final String NCKD_RELBDIRECTNUM = "nckd_relbdirectnum";
    // 组织 含下级调整人数
    private static final String NCKD_ADJUSTNUM = "nckd_adjustnum";
    // 组织 调整类型
    private static final String NCKD_ADJUSTTYPE = "nckd_adjusttype";
    // 组织 现有编制人数
    private static final String NCKD_BREALNUM = "nckd_brealnum";
    // 组织 调整后编制人数
    private static final String NCKD_ADJUSTLATENUM = "nckd_adjustlatenum";
    // 岗位调整类型
    private static final String NCKD_POSTADJUSTTYPE = "nckd_postadjusttype";
    // 组织 现在编制直属人数
    private static final String NCKD_BDIRECTNUM = "nckd_bdirectnum";
    // 岗位 现有含下级编制人数
    private static final String NCKD_RELCYEARSTAFF = "nckd_relcyearstaff";
    // 岗位调整人数
    private static final String NCKD_POSTADJUSTNUM = "nckd_postadjustnum";
    // 岗位调整后编制人数
    private static final String NCKD_POSTADJUSTLATENUM = "nckd_postadjustlatenum";




    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        // 监听 nckd_haos_staff 的变化
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        int iRow = changeData[0].getRowIndex();
        IDataModel model = this.getModel();
        switch (key) {
            case NCKD_HAOS_STAFF:
                this.getModel().deleteEntryData("nckd_centryentity");
                this.getModel().deleteEntryData("nckd_bentryentity");
                this.initEntry(model.getValue("org"),newValue);
                break;
//            case NCKD_ADJUSTNUM:
//                // 调整人数
////                this.adjustnumChange(newValue,oldValue,iRow);
//                break;
//            case NCKD_ADJUSTDIRE:
//                // 直属调整人数
////                this.adjustdireChange(newValue,oldValue,iRow);
//                break;
//            case NCKD_ADJUSTTYPE:
//                //调整类型
////                this.adjusttype(newValue,oldValue,iRow);
//                break;
//            case NCKD_POSTADJUSTTYPE:
//                //岗位调整类型
////                this.postadjusttypeChange(newValue,oldValue,iRow);
//                break;
//            case NCKD_POSTADJUSTNUM:
//                // 岗位调整人数
////                this.postadjustnumChange(newValue,oldValue,iRow);
//                break;

            case "nckd_adjustlatenum":
                // 组织调整后含下级编制人数
                this.nckdadjustlatenum(newValue,oldValue,iRow);
                break;
            case "nckd_relbdirectnum":
                // 组织调整后直属编制人数
                this.relbdirectnum(newValue,oldValue,iRow);
                break;
            case "nckd_postadjustlatenum":
                // 岗位调整后直属编制人数
                this.adjustlatenum(newValue,oldValue,iRow);
                break;
            default:
                break;
        }

    }
    // 岗位计算调整人数
    private void adjustlatenum(Object newValue,Object oldValue,int row){

        if(ObjectUtils.isEmpty(newValue)){
            // 调整后编制人数为空，给调整人数也为空
            this.getModel().setValue("nckd_postadjustnum",null,row);
        }else{
            // 调整后编制人数不为空，计算调整人数,获取现有编制人数
            int relcyearstaff = getIntValue("nckd_relcyearstaff", row);
            int newValueInt = (int) newValue;
            if(newValueInt<0){
                this.getView().showErrorNotification("编制人数不能小于0！");
                this.getModel().setValue("nckd_postadjustlatenum",oldValue,row);
                return;
            }
            this.getModel().setValue("nckd_postadjustnum",newValueInt - relcyearstaff,row);
        }
    }

    // 组织调整后直属编制人数
    private void relbdirectnum(Object newValue,Object oldValue,int row){
        if(ObjectUtils.isEmpty(newValue)){
            // 调整后编制人数为空，给调整人数也为空
            this.getModel().setValue("nckd_adjustdire",null,row);
        }else{
            int relcyearstaff = getIntValue("nckd_adjustdire", row);
            int newValueInt = (int) newValue;
            if(newValueInt<0){
                this.getView().showErrorNotification("编制人数不能小于0！");
                this.getModel().setValue("nckd_relbdirectnum",oldValue,row);
                return;
            }
            this.getModel().setValue("nckd_adjustdire",newValueInt - relcyearstaff,row);
        }
    }

    // 组织调整后直属编制人数
    private void nckdadjustlatenum(Object newValue,Object oldValue,int row){
        if(ObjectUtils.isEmpty(newValue)){
            // 调整后编制人数为空，给调整人数也为空
            this.getModel().setValue("nckd_adjustnum",null,row);
        }else{
            int relcyearstaff = getIntValue("nckd_brealnum", row);
            int newValueInt = (int) newValue;
            if(newValueInt<0){
                this.getView().showErrorNotification("编制人数不能小于0！");
                this.getModel().setValue("nckd_adjustlatenum",oldValue,row);
                return;
            }
            this.getModel().setValue("nckd_adjustnum",newValueInt - relcyearstaff,row);
        }
    }


    // 组织 直属调整人数修改
    private void adjustdireChange(Object newValue,Object oldValue,int row) {
        // 获取直属编制人数
        int nckdBrealnum =getIntValue(NCKD_BDIRECTNUM,row);
        int nckdAdjustlatenum = 0;
        // 调整人数
        int i = newValue == null ? 0 : (int) newValue;
        nckdAdjustlatenum = nckdBrealnum +  i;
        if(nckdAdjustlatenum<0){
            this.getView().showErrorNotification("编制人数不能小于0！");
            this.getModel().setValue(NCKD_ADJUSTDIRE,oldValue,row);
            return;
        }
        this.getModel().setValue(NCKD_RELBDIRECTNUM,nckdAdjustlatenum,row);
    }


    // 组织含下级调整人数修改
    private void adjustnumChange(Object newValue,Object oldValue,int row) {
        // 获取现有编制人数
        int nckdBrealnum =getIntValue(NCKD_BREALNUM,row);
        int nckdAdjustlatenum = 0;
        // 调整人数
        int i = newValue == null ? 0 : (int) newValue;
        nckdAdjustlatenum = nckdBrealnum +  i;
        if(nckdAdjustlatenum<0){
            this.getView().showErrorNotification("编制人数不能小于0！");
            this.getModel().setValue(NCKD_ADJUSTNUM,oldValue,row);
            return;
        }
        this.getModel().setValue(NCKD_ADJUSTLATENUM,nckdAdjustlatenum,row);
    }

    private void adjusttype(Object newValue,Object oldValue,int row) {
        int nckdAdjustlatenum = 0;
        // 调整人数
        int i = this.getModel().getValue(NCKD_ADJUSTNUM,row) == null ? 0 : (int) this.getModel().getValue(NCKD_ADJUSTNUM,row);
        // 获取现有编制人数
        int nckdBrealnum =getIntValue(NCKD_BREALNUM,row);
        if(StringUtils.isEmpty((String)newValue)){
            this.getModel().setValue(NCKD_ADJUSTLATENUM,null,row);
            return;
        }else if(ADJUSTTYPE_ADD.equals(newValue)){
            nckdBrealnum = nckdBrealnum +  i;
        }else{
            nckdBrealnum = nckdBrealnum - i;
        }
        if(nckdBrealnum<0){
            this.getView().showErrorNotification("编制人数不能小于0！");
            this.getModel().setValue(NCKD_ADJUSTTYPE,oldValue,row);
            return;
        }
        this.getModel().setValue(NCKD_ADJUSTLATENUM,nckdBrealnum,row);
    }

    private void postadjusttypeChange(Object newValue,Object oldValue,int row) {
        int nckdAdjustlatenum = 0;
        // 调整人数
        int i = this.getModel().getValue(NCKD_POSTADJUSTNUM,row) == null ? 0 : (int) this.getModel().getValue(NCKD_POSTADJUSTNUM,row);
        // 获取现有编制人数
        int nckdBrealnum =getIntValue(NCKD_RELCYEARSTAFF,row);
        // 获取对应调整类型
        if(StringUtils.isEmpty((String)newValue)){
            this.getModel().setValue(NCKD_POSTADJUSTLATENUM,null,row);
            return;
        }else if(ADJUSTTYPE_ADD.equals(newValue)){
            nckdBrealnum = nckdBrealnum +  i;
        }else{
            nckdBrealnum = nckdBrealnum - i;
        }
        if(nckdBrealnum<0){
            this.getView().showErrorNotification("编制人数不能小于0！");
            this.getModel().setValue(NCKD_POSTADJUSTTYPE,oldValue,row);
            return;
        }
        this.getModel().setValue(NCKD_POSTADJUSTLATENUM,nckdBrealnum,row);

    }

    private void postadjustnumChange(Object newValue,Object oldValue,int row) {
        // 获取对应调整类型
        String nckdAdjusttype = (String) this.getModel().getValue(NCKD_POSTADJUSTTYPE);
        if(StringUtils.isEmpty(nckdAdjusttype)){
            this.getView().showErrorNotification("请先维护调整类型！");
        }else{
            // 获取现有编制人数
            int nckdBrealnum =getIntValue(NCKD_RELCYEARSTAFF,row);
            int nckdAdjustlatenum = 0;
            // 调整人数
            int i = newValue == null ? 0 : (int) newValue;
            if(ADJUSTTYPE_ADD.equals(nckdAdjusttype)){
                // 增加调整人数
                nckdAdjustlatenum = nckdBrealnum +  i;
            }else{
                nckdAdjustlatenum = nckdBrealnum -  i;
            }
            if(nckdAdjustlatenum<0){
                this.getView().showErrorNotification("编制人数不能小于0！");
                this.getModel().setValue(NCKD_POSTADJUSTNUM,oldValue,row);
                return;
            }
            this.getModel().setValue(NCKD_POSTADJUSTLATENUM,nckdAdjustlatenum,row);
        }
    }


    public int getIntValue(String key,int row) {

        Object value = this.getModel().getValue(key, row);
        if (value == null) {
            return 0; // 返回默认值 0
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0; // 如果解析失败，返回默认值 0
        }
    }


    private void initEntry(Object org,Object staff) {
        // 初始化申请单据
        if(isNotEmpty(org) && isNotEmpty(staff)){
            // 填充申请单据数据
            DynamicObject staff1 = (DynamicObject) staff;
            DynamicObject haosStaff = BusinessDataServiceHelper.loadSingle(staff1.getPkValue(), "haos_staff");
            IFormView view = this.getView();
            AbstractFormDataModel model = (AbstractFormDataModel)this.getModel();
            List<Long> orgIds = Arrays.asList((Long) ((DynamicObject) org).getPkValue());
            // 组织id
            Long orgid = (Long) ((DynamicObject) org).getPkValue();
            // 编制维护id
            Long pkValue = (Long) staff1.getPkValue();
            Object[] objects3 = new Object[1];
            objects3[0] = pkValue;
//            HRMServiceResult haosStaffResponse =  DispatchServiceHelper.invokeService("kd.hrmp.haos.servicehelper","haos","IStaffExternalService","queryStaffById",objects3);

            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = sdf.format(date);

            // 拼接 SQL 查询
            StringBuilder sql = new StringBuilder("SELECT a.fid AS id, a.fboid AS boid, A.fparentid as parentorg, "
                    + "t.flevel as level1, t.fstructlongnumber as structlongnumber, ST.fcount as count,ST.fcontainsubcount as containsubcount, "
//                    + "(select top 1 N.fstaffcount  from t_haos_adminorg M where a.fparentid = M.fboid ) as "
//                    + " (select top 1 CASE WHEN N.fstaffcount IS NULL THEN 999999 ELSE N.fstaffcount END AS fstaffcount  "
//                    + "from t_haos_adminorg M left join t_haos_dutyorgdetail N on N.fdutyorgid = M.fid "
//                    + "where M.fenable ='1'  and M.fboid  =a.fboid and N.fenable = '1' and N.fleffdt >='" + dateStr + "' "
//                    + "AND M.fbsed <= '" + dateStr + "' AND M.fbsled >= '" + dateStr + "'  "
//                    + "order by M.fhisversion desc) as staffcount "
                    + "CASE "
                    + "    WHEN (SELECT TOP 1 CASE WHEN N.fstaffcount IS NULL THEN 999999 ELSE N.fstaffcount END "
                    + "          FROM t_haos_adminorg M "
                    + "          LEFT JOIN t_haos_dutyorgdetail N ON N.fdutyorgid = M.fid "
                    + "          WHERE M.fenable = '1' "
                    + "          AND M.fboid = a.fboid "
                    + "          AND N.fenable = '1' "
                    + "          AND N.fleffdt >= '" + dateStr + "' "
                    + "          AND M.fbsed <= '" + dateStr + "' "
                    + "          AND M.fbsled >= '" + dateStr + "' "
                    + "          ORDER BY M.fhisversion DESC) IS NULL "
                    + "    THEN 999999 "
                    + "    ELSE (SELECT TOP 1 CASE WHEN N.fstaffcount IS NULL THEN 999999 ELSE N.fstaffcount END "
                    + "          FROM t_haos_adminorg M "
                    + "          LEFT JOIN t_haos_dutyorgdetail N ON N.fdutyorgid = M.fid "
                    + "          WHERE M.fenable = '1' "
                    + "          AND M.fboid = a.fboid "
                    + "          AND N.fenable = '1' "
                    + "          AND N.fleffdt >= '" + dateStr + "' "
                    + "          AND M.fbsed <= '" + dateStr + "' "
                    + "          AND M.fbsled >= '" + dateStr + "' "
                    + "          ORDER BY M.fhisversion DESC) "
                    + "END AS staffcount "

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
//                    + "AND A.forgid = ? "
                    + "ORDER BY S.fsortcode");
//            Object[] param = new Object[]{(Long) orgid,haosStaff.getPkValue()};
            Object[] param = new Object[]{(Long) orgid};
            DataSet dataSet = HRDBUtil.queryDataSet("haos_adminOrgHisSearch", new DBRoute("hr"), sql.toString(), param);
//            DataSet dataSet = DB.queryDataSet("leaseContractPushCard", DBRoute.of("hr"), sql);

            ORM orm = ORM.create();
            DynamicObjectCollection retDynCol = orm.toPlainDynamicObjectCollection(dataSet.copy());
            List<Long> collect2 = new ArrayList<Long>();
            Set<Long> longSet = new HashSet<Long>();
            for (int i = 0; i < retDynCol.size(); i++) {
                int index = this.getModel().insertEntryRow("nckd_bentryentity", i);
                this.getModel().setValue("nckd_adminorg",retDynCol.get(i).get("BOID"),index);
                this.getModel().setValue("nckd_parentorg",retDynCol.get(i).get("parentorg"),index);
                // 编制人数
                int staffcount = (int)retDynCol.get(i).get("staffcount");
                Object staffcountobj = staffcount == 999999 ? null : staffcount;
                this.getModel().setValue("nckd_brealnum",staffcountobj,index);
                this.getModel().setValue("nckd_adjustlatenum",staffcountobj,index);
                // 实际人数
//                this.getModel().setValue("nckd_rellownum",retDynCol.get(i).get("containsubcount"),index);
                this.getModel().setValue("nckd_relnum",retDynCol.get(i).get("count"),index);
                collect2.add((Long) retDynCol.get(i).get("BOID"));
                longSet.add((Long) retDynCol.get(i).get("parentorg"));
            }
            // 获取到部门key，根据部门key获取到岗位信息,HR岗位hbpm_positionhr
            QFilter qFilter = new QFilter("adminorg.id", QCP.in, collect2);

            Date date2 = DateUtils.stringToDate(dateStr, DatePattern.YYYY_MM_DD_HH_MM_SS);
            qFilter.and(new QFilter("bsled", QCP.large_equals, date2)).and("disabler",QCP.equals,0);
            DynamicObjectCollection query = QueryServiceHelper.query("hbpm_positionhr", "id,adminorg,adminorg.id,number,name,hisversion,createtime", new QFilter[]{qFilter}, "number,createtime desc,hisversion");
            // 岗位id
            List<Long> positionIds = new ArrayList<Long>();
            //  创建一个Map来存储结果
            Map<String, List<DynamicObject>> resultMap = Arrays.stream(query.toArray(new DynamicObject[0]))
                    .collect(Collectors.toMap(
                            obj -> String.valueOf(obj.get("adminorg.id")),
                            obj -> {
                                // 使用一个列表来存储当前 adminorg.id 下的对象
                                List<DynamicObject> list = new ArrayList<>();
                                list.add(obj);
                                positionIds.add((Long) obj.get("id"));
                                return list;
                            },
                            (existingList, newList) -> {
                                // 使用一个Set来记录已有的number值
                                Set<String> existingNumbers = existingList.stream()
                                        .map(o -> String.valueOf(o.get("number")))
                                        .collect(Collectors.toSet());

                                // 只添加那些number值不在existingNumbers中的对象
                                newList.forEach(o -> {
                                    String newNumber = String.valueOf(o.get("number"));
                                    if (!existingNumbers.contains(newNumber)) {
                                        existingList.add(o);
                                        positionIds.add((Long) o.get("id"));
                                    }
                                });
                                return existingList;
                            }
                    ));
            // 调用服务，获取岗位编制信息
            Object[] objects2 = new Object[2];
            objects2[0] = new Date();
            objects2[1] = positionIds;
            StaffResponse<Map<String, Map<String, Object>>> staffpositionResponse =  DispatchServiceHelper.invokeService("kd.hrmp.haos.servicehelper","haos","IHAOSStaffService","queryPositionStaffInfo",objects2);
            Object[] objects = new Object[2];
            objects[0] = new Date();
            objects[1] = collect2;
            StaffResponse<Map<String, Map<String, Object>>> staffResponse = (StaffResponse<Map<String, Map<String, Object>>>) DispatchServiceHelper.invokeService("kd.hrmp.haos.servicehelper","haos","IHAOSStaffService","queryUseStaffInfo",objects);
            Map<Long, String> orgLongNameMap = OrgBatchBillHelper.getOrgLongName(longSet, new Date(), String.valueOf(AdminOrgConstants.ADMINORG_STRUCT));
            this.initTree(staffResponse,staffpositionResponse,resultMap,orgLongNameMap);
            this.getView().updateView("nckd_bentryentity");
            this.getView().updateView("nckd_centryentity");


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
    private void initTree(StaffResponse<Map<String, Map<String, Object>>> staffResponse,StaffResponse<Map<String, Map<String, Object>>> staffpositionResponse,Map<String, List<DynamicObject>> postMap,Map<Long, String> orgLongNameMap) {
        // 获取他的单据体，循环单据体
        DynamicObjectCollection entryentityCols = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_bentryentity");
        for (int i = 0; i < entryentityCols.size(); i++) {
            //  给单据体添加子单据体数据

            DynamicObject centrydynamicObject = entryentityCols.get(i);
            List<DynamicObject> nckdAdminorg1 = postMap.get(String.valueOf(centrydynamicObject.getDynamicObject("nckd_adminorg").getPkValue()));


            DynamicObject enObj = entryentityCols.get(i);
            // 子单据体标识nckd_centryentity
            DynamicObjectCollection cntryEntity = enObj.getDynamicObjectCollection("nckd_centryentity");
            if(ObjectUtils.isNotEmpty(nckdAdminorg1)){
                for (DynamicObject object : nckdAdminorg1) {
                    DynamicObject dynamicObject = new DynamicObject(cntryEntity.getDynamicObjectType());
                    DynamicObject dynamicObject2 = BusinessDataServiceHelper.newDynamicObject("hbpm_positionhr");
                    dynamicObject2.set("id",object.get("id"));
                    BaseDataServiceHelper.clearCache(dynamicObject);
                    dynamicObject.set("nckd_cdutyworkrole",dynamicObject2);
                    dynamicObject.set("nckd_cdutyworknumber",object.getString("number"));
//                    dynamicObject.set("nckd_cdutyworkrole.number",object.get("number"));
                    Map<String, Object> positionMap = staffpositionResponse.getData().get(String.valueOf(object.get("id")));
                    if(ObjectUtils.isNotEmpty(positionMap)){
                        dynamicObject.set("nckd_relcyearstaff",positionMap.get("staffNum"));
                        // todo 岗位调整后人数
//                        dynamicObject.set("nckd_postadjustlatenum",positionMap.get("staffNum"));
                    }
                    cntryEntity.add(dynamicObject);
                }
            }

            // 使用组织上级id
            Long aLong = (Long) entryentityCols.get(i).get("nckd_parentorg");


            if(null == aLong){
                centrydynamicObject.set("pid",0);
                continue;
            }
            if(StringUtils.isNotEmpty(orgLongNameMap.get(aLong))){
                centrydynamicObject.set("nckd_bparentlongname",orgLongNameMap.get(aLong));
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

            } else {
                // 处理未找到匹配的情况
                centrydynamicObject.set("pid",0);
            }
            Map<String, Object> nckdAdminorg = staffResponse.getData().get(String.valueOf(centrydynamicObject.getDynamicObject("nckd_adminorg").getPkValue()));
            if(ObjectUtils.isNotEmpty(nckdAdminorg)){
                // 编制人数
                centrydynamicObject.set("nckd_bdirectnum",nckdAdminorg.get("staffNum"));
                centrydynamicObject.set("NCKD_RELBDIRECTNUM",nckdAdminorg.get("staffNum"));
            }
        }

        DynamicObjectCollection entryentityCols2 = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_bentryentity");
        DynamicObjectCollection entryentityCols3 = entryentityCols2;
        Map<Long, List<DynamicObject>> resultMap = Arrays.stream(entryentityCols2.toArray(new DynamicObject[0]))
                .collect(Collectors.groupingBy(
                        obj -> (Long) obj.get("pid") // 使用 pid 作为 key
                ));

        entryentityCols3.stream().forEach(obj -> {

            List<DynamicObject> dynamicObjects = resultMap.get(obj.getPkValue());
            if(ObjectUtils.isEmpty(dynamicObjects)){
                obj.set("nckd_lowermost","A");
            }
        });

        DynamicObjectCollection useOrgInfos = this.getModel().getEntryEntity("nckd_bentryentity");
        DynamicObjectCollection useOrgInfos2 = useOrgInfos;
        Map<Long, DynamicObject> idVsDynMap = (Map)useOrgInfos.stream().collect(Collectors.toMap((info) -> {
            return info.getLong("id");
        }, (info) -> {
            return info;
        }, (o1, o2) -> {
            return o1;
        }));
        List<DynamicObject> useOrgInfoSortList = (List)useOrgInfos2.stream().sorted(Comparator.comparing((o) -> {
            return String.valueOf(o.get("nckd_parentorg"));
        })).collect(Collectors.toList());

        for(int i = useOrgInfoSortList.size() - 1; i >= 0; --i) {
            DynamicObject useOrgInfo = (DynamicObject)useOrgInfoSortList.get(i);
            Object directNumObject = useOrgInfo.get("nckd_relnum");
            Object realNumWithSubObject = useOrgInfo.get("nckd_rellownum");
            if (!Objects.isNull(directNumObject) || !Objects.isNull(realNumWithSubObject)) {
                int realNumWithSub = 0;
                if (Objects.nonNull(realNumWithSubObject)) {
                    realNumWithSub = (Integer)realNumWithSubObject;
                }

                int directNum = 0;
                if (Objects.nonNull(directNumObject)) {
                    directNum = (Integer)directNumObject;
                }

                useOrgInfo.set("nckd_rellownum", directNum + realNumWithSub);
                long parentEntryId = useOrgInfo.getLong("pid");
                if (parentEntryId != 0L) {
                    DynamicObject parentDyn = (DynamicObject)idVsDynMap.get(parentEntryId);
                    if (!Objects.isNull(parentDyn)) {
                        Object parentRealNumWithSubObject = parentDyn.get("nckd_rellownum");
                        if (Objects.nonNull(parentRealNumWithSubObject)) {
                            parentDyn.set("nckd_rellownum", useOrgInfo.getInt("nckd_rellownum") + (Integer)parentRealNumWithSubObject);
                        }
                    }
                }
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
        if (StringUtils.equals(fieldKey, NCKD_HAOS_STAFF)){
            ListShowParameter showParameter = (ListShowParameter)e.getFormShowParameter();
            //是否展示审核的改为false
            showParameter.setShowApproved(false);
        }else if(StringUtils.equals(fieldKey, "org")){
            ListShowParameter showParameter = (ListShowParameter)e.getFormShowParameter();
            //是否展示审核的改为false
            QFilter qFilter = new QFilter("orgpattern.number", "in", COMPANY_LIST);
            showParameter.getListFilterParameter().setFilter(qFilter);
        }


    }

    @Override
    public void entryRowClick(RowClickEvent evt) {
        // 获取选中的行
        EntryGrid entryentity = this.getView().getControl("nckd_bentryentity");
        int[] selectRows = entryentity.getSelectRows();
        boolean flag = false;
        if (selectRows.length > 0) {
            DynamicObject rowEntity = this.getModel().getEntryRowEntity("nckd_bentryentity", selectRows[0]);
            DynamicObject nckdAdminorg = rowEntity.getDynamicObject("nckd_adminorg");
            this.getModel().setValue("nckd_useorg",nckdAdminorg);
            return;
        }
        this.getModel().setValue("nckd_useorg",null);
    }

    @Override
    public void registerListener(EventObject e) {
        // 监听单据体行点击事件
        EntryGrid entryGrid = this.getView().getControl("nckd_bentryentity");
        entryGrid.addRowClickListener(this);
        BasedataEdit fieldEdit = this.getView().getControl(NCKD_HAOS_STAFF);//基础资料字段标识
        BasedataEdit fieldEdit2 = this.getView().getControl("org");//基础资料字段标识
        fieldEdit.addBeforeF7SelectListener(this);
        fieldEdit2.addBeforeF7SelectListener(this);
    }

}
