package nckd.yanye.fi.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.report.IReportListModel;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.*;
import kd.bos.form.control.events.ClickListener;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.ReportList;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.fi.gl.accsys.AccSysUtil;
import kd.fi.gl.report.NavToAssRpt;
import java.util.*;
import java.util.stream.Collectors;
import kd.fi.cas.util.EmptyUtil;

/**
 * @author wanghaiwu_kd
 * @date 2023/07/12
 * @description 科目余额表报表插件
 * 功能：1、增加核算维度余额表按钮，无核算维度科目可以根据下级科目的核算维度关联查询
 */
public class AccBalanceFormRptExtPlugin extends AbstractReportFormPlugin implements ClickListener {
    public static final String KEY_BTN_NAVASSISTBAL = "nckd_navassistbal";

    public AccBalanceFormRptExtPlugin() {
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners(new String[]{"toolbarap"});
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        switch (evt.getItemKey()) {
            case KEY_BTN_NAVASSISTBAL:
                ReportList control = (ReportList)this.getControl("reportlistap");
                int[] selectRows = control.getEntryState().getSelectedRows();
                if (selectRows.length == 0) {
                    this.getView().showTipNotification(ResManager.loadKDString("请选中一行联查核算维度余额表。", "AccBalanceFormRpt_2", "fi-gl-formplugin", new Object[0]));
                    return;
                } else {
                    IReportListModel reportModel = control.getReportModel();
                    DynamicObject rowData = reportModel.getRowData(selectRows[0]);
                    DynamicObject account = rowData.getDynamicObject("accountnumber");
                    if (account == null) {
                        this.getView().showTipNotification(ResManager.loadKDString("请选中明细行。", "AccBalanceFormRpt_3", "fi-gl-formplugin", new Object[0]));
                        return;
                    } else {
                        DynamicObject period = (DynamicObject)this.getModel().getValue("endperiod");
                        Date endDate = period.getDate("enddate");
                        QFilter fstartDate = new QFilter("startdate", "<=", endDate);
                        QFilter fendDate = new QFilter("enddate", ">=", endDate);
                        QFilter fmasterId = new QFilter("masterid", "=", account.getLong("masterid"));
                        Long orgID = this.getParentOrg();
                        QFilter fids = BaseDataServiceHelper.getBaseDataFilter("bd_accountview", orgID);
                        DynamicObject[] acctDyo = BusinessDataServiceHelper.load("bd_accountview", "id, checkitementry.asstactitem", new QFilter[]{fids, fstartDate, fendDate, fmasterId});
                        DynamicObjectCollection acctEntry = null;
                        if (acctDyo.length == 0) {
                            DynamicObject acct = BusinessDataServiceHelper.loadSingle(account.get("id"), EntityMetadataCache.getDataEntityType("bd_accountview"));
                            acctEntry = acct.getDynamicObjectCollection("checkitementry");
                        } else {
                            acctEntry = acctDyo[0].getDynamicObjectCollection("checkitementry");
                        }

                        if (acctEntry.isEmpty()) {
                            //所选科目无核算维度，查询下级存在核算维度的科目
                            navToAssRptExt(account);
                        } else {
                            //所选科目有核算维度，走标准的逻辑
                            ReportQueryParam queryParam = this.getQueryParam();
                            DynamicObjectCollection orgs = queryParam.getFilter().getDynamicObjectCollection("orgs");
                            if (orgs.isEmpty()) {
                                return;
                            } else {
                                Long assgrpId = rowData.getLong("assgrp_id");
                                (new NavToAssRpt.Builder(queryParam, this.getView())).navKey("gl_rpt_assistbalance").orgId(((DynamicObject)orgs.get(0)).getLong("id")).accountId(account.getLong("id")).assgrpId(assgrpId).build().apply();
                            }
                        }
                    }
                }
            default:
        }
    }

    public void navToAssRptExt(DynamicObject account){
        String longNumber = account.getString("longnumber");
        Object value = this.getModel().getValue("accounttable_id");
        DynamicObject period = (DynamicObject)this.getModel().getValue("endperiod");
        Date endDate = period.getDate("enddate");
        Long orgID = this.getParentOrg();
        if (this.hasProperty("orgs")) {
            DynamicObjectCollection orgs = (DynamicObjectCollection)this.getModel().getValue("orgs");
            List<Long> orgList = (List)orgs.stream().mapToLong((x) -> {
                return x.getLong("fbasedataid_id");
            }).boxed().collect(Collectors.toList());
            orgID = getParentOrgByChildre(orgList);
        }

        QFilter faccounttable = new QFilter("accounttable", "=", value);
        QFilter fstartDate = new QFilter("startdate", "<=", endDate);
        QFilter fendDate = new QFilter("enddate", ">=", endDate);
        QFilter flongnumber = new QFilter("longnumber", "like",longNumber + "_%");
        QFilter fids = BaseDataServiceHelper.getBaseDataFilter("bd_accountview", orgID);

        //查询选择记录的所有下级科目
        DynamicObject[] acctDyo = BusinessDataServiceHelper.load("bd_accountview", "id, checkitementry.asstactitem",
                new QFilter[]{fids, fstartDate, fendDate, faccounttable, flongnumber});

        String acctIds = "";
        //定义科目集合(当前科目下级有核算维度的科目)
        List<Long> itemIds = new ArrayList<>();
        //定义核算维度
        List<Long> acctIdList = new ArrayList<>();

        for (DynamicObject acct : acctDyo) {
            DynamicObjectCollection entry = acct.getDynamicObjectCollection("checkitementry");
            if (entry.size() > 0) {
                acctIdList.add(acct.getLong("id"));
            }

            for (DynamicObject entryRow : entry) {
                DynamicObject assit = entryRow.getDynamicObject("asstactitem");
                itemIds.add(assit.getLong("id"));
            }
        }
        acctIdList = (List<Long>)acctIdList.stream().distinct().collect(Collectors.toList());
        itemIds = (List<Long>)itemIds.stream().distinct().collect(Collectors.toList());

        for (Long acctId : acctIdList) {
            acctIds += (acctIds.length() > 0 ? "," : "") + acctId;
        }

        if(itemIds.size() > 0) {
            this.getPageCache().put("_acctIds", acctIds);
            QFilter qFilter = new QFilter("id", QCP.in, itemIds);
            this.showF7Form("bd_asstacttype", true, qFilter, null, "asstselect", null);
        } else {
            this.getView().showTipNotification(ResManager.loadKDString("该科目无下级科目或下级科目没有核算维度，不能联查。", "AccBalanceFormRpt_4", "fi-gl-formplugin", new Object[0]));
        }
    }

