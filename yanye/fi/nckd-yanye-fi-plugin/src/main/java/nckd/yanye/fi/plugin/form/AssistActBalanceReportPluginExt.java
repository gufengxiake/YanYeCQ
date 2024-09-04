package nckd.yanye.fi.plugin.form;

import kd.bos.algo.*;
import kd.bos.algo.util.Tuple2;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.ILocaleString;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.BasedataEntityType;
import kd.bos.entity.report.*;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.MetadataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.util.CollectionUtils;
import kd.fi.bd.service.balance.BalanceQueryExecutor;
import kd.fi.bd.service.balance.QueryParam;
import kd.fi.bd.util.BillParamUtil;
import kd.fi.gl.accsys.AccSysUtil;
import kd.fi.gl.accsys.AccountBookInfo;
import kd.fi.gl.comassist.model.ComAssistTable;
import kd.fi.gl.comassist.service.ComAssistTableService;
import kd.fi.gl.common.Tuple;
import kd.fi.gl.enums.basedata.AssistValueType;
import kd.fi.gl.report.AssistActBalanceReportPlugin;
import kd.fi.gl.report.ReportUtils;
import kd.fi.gl.report.subsidiary.AssistBalanceQuery;
import kd.fi.gl.report.subsidiary.SubsidiaryPeriod;
import kd.fi.gl.report.subsidiary.SubsidiaryReportDatasetBuilder;
import kd.fi.gl.util.BaseDataUtil;
import kd.fi.gl.util.FlexUtils;
import kd.fi.gl.util.GLUtil;
import kd.fi.gl.util.PermissonType;
import kd.fi.gl.vo.NameHistoryVO;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :总账-账表-辅助核算维度余额表
 * Description      :数量合计
 *
 * @author 梁秦刚
 * @Date 2024/9/4 14:44
 */
public class AssistActBalanceReportPluginExt extends AbstractReportListDataPlugin {
    private static final Log logger = LogFactory.getLog(AssistActBalanceReportPlugin.class);
    private boolean iscount = false;
    private boolean isleafaccount = false;
    private boolean iszeroamount = false;
    private boolean iszerobalance = false;
    private boolean issyncur = false;
    private boolean isallcur = false;
    private boolean issubstractpl = false;
    private boolean isShowSum = false;
    private long org;
    private long booktype;
    private long startperiod;
    private long endperiod;
    private long periodType;
    private long currency = -1L;
    private long accounttable;
    private List<Long> accountIdList;
    private int accountlevel;
    private List<Long> measureunitList;
    private long currencyLocal;
    private Map<String, Set<Object>> assistMap = new LinkedHashMap();
    private static final String ASSIST_DB = "gl_assist_bd";
    private static final String ASSIST_TXT = "gl_assist_txt";
    private static final String ASSIST_TYPE = "bd_asstacttype";
    private static final String acctView = "bd_accountview";
    private static final String SHOW_SUM = "showsum";
    private List<String> assTxtList = new ArrayList();
    private List<String> dataSetCols;
    private List<String> COMASSIST_BAL_LIST = new ArrayList(8);
    private List<String> COMASSIST_LIST = new ArrayList(8);
    List<Tuple2<String, Set<Long>>> COMASSIST_VALUE_LIST = new ArrayList(16);
    private Map<String, String> flexFieldEntityIdMap = new HashMap(16);
    private Map<String, Tuple<String, String>> flexFieldPropMap = new HashMap(16);
    private Map<Long, SubsidiaryPeriod> periodMap;
    private static final String[] yearbeginField = new String[]{"yearbeginfor", "yearbegindebitlocal", "yearbegincreditlocal", "yearbeginqty"};
    private static final String[] beginField = new String[]{"periodbeginfor", "debitbeginlocal", "creditbeginlocal", "periodbeginqty"};
    private static final String[] periodField = new String[]{"debitfor", "debitlocal", "debitqty", "creditfor", "creditlocal", "creditqty"};
    private static final String[] yearField = new String[]{"yeardebitfor", "yeardebitlocal", "yeardebitqty", "yearcreditfor", "yearcreditlocal", "yearcreditqty"};
    private static final String[] endField = new String[]{"endfor", "debitendlocal", "creditendlocal", "endqty"};
    private static final String[] grpBy = new String[]{"asstypename", "flexfield", "assid", "assvalnumber", "assvalname", "actid", "accountnumber", "accountid", "dc"};

    public AssistActBalanceReportPluginExt() {
    }

    private void init(ReportQueryParam param) {
        FilterInfo filter = param.getFilter();
        this.iscount = filter.getBoolean("showqty");
        this.isleafaccount = filter.getBoolean("showleafaccount");
        this.iszeroamount = filter.getBoolean("nodisplayforzero");
        this.iszerobalance = filter.getBoolean("balancezero");
        this.issubstractpl = filter.getBoolean("issubstractpl");
        this.isShowSum = filter.getBoolean("showsum");
        this.org = filter.getLong("org");
        this.booktype = filter.getLong("booktype");
        AccountBookInfo bookdyn = AccSysUtil.getBookFromAccSys(this.org, this.booktype);
        this.currencyLocal = bookdyn.getBaseCurrencyId();
        this.startperiod = filter.getLong("startperiod");
        this.endperiod = filter.getLong("endperiod");
        this.periodType = filter.getLong("periodtype");
        String curStr = filter.getString("currency");
        if ("basecurrency".equals(curStr)) {
            this.issyncur = true;
        } else if ("allcurrency".equals(curStr)) {
            this.isallcur = true;
        } else {
            this.currency = Long.parseLong(curStr);
        }

        this.accounttable = filter.getLong("accounttable");
        FilterItemInfo itemInfo = filter.getFilterItem("account");
        if (itemInfo != null) {
            List<DynamicObject> value = (List)itemInfo.getValue();
            if (value != null) {
                this.accountIdList = new ArrayList(2);
                Iterator var7 = value.iterator();

                while(var7.hasNext()) {
                    DynamicObject dyn = (DynamicObject)var7.next();
                    this.accountIdList.add(dyn.getLong("id"));
                }
            }
        }

        this.accountlevel = filter.getInt("accountlevel");
        FilterItemInfo unitInfo = filter.getFilterItem("measureunits");
        List value;
        if (unitInfo != null) {
            value = (List)unitInfo.getValue();
            if (value != null) {
                this.measureunitList = new ArrayList(1);
                Iterator var14 = value.iterator();

                while(var14.hasNext()) {
                    DynamicObject dyn = (DynamicObject)var14.next();
                    this.measureunitList.add(dyn.getLong("id"));
                }
            }
        }

        value = filter.getFlexFilterItems();
        List<String> flexFieldList = new ArrayList(4);
        Iterator var16 = value.iterator();

        while(var16.hasNext()) {
            FilterItemInfo filterItemInfo = (FilterItemInfo)var16.next();
            String propName = filterItemInfo.getPropName();
            flexFieldList.add(propName);
            this.assistMap.put(propName, new HashSet(64));
        }

        this.fillAssistTxtType(flexFieldList);
        this.accounttable = filter.getLong("accounttable");
        this.periodMap = SubsidiaryReportDatasetBuilder.initPeriodMap(param.getFilter());
        this.setComassist(param);
    }

