package nckd.yanye.hr.plugin.form.web.activity;

import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.field.DateEdit;
import kd.hr.hom.formplugin.web.activity.AbstractCollectDynViewPlugin;

import java.util.Date;
import java.util.EventObject;

/**
 * PC端-》信息采集协作-》基本信息 ,当是退伍军人，入伍和退伍时间必填处理,点击保存时进行校验
 * 信息组页面 ：nckd_hom_infogroupdyn_ext
 * 2024-09-14
 * chengchaohua
 */
public class InfoGroupDynViewPluginEx extends AbstractCollectDynViewPlugin {

    @Override
    public void registerListener(EventObject e) {
        Button button = this.getView().getControl("savesingles001"); // 基本信息的保存按钮
        button.addClickListener(this);
    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        super.beforeClick(evt);
        String key = ((Control)evt.getSource()).getKey();
        // 基本信息 保存标识：savesingles001
        if ("savesingles001".equals(key)) {
            this.getModel();
            String istuiwu = (String)this.getModel().getValue("field2004353791935130624");
            if ("YES".equals(istuiwu)) {
                // 入伍时间 field2004391669838916608
                Date ruwudate = (Date)this.getModel().getValue("field2004391669838916608");
                String warnstr = "";
                if (ruwudate == null) {
                    warnstr = warnstr + "入伍时间、";
                }
                // 退伍时间 field2004391669838916609
                Date tuiwudate = (Date)this.getModel().getValue("field2004391669838916609");
                if (tuiwudate == null) {
                    warnstr = warnstr + "退伍时间、";
                }
                if (warnstr.length() > 0) {
                    this.getView().showTipNotification("以下信息为必填，请填写：" + warnstr.substring(0,warnstr.length() -1));
                    evt.setCancel(true);
                } else {
                    // 日期比较
                    if (!ruwudate.before(tuiwudate)) {
                        this.getView().showTipNotification("退伍时间要大于入伍时间" );
                        evt.setCancel(true);
                    }
                }
            }
        }
    }

    /**
     * PC端-》信息采集协作-》基本信息,页面加载显示后的字段必填性显示
     * @param e
     */
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        String istuiwu = (String)this.getModel().getValue("field2004353791935130624");
        if ("YES".equals(istuiwu)) {
            // 是退伍军人，设置必填
            // 前端属性设置（前端判断必填校验）
            DateEdit property1 = (DateEdit) this.getControl("field2004391669838916608"); // 入伍时间
            property1.setMustInput(true);
            DateEdit property2 = (DateEdit) this.getControl("field2004391669838916609"); // 退伍时间
            property2.setMustInput(true);
        }
    }

}
