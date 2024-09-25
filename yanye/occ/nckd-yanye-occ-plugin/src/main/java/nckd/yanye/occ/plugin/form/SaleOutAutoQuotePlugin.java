package nckd.yanye.occ.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.mpscmm.msbd.business.helper.SysParamHelper;
import kd.mpscmm.msbd.formplugin.AbstractAutoQuotePlugin;

public class SaleOutAutoQuotePlugin extends AbstractAutoQuotePlugin {
    protected boolean isAutoQuote() {
        return true;
    }
    protected String getQuoteBillEntryNumber() {
        return "billentry";
    }

    protected String getMainOrgKey() {
        return "bizorg";
    }

}
