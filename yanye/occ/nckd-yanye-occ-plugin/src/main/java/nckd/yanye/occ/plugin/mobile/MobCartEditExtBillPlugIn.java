package nckd.yanye.occ.plugin.mobile;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.imsc.imbd.common.consts.PC;
import kd.occ.ocbase.common.pagemodel.OcdbdChannelAuthorize;

import java.util.EventObject;

/*
 * 移动购物车过滤下单渠道
 * 表单标识：nckd_ocdma_cart_ext
 * author:吴国强 2024-09-28
 */
public class MobCartEditExtBillPlugIn extends AbstractMobFormPlugin {

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        DynamicObjectCollection entryList = this.getModel().getEntryEntity("itemlist");
        String supplierId = this.getView().getParentView().getFormShowParameter().getCustomParam("supplierid");
        if (supplierId != null && supplierId.trim().length() > 0) {
            DynamicObject supplier = BusinessDataServiceHelper.loadSingle(Long.parseLong(supplierId), OcdbdChannelAuthorize.P_name, OcdbdChannelAuthorize.F_orderchannel);
            if (supplier != null) {
                long orderchannelId = supplier.getLong(OcdbdChannelAuthorize.F_orderchannel + "_" + PC.F_ID);
                entryList.removeIf(r -> r.getLong("orderchannelid") != orderchannelId);
            }
        }
    }
}
