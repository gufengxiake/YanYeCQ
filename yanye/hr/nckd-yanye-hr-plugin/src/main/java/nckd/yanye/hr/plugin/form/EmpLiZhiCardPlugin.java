package nckd.yanye.hr.plugin.form;


import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.metadata.form.Style;
import kd.bos.metadata.form.control.LabelAp;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;

import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心人力云-》人员信息-》附表卡片-》离职信息（nckd_hspm_hrpi_lizhi_dv）
 * author：刘少波
 * 动态表单插件
 */
public class EmpLiZhiCardPlugin extends AbstractCardDrawEdit {
    private static final Log logger = LogFactory.getLog(EmpLiZhiCardPlugin.class);

    public EmpLiZhiCardPlugin() {

    }

    //   ,nckd_lizhinumtype, nckd_lizhibefdepart, nckd_lizhibefposition, nckd_lizhibefjob, nckd_lizhiaftnumtype, nckd_lizhiaftdepart, nckd_salarystopdate, nckd_welfarestopdate, nckd_remark, nckd_isnew, nckd_company, nckd_rehireflag, nckd_onduty, nckd_lizhiaftcompany, nckd_lizhiaftbdnumtype
    // 唯一标识：取元数据nckd_hspm_hrpi_lizhi_dv下内容控件contentaplizhi里contentap后面的字符lizhi
    @Override
    protected String childPlanAp() {
        return "lizhi";
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
            // 头部：离职类型 nckd_departuretype ，标题：离职原因 nckd_departurecause ，离职说明 nckd_departureexplain，具体业务字段：
            CardViewCompareVo compareVo = new CardViewCompareVo("nckd_departuretype",
                    "nckd_departurecause",
                    "nckd_lizhidate,nckd_departuretype,nckd_company,nckd_lizhiaftcompany,nckd_lizhiaftbdnumtype,nckd_lizhiaftdepart");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            // 离职信息基础页面 页面编码：nckd_hrpi_lizhiinform ，字段：离职日期 nckd_lizhidate
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "nckd_hrpi_lizhiinform", "nckd_lizhidate desc");
            this.childPointModify(new CardBindDataDTO(this.getModel(), this.getView(), args, compareVo, this.getTimeMap(), queryDbVo));
            List<String> extFields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            queryDbVo.setFields(extFields);
            this.queryAndAssDataFromDb(queryDbVo);
            //  卡片记录右上角修改和新增的设置按钮
            this.defineSpecial(new DefineSpecialVo(true, "shamedit_", "shamdel_", null, null));
            return preBindDataVo;
        } else {
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
/*
        int index = beforeCreatVo.getIndex();
        String key = (String)labMap.get("number");
        String titlename = (String)labMap.get("displayname");
        Object value = null;
        if (dataMap.get(key) instanceof String) {
            value = dataMap.get(key);
        }
        // 自定义显示值：nckd_hetongcode 合同编码, nckd_qiandingtype 签订类型
        if ("nckd_hetongcode".equals(key) || "nckd_qiandingtype".equals(key)) {
            relMap.put(key, titlename + "：" + value.toString());
        }
*/

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
            // 标题字段：离职原因 nckd_departurecause设置样式
            if ("nckd_departurecause".equals(field)) {
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
