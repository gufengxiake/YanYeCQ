package nckd.yanye.hr.plugin.form.renzhi;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Label;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.hr.hbp.common.util.HRStringUtils;
import kd.hr.hpfs.business.utils.HpfsPersonInfoParamUtil;
import kd.sdk.hr.hspm.business.repository.ErmanFileRepository;
import kd.sdk.hr.hspm.business.service.AttacheHandlerService;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.utils.HspmDateUtils;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 上线后任职经历
 * 菜单：人员档案
 * 开发平台：核心人力云-》人员信息-》附表卡片-》任职经历(上线后任职经历) nckd_hspm_empposorgre_ext，源页面:hspm_empposorgrel_dv
 * author: chengchaohua
 * date:2024-08-06
 */
public class EmpRenZhiHouCardPlugin extends AbstractCardDrawEdit {

    private static final Log logger = LogFactory.getLog(EmpRenZhiHouCardPlugin.class);


    public EmpRenZhiHouCardPlugin() {

    }

    @Override
    protected PreBindDataVo prefixHandlerBeforeBindData(EventObject args) {
        PreBindDataVo preBindDataVo = super.prefixHandlerBeforeBindData(args);
        FormShowParameter formShowParameter = preBindDataVo.getFormShowParameter();
        boolean main = AttacheHandlerService.getInstance().judgeIsMain(formShowParameter);
        if (main) {
            this.getView().setVisible(Boolean.FALSE, new String[]{"flexpanelap", "topflexpanelap", "flexpanelap2"});
        }

        String filterStr = this.getView().getPageCache().get("recordfilter");
        QFilter qFilter = null;
        if (HRStringUtils.isNotEmpty(filterStr)) {
            qFilter = new QFilter("islatestrecord", "=", filterStr);
        }

        this.handlerBeforeBindData(preBindDataVo.getFormShowParameter(), qFilter, preBindDataVo.getDataMap(), args);
        return preBindDataVo;
    }

    private void handlerBeforeBindData(FormShowParameter formShowParameter, QFilter qFilter, Map<String, Object> tabMap, EventObject args) {
        QueryDbVo queryDbVo = new QueryDbVo();
        queryDbVo.setEntityId("hrpi_empposorgrel");
        queryDbVo.setOrderBy("startdate desc,sysenddate desc,createtime desc");
        List<String> fields = this.assessAbleFields(tabMap, args, queryDbVo);
        Long employeeId = HRJSONUtils.getLongValOfCustomParam(formShowParameter.getCustomParam("employee"));
        if (employeeId != null && employeeId != 0L) {
            QFilter idFilter = new QFilter("employee", "=", employeeId);
            List<QFilter> list = new ArrayList();
            list.add(idFilter);
            if (qFilter != null) {
                list.add(qFilter);
            }

            fields.add("iscurrentversion");
            if (!fields.contains("islatestrecord")) {
                fields.add("islatestrecord");
            }

            Long erFileId = HRJSONUtils.getLongValOfCustomParam(formShowParameter.getCustomParam("erfileid"));
            if (erFileId != null && erFileId != 0L) {
                DynamicObject ermanfile = ErmanFileRepository.getErmanfile(erFileId);
                Date startDate = ermanfile.getDate("startdate");
                Date endDate = ermanfile.getDate("enddate");
                if (endDate == null) {
                    endDate = HspmDateUtils.getMaxEndDate();
                }

                Long orgId = ermanfile.getLong("org.id");
                logger.info("org===={}", orgId);
                Map<String, Object> paramMap = HpfsPersonInfoParamUtil.getEmpExpType(orgId);
                logger.info("empExpType===={},isincludebefore===={}", paramMap.get("empexptype"), paramMap.get("isincludebefore"));
                this.queryDataByConfig(list, startDate, endDate, paramMap);
                this.appendDataForMoreEntry(formShowParameter, idFilter, list, paramMap);
                queryDbVo.setFilters((QFilter[])list.toArray(new QFilter[list.size()]));
                queryDbVo.setFields(fields);
                this.queryAndAssDataFromDb(queryDbVo);
                list.remove(qFilter);
                this.setTotalAndNew(fields, (QFilter[])list.toArray(new QFilter[list.size()]));
                this.defineSpecial(new DefineSpecialVo(true, "shamedit_", (String)null, (String)null, (String)null));
            }
        }
    }

    /**
     * 卡片界面显示字段
     * @param tabMap
     * @param args
     * @param queryDbVo
     * @return
     */
    private List<String> assessAbleFields(Map<String, Object> tabMap, EventObject args, QueryDbVo queryDbVo) {
        // startdate 开始日期
        // company 公司，position 岗位
        // islatestrecord 是否用工终止前最新记录,postype 任职类型
        // cmpemp 所属管理范围，variationtype 变动类型，nckd_ganbutype 干部类型， nckd_zhiji 职级
        CardViewCompareVo compareVo = new CardViewCompareVo("startdate,enddate,", "company,position,adminorg,stdposition", "islatestrecord,postype", "cmpemp,variationtype,variationreason,job,isexistprobation,startprobation,endprobation,nckd_ganbutype,nckd_zhiji,description", (String)null);
        this.childPointModify(new CardBindDataDTO(this.getModel(), this.getView(), args, compareVo, this.getTimeMap(), queryDbVo));
        return this.setChildFieldVo(new FieldTransVo(tabMap, compareVo));
    }

