package nckd.yanye.hr.plugin.form;


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
 * 核心人力云-》人员信息-》附表卡片-》任岗信息（nckd_hspm_hrpi_rengang_dv）
 * 动态表单插件
 * author：刘少波
 */
public class EmpRenGangCardPlugin extends AbstractCardDrawEdit {
    private static final Log logger = LogFactory.getLog(EmpRenGangCardPlugin.class);

    public EmpRenGangCardPlugin() {

    }

    // 唯一标识：取元数据nckd_hspm_hrpi_rengang_dv下内容控件contentaprengang里contentap后面的字符rengang
    @Override
    protected String childPlanAp() {
        return "rengang";
    }

    /**
     * 卡片数据展示
     *
     * @param args
     * @return
     */
    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Long personId = HRJSONUtils.getLongValOfCustomParam(preBindDataVo.getFormShowParameter().getCustomParam("person"));
        if (personId != null && personId != 0L) {
            // 头部：任职开始日期 nckd_startdate ，标题：任职结束日期 nckd_enddate ，任职单位 nckd_affiliation，具体业务字段：
            CardViewCompareVo compareVo = new CardViewCompareVo("nckd_startdate",
                    "nckd_enddate", "nckd_position,nckd_department,nckd_affiliation");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            // 任职信息基础页面 页面编码：nckd_hrpi_workinform ，字段：起始日期 nckd_startdate，终止日期 nckd_enddate，创建时间 createtime
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "nckd_hrpi_workinform", "nckd_startdate desc,nckd_enddate desc");
            this.childPointModify(new CardBindDataDTO(this.getModel(), this.getView(), args, compareVo, this.getTimeMap(), queryDbVo));
            List<String> extFields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            queryDbVo.setFields(extFields);
            this.queryAndAssDataFromDb(queryDbVo);
            //  卡片记录右上角修改和新增的设置按钮
            this.defineSpecial(new DefineSpecialVo(true, "shamedit_", "shamdel_", null, null));
            return preBindDataVo;
        }
        {
            return preBindDataVo;
        }
    }

    /**
     * 对查询后的数据进行修改
     *
     * @param beforeCreatVo
     * @return
     */
    @Override
    protected boolean customChangeLabelValue(BeforeCreatVo beforeCreatVo) {
        Map<String, Object> dataMap = beforeCreatVo.getDataMap();
        Map<String, Object> labMap = beforeCreatVo.getLabMap();
        Map<String, String> relMap = beforeCreatVo.getRelMap();
        return false;
    }

    /**
     * 对显示列名设置样式颜色
     *
     * @param afterCreatVo
     */
    protected void customChangeLabelStyle(AfterCreatVo afterCreatVo) {
        String labType = afterCreatVo.getLabType();
        Map<String, Object> filedMap = afterCreatVo.getFiledMap();
        Style style = afterCreatVo.getStyle();
        LabelAp fieldAp = afterCreatVo.getFieldAp();
        if ("text".equals(labType)) {
            String field = (String) filedMap.get("number");
            Map<String, String> colorMap = new HashMap(16);
            // 标题字段：签订类型 nckd_qiandingtype 设置样式
            if ("nckd_enddate".equals(field)) {
                colorMap.put("forColor", "#276FF5");
                colorMap.put("backColor", "rgba(133,184,255,0.1)");
            }

            this.setLabelColorStyle(new TextColorVo(style, fieldAp, (String) colorMap.get("forColor"), (String) colorMap.get("backColor"), "100px"));
        }
    }

    /**
     * 卡片布局和大小设置
     *
     * @param dsVo
     * @return
     */
    protected Map<String, Object> defineSpecial(DefineSpecialVo dsVo) {
        Map<String, Object> defineMap = super.defineSpecial(dsVo);
        // 在雇佣人员主档案页面默认折叠显示（不展开显示内容）
        defineMap.put("viewshowdialog", "1");
        return defineMap;
    }
}