    private DataSet queryPeriods(long begin, long end) {
        String selectFields = "id period,periodnumber periodnumber,periodtype,periodyear";
        QFilter filterBegin = new QFilter("id", ">=", begin);
        QFilter filterEnd = new QFilter("id", "<=", end);
        QFilter filterType = new QFilter("periodtype", "=", this.periodType);
        DataSet periodSet = QueryServiceHelper.queryDataSet(this.getClass().getName() + ".periodid", "bd_period", selectFields, filterBegin.and(filterEnd).and(filterType).toArray(), (String)null);
        return periodSet;
    }

    private void fillAssistTxtType(List<String> propNameList) {
        QFilter qFilter = new QFilter("flexfield", "in", propNameList);
        DataSet assistSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), "bd_asstacttype", "id, valuetype, flexfield", new QFilter[]{qFilter}, (String)null);
        Iterator var4 = assistSet.iterator();

        while(var4.hasNext()) {
            Row row = (Row)var4.next();
            if ("3".equals(row.getString("valuetype"))) {
                this.assTxtList.add(row.getString("flexfield"));
            }
        }

    }

    public DataSet query(ReportQueryParam param, Object obj) throws Throwable {
        this.org = param.getFilter().getLong("org");
        List<Long> permOrgList = AccSysUtil.getAcctOrgPkList("gl_rpt_assistactbalance", false, PermissonType.VIEW);
        if (!permOrgList.contains(this.org)) {
            return GLUtil.getEmptyDS(this.getClass());
        } else {
            this.init(param);
            DataSet ds = this.queryBase(param);
            if (ds.isEmpty()) {
                return GLUtil.getEmptyDS(this.getClass());
            } else {
                List<FilterItemInfo> flexItems = param.getFilter().getFlexFilterItems();
                StringBuilder orderStr = new StringBuilder(" case ");

                for(int i = 0; i < flexItems.size(); ++i) {
                    FilterItemInfo info = (FilterItemInfo)flexItems.get(i);
                    orderStr.append(String.format(" when flexfield = '%s' then %d ", info.getPropName(), i));
                }

                orderStr.append(" else 99999 end ");
                ds = ds.addField(orderStr.toString(), "orderflexfield").orderBy(new String[]{"orderflexfield asc", "assvalnumber", "accountnumber asc", "assorder desc"});
                if (this.isleafaccount) {
                    ds = ds.filter("accountlevel <= " + this.accountlevel);
                }

                ds = ds.addField(String.valueOf(this.currencyLocal), "currencylocalid");
                return ds;
            }
        }
    }

    private DataSet gainSumSet(DataSet ds, String flag) {
        List<String> cols = getDataSetCols(ds);
        List<String> groupBy = new ArrayList();
        groupBy.add("flexfield");
        if (this.iscount) {
            groupBy.add("measureunit");
        } else {
            cols.removeIf((x) -> {
                return x.equals("measureunit");
            });
        }

        if (!this.issyncur) {
            groupBy.add("currencyid");
        } else {
            cols.removeIf((x) -> {
                return x.equals("currencyid");
            });
        }

        List sumGroupList;
        if ("sumacct".equals(flag)) {
            sumGroupList = this.getSumGroup();
            groupBy.addAll(sumGroupList);
            ds = ds.groupBy((String[])groupBy.toArray(new String[0])).sum("beginfor").sum("beginlocal").sum("beginqty").sum("debitfor").sum("debitlocal").sum("debitqty").sum("creditfor").sum("creditlocal").sum("creditqty").sum("yeardebitfor").sum("yeardebitlocal").sum("yeardebitqty").sum("yearcreditfor").sum("yearcreditlocal").sum("yearcreditqty").sum("endfor").sum("endlocal").sum("endqty").sum("count").finish();
            ds = ds.select((String[])cols.toArray(new String[0]));
        } else {
            sumGroupList = this.getSumGroup();
            sumGroupList.add("periodyear");
            sumGroupList.add("periodnumber");
            String sumName = ResManager.loadKDString("维度类型小计", "AssistActBalanceReportPlugin_0", "fi-gl-report", new Object[0]);
            int assorderVal = 2;
            if ("sumvalue".equals(flag)) {
                groupBy.add("asstypename");
                groupBy.add("assvalnumber");
                groupBy.add("assvalname");
            } else if ("sumtype".equals(flag)) {
                groupBy.add("asstypename");
                sumName = ResManager.loadKDString("维度类型小计", "AssistActBalanceReportPlugin_0", "fi-gl-report", new Object[0]);
                assorderVal = 3;
            } else if ("total".equals(flag)) {
                sumGroupList.addAll(groupBy);
                groupBy = new ArrayList();
                sumName = ResManager.loadKDString("总合计", "AssistActBalanceReportPlugin_1", "fi-gl-report", new Object[0]);
                assorderVal = 4;
            }

            sumGroupList.removeAll(groupBy);
            ds = this.sumAssistTotal(ds, groupBy);
            this.clearQtyAndCurrency(sumGroupList, cols);
            List<String> sumGroups = new ArrayList();
            Iterator var9 = sumGroupList.iterator();

            while(var9.hasNext()) {
                String field = (String)var9.next();
                if ("assvalname".equals(field)) {
                    sumGroups.add("'" + sumName + "'" + field);
                } else {
                    sumGroups.add("null " + field);
                }
            }

            sumGroupList.add("assorder");
            sumGroups.add(assorderVal + " assorder");
            List<String> allGroupList = new ArrayList();
            allGroupList.addAll(cols);
            allGroupList.removeAll(sumGroupList);
            allGroupList.addAll(sumGroups);
            ds = ds.select((String[])allGroupList.toArray(new String[0]));
            ds = ds.select((String[])cols.toArray(new String[0]));
        }

        return ds;
    }

    private void clearQtyAndCurrency(List<String> sumGroupList, List<String> cols) {
        Iterator var3 = cols.iterator();

        while(true) {
            String field;
            do {
                if (!var3.hasNext()) {
                    return;
                }

                field = (String)var3.next();
                if (!this.iscount && field.endsWith("qty") && !sumGroupList.contains(field)) {
                    sumGroupList.add(field);
                }
            } while(!this.isallcur && !this.issyncur);

            if (field.endsWith("for") && !sumGroupList.contains(field)) {
                sumGroupList.add(field);
            }
        }
    }

    private DataSet sumAssistTotal(DataSet balance, List<String> groupList) {
        long curYearMinPeriodId = this.startperiod;
        DynamicObject yearMinPeriodDyn = GLUtil.getCurYearMinPeriod(this.endperiod);
        long yearMinPeriodId = yearMinPeriodDyn.getLong("id");
        List<Long> periodIdList = GLUtil.getPeriodIds(this.startperiod, this.endperiod);
        if (periodIdList.contains(yearMinPeriodId)) {
            curYearMinPeriodId = yearMinPeriodId;
        }

        String[] yearBeginList = this.getSumFields("yearbegin");
        GroupbyDataSet groupSet = balance.groupBy((String[])groupList.toArray(new String[0])).sum("count");
        String[] beginList = yearBeginList;
        int var12 = yearBeginList.length;

        int var13;
        for(var13 = 0; var13 < var12; ++var13) {
            String field = beginList[var13];
            groupSet.sum("case when period=" + curYearMinPeriodId + " then " + field + " else 0 end", field);
        }

        beginList = this.getSumFields("begin");
        String[] periodList = beginList;
        var13 = beginList.length;

        int var21;
        for(var21 = 0; var21 < var13; ++var21) {
            String field = periodList[var21];
            groupSet.sum("case when period=" + this.startperiod + " then " + field + " else 0 end", field);
        }

        periodList = this.getSumFields("period");
        String[] yearList = periodList;
        var21 = periodList.length;

        int var22;
        for(var22 = 0; var22 < var21; ++var22) {
            String field = yearList[var22];
            groupSet.sum(field);
        }

        yearList = this.getSumFields("year");
        String[] endList = yearList;
        var22 = yearList.length;

        int var24;
        for(var24 = 0; var24 < var22; ++var24) {
            String field = endList[var24];
            groupSet.sum("case when period=" + this.endperiod + " then " + field + " else 0 end", field);
        }

        endList = this.getSumFields("end");
        String[] var25 = endList;
        var24 = endList.length;

        for(int var26 = 0; var26 < var24; ++var26) {
            String field = var25[var26];
            groupSet.sum("case when period=" + this.endperiod + " then " + field + " else 0 end", field);
        }

        balance = groupSet.finish();
        return balance;
    }

    private String[] getSumFields(String type) {
        String[] list;
        switch (type) {
            case "yearbegin":
                list = yearbeginField;
                break;
            case "begin":
                list = beginField;
                break;
            case "period":
                list = periodField;
                break;
            case "year":
                list = yearField;
                break;
            case "end":
                list = endField;
                break;
            default:
                list = new String[0];
        }

        return list;
    }

    private List<String> getSumGroup() {
        List<String> groups = new ArrayList();
        groups.add("asstypename");
        groups.add("assvalnumber");
        groups.add("assvalname");
        groups.add("actid");
        groups.add("accountnumber");
        groups.add("dc");
        groups.add("accountid");
        groups.add("period");
        groups.add("assid");
        groups.addAll(this.COMASSIST_LIST);
        return groups;
    }

    private DataSet queryBase(ReportQueryParam param) {
        long tick = System.currentTimeMillis();
        DataSet ds = this.queryBalance(param);
        if (!Objects.isNull(ds) && !ds.isEmpty()) {
            logger.info("queryBalance cost:{}ms", System.currentTimeMillis() - tick);
            tick = System.currentTimeMillis();
            logger.info("buildFinalBalannceDataSet cost:{}ms", System.currentTimeMillis() - tick);
            tick = System.currentTimeMillis();
            ds = this.gainSumSet(ds, "sumacct");
            ds = this.dealSum(ds);
            if (this.dataSetCols == null) {
                this.dataSetCols = getDataSetCols(ds);
            }

            ds = ds.addField("1", "assorder");
            List<String> orderList = new ArrayList();
            orderList.add("flexfield");
            if (!this.issyncur) {
                orderList.add("currencyid");
            }

            if (this.iscount) {
                orderList.add("measureunit");
            }

            StringBuilder filterBuilder = new StringBuilder();
            if (this.iszeroamount) {
                if (!this.issyncur) {
                    filterBuilder.append(" debitfor != 0.0 or creditfor != 0.0 or ");
                }

                filterBuilder.append(" debitlocal != 0.0 or creditlocal != 0.0 or count > 0");
            }

            if (this.iszerobalance) {
                if (filterBuilder.length() > 0) {
                    filterBuilder.append(" or ");
                }

                if (!this.issyncur) {
                    filterBuilder.append(" endfor != 0.0 or ");
                }

                filterBuilder.append(" debitendlocal != 0.0 or creditendlocal != 0.0 ");
            }

            if (filterBuilder.length() > 0) {
                ds = ds.filter(filterBuilder.toString());
            }

            DataSet totalSet = this.gainSumSet(ds.copy(), "total");
            if (this.isShowSum) {
                DataSet sumValSet = this.gainSumSet(ds.copy(), "sumvalue");
                DataSet sumTypeSet = this.gainSumSet(ds.copy(), "sumtype");
                ds = ds.union(sumValSet);
                ds = ds.addField("1", "typeorder");
                sumTypeSet = sumTypeSet.addField("2", "typeorder");
                ds = ds.union(sumTypeSet);
                orderList.add("typeorder");
            }

            orderList.add("assvalnumber");
            orderList.add("assvalname");
            orderList.add("assorder");
            ds = ds.orderBy((String[])orderList.toArray(new String[0]));
            if (this.isShowSum) {
                ds = ds.removeFields(new String[]{"typeorder"});
            }

            ds = ds.union(totalSet);
            ds = this.replaceHistoryName(ds);
            logger.info("join cost:{}ms", System.currentTimeMillis() - tick);
            return ds;
        } else {
            return GLUtil.getEmptyDS(this.getClass());
        }
    }

    private DataSet replaceHistoryName(DataSet ds) {
        Map<String, NameHistoryVO> flexVOMap = new HashMap(32);
        Iterator var3 = ds.copy().iterator();

        while(var3.hasNext()) {
            Row row = (Row)var3.next();
            if (!Objects.isNull(row.getLong("period"))) {
                Date nameCtrlDate = ((SubsidiaryPeriod)this.periodMap.get(row.getLong("period"))).getEnddate();
                String flexField = row.getString("flexfield");
                String entityId = (String)this.flexFieldEntityIdMap.get(flexField);
                if (Objects.nonNull(entityId)) {
                    String groupKey = String.format("%s_%s", flexField, row.getLong("period"));
                    Long assid = row.getLong("assid");
                    if (Objects.nonNull(assid) && assid != 0L) {
                        NameHistoryVO nameHistoryVO = (NameHistoryVO)flexVOMap.computeIfAbsent(groupKey, (key) -> {
                            return new NameHistoryVO(entityId, this.org, String.format("id,%s name", ((Tuple)this.flexFieldPropMap.get(flexField)).item1), nameCtrlDate);
                        });
                        nameHistoryVO.addAssId(assid);
                        nameHistoryVO.addProp("period", row.getLong("period"));
                        nameHistoryVO.addProp("flexfield", row.getString("flexfield"));
                    }
                }
            }
        }

        BaseDataUtil.queryBaseDataByDate(flexVOMap.values());
        String[] fieldNames = new String[]{"assistKey", "historyname"};
        DataType[] dataTypes = new DataType[]{DataType.StringType, DataType.StringType};
        RowMeta rowMeta = RowMetaFactory.createRowMeta(fieldNames, dataTypes);
        DataSetBuilder builder = Algo.create(this.getClass().getName() + "buildAssistDataSet").createDataSetBuilder(rowMeta);
        flexVOMap.values().stream().forEach((vo) -> {
            vo.getAssIdNameMap().entrySet().stream().forEach((entry) -> {
                builder.append(new Object[]{String.format("%s_%s_%s", vo.getProp("flexfield"), vo.getProp("period"), entry.getKey()), entry.getValue()});
            });
        });
        DataSet assistDataSet = builder.build();
        ds = ds.addField("concat(String(flexfield),'_',String(period),'_',String(assid))", "assistKey");
        List<String> finalCols = this.getLeftJoinAssistDataSetCols(ds);
        List<String> cols = GLUtil.getDataSetCols(ds);
        ds = ds.join(assistDataSet, JoinType.LEFT).on("assistKey", "assistKey").select((String[])cols.toArray(new String[0]), new String[]{"historyname"}).finish().select((String[])finalCols.toArray(new String[0]));
        return ds;
    }

    private List<String> getLeftJoinAssistDataSetCols(DataSet ds) {
        RowMeta rowMeta = ds.getRowMeta();
        Field[] fields = rowMeta.getFields();
        List<String> list = new ArrayList(fields.length);
        Field[] var5 = fields;
        int var6 = fields.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            Field field = var5[var7];
            if ("assvalname".equals(field.getName())) {
                list.add("case when historyname is null then assvalname else historyname end as assvalname");
            } else {
                list.add(field.getName());
            }
        }

        return list;
    }

    private DataSet queryBalance(ReportQueryParam reportQueryParam) {
        List<String> listFlex = new ArrayList(this.assistMap.keySet());
        QFilter fflex = new QFilter("flexfield", "in", listFlex);
        String algoKey = this.getClass().getName();
        DataSet assTypeSet = QueryServiceHelper.queryDataSet(algoKey, "bd_asstacttype", "id", new QFilter[]{fflex}, (String)null);
        List<Long> assTypeIds = new ArrayList();
        Iterator var7 = assTypeSet.iterator();

        while(var7.hasNext()) {
            Row row = (Row)var7.next();
            assTypeIds.add(row.getLong("id"));
        }

        QFilter faccount = new QFilter("checkitementry.asstactitem.id", "in", assTypeIds);
        DataSet accountSet = QueryServiceHelper.queryDataSet(algoKey, "bd_accountview", "id", new QFilter[]{faccount}, (String)null);
        Set<Long> accountIds = new HashSet();
        Iterator var10 = accountSet.iterator();

        while(var10.hasNext()) {
            Row acct = (Row)var10.next();
            accountIds.add(acct.getLong("id"));
        }

        if (this.accountIdList != null) {
            accountIds.retainAll(this.accountIdList);
        }

        String selectFields = this.getSelectFields();
        Long[] currencyIds = null;
        if (this.iscount) {
            selectFields = selectFields + ",measureunit";
        }

        if (!this.issyncur && !this.isallcur) {
            currencyIds = new Long[]{this.currency};
        }

        AssistBalanceQuery query = new AssistBalanceQuery(reportQueryParam.getFilter());
        QueryParam param = new QueryParam();
        param.setCurrencyIds(currencyIds);
        param.setOnlyLeafAcctBal(true);
        param.setAccountFilter(new QFilter("id", "in", accountIds));
        if (this.measureunitList != null) {
            param.setMeasureUnitIds((Long[])this.measureunitList.toArray(new Long[0]));
        }

        param.setSubstractPL(this.issubstractpl);
        param.setAccountVersionPeriodId(this.endperiod);
        int balancelimit = BillParamUtil.getIntegerValue("83bfebc8000017ac", "fi.gl.report.assistactbal.balancelimit", 100000);
        param.setQueryLimit(balancelimit);
        param.getCustomFilter().addAll(this.buildComAssistQfilters());
        selectFields = selectFields + this.comAssistFields();
        Tuple2<List<QFilter>, String> customFiltersAndSelectField = query.getCustomFlexPropFilterAndSelectFields(new String[]{AssistBalanceQuery.CustomFlexProperty.ofNoneDetailAlia("assval")});
        List<QFilter> customF = param.getCustomFilter();
        if (customF == null) {
            customF = new ArrayList(4);
        }

        List<QFilter> flexFilters = (List)customFiltersAndSelectField.t1;
        String[] flexFields = ((String)customFiltersAndSelectField.t2).split(",");
        Map<String, Triple<String, String, String>> flexMap = FlexUtils.buildFlexValueSourceMap(reportQueryParam);
        List<DataSet> dsList = new ArrayList(8);

        for(int i = 0; i < flexFilters.size(); ++i) {
            List<QFilter> singleFlexFilters = new ArrayList((Collection)customF);
            singleFlexFilters.add(flexFilters.get(i));
            String selectField = flexFields[i + 1];
            String flexField = selectField.split(" ")[2];
            Triple<String, String, String> flexTriple = (Triple)flexMap.get(flexField);
            if (flexTriple.getRight() != null) {
                if (!((String)flexTriple.getRight()).equals("bos_assistantdata_detail")) {
                    this.flexFieldEntityIdMap.put(flexField, flexTriple.getRight());
                }

                BasedataEntityType entity = (BasedataEntityType) MetadataServiceHelper.getDataEntityType((String)flexTriple.getRight());
                this.flexFieldPropMap.put(flexField, Tuple.create(entity.getNameProperty(), entity.getNumberProperty()));
            }

            param.setBalEntityType(query.balEntityType(flexField));
            DataSet singleFlexDataSet = this.getSingleFlexBalanceDataSet(selectFields + "," + selectField, param, singleFlexFilters, flexTriple);
            if (!singleFlexDataSet.isEmpty()) {
                dsList.add(singleFlexDataSet);
            }
        }

        DataSet ds = null;
        if (CollectionUtils.isNotEmpty(dsList)) {
            ds = (DataSet)dsList.get(0);
            if (dsList.size() >= 2) {
                for(int i = 1; i < dsList.size(); ++i) {
                    ds = ds.union((DataSet)dsList.get(i));
                }
            }
        }

        return ds;
    }

    private DataSet getSingleFlexBalanceDataSet(String selectFields, QueryParam param, List<QFilter> singleFlexFilters, Triple<String, String, String> flexTriple) {
        param.setCustomFilter(singleFlexFilters);
        DataSet amountBegin = BalanceQueryExecutor.getInstance().getBalance(selectFields.replace("period,", ""), new Long[]{this.org}, this.booktype, this.accounttable, this.startperiod, this.startperiod, param);
        amountBegin = amountBegin.addField(this.startperiod + "L", "period");
        if (this.startperiod != this.endperiod) {
            DynamicObject nextStartPeriod = GLUtil.getNextPeriod(this.startperiod);
            DataSet gl_balance = BalanceQueryExecutor.getInstance().getBalance(selectFields, new Long[]{this.org}, this.booktype, this.accounttable, nextStartPeriod != null ? nextStartPeriod.getLong("id") : this.endperiod, this.endperiod, param);
            List<String> sels = getDataSetCols(gl_balance);
            gl_balance = gl_balance.select((String[])sels.toArray(new String[0]));
            amountBegin = amountBegin.select((String[])sels.toArray(new String[0]));
            amountBegin = amountBegin.union(gl_balance);
        }

        return this.appendFlexInfo(amountBegin, flexTriple);
    }

    private DataSet appendFlexInfo(DataSet balanceDataSet, Triple<String, String, String> flexTriple) {
        DataSet copy = balanceDataSet.copy();
        Set<Object> assistValues = new HashSet(64);
        Set<Long> accountIdSet = new HashSet(64);
        Iterator var6 = copy.iterator();

        while(var6.hasNext()) {
            Row row = (Row)var6.next();
            assistValues.add(row.get((String)flexTriple.getLeft()));
            accountIdSet.add(row.getLong("actid"));
        }

        Map<String, Set<Long>> flexAccountIdMap = new HashMap(8);
        DataSet accountDataSet = QueryServiceHelper.queryDataSet(this.getClass().getName() + "queryFlexField", "bd_accountview", "id,checkitementry.asstactitem.flexfield flexfield", new QFilter[]{new QFilter("id", "in", accountIdSet)}, (String)null);
        Throwable var8 = null;

        try {
            Row row;
            Set idSet;
            try {
                for(Iterator var9 = accountDataSet.iterator(); var9.hasNext(); ((Set)idSet).add(row.getLong("id"))) {
                    row = (Row)var9.next();
                    String flexfield = row.getString("flexfield");
                    idSet = (Set)flexAccountIdMap.get(flexfield);
                    if (Objects.isNull(idSet)) {
                        idSet = new HashSet(64);
                        flexAccountIdMap.put(flexfield, idSet);
                    }
                }
            } catch (Throwable var20) {
                var8 = var20;
                throw var20;
            }
        } finally {
            if (accountDataSet != null) {
                if (var8 != null) {
                    try {
                        accountDataSet.close();
                    } catch (Throwable var19) {
                        var8.addSuppressed(var19);
                    }
                } else {
                    accountDataSet.close();
                }
            }

        }

        List<String> cols = getDataSetCols(balanceDataSet);
        String flexfield = (String)flexTriple.getLeft();
        if (Objects.isNull(flexAccountIdMap.get(flexfield))) {
            return GLUtil.getEmptyDS(this.getClass());
        } else {
            String actIdStr = ((Set)flexAccountIdMap.get(flexfield)).toString().replace("[", "(").replace("]", ")");
            balanceDataSet = balanceDataSet.filter("actid in" + actIdStr);
            List<String> newCols = new ArrayList(cols);
            if (flexTriple.getRight() != null) {
                Tuple<String, String> propTuple = (Tuple)this.flexFieldPropMap.get(flexTriple.getLeft());
                DataSet assistDataSet = QueryServiceHelper.queryDataSet(AssistActBalanceReportPlugin.class.getName() + "#queryAssistInfo", (String)flexTriple.getRight(), String.format("id,%s assvalname,%s assvalnumber", propTuple.item1, propTuple.item2), (new QFilter("id", "in", assistValues)).toArray(), (String)null);
                balanceDataSet = balanceDataSet.join(assistDataSet, JoinType.LEFT).on(flexfield, "id").select((String[])newCols.toArray(new String[0]), new String[]{"id assid", "assvalname", "assvalnumber"}).finish();
            } else {
                newCols.add("0L assid");
                newCols.add(flexfield + " assvalnumber");
                newCols.add(flexfield + " assvalname");
                balanceDataSet = balanceDataSet.select((String[])newCols.toArray(new String[0]));
            }

            balanceDataSet = balanceDataSet.addField("'" + (String)flexTriple.getMiddle() + "'", "asstypename").addField("'" + flexfield + "'", "flexfield");
            balanceDataSet = balanceDataSet.removeFields(new String[]{flexfield});
            return balanceDataSet;
        }
    }

    private String comAssistFields() {
        StringBuilder comAssistBuildStr = new StringBuilder();
        Iterator var2 = this.COMASSIST_BAL_LIST.iterator();

        while(var2.hasNext()) {
            String comassistField = (String)var2.next();
            comAssistBuildStr.append(" ,");
            comAssistBuildStr.append(comassistField);
        }

        return comAssistBuildStr.toString();
    }

    private List<QFilter> buildComAssistQfilters() {
        return ComAssistTableService.getComAssistFilters(ComAssistTable.get(this.accounttable), Collections.singletonList(this.org), this.getQueryParam().getFilter(), (key) -> {
            return key;
        }, "masterid");
    }

    private void setComassist(ReportQueryParam param) {
        FilterInfo filterInfo = param.getFilter();
        DynamicObject acctTableDyn = filterInfo.getDynamicObject("accounttable");
        if (acctTableDyn != null) {
            DynamicObjectCollection accTableColl = acctTableDyn.getDynamicObjectCollection("comassistentry");
            List<Object[]> entityList = new ArrayList(8);
            int i = 1;
            Iterator var7 = accTableColl.iterator();

            while(var7.hasNext()) {
                DynamicObject row = (DynamicObject)var7.next();
                String valType = row.getString("valuesourcetype");
                if (valType != null) {
                    String entityId = null;
                    ILocaleString locaName = row.getLocaleString("comassistname");
                    if (AssistValueType.isAssistData(valType)) {
                        entityId = "bos_assistantdata_detail";
                    } else if (row.getDynamicObject("valuesource") != null) {
                        DynamicObject valTypeObj = row.getDynamicObject("valuesource");
                        entityId = valTypeObj.getString("number");
                    }

                    String fieldKey = "comassist" + i;
                    if (filterInfo.containProp(fieldKey)) {
                        DynamicObjectCollection coll = filterInfo.getDynamicObjectCollection(fieldKey);
                        String fieldKeyId = fieldKey + "id";
                        if (coll != null) {
                            Set<Long> ids = (Set)coll.stream().map((o) -> {
                                return o.getLong("id");
                            }).collect(Collectors.toSet());
                            this.COMASSIST_VALUE_LIST.add(new Tuple2(fieldKey, ids));
                        } else {
                            this.COMASSIST_VALUE_LIST.add(new Tuple2(fieldKey, new HashSet(8)));
                        }

                        this.COMASSIST_BAL_LIST.add(fieldKey + " " + fieldKeyId);
                        this.COMASSIST_LIST.add(fieldKeyId);
                        entityList.add(new Object[]{fieldKeyId, entityId, locaName});
                    }

                    ++i;
                }
            }

            Map<String, Object> customParam = param.getCustomParam();
            if (!filterInfo.getBoolean("showcomassist")) {
                customParam.put("ComAssistEntityListStr", (Object)null);
                this.COMASSIST_LIST.clear();
                this.COMASSIST_BAL_LIST.clear();
            } else {
                String entityStr = SerializationUtils.toJsonString(entityList);
                customParam.put("ComAssistEntityListStr", entityStr);
            }
        }
    }

    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        super.getColumns(columns);
        this.addComassistColunms(columns);
        return columns;
    }

    private void addComassistColunms(List<AbstractReportColumn> columns) {
        String comAssistValsListStr = (String)this.getQueryParam().getCustomParam().get("ComAssistEntityListStr");
        if (comAssistValsListStr != null) {
            List<List<Object>> comAssistList = (List)SerializationUtils.fromJsonString(comAssistValsListStr, List.class);
            if (!comAssistList.isEmpty()) {
                int index = -1;
                Iterator var5 = columns.iterator();

                String comFieldKey;
                while(var5.hasNext()) {
                    AbstractReportColumn x = (AbstractReportColumn)var5.next();
                    ++index;
                    if (x instanceof ReportColumn) {
                        comFieldKey = ((ReportColumn)x).getFieldKey();
                        if ("measureunit".equals(comFieldKey)) {
                            break;
                        }
                    }
                }

                for(int i = comAssistList.size() - 1; i >= 0; --i) {
                    List<Object> tuplMap = (List)comAssistList.get(i);
                    comFieldKey = (String)tuplMap.get(0);
                    String entityId = (String)tuplMap.get(1);
                    LocaleString nameLocal = new LocaleString();
                    LinkedHashMap<String, String> nameMap = (LinkedHashMap)tuplMap.get(2);
                    Iterator var11 = nameMap.entrySet().iterator();

                    while(var11.hasNext()) {
                        Map.Entry<String, String> map = (Map.Entry)var11.next();
                        String key = (String)map.getKey();
                        String val = (String)map.getValue();
                        nameLocal.setItem(key, val);
                    }

                    columns.add(index, this.createComAssist(comFieldKey, entityId, nameLocal));
                }
            }

        }
    }

    private ReportColumn createComAssist(String fieldKey, String entityId, LocaleString nameLocal) {
        ReportColumn reportColumn = this.createComAssist(nameLocal, fieldKey, "basedata");
        reportColumn.setEntityId(entityId);
        return reportColumn;
    }

    private ReportColumn createComAssist(LocaleString caption, String fieldKey, String fieldType) {
        ReportColumn column = new ReportColumn();
        column.setCaption(caption);
        column.setFieldKey(fieldKey);
        column.setFieldType(fieldType);
        return column;
    }

    private String[] getGroupSelByCur() {
        List<String> list = new ArrayList(Arrays.asList(grpBy));
        if (!this.issyncur) {
            list.add("currencyid");
        }

        if (this.iscount) {
            list.add("measureunit");
        }

        list.addAll(this.COMASSIST_LIST);
        return (String[])list.toArray(new String[0]);
    }

    private DataSet dealSum(DataSet gl_balance) {
        String[] groupBy = this.getGroupSelByCur();
        List<String> cols = getDataSetCols(gl_balance);
        DataSet period = this.queryPeriods(this.startperiod, this.endperiod);
        DataSet assType = gl_balance.copy().groupBy(groupBy).finish();
        DataSet assPeriod = assType.join(period, JoinType.CROSS).select(groupBy, new String[]{"period", "periodyear", "periodnumber"}).finish();
        List<String> assList = getDataSetCols(assPeriod);
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("asstypename");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("flexfield");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("assid");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("assvalnumber");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("assvalname");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("actid");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("accountnumber");
        });
        if (!this.issyncur) {
            cols.removeIf((x) -> {
                return x.equalsIgnoreCase("currencyid");
            });
        }

        if (this.iscount) {
            cols.removeIf((x) -> {
                return x.equalsIgnoreCase("measureunit");
            });
        }

        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("accountid");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("period");
        });
        cols.removeIf((x) -> {
            return x.equalsIgnoreCase("dc");
        });
        JoinDataSet groupData = gl_balance.join(assPeriod, JoinType.RIGHT).on("flexfield", "flexfield").on("assid", "assid").on("assvalnumber", "assvalnumber").on("assvalname", "assvalname").on("actid", "actid").on("period", "period");
        this.COMASSIST_LIST.forEach((x) -> {
            groupData.on(x, x);
            cols.removeIf((y) -> {
                return y.equalsIgnoreCase(x);
            });
        });
        if (!this.issyncur) {
            groupData.on("currencyid", "currencyid");
        }

        if (this.iscount) {
            groupData.on("measureunit", "measureunit");
        }

        gl_balance = groupData.select((String[])cols.toArray(new String[0]), (String[])assList.toArray(new String[0])).finish().orderBy((String[])assList.toArray(new String[0]));
        List<String> sels = getDataSetCols(gl_balance);
        List<String> selNullDeal = new ArrayList();
        Iterator var11 = sels.iterator();

        while(true) {
            String condition;
            while(var11.hasNext()) {
                condition = (String)var11.next();
                if (!condition.endsWith("local") && !condition.endsWith("for") && !condition.endsWith("qty")) {
                    selNullDeal.add(condition);
                } else {
                    selNullDeal.add("case when " + condition + " =null then 0.0 else " + condition + " end as " + condition);
                }
            }

            gl_balance = gl_balance.select((String[])selNullDeal.toArray(new String[0]));
            List<String> selList = new ArrayList(16);
            condition = "PreRowValue(asstypename) = asstypename and PreRowValue(assvalnumber) = assvalnumber and PreRowValue(assid) = assid and PreRowValue(actid) = actid";
            StringBuilder comAssistCond = new StringBuilder(condition);
            this.COMASSIST_LIST.forEach((x) -> {
                comAssistCond.append(" and PreRowValue(");
                comAssistCond.append(x);
                comAssistCond.append(") = ");
                comAssistCond.append(x);
            });
            condition = comAssistCond.toString();
            if (!this.issyncur) {
                condition = condition + " and PreRowValue(currencyid) = currencyid";
            }

            selList.add("asstypename");
            selList.add("assvalnumber");
            selList.add("assvalname");
            selList.add("actid");
            selList.add("accountnumber");
            selList.add("accountid");
            selList.add("period");
            selList.add("periodyear");
            selList.add("periodnumber");
            if (!this.issyncur) {
                selList.add("currencyid");
            }

            selList.addAll(this.COMASSIST_LIST);
            selList.add("dc");
            selList.add("flexfield");
            selList.add("assid");
            selList.add("count");
            if (this.iscount) {
                selList.add("measureunit");
                condition = condition + " and PreRowValue(measureunit) = measureunit";
            }

            selList.add("case when " + condition + " then PreRowValue(endfor) else beginfor end as beginfor");
            selList.add("case when " + condition + " then PreRowValue(endlocal) else beginlocal end as beginlocal");
            selList.add("case when " + condition + " then PreRowValue(endqty) else beginqty end as beginqty");
            selList.add("debitfor");
            selList.add("debitlocal");
            selList.add("debitqty");
            selList.add("creditfor");
            selList.add("creditlocal");
            selList.add("creditqty");
            selList.add("case when " + condition + " then PreRowValue() + debitfor - creditfor else endfor end as endfor");
            selList.add("case when " + condition + " then PreRowValue() + debitlocal - creditlocal else endlocal end as endlocal");
            selList.add("case when " + condition + " then PreRowValue() + debitqty -creditqty else endqty end as endqty");
            String yearCondition = condition + " and PreRowValue(periodyear) = periodyear";
            selList.add("case when " + yearCondition + " then PreRowValue() + debitfor else yeardebitfor end as yeardebitfor");
            selList.add("case when " + yearCondition + " then PreRowValue() + debitlocal else yeardebitlocal end as yeardebitlocal");
            selList.add("case when " + yearCondition + " then PreRowValue() + debitqty else yeardebitqty end as yeardebitqty");
            selList.add("case when " + yearCondition + " then PreRowValue() + creditfor else yearcreditfor end as yearcreditfor");
            selList.add("case when " + yearCondition + " then PreRowValue() + creditlocal else yearcreditlocal end as yearcreditlocal");
            selList.add("case when " + yearCondition + " then PreRowValue() + creditqty else yearcreditqty end as yearcreditqty");
            gl_balance = gl_balance.select((String[])selList.toArray(new String[0]));
            List<String> list = getDataSetCols(gl_balance);
            list.remove("beginfor");
            list.remove("beginlocal");
            list.remove("beginqty");
            list.remove("endfor");
            list.remove("endlocal");
            list.remove("endqty");
            list.add("case when dc='1' then endfor - yeardebitfor + yearcreditfor else -1*(endfor - yeardebitfor + yearcreditfor) end as yearbeginfor");
            list.add("case when dc='1' then beginfor else -1*beginfor end as periodbeginfor");
            list.add("case when dc='1' then endfor else -1*endfor end as endfor");
            Boolean isShowByActDC = ReportUtils.getShowByActDCSysParam(this.org);
            if (isShowByActDC) {
                list.add("case when dc='1' then (endlocal - yeardebitlocal + yearcreditlocal) else 0 end yearbegindebitlocal");
                list.add("case when dc='-1' then -1*(endlocal - yeardebitlocal + yearcreditlocal) else 0 end yearbegincreditlocal");
                list.add("case when dc='1' then endqty - yeardebitqty + yearcreditqty else -1*(endqty - yeardebitqty + yearcreditqty) end yearbeginqty");
                list.add("case when dc='1' then beginlocal else 0 end debitbeginlocal");
                list.add("case when dc='-1' then -1*beginlocal else 0 end creditbeginlocal");
                list.add("case when dc='1' then beginqty else -1*beginqty end periodbeginqty");
                list.add("case when dc='1' then endlocal else 0 end debitendlocal");
                list.add("case when dc='-1' then -1*endlocal else 0 end creditendlocal");
                list.add("case when dc='1' then endqty else -1*endqty end endqty");
            } else {
                list.add("case when (endlocal - yeardebitlocal + yearcreditlocal)>0 then endlocal - yeardebitlocal + yearcreditlocal else 0.0 end yearbegindebitlocal");
                list.add("case when (endlocal - yeardebitlocal + yearcreditlocal)<0 then -1*(endlocal - yeardebitlocal + yearcreditlocal) else 0.0 end yearbegincreditlocal");
                list.add("case when (endqty - yeardebitqty + yearcreditqty)>0 then (endqty - yeardebitqty + yearcreditqty) else -1*(endqty - yeardebitqty + yearcreditqty) end yearbeginqty");
                list.add("case when beginlocal>0 then beginlocal else 0.0 end debitbeginlocal");
                list.add("case when beginlocal<0 then -1*beginlocal else 0.0 end creditbeginlocal");
                list.add("case when beginqty>0 then beginqty else -1*beginqty end periodbeginqty");
                list.add("case when endlocal>0 then endlocal else 0.0 end debitendlocal");
                list.add("case when endlocal<0 then -1*endlocal else 0.0 end creditendlocal");
                list.add("case when endqty>0 then endqty else -1*endqty end endqty");
            }

            gl_balance = gl_balance.select((String[])list.toArray(new String[0]));
            return gl_balance;
        }
    }

    public String getSelectFields() {
        String accountNameField = GLUtil.getAcctNameBySysParam(this.org);
        String fields = String.format("period,account.id actid,account.number accountnumber,account.%s accountid,account.dc dc,currency currencyid,beginfor,beginlocal,beginqty,debitfor,", accountNameField) + "debitlocal,debitqty,creditfor,creditlocal,creditqty,endfor,endlocal,endqty,yeardebitfor,yeardebitlocal,yeardebitqty,yearcreditfor,yearcreditlocal,yearcreditqty,count";
        return fields;
    }

    private DataSet queryAssistGroupSet() {
        List<DataSet> dsList = new ArrayList(4);
        Iterator var3 = this.assistMap.entrySet().iterator();

        DataSet ds;
        while(var3.hasNext()) {
            Map.Entry<String, Set<Object>> entry = (Map.Entry)var3.next();
            String flexfield = (String)entry.getKey();
            Set<Object> value = (Set)entry.getValue();
            ds = this.queryAssistSet(flexfield, value);
            dsList.add(ds);
        }

        ds = (DataSet)dsList.get(0);
        if (dsList.size() >= 2) {
            for(int i = 1; i < dsList.size(); ++i) {
                ds = ds.union((DataSet)dsList.get(i));
            }
        }

        return ds;
    }

    private DataSet queryAssistSet(String ffield, Set<Object> assval) {
        String entityname = "gl_assist_bd";
        if (this.assTxtList.contains(ffield)) {
            entityname = "gl_assist_txt";
        }

        String hgFields = "hg assisthg, asstype, assval";
        ArrayList<QFilter> qfList = new ArrayList();
        QFilter qFilter = new QFilter("asstype", "=", ffield);
        qfList.add(qFilter);
        if (assval != null && assval.size() > 0) {
            qFilter = new QFilter("assval", "in", assval);
            qfList.add(qFilter);
        } else {
            if (this.assTxtList.contains(ffield)) {
                qFilter = new QFilter("assval", "!=", '0');
            } else {
                qFilter = new QFilter("assval", "!=", 0);
            }

            qfList.add(qFilter);
        }

        QFilter[] filters = (QFilter[])qfList.toArray(new QFilter[0]);
        DataSet ds = QueryServiceHelper.queryDataSet(this.getClass().getName() + entityname, entityname, hgFields, filters, (String)null);
        String assFields = "name asstypename,valuesource,flexfield,valuetype,assistanttype";
        QFilter assFilter1 = new QFilter("flexfield", "=", ffield);
        DataSet dSet = QueryServiceHelper.queryDataSet(this.getClass().getName() + ".bd_asstacttype", "bd_asstacttype", assFields, new QFilter[]{assFilter1}, (String)null);
        ds = ds.join(dSet, JoinType.LEFT).on("asstype", "flexfield").select(new String[]{"assisthg", "assval"}, new String[]{"flexfield", "asstypename", "valuesource", "valuetype", "assistanttype"}).finish();
        if (this.assTxtList.contains(ffield)) {
            return this.manualAssistSet(ds);
        } else {
            HashSet<Long> values = new HashSet();
            String sourceName = null;
            String valuetype = null;

            Row row;
            for(Iterator var15 = ds.copy().iterator(); var15.hasNext(); values.add(row.getLong("assval"))) {
                row = (Row)var15.next();
                valuetype = row.getString("valuetype");
                if ("1".equals(valuetype)) {
                    sourceName = row.getString("valuesource");
                } else if ("2".equals(valuetype)) {
                    sourceName = row.getString("assistanttype");
                }
            }

            if (values.size() > 0 && valuetype != null && sourceName != null) {
                DataSet assistSet = null;
                String name = "name";
                String number = "number";
                QFilter fDetail;
                if ("1".equals(valuetype)) {
                    fDetail = new QFilter("id", "in", values);
                    BasedataEntityType entityType = (BasedataEntityType)MetadataServiceHelper.getDataEntityType(sourceName);
                    name = entityType.getNameProperty();
                    number = entityType.getNumberProperty();
                    assistSet = QueryServiceHelper.queryDataSet(this.getClass().getName() + ".assvalname", sourceName, "id," + name + "," + number, new QFilter[]{fDetail}, (String)null);
                } else if ("2".equals(valuetype)) {
                    fDetail = new QFilter("group.id", "=", Long.parseLong(sourceName));
                    assistSet = QueryServiceHelper.queryDataSet(this.getClass().getName() + ".assvalname", "bos_assistantdata_detail", "id,number,name", new QFilter[]{fDetail}, (String)null);
                }

                ds = ds.join(assistSet, JoinType.LEFT).on("assval", "id").select(new String[]{"assisthg", "flexfield", "asstypename"}, new String[]{"id assid", number + " assvalnumber", name + " assvalname"}).finish();
            } else {
                ds = ds.select(new String[]{"assisthg", "flexfield", "asstypename", "assval assid", "'' assvalnumber", "'' assvalname"}).filter("1=0");
            }

            return ds;
        }
    }

    private DataSet manualAssistSet(DataSet ds) {
        ds = ds.select(new String[]{"assisthg", "flexfield", "asstypename", "0L assid", "assval assvalnumber", "assval assvalname"});
        return ds;
    }

    public static List<String> getDataSetCols(DataSet ds) {
        List<String> list = new ArrayList(16);
        RowMeta rowMeta = ds.copy().getRowMeta();
        Field[] fields = rowMeta.getFields();
        Field[] var4 = fields;
        int var5 = fields.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Field field = var4[var6];
            list.add(field.getName());
        }

        return list;
    }
}
