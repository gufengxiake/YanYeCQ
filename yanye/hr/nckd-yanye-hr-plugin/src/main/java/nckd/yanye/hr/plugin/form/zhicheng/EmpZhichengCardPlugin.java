package nckd.yanye.hr.plugin.form.zhicheng;

import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * 开发平台：核心人力云-》人员信息-》附表卡片-》职称信息（nckd_hspm_perprotitl_ext4）源页面: hspm_perprotitle_dv
 * 动态表单插件
 * 菜单：人员档案-》职称及技能信息
 * author:程超华
 * date:2024-08-06
 */
public class EmpZhichengCardPlugin extends AbstractCardDrawEdit {

    public EmpZhichengCardPlugin() {

    }

    @Override
    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Object personObj = preBindDataVo.getFormShowParameter().getCustomParam("person");
        Long personId = HRJSONUtils.getLongValOfCustomParam(personObj);
        if (personId != null && personId != 0L) {
            // 头部：类型 nckd_type ，标题：职称/职业技能名称 nckd_zhichengname ，具体业务字段：
            CardViewCompareVo compareVo = new CardViewCompareVo((String)null, "nckd_type", "nckd_zhichengname", "awardtime,unit,approvnum,office,firsttime,secondtime,description", "attachmentpanelap_std");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId), new QFilter("iscurrentversion", "=", "1")};
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "hrpi_perprotitle", "createtime desc");
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

    @Override
    protected Map<String, Object> defineSpecial(DefineSpecialVo defineSpecialVo) {
        Map<String, Object> timeMap = super.defineSpecial(defineSpecialVo);
        timeMap.put("attach", Boolean.TRUE);
        timeMap.put("delattach", "attachmentpanelap_std");
        timeMap.put("delattachform", "hrpi_perprotitle");
        timeMap.put("viewshowdialog", "1");
        return timeMap;
    }
}
