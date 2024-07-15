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

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心人力云-》人员信息-》附表卡片-》人员聘用情况（nckd_hspm_hrpi_perpyqk_dv）
 * 动态表单插件
 * author:程超华 2024-04-11
 */
public class EmpPersonPingYongCardPlugin extends AbstractCardDrawEdit {
    private static final Log logger = LogFactory.getLog(EmpPersonPingYongCardPlugin.class);

    public EmpPersonPingYongCardPlugin() {
    }

    // 唯一标识：取元数据 nckd_hspm_hrpi_perpyqk_dv 下内容控件 contentapperpyqk 里contentap后面的字符 perpyqk
    @Override
    protected String childPlanAp() {
        return "perpyqk";
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
            // 头部：录聘时间 nckd_lupingdate ，标题：录聘标识 nckd_lupingsign ，具体业务字段：进入方式 nckd_inputtype, 录聘用来源 nckd_lupingsource
            CardViewCompareVo compareVo = new CardViewCompareVo("nckd_lupingdate", "nckd_lupingsign",
                    "nckd_inputtype,nckd_lupingsource");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            // 聘用情况基础页面 页面编码：nckd_hrpi_personpingyqk，字段：录聘时间 nckd_lupingdate
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "nckd_hrpi_personpingyqk", "nckd_lupingdate desc");
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

        if (dataMap.get(key) instanceof String) {
            value = dataMap.get(key);
        }
        // 日期字段
        if ("nckd_lupingdate".equals(key)) {
            SimpleDateFormat smp = new SimpleDateFormat("yyyy-MM-dd");
            Timestamp luppingdate = (Timestamp )dataMap.get(key);
            if (luppingdate != null) {
                value = smp.format(luppingdate);
            }
        }

        // 自定义显示值：录聘时间 nckd_lupingdate ，标题：录聘标识 nckd_lupingsign
        if ("nckd_lupingdate".equals(key) || "nckd_lupingsign".equals(key)) {
            relMap.put(key, titlename + "：" + value);
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
            // 标题字段：录聘标识 nckd_lupingsign 设置样式
            if ("nckd_lupingsign".equals(field)) {
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
