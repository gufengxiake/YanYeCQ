package nckd.yanye.occ.plugin.mobile;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.imsc.dmw.utils.StringUtil;
import kd.imsc.imbd.common.consts.PC;
import kd.occ.ocbase.business.b2b.B2BUserHelper;
import kd.occ.ocbase.common.pagemodel.OcdbdChannelAuthorize;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

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
        if (StringUtil.isNotNull(supplierId)) {
            DynamicObject supplier = BusinessDataServiceHelper.loadSingle(Long.parseLong(supplierId), OcdbdChannelAuthorize.P_name, OcdbdChannelAuthorize.F_orderchannel);
            if (supplier != null) {
                long orderchannelId = supplier.getLong(OcdbdChannelAuthorize.F_orderchannel + "_" + PC.F_ID);
                entryList.removeIf(r -> r.getLong("orderchannelid") != orderchannelId);
            }
        }
    }
}
