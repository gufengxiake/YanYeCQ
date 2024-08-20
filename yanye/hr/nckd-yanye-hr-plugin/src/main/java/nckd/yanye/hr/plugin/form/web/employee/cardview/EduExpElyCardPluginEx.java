package nckd.yanye.hr.plugin.form.web.employee.cardview;

import kd.bos.dataentity.Tuple;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.metadata.form.Border;
import kd.bos.metadata.form.Style;
import kd.bos.metadata.form.control.LabelAp;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.sdk.hr.hspm.common.dto.FieldDTO;
import kd.sdk.hr.hspm.formplugin.web.file.employee.base.BigElyTimeAxisCardEdit;

import java.util.List;
import java.util.Objects;

/**
 * HR员工自助PC端-教育经历-列表
 * 动态表单标识：nckd_hspm_pereduexp_p_ext
 * author:chengchaohua
 * date:2024-08-19
 */
public class EduExpElyCardPluginEx extends BigElyTimeAxisCardEdit {

    private static final HRBaseServiceHelper SERVICE_HELPER = new HRBaseServiceHelper("hrpi_pereduexp");

    public EduExpElyCardPluginEx() {}

    // 设置标题名称：毕业学校名称
    public void setTitleField(FieldDTO titleField) {
        titleField.addField("hrpi_pereduexp.nckd_biyexuexiaoname");
    }

    // 副标题
    public void setTagField(FieldDTO tagField) {
        tagField.addField("hrpi_pereduexp.ishighestdegree"); // 是否最高学历
        tagField.addField("hrpi_pereduexp.isfulltime"); // 是否全日制
    }

    // 内容
    public void setContentField(FieldDTO contentFields) {
        contentFields.addField("hrpi_pereduexp.education"); // 学历
        contentFields.addField("hrpi_pereduexp.degree"); // 学位
        contentFields.addField("hrpi_pereduexp.major"); // 第一专业
    }

    // admissiondate:入学日期, gradutiondate:毕业日期
    public void setTimeLineField(List<Tuple<String, String>> timeLineField) {
        timeLineField.add(Tuple.create("hrpi_pereduexp.admissiondate", "hrpi_pereduexp.gradutiondate"));
    }

    public boolean hasDeleteOperate() {
        return true;
    }

    public boolean hasAddOperate() {
        return true;
    }

    public boolean hasEditOperate() {
        return true;
    }

    // admissiondate:入学日期, gradutiondate:毕业日期,
    public String getOrderBys(String tableName) {
        return "admissiondate desc,gradutiondate desc,createtime desc";
    }

    protected void changeLabelStyle(LabelAp labelAp) {
        String key = labelAp.getId();
        if (key != null) {
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
                    border.setTop("1px_solid_#666666");
                    border.setBottom("1px_solid_#666666");
                    border.setLeft("1px_solid_#666666");
                    border.setRight("1px_solid_#666666");
                } else {
                    labelAp.setName(new LocaleString(""));
                    labelAp.setStyle(new Style());
                }

            }
        }
    }

    protected void handleDel(String key) {
        String[] delKey = this.getDelKey(key);
        String id = delKey[2];
        DynamicObject dy = SERVICE_HELPER.queryOne("ishighestdegree,datastatus", Long.valueOf(id));
        if (Objects.isNull(dy)) {
            this.getView().showErrorNotification(ResManager.loadKDString("数据已不存在，请刷新页面", "EduExpElyCardPlugin_0", "hr-hspm-formplugin", new Object[0]));
        } else {
            super.handleDel(key);
        }
    }

    private boolean queryEduByPersonId(Long personId) {
        QFilter initStatusFilter = new QFilter("initstatus", "=", "2");
        QFilter curFilter = new QFilter("iscurrentversion", "=", "1");
        QFilter conFilter = new QFilter("person", "=", personId);
        QFilter dataFilter = new QFilter("datastatus", "=", "1");
        return SERVICE_HELPER.count(SERVICE_HELPER.getEntityName(), new QFilter[]{curFilter, initStatusFilter, dataFilter, conFilter}) > 1;
    }

}
