package nckd.yanye.hr.plugin.form.web.employee.cardview;


import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.SmallElyCardEdit;

/**
 * HR员工自助PC端-处分信息-列表
 * 动态表单标识：nckd_hspm_chufeninfo_pdv
 * author:chengchaohua
 * date:2024-10-11
 */
public class ChuFenInFoElyCardPlugin extends SmallElyCardEdit {

    public ChuFenInFoElyCardPlugin(){};

    // 设置标题名称：处分名称 nckd_chufenname
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("nckd_hrpi_chufeninfo.nckd_chufenname");
    }


    // 设置内容行字段
    // nckd_leibie 处分类别,nckd_chufentype 处理结果,nckd_chufenenddate 处分影响结束日期
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("nckd_hrpi_chufeninfo.nckd_leibie"); // 处分类别
        contentFields.addField("nckd_hrpi_chufeninfo.nckd_chufentype"); // 处理结果
        contentFields.addField("nckd_hrpi_chufeninfo.nckd_chufenenddate"); // 处分影响结束日期
    }

    // 有删除按钮
    public boolean hasDeleteOperate() {
        return false;
    }
    // 有编辑按钮
    public boolean hasEditOperate() {
        return false;
    }
    // 有新增按钮
    public boolean hasAddOperate() {
        return false;
    }
    // 数据排序 处分日期 nckd_chufendate
    public String getOrderBys(String tableName) {
        return "nckd_chufendate desc";
    }

}
