package nckd.yanye.fi.plugin.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.IFormView;
import kd.bos.form.ShowType;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.ReportShowParameter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;
import kd.fi.gl.util.GLUtil;
import java.util.*;

/**
 * @author wanghaiwu_kd
 * @date 2023/07/14
 */
public class NavToAssRptExt {
    protected ReportQueryParam param;
    protected IFormView view;
    protected String acctIds;
    protected Long orgId;
    protected Long curId;
    protected Long measureunitId;
    protected String assgrpIds;
    protected String navKey;

    private static final String KEY_FIELD_ISSUB = "issubstractpl";

    public void apply() {
        ReportShowParameter report = new ReportShowParameter();

        if(StringUtils.isEmpty(this.navKey)){
            report.setFormId("gl_rpt_subsidiaryledger");
        } else {
            report.setFormId(this.navKey);
        }
        report.getOpenStyle().setShowType(ShowType.MainNewTabPage);
        FilterInfo info = this.param.getFilter();
        Long org = this.orgId;
        if (this.orgId == null || this.orgId == 0L) {
            org = info.getLong("org");
        }

        Long bookType = info.getLong("booktype");
        FilterInfo filter = new FilterInfo();
        long periodType = info.getLong("periodtype");
        long startPeriod = info.getLong("startperiod");
        long endPeriod = info.getLong("endperiod");
        Date dateBegin = info.getDate("datebegin");
        Date dateEnd = info.getDate("dateend");
        DynamicObject acct;
        if (dateBegin != null && dateEnd != null) {
            DynamicObject sp = GLUtil.getPeriodByDate(dateBegin, periodType);
            acct = GLUtil.getPeriodByDate(dateEnd, periodType);
            if (sp != null && acct != null) {
                startPeriod = sp.getLong("id");
                endPeriod = acct.getLong("id");
            }
        }

        Map<String, Object> flexProp = new HashMap<String, Object>();
        if ("gl_rpt_assistbalance".equals(this.navKey)) {
            filter.addFilterItem("orgs", info.getValue("orgs"));
            filter.addFilterItem("orgview", info.getValue("orgview"));

            if (StringUtils.isNotEmpty(this.assgrpIds)) {
                List<Long> flexIdList = new ArrayList<Long>();
                for (String id : this.assgrpIds.split(",")){
                    flexIdList.add(Long.valueOf(id));
                }
                QFilter qFilter = new QFilter("id", QCP.in, flexIdList);
                String selectProperties = "id, flexfield";
                DynamicObject[] actTypes = BusinessDataServiceHelper.load("bd_asstacttype", selectProperties, new QFilter[]{qFilter});
                for (DynamicObject actType : actTypes) {
                    flexProp.put(actType.getString("flexfield"), 0);
                }
            }
        }

        List<FilterItemInfo> ass = new ArrayList<FilterItemInfo>(flexProp.size());

        for (Map.Entry<String, Object> entry2 : flexProp.entrySet()) {
            Set<Object> vals = new HashSet<Object>();
            ass.add(new FilterItemInfo((String)entry2.getKey(), vals, "in"));
        }

        String curStr;
        if (this.curId != null && this.curId != 0L) {
            curStr = String.valueOf(this.curId);
        } else {
            curStr = info.getString("currency");
        }

        List<Long> acctIdList = new ArrayList<Long>();
        for (String id : this.acctIds.split(",")){
            acctIdList.add(Long.valueOf(id));
        }

        filter.addFilterItem("org", org);
        filter.addFilterItem("booktype", bookType);
        filter.addFilterItem("periodtype", periodType);
        filter.addFilterItem("startperiod", startPeriod);
        filter.addFilterItem("endperiod", endPeriod);
        filter.addFilterItem("accounttable", info.getLong("accounttable"));
        filter.addFilterItem("account", acctIdList, QCP.in);
        filter.addFilterItem("accountlevel", info.getString("accountlevel"));
        filter.addFilterItem("currency", curStr);
        filter.addFilterItem("measureunits", this.measureunitId);
        filter.addFilterItem("showqty", info.getBoolean("showqty"));
        filter.addFilterItem("showleafaccount", info.getBoolean("showleafaccount"));
        filter.addFilterItem("nodisplayforzero", info.getBoolean("nodisplayforzero"));
        filter.addFilterItem("balancezero", info.getBoolean("balancezero"));

        if (info.containProp(KEY_FIELD_ISSUB)) {
            filter.addFilterItem(KEY_FIELD_ISSUB, info.getBoolean(KEY_FIELD_ISSUB));
        }

        filter.setFlexFilterItems(ass);
        ReportQueryParam queryParam = new ReportQueryParam();
        queryParam.setFilter(filter);
        report.setQueryParam(queryParam);
        this.view.showForm(report);
    }

    protected NavToAssRptExt(NavToAssRptExt.Builder builder) {
        this.param = builder.param;
        this.view = builder.view;
        this.acctIds = builder.acctIds;
        this.orgId = builder.orgId;
        this.curId = builder.curId;
        this.measureunitId = builder.measureunitId;
        this.assgrpIds = builder.assgrpIds;
        this.navKey = builder.navKey;
    }

    public static class Builder {
        private ReportQueryParam param;
        private String navKey;
        private IFormView view;
        private String acctIds;
        private Long orgId;
        private Long curId;
        private Long measureunitId;
        private String assgrpIds;

        public Builder(ReportQueryParam param, IFormView view) {
            this.param = param;
            this.view = view;
        }

        public NavToAssRptExt.Builder acctIds(String acctIds) {
            this.acctIds = acctIds;
            return this;
        }

        public NavToAssRptExt.Builder navKey(String navKey) {
            this.navKey = navKey;
            return this;
        }

        public NavToAssRptExt.Builder currencyId(Long curId) {
            this.curId = curId;
            return this;
        }

        public NavToAssRptExt.Builder orgId(Long orgId) {
            this.orgId = orgId;
            return this;
        }

        public NavToAssRptExt.Builder measureunitId(Long measureunitId) {
            this.measureunitId = measureunitId;
            return this;
        }

        public NavToAssRptExt.Builder assgrpIds(String assgrpIds) {
            this.assgrpIds = assgrpIds;
            return this;
        }

        public NavToAssRptExt build() {
            return new NavToAssRptExt(this);
        }
    }
}
