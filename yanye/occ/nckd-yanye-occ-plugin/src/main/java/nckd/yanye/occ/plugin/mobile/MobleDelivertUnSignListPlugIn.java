package nckd.yanye.occ.plugin.mobile;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.metadata.dynamicobject.DynamicObjectType;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.CloseCallBack;
import kd.bos.form.MobileFormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.container.Tab;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.TabSelectEvent;
import kd.bos.form.control.events.TabSelectListener;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.BillList;
import kd.bos.list.MobileSearch;
import kd.bos.orm.query.QFilter;
import kd.bos.orm.query.fulltext.QMatches;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.MetadataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.occ.ocbase.business.b2b.B2BUserHelper;
import kd.occ.ocbase.common.util.CommonUtils;
import kd.occ.ocbase.common.util.MobileControlUtils;
import kd.occ.ocbase.common.util.OperationResultUtil;
import kd.occ.ocbase.formplugin.base.OcbaseFormMobPlugin;

import java.util.EventObject;

/*
 * 签收处理移动表单插件
 * 表单标识：nckd_ocdma_deliverysign
 * author:吴国强 2024-08-25
 */
public class MobleDelivertUnSignListPlugIn extends OcbaseFormMobPlugin implements TabSelectListener {

    @Override
    public void registerListener(EventObject e) {
        this.addTabSelectListener(this, new String[]{"tabap"});
        super.registerListener(e);
    }
    public void afterDoOperation(AfterDoOperationEventArgs args) {
        //拒签
        if (args.getOperateKey().equals("unsign")) {
            BillList billList = (BillList) this.getControl("billlistap");
            //单据Id
            Object id = billList.getCurrentSelectedRowInfo().getPrimaryKeyValue();
            OperateOption operateOption = CommonUtils.getOperateOption();
            DynamicObjectType type = MetadataServiceHelper.getDataEntityType("ocbsoc_delivery_record");
            DynamicObject[] deliveryDataArray = BusinessDataServiceHelper.load(new Object[]{id}, type);
            //operateOption.setVariableValue("completesign", "true");
            OperationResult result = OperationServiceHelper.executeOperate("unsign", "ocbsoc_delivery_record", deliveryDataArray, operateOption);
            if (!result.isSuccess()) {
                this.getView().showErrorNotification(OperationResultUtil.getErrorInfoMsg(result));
            } else {
                this.getView().showSuccessNotification(String.format("单据:%s，拒签成功", deliveryDataArray[0].getString("billno")));
            }
            this.refreshBillList();
        }

        super.afterDoOperation(args);
    }

    public void tabSelected(TabSelectEvent evt) {
        switch (((Control)evt.getSource()).getKey().toLowerCase()) {
            case "tabap":
                this.refreshBillList();
            default:
        }
    }

    private void refreshBillList() {
        BillList billList = (BillList) this.getControl("billlistap");
        MobileControlUtils.BillListRefresh(billList, new QFilter[]{this.getListFilter()});
    }

    private QFilter getListFilter() {
        QFilter filter = new QFilter("customer", "=", B2BUserHelper.getLoginChannelId());
        filter.or(new QFilter("receivechannelid", "=", B2BUserHelper.getLoginChannelId()));
        Tab tab = (Tab) this.getControl("tabap");
        String curTab = tab.getCurrentTab();
        if ("tab_notsign".equals(curTab)) {
            filter.and("billstatus", "in", new String[]{"B", "D"});
        } else if ("tab_sign".equals(curTab)) {
            filter.and("billstatus", "in", new String[]{"C"});
        } else if ("nckd_tabunsign".equals(curTab)) {
            filter.and("billstatus", "in", new String[]{"E"});
        }

        MobileSearch search = (MobileSearch) this.getControl("searchap");
        String searchText = search.getText();
        if (searchText != null && !"".equals(searchText.trim())) {
            filter = filter.and(QMatches.ftlike(new String[]{searchText}, new String[]{"billno"}));
        }

        return filter;
    }
}
