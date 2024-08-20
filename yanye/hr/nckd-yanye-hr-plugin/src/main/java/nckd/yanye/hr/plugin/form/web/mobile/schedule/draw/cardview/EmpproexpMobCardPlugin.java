package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

/**
 * HR员工自助移动端-年度考核-列表
 * 移动动态表单标识：nckd_hspm_yearkaohe_mdv
 * author:chengchaohua
 * date:2024-08-18
 */
public class EmpproexpMobCardPlugin extends AbstractMobCardEdit {

    public EmpproexpMobCardPlugin() {

    }

    // 设置标题名称：项目名称
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_empproexp.nckd_projectname");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_empproexp.startdate"); // 开始日期
        contentFields.addField("hrpi_empproexp.enddate"); // 结束日期
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
        return "startdate desc";
    }
}
