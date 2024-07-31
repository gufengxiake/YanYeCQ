package nckd.yanye.hr.plugin.form.reg.web.applybill;

import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.hr.hbp.formplugin.web.HRCoreBaseBillEdit;
import kd.hr.hdm.business.reg.domain.service.bill.IPersonAboutService;
import kd.hr.hdm.common.transfer.util.ObjectUtils;
import kd.hr.hdm.formplugin.reg.web.applybill.RegPageUtils;

import java.util.Arrays;
import java.util.EventObject;
import java.util.Map;

/**
 * 单据界面插件
 * author:tangyuxuan
 * date:2024-07-30
 */
public class RegBaseBillPluginEx extends HRCoreBaseBillEdit {

    public RegBaseBillPluginEx() {

    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
    }

    private static final Log Logger = LogFactory.getLog(RegBaseBillPluginEx.class);


    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        Object erManFile = this.getView().getFormShowParameter().getCustomParam("ermanfile");
        if (!ObjectUtils.isEmpty(erManFile)) {
            this.getView().setVisible(Boolean.TRUE, new String[]{"panelbarcode"});
            this.getModel().setValue("ermanfile", erManFile);
            this.getView().setEnable(Boolean.FALSE, new String[]{"ermanfile", "org"});
            this.showPersonByErManFile(true);
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        ChangeData changeData = e.getChangeSet()[0];
        switch (e.getProperty().getName()) {
            case "ermanfile":
                if (!ObjectUtils.isEmpty(changeData.getNewValue())) {
                    this.showPersonByErManFile(true);
                }
            default:
        }
    }

    private void showPersonByErManFile(boolean forceRefresh) {

        IPersonAboutService personAboutService = IPersonAboutService.getInstance();
        Map<String, Object> personReturnMap = personAboutService.buildPersonChangeParams(this.getModel().getDataEntity().getDynamicObject("ermanfile"));
        if (!RegPageUtils.haveLicence((Long)personReturnMap.get("person_id"), this.getView())) {
            this.getModel().setValue("ermanfile", (Object)null);
        } else {
            String formId = this.getView().getFormShowParameter().getFormId();
            if ("hdm_regselfhelpbill".equals(formId)) {
                return;
            }

            Map<String, Object> regReturnMap = personAboutService.buildRegInfo(this.getModel().getDataEntity().getDynamicObject("ermanfile"));
            /*  probation 实习期抵扣后试用期时长
                probationunit 试用期单位
                nckd_isshixiqi 是否有实习期
                nckd_hetongshiyong 合同约定试用期时长
                nckd_perprobationtime 单位
                nckd_shixidikou 实习期时长（可抵扣试用期
                nckd_perprobationtimedk 单位
            */

            if (!ObjectUtils.isEmpty(regReturnMap)) {
                this.getModel().setValue("probation", regReturnMap.get("probation"));
                this.getModel().setValue("probationunit", regReturnMap.get("probationunit"));
                this.getModel().setValue("nckd_isshixiqi", regReturnMap.get("nckd_isshixiqi"));
                this.getModel().setValue("nckd_hetongshiyong", regReturnMap.get("nckd_hetongshiyong"));
                this.getModel().setValue("nckd_perprobationtime", regReturnMap.get("nckd_perprobationtime"));
                this.getModel().setValue("nckd_shixidikou", regReturnMap.get("nckd_shixidikou"));
                this.getModel().setValue("nckd_perprobationtimedk", regReturnMap.get("nckd_perprobationtimedk"));
            }

        }

    }

}