package nckd.yanye.occ.plugin.form;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import kd.bos.bill.MobileFormPosition;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.MobileFormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.PreOpenFormEventArgs;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractMobListPlugin;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Module           :全渠道云-B2B订单中心-要货订单
 * Description      :1)要货订单增加按钮 “结算”，点击后，需弹出窗口。支付金额：(内容-待收金额)，如待收金额为0，则提示已收款，无需支付；支付金额栏位可修改，但不可超过待收金额；
 * 点击确认后，进行检查是否有支付流水存在”不支持的交易状态”的状态，如不存在，则唤起支付，将订单信息、金额传递至银行接口，获取银行二维码；
 * 如存在，则提示已存在结算中的支付流水，是否需要继续支付，点击是，则唤起支付，将订单信息、金额传递至银行接口，获取银行二维码；
 *
 * @author : zhujintao
 * @date : 2024/8/19
 */
public class OcbSocSaleOrderMobListPlugin extends AbstractMobListPlugin {
    private static final Log logger = LogFactory.getLog(OcbSocSaleOrderMobListPlugin.class);
    //1)点击后，需弹出窗口。支付金额：(内容-待收金额)，如待收金额为0，则提示已收款，无需支付；支付金额栏位可修改，但不可超过待收金额；

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"nckd_settlement"});
    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        super.beforeClick(evt);
        String key = ((Control) evt.getSource()).getKey();
        if ("nckd_settlement".equals(key)) {
            //获取列表选中数据
            BillList billlistap = this.getView().getControl("billlistap");
            Object id = billlistap.getCurrentSelectedRowInfo().getPrimaryKeyValue();
            EntityType entityType = billlistap.getEntityType();
            //获取完整数据
            DynamicObject saleOrderbill = BusinessDataServiceHelper.loadSingle(id, entityType);
            if (ObjectUtil.isNotEmpty(saleOrderbill)) {
                if ("C".equals(saleOrderbill.getString("paystatus"))) {
                    this.getView().showErrorNotification("订单已收款，无需支付");
                    evt.setCancel(true);
                    return;
                }
                if ("B".equals(saleOrderbill.getString("closestatus"))) {
                    this.getView().showErrorNotification("订单已关闭，无法支付");
                    evt.setCancel(true);
                    return;
                }
                //获取待收金额 sumunrecamount 应收金额 sumreceivableamount
                BigDecimal sumunrecamount = saleOrderbill.getBigDecimal("sumunrecamount");
                if (sumunrecamount.compareTo(new BigDecimal(0)) == 0) {
                    this.getView().showErrorNotification("订单已收款，无需支付");
                    evt.setCancel(true);
                    return;
                }
            }
        }
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        String key = ((Control) evt.getSource()).getKey();
        if ("nckd_settlement".equals(key)) {
            //获取列表选中数据
            BillList billlistap = this.getView().getControl("billlistap");
            Object id = billlistap.getCurrentSelectedRowInfo().getPrimaryKeyValue();
            EntityType entityType = billlistap.getEntityType();
            //获取完整数据
            DynamicObject saleOrderbill = BusinessDataServiceHelper.loadSingle(id, entityType);
            if (ObjectUtil.isNotEmpty(saleOrderbill)) {
                //获取待收金额 sumunrecamount 应收金额 sumreceivableamount
                BigDecimal sumunrecamount = saleOrderbill.getBigDecimal("sumunrecamount");
                //弹框 设置支付金额
                MobileFormShowParameter showParameter = new MobileFormShowParameter();
                showParameter.setFormId("nckd_setpayamount");
                showParameter.setCaption("设置支付金额");
                showParameter.setPosition(MobileFormPosition.Bottom);
                showParameter.getOpenStyle().setShowType(ShowType.Modal);
                Map<String, Object> customParams = new HashMap<>();
                customParams.put("sumunrecamount", sumunrecamount);
                customParams.put("orderNo", DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(6));
                customParams.put("billNo", saleOrderbill.getString("billno"));
                customParams.put("saleorgid", saleOrderbill.getDynamicObject("saleorgid"));
                customParams.put("orderdate", saleOrderbill.getDate("orderdate"));
                // 设置回调
                showParameter.setCloseCallBack(new CloseCallBack(this, "refreshlist"));
                //还有一些要货订单的数据传过去用于生成支付日志记录单和支付流水记录单
                showParameter.setCustomParams(customParams);
                this.getView().showForm(showParameter);
            }
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        String key = closedCallBackEvent.getActionId();
        if (StringUtils.equals("refreshlist", key)) {
            // 刷新列表
            ((BillList) this.getControl("billlistap")).refresh();
        }
    }
}