    private void queryDataByConfig(List<QFilter> list, Date startDate, Date endDate, Map<String, Object> paramMap) {
        HpfsPersonInfoParamUtil.EmpExpTypeEnum empexptype = (HpfsPersonInfoParamUtil.EmpExpTypeEnum)paramMap.get("empexptype");
        QFilter dateFilter;
        if (empexptype == HpfsPersonInfoParamUtil.EmpExpTypeEnum.ONLY_DEPT) {
            dateFilter = (new QFilter("enddate", ">=", startDate)).and(new QFilter("startdate", "<=", endDate));
            list.add(dateFilter);
        } else if (empexptype == HpfsPersonInfoParamUtil.EmpExpTypeEnum.ONLY_AND_AFTER_DEPT) {
            dateFilter = new QFilter("enddate", ">=", startDate);
            list.add(dateFilter);
        } else if (empexptype == HpfsPersonInfoParamUtil.EmpExpTypeEnum.ONLY_AND_BEFORE_DEPT) {
            dateFilter = new QFilter("startdate", "<=", endDate);
            list.add(dateFilter);
        }

    }

    private void appendDataForMoreEntry(FormShowParameter formShowParameter, QFilter idFilter, List<QFilter> list, Map<String, Object> paramMap) {
        if (paramMap.get("isincludebefore") != null && Boolean.TRUE.equals(paramMap.get("isincludebefore"))) {
            Long personId = HRJSONUtils.getLongValOfCustomParam(formShowParameter.getCustomParam("person"));
            if (personId != null && personId != 0L) {
                HRBaseServiceHelper helper = new HRBaseServiceHelper("hrpi_person");
                DynamicObject[] objects = helper.query("personindexid", new QFilter[]{new QFilter("id", "=", personId)});
                if (objects != null && objects.length > 0) {
                    long pid = objects[0].getLong("personindexid");
                    QFilter curFilter = new QFilter("iscurrentversion", "=", "1");
                    QFilter initstatusFilter = new QFilter("initstatus", "=", "2");
                    QFilter dataFilter = new QFilter("datastatus", "=", "1");
                    DynamicObject[] allPerson = helper.query("id", new QFilter[]{curFilter, initstatusFilter, dataFilter, new QFilter("personindexid", "=", pid)});
                    Set<Long> collect = (Set)Arrays.stream(allPerson).map((dynamicObject) -> {
                        return dynamicObject.getLong("id");
                    }).collect(Collectors.toSet());
                    QFilter personFilter = new QFilter("person", "in", collect);
                    list.remove(idFilter);
                    list.add(personFilter);
                }
            }
        }

    }

    private void setTotalAndNew(List<String> fields, QFilter[] conFilter) {
        List<Map<String, Object>> dataList = this.getDataList();
        String totalTimeStr = "0";
        String reordStr = "0";
        if (dataList != null && dataList.size() > 0) {
            totalTimeStr = dataList.size() + "";
            List<Object> collect = (List)dataList.stream().filter((map) -> {
                return (Boolean)map.get("islatestrecord");
            }).collect(Collectors.toList());
            if (collect.size() > 0) {
                reordStr = collect.size() + "";
            } else {
                this.getView().setVisible(false, new String[]{"filterdatapanelap"});
            }
        }

        String filterStr = this.getView().getPageCache().get("recordfilter");
        if (HRStringUtils.isNotEmpty(filterStr)) {
            List<Map<String, Object>> list = this.queryAndAssDataFromDb(new QueryDbVo(conFilter, fields, "hrpi_empposorgrel", Boolean.FALSE));
            if (list.size() > 0) {
                totalTimeStr = list.size() + "";
            }
        }

        Label empposnewrecord;
        if (this.getControl("totaltime") instanceof Label) {
            empposnewrecord = (Label)this.getControl("totaltime");
            empposnewrecord.setText(totalTimeStr);
        } else {
            this.getModel().setValue("totaltime", totalTimeStr);
        }

        if (this.getControl("empposnewrecord") instanceof Label) {
            empposnewrecord = (Label)this.getControl("empposnewrecord");
            empposnewrecord.setText(reordStr);
        } else {
            this.getModel().setValue("empposnewrecord", reordStr);
        }

    }

    /**
     * 点击卡片进入查看
     * @param dsVo
     * @return
     */
    protected Map<String, Object> defineSpecial(DefineSpecialVo dsVo) {
        Map<String, Object> defineMap = super.defineSpecial(dsVo);
        defineMap.put("viewshowdialog", "1");
        return defineMap;
    }

}
