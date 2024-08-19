package nckd.yanye.hr.plugin.form.web.employee.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.SmallElyCardEdit;

/**
 * HR员工自助PC端-上线前任职经历-列表
 * 动态表单标识：nckd_hspm_emporgrelo_pdv
 * author:chengchaohua
 * date:2024-08-19
 */
public class EmporgreloutElyCardPlugin extends SmallElyCardEdit
{
    public EmporgreloutElyCardPlugin(){

    }

    // 设置标题名称：所属公司
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("nckd_hrpi_emporgrelout.company");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("nckd_hrpi_emporgrelout.adminorg"); // 行政组织
        contentFields.addField("nckd_hrpi_emporgrelout.position"); // 岗位
        contentFields.addField("nckd_hrpi_emporgrelout.startdate"); // 开始日期
        contentFields.addField("nckd_hrpi_emporgrelout.enddate"); // 结束日期
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
