package nckd.yanye.occ.plugin.mobile;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.db.DB;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.*;
import kd.bos.form.cardentry.CardEntry;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.operate.AbstractOperate;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.BillList;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.occ.ocbase.common.util.CommonUtils;
import kd.occ.ocbase.common.util.DateUtil;
import kd.occ.ocbase.common.util.DynamicObjectUtils;

import java.math.BigDecimal;
import java.util.*;

/*
 * 要货订单移动列表插件
 * 表单标识：nckd_ocdma_saleorder_list
 * author:吴国强 2024-08-26
 */
public class MobileSalOrderListPlugIn extends AbstractMobFormPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
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

}
