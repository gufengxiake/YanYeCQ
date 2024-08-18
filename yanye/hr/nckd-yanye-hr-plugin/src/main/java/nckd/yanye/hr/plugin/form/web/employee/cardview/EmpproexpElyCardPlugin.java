package nckd.yanye.hr.plugin.form.web.employee.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.SmallElyCardEdit;

/**
 * HR员工自助PC端-项目经历-列表
 * 动态表单标识：nckd_hspm_empproexp_pdv
 * author:chengchaohua
 * date:2024-08-18
 */
public class EmpproexpElyCardPlugin extends SmallElyCardEdit
{
    public EmpproexpElyCardPlugin(){

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
