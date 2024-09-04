package nckd.yanye.occ.plugin.form;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import kd.bos.bill.MobileFormPosition;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.MobileFormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractMobListPlugin;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.math.BigDecimal;
import java.util.*;

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
    public void click(EventObject evt) {
        super.click(evt);
        String key = ((Control) evt.getSource()).getKey();
        if ("nckd_settlement".equals(key)) {
            //获取列表选中数据
            BillList billlistap = this.getView().getControl("billlistap");
            ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
            if (selectedRows.size() > 1) {
                this.getView().showErrorNotification("只能选中一条记录进行结算");
                return;
            }
            EntityType entityType = billlistap.getEntityType();
            //获取选中行pkid
            Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
            //获取完整数据
            DynamicObject[] saleOrderbillArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
            if (saleOrderbillArr.length > 0) {
                DynamicObject saleOrderbill = saleOrderbillArr[0];
                //获取待收金额 sumunrecamount 应收金额 sumreceivableamount
                BigDecimal sumunrecamount = saleOrderbill.getBigDecimal("sumunrecamount");
                if (sumunrecamount.compareTo(new BigDecimal(0)) == 0) {
                    this.getView().showErrorNotification("已收款，无需支付");
                    return;
                }
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

                //还有一些要货订单的数据传过去用于生成支付日志记录单和支付流水记录单
                showParameter.setCustomParams(customParams);
                this.getView().showForm(showParameter);
            }
        }
    }
}
