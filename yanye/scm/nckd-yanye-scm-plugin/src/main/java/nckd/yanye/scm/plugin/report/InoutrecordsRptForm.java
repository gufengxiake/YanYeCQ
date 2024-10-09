package nckd.yanye.scm.plugin.report;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.dataentity.metadata.dynamicobject.DynamicProperty;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.EntityTypeUtil;
import kd.bos.entity.GetFilterFieldsParameter;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.filter.FilterCondition;
import kd.bos.entity.property.GroupProp;
import kd.bos.entity.property.ParentBasedataProp;
import kd.bos.entity.property.UnitProp;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.IFormView;
import kd.bos.form.control.FilterGrid;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.DispatchServiceHelper;
import kd.bos.servicehelper.MetadataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bplat.scmc.report.conf.ReportConf;
import kd.bplat.scmc.report.core.ReportDataHandle;
import kd.bplat.scmc.report.util.ReportUtil;
import kd.fi.cal.common.helper.OrgHelper;
import kd.fi.cal.common.helper.PeriodHelper;
import kd.fi.cal.common.helper.PermissionHelper;
import kd.fi.cal.common.helper.ReportF7Helper;
import kd.fi.cal.common.util.DateUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author husheng
 * @date 2024-09-03 14:54
 * @description 出入库流水账（nckd_inoutrecords）报表界面插件
 */
