package nckd.yanye.scm.plugin.operate;

import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Module           :供应链云-采购管理-采购合同
 * Description      :采购合同如果招采成交价和价税合计不一致，不允许提交
 * 单据标识：nckd_conm_purcontract_ext
 *
 * @author : liuxiao
 * @since : 2024/9/4
 */
public class PurcontractbillSubmitOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_totalprice");
        fieldKeys.add("totalallamount");
    }

    /**
     * @param e
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    BigDecimal totalprice = (BigDecimal) dataEntity.getValue("nckd_totalprice");
                    if (totalprice == null) {
                        continue;
                    }
                    BigDecimal totalallamount = (BigDecimal) dataEntity.getValue("totalallamount");
                    if (totalallamount == null) {
                        continue;
                    }
                    if (totalprice.compareTo(totalallamount) != 0) {
                        this.addErrorMessage(dataEntity, "价税合计和招采成交价税合计不一致，不允许提交！");
                    }
                }
            }
        });
    }
}