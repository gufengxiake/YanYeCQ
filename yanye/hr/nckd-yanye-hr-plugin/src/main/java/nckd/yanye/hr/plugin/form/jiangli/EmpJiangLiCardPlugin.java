package nckd.yanye.hr.plugin.form.jiangli;

import kd.bos.orm.query.QFilter;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.EventObject;
import java.util.List;

/**
 * 核心人力云->人员信息->附表卡片
 * 奖惩记录 nckd_hspm_perrprecor_ext3 动态表单
 * 2024-07-26
 * chengchaohua
 */
public class EmpJiangLiCardPlugin  extends AbstractCardDrawEdit {

    public EmpJiangLiCardPlugin() {

    }
    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        Long personId = HRJSONUtils.getLongValOfCustomParam(preBindDataVo.getFormShowParameter().getCustomParam("person"));
        if (personId != null && personId != 0L) {
            // 头部：奖励名称 nckd_jiangliname ，标题：奖项批准单位名称 nckd_jiangxiangdanwei ，具体业务字段：
            CardViewCompareVo compareVo = new CardViewCompareVo("", "nckd_jiangliname", (String)null, "unit,level,content,rewarddate,witness,comment", (String)null);
            List<String> fields = this.setChildFieldVo(new FieldTransVo(preBindDataVo.getDataMap(), compareVo));
            QFilter[] conFilter = new QFilter[]{new QFilter("person", "=", personId)};
            QueryDbVo queryDbVo = new QueryDbVo(conFilter, fields, "hrpi_perrprecord", "createtime desc");
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
}
