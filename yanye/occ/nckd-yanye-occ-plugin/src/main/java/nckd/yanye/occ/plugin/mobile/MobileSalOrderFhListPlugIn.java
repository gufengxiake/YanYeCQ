package nckd.yanye.occ.plugin.mobile;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.container.Tab;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.TabSelectEvent;
import kd.bos.form.control.events.TabSelectListener;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.BillList;
import kd.bos.list.MobileSearch;
import kd.bos.list.events.ListRowClickEvent;
import kd.bos.list.events.ListRowClickListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.orm.query.fulltext.QMatches;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.occ.ocbase.common.util.MobileControlUtils;
import kd.occ.ocbase.common.util.StringUtils;
import kd.occ.ocdma.business.order.SaleOrderHelper;

import javax.xml.crypto.Data;
import java.time.LocalDateTime;
import java.util.*;

public class MobileSalOrderFhListPlugIn extends AbstractMobFormPlugin implements TabSelectListener,ListRowClickListener {

    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addTabSelectListener(this, new String[]{"nckd_tabap"});
        BillList billList = (BillList)this.getControl("nckd_billlistap");
        billList.addListRowClickListener(this);
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        switch (e.getProperty().getName()) {
            case "nckd_ywy":
            case "nckd_regiongroup":
            case "nckd_deliveryman":
                this.refreshBillList();
            default:
        }
    }

    public void listRowClick(ListRowClickEvent evt) {
        BillList billList = (BillList)this.getControl("nckd_billlistap");
        int row= billList.getSelectedRows().getPrimaryKeyValues().length;
        this.getModel().setValue("nckd_payamount",row);

    }

    public void tabSelected(TabSelectEvent tabSelectEvent) {
        switch (((Control)tabSelectEvent.getSource()).getKey().toLowerCase()) {
            case "nckd_tabap":
                BillList billList = (BillList)this.getControl("nckd_billlistap");
                MobileControlUtils.BillListRefresh(billList, new QFilter[]{this.getOrderFilter(tabSelectEvent.getTabKey())});
            default:
        }
    }

    public void afterCreateNewData(EventObject e) {
        //this.getModel().setValue("nckd_payamount",0);
        this.refreshBillList();
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        if (e.getOperateKey().equals("fh")) {
            BillList billList = this.getControl("nckd_billlistap");
            Object[] ids = billList.getSelectedRows().getPrimaryKeyValues();
            Long[] longArray = new Long[ids.length];
            List<DynamicObject> updateData = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] instanceof Long) {
                    longArray[i] = (Long) ids[i];
                } else {
                    longArray[i] = null;
                }
                DynamicObject saleorder = BusinessDataServiceHelper.loadSingle(ids[i], "ocbsoc_saleorder");
                if (saleorder != null) {
                    saleorder.set("nckd_isdelivery", "1");//安排发货
                    saleorder.set("nckd_deliverydate", new Date());//安排发货日期
                    updateData.add(saleorder);
                }
            }
            SaveServiceHelper.update(updateData.toArray(new DynamicObject[0]));
            if (ids.length > 0) {
                Map<String, HashSet<Long>> targetBills = BFTrackerServiceHelper.findTargetBills("ocbsoc_saleorder", longArray);
                String botpbill1_EntityNumber = "ocococ_deliveryorder";//发货单

                if (targetBills.containsKey(botpbill1_EntityNumber)) {
                    HashSet<Long> botpbill1_Ids = targetBills.get(botpbill1_EntityNumber);
                    if (!botpbill1_Ids.isEmpty()) {
                        List<DynamicObject> updatedeliverData = new ArrayList<>();
                        for (Long pkId : botpbill1_Ids) {
                            DynamicObject deliveryorder = BusinessDataServiceHelper.loadSingle(pkId, botpbill1_EntityNumber);
                            if (deliveryorder != null) {
                                deliveryorder.set("nckd_isdelivery", true);//安排发货
                                deliveryorder.set("nckd_deliverydate", new Date());//安排发货日期
                                updatedeliverData.add(deliveryorder);
                            }
                        }
                        if (!updatedeliverData.isEmpty()) {
                            SaveServiceHelper.update(updatedeliverData.toArray(new DynamicObject[0]));

                            this.refreshBillList();
                            this.getView().showSuccessNotification("安排发货完成！");
                        }
                    }

                }
            } else {
                this.getView().showErrorNotification("请至少选择一行数据！");
            }
        }
    }


    private void refreshBillList() {
        BillList billList = (BillList) this.getControl("nckd_billlistap");
        Tab tab = (Tab)this.getControl("nckd_tabap");
        String curTabKey = tab.getCurrentTab();
        MobileControlUtils.BillListRefresh(billList, new QFilter[]{this.getOrderFilter(curTabKey)});
    }

    private QFilter getOrderFilter(String key) {
        QFilter filter = SaleOrderHelper.getOrderChannelFilter();
        filter.and("billtypeid", "!=", 100001L);
        filter.and("billstatus", "=", "C");
        if(key.equalsIgnoreCase("nckd_c")){
            filter.and("nckd_isdelivery", "=", "0");
        }else if(key.equalsIgnoreCase("nckd_d")){
            filter.and("nckd_isdelivery", "=", "1");
        }
        //业务员
        DynamicObject ywy = (DynamicObject) this.getModel().getValue("nckd_ywy");
        if (ywy != null) {
            Object ywyId = ywy.getPkValue();
            filter.and("nckd_salerid.id", QCP.equals, ywyId);
        }
        //销售片区
        DynamicObject pq = (DynamicObject) this.getModel().getValue("nckd_regiongroup");
        if (pq != null) {
            Object pqId = pq.getPkValue();
            filter.and("orderchannelid.nckd_regiongroup.id", QCP.equals, pqId);
        }
        //配送员
        DynamicObject psy = (DynamicObject) this.getModel().getValue("nckd_deliveryman");
        if (psy != null) {
            Object psyId = psy.getPkValue();
            filter.and("nckd_deliveryman.id", QCP.equals, psyId);
        }
        //this.setDateFilter(filter);
        return filter;
    }

    public void addTabSelectListener(TabSelectListener form, String... tabName) {
        Tab tab = null;
        int i = 0;

        for(int len = tabName.length; i < len; ++i) {
            tab = (Tab)this.getControl(tabName[i]);
            if (tab != null) {
                tab.addTabSelectListener(form);
            }
        }

    }
}
