package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.DB;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.TreeEntryGrid;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.control.events.RowClickEvent;
import kd.bos.form.control.events.RowClickEventListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


public class ProductionPlanFromPlugin extends AbstractBillPlugIn implements RowClickEventListener {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("tbmain", "nckd_advcontoolbarap");
        Toolbar toolbar = this.getControl("tbmain");
        toolbar.addItemClickListener(this);
        //监听单据体
        EntryGrid grid = this.getControl("pom_planning_entry");
        grid.addRowClickListener(this);
    }

    @Override
    public void entryRowClick(RowClickEvent evt) {
        RowClickEventListener.super.entryRowClick(evt);
        int row = evt.getRow();
        if (row < 0) {
            return;
        }
        DynamicObject data = this.getModel().getDataEntity();
        String number = data.getString("billno");
        if (number == null) {
            return;
        }
        DynamicObject entry = this.getModel().getEntryEntity("pom_planning_entry").get(row);
        DynamicObject material = entry.getDynamicObject("material");
        if (material == null) {
            return;
        }

        DynamicObject materialInfo = BusinessDataServiceHelper.loadSingle(material.getPkValue(), "bd_materialmftinfo");
        if (!"10030".equals(materialInfo.get("materialattr"))) {
            return;
        }
        DynamicObject[] mft = BusinessDataServiceHelper.load("pom_mftorder", "id,treeentryentity,treeentryentity.material,treeentryentity.producedept,treeentryentity.qty,treeentryentity.unit",
                new QFilter[]{new QFilter("billno", QCP.equals, number)});
        if (mft == null || mft.length <= 0) {
            return;
        }
        DynamicObjectCollection subentryentity = entry.getDynamicObjectCollection("nckd_subentryentity");
        if (subentryentity.size() > 0) {
            subentryentity.clear();
        }
        for (DynamicObject d : mft) {
            DynamicObjectCollection treeentryentity = d.getDynamicObjectCollection("treeentryentity");
            for (int i = 0; i < treeentryentity.size(); i++) {
                DynamicObject dyt = treeentryentity.get(i);
                if (material.getPkValue().equals(dyt.getDynamicObject("material").getPkValue())) {
                    subentryentity = entry.getDynamicObjectCollection("nckd_subentryentity");
                    DynamicObject entity = subentryentity.addNew();
                    entity.set("nckd_material", material);
                    entity.set("producedept", dyt.getDynamicObject("producedept"));
                    entity.set("yiel", dyt.get("qty"));
                    entity.set("unit", dyt.getDynamicObject("unit"));
                }
            }
        }
        SaveServiceHelper.save(new DynamicObject[]{data});
        this.getView().updateView("nckd_subentryentity");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        if (StringUtils.equals(itemKey, "tb_new")) {
            Object data = this.getModel().getValue("nckd_plan_month");
            if (data != null) {
                EntryGrid treeEntryEntity = this.getControl("pom_planning_entry");
                int[] rows = treeEntryEntity.getSelectRows();
                if (rows != null && rows.length > 0) {
                    for (int row : rows) {
                        this.getModel().setValue("nckd_planstarttime", data, row);
                        this.getModel().setValue("nckd_planendtime", data, row);
                    }
                }
            }
        }
        if (StringUtils.equals(itemKey, "nckd_baritemap")) {
            DynamicObjectCollection entity = this.getModel().getEntryEntity("pom_planning_entry");
            if (entity.size() > 0) {
                this.getModel().setValue("nckd_materiel", entity.get(0).getDynamicObject("material"));
            }
        }
        if (StringUtils.equals(itemKey, "nckd_bar")) {
            setPlanToMft();
        }
    }


    private void setPlanToMft() {
        DynamicObject dataEntity = this.getModel().getDataEntity();
        DynamicObjectCollection pomPlanningEntryColl = this.getModel().getEntryEntity("pom_planning_entry");

        String materialId = "";
        String depId = "";
        List<DynamicObject> aList = new ArrayList<>();
        for (int i = 0; i < pomPlanningEntryColl.size(); i++) {
            DynamicObject pom = pomPlanningEntryColl.get(i);
            if ("C".equals(pom.getString("nckd_producttype"))) {
                materialId = pom.getDynamicObject("material").getPkValue().toString();
                depId = pom.getDynamicObject("nckd_producedept").getPkValue().toString();
                aList.add(pom);
                for (DynamicObject d : pomPlanningEntryColl) {
                    if ("C".equals(d.getString("nckd_producttype"))) {
                        if (!d.getPkValue().equals(pom.getPkValue())){
                            if (!(d.getDynamicObject("material").getPkValue().toString() + d.getDynamicObject("nckd_producedept").getPkValue().toString()).equals(materialId + depId)){
                                aList.add(d);
                            }
                        }
                    }

                }
                break;
            }
        }
        if (aList.size() <= 1){
            DynamicObject pomMftorder = BusinessDataServiceHelper.newDynamicObject("pom_mftorder");
            DynamicObjectCollection treeentryentity = pomMftorder.getDynamicObjectCollection("treeentryentity");//拿到生产工单树形单据体

            pomMftorder.set("billno", dataEntity.get("billno"));
            pomMftorder.set("org", dataEntity.getDynamicObject("org"));
            pomMftorder.set("transactiontype", dataEntity.getDynamicObject("nckd_transactiontype"));
            pomMftorder.set("billdate", new Date());
            pomMftorder.set("billstatus", "B");
            //根据单据体父子关系分组
            List<List<DynamicObject>> lists = new ArrayList<>();
            for (DynamicObject dynamicObject : pomPlanningEntryColl) {
                if ("C".equals(dynamicObject.getString("nckd_producttype"))) {
                    List<DynamicObject> cList = new ArrayList<>();
                    cList.add(dynamicObject);
                    for (DynamicObject object : pomPlanningEntryColl) {
                        if (dynamicObject.getPkValue().equals(object.get("pid"))) {
                            cList.add(object);
                        }
                    }
                    lists.add(cList);
                }
            }
            if (lists.size() == 1) {
                long id = DB.genLongId("t_pom_mftorderentry");
                for (int i = 0; i < lists.get(0).size(); i++) {
                    DynamicObject pomPlanningEntry = lists.get(0).get(i);
                    //Object materlal = pomPlanningEntry.getDynamicObject("material").get("masterid");
                    //DynamicObject materlalDym = BusinessDataServiceHelper.loadSingle("bd_material", "id", new QFilter[]{new QFilter("number", QCP.equals, materlal)});
                    if ("C".equals(pomPlanningEntry.get("nckd_producttype"))) {
                        //一顿赋值操作
                        DynamicObject newOne = treeentryentity.addNew();
                        newOne.set("id", id);//随机生成一个long类型的id
                        newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));
                        newOne.set("material", pomPlanningEntry.getDynamicObject("material"));//物料
                        newOne.set("materielmasterid", pomPlanningEntry.getDynamicObject("material").get("masterid"));//物料
                        newOne.set("producedept", pomPlanningEntry.getDynamicObject("nckd_producedept"));
                        newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                        newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                        newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                        newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                        newOne.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                        pomMftorder.set("remark", pomMftorder.get("remark") == null ? pomPlanningEntry.getString("nckd_remark") : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                    }
                    if (i > 0) {
                        //这个是本次副产品对应上一条主产品
                        DynamicObject newOne = treeentryentity.addNew();
                        newOne.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                        newOne.set("pid", id);//添加父id  建立父子关系
                        newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));//物料
                        newOne.set("materielmasterid", pomPlanningEntry.getDynamicObject("material").get("masterid"));//物料
                        newOne.set("material", pomPlanningEntry.getDynamicObject("material"));
                        newOne.set("producedept", pomPlanningEntry.getDynamicObject("nckd_producedept"));
                        newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                        newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                        newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                        newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                        pomMftorder.set("remark", pomMftorder.get("remark") == null ? pomPlanningEntry.getString("nckd_remark")
                                : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                    }
                }
                SaveServiceHelper.save(new DynamicObject[]{pomMftorder});
            }
            //根据物料＋生产部门进行拆单
            if (lists.size() > 1) {
                for (List<DynamicObject> list : lists) {
                    long id = 0;
                    for (int i = 0; i < list.size(); i++) {
                        DynamicObject pomPlanningEntry = list.get(i);
                        //Object materlal = pomPlanningEntry.getDynamicObject("material").get("masterid");
                        //DynamicObject materlalDym = BusinessDataServiceHelper.loadSingle("bd_material", "id", new QFilter[]{new QFilter("number", QCP.equals, materlal)});
                        if ("C".equals(pomPlanningEntry.get("nckd_producttype"))) {
                            //一顿赋值操作
                            id = DB.genLongId("t_pom_mftorderentry");
                            DynamicObject newOne = treeentryentity.addNew();
                            newOne.set("id", id);//随机生成一个long类型的id
                            newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));
                            newOne.set("material", pomPlanningEntry.getDynamicObject("material"));//物料
                            newOne.set("materielmasterid", pomPlanningEntry.getDynamicObject("material").get("masterid"));//物料
                            newOne.set("producedept", pomPlanningEntry.getDynamicObject("nckd_producedept"));
                            newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                            newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                            newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                            newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                            newOne.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                            pomMftorder.set("remark", pomMftorder.get("remark") == null ?
                                    pomPlanningEntry.getString("nckd_remark") : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                        }
                        if (i > 0) {
                            DynamicObject newTwo = treeentryentity.addNew();
                            newTwo.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                            newTwo.set("pid", id);//添加父id  建立父子关系
                            newTwo.set("producttype", pomPlanningEntry.getString("nckd_producttype"));//物料
                            newTwo.set("materielmasterid", pomPlanningEntry.getDynamicObject("material").get("masterid"));//物料
                            newTwo.set("material", pomPlanningEntry.getDynamicObject("material"));
                            newTwo.set("producedept", pomPlanningEntry.getDynamicObject("nckd_producedept"));
                            newTwo.set("qty", pomPlanningEntry.get("nckd_yield"));
                            newTwo.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                            newTwo.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                            newTwo.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                            pomMftorder.set("remark", pomMftorder.get("remark") == null ? pomPlanningEntry.getString("nckd_remark") :
                                    pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                        }
                    }
                }
                SaveServiceHelper.save(new DynamicObject[]{pomMftorder});
            }

        }
        if (aList.size() > 1){
            long id = 0;
            for (int i = 0; i < aList.size(); i++) {
                DynamicObject pomMftorder = BusinessDataServiceHelper.newDynamicObject("pom_mftorder");
                DynamicObjectCollection treeentryentity = pomMftorder.getDynamicObjectCollection("treeentryentity");//拿到生产工单树形单据体
                pomMftorder.set("billno", dataEntity.get("billno"));
                pomMftorder.set("org", dataEntity.getDynamicObject("org"));
                pomMftorder.set("transactiontype", dataEntity.getDynamicObject("nckd_transactiontype"));
                pomMftorder.set("billdate", new Date());
                pomMftorder.set("billstatus", "B");
                DynamicObject pomPlanningEntry =  aList.get(i);
                id = DB.genLongId("t_pom_mftorderentry");
                DynamicObject newOne = treeentryentity.addNew();
                newOne.set("id", id);//随机生成一个long类型的id
                newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));
                newOne.set("material", pomPlanningEntry.getDynamicObject("material"));//物料
                newOne.set("materielmasterid", pomPlanningEntry.getDynamicObject("material").get("masterid"));//物料
                newOne.set("producedept", pomPlanningEntry.getDynamicObject("nckd_producedept"));
                newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                newOne.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                pomMftorder.set("remark", pomMftorder.get("remark") == null ?
                        pomPlanningEntry.getString("nckd_remark") : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                for (DynamicObject d : pomPlanningEntryColl) {
                    if (pomPlanningEntry.getPkValue().equals(d.get("pid"))){
                        DynamicObject newTwo = treeentryentity.addNew();
                        newTwo.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                        newTwo.set("pid", id);//添加父id  建立父子关系
                        newTwo.set("producttype", d.getString("nckd_producttype"));//物料
                        newTwo.set("materielmasterid", d.getDynamicObject("material").get("masterid"));//物料
                        newTwo.set("material", d.getDynamicObject("material"));
                        newTwo.set("producedept", d.getDynamicObject("nckd_producedept"));
                        newTwo.set("qty", d.get("nckd_yield"));
                        newTwo.set("unit", d.getDynamicObject("nckd_unit"));
                        newTwo.set("bomid", d.getDynamicObject("nckd_bomid"));
                        newTwo.set("planbegintime", d.get("nckd_planstarttime"));
                        pomMftorder.set("remark", pomMftorder.get("remark") == null ? d.getString("nckd_remark") :
                                d.getString("nckd_remark") + pomMftorder.getString("remark"));
                    }
                }
                SaveServiceHelper.save(new DynamicObject[]{pomMftorder});
            }
        }


    }


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        //物料
        if (StringUtils.equals("material", fieldKey)) {
            setMaterialToBOM();
        }
        //计划数量
        if (StringUtils.equals("nckd_yield", fieldKey)) {
            setYield();
        }
        //计划月份
        if (StringUtils.equals("nckd_plan_month", fieldKey)) {
            setPlanMonth();
        }
    }

    private void setPlanMonth() {
        Object data = this.getModel().getValue("nckd_plan_month");
        if (data == null) {
            return;
        }
        int rowCount = this.getModel().getEntryRowCount("pom_planning_entry");
        if (rowCount > 0) {
            for (int i = 0; i < rowCount; i++) {
                this.getModel().setValue("nckd_planstarttime", data, i);
                this.getModel().setValue("nckd_planendtime", data, i);
                this.getView().updateView();
            }
        }
    }

    private void setYield() {
        int rowIndex = this.getModel().getEntryCurrentRowIndex("pom_planning_entry");
        DynamicObjectCollection entity = this.getModel().getEntryEntity("pom_planning_entry");
        DynamicObject entry = entity.get(rowIndex);
        String nckdProducttype = entry.getString("nckd_producttype");
        if ("A".equals(nckdProducttype) || "B".equals(nckdProducttype)) {
            return;
        }
        BigDecimal nckdYield = entry.getBigDecimal("nckd_yield");
        for (int i = 0; i < entity.size(); i++) {
            if (entry.getPkValue().equals(entity.get(i).get("pid"))){
                DynamicObject nckdBomid = entry.getDynamicObject("nckd_bomid");
                DynamicObject nckdBomid_a = entity.get(i).getDynamicObject("nckd_bomid");
                String type = entity.get(i).getString("nckd_producttype");
                if (nckdYield != null && nckdBomid_a != null) {
                    if (("A".equals(type) || "B".equals(type)) && nckdBomid.getPkValue().equals(nckdBomid_a.getPkValue())) {
                        nckdBomid_a = BusinessDataServiceHelper.loadSingle(nckdBomid_a.getPkValue(), "pdm_mftbom");
                        DynamicObjectCollection copentry = nckdBomid_a.getDynamicObjectCollection("copentry");
                        if (copentry.size() > 0) {
                            BigDecimal copentryqty = copentry.get(0).getBigDecimal("copentryqty");
                            this.getModel().setValue("nckd_yield", nckdYield.multiply(copentryqty), i);
                            this.getView().updateView();
                        }
                    }
                }
            }
        }
    }

    private void setMaterialToBOM() {
        TreeEntryGrid grid = this.getView().getControl("pom_planning_entry");
        DynamicObjectCollection entrys = this.getModel().getEntryEntity("pom_planning_entry");
        int rowIndex = this.getModel().getEntryCurrentRowIndex("pom_planning_entry");
        DynamicObject entry = entrys.get(rowIndex);
        //entry = entrys.get(selectRow);
        DynamicObject material = entry.getDynamicObject("material");
        if (material != null) {
            //查物料
            material = BusinessDataServiceHelper.loadSingle(material.getPkValue(), "bd_materialmftinfo");
            this.getModel().setValue("nckd_producedept", material.getDynamicObject("createorg"));
            this.getModel().setValue("nckd_unit", material.getDynamicObject("mftunit"), rowIndex);
            //根据物料查bom
            DynamicObject mftbom = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material,copentry,copentry.copentrymaterial,copentry.copentryunit,copentry.copentrytype",
                    new QFilter[]{new QFilter("material.id", QCP.equals, material.getPkValue())});
            if (mftbom != null) {
                this.getModel().setValue("nckd_bomid", mftbom.getPkValue(), rowIndex);

                DynamicObjectCollection copentrys = mftbom.getDynamicObjectCollection("copentry");
                if (copentrys.size() <= 0) {
                    if (entrys.size() > 1) {
                        for (int i = 0; i < entrys.size(); i++) {
                            if (entry.getPkValue().equals(entrys.get(i).get("pid"))) {
                                this.getModel().deleteEntryRow("pom_planning_entry", i);
                            }
                        }
                    }
                    return;
                }
                IDataModel entryOperate = this.getModel();
                DynamicObject now = entryOperate.getEntryRowEntity("pom_planning_entry", rowIndex);
                if ("A".equals(now.getString("nckd_producttype")) || "B".equals(now.getString("nckd_producttype"))) {
                    return;
                }

                Object id = entry.getPkValue();
                for (int i = 0; i < entrys.size(); i++) {
                    if (id.equals(entrys.get(i).get("pid"))) {
                        DynamicObject pomPlanningEntry = entryOperate.getEntryRowEntity("pom_planning_entry", rowIndex + 1);
                        if ("10720".equals(copentrys.get(0).getString("copentrytype"))) {
                            pomPlanningEntry.set("nckd_producttype", "A");
                        } else {
                            pomPlanningEntry.set("nckd_producttype", "B");
                        }
                        pomPlanningEntry.set("nckd_producedept", entrys.get(rowIndex).getDynamicObject("nckd_producedept"));
                        pomPlanningEntry.set("nckd_bomid", mftbom);
                        pomPlanningEntry.set("material", copentrys.get(0).getDynamicObject("copentrymaterial"));
                        pomPlanningEntry.set("nckd_unit", copentrys.get(0).getDynamicObject("copentryunit"));
                        grid.expand(rowIndex);
                        this.getView().updateView();
                        return;
                    }
                }
                entryOperate.insertEntryRow("pom_planning_entry", rowIndex);
                DynamicObject pomPlanningEntry = entryOperate.getEntryRowEntity("pom_planning_entry", rowIndex + 1);
                if ("10720".equals(copentrys.get(0).getString("copentrytype"))) {
                    pomPlanningEntry.set("nckd_producttype", "A");
                } else {
                    pomPlanningEntry.set("nckd_producttype", "B");
                }
                pomPlanningEntry.set("nckd_producedept", entrys.get(rowIndex).getDynamicObject("nckd_producedept"));
                pomPlanningEntry.set("nckd_bomid", mftbom);
                pomPlanningEntry.set("material", copentrys.get(0).getDynamicObject("copentrymaterial"));
                pomPlanningEntry.set("nckd_unit", copentrys.get(0).getDynamicObject("copentryunit"));
                grid.expand(rowIndex);
                this.getView().updateView();
            }
        }
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
    }
}
