package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module           :
 * Description      :
 *
 * @author : zhujintao
 * @date : 2024/8/22
 */
public class CasPaybillOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        // 提前加载表单里的字段
        List<String> fieldKeys = e.getFieldKeys();
        MainEntityType dt = MetadataServiceHelper.getDataEntityType("cas_paybill");
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
                String[] numberStr = {"JSFS06", "JSFS07", "JSFS02", "JSFS03"};
                QFilter qFilter = new QFilter("number", QCP.in, numberStr);
                DynamicObject[] settlementtype = BusinessDataServiceHelper.load("bd_settlementtype", "id", qFilter.toArray());
                List<Object> settlementtypeId = Arrays.stream(settlementtype).map(e -> e.getPkValue()).collect(Collectors.toList());
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    DynamicObject casPaybill = dataEntity.getDataEntity();
                    //背书仅适应结算方式类型是承兑汇票、支票或本票且结算号选择了库存票据的付款单，你所选单据不支持背书。
                    if (!settlementtypeId.contains(casPaybill.getDynamicObject("settletype").getPkValue())) {
                        this.addErrorMessage(dataEntity, "背书仅适应结算方式类型是承兑汇票、支票或本票且结算号选择了库存票据的付款单，你所选单据" + casPaybill.getString("billno") + "不支持背书。");
                        continue;
                    }
                    if (!"C".equals(casPaybill.getString("billstatus"))) {
                        this.addErrorMessage(dataEntity, "单据" + casPaybill.getString("billno") + "未审核不允许背书");
                    }
                }
            }
        });
    }
}
