package nckd.yanye.hr.plugin.form.project;

import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.metadata.form.Style;
import kd.bos.metadata.form.control.LabelAp;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心人力云->人员信息->附表卡片
 * 项目经历 nckd_hspm_empproexp_d_ext 动态表单
 * 2024-07-26
 * chengchaohua
 */
public class EmpProjectExpCardPlugin extends AbstractCardDrawEdit {
    private static final Log logger = LogFactory.getLog(EmpProjectExpCardPlugin.class);

    public EmpProjectExpCardPlugin() {

    }

    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Long personId = HRJSONUtils.getLongValOfCustomParam(preBindDataVo.getFormShowParameter().getCustomParam("person"));
        if (personId != null && personId != 0L) {
            // 头部：项目名称 nckd_projectname ，标题： ，具体业务字段：
            CardViewCompareVo compareVo = new CardViewCompareVo("nckd_projectname", "area,projecttype", "startdate,enddate,role,duty,certifier,witnessphone,description");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "hrpi_empproexp", "startdate desc,enddate desc,createtime desc");
            this.childPointModify(new CardBindDataDTO(this.getModel(), this.getView(), args, compareVo, this.getTimeMap(), queryDbVo));
            List<String> extFields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            queryDbVo.setFields(extFields);
            this.queryAndAssDataFromDb(queryDbVo);
            this.defineSpecial(new DefineSpecialVo(false, (String)null, (String)null, "shamedit_", "shamdel_"));
            return preBindDataVo;
        } else {
            return preBindDataVo;
        }
    }

    protected Map<String, Object> defineSpecial(DefineSpecialVo dsVo) {
        Map<String, Object> defineMap = super.defineSpecial(dsVo);
        defineMap.put("viewshowdialog", "1");
        return defineMap;
    }

    protected void customChangeLabelStyle(AfterCreatVo afterCreatVo) {
        String labType = afterCreatVo.getLabType();
        Map<String, Object> filedMap = afterCreatVo.getFiledMap();
        Style style = afterCreatVo.getStyle();
        LabelAp fieldAp = afterCreatVo.getFieldAp();
        if ("text".equals(labType)) {
            String field = (String)filedMap.get("number");
            Map<String, String> colorMap = new HashMap(16);
            if ("area".equals(field)) {
                colorMap.put("forColor", "#1BA854");
                colorMap.put("backColor", "rgba(242,255,245,0.1)");
            } else if ("projecttype".equals(field)) {
                colorMap.put("forColor", "#276FF5");
                colorMap.put("backColor", "rgba(133,184,255,0.1)");
            }

            this.setLabelColorStyle(new TextColorVo(style, fieldAp, (String)colorMap.get("forColor"), (String)colorMap.get("backColor"), "100px"));
        }

    }
}
