package nckd.yanye.hr.plugin.form.renzhi;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.Tips;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Label;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.FieldEdit;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.metadata.form.Style;
import kd.bos.metadata.form.control.LabelAp;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.hr.hbp.common.util.HRJSONUtils;
import kd.hr.hbp.common.util.HRStringUtils;
import kd.hr.hpfs.business.utils.HpfsPersonInfoParamUtil;
import kd.sdk.hr.hspm.business.repository.ErmanFileRepository;
import kd.sdk.hr.hspm.business.service.AttacheHandlerService;
import kd.sdk.hr.hspm.common.ext.file.CardBindDataDTO;
import kd.sdk.hr.hspm.common.utils.CommonUtil;
import kd.sdk.hr.hspm.common.utils.HspmDateUtils;
import kd.sdk.hr.hspm.common.vo.*;
import kd.sdk.hr.hspm.formplugin.web.file.ermanfile.base.AbstractCardDrawEdit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 上线后任职经历
 * 菜单：人员档案
 * 开发平台：核心人力云-》人员信息-》附表卡片-》任职经历(上线后任职经历) nckd_hspm_empposorgre_ext，源页面:hspm_empposorgrel_dv
 * 增加卡片显示字段
 * author: chengchaohua
 * date:2024-08-06
 */
public class EmpRenZhiHouCardPlugin extends AbstractCardDrawEdit {

    private static final Log logger = LogFactory.getLog(EmpRenZhiHouCardPlugin.class);
    private static final String TIME_FIELDS = "startdate,enddate,";
    private static final String HEAD_FIELDS = "company,position,adminorg,stdposition";
    private static final String TEXT_FIELDS = "islatestrecord,postype";
    private static final String CONTENT_FIELDS = "cmpemp,variationtype,variationreason,job,isexistprobation,startprobation,endprobation,description";


    public EmpRenZhiHouCardPlugin() {

    }

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

