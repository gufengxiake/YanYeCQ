package nckd.yanye.hr.plugin.form.zhicheng;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.db.SqlBuilder;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.DateProp;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.UploadEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.ComboEdit;
import kd.bos.form.field.DateEdit;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.sdk.hr.hspm.business.service.AttacheHandlerService;
import kd.sdk.hr.hspm.common.utils.HspmDateUtils;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractFormDrawEdit;
import kd.sdk.plugin.Plugin;

import java.util.*;

/**
 *核心人力云->人员信息-》附表弹框
 * 人员档案，职称信息，页面编码: nckd_hspm_perprotitl_ext3
 * 2024-07-26
 * chengchaohua
 */
public class EmpZhiChengHrpiPlugin extends AbstractFormDrawEdit {

    public EmpZhiChengHrpiPlugin() {}


    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        OperationStatus status = formShowParameter.getStatus();
        if (OperationStatus.EDIT.equals(status) || OperationStatus.VIEW.equals(status)) {
            this.setValueFromDb(formShowParameter, "hrpi_perprotitle", (String)null);
            this.setAttachment("hrpi_perprotitle", "attachmentpanelap_std");
        }

        this.getModel().setDataChanged(false);

        // 二开部分--start
        IDataModel model = this.getModel();

        Boolean nckd_ispinren = (Boolean)model.getValue("nckd_ispinren"); // 是否聘任
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
        // 二开部分--end
    }

    public void registerListener(EventObject eventObject) {
        super.registerListener(eventObject);
        this.addClickListeners(new String[]{"btnsave"});
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control)evt.getSource();
        String key = source.getKey();
        OperationStatus status = this.getView().getFormShowParameter().getStatus();
        if ("btnsave".equals(key)) {
            if (!this.validateDate()) {
                return;
            }

            Map<String, Object> resultMap = new HashMap(16);
            if (OperationStatus.EDIT.equals(status)) {
                resultMap = this.updateAttachData("hrpi_perprotitle", this.getView(), false, (String)null);
            } else if (OperationStatus.ADDNEW.equals(status)) {
                resultMap = this.addAttachData("hrpi_perprotitle", this.getView(), this.getModel().getDataEntity(), false);
            }

            Object pkId = this.getView().getFormShowParameter().getCustomParam("pkid");
            this.successAfterSave(pkId, (Map)resultMap, "attachmentpanelap_std", "hrpi_perprotitle");
            AttacheHandlerService.getInstance().closeView(this.getView(), (Map)resultMap, this.getView().getParentView());
        }

    }

    public void remove(UploadEvent evt) {
        this.defaultRemoveAttachment(evt);
    }

    public void upload(UploadEvent evt) {
        this.defaultUploadAttachment(evt);
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        IDataModel model = this.getModel();
        // 1)
        // 2)
        if(StringUtils.equals("nckd_iszuigao", fieldKey)) {
            // 是否最高 值切换
            Boolean nckd_iszuigao = (Boolean)model.getValue("nckd_iszuigao");
            if(nckd_iszuigao) {
                this.getView().showMessage("该职称/职业技能登记将保存为最高职称/职业技能");
            }
        }
        // 3)
        if(StringUtils.equals("nckd_ispinren", fieldKey)) {
            // 是否公司聘任 值切换
            Boolean nckd_ispinren = (Boolean)model.getValue("nckd_ispinren");
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
    }


    private boolean validateDate() {
        boolean isTip = this.checkTipTime();
        Date secondTime = this.getDateIfExist("secondtime");
        Date firstTime = this.getDateIfExist("firsttime");
        Date awardTime = this.getDateIfExist("awardtime");
        Date lastSecond = HspmDateUtils.getMidnight();
        return this.validDate(awardTime, firstTime, isTip, ResManager.loadKDString("授予日期需早于第一次复审日期", "PerProTitleEditPlugin_1", "hr-hspm-formplugin", new Object[0])) && this.validDate(awardTime, secondTime, isTip, ResManager.loadKDString("授予日期需早于第二次复审日期", "PerProTitleEditPlugin_2", "hr-hspm-formplugin", new Object[0])) && this.validDate(firstTime, secondTime, isTip, ResManager.loadKDString("第一次复审日期需早于第二次复审日期", "PerProTitleEditPlugin_4", "hr-hspm-formplugin", new Object[0]));
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
    }

    // 二开部分
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

                // 该人全部记录更新为非最高
                SqlBuilder builder = new SqlBuilder();
                builder.append("UPDATE t_hrpi_perprotitle SET fk_nckd_iszuigao='0' WHERE fiscurrentversion='1' and fpersonid = ? and fk_nckd_iszuigao='1' and fk_nckd_type = ?", personid, nckd_type);

                boolean execute = DB.execute(hr, builder);
            }
        }
    }

    private boolean validOnAwardTimeChanged(Date awardTime) {
        Date secondTime = this.getDateIfExist("secondtime");
        Date firstTime = this.getDateIfExist("firsttime");
        return this.validDate(awardTime, firstTime, true, ResManager.loadKDString("授予日期需早于第一次复审日期", "PerProTitleEditPlugin_1", "hr-hspm-formplugin", new Object[0])) && this.validDate(awardTime, secondTime, true, ResManager.loadKDString("授予日期需早于第二次复审日期", "PerProTitleEditPlugin_2", "hr-hspm-formplugin", new Object[0]));
    }

    private boolean validOnFirstTimeChanged(Date firstTime) {
        Date secondTime = this.getDateIfExist("secondtime");
        Date awardTime = this.getDateIfExist("awardtime");
        return this.validDate(awardTime, firstTime, true, ResManager.loadKDString("授予日期需早于第一次复审日期", "PerProTitleEditPlugin_1", "hr-hspm-formplugin", new Object[0])) && this.validDate(firstTime, secondTime, true, ResManager.loadKDString("第一次复审日期需早于第二次复审日期", "PerProTitleEditPlugin_4", "hr-hspm-formplugin", new Object[0]));
    }

    private boolean validOnSecondTimeChanged(Date secondTime) {
        Date firstTime = this.getDateIfExist("firsttime");
        Date awardTime = this.getDateIfExist("awardtime");
        return this.validDate(awardTime, secondTime, true, ResManager.loadKDString("授予日期需早于第二次复审日期", "PerProTitleEditPlugin_2", "hr-hspm-formplugin", new Object[0])) && this.validDate(firstTime, secondTime, true, ResManager.loadKDString("第一次复审日期需早于第二次复审日期", "PerProTitleEditPlugin_4", "hr-hspm-formplugin", new Object[0]));
    }

    protected Map<String, Object> diffDialogOrForm() {
        Map<String, Object> diffMap = super.diffDialogOrForm();
        diffMap.put("attachmentpanelap_std", "attachmentpanelap_std");
        return diffMap;
    }
}
