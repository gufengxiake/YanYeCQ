package nckd.yanye.hr.plugin.form.zhaoping;

import com.alibaba.fastjson.JSONObject;
import kd.fi.dcm.common.util.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kd.bamp.mbis.common.mega.utils.OrgViewUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.consts.OrgViewTypeConst;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.*;
import kd.bos.form.ConfirmCallBackListener;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.MessageBoxResult;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.importentry.resolving.ImportEntryData;
import kd.bos.list.ListShowParameter;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import org.apache.commons.lang3.ObjectUtils;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Module           :人才供应云-招聘直通车-首页-临时招聘申请
 * Description      :临时招聘申请单据插件
 *
 * @author guozhiwei
 * @date  2024/9/10 9：40
 * 标识 nckd_casrecrapply
 */



public class CasrecrapplyFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {

    private static Log logger = LogFactory.getLog(CasrecrapplyFormPlugin.class);

    // 公司类型
    private final List<String> COMPANY_LIST = Arrays.asList(new String[]{"1020_S","1050_S","1060_S","1070_S"});

    private final List<String> COMPANY_LIST2 = Arrays.asList(new String[]{"Orgform01","Orgform01-100","Orgform02","Orgform03"});

    // 定义日期格式
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final String SUBMIT = "submit";

    private static String OPPARAM_AFTERCONFIRM = "afterconfirm";



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
        /*
        Long pkValue = (Long) org.getPkValue();

        */
        updateStaffCount();
        // 实际人数
        this.getModel().setValue("nckd_relnum",YearcrapplyFormPlugin.getStaffCount(org.getPkValue()));
    }

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
    public void beforeImportEntry(BeforeImportEntryEventArgs e) {
        // 临时招聘 导入数据
//        super.beforeImportEntry(e);
        logger.info("临时招聘导入数据--------");
        QFilter qFilter = new QFilter("status", QCP.equals, "C")
                .and("enable", QCP.equals, "1")
                .and("iscurrentversion", QCP.equals, "1");
        Map<String, List<Object>> entryDataMap = e.getEntryDataMap();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 转换为 JSON 字符串
            String jsonString = objectMapper.writeValueAsString(entryDataMap);
            logger.info("jsonString:{}",jsonString);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }


        Map<String, List<ImportEntryData>> source = (Map)e.getSource();
        Set<Map.Entry<String, List<ImportEntryData>>> entries = source.entrySet();
        Iterator var4 = entries.iterator();
        while (var4.hasNext()){
            Map.Entry<String, List<ImportEntryData>> entry = (Map.Entry)var4.next();
            String key = entry.getKey();
            if("entryentity".equals(key)){
                List<ImportEntryData> entryValue = (List) entry.getValue();
                List<ImportEntryData> entryValue2 = entryValue;
                if(!CollectionUtils.isEmpty(entryValue)){
                    List<Object> listQuerOrg = new ArrayList<>();
                    List<Object> listQuerPost = new ArrayList<>();
                    String orgQuery = null;
                    String postQuery = null;
                    for (int i = 0; i < entryValue2.size(); i++) {
                        // 部门
                        JSONObject nckdRecruitorg = entryValue2.get(i).getData().getJSONObject("nckd_recruitorg");
                        // 岗位
                        JSONObject nckdRecruitpost = entryValue2.get(i).getData().getJSONObject("nckd_recruitpost");

                        if(i == 0){
                            orgQuery = nckdRecruitorg.getString("importprop");
                            postQuery = nckdRecruitpost.getString("importprop");
                        }
                        listQuerOrg.add(nckdRecruitorg.get(orgQuery));
                        listQuerPost.add(nckdRecruitpost.get(postQuery));
                    }
                    QFilter qFilterorg = new QFilter(orgQuery, QCP.in, listQuerOrg);
                    QFilter qFilterpost = new QFilter(postQuery, QCP.in, listQuerPost);
                    DynamicObject[] haosAdminorgf7s = BusinessDataServiceHelper.load("haos_adminorgf7", "id,name,number,status,enable,iscurrentversion", new QFilter[]{qFilter, qFilterorg});
                    String finalOrgQuery = orgQuery;
                    Map<String, Object> map1 =
                            Arrays.stream(haosAdminorgf7s)
                                    .collect(Collectors.toMap(
                                            detail -> detail.get(finalOrgQuery).toString(),
                                            detail -> detail.get("id"),
                                            (existing, replacement) -> existing // 保留前面的值
                                    ));

                    DynamicObject[] hbpmPositionhrs = BusinessDataServiceHelper.load("hbpm_positionhr", "id,name,number,status,enable,iscurrentversion", new QFilter[]{qFilter, qFilterpost});
                    String finalOrgQuery2 = postQuery;
                    Map<String, Object> map2 =
                            Arrays.stream(hbpmPositionhrs)
                                    .collect(Collectors.toMap(
                                            detail -> detail.get(finalOrgQuery2).toString(),
                                            detail -> detail.get("id"),
                                            (existing, replacement) -> existing // 保留前面的值
                                    ));

                    entryValue.stream().forEach(importEntryData -> {
                        logger.info("修改前importEntryData:{},jsondata:{}",importEntryData.getEntryName(),importEntryData.getData().toString());
                        // 部门
                        JSONObject nckdRecruitorg = importEntryData.getData().getJSONObject("nckd_recruitorg");
                        String imoportprop = nckdRecruitorg.getString("importprop");
                        Object o = nckdRecruitorg.get(imoportprop);
                        if(ObjectUtils.isNotEmpty(o)){
                            Object o1 = map1.get(o);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("importprop","id");
                            jsonObject.put("id",o1);
                            importEntryData.getData().remove("nckd_recruitorg");
                            importEntryData.getData().put("nckd_recruitorg",jsonObject);
                        }
                        // 岗位
                        JSONObject nckdRecruitpost = importEntryData.getData().getJSONObject("nckd_recruitpost");
                        String imoportprop2 = nckdRecruitpost.getString("importprop");
                        Object o2 = nckdRecruitpost.get(imoportprop2);
                        if(ObjectUtils.isNotEmpty(o)){
                            Object o1 = map2.get(o2);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("importprop","id");
                            jsonObject.put("id",o1);
                            importEntryData.getData().remove("nckd_recruitpost");
                            importEntryData.getData().put("nckd_recruitpost",jsonObject);
                        }
                    });
                }
            }

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
            case "nckd_year":
                updateStaffCount();
                break;
            case "nckd_recruitcompany":
                // 公司为空清除部门岗位
                if(!isNotEmpty(newValue)){
                    this.getModel().setValue("nckd_recruitorg", null,iRow);
                    this.getModel().setValue("nckd_recruitpost", null,iRow);
                }
                break;
            case "nckd_recruitorg":
                // 部门，判断是否存企业，如果不存在企业则将部门的所属公司带入到企业中
                if(!isNotEmpty(newValue)){
                    this.getModel().setValue("nckd_recruitpost", null,iRow);
                }
                break;
            case "nckd_recruitpost":
                // 岗位，判断是否存企业，如果不存在企业则将部门的所属公司带入到企业中
                if(isNotEmpty(newValue)){
                    DynamicObject newValue1 = (DynamicObject) newValue;
                    // 获取行政组织，和行政组织的所属公司
                    // 行政组织
                    DynamicObject nckdRecruitcompany = (DynamicObject)this.getModel().getValue("nckd_recruitorg", iRow);
                    if(ObjectUtils.isEmpty(nckdRecruitcompany)){
                        DynamicObject adminorg = newValue1.getDynamicObject("adminorg");
                        this.getModel().setValue("nckd_recruitorg",adminorg,iRow);
                    }
                }
                break;
            case "nckd_payrange":
                // 补充 万
                if(ObjectUtils.isNotEmpty(newValue)){
                    String str =  (String) newValue;
                    String lastChar = String.valueOf(str.charAt(str.length() - 1));
                    if(!"万".equals(lastChar)){
                        str = str+"万";
                        this.getModel().setValue("nckd_payrange", str,iRow);
                    }
                }
                break;
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
            case "nckd_recruitcompany":
                // 企业名称
                ListShowParameter showParameter = (ListShowParameter)e.getFormShowParameter();
                //是否展示审核的改为false
                QFilter qFilter = new QFilter("adminorgtype.number", "in", COMPANY_LIST);
                showParameter.getListFilterParameter().setFilter(qFilter);
                break;
            case "nckd_recruitorg":
                // 组织名称
                ListShowParameter showParameter2 = (ListShowParameter)e.getFormShowParameter();
                // 展示部门，如果选择了企业，展示企业下的部门，添加企业筛选条件
                DynamicObject nckdRecruitcompany = (DynamicObject)this.getModel().getValue("org");

                List<Long> longs = new ArrayList<Long>();
                Long pkValue = (Long) nckdRecruitcompany.getPkValue();
                longs.add(pkValue);
                List<Long> allSubordinateOrgIds = OrgUnitServiceHelper.getAllSubordinateOrgs("01", longs, true);
                // 使用流处理过滤掉指定的值
//                List<Long> filteredOrgIds = allSubordinateOrgIds.stream()
//                        .filter(orgId -> !orgId.equals(pkValue)) // 过滤掉值
//                        .collect(Collectors.toList());

                // 获取组织编码
//                QFilter belongcompanyFilter = new QFilter("belongcompany", "in", allSubordinateOrgIds);
                QFilter qFilter2 = new QFilter("belongcompany", QCP.in,allSubordinateOrgIds);
                showParameter2.getListFilterParameter().setFilter(qFilter2);

                break;
            case "nckd_recruitpost":
                // 岗位名称
                ListShowParameter showParameter3 = (ListShowParameter)e.getFormShowParameter();
                // 展示部门，如果选择了企业，展示企业下的部门
                EntryGrid treeEntryEntity2 = this.getControl("entryentity");
                int[] rows2 = treeEntryEntity2.getSelectRows();
                DynamicObject nckdRecruitcompany2 = (DynamicObject)this.getModel().getValue("org");
                DynamicObject nckdRecruitorg = (DynamicObject)this.getModel().getValue("nckd_recruitorg", rows2[0]);
                List<Long> longs2 = new ArrayList<Long>();
                longs2.add((Long)nckdRecruitcompany2.getPkValue());
                List<Long> allSubordinateOrgIds2 = OrgUnitServiceHelper.getAllSubordinateOrgs("01", longs2, true);
                QFilter qFilter1 = new QFilter("adminor", QCP.in, allSubordinateOrgIds2);
                if(ObjectUtils.isNotEmpty(nckdRecruitorg)){
                    qFilter1.and("adminorg.id", QCP.equals, nckdRecruitorg.getPkValue());
                }
                showParameter3.getListFilterParameter().setFilter(qFilter1);
                break;
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
        BasedataEdit fieldEdit = (BasedataEdit) this.getView().getControl("nckd_recruitcompany");
        BasedataEdit fieldEdit2 = (BasedataEdit) this.getView().getControl("nckd_recruitorg");
        BasedataEdit fieldEdit3 = (BasedataEdit) this.getView().getControl("nckd_recruitpost");
        BasedataEdit fieldEdit4 = this.getView().getControl("org");
        fieldEdit.addBeforeF7SelectListener(this);
        fieldEdit.addBeforeF7SelectListener(this);
        fieldEdit2.addBeforeF7SelectListener(this);
        fieldEdit3.addBeforeF7SelectListener(this);
        fieldEdit4.addBeforeF7SelectListener(this);
    }


    public static boolean isNotEmpty(Object  key) {
        // 基础资料判空
        if(ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(((DynamicObject)key).getDataStorage())){
            return false;
        }
        return true;
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

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate opreate = (FormOperate) args.getSource();
        switch (opreate.getOperateKey()) {
            case "submit":
                // 在此添加处理逻辑
                int nckdSftaffcount = (int) this.getModel().getValue("nckd_sftaffcount");
                // 实际人数
                int nckdRelnum = (int) this.getModel().getValue("nckd_relnum");
                // 申请人数
                int nckdApplynum = (int) this.getModel().getValue("nckd_applynum");
                if(nckdSftaffcount < nckdRelnum + nckdApplynum){
                    // 判断是否处理过
                    String isDealed = this.getView().getPageCache().get("isDealed");
                    if (!"true".equals(isDealed)) {
                        // 取消原来的操作
                        args.setCancel(true);
                        // 在用户点击确认框上的按钮后，系统会调用confirmCallBack方法
                        ConfirmCallBackListener confirmCallBackListener = new ConfirmCallBackListener("isExceed", this);
                        // 设置页面确认框，参数为：标题，选项框类型，回调监听
                        this.getView().showConfirm("请注意，申请人数超编，是否继续提报？", MessageBoxOptions.YesNo, confirmCallBackListener);
                        // 只执行一次
                        this.getView().getPageCache().put("isDealed", "true");
                    }
                }
                break;
            default:
                break;
        }
    }

    public List<Long> selectLevelDept(Long ids){
        //包含本级和下级所有部门的id集合
        List<Long> allIds = new ArrayList<>();
        allIds.add(ids);
        //获取所有下属组织
        List<Long> allSubordinateOrgs =  OrgViewUtils.getSubOrgId(OrgViewTypeConst.Admin, allIds, false, false, (QFilter) null);
        allIds.addAll(allSubordinateOrgs);
        return allIds;
    }



    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        this.getView().getPageCache().remove("isDealed");
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        //判断回调参数id
        if ("isExceed".equals(messageBoxClosedEvent.getCallBackId())) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                this.getView().invokeOperation("submit");
            } else if (MessageBoxResult.No.equals(messageBoxClosedEvent.getResult())) {
                // 点击否也清除
                this.getView().getPageCache().remove("isDealed");
            }
        }
    }



}
