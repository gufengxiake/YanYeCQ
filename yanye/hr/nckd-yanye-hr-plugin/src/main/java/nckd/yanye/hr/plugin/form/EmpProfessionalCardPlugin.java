package nckd.yanye.hr.plugin.form;/*
 *@title EmpProfessionalCardEditPlugin
 *@description
 *@author jyx
 *@version 1.0
 *@create 2024/4/11 13:51
 */

import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.EventObject;
import java.util.List;

public class EmpProfessionalCardPlugin extends AbstractCardDrawEdit {

    public EmpProfessionalCardPlugin() {
    }
    //显示字段数据
    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Long personId = HRJSONUtils.getLongValOfCustomParam(preBindDataVo.getFormShowParameter().getCustomParam("person"));
        if (personId != null && personId != 0L) {
            //显示人员国家信息页签数据
            CardViewCompareVo compareVo = new CardViewCompareVo("nckd_dengbegin,nckd_dengend", "nckd_dindate", "nckd_cenginfo,nckd_cengspe,nckd_cengunit,nckd_ctechduty,nckd_cprounit,nckd_cincomeway");
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "nckd_professionalser", "nckd_dengbegin desc");
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
}

