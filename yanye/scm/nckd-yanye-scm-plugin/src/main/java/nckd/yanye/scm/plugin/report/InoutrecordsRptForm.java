package nckd.yanye.scm.plugin.report;

import java.util.*;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicProperty;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.GroupProp;
import kd.bos.entity.property.ParentBasedataProp;
import kd.bos.form.IFormView;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.DispatchServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.fi.cal.common.helper.OrgHelper;
import kd.fi.cal.common.helper.PermissionHelper;
import kd.fi.cal.common.helper.ReportF7Helper;

/**
 * @author husheng
 * @date 2024-09-03 14:54
 * @description 出入库流水账（nckd_inoutrecords）报表界面插件
 */
public class InoutrecordsRptForm extends AbstractReportFormPlugin implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addF7Listener(this, "nckd_mulcalorg", "nckd_mulcostaccount", "nckd_mulmaterialgroup", "nckd_mulmaterial", "nckd_materialto","nckd_mulbilltype");
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
        model.setValue("nckd_mulmaterial",null);
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
//        DynamicObjectCollection costaccounts = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_mulcostaccount");
//        if (costaccounts != null && costaccounts.size() != 0) {
//            Set<Long> costaccountIdSet = new HashSet();
//            Iterator var3 = costaccounts.iterator();
//
//            while(var3.hasNext()) {
//                DynamicObject costaccount = (DynamicObject)var3.next();
//                costaccountIdSet.add(costaccount.getDynamicObject("fbasedataid").getLong("id"));
//            }
//
//            Map<Long, DynamicObject> periods = PeriodHelper.getCurrentPeriods(costaccountIdSet);
//            Set<Long> periodIds = new HashSet();
//            Iterator var5 = costaccountIdSet.iterator();
//
//            Long minid;
//            while(var5.hasNext()) {
//                minid = (Long)var5.next();
//                DynamicObject period = (DynamicObject)periods.get(minid);
//                if (period != null) {
//                    periodIds.add(period.getLong("id"));
//                }
//            }
//
//            if (periodIds.isEmpty()) {
//                this.getModel().setValue("startperiod", (Object)null);
//                this.getModel().setValue("endperiod", (Object)null);
//            } else {
//                Long maxid = (Long)Collections.max(periodIds);
//                minid = (Long)Collections.min(periodIds);
//                this.getModel().setValue("startperiod", periodIds.size() == 0 ? null : minid);
//                this.getModel().setValue("endperiod", periodIds.size() == 0 ? null : maxid);
//            }
//        } else {
//            this.getModel().setValue("startperiod", (Object)null);
//            this.getModel().setValue("endperiod", (Object)null);
//        }
    }

    private void calOrgChanged() {
        IDataModel model = this.getModel();
//        model.setValue("mulstorageorg", (Object)null);
//        model.setValue("mulowner", (Object)null);
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
        } else if ("nckd_mulbilltype".equals(key)){
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
        DynamicObjectCollection materialgroupCol = (DynamicObjectCollection)model.getValue("nckd_mulmaterialgroup");
        if (materialgroupCol != null && !materialgroupCol.isEmpty()) {
            QFilter matFilter = QFilter.of("1 = 1", new Object[0]);
            DynamicObjectCollection matgroupdetailCol = QueryServiceHelper.query("bd_materialgroupdetail", "material.id", new QFilter[]{this.getGroupFilter(materialgroupCol, false, "group.longnumber"), matFilter});
            List<Object> materialIds = new ArrayList(4096);
            Iterator var6 = matgroupdetailCol.iterator();

            while(var6.hasNext()) {
                DynamicObject matgroupdetail = (DynamicObject)var6.next();
                materialIds.add(matgroupdetail.getLong("material.id"));
            }

            ListShowParameter formShowParameter = (ListShowParameter)e.getFormShowParameter();
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
            GroupProp group = (GroupProp)property;
            String longNumberDLM = "";
            if (group instanceof ParentBasedataProp) {
                longNumberDLM = ((ParentBasedataProp)group).getLongNumberDLM();
            }

            QFilter groupFilter = QFilter.of("1 != 1", new Object[0]);
            Set<String> groupNumSet = new HashSet(16);
            Iterator var8 = materialgroupColl.iterator();

            while(var8.hasNext()) {
                DynamicObject matgroup = (DynamicObject)var8.next();
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

        DynamicObjectCollection calOrgList = (DynamicObjectCollection)this.getModel().getValue("nckd_mulcalorg");
        List<Long> orgIds = new ArrayList(1);
        Iterator var8 = calOrgList.iterator();

        while(var8.hasNext()) {
            DynamicObject calOrg = (DynamicObject)var8.next();
            orgIds.add(calOrg.getDynamicObject("fbasedataid").getLong("id"));
        }

        QFilter serviceResponse = DispatchServiceHelper.invokeBizService("bd", "bd", "IMasterDataStandardService", "getGroupByOrgs", new Object[]{"bd_material", orgIds, matgroupstandardId, Boolean.FALSE});
        ((ListShowParameter)e.getFormShowParameter()).getListFilterParameter().getQFilters().add(qFilter.and(serviceResponse));
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