public class InoutrecordsRptForm extends AbstractReportFormPlugin implements BeforeF7SelectListener {
    private ReportConf confCache;

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addF7Listener(this, "nckd_mulcalorg", "nckd_mulcostaccount", "nckd_mulmaterialgroup", "nckd_mulmaterial", "nckd_materialto", "nckd_mulbilltype");
    }

    private void addF7Listener(BeforeF7SelectListener form, String... f7Names) {
        BasedataEdit f7 = null;
        String[] var4 = f7Names;
        int var5 = f7Names.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            String f7Name = var4[var6];
            f7 = this.getControl(f7Name);
            if (f7 != null) {
                f7.addBeforeF7SelectListener(form);
            }
        }

    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

        Long userId = RequestContext.get().getCurrUserId();
        Long userOrgId = RequestContext.get().getOrgId();
        Long calOrgId = OrgHelper.getCalOrgByUserOrg(userId, userOrgId, "nckd_inoutrecords");
        if (calOrgId != null && calOrgId != 0L) {
            Long[] ids = new Long[]{calOrgId};
            this.getModel().setValue("nckd_mulcalorg", ids);
            DynamicObject costAccount = OrgHelper.getCostAccountByCalOrg(calOrgId);
            if (costAccount != null) {
                this.getModel().setValue("nckd_mulcostaccount", new Long[]{costAccount.getLong("id")});
                this.costAccountChanged();
            }
        }

        this.initFilterAp();
    }

    private void initFilterAp() {
        ReportConf conf = this.getReportConf();
        final Map<String, String> renames = new HashMap(32);
        final Set<String> fsCol = ReportUtil.filterBigtableCols(conf, (colConf) -> {
            String calType = colConf.getCalType();
            boolean result = !"D".equals(calType) && !"B".equals(calType);
            if (result) {
                renames.put(colConf.getCol(), colConf.getColName());
            }

            return result;
        });
        String repoEntity = conf.getRepoEntity();
        MainEntityType type = MetadataServiceHelper.getDataEntityType(repoEntity);
        Map<String, IDataEntityProperty> allFields = type.getAllFields();
        final Set<String> unitCols = new HashSet(8);
        IDataEntityProperty pro = null;
        Iterator var9 = allFields.entrySet().iterator();

        while(var9.hasNext()) {
            Map.Entry<String, IDataEntityProperty> field = (Map.Entry)var9.next();
            pro = field.getValue();
            if (pro instanceof UnitProp) {
                unitCols.add(field.getKey());
            }
        }

        fsCol.removeAll(unitCols);
        this.setCols4FilterGrid(conf.getRepoEntity(), "nckd_commonfs", new Predicate<Map<String, Object>>() {
            public boolean test(Map<String, Object> info) {
                boolean result = false;
                String fieldName = (String)info.get("fieldName");
                if (fieldName != null) {
                    String[] splitName = fieldName.split("\\.");
                    if (fsCol.contains(splitName[0])) {
                        result = true;
                    } else if (unitCols.contains(splitName[0])) {
                        result = splitName.length > 1 && ("name".equals(splitName[1]) || "number".equals(splitName[1]));
                    }

                    String rename = renames.get(splitName[0]);
                    if (result && rename != null) {
                        String fieldCaption = (String)info.get("fieldCaption");
                        if (fieldCaption != null) {
                            String[] splitCaption = fieldCaption.split("\\.");
                            splitCaption[0] = rename;
                            info.put("fieldCaption", String.join(".", splitCaption));
                        }
                    }
                }

                return result;
            }
        });
    }

    protected final ReportConf getReportConf() {
        if (this.confCache == null) {
            this.confCache = ReportDataHandle.loadReportConf(this.getReoprtEntity());
        }

        return this.confCache;
    }

    protected String getReoprtEntity() {
        return this.getModel().getDataEntityType().getName();
    }

    private void setCols4FilterGrid(String entityName, String grid, Predicate<Map<String, Object>> predicate) {
        FilterGrid filterGrid = this.getView().getControl(grid);
        filterGrid.SetValue(new FilterCondition());
        if (StringUtils.isBlank(entityName)) {
            filterGrid.setFilterColumns(new ArrayList());
        } else {
            MainEntityType entityType = MetadataServiceHelper.getDataEntityType(entityName);
            GetFilterFieldsParameter filterFieldsParameter = new GetFilterFieldsParameter(entityType);
            filterFieldsParameter.setNeedAliasEmptyFieldProp(true);
            filterFieldsParameter.setNeedFieldCompareType(false);
            List<Map<String, Object>> cols = EntityTypeUtil.createFilterColumns(filterFieldsParameter).stream().filter(predicate).collect(Collectors.toList());
            filterGrid.setEntityNumber(entityType.getName());
            filterGrid.setFilterColumns(cols);
        }

        this.getView().updateView(grid);
    }

    @Override
    public boolean verifyQuery(ReportQueryParam queryParam) {
        IDataModel model = this.getModel();
        DynamicObject dataEntity = this.getModel().getDataEntity(true);
        Date startdate = (Date) model.getValue("nckd_startdate");
        Date enddate = (Date) model.getValue("nckd_enddate");
        DynamicObjectCollection calOrgs = dataEntity.getDynamicObjectCollection("nckd_mulcalorg");
        DynamicObjectCollection costaccounts = dataEntity.getDynamicObjectCollection("nckd_mulcostaccount");
        if (calOrgs != null && calOrgs.size() != 0 && costaccounts != null && costaccounts.size() != 0 && startdate != null && enddate != null) {
            Set<Date> beginDateSet = new HashSet(costaccounts.size());
            Set<Long> costaccountIdSet = new HashSet(costaccounts.size());
            Iterator var10 = costaccounts.iterator();

            while (var10.hasNext()) {
                DynamicObject costaccount = (DynamicObject) var10.next();
                long costAccountId = costaccount.getDynamicObject("fbasedataid").getLong("id");
                costaccountIdSet.add(costAccountId);
            }

            QFilter filter = new QFilter("id", "in", costaccountIdSet);
            DynamicObjectCollection calAcctDyc = QueryServiceHelper.query("cal_bd_costaccount", "id,calpolicy,calpolicy.periodtype", new QFilter[]{filter});
            Iterator var23 = calAcctDyc.iterator();

            while (var23.hasNext()) {
                DynamicObject costAcct = (DynamicObject) var23.next();
                DynamicObject periodDyc = this.getYearPeriodByDate(costAcct.getLong("id"), startdate);
                if (periodDyc == null) {
                    this.getView().showTipNotification(ResManager.loadKDString("开始日期对应的期间不存在。", "StockGatherDetailRptQueryPlugin_10", "fi-cal-report", new Object[0]));
                    return false;
                }

                getBeginPeriod(costAcct, beginDateSet);
            }

            Set<Object> calpolicyPeriodtype = new HashSet(16);
            DynamicObjectCollection costAccountInfoS = QueryServiceHelper.query("cal_bd_costaccount", "calpolicy.periodtype,calpolicy.currency,calpolicy.currency.amtprecision", new QFilter[]{new QFilter("id", "in", costaccountIdSet)});
            Map<Integer, Long> currencyAmtprecisionMap = new HashMap(16);
            Set<Integer> amtprecisionSet = new HashSet(16);
            Iterator var17 = costAccountInfoS.iterator();

            while (var17.hasNext()) {
                DynamicObject calpolicy = (DynamicObject) var17.next();
                calpolicyPeriodtype.add(calpolicy.get("calpolicy.periodtype"));
                Long currencyId = calpolicy.getLong("calpolicy.currency");
                int amtprecision = calpolicy.getInt("calpolicy.currency.amtprecision");
                amtprecisionSet.add(amtprecision);
                currencyAmtprecisionMap.put(amtprecision, currencyId);
            }

            if (calpolicyPeriodtype.size() > 1) {
                this.getView().showTipNotification(ResManager.loadKDString("所选的期间类型不一致，不能同时选择。", "StockGatherRptFormPlugin_9", "fi-cal-report", new Object[0]));
                return false;
            } else {
                Date min = Collections.min(beginDateSet);
                if (startdate.compareTo(min) < 0) {
                    this.getView().showTipNotification(ResManager.loadKDString("开始日期对应的期间必须在账簿的启用期间之后。", "StockCostDetailRptFormPlugin_4", "fi-cal-report", new Object[0]));
                    return false;
                } else if (enddate.before(startdate)) {
                    this.getView().showTipNotification(ResManager.loadKDString("结束日期必须大于等于开始日期。", "TransactionDetailRptFormPlugin_3", "fi-cal-report", new Object[0]));
                    return false;
                } else {
                    LocalDateTime startDateTime = startdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    LocalDateTime endDateTime = enddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    if (startDateTime.plusYears(1L).isBefore(endDateTime)) {
                        this.getView().showTipNotification(ResManager.loadKDString("结束日期与开始日期间隔超过一年，请修改后再查询。", "TransactionDetailRptFormPlugin_4", "fi-cal-report", new Object[0]));
                        return false;
                    } else {
                        this.getModel().setValue("nckd_startdate", DateUtils.getDayStartTime(startdate));
                        this.getModel().setValue("nckd_enddate", DateUtils.getDayEndTime(enddate));
                        return super.verifyQuery(queryParam);
                    }
                }
            }
        } else {
            this.getView().showTipNotification(ResManager.loadKDString("请检查必录项", "StockGatherRptFormPlugin_0", "fi-cal-report", new Object[0]));
            return false;
        }
    }

    private DynamicObject getYearPeriodByDate(Object costAccountId, Date date) {
        DynamicObject periodDyc = null;
        QFilter filter = new QFilter("id", "=", costAccountId);
        DynamicObject calAcctDyc = QueryServiceHelper.queryOne("cal_bd_costaccount", "calpolicy,calpolicy.periodtype", new QFilter[]{filter});
        if (calAcctDyc != null) {
            QFilter beginDate = new QFilter("begindate", "<=", date);
            QFilter endDatef = new QFilter("enddate", ">=", date);
            QFilter periodTypef = new QFilter("periodtype", "=", calAcctDyc.getLong("calpolicy.periodtype"));
            QFilter notAdjPeriodf = new QFilter("isadjustperiod", "=", Boolean.FALSE);
            periodDyc = BusinessDataServiceHelper.loadSingle("bd_period", "periodyear,periodnumber,begindate,enddate", new QFilter[]{beginDate, endDatef, periodTypef, notAdjPeriodf});
        }

        return periodDyc;
    }

    private static void getBeginPeriod(DynamicObject costAccount, Set<Date> beginDateSet) {
        if (costAccount != null) {
            DynamicObject startPeriod = PeriodHelper.getSysCtrlEntity(costAccount.getLong("id"));
            if (startPeriod != null) {
                Long startPeriodId = startPeriod.getLong("startperiod.id");
                DynamicObject period = BusinessDataServiceHelper.loadSingle("bd_period", "periodyear,periodnumber,begindate,enddate", (new QFilter("id", "=", startPeriodId)).toArray());
                if (period != null) {
                    Date begindate = period.getDate("begindate");
                    beginDateSet.add(begindate);
                }
            }

        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);

        String key = e.getProperty().getName();
        if ("nckd_mulcalorg".equals(key)) {
            this.calOrgChanged();
        } else if ("nckd_mulcostaccount".equals(key)) {
            this.costAccountChanged();
        } else if ("nckd_mulmaterial".equals(key)) {
            this.mulMaterialChanged(this.getModel(), this.getView());
        } else if ("nckd_mulmaterialgroup".equals(key)) {
            this.materialGroupChanged(this.getModel());
        }
    }

    private void materialGroupChanged(IDataModel model) {
        model.setValue("nckd_mulmaterial", null);
        model.setValue("nckd_materialto", null);
    }

    private void mulMaterialChanged(IDataModel model, IFormView view) {
        DynamicObjectCollection coll = (DynamicObjectCollection) model.getValue("nckd_mulmaterial");
        if (coll != null) {
            if (coll.size() == 1) {
                model.setValue("nckd_materialto", (coll.get(0)).getDynamicObject("fbasedataid").getPkValue());
                view.setEnable(Boolean.TRUE, new String[]{"nckd_materialto"});
            } else if (coll.size() == 0) {
                model.setValue("nckd_materialto", null);
                view.setEnable(Boolean.TRUE, new String[]{"nckd_materialto"});
            } else {
                model.setValue("nckd_materialto", null);
                view.setEnable(Boolean.FALSE, new String[]{"nckd_materialto"});
            }
        } else {
            model.setValue("nckd_materialto", null);
            view.setEnable(Boolean.TRUE, new String[]{"nckd_materialto"});
        }

    }

    private void costAccountChanged() {
        DynamicObjectCollection costaccounts = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_mulcostaccount");
        if (costaccounts != null && costaccounts.size() != 0) {
            int size = costaccounts.size();
            Set<Long> costaccountIdSet = new HashSet(size);
            Iterator var4 = costaccounts.iterator();

            while (var4.hasNext()) {
                DynamicObject costAccount = (DynamicObject) var4.next();
                costaccountIdSet.add(costAccount.getDynamicObject("fbasedataid").getLong("id"));
            }

            Map<Long, DynamicObject> periods = PeriodHelper.getCurrentPeriods(costaccountIdSet);
            if (periods.isEmpty()) {
                this.getModel().setValue("nckd_startdate", null);
                this.getModel().setValue("nckd_enddate", null);
            } else {
                Set<Date> beginDateSet = new HashSet(periods.size());
                Set<Date> endDateSet = new HashSet(periods.size());
                Iterator var7 = costaccountIdSet.iterator();

                while (var7.hasNext()) {
                    Long costAccountId = (Long) var7.next();
                    DynamicObject period = periods.get(costAccountId);
                    if (period != null) {
                        Date beginDate = period.getDate("begindate");
                        Date endDate = period.getDate("enddate");
                        beginDateSet.add(beginDate);
                        endDateSet.add(endDate);
                    }
                }

                if (!beginDateSet.isEmpty() && !endDateSet.isEmpty()) {
                    Date max = Collections.max(endDateSet);
                    Date min = Collections.min(beginDateSet);
                    this.getModel().setValue("nckd_startdate", min);
                    this.getModel().setValue("nckd_enddate", max);
                } else {
                    this.getModel().setValue("nckd_startdate", null);
                    this.getModel().setValue("nckd_enddate", null);
                }
            }
        } else {
            this.getModel().setValue("nckd_startdate", null);
            this.getModel().setValue("nckd_enddate", null);
        }
    }

    private void calOrgChanged() {
        IDataModel model = this.getModel();
        DynamicObjectCollection calOrgList = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_mulcalorg");
        if (calOrgList != null && calOrgList.size() != 0) {
            Set<Long> calOrgIds = new HashSet();
            Iterator var4 = calOrgList.iterator();

            while (var4.hasNext()) {
                DynamicObject calOrg = (DynamicObject) var4.next();
                calOrgIds.add(calOrg.getDynamicObject("fbasedataid").getLong("id"));
            }

            Set<Long> costAccountIds = OrgHelper.getCostAccountIdsByCalOrg(calOrgIds);
            if (costAccountIds != null && costAccountIds.size() > 0) {
                this.getModel().setValue("nckd_mulcostaccount", costAccountIds.toArray());
            } else {
                this.getModel().setValue("nckd_mulcostaccount", null);
            }
        } else {
            model.setValue("nckd_mulcostaccount", null);
        }
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String key = beforeF7SelectEvent.getProperty().getName();
        if ("nckd_mulcalorg".equals(key)) {
            this.beforeF7Select4CalOrg(beforeF7SelectEvent);
        } else if ("nckd_mulcostaccount".equals(key)) {
            this.beforeF7Select4CostAccount(beforeF7SelectEvent);
        } else if ("nckd_mulmaterialgroup".equals(key)) {
            this.beforeF7SelectMaterialGroup(beforeF7SelectEvent);
        } else if ("nckd_mulmaterial".equals(key) || "nckd_materialto".equals(key)) {
            this.beforeF7Select4Mulmaterial(this.getModel(), beforeF7SelectEvent);
        } else if ("nckd_mulbilltype".equals(key)) {
            this.beforeF7Select4Billtype(beforeF7SelectEvent);
        }
    }

    private void beforeF7Select4Billtype(BeforeF7SelectEvent e) {
         /*
            领料出库单	im_MaterialReqOutBill_STD_BT_S	标准领料出库单
            其他入库单	im_OtherInBill_STD_BT_S	标准其他入库单
            其他出库单	im_OtherOutBill_STD_BT_S	标准其他出库单
            生产入库单	im_ProductInbill_STD_BT_S	生产入库单
            采购入库单	im_PurInBill_STD_BT_S	标准采购入库单
            销售出库单	im_SalOutBill_STD_BT_S	标准销售出库单
            完工入库单	im_mdc_mftmanuinbill_BT_S	完工入库单
            完工退库单	im_mdc_mftreturnbill_BT_S	完工退库单
            生产领料单	im_mdc_mftproorder_BT_S	生产领料单
            生产补料单	im_mdc_mftfeedorder_BT_R	生产补料单
            生产退料单	im_mdc_mftreturnorder_BT_S_R	生产退料单
        */
        List<String> numbers = new ArrayList<>();
        numbers.add("im_materialreqoutbill");
        numbers.add("im_otherinbill");
        numbers.add("im_otheroutbill");
        numbers.add("im_productinbill");
        numbers.add("im_purinbill");
        numbers.add("im_saloutbill");
        numbers.add("im_mdc_mftmanuinbill");
        numbers.add("im_mdc_mftreturnbill");
        numbers.add("im_mdc_mftproorder");
        numbers.add("im_mdc_mftfeedorder");
        numbers.add("im_mdc_mftreturnorder");
        QFilter q = new QFilter("billformid", QCP.in, numbers);
        ((ListShowParameter) e.getFormShowParameter()).getListFilterParameter().setFilter(q);
    }

    private void beforeF7Select4Mulmaterial(IDataModel model, BeforeF7SelectEvent e) {
        DynamicObjectCollection materialgroupCol = (DynamicObjectCollection) model.getValue("nckd_mulmaterialgroup");
        if (materialgroupCol != null && !materialgroupCol.isEmpty()) {
            QFilter matFilter = QFilter.of("1 = 1", new Object[0]);
            DynamicObjectCollection matgroupdetailCol = QueryServiceHelper.query("bd_materialgroupdetail", "material.id", new QFilter[]{this.getGroupFilter(materialgroupCol, false, "group.longnumber"), matFilter});
            List<Object> materialIds = new ArrayList(4096);
            Iterator var6 = matgroupdetailCol.iterator();

            while (var6.hasNext()) {
                DynamicObject matgroupdetail = (DynamicObject) var6.next();
                materialIds.add(matgroupdetail.getLong("material.id"));
            }

            ListShowParameter formShowParameter = (ListShowParameter) e.getFormShowParameter();
            formShowParameter.setF7ClickByFilter(true);
            formShowParameter.setShowApproved(false);
            formShowParameter.setShowUsed(false);
            formShowParameter.getListFilterParameter().getQFilters().add(new QFilter("id", "in", materialIds));
        } else {
            ReportF7Helper.beforeF7Select4Material(e);
        }

    }

    private QFilter getGroupFilter(DynamicObjectCollection materialgroupColl, boolean isFromFilteInfo, String fieldName) {
        if (materialgroupColl != null) {
            DynamicProperty property = EntityMetadataCache.getDataEntityType("bd_materialgroup").getProperty("parent");
            GroupProp group = (GroupProp) property;
            String longNumberDLM = "";
            if (group instanceof ParentBasedataProp) {
                longNumberDLM = ((ParentBasedataProp) group).getLongNumberDLM();
            }

            QFilter groupFilter = QFilter.of("1 != 1", new Object[0]);
            Set<String> groupNumSet = new HashSet(16);
            Iterator var8 = materialgroupColl.iterator();

            while (var8.hasNext()) {
                DynamicObject matgroup = (DynamicObject) var8.next();
                String longnumber;
                if (isFromFilteInfo) {
                    longnumber = matgroup.getString("longnumber");
                } else if ("bd_materialgroup".equals(matgroup.getDataEntityType().getName())) {
                    longnumber = matgroup.getString("longnumber");
                } else {
                    longnumber = matgroup.getDynamicObject("fbasedataid").getString("longnumber");
                }

                groupFilter.or(new QFilter(fieldName, "like", longnumber + longNumberDLM + "%"));
                groupNumSet.add(longnumber);
            }

            if (!groupNumSet.isEmpty()) {
                groupFilter.or(new QFilter(fieldName, "in", groupNumSet));
            }

            return groupFilter;
        } else {
            return QFilter.of("1=1", new Object[0]);
        }
    }

    private void beforeF7SelectMaterialGroup(BeforeF7SelectEvent e) {
        QFilter qFilter = new QFilter("status", "=", "C");
        long matgroupstandardId = 730148448254487552L;

        DynamicObjectCollection calOrgList = (DynamicObjectCollection) this.getModel().getValue("nckd_mulcalorg");
        List<Long> orgIds = new ArrayList(1);
        Iterator var8 = calOrgList.iterator();

        while (var8.hasNext()) {
            DynamicObject calOrg = (DynamicObject) var8.next();
            orgIds.add(calOrg.getDynamicObject("fbasedataid").getLong("id"));
        }

        QFilter serviceResponse = DispatchServiceHelper.invokeBizService("bd", "bd", "IMasterDataStandardService", "getGroupByOrgs", new Object[]{"bd_material", orgIds, matgroupstandardId, Boolean.FALSE});
        ((ListShowParameter) e.getFormShowParameter()).getListFilterParameter().getQFilters().add(qFilter.and(serviceResponse));
    }

    private void beforeF7Select4CostAccount(BeforeF7SelectEvent e) {
        DynamicObjectCollection calOrgList = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_mulcalorg");
        QFilter q = new QFilter("id", "=", -1L);
        if (calOrgList != null && calOrgList.size() > 0) {
            Set<Long> calOrgIds = new HashSet();
            Iterator var5 = calOrgList.iterator();

            while (var5.hasNext()) {
                DynamicObject calOrg = (DynamicObject) var5.next();
                calOrgIds.add(calOrg.getDynamicObject("fbasedataid").getLong("id"));
            }

            q = new QFilter("calorg", "in", calOrgIds);
        }

        ((ListShowParameter) e.getFormShowParameter()).getListFilterParameter().setFilter(q);
    }

    private void beforeF7Select4CalOrg(BeforeF7SelectEvent e) {
        Long userId = RequestContext.get().getCurrUserId();
        List<Long> list = PermissionHelper.getUserPermOrgs(userId, "nckd_inoutrecords", "47150e89000000ac");
        if (list != null) {
            QFilter q = new QFilter("id", "in", list);
            ((ListShowParameter) e.getFormShowParameter()).getListFilterParameter().setFilter(q);
        }
    }
}
