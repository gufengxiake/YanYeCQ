package nckd.yanye.hr.plugin.form.web.employee.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.SmallElyCardEdit;

/**
 * HR员工自助PC端-奖励信息-列表
 * author:chengchaohua
 * date:2024-08-16
 */
public class PerrprecordElyCardPlugin extends SmallElyCardEdit
{
    public PerrprecordElyCardPlugin(){

    }

    // 设置标题名称：奖励名称
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_perrprecord.nckd_jiangliname");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_perrprecord.rewarddate"); // 奖惩日期,奖励日期
        contentFields.addField("hrpi_perrprecord.nckd_jiangxiangdanwei"); // 奖项批准单位名称
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
        return "createtime desc";
    }
}
