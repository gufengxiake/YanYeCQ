package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.db.DB;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.TreeEntryGrid;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.control.events.RowClickEvent;
import kd.bos.form.control.events.RowClickEventListener;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.AfterF7SelectEvent;
import kd.bos.form.field.events.AfterF7SelectListener;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.operation.DeleteServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 供应链-生产计划单
 * 表单标识：nckd_pom_planning
 * author：xiaoxiaopeng
 * date：2024-08-05
 */

public class ProductionPlanFromPlugin extends AbstractBillPlugIn implements RowClickEventListener, BeforeF7SelectListener, AfterF7SelectListener {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("tbmain", "nckd_advcontoolbarap");
        Toolbar toolbar = this.getControl("tbmain");
        toolbar.addItemClickListener(this);
        //监听单据体
        EntryGrid grid = this.getControl("pom_planning_entry");
        grid.addRowClickListener(this);
        //监听Before7
        BasedataEdit produceDept = this.getView().getControl("nckd_producedept");//生产部门
        produceDept.addBeforeF7SelectListener(this);
        BasedataEdit fieldEdit = this.getView().getControl("material");//物料
        fieldEdit.addAfterF7SelectListener(this);
    }

    /**
     * 单据体点击事件
     * @param evt
     */
    @Override
    public void entryRowClick(RowClickEvent evt) {
        RowClickEventListener.super.entryRowClick(evt);
        int row = evt.getRow();
        DynamicObject data = this.getModel().getDataEntity();
        if (row < 0) {
            return;
        }
        String number = data.getString("billno");//单据编码
        if (number == null) {
            return;
        }
        //获取当前选中单据体行
        DynamicObject entry = this.getModel().getEntryEntity("pom_planning_entry").get(row);
        //选中行物料
        DynamicObject material = entry.getDynamicObject("material");
        if (material == null) {
            return;
        }
        //获取子单据体
        DynamicObjectCollection subentryentity = entry.getDynamicObjectCollection("nckd_subentryentity");
        //如果产品类型不是主产品则清空子单据体
        if (!"C".equals(entry.getString("nckd_producttype"))) {
            subentryentity.clear();
            this.getView().updateView("nckd_subentryentity");
            return;
        }
        //获取当前选中行数量
        BigDecimal nckdYield = entry.getBigDecimal("nckd_yield");
        if (nckdYield.compareTo(BigDecimal.ZERO) < 1) {
            return;
        }
        //以上校验都通过则先清除子单据体数据
        if (subentryentity.size() > 0) {
            subentryentity.clear();
            this.getView().updateView("nckd_subentryentity");
        }
        //获取当前选中行bom
        DynamicObject nckdBomid = entry.getDynamicObject("nckd_bomid");
        if (nckdBomid == null) {
            return;
        }
        nckdBomid = BusinessDataServiceHelper.loadSingle(nckdBomid.getPkValue(), "pdm_mftbom");
        //查bom组件
        DynamicObjectCollection bomEntry = nckdBomid.getDynamicObjectCollection("entry");
        if (bomEntry.size() <= 0) {
            return;
        }
        for (DynamicObject b : bomEntry) {
            //获取bom组件物料
            DynamicObject entrymaterial = b.getDynamicObject("entrymaterial");
            DynamicObject pBom = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material,entry", new QFilter[]{new QFilter("material.id", QCP.equals, entrymaterial.getPkValue())});
            if (pBom == null) {
                continue;
            }
            //查组件上的物料对应的bom
            pBom = BusinessDataServiceHelper.loadSingle(pBom.getPkValue(), "pdm_mftbom");
            //拿到组件物料对应bom的组件信息
            DynamicObjectCollection newBomEntry = pBom.getDynamicObjectCollection("entry");
            if (newBomEntry.size() > 0) {
                //子单据体新增一条数据存当前组件信息展示
                DynamicObject subentry = subentryentity.addNew();
                //查物料生产信息
                entrymaterial = BusinessDataServiceHelper.loadSingle(entrymaterial.getPkValue(), "bd_materialmftinfo");
                //获取bom组件的分子分母，计算公式：主产品数量*bom中子项自制件分子/分母
                BigDecimal entryqtynumerator = b.getBigDecimal("entryqtynumerator");
                BigDecimal entryqtydenominator = b.getBigDecimal("entryqtydenominator");
                BigDecimal result = (nckdYield.multiply(entryqtynumerator).divide(entryqtydenominator)).setScale(2, BigDecimal.ROUND_HALF_UP);
                subentry.set("nckd_material", entrymaterial);//子单据体物料
                subentry.set("nckd_mname", entrymaterial.getDynamicObject("masterid").get("name"));//物料名称
                subentry.set("producedept", entrymaterial.getDynamicObject("departmentorgid"));//生产部门
                subentry.set("yiel", result);//数量
                subentry.set("unit", b.getDynamicObject("entryunit"));//单位
                //因为不清楚一个主产品有多少个半成品组件，这里用递归
                setSubentry(pBom, subentryentity, result);
            }

        }
        SaveServiceHelper.save(new DynamicObject[]{data});
        this.getView().updateView("nckd_subentryentity");
    }

    /**
     * 递归查询产品组件并赋值
     * @param nckdBomid
     * @param subentryentity
     * @param nckdYield
     */
    private void setSubentry(DynamicObject nckdBomid, DynamicObjectCollection subentryentity, BigDecimal nckdYield) {
        DynamicObjectCollection bomEntry = nckdBomid.getDynamicObjectCollection("entry");
        for (DynamicObject b : bomEntry) {
            DynamicObject entrymaterial = b.getDynamicObject("entrymaterial");
            DynamicObject pBom = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material,entry", new QFilter[]{new QFilter("material.id", QCP.equals, entrymaterial.getPkValue())});
            if (pBom == null) {
                return;
            }
            pBom = BusinessDataServiceHelper.loadSingle(pBom.getPkValue(), "pdm_mftbom");
            DynamicObjectCollection newBomEntry = pBom.getDynamicObjectCollection("entry");
            if (newBomEntry.size() > 0) {
                DynamicObject subentry = subentryentity.addNew();
                entrymaterial = BusinessDataServiceHelper.loadSingle(entrymaterial.getPkValue(), "bd_materialmftinfo");
                BigDecimal entryqtynumerator = b.getBigDecimal("entryqtynumerator");
                BigDecimal entryqtydenominator = b.getBigDecimal("entryqtydenominator");
                BigDecimal result = (nckdYield.multiply(entryqtynumerator).divide(entryqtydenominator)).setScale(2, BigDecimal.ROUND_HALF_UP);
                subentry.set("nckd_material", entrymaterial);
                subentry.set("nckd_mname", entrymaterial.getDynamicObject("masterid").get("name"));
                subentry.set("producedept", entrymaterial.getDynamicObject("departmentorgid"));
                subentry.set("yiel", result);
                subentry.set("unit", b.getDynamicObject("entryunit"));
                setSubentry(pBom, subentryentity, nckdYield);
            }

        }
    }

    /**
     * 按钮监听
     * @param evt
     */
    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        //新增分录
        if (StringUtils.equals(itemKey, "tb_new")) {
            Object data = this.getModel().getValue("nckd_plan_month");//计划月份
            if (data != null) {
                //默认计划开始时间为月初，计划结束时间为月末
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat simpleDateFormat_day = new SimpleDateFormat("yyyy-MM-dd");
                Calendar cal = Calendar.getInstance();
                cal.setTime((Date) data);
                int last = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                cal.set(Calendar.DAY_OF_MONTH, last);
                Date formatData = null;
                try {
                    formatData = simpleDateFormat.parse((simpleDateFormat_day.format(cal.getTime())) + " 23:59:59");
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                EntryGrid treeEntryEntity = this.getControl("pom_planning_entry");
                int[] rows = treeEntryEntity.getSelectRows();
                if (rows != null && rows.length > 0) {
                    for (int row : rows) {
                        this.getModel().setValue("nckd_planstarttime", data, row);//计划开始时间
                        this.getModel().setValue("nckd_planendtime", formatData, row);//计划结束时间
                    }
                }
            }
        }
        //关联生成（下推生产工单）
        if (StringUtils.equals(itemKey, "nckd_bar")) {
            //判断状态，审核完成才可以下推
            Object billstatus = this.getModel().getValue("billstatus");
            if (!"C".equals(billstatus)) {
                this.getView().showErrorNotification("该单据未审核完成，不可下推");
                return;
            }
            //校验该单据是否已经下推生产工单
            DynamicObject dataEntity = this.getModel().getDataEntity();
            DynamicObject pom = BusinessDataServiceHelper.loadSingle("pom_mftorder", "id,billno,nckd_sourcebill", new QFilter[]{new QFilter("nckd_sourcebill", QCP.equals, dataEntity.getString("billno"))});
            if (pom != null) {
                this.getView().showErrorNotification("请勿重复下推");
                return;
            }
            //拿到单据体数据
            DynamicObjectCollection entity = this.getModel().getEntryEntity("pom_planning_entry");
            //bom上下级没有直接可以关联查询的字段，只能通过拿到当前bom组件物料去查该物料对应的主产品bom，如果查到了则说明有下级关系，反之则没有
            if (entity != null && entity.size() > 0) {
                //拿到计划单每行分录物料信息去查该物料是否有下级关系，有则count+1，count>0则需要拆成多个工单
                int count = 0;
                for (DynamicObject d : entity) {
                    DynamicObject pdm = d.getDynamicObject("nckd_bomid");//bom
                    pdm = BusinessDataServiceHelper.loadSingle(pdm.getPkValue(), "pdm_mftbom");
                    DynamicObjectCollection entry = pdm.getDynamicObjectCollection("entry");//bom组件
                    if (entry.size() > 0) {
                        for (DynamicObject eny : entry) {
                            DynamicObject entrymaterial = eny.getDynamicObject("entrymaterial");//组件物料
                            //通过物料查bom
                            pdm = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material", new QFilter[]{new QFilter("material", QCP.equals, entrymaterial.getPkValue())});
                            if (pdm != null) {
                                count = count + 1;
                            }
                        }
                    }
                }
                if (count > 0) {
                    setFinishToMft(entity);
                    return;
                }
            }
            setPlanToMft();
        }
    }

    /**
     * 计划单信息下推生产工单（合并拆分）
     * @param entity
     */
    private void setFinishToMft(DynamicObjectCollection entity) {

        DynamicObject dataEntity = this.getModel().getDataEntity();
        for (int i = 0; i < entity.size(); i++) {
            //主产品才生成工单，联副产品不单独生成工单，放在对应主产品下面保留树形结构
            if (!"C".equals(entity.get(i).getString("nckd_producttype"))) {
                continue;
            }
            //拿到生产工单树形单据体
            DynamicObject pomMftorder = BusinessDataServiceHelper.newDynamicObject("pom_mftorder");
            DynamicObjectCollection treeentryentity = pomMftorder.getDynamicObjectCollection("treeentryentity");

            //获取生产工单编码规则生成编码
            CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(pomMftorder.getDataEntityType().getName(), pomMftorder, null);
            String number = CodeRuleServiceHelper.readNumber(codeRule, pomMftorder);

            //查询生产工单单据类型
            DynamicObject pmf = BusinessDataServiceHelper.loadSingle("bos_billtype", "id", new QFilter[]{new QFilter("number", QCP.equals, "pom_mftorder_BT_S")});
            //计划单分录行
            DynamicObject oldEntity = entity.get(i);
            pomMftorder.set("billno", number);//编码
            pomMftorder.set("org", dataEntity.getDynamicObject("org"));//生产组织
            pomMftorder.set("transactiontype", dataEntity.getDynamicObject("nckd_transactiontype"));//生产事务类型
            pomMftorder.set("billdate", new Date());//单据日期
            pomMftorder.set("billstatus", "A");//单据状态
            pomMftorder.set("nckd_sourcebill", dataEntity.get("billno"));//来源单据编号
            pomMftorder.set("billtype", pmf);//单据类型
            pomMftorder.set("entryinwardept", dataEntity.getDynamicObject("org"));//入库组织
            pomMftorder.set("nckd_plan_month", dataEntity.get("nckd_plan_month"));//计划月份
            pomMftorder.set("nckd_plan_unit", dataEntity.getDynamicObject("nckd_plan_unit"));//计划下达单位
            pomMftorder.set("nckd_planorg", oldEntity.getDynamicObject("nckd_producedept"));//计划单成品生产部门
            pomMftorder.set("nckd_planmaterial", oldEntity.getDynamicObject("material"));//计划单成品物料
            pomMftorder.set("entrybaseqty", oldEntity.getBigDecimal("nckd_yield"));//
            DynamicObject nckdBomid = oldEntity.getDynamicObject("nckd_bomid");
            if (nckdBomid == null) {
                return;
            }
            //查bom
            nckdBomid = BusinessDataServiceHelper.loadSingle(nckdBomid.getPkValue(), "pdm_mftbom");
            //获取bom主产品物料
            DynamicObject material = nckdBomid.getDynamicObject("material");
            material = BusinessDataServiceHelper.loadSingle(material.getPkValue(), "bd_materialmftinfo");
            //生成随机单据体id
            long id = DB.genLongId("t_pom_mftorderentry");
            //计划数量
            BigDecimal yield = oldEntity.getBigDecimal("nckd_yield");
            DynamicObject newOne = treeentryentity.addNew();
            newOne.set("id", id);//随机生成一个long类型的id
            newOne.set("producttype", "C");
            newOne.set("material", material);//物料
            newOne.set("materielmasterid", nckdBomid.getDynamicObject("material").get("masterid"));//物料(主数据)
            newOne.set("producedept", oldEntity.getDynamicObject("nckd_producedept"));//生产部门
            newOne.set("qty", yield);//数量
            newOne.set("unit", material.getDynamicObject("mftunit"));//计量单位
            newOne.set("bomid", nckdBomid);//bom
            newOne.set("planbegintime", oldEntity.get("nckd_planstarttime"));//计划开工时间
            newOne.set("planendtime", oldEntity.get("nckd_planendtime"));//计划完工时间
            newOne.set("planstatus", "B");//计划状态
            newOne.set("baseunit", material.getDynamicObject("mftunit"));//基本单位
            newOne.set("expendbomtime", oldEntity.get("nckd_planendtime"));//展开时间
            newOne.set("inwardept", dataEntity.getDynamicObject("org"));//入库组织
            newOne.set("planpreparetime", oldEntity.get("nckd_planstarttime"));//计划准备时间
            newOne.set("baseqty", yield);//基本数量
            pomMftorder.set("remark", pomMftorder.get("remark") == null ? oldEntity.getString("nckd_remark") : oldEntity.getString("nckd_remark") + pomMftorder.getString("remark"));//备注
            pomMftorder.set("nckd_planentryid", oldEntity.getPkValue());
            //获取bom对应的联副产品
            DynamicObjectCollection copent = nckdBomid.getDynamicObjectCollection("copentry");
            if (copent.size() > 0) {
                //生成联副产品对应分录
                for (int j = 0; j < copent.size(); j++) {
                    DynamicObject cop = copent.get(j);
                    DynamicObject newTwo = treeentryentity.addNew();
                    newTwo.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                    newTwo.set("pid", id);//添加父id  建立父子关系
                    if ("10720".equals(cop.getString("copentrytype"))) {
                        newTwo.set("producttype", "A");
                    } else {
                        newTwo.set("producttype", "B");
                    }
                    DynamicObject copentrymaterial = cop.getDynamicObject("copentrymaterial") == null ? cop.getDynamicObject("copentrymaterial") : BusinessDataServiceHelper.loadSingle(cop.getDynamicObject("copentrymaterial").getPkValue(), "bd_materialmftinfo");
                    newTwo.set("materielmasterid", copentrymaterial == null ? null : cop.getDynamicObject("copentrymaterial"));//物料
                    newTwo.set("material", copentrymaterial);
                    newTwo.set("producedept", copentrymaterial == null ? copentrymaterial : copentrymaterial.getDynamicObject("departmentorgid"));
                    newTwo.set("qty", oldEntity.getBigDecimal("nckd_yield").multiply(cop.getBigDecimal("copentryqty")));
                    newTwo.set("unit", cop.getDynamicObject("copentryunit"));
                    newTwo.set("bomid", nckdBomid);
                    newTwo.set("planbegintime", oldEntity.get("nckd_planstarttime"));
                    newTwo.set("planendtime", oldEntity.get("nckd_planendtime"));
                    newTwo.set("planstatus", "B");
                    newTwo.set("baseunit", cop.getDynamicObject("copentryunit"));
                    newTwo.set("inwardept", dataEntity.getDynamicObject("org"));
                    newTwo.set("expendbomtime", oldEntity.get("nckd_planendtime"));
                    newTwo.set("planpreparetime", oldEntity.get("nckd_planstarttime"));
                    newTwo.set("baseqty", oldEntity.getBigDecimal("nckd_yield").multiply(cop.getBigDecimal("copentryqty")));
                    pomMftorder.set("remark", pomMftorder.get("remark") == null ? oldEntity.getString("nckd_remark")
                            : oldEntity.getString("nckd_remark") + pomMftorder.getString("remark"));
                }
            }
            OperationResult result1 = OperationServiceHelper.executeOperate("save", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
            if (!result1.isSuccess()) {
                this.getView().showMessage(result1.getMessage());
                return;
            }
            //TODO
            //递归关联查找到BOM对应的半成品并生成工单
            setProducts(nckdBomid, oldEntity, pmf, pomMftorder, yield);

            //根据单据体id找到对应生成的生产工单回写单据编号给计划单
            StringBuilder stringBuilder = new StringBuilder();
            DynamicObject[] pom_mftorder = BusinessDataServiceHelper.load("pom_mftorder", "id,billno,nckd_planentryid", new QFilter[]{new QFilter("nckd_planentryid", QCP.equals, oldEntity.getPkValue().toString())});
            for (DynamicObject pom : pom_mftorder) {
                stringBuilder.append(pom.getString("billno") + "/");
            }
            oldEntity.set("nckd_pom", stringBuilder.toString());
            SaveServiceHelper.update(dataEntity);
            this.getView().invokeOperation("refresh");

        }
        //判断是否需要合并
        List<String> deptIdList = new ArrayList<>();
        for (DynamicObject d : entity) {
            if (!d.get("nckd_producttype").equals("C")) {
                continue;
            }
            //循环获取，将计划主产品明细部门编码和当前主产品bom下对应组件物料编码整和为key存储
            DynamicObject nckdBomid = d.getDynamicObject("nckd_bomid");
            nckdBomid = BusinessDataServiceHelper.loadSingle(nckdBomid.getPkValue(), "pdm_mftbom");
            DynamicObjectCollection entry = nckdBomid.getDynamicObjectCollection("entry");
            for (DynamicObject e : entry) {
                deptIdList.add(d.getDynamicObject("nckd_producedept").getPkValue().toString() + e.getDynamicObject("entrymaterial").getPkValue().toString());
            }
        }
        //判断是否有重复key，有则需要合并
        long count = deptIdList.stream().distinct().count();
        if (deptIdList.size() == count) {
            //不需要合并，无需走下面的逻辑，直接查询到对应生产的生产工单调用提交审核操作自动生成组件清单
            DynamicObject[] pom = BusinessDataServiceHelper.load("pom_mftorder", "id,billno,nckd_sourcebill,treeentryentity,treeentryentity.producedept,treeentryentity.material,treeentryentity.qty,nckd_planentryid,nckd_merge,nckd_planorg,nckd_planmaterial",
                    new QFilter[]{new QFilter("nckd_sourcebill", QCP.equals, dataEntity.get("billno"))});
            for (DynamicObject p : pom) {
                p = BusinessDataServiceHelper.loadSingle(p.getPkValue(), "pom_mftorder");
                OperationResult result1 = OperationServiceHelper.executeOperate("submit", "pom_mftorder", new DynamicObject[]{p}, OperateOption.create());
                if (result1.isSuccess()) {
                    OperationServiceHelper.executeOperate("audit", "pom_mftorder", new DynamicObject[]{p}, OperateOption.create());
                }
                BusinessDataServiceHelper.removeCache(p.getDynamicObjectType());
            }
            return;
        }

        //查询对应生成生产工单，通过工单物料明细的部门id+物料id为key，分组出需要合并的工单
        DynamicObject[] pom = BusinessDataServiceHelper.load("pom_mftorder", "id,billno,nckd_sourcebill,nckd_builds,treeentryentity,treeentryentity.producedept,treeentryentity.material,treeentryentity.qty,nckd_merge,nckd_planentryid,nckd_planorg,nckd_planmaterial,treeentryentity.baseqty,treeentryentity.planqty,treeentryentity.planbaseqty",
                new QFilter[]{new QFilter("nckd_sourcebill", QCP.equals, dataEntity.get("billno")).and("nckd_builds", QCP.equals, true)});
        Map<String, List<DynamicObject>> map = new HashMap<>();
        for (DynamicObject p : pom) {
            List<DynamicObject> list = new ArrayList<>();
            DynamicObjectCollection treeentryentity = p.getDynamicObjectCollection("treeentryentity");
            Object materialId = treeentryentity.get(0).getDynamicObject("material").getPkValue();
            Object deptId = p.getDynamicObject("nckd_planorg").getPkValue();
            String key = deptId.toString() + materialId.toString();
            if (map.containsKey(key)) {
                continue;
            }
            list.add(p);
            for (DynamicObject y : pom) {
                if (p.getPkValue().equals(y.getPkValue())) {
                    continue;
                }
                DynamicObjectCollection treeentryentity_y = y.getDynamicObjectCollection("treeentryentity");
                Object materialId_y = treeentryentity_y.get(0).getDynamicObject("material").getPkValue();
                Object deptId_y = y.getDynamicObject("nckd_planorg").getPkValue();
                String key_y = deptId_y.toString() + materialId_y.toString();
                //判断物料+部门是否相同
                if (key.equals(key_y)) {
                    list.add(y);
                }
            }
            map.put(key, list);
        }
        //处理分组完成的map，将同一分组的工单数量就行汇总，删除多余工单保留一个
        for (List<DynamicObject> list : map.values()) {
            if (list.size() > 1) {
                BigDecimal qty = BigDecimal.ZERO;
                String pk = "";
                for (DynamicObject d : list) {
                    qty = qty.add(d.getDynamicObjectCollection("treeentryentity").get(0).getBigDecimal("qty"));
                    pk += d.getString("nckd_planentryid");
                }
                List<Object> idList = new ArrayList<>();
                for (int i = list.size() - 1; i > -1; i--) {
                    DynamicObject dynamicObject = list.get(i);
                    if (i == 0) {
                        DynamicObject treeentry = dynamicObject.getDynamicObjectCollection("treeentryentity").get(0);
                        treeentry.set("qty", qty);
                        treeentry.set("baseqty", qty);
                        treeentry.set("planbaseqty", qty);
                        treeentry.set("planqty", qty);
                        dynamicObject.set("nckd_merge", true);
                        dynamicObject.set("nckd_planentryid", pk);
                        SaveServiceHelper.update(new DynamicObject[]{dynamicObject});
                    } else {
                        idList.add(dynamicObject.getPkValue());
                    }
                }
                DeleteServiceHelper.delete("pom_mftorder", new QFilter[]{new QFilter("id", QCP.in, idList)});
            }
        }
        //处理完成找到生成的这批对应工单就行提交审核操作，生成组件清单
        pom = BusinessDataServiceHelper.load("pom_mftorder", "id,billno,nckd_sourcebill,treeentryentity,treeentryentity.producedept,treeentryentity.material,treeentryentity.qty,nckd_planentryid,nckd_merge,nckd_planorg,nckd_planmaterial",
                new QFilter[]{new QFilter("nckd_sourcebill", QCP.equals, dataEntity.get("billno"))});
        for (DynamicObject p : pom) {
            p = BusinessDataServiceHelper.loadSingle(p.getPkValue(), "pom_mftorder");
            OperationResult result1 = OperationServiceHelper.executeOperate("submit", "pom_mftorder", new DynamicObject[]{p}, OperateOption.create());
            if (result1.isSuccess()) {
                OperationServiceHelper.executeOperate("audit", "pom_mftorder", new DynamicObject[]{p}, OperateOption.create());
                BusinessDataServiceHelper.removeCache(p.getDynamicObjectType());
            }
        }
        //将生产工单编码回写至对应计划单
        for (int i = 0; i < entity.size(); i++) {
            DynamicObject d = entity.get(i);
            if (!"C".equals(d.get("nckd_producttype"))) {
                continue;
            }
            DynamicObject producedept = d.getDynamicObject("nckd_producedept");
            DynamicObject material = d.getDynamicObject("material");
            String oldKey = producedept.getPkValue().toString() + material.getPkValue().toString();
            StringBuilder stringBuilder = new StringBuilder();
            for (DynamicObject p : pom) {
                DynamicObject nckdPlanorg = p.getDynamicObject("nckd_planorg");
                DynamicObject planmaterial = p.getDynamicObject("nckd_planmaterial");
                String key = nckdPlanorg.getPkValue().toString() + planmaterial.getPkValue().toString();
                boolean nckdMerge = p.getBoolean("nckd_merge");
                if (oldKey.equals(key) && !nckdMerge) {
                    stringBuilder.append(p.getString("billno") + "/");
                    continue;
                }
                String nckdPlanentryid = p.getString("nckd_planentryid");
                if (nckdMerge && nckdPlanentryid.contains(d.getPkValue().toString())) {
                    stringBuilder.append(p.getString("billno") + "/");
                }
            }
            this.getModel().setValue("nckd_pom", stringBuilder.toString(), i);
            this.getView().updateView();
        }
    }

    private void setProducts(DynamicObject nckdBomid, DynamicObject oldEntity, DynamicObject pmf, DynamicObject pomMftorder, BigDecimal qty) {
        DynamicObjectCollection bomEntry = nckdBomid.getDynamicObjectCollection("entry");
        DynamicObject dataEntity = this.getModel().getDataEntity();
        if (bomEntry.size() > 0) {
            for (DynamicObject b : bomEntry) {
                DynamicObject entrymaterial = b.getDynamicObject("entrymaterial");
                DynamicObject pdm = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material",
                        new QFilter[]{new QFilter("material", QCP.equals, entrymaterial.getPkValue())});
                if (pdm == null) {
                    continue;
                } else {
                    DynamicObjectCollection entry = dataEntity.getDynamicObjectCollection("pom_planning_entry");
                    ArrayList<Object> ids = new ArrayList<>();
                    entry.forEach(d -> ids.add(d.getDynamicObject("material").getPkValue()));
                    if (ids.contains(entrymaterial.getPkValue())) {
                        continue;
                    }
                    entrymaterial = BusinessDataServiceHelper.loadSingle(entrymaterial.getPkValue(), "bd_materialmftinfo");
                    BigDecimal newQty = (qty.multiply(b.getBigDecimal("entryqtynumerator"))).divide(b.getBigDecimal("entryqtydenominator"));
                    pdm = BusinessDataServiceHelper.loadSingle(pdm.getPkValue(), "pdm_mftbom");
                    DynamicObject pomMftorder_a = BusinessDataServiceHelper.newDynamicObject("pom_mftorder");
                    DynamicObjectCollection treeentryentity_a = pomMftorder_a.getDynamicObjectCollection("treeentryentity");
                    CodeRuleInfo codeRule_a = CodeRuleServiceHelper.getCodeRule(pomMftorder_a.getDataEntityType().getName(), pomMftorder_a, null);
                    String number_a = CodeRuleServiceHelper.readNumber(codeRule_a, pomMftorder_a);

                    pomMftorder_a.set("billno", number_a);
                    pomMftorder_a.set("org", dataEntity.getDynamicObject("org"));
                    pomMftorder_a.set("transactiontype", dataEntity.getDynamicObject("nckd_transactiontype"));
                    pomMftorder_a.set("billdate", new Date());
                    pomMftorder_a.set("billstatus", "A");
                    pomMftorder_a.set("nckd_sourcebill", dataEntity.get("billno"));//来源单据
                    pomMftorder_a.set("billtype", pmf);//单据类型
                    pomMftorder_a.set("nckd_builds", true);
                    pomMftorder_a.set("entryinwardept", dataEntity.getDynamicObject("org"));//入库组织
                    pomMftorder_a.set("nckd_plan_month", dataEntity.get("nckd_plan_month"));//计划月份
                    pomMftorder_a.set("nckd_plan_unit", dataEntity.getDynamicObject("nckd_plan_unit"));//计划下达单位
                    pomMftorder_a.set("nckd_planorg", oldEntity.getDynamicObject("nckd_producedept"));
                    pomMftorder_a.set("nckd_planmaterial", oldEntity.getDynamicObject("material"));
                    pomMftorder_a.set("entrybaseqty", newQty);
                    long id_a = DB.genLongId("t_pom_mftorderentry");
                    DynamicObject newOne_a = treeentryentity_a.addNew();
                    newOne_a.set("id", id_a);//随机生成一个long类型的id
                    newOne_a.set("producttype", "C");
                    newOne_a.set("material", entrymaterial);//物料
                    newOne_a.set("materielmasterid", entrymaterial.get("masterid"));//物料
                    newOne_a.set("producedept", entrymaterial.getDynamicObject("departmentorgid"));
                    newOne_a.set("qty", newQty);
                    newOne_a.set("unit", entrymaterial.getDynamicObject("mftunit"));
                    newOne_a.set("bomid", pdm);
                    newOne_a.set("planbegintime", oldEntity.get("nckd_planstarttime"));
                    newOne_a.set("planendtime", oldEntity.get("nckd_planendtime"));
                    newOne_a.set("planstatus", "B");
                    newOne_a.set("baseunit", entrymaterial.getDynamicObject("mftunit"));
                    newOne_a.set("inwardept", dataEntity.getDynamicObject("org"));
                    newOne_a.set("expendbomtime", oldEntity.get("nckd_planendtime"));
                    newOne_a.set("planpreparetime", oldEntity.get("nckd_planstarttime"));
                    newOne_a.set("baseqty", newQty);
                    pomMftorder_a.set("remark", pomMftorder_a.get("remark") == null ? oldEntity.getString("nckd_remark") : oldEntity.getString("nckd_remark") + pomMftorder.getString("remark"));
                    pomMftorder_a.set("nckd_planentryid", oldEntity.getPkValue());
                    DynamicObjectCollection copent_a = pdm.getDynamicObjectCollection("copentry");
                    if (copent_a.size() > 0) {
                        for (int j = 0; j < copent_a.size(); j++) {
                            DynamicObject cop = copent_a.get(j);
                            DynamicObject newTwo = treeentryentity_a.addNew();
                            DynamicObject copentrymaterial = cop.getDynamicObject("copentrymaterial") == null ? cop.getDynamicObject("copentrymaterial") : BusinessDataServiceHelper.loadSingle(cop.getDynamicObject("copentrymaterial").getPkValue(), "bd_materialmftinfo");
                            newTwo.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                            newTwo.set("pid", id_a);//添加父id  建立父子关系
                            if ("10720".equals(cop.getString("copentrytype"))) {
                                newTwo.set("producttype", "A");
                            } else {
                                newTwo.set("producttype", "B");
                            }
                            newTwo.set("materielmasterid", copentrymaterial.get("masterid"));//物料
                            newTwo.set("material", copentrymaterial);
                            newTwo.set("producedept", copentrymaterial.getDynamicObject("departmentorgid"));
                            newTwo.set("qty", newQty.multiply(cop.getBigDecimal("copentryqty")));
                            newTwo.set("unit", cop.getDynamicObject("copentryunit"));
                            newTwo.set("bomid", pdm);
                            newTwo.set("planbegintime", oldEntity.get("nckd_planstarttime"));
                            newTwo.set("planendtime", oldEntity.get("nckd_planendtime"));
                            newTwo.set("planstatus", "B");
                            newTwo.set("baseunit", cop.getDynamicObject("copentryunit"));
                            newTwo.set("inwardept", dataEntity.getDynamicObject("org"));
                            newTwo.set("expendbomtime", oldEntity.get("nckd_planendtime"));
                            newTwo.set("planpreparetime", oldEntity.get("nckd_planstarttime"));
                            newTwo.set("baseqty", newQty.multiply(cop.getBigDecimal("copentryqty")));
                            pomMftorder_a.set("remark", pomMftorder_a.get("remark") == null ? oldEntity.getString("nckd_remark")
                                    : oldEntity.getString("nckd_remark") + pomMftorder_a.getString("remark"));
                        }
                    }
                    OperationResult result = OperationServiceHelper.executeOperate("save", "pom_mftorder", new DynamicObject[]{pomMftorder_a}, OperateOption.create());
                    if (!result.isSuccess()) {
                        this.getView().showMessage(result.getMessage());
                        return;
                    }
                    setProducts(pdm, oldEntity, pmf, pomMftorder_a, newQty);
                }
            }
        }
    }


    private void setPlanToMft() {
        DynamicObject dataEntity = this.getModel().getDataEntity();
        DynamicObjectCollection pomPlanningEntryColl = this.getModel().getEntryEntity("pom_planning_entry");

        String materialId = "";
        String depId = "";
        String pomId = "";
        List<DynamicObject> aList = new ArrayList<>();
        for (int i = 0; i < pomPlanningEntryColl.size(); i++) {
            DynamicObject pom = pomPlanningEntryColl.get(i);
            if ("C".equals(pom.getString("nckd_producttype"))) {
                materialId = pom.getDynamicObject("material").getPkValue().toString();
                depId = pom.getDynamicObject("nckd_producedept").getPkValue().toString();
                pomId = pom.getPkValue().toString();
                aList.add(pom);
                for (DynamicObject d : pomPlanningEntryColl) {
                    if ("C".equals(d.getString("nckd_producttype"))) {
                        if (!d.getPkValue().equals(pom.getPkValue())) {
                            if (!(d.getDynamicObject("material").getPkValue().toString() + d.getDynamicObject("nckd_producedept").getPkValue().toString()).equals(materialId + depId)) {
                                aList.add(d);
                            }
                            if ((d.getDynamicObject("material").getPkValue().toString() + d.getDynamicObject("nckd_producedept").getPkValue().toString()).equals(materialId + depId) && !pomId.equals(d.getPkValue())) {
                                aList.add(d);
                            }
                        }
                    }

                }
                break;
            }
        }
        if (aList.size() <= 1) {
            DynamicObject pomMftorder = BusinessDataServiceHelper.newDynamicObject("pom_mftorder");
            DynamicObjectCollection treeentryentity = pomMftorder.getDynamicObjectCollection("treeentryentity");//拿到生产工单树形单据体

            CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(pomMftorder.getDataEntityType().getName(), pomMftorder, null);
            String number = CodeRuleServiceHelper.readNumber(codeRule, pomMftorder);
            DynamicObject pmf = BusinessDataServiceHelper.loadSingle("bos_billtype", "id", new QFilter[]{new QFilter("number", QCP.equals, "pom_mftorder_BT_S")});
            pomMftorder.set("billno", number);
            pomMftorder.set("org", dataEntity.getDynamicObject("org"));
            pomMftorder.set("transactiontype", dataEntity.getDynamicObject("nckd_transactiontype"));
            pomMftorder.set("billdate", new Date());
            pomMftorder.set("billstatus", "A");
            pomMftorder.set("nckd_sourcebill", dataEntity.get("billno"));//来源单据
            pomMftorder.set("billtype", pmf);//单据类型
            pomMftorder.set("entryinwardept", dataEntity.getDynamicObject("org"));//入库组织
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
                    if ("C".equals(pomPlanningEntry.get("nckd_producttype"))) {
                        //一顿赋值操作
                        DynamicObject newOne = treeentryentity.addNew();
                        DynamicObject material = pomPlanningEntry.getDynamicObject("material") == null ? pomPlanningEntry.getDynamicObject("material") : BusinessDataServiceHelper.loadSingle(pomPlanningEntry.getDynamicObject("material").getPkValue(), "bd_materialmftinfo");
                        newOne.set("id", id);//随机生成一个long类型的id
                        newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));
                        newOne.set("material", material);//物料
                        newOne.set("materielmasterid", material.get("masterid"));//物料
                        newOne.set("producedept", material.getDynamicObject("departmentorgid"));
                        newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                        newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                        newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                        newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                        newOne.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                        newOne.set("planstatus", "B");
                        newOne.set("baseunit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                        newOne.set("expendbomtime", pomPlanningEntry.get("nckd_planendtime"));
                        newOne.set("inwardept", dataEntity.getDynamicObject("org"));
                        newOne.set("planpreparetime", pomPlanningEntry.get("nckd_planstarttime"));
                        newOne.set("baseqty", pomPlanningEntry.get("nckd_yield"));
                        pomMftorder.set("remark", pomMftorder.get("remark") == null ? pomPlanningEntry.getString("nckd_remark") : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                        pomMftorder.set("entrybaseqty", pomPlanningEntry.getBigDecimal("nckd_yield"));
                    }
                    if (i > 0) {
                        //这个是本次副产品对应上一条主产品
                        DynamicObject newOne = treeentryentity.addNew();
                        DynamicObject material = pomPlanningEntry.getDynamicObject("material") == null ?
                                pomPlanningEntry.getDynamicObject("material") : BusinessDataServiceHelper.loadSingle(pomPlanningEntry.getDynamicObject("material").getPkValue(), "bd_materialmftinfo");
                        newOne.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                        newOne.set("pid", id);//添加父id  建立父子关系
                        newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));//物料
                        newOne.set("materielmasterid", material.get("masterid"));//物料
                        newOne.set("material", material);
                        newOne.set("producedept", material.getDynamicObject("departmentorgid"));
                        newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                        newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                        newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                        newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                        newOne.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                        newOne.set("planstatus", "B");
                        newOne.set("expendbomtime", pomPlanningEntry.get("nckd_planendtime"));
                        newOne.set("baseunit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                        newOne.set("inwardept", dataEntity.getDynamicObject("org"));
                        newOne.set("planpreparetime", pomPlanningEntry.get("nckd_planstarttime"));
                        newOne.set("baseqty", pomPlanningEntry.get("nckd_yield"));

                        pomMftorder.set("remark", pomMftorder.get("remark") == null ? pomPlanningEntry.getString("nckd_remark")
                                : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                    }
                }
                OperationResult result = OperationServiceHelper.executeOperate("save", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                if (!result.isSuccess()) {
                    this.getView().showMessage(result.getMessage());
                    return;
                }
                OperationResult subResult = OperationServiceHelper.executeOperate("submit", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                if (subResult.isSuccess()) {
                    OperationServiceHelper.executeOperate("audit", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                }
                this.getModel().setValue("nckd_pom", number, 0);
            }
            //根据物料＋生产部门进行拆单
            if (lists.size() > 1) {
                for (List<DynamicObject> list : lists) {
                    long id = 0;
                    for (int i = 0; i < list.size(); i++) {
                        DynamicObject pomPlanningEntry = list.get(i);
                        if ("C".equals(pomPlanningEntry.get("nckd_producttype"))) {
                            //一顿赋值操作
                            id = DB.genLongId("t_pom_mftorderentry");
                            DynamicObject newOne = treeentryentity.addNew();
                            DynamicObject material = pomPlanningEntry.getDynamicObject("material") == null ?
                                    pomPlanningEntry.getDynamicObject("material") : BusinessDataServiceHelper.loadSingle(pomPlanningEntry.getDynamicObject("material").getPkValue(), "bd_materialmftinfo");
                            newOne.set("id", id);//随机生成一个long类型的id
                            newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));
                            newOne.set("material", material);//物料
                            newOne.set("materielmasterid", material.get("masterid"));//物料
                            newOne.set("producedept", material.getDynamicObject("departmentorgid"));
                            newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                            newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                            newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                            newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                            newOne.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                            newOne.set("planstatus", "B");
                            newOne.set("expendbomtime", pomPlanningEntry.get("nckd_planendtime"));
                            newOne.set("inwardept", dataEntity.getDynamicObject("org"));
                            newOne.set("planpreparetime", pomPlanningEntry.get("nckd_planstarttime"));
                            newOne.set("baseqty", pomPlanningEntry.get("nckd_yield"));
                            newOne.set("baseunit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                            pomMftorder.set("remark", pomMftorder.get("remark") == null ?
                                    pomPlanningEntry.getString("nckd_remark") : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                        }
                        if (i > 0) {
                            DynamicObject newTwo = treeentryentity.addNew();
                            DynamicObject material = pomPlanningEntry.getDynamicObject("material") == null ?
                                    pomPlanningEntry.getDynamicObject("material") : BusinessDataServiceHelper.loadSingle(pomPlanningEntry.getDynamicObject("material").getPkValue(), "bd_materialmftinfo");
                            newTwo.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                            newTwo.set("pid", id);//添加父id  建立父子关系
                            newTwo.set("producttype", pomPlanningEntry.getString("nckd_producttype"));//物料
                            newTwo.set("materielmasterid", material.get("masterid"));//物料
                            newTwo.set("material", material);
                            newTwo.set("producedept", material.getDynamicObject("departmentorgid"));
                            newTwo.set("qty", pomPlanningEntry.get("nckd_yield"));
                            newTwo.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                            newTwo.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                            newTwo.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                            newTwo.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                            newTwo.set("planstatus", "B");
                            newTwo.set("expendbomtime", pomPlanningEntry.get("nckd_planendtime"));
                            newTwo.set("baseunit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                            newTwo.set("inwardept", dataEntity.getDynamicObject("org"));
                            newTwo.set("planpreparetime", pomPlanningEntry.get("nckd_planstarttime"));
                            newTwo.set("baseqty", pomPlanningEntry.get("nckd_yield"));
                            pomMftorder.set("remark", pomMftorder.get("remark") == null ? pomPlanningEntry.getString("nckd_remark") :
                                    pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                        }
                    }
                }
                OperationResult result = OperationServiceHelper.executeOperate("save", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                if (!result.isSuccess()) {
                    this.getView().showMessage(result.getMessage());
                    return;
                }
                OperationResult submit = OperationServiceHelper.executeOperate("submit", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                if (submit.isSuccess()) {
                    OperationServiceHelper.executeOperate("audit", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                }
                this.getModel().setValue("nckd_pom", number, 0);

            }

        }
        if (aList.size() > 1) {
            long id = 0;
            for (int i = 0; i < aList.size(); i++) {
                DynamicObject pomMftorder = BusinessDataServiceHelper.newDynamicObject("pom_mftorder");
                DynamicObjectCollection treeentryentity = pomMftorder.getDynamicObjectCollection("treeentryentity");//拿到生产工单树形单据体
                CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule(pomMftorder.getDataEntityType().getName(), pomMftorder, null);
                String number = CodeRuleServiceHelper.readNumber(codeRule, pomMftorder);
                DynamicObject pmf = BusinessDataServiceHelper.loadSingle("bos_billtype", "id", new QFilter[]{new QFilter("number", QCP.equals, "pom_mftorder_BT_S")});
                pomMftorder.set("billno", number);
                pomMftorder.set("org", dataEntity.getDynamicObject("org"));
                pomMftorder.set("transactiontype", dataEntity.getDynamicObject("nckd_transactiontype"));
                pomMftorder.set("billdate", new Date());
                pomMftorder.set("billstatus", "A");
                pomMftorder.set("nckd_sourcebill", dataEntity.get("billno"));//来源单据
                pomMftorder.set("billtype", pmf);//单据类型

                DynamicObject pomPlanningEntry = aList.get(i);
                id = DB.genLongId("t_pom_mftorderentry");
                DynamicObject newOne = treeentryentity.addNew();
                DynamicObject material = pomPlanningEntry.getDynamicObject("material") == null ?
                        pomPlanningEntry.getDynamicObject("material") : BusinessDataServiceHelper.loadSingle(pomPlanningEntry.getDynamicObject("material").getPkValue(), "bd_materialmftinfo");
                newOne.set("id", id);//随机生成一个long类型的id
                newOne.set("producttype", pomPlanningEntry.getString("nckd_producttype"));
                newOne.set("material", material);//物料
                newOne.set("materielmasterid", material.get("masterid"));//物料
                newOne.set("producedept", material.getDynamicObject("departmentorgid"));
                newOne.set("qty", pomPlanningEntry.get("nckd_yield"));
                newOne.set("unit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                newOne.set("bomid", pomPlanningEntry.getDynamicObject("nckd_bomid"));
                newOne.set("planbegintime", pomPlanningEntry.get("nckd_planstarttime"));
                newOne.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                newOne.set("planstatus", "B");
                newOne.set("expendbomtime", pomPlanningEntry.get("nckd_planendtime"));
                newOne.set("baseunit", pomPlanningEntry.getDynamicObject("nckd_unit"));
                newOne.set("inwardept", dataEntity.getDynamicObject("org"));
                newOne.set("planpreparetime", pomPlanningEntry.get("nckd_planstarttime"));
                newOne.set("baseqty", pomPlanningEntry.get("nckd_yield"));
                pomMftorder.set("remark", pomMftorder.get("remark") == null ?
                        pomPlanningEntry.getString("nckd_remark") : pomPlanningEntry.getString("nckd_remark") + pomMftorder.getString("remark"));
                for (DynamicObject d : pomPlanningEntryColl) {
                    if (pomPlanningEntry.getPkValue().equals(d.get("pid"))) {
                        DynamicObject newTwo = treeentryentity.addNew();
                        DynamicObject material_a = d.getDynamicObject("material") == null ?
                                d.getDynamicObject("material") : BusinessDataServiceHelper.loadSingle(d.getDynamicObject("material").getPkValue(), "bd_materialmftinfo");
                        newTwo.set("id", DB.genLongId("t_pom_mftorderentry"));//随机生成一个long类型的id
                        newTwo.set("pid", id);//添加父id  建立父子关系
                        newTwo.set("producttype", d.getString("nckd_producttype"));//物料
                        newTwo.set("materielmasterid", material_a.get("masterid"));//物料
                        newTwo.set("material", material_a);
                        newTwo.set("producedept", material_a.getDynamicObject("departmentorgid"));
                        newTwo.set("qty", d.get("nckd_yield"));
                        newTwo.set("unit", d.getDynamicObject("nckd_unit"));
                        newTwo.set("bomid", d.getDynamicObject("nckd_bomid"));
                        newTwo.set("planbegintime", d.get("nckd_planstarttime"));
                        newTwo.set("planendtime", pomPlanningEntry.get("nckd_planendtime"));
                        newTwo.set("planstatus", "B");
                        newTwo.set("expendbomtime", pomPlanningEntry.get("nckd_planendtime"));
                        newTwo.set("baseunit", d.getDynamicObject("nckd_unit"));
                        newTwo.set("inwardept", dataEntity.getDynamicObject("org"));
                        newTwo.set("planpreparetime", pomPlanningEntry.get("nckd_planstarttime"));
                        newTwo.set("baseqty", d.get("nckd_yield"));
                        pomMftorder.set("remark", pomMftorder.get("remark") == null ? d.getString("nckd_remark") :
                                d.getString("nckd_remark") + pomMftorder.getString("remark"));
                    }
                }
                OperationResult result = OperationServiceHelper.executeOperate("save", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                if (!result.isSuccess()) {
                    this.getView().showMessage(result.getMessage());
                    return;
                }
                OperationResult submit = OperationServiceHelper.executeOperate("submit", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                if (submit.isSuccess()) {
                    OperationServiceHelper.executeOperate("audit", "pom_mftorder", new DynamicObject[]{pomMftorder}, OperateOption.create());
                }
                this.getModel().setValue("nckd_pom", number, i);
                this.getView().updateView();

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
        //获取所在月份最后一天
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat simpleDateFormat_day = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) data);
        int last = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, last);
        Date formatData = null;
        try {
            formatData = simpleDateFormat.parse((simpleDateFormat_day.format(cal.getTime())) + " 23:59:59");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        int rowCount = this.getModel().getEntryRowCount("pom_planning_entry");
        if (rowCount > 0) {
            for (int i = 0; i < rowCount; i++) {
                this.getModel().setValue("nckd_planstarttime", data, i);
                this.getModel().setValue("nckd_planendtime", formatData, i);
                this.getView().updateView();
            }
        }
    }

    /**
     * 生产计划单物料明细填写数量自动计算对应联副产品数量
     */
    private void setYield() {
        //当前物料明细操作行
        int rowIndex = this.getModel().getEntryCurrentRowIndex("pom_planning_entry");
        DynamicObjectCollection entity = this.getModel().getEntryEntity("pom_planning_entry");
        DynamicObject entry = entity.get(rowIndex);
        //计划数量
        BigDecimal value = (BigDecimal) this.getModel().getValue("nckd_yield", rowIndex);
        //保留两位小数
        this.getModel().setValue("nckd_yield", value.setScale(2, BigDecimal.ROUND_HALF_UP));
        //生产部门
        String nckdProducttype = entry.getString("nckd_producttype");
        if ("A".equals(nckdProducttype) || "B".equals(nckdProducttype)) {
            return;
        }
        BigDecimal nckdYield = entry.getBigDecimal("nckd_yield");
        for (int i = 0; i < entity.size(); i++) {
            //找到主产品对应联副产品
            if (entry.getPkValue().equals(entity.get(i).get("pid"))) {
                DynamicObject nckdBomid = entry.getDynamicObject("nckd_bomid");
                DynamicObject nckdBomid_a = entity.get(i).getDynamicObject("nckd_bomid");
                String type = entity.get(i).getString("nckd_producttype");
                if (nckdYield != null && nckdBomid_a != null) {
                    if (("A".equals(type) || "B".equals(type)) && nckdBomid.getPkValue().equals(nckdBomid_a.getPkValue())) {
                        //通过bom找到对应联副产品数量计算计划数量
                        nckdBomid_a = BusinessDataServiceHelper.loadSingle(nckdBomid_a.getPkValue(), "pdm_mftbom");
                        DynamicObjectCollection copentry = nckdBomid_a.getDynamicObjectCollection("copentry");
                        if (copentry.size() > 0) {
                            for (int j = 0; j < copentry.size(); j++) {
                                DynamicObject copent = copentry.get(j);
                                if (copent.getDynamicObject("copentrymaterial") == null) {
                                    return;
                                }
                                if (copent.getDynamicObject("copentrymaterial").getPkValue().equals(entity.get(i).getDynamicObject("material").getPkValue())) {
                                    BigDecimal copentryqty = copent.getBigDecimal("copentryqty");
                                    this.getModel().setValue("nckd_yield", nckdYield.multiply(copentryqty), i);
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 选择物料自动带出相关信息
     */
    private void setMaterialToBOM() {
        TreeEntryGrid grid = this.getView().getControl("pom_planning_entry");
        DynamicObjectCollection entrys = this.getModel().getEntryEntity("pom_planning_entry");
        int rowIndex = this.getModel().getEntryCurrentRowIndex("pom_planning_entry");
        DynamicObject entry = entrys.get(rowIndex);
        DynamicObjectCollection subentryentity = entry.getDynamicObjectCollection("nckd_subentryentity");
        if (subentryentity.size() > 0) {
            subentryentity.clear();
            this.getView().updateView("nckd_subentryentity");
        }
        DynamicObject material = entry.getDynamicObject("material");
        if (material != null) {
            //查物料
            material = BusinessDataServiceHelper.loadSingle(material.getPkValue(), "bd_materialmftinfo");
            this.getModel().setValue("nckd_producedept", material.getDynamicObject("departmentorgid"));
            this.getModel().setValue("nckd_unit", material.getDynamicObject("mftunit"), rowIndex);
            //根据物料查bom
            DynamicObject mftbom = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material,copentry,entry,entry.entrymaterial,copentry.copentrymaterial,copentry.copentryunit,copentry.copentrytype,copentry.copentryqty",
                    new QFilter[]{new QFilter("material.id", QCP.equals, material.getPkValue())});
            if (mftbom == null) {
                if (entrys.size() > 1) {
                    for (int i = entrys.size() - 1; i > -1; i--) {
                        if (entry.getPkValue().equals(entrys.get(i).get("pid"))) {
                            this.getModel().deleteEntryRow("pom_planning_entry", i);
                        }
                    }
                }
                this.getModel().setValue("nckd_yield", null, rowIndex);
                this.getModel().setValue("nckd_producedept", null, rowIndex);
                this.getModel().setValue("nckd_bomid", null, rowIndex);
                this.getModel().setValue("nckd_unit", null, rowIndex);
                return;
            }
            if (mftbom != null) {
                this.getModel().setValue("nckd_bomid", mftbom.getPkValue(), rowIndex);
                DynamicObjectCollection copentrys = mftbom.getDynamicObjectCollection("copentry");
                if (copentrys.size() <= 0) {
                    if (entrys.size() > 1) {
                        for (int i = entrys.size() - 1; i > -1; i--) {
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
                for (int i = entrys.size() - 1; i > -1; i--) {
                    if (id.equals(entrys.get(i).get("pid"))) {
                        this.getModel().deleteEntryRow("pom_planning_entry", i);
                    }
                }

                for (int i = 0; i < copentrys.size(); i++) {
                    entryOperate.insertEntryRow("pom_planning_entry", rowIndex);
                    DynamicObject pomPlanningEntry = entryOperate.getEntryRowEntity("pom_planning_entry", rowIndex + i + 1);
                    DynamicObject copentrymaterial = copentrys.get(i).getDynamicObject("copentrymaterial") == null ? null :
                            BusinessDataServiceHelper.loadSingle(copentrys.get(i).getDynamicObject("copentrymaterial").getPkValue(), "bd_materialmftinfo");
                    if ("10720".equals(copentrys.get(i).getString("copentrytype"))) {
                        pomPlanningEntry.set("nckd_producttype", "A");
                    } else {
                        pomPlanningEntry.set("nckd_producttype", "B");
                    }
                    BigDecimal nckdYield = entrys.get(rowIndex).getBigDecimal("nckd_yield");
                    BigDecimal copentryqty = copentrys.get(i).getBigDecimal("copentryqty");
                    pomPlanningEntry.set("nckd_yield", nckdYield == null ? nckdYield : nckdYield.multiply(copentryqty));
                    pomPlanningEntry.set("nckd_producedept", copentrymaterial.getDynamicObject("departmentorgid"));
                    pomPlanningEntry.set("nckd_bomid", mftbom);
                    pomPlanningEntry.set("material", copentrymaterial);
                    pomPlanningEntry.set("nckd_unit", copentrys.get(i).getDynamicObject("copentryunit"));
                }
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
        DynamicObjectCollection entrys = this.getModel().getEntryEntity("pom_planning_entry");
        if (entrys.size() <= 0) {
            return;
        }
        DynamicObjectCollection subent = entrys.get(0).getDynamicObjectCollection("nckd_subentryentity");
        subent.clear();
        this.getView().updateView("nckd_subentryentity");
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        ListShowParameter showParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();
        DynamicObject org = (DynamicObject) this.getModel().getValue("org");
        if ("nckd_producedept".equals(name)) {
            DynamicObject[] orgrelation = BusinessDataServiceHelper.load("bos_org_orgrelation_dept", "id,fromorg,toorg", new QFilter[]{new QFilter("fromorg", QCP.equals, org.getPkValue())});
            List<Object> orgList = new ArrayList<>();
            if (orgrelation.length > 0){
                for (DynamicObject d : orgrelation) {
                    DynamicObject toorg = d.getDynamicObject("toorg");
                    DynamicObject dutyrelation = BusinessDataServiceHelper.loadSingle("bos_org_dutyrelation", "id,org,orgduty", new QFilter[]{new QFilter("org", QCP.equals, toorg.getPkValue())});
                    if (dutyrelation != null){
                        DynamicObject orgduty = dutyrelation.getDynamicObject("orgduty");
                        if (orgduty != null){
                            if ("4".equals(orgduty.getString("number"))) {
                                orgList.add(d.getDynamicObject("toorg").getPkValue());
                            }
                        }
                    }
                }
            }
            QFilter qFilter = new QFilter("id", QCP.in, orgList);
            showParameter.getListFilterParameter().setFilter(qFilter);
        }
    }

    @Override
    public void afterF7Select(AfterF7SelectEvent afterF7SelectEvent) {
        ListSelectedRowCollection list = afterF7SelectEvent.getListSelectedRowCollection();
        if (list.size() <= 1) {
            return;
        }
        DynamicObjectCollection entity = this.getModel().getEntryEntity("pom_planning_entry");
        int count = 1;
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                DynamicObject material = BusinessDataServiceHelper.loadSingle(list.get(i).getPrimaryKeyValue(), "bd_materialmftinfo");
                DynamicObject bom = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material,copentry",
                        new QFilter[]{new QFilter("material", QCP.equals, material.getPkValue())});
                if (bom != null) {
                    DynamicObjectCollection copentry = bom.getDynamicObjectCollection("copentry");
                    count = count + copentry.size();
                }
                continue;
            }
            DynamicObject material = BusinessDataServiceHelper.loadSingle(list.get(i).getPrimaryKeyValue(), "bd_materialmftinfo");
            DynamicObject bom = BusinessDataServiceHelper.loadSingle("pdm_mftbom", "id,material,copentry,copentry.copentrytype,copentry.copentrymaterial",
                    new QFilter[]{new QFilter("material", QCP.equals, material.getPkValue())});
            this.getModel().setValue("nckd_producttype", "C", count);
            this.getModel().setValue("nckd_bomid", bom, count);
            this.getModel().setValue("nckd_producedept", material.getDynamicObject("departmentorgid"), count);
            this.getModel().setValue("nckd_unit", material.getDynamicObject("mftunit"), count);
            this.getModel().setValue("material", material, count);
            count++;
        }
        for (int i = 0; i < entity.size(); i++) {
            DynamicObject dynamicObject = entity.get(i);
            if (i == 0) {
                continue;
            }
            if (!dynamicObject.get("nckd_producttype").equals("C")) {
                continue;
            }
            DynamicObject nckdBomid = dynamicObject.getDynamicObject("nckd_bomid");
            if (nckdBomid == null) {
                continue;
            }
            nckdBomid = BusinessDataServiceHelper.loadSingle(nckdBomid.getPkValue(), "pdm_mftbom");
            DynamicObjectCollection copentry = nckdBomid.getDynamicObjectCollection("copentry");
            if (copentry.size() < 1) {
                continue;
            }
            for (DynamicObject object : copentry) {
                int index = this.getModel().insertEntryRow("pom_planning_entry", i);
                DynamicObject pomPlanningEntry = this.getModel().getEntryRowEntity("pom_planning_entry", index);
                if ("10720".equals(object.getString("copentrytype"))) {
                    pomPlanningEntry.set("nckd_producttype", "A");
                } else {
                    pomPlanningEntry.set("nckd_producttype", "B");
                }
                DynamicObject copentrymaterial = object.getDynamicObject("copentrymaterial");
                copentrymaterial = BusinessDataServiceHelper.loadSingle(copentrymaterial.getPkValue(), "bd_materialmftinfo");
                pomPlanningEntry.set("nckd_producedept", copentrymaterial.getDynamicObject("departmentorgid"));
                pomPlanningEntry.set("nckd_bomid", nckdBomid);
                pomPlanningEntry.set("material", copentrymaterial);
                pomPlanningEntry.set("nckd_unit", copentrymaterial.getDynamicObject("mftunit"));
                this.getView().updateView();
            }
        }
    }
}
