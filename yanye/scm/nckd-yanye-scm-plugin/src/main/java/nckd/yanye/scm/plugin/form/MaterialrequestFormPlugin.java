package nckd.yanye.scm.plugin.form;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.exception.KDBizException;
import kd.bos.form.*;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.bos.trace.TraceSpan;
import kd.bos.trace.Tracer;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :制造云-生产任务管理-物料申请单
 * Description      :物料申请单表单插件
 *
 * @author : yaosijie
 * @date : 2024/8/16
 */
public class MaterialrequestFormPlugin extends AbstractFormPlugin implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        BasedataEdit administrativeorgEdit = this.getControl("nckd_administrativeorg");
        administrativeorgEdit.addBeforeF7SelectListener(this);

        this.addItemClickListeners("nckd_advcontoolbarap");
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

        QFilter qFilter = new QFilter("number", QCP.equals, "1");
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bos_adminorg", new QFilter[]{qFilter});
        //创建组织默认江盐集团
        this.getModel().setValue("nckd_createorg", dynamicObject);

        // 当前用户所属部门
        long userMainOrgId = UserServiceHelper.getUserMainOrgId(UserServiceHelper.getCurrentUserId());
        // 发起部门
        this.getModel().setValue("nckd_administrativeorg", userMainOrgId);
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);

        FormOperate formOperate = (FormOperate) args.getSource();
        String operateKey = formOperate.getOperateKey();
        if ("save".equals(operateKey) || "submit".equals(operateKey)) {
            boolean flag = this.saveOrSubmitVerify();
            if (flag) {
                this.showMessage(args, operateKey);
            }
        }
    }

    private void showMessage(BeforeDoOperationEventArgs args, String operateKey) {
        // 判断是否处理过
        String isDealed = this.getView().getPageCache().get("isDealed");
        if (!"true".equals(isDealed)) {
            // 取消原来的操作
            args.setCancel(true);
            // 在用户点击确认框上的按钮后，系统会调用confirmCallBack方法
            ConfirmCallBackListener confirmCallBackListener = new ConfirmCallBackListener(operateKey, this);
            // 设置页面确认框，参数为：标题，选项框类型，回调监听
            this.getView().showConfirm("物料名称重复！", MessageBoxOptions.YesNo, confirmCallBackListener);
            // 只执行一次
            this.getView().getPageCache().put("isDealed", "true");
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);

        String callBackId = messageBoxClosedEvent.getCallBackId();
        //判断回调参数id
        if ("save".equals(callBackId) || "submit".equals(callBackId)) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                this.getView().invokeOperation(callBackId);
            } else if (MessageBoxResult.No.equals(messageBoxClosedEvent.getResult())) {
                // 点击否也清除
                this.getView().getPageCache().remove("isDealed");
            }
        }
    }

    /**
     * 保存或提交时校验
     */
    private Boolean saveOrSubmitVerify() {
        final Boolean[] flag = {false};

        String billno = (String) this.getModel().getValue("billno");
        // 物料分录
        DynamicObjectCollection materialentries = this.getModel().getEntryEntity("nckd_materialentries");

        // 校验物料名称、规格、型号，组合唯一性
        // 根据物料名称、规格、型号分组
        Map<String, List<DynamicObject>> listMap = materialentries.stream().collect(Collectors.groupingBy(m -> m.getString("nckd_materialname") + m.getString("nckd_specifications") + m.getString("nckd_model")));
        listMap.keySet().stream().forEach(t -> {
            if (listMap.get(t).size() > 1) {
                throw new KDBizException("物料名称、规格、型号，组合需唯一!");
            }
        });

        // 校验物料名称唯一性
        // 根据物料名称分组
        Map<String, List<DynamicObject>> listMap2 = materialentries.stream().collect(Collectors.groupingBy(m -> m.getString("nckd_materialname")));
        listMap2.keySet().stream().forEach(t -> {
            if (listMap2.get(t).size() > 1) {
                flag[0] = true;
            }
        });

        materialentries.stream().forEach(m -> {
            String materialname = m.getString("nckd_materialname");//物料名称
            String specifications = m.getString("nckd_specifications");//规格
            String model = m.getString("nckd_model");//型号

            // 校验物料名称、规格、型号，组合唯一性
            // 物料
            QFilter qFilter1 = new QFilter("name", QCP.equals, materialname)
                    .and("modelnum", QCP.equals, specifications)
                    .and("nckd_model", QCP.equals, model);
            boolean exists1 = QueryServiceHelper.exists("bd_material", qFilter1.toArray());

            // 物料申请单
            QFilter qFilter2 = new QFilter("nckd_materialentries.nckd_materialname", QCP.equals, materialname)
                    .and("nckd_materialentries.nckd_specifications", QCP.equals, specifications)
                    .and("nckd_materialentries.nckd_model", QCP.equals, model)
                    .and("billno", QCP.not_equals, billno);
            int size1 = QueryServiceHelper.query("nckd_materialrequest", "id", qFilter2.toArray()).size();

            // 物料维护单
            QFilter qFilter3 = new QFilter("nckd_altermaterialname", QCP.equals, materialname)
                    .and("nckd_alterspecificat", QCP.equals, specifications)
                    .and("nckd_altermodel", QCP.equals, model)
                    .and("nckd_materialmaintunit", QCP.equals, "updateinfo");
            boolean exists2 = QueryServiceHelper.exists("nckd_materialmaintenan", qFilter3.toArray());

            // 物料名称、规格、型号，需要组合校验唯一性
            if (exists1 || size1 > 0 || exists2) {
                throw new KDBizException("物料名称、规格、型号，组合需唯一!");
            }

            // 校验物料名称唯一性
            // 物料
            QFilter qFilter4 = new QFilter("name", QCP.equals, materialname);
            boolean exists3 = QueryServiceHelper.exists("bd_material", qFilter4.toArray());

            // 物料申请单
            QFilter qFilter5 = new QFilter("nckd_materialentries.nckd_materialname", QCP.equals, materialname)
                    .and("billno", QCP.not_equals, billno);
            int size2 = QueryServiceHelper.query("nckd_materialrequest", "id", qFilter5.toArray()).size();

            // 物料维护单
            QFilter qFilter6 = new QFilter("nckd_altermaterialname", QCP.equals, materialname)
                    .and("nckd_materialmaintunit", QCP.equals, "updateinfo");
            boolean exists4 = QueryServiceHelper.exists("nckd_materialmaintenan", qFilter6.toArray());

            // 物料名称、规格、型号，需要组合校验唯一性
            if (exists3 || size2 > 0 || exists4) {
                flag[0] = true;
            }
        });

        return flag[0];
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        String itemKey = evt.getItemKey();
        if (itemKey.equals("nckd_btn_addrow")) {
            int index = this.getModel().getEntryCurrentRowIndex("nckd_materialentries");
            DynamicObject rowEntity = this.getModel().getEntryRowEntity("nckd_materialentries", index);
            DynamicObject baseUnit = rowEntity.getDynamicObject("nckd_baseunit");
            if (baseUnit == null) {
                this.getView().showErrorNotification("请先设置基本单位。");
            } else {
                this.showUnitList();
            }
        }
    }

    private void showUnitList() {
        int index = this.getModel().getEntryCurrentRowIndex("nckd_materialentries");
        DynamicObject rowEntity = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_materialentries").get(index);
        Object baseunit = rowEntity.get("nckd_baseunit");
        // 辅助单位
        DynamicObject auxptyunit = rowEntity.getDynamicObject("nckd_auxptyunit");

        ListShowParameter lsp = ShowFormHelper.createShowListForm("bd_measureunits", true, 2);
        int rowCount = this.getModel().getEntryRowCount("nckd_unitentryentity");
        Long[] muids = new Long[rowCount];
        if (auxptyunit != null) {
            muids = new Long[rowCount + 1];
        }

        for (int i = 0; i < rowCount; ++i) {
            DynamicObject mu = (DynamicObject) this.getModel().getValue("nckd_measureunitid", i);
            muids[i] = mu.getLong("id");
        }

        if (auxptyunit != null) {
            muids[rowCount] = auxptyunit.getLong("id");
        }

        lsp.setSelectedRows(muids);
        lsp.setFormId("bos_treelistf7");
        lsp.setCloseCallBack(new CloseCallBack(this, "selectMUs"));
        lsp.getListFilterParameter().getQFilters().add(new QFilter("enable", "=", "1"));
        lsp.getListFilterParameter().getQFilters().add(new QFilter("id", "!=", ((DynamicObject) baseunit).getPkValue()));
        this.getView().showForm(lsp);
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        super.closedCallBack(e);

        String actionId = e.getActionId();
        if ("selectMUs".equals(actionId)) {
            TraceSpan span = Tracer.create("MaterialmaintenanFormPlugin", "showUintInfo");
            Throwable var4 = null;

            try {
                this.showUintInfo(e);
            } catch (Throwable var13) {
                var4 = var13;
                throw var13;
            } finally {
                if (span != null) {
                    if (var4 != null) {
                        try {
                            span.close();
                        } catch (Throwable var12) {
                            var4.addSuppressed(var12);
                        }
                    } else {
                        span.close();
                    }
                }

            }
        }
    }

    private void showUintInfo(ClosedCallBackEvent e) {
        ListSelectedRowCollection col = (ListSelectedRowCollection) e.getReturnData();
        if (col != null && col.size() != 0) {
            int rowCount = this.getModel().getEntryRowCount("nckd_unitentryentity");
            Set<Long> muids = new HashSet(rowCount);

            DynamicObject baseUnit;
            for (int i = 0; i < rowCount; ++i) {
                baseUnit = (DynamicObject) this.getModel().getValue("nckd_measureunitid", i);
                muids.add(baseUnit.getLong("id"));
            }

            List<Long> muidsToAdd = new ArrayList();
            Iterator var15 = col.iterator();

            while (var15.hasNext()) {
                ListSelectedRow row = (ListSelectedRow) var15.next();
                Long muid = (Long) row.getPrimaryKeyValue();
                if (!muids.contains(muid)) {
                    muidsToAdd.add(muid);
                }
            }

            if (muidsToAdd.size() > 0) {
                this.getModel().batchCreateNewEntryRow("nckd_unitentryentity", muidsToAdd.size());

                int index = this.getModel().getEntryCurrentRowIndex("nckd_materialentries");
                DynamicObject rowEntity = this.getModel().getEntryRowEntity("nckd_materialentries", index);
                baseUnit = rowEntity.getDynamicObject("nckd_baseunit");

                this.getModel().beginInit();

                for (int i = 0; i < muidsToAdd.size(); ++i) {
                    long srcmuid = muidsToAdd.get(i);
                    long desmuid = baseUnit.getLong("id");
                    this.getModel().setValue("nckd_measureunitid", srcmuid, i + rowCount);
                    this.getModel().setValue("nckd_desmuid", desmuid, i + rowCount);
                    DynamicObject queryOne = BusinessDataServiceHelper.loadSingleFromCache("bd_measureunits", "precision", new QFilter[]{new QFilter("id", "=", srcmuid)});
                    if (queryOne != null) {
                        this.getModel().setValue("nckd_precision", queryOne.get("precision"), i + rowCount);
                    }

                    DynamicObject unitConvert = this.getUnitConvert(srcmuid, desmuid);
                    if (unitConvert != null) {
                        this.getModel().setValue("nckd_numerator", unitConvert.get("numerator"), i + rowCount);
                        this.getModel().setValue("nckd_denominator", unitConvert.get("denominator"), i + rowCount);
                    }
                }

                this.getModel().endInit();
                this.getView().updateView("nckd_unitentryentity");
            }

        }
    }

    private DynamicObject getUnitConvert(long srcmuid, long desmuid) {
        DynamicObject result = null;
        QFilter[] filters = new QFilter[]{new QFilter("srcmuid", "=", srcmuid), new QFilter("desmuid", "=", desmuid)};
        result = BusinessDataServiceHelper.loadSingleFromCache("bd_measureunitconv", "numerator, denominator, converttype", filters);
        return result;
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);

        String name = e.getProperty().getName();
        if (StringUtils.equals("nckd_materialattribute", name)) {// 物料属性
            ChangeData changeData = e.getChangeSet()[0];
            String materialattribute = (String) changeData.getNewValue();
            if (StringUtils.equals("1", materialattribute)) {
                // 自制
                this.getModel().setValue("nckd_selfmade", "1", changeData.getRowIndex());
                // 采购
                this.getModel().setValue("nckd_purchase", "0", changeData.getRowIndex());
            } else if (StringUtils.equals("2", materialattribute)) {
                // 自制
                this.getModel().setValue("nckd_selfmade", "0", changeData.getRowIndex());
                // 采购
                this.getModel().setValue("nckd_purchase", "1", changeData.getRowIndex());
            }
            this.getView().updateView();
        } else if (StringUtils.equals("nckd_selfmaterialtype", name)) {// 自制物料类型
            ChangeData changeData = e.getChangeSet()[0];
            String selfmaterialtype = (String) changeData.getNewValue();
            if (StringUtils.equals("1", selfmaterialtype)) {
                // 销售
                this.getModel().setValue("nckd_sales", "1", changeData.getRowIndex());
            } else {
                // 销售
                this.getModel().setValue("nckd_sales", "0", changeData.getRowIndex());
            }
            this.getView().updateView();
        } else if (StringUtils.equals("nckd_baseunit", name)) {// 基本单位
            ChangeData changeData = e.getChangeSet()[0];
            DynamicObject baseunit = (DynamicObject) changeData.getNewValue();
            DynamicObject rowEntity = this.getModel().getEntryRowEntity("nckd_materialentries", changeData.getRowIndex());
            if (baseunit != null) {
                this.setUnitentryentity(baseunit, rowEntity);
            }

            // 辅助单位
            DynamicObject auxptyunit = rowEntity.getDynamicObject("nckd_auxptyunit");
            if (baseunit != null && auxptyunit != null) {
                // 子单据体
                DynamicObjectCollection nckdUnitentryentity = rowEntity.getDynamicObjectCollection("nckd_unitentryentity");
                long count = nckdUnitentryentity.stream()
                        .filter(t -> t.getDynamicObject("nckd_measureunitid").getLong("id") == auxptyunit.getLong("id") && t.getDynamicObject("nckd_desmuid").getLong("id") == baseunit.getLong("id"))
                        .count();
                if (count == 0) {
                    this.insertUnitentryentity(baseunit, auxptyunit);
                }
            }
            this.getView().updateView("nckd_unitentryentity");
        } else if (StringUtils.equals("nckd_auxptyunit", name)) {// 辅助单位
            ChangeData changeData = e.getChangeSet()[0];
            DynamicObject auxptyunit = (DynamicObject) changeData.getNewValue();

            DynamicObject rowEntity = this.getModel().getEntryRowEntity("nckd_materialentries", changeData.getRowIndex());
            // 基本单位
            DynamicObject baseunit = rowEntity.getDynamicObject("nckd_baseunit");

            if (baseunit != null && auxptyunit != null) {
                // 变更前辅助单位
                DynamicObject oldAuxptyunit = (DynamicObject) changeData.getOldValue();
                if (oldAuxptyunit != null) {
                    // 子单据体
                    DynamicObjectCollection nckdUnitentryentity = rowEntity.getDynamicObjectCollection("nckd_unitentryentity");
                    DynamicObject dynamicObject = nckdUnitentryentity.stream()
                            .filter(t -> t.getDynamicObject("nckd_measureunitid").getLong("id") == oldAuxptyunit.getLong("id") && t.getDynamicObject("nckd_desmuid").getLong("id") == baseunit.getLong("id"))
                            .findFirst().orElse(null);
                    if (dynamicObject != null) {
                        // 辅助单位
                        dynamicObject.set("nckd_measureunitid", auxptyunit);
                        // 获取单位换算系数
                        DynamicObject unitConvert = this.getUnitConvert(auxptyunit.getLong("id"), baseunit.getLong("id"));
                        if (unitConvert != null) {
                            dynamicObject.set("nckd_numerator", unitConvert.get("numerator"));
                            dynamicObject.set("nckd_denominator", unitConvert.get("denominator"));
                        }
                    } else {
                        this.insertUnitentryentity(baseunit, auxptyunit);
                    }
                } else {
                    this.insertUnitentryentity(baseunit, auxptyunit);
                }
            }
            this.getView().updateView("nckd_unitentryentity");
        }
    }

    /**
     * @param baseunit  基本单位
     * @param rowEntity 选中的行数据
     */
    private void setUnitentryentity(DynamicObject baseunit, DynamicObject rowEntity) {
        // 获取子单据数据
        DynamicObjectCollection nckdUnitentryentity = rowEntity.getDynamicObjectCollection("nckd_unitentryentity");
        nckdUnitentryentity.stream().forEach(t -> {
            t.set("nckd_desmuid", baseunit);

            // 获取单位换算系数
            DynamicObject unitConvert = this.getUnitConvert(t.getDynamicObject("nckd_measureunitid").getLong("id"), baseunit.getLong("id"));
            if (unitConvert != null) {
                t.set("nckd_numerator", unitConvert.get("numerator"));
                t.set("nckd_denominator", unitConvert.get("denominator"));
            }
        });
    }

    private void insertUnitentryentity(DynamicObject baseunit, DynamicObject auxptyunit) {
        int rowCount = this.getModel().getEntryRowCount("nckd_unitentryentity");
        this.getModel().batchCreateNewEntryRow("nckd_unitentryentity", 1);

        this.getModel().beginInit();

        long srcmuid = auxptyunit.getLong("id");
        long desmuid = baseunit.getLong("id");
        this.getModel().setValue("nckd_measureunitid", srcmuid, 0 + rowCount);
        this.getModel().setValue("nckd_desmuid", desmuid, 0 + rowCount);

        // 源单位精度
        DynamicObject queryOne = BusinessDataServiceHelper.loadSingleFromCache("bd_measureunits", "precision", new QFilter[]{new QFilter("id", "=", srcmuid)});
        if (queryOne != null) {
            this.getModel().setValue("nckd_precision", queryOne.get("precision"), 0 + rowCount);
        }

        // 获取单位换算系数
        DynamicObject unitConvert = this.getUnitConvert(srcmuid, desmuid);
        if (unitConvert != null) {
            this.getModel().setValue("nckd_numerator", unitConvert.get("numerator"), 0 + rowCount);
            this.getModel().setValue("nckd_denominator", unitConvert.get("denominator"), 0 + rowCount);
        }

        this.getModel().endInit();
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        ListShowParameter showParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();
        if (name.equals("nckd_administrativeorg")) {
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            if (org == null) {
                throw new KDBizException("请先选择申请组织");
            }

            List<Long> longs = new ArrayList<>();
            longs.add(org.getLong("id"));
            List<Long> allSubordinateOrgIds = OrgUnitServiceHelper.getAllSubordinateOrgs("01", longs, true);

            QFilter qFilter = new QFilter("belongcompany", QCP.in, allSubordinateOrgIds);
            showParameter.getListFilterParameter().setFilter(qFilter);
        }
    }
}
