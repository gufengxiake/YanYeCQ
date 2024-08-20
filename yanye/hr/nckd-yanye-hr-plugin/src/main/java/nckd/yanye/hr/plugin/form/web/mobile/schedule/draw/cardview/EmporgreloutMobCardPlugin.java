package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

/**
 * HR员工自助移动端-上线前任职经历-列表
 * 移动动态表单标识：nckd_hspm_emporgrelo_mdv
 * author:chengchaohua
 * date:2024-08-19
 */
public class EmporgreloutMobCardPlugin extends AbstractMobCardEdit {

    public EmporgreloutMobCardPlugin() {

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
