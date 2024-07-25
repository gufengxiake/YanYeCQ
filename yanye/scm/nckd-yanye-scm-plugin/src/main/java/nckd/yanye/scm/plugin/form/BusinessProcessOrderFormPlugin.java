package nckd.yanye.scm.plugin.form;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.*;
/**
 * Module           :制造云-生产任务管理-生产工单（新）
 * Description      :物料-业务处理对应单单据插件
 *
 * @author : yaosijie
 * @date : 2024/7/23
 */
public class BusinessProcessOrderFormPlugin extends AbstractFormPlugin implements BeforeF7SelectListener {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //注册基础资料点击前的监听
        BasedataEdit materielfieldEdit = this.getView().getControl("nckd_materielfield");
        materielfieldEdit.addBeforeF7SelectListener(this);
        BasedataEdit warehouseEdit = this.getView().getControl("nckd_warehouse");
        warehouseEdit.addBeforeF7SelectListener(this);
        BasedataEdit mainproduceEdit = this.getView().getControl("nckd_mainproduce");
        mainproduceEdit.addBeforeF7SelectListener(this);
        BasedataEdit useworkshoptEdit = this.getView().getControl("nckd_useworkshop");
        useworkshoptEdit.addBeforeF7SelectListener(this);
        BasedataEdit wareorderworkshopEdit = this.getView().getControl("nckd_wareorderworkshop");
        wareorderworkshopEdit.addBeforeF7SelectListener(this);


    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        List<QFilter> qFilters = new ArrayList<>();
        if (name.equals("nckd_materielfield")){
            //构造物料库存信息查询条件（"1" 表示物料允许负库存）
            QFilter qFilter = new QFilter("isallowneginv", QCP.equals, "1");
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("bd_materialinventoryinfo", "id,masterid", new QFilter[]{qFilter});
            Set<Long> mates = new HashSet<>();
            Arrays.stream(dynamicObjects).forEach(t->{
                DynamicObject dynamicObject = t.getDynamicObject("masterid");
                mates.add(dynamicObject.getLong("masterid"));
            });
            //构造物料信息查询条件
            QFilter MetesqFilter = new QFilter("id", QCP.in, mates);
            qFilters.add(MetesqFilter);
        }else if (name.equals("nckd_warehouse")){
            DynamicObjectCollection collection = this.getModel().getEntryEntity("nckd_bussinessentries");
            EntryGrid entryGrid = this.getView().getControl("nckd_bussinessentries");
            int[] rows = entryGrid.getSelectRows();
            DynamicObject dynamicObject = collection.get(rows[0]);
            Long orgId = dynamicObject.getLong("nckd_inventoryorg.masterid");
            //构造仓库查询条件
            QFilter qFilter = new QFilter("org", QCP.equals, orgId);
            qFilters.add(qFilter);
        }else if (name.equals("nckd_mainproduce")){
            DynamicObjectCollection collection = this.getModel().getEntryEntity("nckd_bussinessentries");
            EntryGrid entryGrid = this.getView().getControl("nckd_bussinessentries");
            int[] rows = entryGrid.getSelectRows();
            DynamicObject dynamicObject = collection.get(rows[0]);
            String number = dynamicObject.getString("nckd_materielfield.number");
            //构造物料生产信息查询条件("10030" 表示自制件)
            QFilter qFilter = new QFilter("materialattr", QCP.equals, "10030");
            QFilter mateqFilter = new QFilter("masterid.number", QCP.not_equals, number);
            qFilters.add(qFilter);
            qFilters.add(mateqFilter);
        }else if (name.equals("nckd_useworkshop") || name.equals("nckd_wareorderworkshop")){
            Set<Long> orgIds = new HashSet<>();
            DynamicObject dynamicObject = (DynamicObject)this.getModel().getValue("org");
            Long orgId = dynamicObject.getLong("id");
            //构造业务单元分配部门查询条件
            QFilter qFilter = new QFilter("fromorg", QCP.equals, orgId);
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("bos_org_orgrelation_dept", "id,toorg", new QFilter[]{qFilter});
            Arrays.asList(dynamicObjects).forEach(t->{
                orgIds.add(t.getLong("toorg.masterid"));
            });
            //构造行政组织查询条件
            QFilter orgqFilter = new QFilter("id", QCP.in, orgIds);
            qFilters.add(orgqFilter);
        }
        beforeF7SelectEvent.setCustomQFilters(qFilters);
    }
}