    public Long getParentOrg() {
        String parentOrg = this.getPageCache().get("_parentOrg");
        return StringUtils.isBlank(parentOrg) ? 0L : Long.valueOf(parentOrg);
    }

    private boolean hasProperty(String name) {
        return this.getModel().getProperty(name) != null;
    }

    public static long getParentOrgByChildre(List<Long> filterOrgs) {
        return AccSysUtil.getParentOrgByChildre(filterOrgs);
    }



    public void closedCallBack(ClosedCallBackEvent evt) {
        super.closedCallBack(evt);

        if ("asstselect".equals(evt.getActionId())) {
            if (EmptyUtil.isNoEmpty(evt.getReturnData())) {
                ReportQueryParam queryParam = this.getQueryParam();
                DynamicObjectCollection orgs = queryParam.getFilter().getDynamicObjectCollection("orgs");
                if (orgs.isEmpty()) {
                    return;
                } else {
                    ListSelectedRowCollection returnDatas = (ListSelectedRowCollection)evt.getReturnData();
                    if (EmptyUtil.isEmpty(returnDatas)) {
                        return;
                    }
                    String asstIds = "";

                    for (ListSelectedRow row : returnDatas) {
                        asstIds += (asstIds.length() > 0 ? "," : "") + row.getPrimaryKeyValue();
                    }

                    String acctIds = this.getPageCache().get("_acctIds");

                    (new NavToAssRptExt.Builder(queryParam, this.getView())).navKey("gl_rpt_assistbalance").orgId(((DynamicObject)orgs.get(0)).getLong("id")).acctIds(acctIds).assgrpIds(asstIds).build().apply();
                }
            } else {
                this.getView().showTipNotification(ResManager.loadKDString("未选择核算维度，不能联查。", "AccBalanceFormRpt_4", "fi-gl-formplugin", new Object[0]));
            }
        }
    }

    protected void showF7Form(String formId, boolean isMultiSelect, QFilter filter, Map param, String callBackKey, Object[] selectRows) {
        ListShowParameter parameter = ShowFormHelper.createShowListForm(formId, isMultiSelect, 0, true);
        if (EmptyUtil.isNoEmpty(param)) {
            parameter.getCustomParams().putAll(param);
        }

        if (EmptyUtil.isNoEmpty(callBackKey)) {
            CloseCallBack callBack = new CloseCallBack(this, callBackKey);
            parameter.setCloseCallBack(callBack);
        }

        if (EmptyUtil.isNoEmpty(filter)) {
            parameter.getListFilterParameter().setFilter(filter);
        }

//        parameter.setSelectedRows(selectRows);
        this.getView().showForm(parameter);
    }

    public static ListShowParameter createShowListForm(String formId, boolean isMultiSelect, int f7Style, boolean isLookUP) {
        ListShowParameter para = new ListShowParameter();
        para.setLookUp(isLookUP);
        para.setBillFormId(formId);
        if (isLookUP) {
            para.getOpenStyle().setShowType(ShowType.Modal);
            StyleCss css = new StyleCss();
            css.setWidth("960px");
            css.setHeight("580px");
            para.getOpenStyle().setInlineStyleCss(css);
            para.setFormId(getListFormId(formId, f7Style));
            para.setF7Style(f7Style);
        } else {
            para.getOpenStyle().setShowType(ShowType.MainNewTabPage);
            FormConfig formConfig = FormMetadataCache.getListFormConfig(formId);
            para.setFormId(formConfig.getListFormId());
        }

        para.setMultiSelect(isMultiSelect);
        para.setShowTitle(false);
        para.setHasRight(true);
        return para;
    }

    private static String getListFormId(String formId, int f7Style) {
        FormConfig formConfig = FormMetadataCache.getListFormConfig(formId);
        switch (f7Style) {
            case 1:
                if ("bos_user".equals(formId)) {
                    return formConfig.getF7ListFormId();
                }

                return "bos_orgtreelistf7";
            case 2:
                return "bos_listf7";
            default:
                return formConfig == null ? formId : formConfig.getF7ListFormId();
        }
    }
}
