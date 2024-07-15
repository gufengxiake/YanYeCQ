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
 * 核心人力云-》人员信息-》附表卡片-》处分信息（nckd_hspm_chufeninfo_dv）
 * 动态表单插件
 * author:程超华 2024-07-12
 */
public class EmpChuFenInfoCardPlugin extends AbstractCardDrawEdit {
    private static final Log logger = LogFactory.getLog(EmpChuFenInfoCardPlugin.class);

    public EmpChuFenInfoCardPlugin() {
    }

    // 唯一标识：取元数据nckd_hspm_chufeninfo_dv下内容控件contentapchufeninfo里contentap后面的字符chufeninfo
    @Override
    protected String childPlanAp() {
        return "chufeninfo";
    }

    /**
     * 卡片数据展示
     * @param args
     * @return
     */
    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Long personId = HRJSONUtils.getLongValOfCustomParam(preBindDataVo.getFormShowParameter().getCustomParam("person"));
        if (personId != null && personId != 0L) {
            // 头部：处分日期 nckd_chufendate ，标题：处分名称 nckd_chufenname ，具体业务字段：
            CardViewCompareVo compareVo = new CardViewCompareVo("nckd_chufendate", "nckd_chufenname",
                    "nckd_chufentype,nckd_chufenenddate,nckd_chufenpizhun,nckd_chufenwenhao");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            // 年度考核信息基础页面 页面编码：nckd_hrpi_chufeninfo，排序字段：处分日期 nckd_chufendate
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "nckd_hrpi_chufeninfo", "nckd_chufendate desc");
            this.childPointModify(new CardBindDataDTO(this.getModel(), this.getView(), args, compareVo, this.getTimeMap(), queryDbVo));
            List<String> extFields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            queryDbVo.setFields(extFields);
            this.queryAndAssDataFromDb(queryDbVo);
            // 卡片记录右上角修改和新增的设置按钮
            this.defineSpecial(new DefineSpecialVo(true, "shamedit_", "shamdel_",null ,null));
            return preBindDataVo;
        } else {
            return preBindDataVo;
        }
    }

    /**
     * 对查询后的数据进行修改
     * @param beforeCreatVo
     * @return
     */
    @Override
    protected boolean customChangeLabelValue(BeforeCreatVo beforeCreatVo) {
        Map<String, Object> dataMap = beforeCreatVo.getDataMap();
        Map<String, Object> labMap = beforeCreatVo.getLabMap();
        Map<String, String> relMap = beforeCreatVo.getRelMap();
        int index = beforeCreatVo.getIndex();
        String key = (String)labMap.get("number");
        String titlename = (String)labMap.get("displayname");
        Object value = null;
        // 自定义显示值：nckd_chufendate 处分日期, 处分名称 nckd_chufenname
        if ("nckd_chufendate".equals(key) || "nckd_chufenname".equals(key)) {

                value = dataMap.get(key);
                if ("nckd_chufendate".equals(key) && value != null) {
                    value = value.toString().substring(0,10);
                }

            relMap.put(key, titlename + "：" + value.toString());
        }

        return false;
    }

    /**
     * 对显示列名设置样式颜色
     * @param afterCreatVo
     */
    protected void customChangeLabelStyle(AfterCreatVo afterCreatVo) {
        String labType = afterCreatVo.getLabType();
        Map<String, Object> filedMap = afterCreatVo.getFiledMap();
        Style style = afterCreatVo.getStyle();
        LabelAp fieldAp = afterCreatVo.getFieldAp();
        if ("text".equals(labType)) {
            String field = (String)filedMap.get("number");
            Map<String, String> colorMap = new HashMap(16);
            // 标题字段：处分名称 nckd_chufenname 设置样式
            if ("nckd_chufenname".equals(field)) {
                colorMap.put("forColor", "#276FF5");
                colorMap.put("backColor", "rgba(133,184,255,0.1)");
            }

            this.setLabelColorStyle(new TextColorVo(style, fieldAp, (String)colorMap.get("forColor"), (String)colorMap.get("backColor"), "100px"));
        }

    }

    /**
     * 定制化
     * @param dsVo
     * @return
     */
    protected Map<String, Object> defineSpecial(DefineSpecialVo dsVo) {
        Map<String, Object> defineMap = super.defineSpecial(dsVo);
        // 在雇佣人员主档案页面，点击内容进入查看界面
        defineMap.put("viewshowdialog", "1");
        return defineMap;
    }
}
