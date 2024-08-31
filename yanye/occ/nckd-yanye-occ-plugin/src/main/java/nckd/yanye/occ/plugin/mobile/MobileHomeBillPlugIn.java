package nckd.yanye.occ.plugin.mobile;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.MobileFormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.control.Label;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.occ.ocbase.common.helper.CUserHelper;
import kd.occ.ocbase.common.status.Status;
import kd.occ.ocbase.common.util.CommonUtils;
import kd.occ.ocbase.common.util.DateUtil;
import kd.occ.ocbase.common.util.StringUtils;
import kd.occ.ocsaa.formplugin.OcsaaFormMobPlugin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class MobileHomeBillPlugIn extends OcsaaFormMobPlugin {
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"nckd_allderpanel", "nckd_cderpanel", "nckd_dderpanel", "nckd_fderpanel", "nckd_sderpanel"});
    }

    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        //获取不同状态订单的数量
        this.initSaleOrderCount();
    }

    public void propertyChanged(PropertyChangedArgs e) {
        if (e.getProperty().getName().equals("nckd_dateselect")) {
            this.initSaleOrderCount();
        }
        super.propertyChanged(e);
    }

    public void click(EventObject evt) {
        Control source = (Control) evt.getSource();
        Object orderDateSpan = this.getValue("nckd_dateselect");
        switch (source.getKey()) {
            case "nckd_allderpanel"://全部
                String all = this.getView().getPageCache().get("all");
                all = all.replace("[", "").replace("]", "").replace(" ", "");
                if (StringUtils.isNotNull(all) && !"0".equalsIgnoreCase(all)) {
                    MobileFormShowParameter mobileFormShowParameter = new MobileFormShowParameter();
                    mobileFormShowParameter.getOpenStyle().setShowType(ShowType.Floating);
                    mobileFormShowParameter.setFormId("ocdma_saleorder_list");
                    mobileFormShowParameter.setCustomParam("orderDateSpan", orderDateSpan);
                    mobileFormShowParameter.setCustomParam("tabap", "tp_all");
                    mobileFormShowParameter.setCustomParam("lable", "home");
                    this.getView().showForm(mobileFormShowParameter);
                }
                break;
            case "nckd_cderpanel"://代发货
                String c = this.getView().getPageCache().get("dfhCount");
                c = c.replace("[", "").replace("]", "").replace(" ", "");
                if (StringUtils.isNotNull(c) && !"0".equalsIgnoreCase(c)) {
                    MobileFormShowParameter mobileFormShowParameter = new MobileFormShowParameter();
                    mobileFormShowParameter.getOpenStyle().setShowType(ShowType.Floating);
                    mobileFormShowParameter.setFormId("ocdma_saleorder_list");
                    mobileFormShowParameter.setCustomParam("orderDateSpan", orderDateSpan);
                    mobileFormShowParameter.setCustomParam("tabap", "c");
                    mobileFormShowParameter.setCustomParam("lable", "home");
                    this.getView().showForm(mobileFormShowParameter);
                }
                break;
            case "nckd_dderpanel"://待收货
                String d = this.getView().getPageCache().get("dshCount");
                d = d.replace("[", "").replace("]", "").replace(" ", "");
                if (StringUtils.isNotNull(d) && !"0".equalsIgnoreCase(d)) {
                    MobileFormShowParameter mobileFormShowParameter = new MobileFormShowParameter();
                    mobileFormShowParameter.getOpenStyle().setShowType(ShowType.Floating);
                    mobileFormShowParameter.setFormId("ocdma_saleorder_list");
                    mobileFormShowParameter.setCustomParam("orderDateSpan", orderDateSpan);
                    mobileFormShowParameter.setCustomParam("tabap", "d");
                    mobileFormShowParameter.setCustomParam("lable", "home");
                    this.getView().showForm(mobileFormShowParameter);
                }
                break;
            case "nckd_fderpanel"://已完成
                String f = this.getView().getPageCache().get("ywcCount");
                f = f.replace("[", "").replace("]", "").replace(" ", "");
                if (StringUtils.isNotNull(f) && !"0".equalsIgnoreCase(f)) {
                    MobileFormShowParameter mobileFormShowParameter = new MobileFormShowParameter();
                    mobileFormShowParameter.getOpenStyle().setShowType(ShowType.Floating);
                    mobileFormShowParameter.setFormId("ocdma_saleorder_list");
                    mobileFormShowParameter.setCustomParam("orderDateSpan", orderDateSpan);
                    mobileFormShowParameter.setCustomParam("tabap", "f");
                    mobileFormShowParameter.setCustomParam("lable", "home");
                    this.getView().showForm(mobileFormShowParameter);
                }
                break;
            case "nckd_sderpanel"://代收款
                String s = this.getView().getPageCache().get("ywcCount");
                s = s.replace("[", "").replace("]", "").replace(" ", "");
                if (StringUtils.isNotNull(s) && !"0".equalsIgnoreCase(s)) {
                    MobileFormShowParameter mobileFormShowParameter = new MobileFormShowParameter();
                    mobileFormShowParameter.getOpenStyle().setShowType(ShowType.Floating);
                    mobileFormShowParameter.setFormId("ocdma_saleorder_list");
                    mobileFormShowParameter.setCustomParam("orderDateSpan", orderDateSpan);
                    mobileFormShowParameter.setCustomParam("tabap", "nckd_s");
                    mobileFormShowParameter.setCustomParam("lable", "home");
                    this.getView().showForm(mobileFormShowParameter);
                }


        }

    }

    //获取订单状态数量
    private void initSaleOrderCount() {
        //获取用户渠道权限
        List<Long> channelIds = CUserHelper.getAuthorizedChannelIdList();
        if (channelIds != null && !channelIds.isEmpty()) {
            String selectFields = String.join(",", "billno", "id", "billstatus", "paystatus");
            QFilter filter = new QFilter("billtypeid", "!=", 100001L);
            this.setDateFilter(filter);
            filter.and("orderchannelid", "in", channelIds);
            //查询订单数据
            DynamicObjectCollection orderInfoList = QueryServiceHelper.query("ocbsoc_saleorder", selectFields, filter.toArray());
            int all = 0;
            int dfhCount = 0;
            int dshCount = 0;
            int ywcCount = 0;
            int dskCount = 0;
            if (orderInfoList != null && !orderInfoList.isEmpty()) {
                all = orderInfoList.size();
                for (DynamicObject orderInfo : orderInfoList) {
                    String billstatus = orderInfo.getString("billstatus");
                    String paystatus = orderInfo.getString("paystatus");
                    //待发货
                    if (billstatus.equalsIgnoreCase("c")) {
                        dfhCount++;
                    }
                    //待收货
                    if (billstatus.equalsIgnoreCase("d") || billstatus.equalsIgnoreCase("e")) {
                        dshCount++;
                    }
                    //已完成
                    if (billstatus.equalsIgnoreCase("f")) {
                        ywcCount++;
                    }
                    //待收款 单据未拒签
                    if (!paystatus.equalsIgnoreCase("c") && !billstatus.equalsIgnoreCase("g")) {
                        dskCount++;
                    }
                }
            }

            //设置标签名称 显示对应数量
            Label channelorder = (Label) this.getControl("nckd_all");
            channelorder.setText(String.valueOf(all));
            Label nochannelorder = (Label) this.getControl("nckd_c");
            nochannelorder.setText(String.valueOf(dfhCount));
            Label orderCountLabel = (Label) this.getControl("nckd_d");
            orderCountLabel.setText(String.valueOf(dshCount));
            Label orderQtyLabel = (Label) this.getControl("nckd_f");
            orderQtyLabel.setText(String.valueOf(ywcCount));
            Label orderAmountLabel = (Label) this.getControl("nckd_s");
            orderAmountLabel.setText(String.valueOf(dskCount));
            this.getView().getPageCache().put("all", String.valueOf(all));
            this.getView().getPageCache().put("dfhCount", String.valueOf(dfhCount));
            this.getView().getPageCache().put("dshCount", String.valueOf(dshCount));
            this.getView().getPageCache().put("ywcCount", String.valueOf(ywcCount));
            this.getView().getPageCache().put("dskCount", String.valueOf(dskCount));
        }
    }

    //日期过滤
    private void setDateFilter(QFilter filter) {
        Object orderDateSpan = this.getValue("nckd_dateselect");
        String selectDate = "F";
        if (!CommonUtils.isNull(orderDateSpan)) {
            selectDate = orderDateSpan.toString();
        }
        switch (selectDate) {
            case "A"://最近七天
                filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(6L))));
                break;
            case "B"://本月
                filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.getFirstDayOfMonth()));
                break;
            case "C"://过去三个月
                filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(90L))));
                break;
            case "D"://前天
                filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(2L))));
                filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().minusDays(2L))));
                break;
            case "E"://昨天
                filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().minusDays(1L))));
                filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().minusDays(1L))));
                break;
            case "G"://明天
                filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.asDate(LocalDateTime.now().plusDays(1L))));
                filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.asDate(LocalDateTime.now().plusDays(1L))));
                break;
            case "F"://今天
            default:
                filter.and("orderdate", ">=", DateUtil.getDayFirst(DateUtil.getNowDate()));
                filter.and("orderdate", "<", DateUtil.getDayLast(DateUtil.getNowDate()));
        }


    }

}
