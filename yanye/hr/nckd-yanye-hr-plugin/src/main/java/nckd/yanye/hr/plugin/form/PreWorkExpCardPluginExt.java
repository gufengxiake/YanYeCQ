package nckd.yanye.hr.plugin.form;
/*
 *@title PreWorkExpCardPluginExt
 *@description
 *@author jyx
 *@version 1.0
 *@create 2024/4/16 10:41
 */

import kd.bos.dataentity.resource.ResManager;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

public class PreWorkExpCardPluginExt extends AbstractCardDrawEdit {
    private static final String HEAD_FIELDS = "unitname";
    private static final String TEXT_FIELDS = "isabroadbackground";
    private static final String CONTENT_FIELDS = "startdate,enddate,department,position,jobdesc,witness,witnessphone,description";
    private static final String ORDER_BY = "startdate desc,enddate desc,createtime desc";

    public PreWorkExpCardPluginExt() {
    }

    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Object personObj = preBindDataVo.getFormShowParameter().getCustomParam("person");
        Long personId = HRJSONUtils.getLongValOfCustomParam(personObj);
        if (personId != null && personId != 0L) {
            CardViewCompareVo compareVo = new CardViewCompareVo((String)null, "unitname", "isabroadbackground", "description,nckd_ryleibie,nckd_rynum,nckd_tycompany,nckd_rydept,nckd_ryposition,nckd_ryzjm,nckd_ryfcbj,nckd_rydzri,exitdate,nckd_rygwdj,nckd_rygwxl,nckd_ryzwmc,nckd_ryzyw,nckd_rysfzg,nckd_ryjrly,nckd_rysfzz,nckd_ryzrrq,nckd_ryxcfltfrq,nckd_ryjytj,nckd_ryyywbs,nckd_ryyywbh,nckd_rygsfw,nckd_onpostdate", "attachmentpanelap_std");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId), new QFilter("iscurrentversion", "=", "1")};
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "hrpi_preworkexp", "startdate desc,enddate desc,createtime desc");
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

    protected boolean createLabel(BeforeCreatVo beforeCreatVo) {
        if (!"text".equals(beforeCreatVo.getLabType())) {
            return super.createLabel(beforeCreatVo);
        } else {
            Map<String, Object> labMap = beforeCreatVo.getLabMap();
            Map<String, Object> dataMap = beforeCreatVo.getDataMap();
            String key = (String)labMap.get("number");
            if ("isabroadbackground".equals(key)) {
                return !(Boolean)dataMap.get(key);
            } else {
                return super.createLabel(beforeCreatVo);
            }
        }
    }

    protected boolean customChangeLabelValue(BeforeCreatVo beforeCreatVo) {
        if (!"text".equals(beforeCreatVo.getLabType())) {
            return false;
        } else {
            Map<String, Object> dataMap = beforeCreatVo.getDataMap();
            Map<String, Object> labMap = beforeCreatVo.getLabMap();
            Map<String, String> relMap = beforeCreatVo.getRelMap();
            String key = (String)labMap.get("number");
            if ("isabroadbackground".equals(key) && Boolean.parseBoolean(dataMap.get(key).toString())) {
                relMap.put(key, ResManager.loadKDString("海外工作背景", "PreWorkExpCardPlugin_0", "hr-hspm-formplugin", new Object[0]));
            }

            return false;
        }
    }

    protected Map<String, Object> defineSpecial(DefineSpecialVo defineSpecialVo) {
        Map<String, Object> timeMap = super.defineSpecial(defineSpecialVo);
        timeMap.put("delattach", "attachmentpanelap_std");
        timeMap.put("delattachform", "hrpi_preworkexp");
        timeMap.put("attach", Boolean.TRUE);
        timeMap.put("viewshowdialog", "1");
        return timeMap;
    }
}

