package nckd.yanye.hr.plugin.form.web.employee.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.SmallElyCardEdit;

/**
 * HR员工自助PC端-培训经历-列表
 * 动态表单标识：nckd_hspm_emptrainfi_pdv
 * author:chengchaohua
 * date:2024-08-19
 */
public class EmptrainfileElyCardPlugin extends SmallElyCardEdit
{
    public EmptrainfileElyCardPlugin(){

    }

    // 设置标题名称：培训课程
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_emptrainfile.name");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_emptrainfile.startdate"); // 开始日期
        contentFields.addField("hrpi_emptrainfile.enddate"); // 结束日期
        contentFields.addField("hrpi_emptrainfile.traintype"); // 培训类别（基础资料），默认显示名称
    }

    // 有删除按钮
    public boolean hasDeleteOperate() {
        return true;
    }
    // 有编辑按钮
    public boolean hasEditOperate() {
        return true;
    }
    // 有新增按钮
    public boolean hasAddOperate() {
        return true;
    }
    // 数据排序
    public String getOrderBys(String tableName) {
        return "startdate desc";
    }
}
