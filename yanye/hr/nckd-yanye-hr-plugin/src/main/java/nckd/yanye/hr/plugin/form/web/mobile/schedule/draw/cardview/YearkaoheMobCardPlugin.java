package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

/**
 * HR员工自助移动端-年度考核-列表
 * 移动动态表单标识：nckd_hspm_yearkaohe_mdv
 * author:chengchaohua
 * date:2024-08-18
 */
public class YearkaoheMobCardPlugin extends AbstractMobCardEdit {

    public YearkaoheMobCardPlugin() {

    }

    // 设置标题名称：考核年度
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("nckd_hrpi_yearkaohe.nckd_kaoheyear");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("nckd_hrpi_yearkaohe.nckd_kaoheresult"); // 考核结果
        contentFields.addField("nckd_hrpi_yearkaohe.nckd_pingjiaorg"); // 评价单位
        contentFields.addField("nckd_hrpi_yearkaohe.nckd_wcjreason"); // 未参加考核原因
    }

    // 有删除按钮
    protected boolean hasDeleteOperate() {
        return true;
    }

    // 有新增按钮
    protected boolean hasAddOperate() {
        return true;
    }

    protected String getOrderBys(String tableName) {
        return "nckd_kaoheyear desc";
    }
}
