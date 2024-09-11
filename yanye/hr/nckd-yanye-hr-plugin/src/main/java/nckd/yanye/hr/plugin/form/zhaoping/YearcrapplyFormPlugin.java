package nckd.yanye.hr.plugin.form.zhaoping;


import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.LoadDataEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import org.apache.commons.lang3.ObjectUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

/**
 * Module           :人才供应云-招聘直通车-首页-年度招聘申请
 * Description      :年度招聘申请单据插件
 *
 * @author guozhiwei
 * @date  2024/9/11 17：10
 * 标识 nckd_yearapply
 */



public class YearcrapplyFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {

    // 公司类型
    private final List<String> COMPANY_LIST = Arrays.asList(new String[]{"1020_S","1050_S","1060_S","1070_S"});

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
            case "nckd_recruitcompany":
                // 公司为空清除部门岗位
                if(!isNotEmpty(newValue)){
                    this.getModel().setValue("nckd_recruitorg", null,iRow);
                    this.getModel().setValue("nckd_recruitpost", null,iRow);
                }
                break;
            case "nckd_recruitorg":
                // 部门，判断是否存企业，如果不存在企业则将部门的所属公司带入到企业中
                if(isNotEmpty(newValue)){
                    DynamicObject newValue1 = (DynamicObject) newValue;
                    DynamicObject nckdRecruitcompany = (DynamicObject)this.getModel().getValue("nckd_recruitcompany", iRow);
                    if(ObjectUtils.isEmpty(nckdRecruitcompany)){
                        this.getModel().setValue("nckd_recruitcompany", newValue1.getDynamicObject("belongcompany").getPkValue(),iRow);
                    }
                }else{
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
                        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle(adminorg.getPkValue(), "haos_adminorgf7");
                        this.getModel().setValue("nckd_recruitorg", dynamicObject.getDynamicObject("belongcompany").getPkValue(),iRow);
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
                // 展示部门，如果选择了企业，展示企业下的部门
                QFilter qFilter2 = new QFilter("adminorgtype.number", QCP.in, "1040_S");
                EntryGrid treeEntryEntity = this.getControl("entryentity");
                int[] rows = treeEntryEntity.getSelectRows();
                if(rows.length > 0){
                    // 企业不为null，添加企业筛选条件
                    DynamicObject nckdRecruitcompany = (DynamicObject)this.getModel().getValue("nckd_recruitcompany", rows[0]);
                    if(ObjectUtils.isNotEmpty(nckdRecruitcompany)){
                        qFilter2.and("belongcompany.number", QCP.like, nckdRecruitcompany.getString("number")+"%");
                    }
                }
                showParameter2.getListFilterParameter().setFilter(qFilter2);
                break;
            case "nckd_recruitpost":
                // 岗位名称
                ListShowParameter showParameter3 = (ListShowParameter)e.getFormShowParameter();
                // 展示部门，如果选择了企业，展示企业下的部门
                EntryGrid treeEntryEntity2 = this.getControl("entryentity");
                int[] rows2 = treeEntryEntity2.getSelectRows();
                DynamicObject nckdRecruitcompany2 = (DynamicObject)this.getModel().getValue("nckd_recruitcompany", rows2[0]);
                DynamicObject nckdRecruitorg = (DynamicObject)this.getModel().getValue("nckd_recruitorg", rows2[0]);
                QFilter qFilter1 = null;
                if(ObjectUtils.isNotEmpty(nckdRecruitcompany2)){
                    qFilter1 = new QFilter("adminorg.number", QCP.like, nckdRecruitcompany2.getString("number") + "%");
                }
                if(ObjectUtils.isNotEmpty(nckdRecruitorg)){
                    if(qFilter1 ==null){
                        qFilter1 = new QFilter("adminorg.id", QCP.equals, nckdRecruitorg.getPkValue());
                    }else{
                        qFilter1.and("adminorg.id", QCP.equals, nckdRecruitorg.getPkValue());
                    }
                }
                showParameter3.getListFilterParameter().setFilter(qFilter1);
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
        fieldEdit.addBeforeF7SelectListener(this);
        fieldEdit.addBeforeF7SelectListener(this);
        fieldEdit2.addBeforeF7SelectListener(this);
        fieldEdit3.addBeforeF7SelectListener(this);
    }


    public static boolean isNotEmpty(Object  key) {
        // 基础资料判空
        if(ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(((DynamicObject)key).getDataStorage())){
            return false;
        }
        return true;
    }



}
