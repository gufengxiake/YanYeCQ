package nckd.yanye.occ.plugin.mobile;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.db.DB;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.*;
import kd.bos.form.cardentry.CardEntry;
import kd.bos.form.container.Tab;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.TabSelectEvent;
import kd.bos.form.control.events.TabSelectListener;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.operate.AbstractOperate;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.BillList;
import kd.bos.list.MobileSearch;
import kd.bos.orm.query.QFilter;
import kd.bos.orm.query.fulltext.QMatches;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.occ.ocbase.common.util.CommonUtils;
import kd.occ.ocbase.common.util.DateUtil;
import kd.occ.ocbase.common.util.DynamicObjectUtils;
import kd.occ.ocbase.common.util.MobileControlUtils;
import kd.occ.ocdma.business.order.SaleOrderHelper;
import kd.occ.ocdma.formplugin.OcdmaFormMobPlugin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/*
 * 要货订单移动列表插件
 * 表单标识：nckd_ocdma_saleorder_list
 * author:吴国强 2024-08-26
 */
public class MobileSalOrderListPlugIn extends OcdmaFormMobPlugin implements TabSelectListener {

    private static final String[] billStatusArray = new String[]{"D", "E"};
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addTabSelectListener(this, new String[]{"tabap"});
        this.addClickListeners(new String[]{"nckd_unsign"});
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        if (e.getOperateKey().equals("unsign")) {
            BillList billList = this.getControl("billlistap");
            Object id = billList.getCurrentSelectedRowInfo().getPrimaryKeyValue();
            //获取下游单据
            Map<String, HashSet<Long>> targetBillIds = BFTrackerServiceHelper.findTargetBills("ocbsoc_saleorder", new Long[]{(Long) id});
            // 从所有下游单中寻找需要的
            HashSet<Long> botpbill1_Ids = new HashSet<>();
            String botpbill1_EntityNumber = "ocbsoc_delivery_record";//要货记录
            if (targetBillIds.containsKey(botpbill1_EntityNumber)) {
                botpbill1_Ids = targetBillIds.get(botpbill1_EntityNumber);
            } else {
                this.getView().showErrorNotification("当前要货订单还未生成发货记录，无法拒签！");
                return;
            }
            for (Long deliveryBillId : botpbill1_Ids) {
                DynamicObject dataObject = BusinessDataServiceHelper.loadSingle(deliveryBillId, botpbill1_EntityNumber);
                String billstatus = dataObject.getString("billstatus");
                if ("C".equalsIgnoreCase(billstatus)) {
                    this.getView().showErrorNotification("当前要货订单已签收，无法拒签！");
                    return;
                } else if ("E".equalsIgnoreCase(billstatus)) {
                    this.getView().showErrorNotification("当前要货订单已拒签，无法拒签！");
                    return;
                }
                //执行发货记录拒签
                OperationResult unsign= OperationServiceHelper.executeOperate("unsign", "ocbsoc_delivery_record", new DynamicObject[]{dataObject});
                if(unsign.isSuccess()){
                    //刷新列表
                    billList.refresh();
                    this.getView().showSuccessNotification("拒签成功！");
                }
            }

        }
    }



    public void tabSelected(TabSelectEvent tabSelectEvent) {
        switch (((Control)tabSelectEvent.getSource()).getKey().toLowerCase()) {
            case "tabap":
                BillList billList = (BillList)this.getControl("billlistap");
                MobileControlUtils.BillListRefresh(billList, new QFilter[]{this.getOrderFilter(tabSelectEvent.getTabKey())});
            default:
        }
    }

    public void propertyChanged(PropertyChangedArgs e) {
        switch (e.getProperty().getName()) {
            case "enddate":
            case "startdate":
            case "orderdatespan":
                this.refreshBillList();
            default:
        }
    }

    private void refreshBillList() {
        BillList billList = (BillList)this.getControl("billlistap");
        Tab tab = (Tab)this.getControl("tabap");
        String curTabKey = tab.getCurrentTab();
        MobileControlUtils.BillListRefresh(billList, new QFilter[]{this.getOrderFilter(curTabKey)});
    }
    private QFilter getOrderFilter(String key) {
        QFilter filter = SaleOrderHelper.getOrderChannelFilter();
        filter.and("billtypeid", "!=", 100001L);
        switch (key) {
            case "tp_all":
                break;
            case "a":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "b":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "c":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "d":
                filter.and("billstatus", "in", billStatusArray);
                break;
            case "f":
                filter.and("billstatus", "=", key.toUpperCase());
                break;
            case "nckd_s":
                filter.and("paystatus","!=","C");//收款状态不等于已收款
                filter.and("billstatus","!=","G");//单据状态不为拒签
        }

        MobileSearch search = (MobileSearch)this.getControl("ordersearch");
        String searchText = search.getText();
        if (searchText != null && !"".equals(searchText.trim())) {
            filter = filter.and(QMatches.ftlike(new String[]{searchText}, new String[]{"billno,itementry.itemid.name,itementry.itemid.number"}));
        }

        this.setDateFilter(filter);
        Object fromFormId = this.getParameter("fromFormId");
        if ("ocsaa_channel".equals(fromFormId)) {
            long channelId = (Long)this.getParameter("channelId");
            filter.and("orderchannelid", "=", channelId);
        }

        return filter;
    }
    private void setDateFilter(QFilter filter) {
        Date startDate = (Date)this.getValue("startdate");
        Date endDate = (Date)this.getValue("enddate");
        if (startDate != null && endDate != null) {
            filter.and("orderdate", ">=", DateUtil.getDayFirst(startDate));
            filter.and("orderdate", "<=", DateUtil.getDayLast(endDate));
        } else {
            Object orderDateSpan = this.getValue("orderdatespan");
            String selectDate = "F";
            if (!CommonUtils.isNull(orderDateSpan)) {
                selectDate = orderDateSpan.toString();
            }

            switch (selectDate) {
                case "A":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(6L))));
                    break;
                case "B":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.getFirstDayOfMonth()));
                    break;
                case "C":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(90L))));
                    break;
                case "D":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(2L))));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().minusDays(2L))));
                    break;
                case "E":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(1L))));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().minusDays(1L))));
                    break;
                case "G":
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().plusDays(1L))));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().plusDays(1L))));
                    break;
                case "F":
                default:
                    filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.getNowDate()));
                    filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.getNowDate()));
            }

        }
    }

}