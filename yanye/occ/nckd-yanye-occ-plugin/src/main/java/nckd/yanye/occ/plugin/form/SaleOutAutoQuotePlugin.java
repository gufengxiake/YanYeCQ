package nckd.yanye.occ.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.mpscmm.msbd.business.helper.SysParamHelper;
import kd.mpscmm.msbd.formplugin.AbstractAutoQuotePlugin;

/*
 *销售出库单表单插件，自动取价
 * 表单标识：nckd_im_saloutbill_ext
 * author:吴国强 2024-09-22
 */
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
