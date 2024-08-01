package nckd.yanye.hr.plugin.form.baoxiaojibie;

import java.util.*;
import java.util.stream.Collectors;

import kd.bd.assistant.er.util.ErReimburseSettingUtil;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.property.UserProp;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;

/**
 * @author husheng
 * @date 2024-07-31 10:43
 * @description 出差申请单-表单插件-自动获取报销级别
 */
public class ErTripreqbillExtFormPlugin extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

        // 申请人
        DynamicObject applier = (DynamicObject) this.getModel().getValue("applier");
        // 公司
        DynamicObject company = (DynamicObject) this.getModel().getValue("company");

        // 获取报销级别设置信息
        QFilter qFilter = new QFilter("number", QCP.equals, applier.get("number"));
        DynamicObject reimbursesettings = BusinessDataServiceHelper.loadSingle("er_reimbursesetting", qFilter.toArray());

        // 报销级别
        this.getModel().setValue("nckd_reimburselevel_bill", this.getReimburse(reimbursesettings,company));
    }

    public Object getReimburse(DynamicObject reimbursesettings,DynamicObject company) {
        Object reimburseLevel = null;
        DynamicObjectCollection orgEntrys = reimbursesettings.getDynamicObjectCollection("entryentity");
        if (!orgEntrys.isEmpty()) {
            LinkedList<ErTripreqbillExtFormPlugin.OrgInfo> orgs = this.getOrgs(orgEntrys);
            List<ErTripreqbillExtFormPlugin.OrgInfo> companys = this.getCompanys(orgs);
            if (!companys.isEmpty()) {
                Map<Object, Object> reimburseLevelMap = ErReimburseSettingUtil.getReimburseLevel((Long) reimbursesettings.getPkValue(), (List) companys.stream().map(ErTripreqbillExtFormPlugin.OrgInfo::getOrgId).collect(Collectors.toList()));
                Iterator<ErTripreqbillExtFormPlugin.OrgInfo> companyIter = companys.iterator();

                for(int var10 = 0; var10 < companys.size(); ++var10) {
                    ErTripreqbillExtFormPlugin.OrgInfo next = companyIter.next();
                    if(Objects.equals(next.getOrgId(),company.getPkValue())){
                        reimburseLevel = reimburseLevelMap.get(next.getOrgId());
                    }
                }
            }
        }
        return reimburseLevel;
    }

    private List<ErTripreqbillExtFormPlugin.OrgInfo> getCompanys(LinkedList<ErTripreqbillExtFormPlugin.OrgInfo> orgs) {
        List<ErTripreqbillExtFormPlugin.OrgInfo> companys = new ArrayList(orgs.size());
        orgs.forEach((org) -> {
            Map<String, Object> companyByOrg = OrgUnitServiceHelper.getCompanyfromOrg(org.getOrgId());
            Long companyId = (Long) companyByOrg.getOrDefault("id", 0L);
            ErTripreqbillExtFormPlugin.OrgInfo orgInfo = new ErTripreqbillExtFormPlugin.OrgInfo(companyId, org.ispartjob());
            if (!companys.contains(orgInfo)) {
                companys.add(orgInfo);
            }

        });
        return companys;
    }

    private LinkedList<ErTripreqbillExtFormPlugin.OrgInfo> getOrgs(DynamicObjectCollection orgEntrys) {
        LinkedList<ErTripreqbillExtFormPlugin.OrgInfo> orgs = new LinkedList();
        orgEntrys.forEach((entry) -> {
            boolean ispartJob = entry.getBoolean("ispartjob");
            ErTripreqbillExtFormPlugin.OrgInfo info = new ErTripreqbillExtFormPlugin.OrgInfo(entry.getLong("dpt.id"), ispartJob);
            if (!ispartJob) {
                orgs.addFirst(info);
            } else {
                orgs.add(info);
            }

        });
        return orgs;
    }

    private static class OrgInfo {
        private Long orgId;
        private boolean partjob;

        public OrgInfo(Long orgId, boolean ispartjob) {
            this.orgId = orgId;
            this.partjob = ispartjob;
        }

        public Long getOrgId() {
            return this.orgId;
        }

        public boolean ispartjob() {
            return this.partjob;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                return this.getClass() != obj.getClass() ? false : ((ErTripreqbillExtFormPlugin.OrgInfo) obj).getOrgId().equals(this.getOrgId());
            }
        }

        public int hashCode() {
            return this.getOrgId().intValue();
        }
    }
}
