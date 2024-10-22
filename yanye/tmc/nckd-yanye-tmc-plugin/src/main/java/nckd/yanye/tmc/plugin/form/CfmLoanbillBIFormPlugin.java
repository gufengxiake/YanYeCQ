package nckd.yanye.tmc.plugin.form;

import java.util.EventObject;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.entity.property.BasedataProp;
import kd.bos.form.field.BasedataEdit;

/**
 * @author husheng
 * @date 2024-10-12 15:11
 * @description  银行提款处理(nckd_cfm_loanbill_b_l_ext)
 */
public class CfmLoanbillBIFormPlugin extends AbstractBillPlugIn {
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);

        /*
            数据来源
                贷款管理	cfm
                投资理财	invest
                发债管理	bond
                内部金融管理	ifm
         */
        String datasource = (String) this.getModel().getValue("datasource");
        /*
            贷款类型
                普通贷款	loan
                银团贷款	sl
                企业往来	ec
                委托贷款	entrust
                债券发行	bond
         */
        String loantype = (String) this.getModel().getValue("loantype");
        if("cfm".equals(datasource) && ("loan".equals(loantype) || "sl".equals(loantype))){
            // 设置占用授信字段必填 页面上的必填和数据校验的必填
            BasedataEdit creditlimit = this.getControl("creditlimit");
            creditlimit.setMustInput(true);
            BasedataProp creditlimit2 = (BasedataProp) this.getModel().getDataEntityType().getProperty("creditlimit");
            creditlimit2.setMustInput(true);
        }
    }
}
