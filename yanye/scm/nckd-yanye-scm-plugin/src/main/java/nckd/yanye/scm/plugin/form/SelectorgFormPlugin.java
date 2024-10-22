package nckd.yanye.scm.plugin.form;

import java.util.*;
import java.util.stream.Collectors;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.exception.KDBizException;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

/**
 * @author husheng
 * @date 2024-09-25 14:33
 * @description 选择组织（nckd_selectorg）
 */
public class SelectorgFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        BasedataEdit orgEdit = this.getControl("nckd_org");
        orgEdit.addBeforeF7SelectListener(this);
        BasedataEdit departmentEdit = this.getControl("nckd_department");
        departmentEdit.addBeforeF7SelectListener(this);

        this.addClickListeners("btnok");
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

        // 当前用户所属部门
        long userMainOrgId = UserServiceHelper.getUserMainOrgId(UserServiceHelper.getCurrentUserId());
        // 申请部门
        this.getModel().setValue("nckd_department", userMainOrgId);

        // 物料分录
        FormShowParameter showParameter = this.getView().getFormShowParameter();
        Map<String, Object> customParams = showParameter.getCustomParams();
        List<String> materialnumberList = (List<String>) customParams.get("materialnumberList");
        if (materialnumberList != null) {
            QFilter qFilter = new QFilter("number", QCP.in, materialnumberList);
            DynamicObject[] objects = BusinessDataServiceHelper.load("bd_material", "id,number,name,modelnum,nckd_model", qFilter.toArray());

            DynamicObjectCollection entryentity = this.getModel().getEntryEntity("nckd_entryentity");

            Arrays.stream(objects).forEach(t -> {
                DynamicObject dynamicObject = entryentity.addNew();
                dynamicObject.set("nckd_material", t);
            });
        }
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);

        Control source = (Control) evt.getSource();
        String key = source.getKey();
        if (key.equals("btnok")) {
            Map<String, Object> map = new HashMap<>();
            DynamicObject org = (DynamicObject) this.getModel().getValue("nckd_org");
            DynamicObject department = (DynamicObject) this.getModel().getValue("nckd_department");
            DynamicObjectCollection entryentity = this.getModel().getEntryEntity("nckd_entryentity");
            if (org != null && department != null && entryentity.size() > 0) {
//                FormShowParameter showParameter = this.getView().getFormShowParameter();
//                Map<String, Object> customParams = showParameter.getCustomParams();
//                List<Long> orgIds = (List<Long>) customParams.get("orgIds");
//                if (orgIds.contains(org.getLong("id"))) {
//                    throw new KDBizException("属性申请组织重复!");
//                }

//                List<String> materialnumberList = (List<String>) customParams.get("materialnumberList");

                List<Long> materialIdList = entryentity.stream().map(t -> t.getDynamicObject("nckd_material").getLong("id")).collect(Collectors.toList());

                QFilter qFilter = new QFilter("org", QCP.equals, org.getLong("id"))
                        .and("nckd_materialnumber", QCP.in, materialIdList);
                boolean exists = QueryServiceHelper.exists("nckd_materialmaintenan", qFilter.toArray());
                if (exists) {
                    throw new KDBizException("属性申请组织对应的物料存在物料维护单!");
                }

                // 物料采购信息
                QFilter qFilter1 = new QFilter("masterid", QCP.in, materialIdList).and("createorg", QCP.equals, org.getLong("id"));
                boolean exists1 = QueryServiceHelper.exists("bd_materialpurchaseinfo", qFilter1.toArray());
                // 物料采购员信息
                QFilter qFilter2 = new QFilter("entryentity.material", QCP.in, materialIdList).and("org", QCP.equals, org.getLong("id"));
                boolean exists2 = QueryServiceHelper.exists("msbd_puropermaterctrl", qFilter2.toArray());
                // 库存基本信息
                QFilter qFilter3 = new QFilter("masterid", QCP.in, materialIdList).and("createorg", QCP.equals, org.getLong("id"));
                boolean exists3 = QueryServiceHelper.exists("bd_materialinventoryinfo", qFilter3.toArray());
                // 销售基本信息
                QFilter qFilter4 = new QFilter("masterid", QCP.in, materialIdList).and("createorg", QCP.equals, org.getLong("id"));
                boolean exists4 = QueryServiceHelper.exists("bd_materialsalinfo", qFilter4.toArray());
                // 生产基本信息
                QFilter qFilter5 = new QFilter("masterid", QCP.in, materialIdList).and("createorg", QCP.equals, org.getLong("id"));
                boolean exists5 = QueryServiceHelper.exists("bd_materialmftinfo", qFilter5.toArray());
                // 核算基本信息
                QFilter qFilter6 = new QFilter("masterid", QCP.in, materialIdList).and("createorg", QCP.equals, org.getLong("id"));
                boolean exists6 = QueryServiceHelper.exists("bd_materialcalinfo", qFilter6.toArray());
                // 计划基本信息
                QFilter qFilter7 = new QFilter("masterid", QCP.in, materialIdList).and("createorg", QCP.equals, org.getLong("id"));
                boolean exists7 = QueryServiceHelper.exists("mpdm_materialplan", qFilter7.toArray());
                // 质检基本信息
                QFilter qFilter8 = new QFilter("masterid", QCP.in, materialIdList).and("createorg", QCP.equals, org.getLong("id"));
                boolean exists8 = QueryServiceHelper.exists("bd_inspect_cfg", qFilter8.toArray());

                if (exists1 || exists2 || exists3 || exists4 || exists5 || exists6 || exists7 || exists8) {
                    throw new KDBizException("属性申请组织对应的物料存在属性信息!");
                }

                map.put("orgId", org.getLong("id"));
                map.put("department", department);
                map.put("entryentity", entryentity);
                //返回数据
                this.getView().returnDataToParent(map);
                this.getView().close();
            }
        }
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        ListShowParameter showParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();
        if (name.equals("nckd_org")) {
//            QFilter filter = new QFilter("orgpattern.number", QCP.in, new String[]{"Orgform01", "Orgform01-100", "Orgform02", "Orgform03"});
//            showParameter.getListFilterParameter().setFilter(filter);
        } else if (name.equals("nckd_department")) {
            DynamicObject org = (DynamicObject) this.getModel().getValue("nckd_org");
            if (org == null) {
                throw new KDBizException("请先选择属性申请组织");
            }

            List<Long> longs = new ArrayList<>();
            longs.add(org.getLong("id"));
            List<Long> allSubordinateOrgIds = OrgUnitServiceHelper.getAllSubordinateOrgs("01", longs, true);

            QFilter qFilter = new QFilter("belongcompany", QCP.in, allSubordinateOrgIds);
            showParameter.getListFilterParameter().setFilter(qFilter);
        }
    }
}
