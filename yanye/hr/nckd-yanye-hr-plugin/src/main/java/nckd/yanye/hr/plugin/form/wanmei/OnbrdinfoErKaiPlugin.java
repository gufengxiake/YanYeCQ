package nckd.yanye.hr.plugin.form.wanmei;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillOperationStatus;
import kd.bos.bill.IBillView;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.ComboProp;
import kd.bos.entity.property.DecimalProp;
import kd.bos.entity.property.IntegerProp;
import kd.bos.entity.property.TextProp;
import kd.bos.form.field.ComboEdit;
import kd.bos.form.field.DecimalEdit;
import kd.bos.form.field.IntegerEdit;
import kd.bos.form.field.TextEdit;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.hr.hbp.formplugin.web.HRDataBaseEdit;

import java.util.EventObject;

/**
 * 入职办理单 nckd_hom_onbrdinfo_ext
 * date:2024-07-24
 * author:chengchaohua
 */
public class OnbrdinfoErKaiPlugin  extends AbstractBillPlugIn {

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        IDataModel model = this.getModel();
        // 是否有实习期 nckd_isshixiqi
        Boolean nckd_isshixiqi = (Boolean)model.getValue("nckd_isshixiqi");
        if(nckd_isshixiqi){
            // 如果有实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
            // 前端属性设置
            DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
            nckd_shixidikouProperty.setMustInput(true);
            // 后端属性设置
            DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
            prop.setMustInput(true);
            // 解锁
            this.getView().setEnable(true, "nckd_shixidikou");

            // 前端属性设置
            ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
            nckd_perprobationtimedkProperty.setMustInput(true);
            // 后端属性设置
            ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
            prop2.setMustInput(true);
            // 解锁
            this.getView().setEnable(true, "nckd_perprobationtimedk");

        } else {
            // 如果无实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
            // 前端属性设置
            DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
            nckd_shixidikouProperty.setMustInput(false);
            // 后端属性设置
            DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
            prop.setMustInput(false);
            // 锁定
            this.getView().setEnable(false, "nckd_shixidikou");

            // 前端属性设置
            ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
            nckd_perprobationtimedkProperty.setMustInput(false);
            // 后端属性设置
            ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
            prop2.setMustInput(false);
            // 锁定
            this.getView().setEnable(false, "nckd_perprobationtimedk");
        }
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        DynamicObject workcalendar = (DynamicObject)this.getModel().getValue("workcalendar");
        if (workcalendar == null) {
            Long workid= 1964567443623906304L;
            this.getModel().setValue("workcalendar",workid);
        }

        // 获取当前登录人id
        long currentUserId = UserServiceHelper.getCurrentUserId();
        this.getModel().setValue("handler", currentUserId);

    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        IDataModel model = this.getModel();
        // 是否有实习期 nckd_isshixiqi
        if(StringUtils.equals("nckd_isshixiqi", fieldKey)) {
            Boolean nckd_isshixiqi = (Boolean)model.getValue("nckd_isshixiqi");
            if(nckd_isshixiqi){
                // 如果有实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
                // 前端属性设置
                DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
                nckd_shixidikouProperty.setMustInput(true);
                // 后端属性设置
                DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
                prop.setMustInput(true);
                // 解锁
                this.getView().setEnable(true, "nckd_shixidikou");

                // 前端属性设置
                ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
                nckd_perprobationtimedkProperty.setMustInput(true);
                // 后端属性设置
                ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
                prop2.setMustInput(true);
                // 解锁
                this.getView().setEnable(true, "nckd_perprobationtimedk");

            } else {
                // 如果无实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
                // 前端属性设置
                DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
                nckd_shixidikouProperty.setMustInput(false);
                // 后端属性设置
                DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
                prop.setMustInput(false);
                // 锁定
                this.getView().setEnable(false, "nckd_shixidikou");

                // 前端属性设置
                ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
                nckd_perprobationtimedkProperty.setMustInput(false);
                // 后端属性设置
                ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
                prop2.setMustInput(false);
                // 锁定
                this.getView().setEnable(false, "nckd_perprobationtimedk");
            }
        }
    }

}
