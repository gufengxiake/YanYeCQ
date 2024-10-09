package nckd.yanye.scm.plugin.form;


import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.trace.TraceSpan;
import kd.bos.trace.Tracer;

import java.util.*;

/**
 * Module           :制造云-生产任务管理-物料申请单
 * Description      :物料申请单表单插件
 *
 * @author : yaosijie
 * @date : 2024/8/16
 */
public class MaterialrequestFormPlugin extends AbstractFormPlugin {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addItemClickListeners("nckd_advcontoolbarap");
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        QFilter qFilter = new QFilter("number", QCP.equals,"1");
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bos_adminorg",new QFilter[]{qFilter});
        //创建组织默认江盐集团
        this.getModel().setValue("nckd_createorg",dynamicObject);
        this.getModel().setValue("nckd_administrativeorg", RequestContext.get().getOrgId());
        super.afterCreateNewData(e);
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        String itemKey = evt.getItemKey();
        if (itemKey.equals("nckd_btn_addrow")) {
            this.showUnitList();
        }
    }

    private void showUnitList() {
        int index = this.getModel().getEntryCurrentRowIndex("nckd_materialentries");
        DynamicObject rowEntity = this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_materialentries").get(index);
        Object baseunit = rowEntity.get("nckd_baseunit");

        ListShowParameter lsp = ShowFormHelper.createShowListForm("bd_measureunits", true, 2);
        int rowCount = this.getModel().getEntryRowCount("nckd_unitentryentity");
        Long[] muids = new Long[rowCount];

        for(int i = 0; i < rowCount; ++i) {
            DynamicObject mu = (DynamicObject)this.getModel().getValue("nckd_measureunitid", i);
            muids[i] = mu.getLong("id");
        }

        lsp.setSelectedRows(muids);
        lsp.setFormId("bos_treelistf7");
        lsp.setCloseCallBack(new CloseCallBack(this, "selectMUs"));
        lsp.getListFilterParameter().getQFilters().add(new QFilter("enable", "=", "1"));
        lsp.getListFilterParameter().getQFilters().add(new QFilter("id", "!=", ((DynamicObject)baseunit).getPkValue()));
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
        ListSelectedRowCollection col = (ListSelectedRowCollection)e.getReturnData();
        if (col != null && col.size() != 0) {
            int rowCount = this.getModel().getEntryRowCount("nckd_unitentryentity");
            Set<Long> muids = new HashSet(rowCount);

            DynamicObject baseUnit;
            for(int i = 0; i < rowCount; ++i) {
                baseUnit = (DynamicObject)this.getModel().getValue("nckd_measureunitid", i);
                muids.add(baseUnit.getLong("id"));
            }

            List<Long> muidsToAdd = new ArrayList();
            Iterator var15 = col.iterator();

            while(var15.hasNext()) {
                ListSelectedRow row = (ListSelectedRow)var15.next();
                Long muid = (Long)row.getPrimaryKeyValue();
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

                for(int i = 0; i < muidsToAdd.size(); ++i) {
                    long srcmuid = (Long)muidsToAdd.get(i);
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
}
