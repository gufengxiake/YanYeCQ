package nckd.yanye.hr.plugin.form.web.employee.formview;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.db.SqlBuilder;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.DateProp;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.DateEdit;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.AbstractPCFormDrawEdit;

import java.util.Date;
import java.util.EventObject;
import java.util.Map;

/**
 * HR员工自助PC端-职称及技能信息-弹框
 * 移动动态表单标识：nckd_hspm_perprotitl_ext7
 * author:chengchaohua
 * date:2024-08-20
 */
public class PerProTitlepcEditPluginEx extends AbstractPCFormDrawEdit {


    public PerProTitlepcEditPluginEx() {}

    // 进入单据时-二开cch
    @Override
    public void beforeBindData(EventObject eventObject) {
        super.beforeBindData(eventObject);
        IDataModel model = this.getModel();
        String nckd_type = (String)model.getValue("nckd_type");
        // 当“类型”字段选择码值为“职称”(zhicheng)后，展示“是否公司聘任”该字段，
        if("zhicheng".equals(nckd_type)) {
            // 显示“是否公司聘任”该字段
            this.getView().setVisible(true , "nckd_ispinren");
        } else {
            // 隐藏,“是否公司聘任”该字段
            this.getView().setVisible(false , "nckd_ispinren");
            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            // 隐藏：聘任日期
            this.getView().setVisible(false , "nckd_pinrendate");
            // API地址设置为非必填，页面上的必填和数据校验的必填中去掉
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(false);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(false);

            // 隐藏：聘任终止日期
            this.getView().setVisible(false , "nckd_pinrenenddaten");

            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(false);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop3.setMustInput(false);
        }

        Boolean nckd_ispinren = (Boolean)model.getValue("nckd_ispinren");
        if(nckd_ispinren) {
            // 勾选了“是否聘任”，显示：聘任日期
            this.getView().setVisible(true , "nckd_pinrendate");
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(true);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(true);
            // 显示：聘任终止日期
            this.getView().setVisible(true , "nckd_pinrenenddaten");
            // 显示：聘任单位
            this.getView().setVisible(true , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(true);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop3.setMustInput(true);
        }else {
            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            // 隐藏：聘任日期
            this.getView().setVisible(false , "nckd_pinrendate");
            // API地址设置为非必填，页面上的必填和数据校验的必填中去掉
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(false);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(false);

            // 隐藏：聘任终止日期
            this.getView().setVisible(false , "nckd_pinrenenddaten");

            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(false);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop3.setMustInput(false);
        }
    }

    protected Map<String, Object> diffDialogOrForm() {
        Map<String, Object> diffMap = super.diffDialogOrForm();
        diffMap.put("attachmentpanelap_std", "attachmentpanelap_std"); // 附件
        return diffMap;
    }

    // 字段修改事件
    public void propertyChanged(PropertyChangedArgs propertyChangedArgs) {
        String fieldKey = propertyChangedArgs.getProperty().getName();
        Object newValue = propertyChangedArgs.getChangeSet()[0].getNewValue();
        if (newValue != null) {
            switch (fieldKey) {
                case "awardtime":  // 授予时间
                    this.validOnAwardTimeChanged((Date)newValue);
                    break;
                case "firsttime": // 第一次复审时间
                    this.validOnFirstTimeChanged((Date)newValue);
                    break;
                case "secondtime": // 第二次复审时间
                    this.validOnSecondTimeChanged((Date)newValue);
                    break;
                case "nckd_type": // 类型
                    this.typeChange((String)newValue);
                    break;
                case "nckd_iszuigao":  // 是否最高 值切换
                    Boolean nckd_iszuigao = (Boolean)newValue;
                    if(nckd_iszuigao) {
                        this.getView().showMessage("该职称/职业技能登记将保存为最高职称/职业技能");
                    }
                    break;
                case "nckd_ispinren": // 是否公司聘任 值切换
                    this.ispinrenChange((Boolean)newValue);
                    break;
            }

        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        if("do_save".equals(afterDoOperationEventArgs.getOperateKey())) {
            // 个人仅允许保存一条最高职称和一条最高职业技能，当开关开启后，原来最高的那条职称/职业技能的“是否最高”开关自动关闭
            IDataModel model = this.getModel();
            Boolean nckd_iszuigao = (Boolean)model.getValue("nckd_iszuigao");
            if(nckd_iszuigao) {
                DBRoute hr = new DBRoute("hr");
                OperationStatus status = this.getView().getFormShowParameter().getStatus();

                Long personid =(Long) this.getView().getFormShowParameter().getCustomParam("person");
                // 类型
                String nckd_type = (String)model.getValue("nckd_type");

                // 该人该类型下全部记录更新为非最高（因为当前这条记录会更新为最高，下面的sql更新不影响当前这条记录）
                SqlBuilder builder = new SqlBuilder();
                builder.append("UPDATE t_hrpi_perprotitle SET fk_nckd_iszuigao='0' WHERE fiscurrentversion='1' and fpersonid = ? and fk_nckd_iszuigao='1' and fk_nckd_type = ?", personid, nckd_type);

                boolean execute = DB.execute(hr, builder);
            }
        }
    }

    private boolean validateDate() {
        boolean isTip = this.checkTipTime();
        Date secondTime = this.getDateIfExist("secondtime"); // 第二次复审时间
        Date firstTime = this.getDateIfExist("firsttime"); // 第一次复审时间
        Date awardTime = this.getDateIfExist("awardtime"); // 授予时间
        return this.validDate(awardTime, firstTime, isTip, ResManager.loadKDString("授予日期不得晚于第一次复审日期", "PerProTitleEditPlugin_1", "hr-hspm-formplugin", new Object[0])) && this.validDate(awardTime, secondTime, isTip, ResManager.loadKDString("授予日期不得晚于第二次复审日期", "PerProTitleEditPlugin_2", "hr-hspm-formplugin", new Object[0])) && this.validDate(firstTime, secondTime, isTip, ResManager.loadKDString("第一次复审日期不得晚于第二次复审日期", "PerProTitleEditPlugin_4", "hr-hspm-formplugin", new Object[0]));
    }

    private boolean validOnAwardTimeChanged(Date awardTime) {
        Date secondTime = this.getDateIfExist("secondtime"); // 第二次复审时间
        Date firstTime = this.getDateIfExist("firsttime"); // 第一次复审时间
        return this.validDate(awardTime, firstTime, true, ResManager.loadKDString("授予日期不得晚于第一次复审日期", "PerProTitleEditPlugin_1", "hr-hspm-formplugin", new Object[0])) && this.validDate(awardTime, secondTime, true, ResManager.loadKDString("授予日期不得晚于第二次复审日期", "PerProTitleEditPlugin_2", "hr-hspm-formplugin", new Object[0]));
    }

    private boolean validOnFirstTimeChanged(Date firstTime) {
        Date secondTime = this.getDateIfExist("secondtime"); // 第二次复审时间
        Date awardTime = this.getDateIfExist("awardtime"); // 授予时间
        return this.validDate(awardTime, firstTime, true, ResManager.loadKDString("授予日期不得晚于第一次复审日期", "PerProTitleEditPlugin_1", "hr-hspm-formplugin", new Object[0])) && this.validDate(firstTime, secondTime, true, ResManager.loadKDString("第一次复审日期不得晚于第二次复审日期", "PerProTitleEditPlugin_4", "hr-hspm-formplugin", new Object[0]));
    }

    private boolean validOnSecondTimeChanged(Date secondTime) {
        Date firstTime = this.getDateIfExist("firsttime"); // 第一次复审时间
        Date awardTime = this.getDateIfExist("awardtime"); // 授予时间
        return this.validDate(awardTime, secondTime, true, ResManager.loadKDString("授予日期不得晚于第二次复审日期", "PerProTitleEditPlugin_2", "hr-hspm-formplugin", new Object[0])) && this.validDate(firstTime, secondTime, true, ResManager.loadKDString("第一次复审日期不得晚于第二次复审日期", "PerProTitleEditPlugin_4", "hr-hspm-formplugin", new Object[0]));
    }

    // 根据类型值进行其它字段操作-二开cch
    public void typeChange(String nckd_type) {
        if("zhicheng".equals(nckd_type)) {
            // 显示“是否公司聘任”该字段
            this.getView().setVisible(true , "nckd_ispinren");
        } else {
            // 隐藏,“是否公司聘任”该字段
            this.getView().setVisible(false , "nckd_ispinren");
            this.getModel().setValue("nckd_ispinren",false); // 取消“是否公司聘任”勾选
            // 隐藏，聘任日期，聘任终止日期，聘任单位
            this.ispinrenChange(false);
        }
    }

    // 根据是否公司聘任进行其它字段操作-二开cch
    public void ispinrenChange(Boolean nckd_ispinren) {
        if(nckd_ispinren) {
            // 显示：聘任单位
            this.getView().setVisible(true , "nckd_pinrenorg");
            BasedataEdit apiaddressPropertyp1 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressPropertyp1.setMustInput(true);
            BasedataProp propp1 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            propp1.setMustInput(true);
            // 显示：聘任日期
            this.getView().setVisible(true , "nckd_pinrendate");
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(true);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(true);
            // 显示：聘任终止日期
            this.getView().setVisible(true , "nckd_pinrenenddaten");
        }else {
            // 隐藏：聘任日期
            // 置空3个值
            this.getModel().setValue("nckd_pinrendate",null); // 聘任日期
            this.getModel().setValue("nckd_pinrenenddaten",null); // 聘任终止日期
            this.getModel().setValue("nckd_pinrenorg",null); // 聘任单位
            // 隐藏：聘任日期
            this.getView().setVisible(false , "nckd_pinrendate");
            // API地址设置为非必填，页面上的必填和数据校验的必填中去掉
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(false);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(false);

            // 隐藏：聘任终止日期
            this.getView().setVisible(false , "nckd_pinrenenddaten");

            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(false);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop3.setMustInput(false);
        }
    }

    protected Map<String, String> getPageParams() {
        Map<String, String> pageParams = super.getPageParams();
        pageParams.put(PAGE_ID, "hrpi_perprotitle");
        return pageParams;
    }

    protected boolean validInputParams() {
        return this.validateDate();
    }
}
