package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.metadata.form.Border;
import kd.bos.metadata.form.Style;
import kd.bos.metadata.form.control.LabelAp;
import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

/**
 * HR员工自助移动端-职称及技能信息-列表
 * 移动动态表单标识：nckd_hspm_perprotitl_ext6
 * author:chengchaohua
 * date:2024-08-19
 */
public class PerProTitleMobCardPluginEx extends AbstractMobCardEdit {

    public PerProTitleMobCardPluginEx(){}

    protected void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_perprotitle.nckd_zhichengname"); // 职称/职业技能名称
    }

    protected void setTagField(FieldDTO tagField) {
        tagField.addField("hrpi_perprotitle.ishigh");
    } // 是否最高职称

    protected void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_perprotitle.nckd_zhichenglevel"); // 职称/职业技能等级
        contentFields.addField("hrpi_perprotitle.awardtime"); // 授予时间
        contentFields.addField("hrpi_perprotitle.unit"); // 评定单位
        contentFields.addField("hrpi_perprotitle.office"); // 发证机关
        contentFields.addField("hrpi_perprotitle.approvnum"); // 批准文号
        contentFields.addField("hrpi_perprotitle.description"); // 描述
    }

    protected boolean hasAddOperate() {
        return true;
    }

    protected boolean hasDeleteOperate() {
        return true;
    }

    protected void changeLabel(LabelAp labelAp) {
        if ("hrpi_perprotitle.ishigh".equals(labelAp.getId())) {
            if ("true".equals(labelAp.getName().getLocaleValue())) {
                labelAp.setName(new LocaleString(ResManager.loadKDString("最高职称", "PerProTitleElyCardPlugin_0", "hr-hspm-formplugin", new Object[0])));
                if (labelAp.getStyle() == null) {
                    labelAp.setStyle(new Style());
                }

                if (labelAp.getStyle().getBorder() == null) {
                    labelAp.getStyle().setBorder(new Border());
                }

                labelAp.setForeColor("#666666");
                labelAp.getStyle().getBorder().setTop("0.5px_solid_#CCCCCC");
                labelAp.getStyle().getBorder().setBottom("0.5px_solid_#CCCCCC");
                labelAp.getStyle().getBorder().setLeft("0.5px_solid_#CCCCCC");
                labelAp.getStyle().getBorder().setRight("0.5px_solid_#CCCCCC");
            } else {
                labelAp.setName(new LocaleString(""));
                if (labelAp.getStyle() == null) {
                    labelAp.setStyle(new Style());
                }

                if (labelAp.getStyle().getBorder() == null) {
                    labelAp.getStyle().setBorder(new Border());
                }

                labelAp.getStyle().getBorder().setTop("0px_solid_#CCCCCC");
                labelAp.getStyle().getBorder().setBottom("0px_solid_#CCCCCC");
                labelAp.getStyle().getBorder().setLeft("0px_solid_#CCCCCC");
                labelAp.getStyle().getBorder().setRight("0px_solid_#CCCCCC");
            }
        }

    }

    protected String getOrderBys(String tableName) {
        return "createtime desc";
    }
}
