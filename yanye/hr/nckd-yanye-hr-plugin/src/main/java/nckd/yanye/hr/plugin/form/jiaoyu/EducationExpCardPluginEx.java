package nckd.yanye.hr.plugin.form.jiaoyu;

import kd.bos.dataentity.resource.ResManager;
import kd.bos.form.FormShowParameter;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.business.service.AttacheHandlerService;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * 标识：nckd_hspm_pereduexp_d_ext ，名称 教育经历 ，源单据：hspm_pereduexp_dv
 * 菜单：人员档案-》教育经历
 * 把标准版的代码都拷贝过来，然后进行修改，再禁用标准版的插件
 * author:程超华
 * date:2024-08-25
 */
public class EducationExpCardPluginEx  extends AbstractCardDrawEdit {

    public EducationExpCardPluginEx() {}

    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        boolean main = AttacheHandlerService.getInstance().judgeIsMain(this.getView().getFormShowParameter());
        if (main) {
            this.getView().setVisible(Boolean.FALSE, new String[]{"flexpanelap"});
        }

        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        Long personId = HRJSONUtils.getLongValOfCustomParam(formShowParameter.getCustomParam("person"));
        if (AttacheHandlerService.getInstance().judgeIsMain(formShowParameter)) {
            this.getView().setVisible(Boolean.FALSE, new String[]{"marginflexpanelap"});
        }

        if (personId != null && personId != 0L) {
            // 序时卡片，卡片上面紧挨显示admissiondate 入学日期，gradutiondate 毕业日期
            // 标题：nckd_biyexuexiaoname 毕业学校名称
            // 内容：isfulltime 是否全日制，ishighestdegree 是否最高学历，isoverseas 是否海外教育经历
            // nckd_biyeyuanxiname 毕业院系名称，education 学历，degree 学位，major 第一专业
            // nckd_isquanrizuigaoxl 是否全日制最高学历,nckd_isfeiquanrizuigxl 是否非全日制最高学历,
            CardViewCompareVo compareVo = new CardViewCompareVo("admissiondate,gradutiondate", "nckd_biyexuexiaoname", "isfulltime,ishighestdegree,isoverseas", "nckd_biyeyuanxiname,education,degree,major,nckd_isquanrizuigaoxl,nckd_isfeiquanrizuigxl", (String)null);
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "hrpi_pereduexp", "admissiondate desc,gradutiondate desc,createtime desc");
            this.childPointModify(new CardBindDataDTO(this.getModel(), this.getView(), args, compareVo, this.getTimeMap(), queryDbVo));
            this.queryAndAssDataFromDb(queryDbVo);
            List<String> extFields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            queryDbVo.setFields(extFields);
            this.defineSpecial(new DefineSpecialVo(true, "shamedit_", "shamdel_", (String)null, (String)null));
            return preBindDataVo;
        } else {
            return preBindDataVo;
        }
    }

    protected void handlerDel(String labelName) {
        super.handlerDel(labelName);
    }

    protected boolean customChangeLabelValue(BeforeCreatVo beforeCreatVo) {
        super.customChangeLabelValue(beforeCreatVo);
        Map<String, Object> dataMap = beforeCreatVo.getDataMap();
        Map<String, Object> labMap = beforeCreatVo.getLabMap();
        Map<String, String> relMap = beforeCreatVo.getRelMap();
        String key = (String)labMap.get("number");
        Object value = null;
        if (dataMap.get(key) instanceof String) {
            value = dataMap.get(key);
        }

        if ("1".equals(value)) {
            switch (key) {
                case "isfulltime":
                    relMap.put(key, ResManager.loadKDString("全日制", "EducationExpCardPlugin_0", "hr-hspm-formplugin", new Object[0]));
                    break;
                case "ishighestdegree":
                    relMap.put(key, ResManager.loadKDString("最高学历", "EducationExpCardPlugin_2", "hr-hspm-formplugin", new Object[0]));
            }
        }

        if (key.equals("isoverseas") && (Boolean)dataMap.get(key)) {
            relMap.put(key, ResManager.loadKDString("海外经历", "EducationExpCardPlugin_1", "hr-hspm-formplugin", new Object[0]));
        }

        return false;
    }

    protected boolean createLabel(BeforeCreatVo beforeCreatVo) {
        Map<String, Object> labMap = beforeCreatVo.getLabMap();
        Map<String, Object> dataMap = beforeCreatVo.getDataMap();
        String field = (String)labMap.get("number");
        String hideFields = "isfulltime,ishighestdegree";
        if (hideFields.contains(field)) {
            String value = (String)dataMap.get(field);
            if (!"1".equals(value)) {
                return true;
            }
        }

        if (field.equals("isoverseas")) {
            Boolean isOverseas = (Boolean)dataMap.get(field);
            return !isOverseas;
        } else {
            return false;
        }
    }

    protected Map<String, Object> defineSpecial(DefineSpecialVo dsVo) {
        Map<String, Object> defineMap = super.defineSpecial(dsVo);
        defineMap.put("viewshowdialog", "1");
        return defineMap;
    }

}
