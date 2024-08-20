package nckd.yanye.hr.plugin.form.web.mobile.schedule.draw.cardview;

import kd.bos.dataentity.Tuple;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.metadata.form.Border;
import kd.bos.metadata.form.Style;
import kd.bos.metadata.form.control.LabelAp;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.mobile.file.base.AbstractMobCardEdit;

import java.util.List;

/**
 * HR员工自助移动端-教育经历-列表
 * 动态表单标识：nckd_hspm_pereduexp_m_ext
 * author:chengchaohua
 * date:2024-08-19
 */
public class EduExpMobCardPluginEx extends AbstractMobCardEdit {

    public EduExpMobCardPluginEx(){}
    private static final HRBaseServiceHelper SERVICE_HELPER = new HRBaseServiceHelper("hrpi_pereduexp");

    // 设置标题名称：毕业学校名称
    protected void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_pereduexp.nckd_biyexuexiaoname");
    }

    protected void setTagField(FieldDTO tagField) {
        tagField.addField("hrpi_pereduexp.ishighestdegree"); // 是否最高学历
        tagField.addField("hrpi_pereduexp.isfulltime"); // 是否全日制
    }

    protected boolean hasContentTitle() {
        return false;
    }

    protected void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_pereduexp.major"); // 第一专业
        contentFields.addField("hrpi_pereduexp.education"); // 学历
        contentFields.addField("hrpi_pereduexp.admissiondate"); // 入学日期
        contentFields.addField("hrpi_pereduexp.gradutiondate"); // 毕业日期
    }

    protected boolean hasDeleteOperate() {
        return true;
    }

    protected boolean hasAddOperate() {
        return true;
    }

    protected String getOrderBys(String tableName) {
        return "admissiondate desc,gradutiondate desc,createtime desc";
    }

    protected void changeLabel(LabelAp labelAp) {
        String key = labelAp.getId();
        String name = "";
        boolean isReturn = true;
        switch (key) {
            case "hrpi_pereduexp.isfulltime":
                name = ResManager.loadKDString("全日制", "EducationExpCardPlugin_0", "hr-hspm-formplugin", new Object[0]);
                isReturn = false;
                break;
            case "hrpi_pereduexp.ishighestdegree":
                name = ResManager.loadKDString("最高学历", "EducationExpCardPlugin_2", "hr-hspm-formplugin", new Object[0]);
                isReturn = false;
                break;
            case "hrpi_pereduexp.major":
            case "hrpi_pereduexp.education":
                labelAp.setFontWeight("400");
                labelAp.setForeColor("#212121");
        }

        if (!isReturn) {
            if (labelAp.getStyle() == null) {
                labelAp.setStyle(new Style());
            }

            if (labelAp.getStyle().getBorder() == null) {
                labelAp.getStyle().setBorder(new Border());
            }

            Border border = labelAp.getStyle().getBorder();
            if ("true".equals(labelAp.getName().getLocaleValue())) {
                labelAp.setName(new LocaleString(name));
                labelAp.setForeColor("#666666");
                border.setTop("0.5px_solid_#CCCCCC");
                border.setBottom("0.5px_solid_#CCCCCC");
                border.setLeft("0.5px_solid_#CCCCCC");
                border.setRight("0.5px_solid_#CCCCCC");
            } else {
                labelAp.setName(new LocaleString(""));
                labelAp.setStyle(new Style());
            }

        }
    }

    protected void setTimeLineField(List<Tuple<String, String>> timeLineField) {
        timeLineField.add(Tuple.create("hrpi_pereduexp.admissiondate", "hrpi_pereduexp.gradutiondate"));
    }

    protected void handleDel(String key) {
        super.handleDel(key);
    }

    private boolean queryEduByPersonId(Long personId) {
        QFilter initStatusFilter = new QFilter("initstatus", "=", "2");
        QFilter conFilter = new QFilter("person", "=", personId);
        QFilter dataFilter = new QFilter("datastatus", "=", "1");
        QFilter curFilter = new QFilter("iscurrentversion", "=", "1");
        return SERVICE_HELPER.count(SERVICE_HELPER.getEntityName(), new QFilter[]{curFilter, initStatusFilter, dataFilter, conFilter}) > 1;
    }
}
