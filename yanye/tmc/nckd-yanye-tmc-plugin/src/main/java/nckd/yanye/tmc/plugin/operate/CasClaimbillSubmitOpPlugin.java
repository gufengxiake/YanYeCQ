package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.MetadataServiceHelper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Module           :财务云-出纳-收款认领
 * Description      :1.认领处理单，认领处理单的应收金额不能大于要货订单" + eCorebillno + "的待收金额
 *
 * @author : zhujintao
 * @date : 2024/9/6
 */
public class CasClaimbillSubmitOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        // 提前加载表单里的字段
        List<String> fieldKeys = e.getFieldKeys();
        MainEntityType dt = MetadataServiceHelper.getDataEntityType("cas_claimbill");
        Map<String, IDataEntityProperty> fields = dt.getAllFields();
        fields.forEach((Key, value) -> {
            fieldKeys.add(Key);
        });
    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    DynamicObject bill = dataEntity.getDataEntity();
                    DynamicObjectCollection casRecbillCollection = bill.getDynamicObjectCollection("entryentity");
                    for (DynamicObject casRecbillEntry : casRecbillCollection) {
                        if ("ocbsoc_saleorder".equals(casRecbillEntry.getString("e_corebilltype"))) {
                            //应收金额
                            BigDecimal eReceivableamt = casRecbillEntry.getBigDecimal("e_receivableamt");
                            //要货订单编码
                            String eCorebillno = casRecbillEntry.getString("e_corebillno");
                            QFilter qFilter = new QFilter("billno", QCP.equals, eCorebillno);
                            //应收金额 sumreceivableamount,已收金额 sumrecamount,待收金额 sumunrecamount
                            DynamicObject loadSingleRecbill = BusinessDataServiceHelper.loadSingle("ocbsoc_saleorder", "id,sumreceivableamount,sumrecamount,sumunrecamount", qFilter.toArray());
                            //待收金额
                            BigDecimal sumunrecamount = loadSingleRecbill.getBigDecimal("sumunrecamount");
                            if (eReceivableamt.compareTo(sumunrecamount) > 0) {
                                this.addErrorMessage(dataEntity, "认领处理单的应收金额不能大于要货订单" + eCorebillno + "的待收金额");
                            }
                        }
                    }
                }
            }
        });
    }
}
