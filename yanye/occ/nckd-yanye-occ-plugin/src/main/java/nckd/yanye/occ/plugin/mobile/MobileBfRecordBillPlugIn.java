package nckd.yanye.occ.plugin.mobile;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.control.Control;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang.StringUtils;
import winkey.hmua.crm.sfa.common.utils.WinkeyUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;

/*
 * 渠道拜访更新最近拜访日期
 * 表单标识：nckd_sfa_bf_record
 * author:吴国强 2024-10-16
 */
public class MobileBfRecordBillPlugIn extends AbstractMobFormPlugin {

    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{ "hmua_bar_clock"});
    }
    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control)evt.getSource();
        String key = source.getKey();
        if ("hmua_bar_clock".equals(key)) {
            this.clockIn();
        }
    }

    private void clockIn() {
        DynamicObject dataEntity = this.getModel().getDataEntity();
        String lng = dataEntity.getString("hmua_longitude");
        String lat = dataEntity.getString("hmua_latitude");
        if (StringUtils.isEmpty(lng) && StringUtils.isEmpty(lat)) {
            this.getView().showTipNotification("未获取到定位信息，请刷新页面后重试！");
        } else {
            String bfType = dataEntity.getString("hmua_bf_type");
            Long channelId = 0L;
            DynamicObject hmuaBfCust;
            if ("0".equals(bfType)) {
                hmuaBfCust = dataEntity.getDynamicObject("hmua_visit_stores");
                channelId = (Long)hmuaBfCust.getPkValue();
            } else if ("1".equals(bfType)) {
                hmuaBfCust = dataEntity.getDynamicObject("hmua_bf_cust");
                channelId = (Long)hmuaBfCust.getPkValue();
            }

            hmuaBfCust = BusinessDataServiceHelper.loadSingle(channelId, "ocdbd_channel");
            BigDecimal longitude = (BigDecimal)hmuaBfCust.get("longitude");
            BigDecimal latitude = (BigDecimal)hmuaBfCust.get("latitude");
            double distance = WinkeyUtils.getDistance(Double.parseDouble(lat), Double.parseDouble(lng), latitude.doubleValue(), longitude.doubleValue());
            Integer clockRange = (Integer)WinkeyUtils.getSystemParam("hmua_sys_params", "hmua_clock_range");
            if (distance <= (double)clockRange) {
                hmuaBfCust.set("nckd_bfdate", new Date());
                SaveServiceHelper.save(new DynamicObject[]{hmuaBfCust}, OperateOption.create());

            } else {
                this.getView().showTipNotification("当前不在拜访渠道范围内！");
            }

        }
    }
}
