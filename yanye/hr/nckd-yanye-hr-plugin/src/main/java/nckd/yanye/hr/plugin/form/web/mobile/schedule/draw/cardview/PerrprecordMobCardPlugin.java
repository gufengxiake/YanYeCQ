package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

/**
 * HR员工自助移动端-奖励信息-列表
 * 移动动态表单标识：nckd_hspm_perrprecord_mdv
 * author:chengchaohua
 * date:2024-08-17
 */
public class PerrprecordMobCardPlugin extends AbstractMobCardEdit {

    public PerrprecordMobCardPlugin() {

    }

    // 设置标题名称：奖励名称
    protected void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_perrprecord.nckd_jiangliname");
    }

    // 设置内容行字段
    protected void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_perrprecord.rewarddate"); // 奖惩日期,奖励日期
        contentFields.addField("hrpi_perrprecord.nckd_jiangxiangdanwei"); // 奖项批准单位名称
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
        return "createtime desc";
    }
}
