package nckd.yanye.hr.plugin.form.web.employee.cardview;

import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.metadata.form.Border;
import kd.bos.metadata.form.Style;
import kd.bos.metadata.form.control.LabelAp;
import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.BigElyCardEdit;

/**
 * HR员工自助移动端-职称及技能信息-列表
 * 移动动态表单标识：nckd_hspm_perprotitl_ext5
 * author:chengchaohua
 * date:2024-08-19
 */
public class PerProTitleElyCardPluginEx extends BigElyCardEdit {

    public PerProTitleElyCardPluginEx(){}

    public void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_perprotitle.nckd_zhichengname"); // 职称/职业技能名称
        titleField.addField("hrpi_perprotitle.nckd_zhichenglevel"); // 职称/职业技能等级
    }

    // 副标题
    public void setTagField(FieldDTO tagField) {
        tagField.addField("hrpi_perprotitle.ishigh");
    } // 是否最高职称

    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_perprotitle.awardtime"); // 授予时间
        contentFields.addField("hrpi_perprotitle.unit"); // 评定单位
        contentFields.addField("hrpi_perprotitle.office"); // 发证机关
        contentFields.addField("hrpi_perprotitle.approvnum"); // 批准文号
        contentFields.addField("hrpi_perprotitle.description"); // 描述
    }

    public boolean hasAddOperate() {
        return true;
    }

    public boolean hasEditOperate() {
        return true;
    }

    public boolean hasDeleteOperate() {
        return true;
    }

    protected void changeLabelStyle(LabelAp labelAp) {
        if ("hrpi_perprotitle.ishigh".equals(labelAp.getId())) {
            if (labelAp.getStyle() == null) {
                labelAp.setStyle(new Style());
            }

            if (labelAp.getStyle().getBorder() == null) {
                labelAp.getStyle().setBorder(new Border());
            }

            Border border = labelAp.getStyle().getBorder();
            if ("true".equals(labelAp.getName().getLocaleValue())) {
                labelAp.setName(new LocaleString(ResManager.loadKDString("最高职称", "PerProTitleElyCardPlugin_0", "hr-hspm-formplugin", new Object[0])));
                labelAp.setForeColor("#666666");
                border.setTop("1px_solid_#666666");
                border.setBottom("1px_solid_#666666");
                border.setLeft("1px_solid_#666666");
                border.setRight("1px_solid_#666666");
            } else {
                labelAp.setName(new LocaleString(""));
                labelAp.setStyle((Style)null);
            }
        }

    }
}
