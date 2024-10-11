package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

/**
 * HR员工自助移动端-处分信息-列表
 * 移动动态表单标识：nckd_hspm_chufeninfo_mdv
 * author:chengchaohua
 * date:2024-10-11
 */
public class ChuFenInFoMobCardPlugin extends AbstractMobCardEdit {

    public ChuFenInFoMobCardPlugin() {}

    // 设置标题名称：处分名称 nckd_chufenname
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("nckd_hrpi_chufeninfo.nckd_chufenname");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("nckd_hrpi_chufeninfo.nckd_leibie"); // 处分类别
        contentFields.addField("nckd_hrpi_chufeninfo.nckd_chufentype"); // 处理结果
        contentFields.addField("nckd_hrpi_chufeninfo.nckd_chufenenddate"); // 处分影响结束日期
    }

    // 有删除按钮-true显示，false不显示
    protected boolean hasDeleteOperate() {
        return false;
    }

    // 有新增按钮
    protected boolean hasAddOperate() {
        return false;
    }

    protected boolean enableEnterDetail() {
        return false;
    }

    // 数据排序 处分日期 nckd_chufendate
    protected String getOrderBys(String tableName) {
        return "nckd_chufendate desc";
    }

}
