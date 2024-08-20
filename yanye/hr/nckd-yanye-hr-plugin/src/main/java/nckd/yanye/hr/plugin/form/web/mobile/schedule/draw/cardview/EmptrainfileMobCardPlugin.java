package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

/**
 * HR员工自助移动端-培训经历-列表
 * 移动动态表单标识：nckd_hspm_emptrainfi_mdv
 * author:chengchaohua
 * date:2024-08-19
 */
public class EmptrainfileMobCardPlugin extends AbstractMobCardEdit {

    public EmptrainfileMobCardPlugin() {

    }

    // 设置标题名称：培训课程
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_emptrainfile.name");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_emptrainfile.startdate"); // 开始日期
        contentFields.addField("hrpi_emptrainfile.enddate"); // 结束日期
        contentFields.addField("hrpi_emptrainfile.traintype"); // 培训类别
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
