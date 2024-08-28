package nckd.yanye.hr.plugin.form.web.employee.cardview;

import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.SmallElyCardEdit;

/**
 * HR员工自助PC端-年度考核-列表
 * 动态表单标识：nckd_hspm_yearkaohe_pdv
 * author:chengchaohua
 * date:2024-08-18
 */
public class YearkaoheElyCardPlugin extends SmallElyCardEdit
{
    public YearkaoheElyCardPlugin(){

    }

    // 设置标题名称：考核年度
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("nckd_hrpi_yearkaohe.nckd_kaoheyear");
    }

    // 设置内容行字段
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("nckd_hrpi_yearkaohe.nckd_kaoheresult"); // 考核结果
        contentFields.addField("nckd_hrpi_yearkaohe.nckd_pingjiaorg"); // 评价单位
        contentFields.addField("nckd_hrpi_yearkaohe.nckd_wcjreason"); // 未参加考核原因
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
    // 数据排序
    public String getOrderBys(String tableName) {
        return "nckd_kaoheyear desc";
    }
}