    public void propertyChanged(PropertyChangedArgs changedArgs) {
        String key = changedArgs.getProperty().getName();
        if ("empnewrecord".equals(key)) {
            Boolean record = (Boolean)this.getModel().getValue("empnewrecord");
            if (record) {
                this.getView().getPageCache().put("recordfilter", "1");
            } else {
                this.getView().getPageCache().put("recordfilter", (String)null);
            }

            this.getView().updateView();
        }

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

    protected Map<String, Object> defineSpecial(DefineSpecialVo dsVo) {
        Map<String, Object> defineMap = super.defineSpecial(dsVo);
        defineMap.put("viewshowdialog", "1");
        return defineMap;
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

    protected boolean createLabel(BeforeCreatVo beforeCreatVo) {
        boolean label = super.createLabel(beforeCreatVo);
        if (!label) {
            return label;
        } else {
            String labType = beforeCreatVo.getLabType();
            boolean flag = true;
            if ("text".equals(labType)) {
                flag = this.handlerSpecialWordShow(beforeCreatVo);
            }

            return flag;
        }
    }

    private boolean handlerSpecialWordShow(BeforeCreatVo beforeCreatVo) {
        Map<String, Object> dataMap = beforeCreatVo.getDataMap();
        Map<String, String> relMap = beforeCreatVo.getRelMap();
        List<String> wordList = this.specialWord("0");
        String field = (String)relMap.get("number");
        if (!wordList.contains(field)) {
            return true;
        } else {
            Object recordObj = dataMap.get(field);
            if (recordObj == null) {
                return false;
            } else {
                return !"0".equals(recordObj);
            }
        }
    }

    protected boolean customChangeLabelValue(BeforeCreatVo beforeCreatVo) {
        super.customChangeLabelValue(beforeCreatVo);
        Map<String, Object> dataMap = beforeCreatVo.getDataMap();
        Map<String, Object> labMap = beforeCreatVo.getLabMap();
        Map<String, String> relMap = beforeCreatVo.getRelMap();
        String labType = beforeCreatVo.getLabType();
        String field = (String)labMap.get("number");
        if ("text".equals(labType)) {
            List<String> wordList = this.specialWord("0");
            if (!wordList.contains(field)) {
                return false;
            }

            Object obj = dataMap.get(field);
            if (CommonUtil.objIsEmpty(obj)) {
                return false;
            }

            if (obj instanceof Boolean && "islatestrecord".equals(field)) {
                if (!(Boolean)obj) {
                    return true;
                }

                relMap.put(field, ResManager.loadKDString("最新记录", "EmpPosoRgRelCardPlugin_0", "hr-hspm-formplugin", new Object[0]));
            }
        } else if ("content".equals(labType)) {
            Object obj = dataMap.get(field);
            if (CommonUtil.objIsEmpty(obj)) {
                return false;
            }

            if (obj instanceof Boolean && "isexistprobation".equals(field)) {
                return AttacheHandlerService.getInstance().transferBoolType(beforeCreatVo);
            }
        }

        return false;
    }

    protected void customChangeLabelStyle(AfterCreatVo afterCreatVo) {
        String labType = afterCreatVo.getLabType();
        Map<String, Object> filedMap = afterCreatVo.getFiledMap();
        String field = (String)filedMap.get("number");
        if ("text".equals(labType)) {
            List<String> wordList = this.specialWord("1");
            if (!wordList.contains(field)) {
                return;
            }

            afterCreatVo.setField(field);
            this.handlerSpecialWordStyle(afterCreatVo);
        } else if ("head".equals(labType) && ("stdposition".equals(field) || "position".equals(field))) {
            this.setTips(afterCreatVo, field);
        }

    }

    private void setTips(AfterCreatVo afterCreatVo, String position) {
        LabelAp fieldAp = afterCreatVo.getFieldAp();
        Map<String, Object> dataMap = afterCreatVo.getDataMap();
        Object postObj = dataMap.get(position);
        if (postObj instanceof DynamicObject) {
            DynamicObject postDy = (DynamicObject)postObj;
            Tips ctlTips = new Tips();
            ctlTips.setShowIcon(false);
            ctlTips.setContent(new LocaleString(postDy.getString("number")));
            ctlTips.setType("text");
            fieldAp.setCtlTips(ctlTips);
        }

    }

    private void handlerSpecialWordStyle(AfterCreatVo afterCreatVo) {
        Map<String, Object> dataMap = afterCreatVo.getDataMap();
        String field = afterCreatVo.getField();
        Style style = afterCreatVo.getStyle();
        LabelAp fieldAp = afterCreatVo.getFieldAp();
        Object data = dataMap.get(field);
        if (!CommonUtil.objIsEmpty(data)) {
            Map<String, String> posTypeColorMap = new HashMap(16);
            if ("postype".equals(field)) {
                HRBaseServiceHelper serviceHelper = new HRBaseServiceHelper("hbss_postype");
                Long pkId = 0L;
                if (data instanceof DynamicObject) {
                    pkId = ((DynamicObject)data).getLong("id");
                }

                if (pkId != 0L) {
                    DynamicObject dynamicObject = serviceHelper.queryOne("postcategory.number", pkId);
                    if (dynamicObject == null) {
                        return;
                    }

                    String pst = dynamicObject.getString("postcategory.number");
                    posTypeColorMap = this.getPosTypeColorMap(pst);
                }
            } else if ("islatestrecord".equals(field)) {
                Boolean record = (Boolean)dataMap.get(field);
                posTypeColorMap = this.getRecordColorMap(record);
            }

            this.setLabelColorStyle(new TextColorVo(style, fieldAp, (String)((Map)posTypeColorMap).get("forColor"), (String)((Map)posTypeColorMap).get("backColor"), "100px"));
        }
    }

    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        String key = closedCallBackEvent.getActionId();
        String entityId = this.getView().getEntityId();
        if (key.contains(entityId)) {
            Object returnData = closedCallBackEvent.getReturnData();
            if (returnData == null) {
                return;
            }

            this.getView().updateView();
        }

    }

    private List<String> specialWord(String type) {
        List<String> list = new ArrayList();
        if ("1".equals(type)) {
            list.add("postype");
        }

        list.add("islatestrecord");
        return list;
    }

    private Map<String, String> getPosTypeColorMap(String posTypeId) {
        Map<String, Map<String, String>> colorMap = new HashMap(16);
        Map<String, String> posTypeMap = new HashMap(16);
        posTypeMap.put("forColor", "#1BA854");
        posTypeMap.put("backColor", "rgba(242,255,245,0.1)");
        colorMap.put("1010_S", posTypeMap);
        posTypeMap = new HashMap(16);
        posTypeMap.put("forColor", "#276FF5");
        posTypeMap.put("backColor", "rgba(133,184,255,0.1)");
        colorMap.put("1020_S", posTypeMap);
        posTypeMap = new HashMap(16);
        posTypeMap.put("forColor", "#ffb44a");
        posTypeMap.put("backColor", "rgba(255,180,74,0.1)");
        colorMap.put("1040_S", posTypeMap);
        Map<String, String> rePosTypeMap = (Map)colorMap.get(posTypeId);
        return (Map)(rePosTypeMap == null ? new HashMap(16) : rePosTypeMap);
    }

    private Map<String, String> getRecordColorMap(Boolean record) {
        Map<Boolean, Map<String, String>> recordMap = new HashMap(16);
        Map<String, String> posTypeMap = new HashMap(16);
        posTypeMap.put("forColor", "#FB2323");
        posTypeMap.put("backColor", "rgba(255,242,244,0.1)");
        recordMap.put(Boolean.TRUE, posTypeMap);
        Map<String, String> rePosTypeMap = (Map)recordMap.get(record);
        return (Map)(rePosTypeMap == null ? new HashMap(16) : rePosTypeMap);
    }

    public void afterBindData(EventObject eventObject) {
        super.afterBindData(eventObject);
        FieldEdit fieldEdit = (FieldEdit)this.getControl("empnewrecord");
        fieldEdit.setCaption(new LocaleString(ResManager.loadKDString("仅显示最新记录", "EmpPosoRgRelCardPlugin_1", "hr-hspm-formplugin", new Object[0])));
    }

    protected String getOpenPageName() {
        return "hspm_revempposorgrel";
    }

    protected String getEntityName() {
        return "hrpi_empposorgrel";
    }
}