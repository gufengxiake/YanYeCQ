package nckd.yanye.hr.plugin.form;/*
 *@title EmpCountryCardPlugin
 *@description  人员档案-国家职称信息的字段显示
 *@author jyx
 *@version 1.0
 *@create 2024/4/10 11:34
 */

import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

public class EmpCountryCardPlugin extends AbstractCardDrawEdit {

    public EmpCountryCardPlugin() {
    }
//显示字段数据
    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Long personId = HRJSONUtils.getLongValOfCustomParam(preBindDataVo.getFormShowParameter().getCustomParam("person"));
        if (personId != null && personId != 0L) {
            //显示人员国家信息页签数据
            CardViewCompareVo compareVo = new CardViewCompareVo("nckd_nationaldate", "nckd_nationaldate", "nckd_countryname,nckd_approvedcountry,nckd_comapny");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "nckd_hrpi_country", "nckd_nationaldate desc");
            this.childPointModify(new CardBindDataDTO(this.getModel(), this.getView(), args, compareVo, this.getTimeMap(), queryDbVo));
            List<String> extFields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            queryDbVo.setFields(extFields);
            this.queryAndAssDataFromDb(queryDbVo);//查询数据
            this.defineSpecial(new DefineSpecialVo(true, "shamedit_", "shamdel_",null ,null));
            return preBindDataVo;
        } else {
            return preBindDataVo;
        }
    }

    protected boolean customChangeLabelValue(BeforeCreatVo beforeCreatVo) {
        Map<String, Object> dataMap = beforeCreatVo.getDataMap();
        Map<String, Object> labMap = beforeCreatVo.getLabMap();
        Map<String, String> relMap = beforeCreatVo.getRelMap();
        int index = beforeCreatVo.getIndex();
        String key = (String)labMap.get("number");
        Object value = null;
        if (dataMap.get(key) instanceof String) {
            value = dataMap.get(key);
        }

        if("nckd_nationaldate".equals(key)){

            relMap.put(key, "取得国家职业资格的时间："+relMap.get(key));

        }


        return false;
    }

}

