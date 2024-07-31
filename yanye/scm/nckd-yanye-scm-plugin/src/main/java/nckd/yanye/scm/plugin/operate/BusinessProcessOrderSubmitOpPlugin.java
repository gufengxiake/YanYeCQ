package nckd.yanye.scm.plugin.operate;

import dm.jdbc.util.StringUtil;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;

import java.util.Arrays;
import java.util.List;

/**
 * Module           :制造云-生产任务管理-生产工单（新）
 * Description      :物料-业务处理对应单提交插件
 *
 * @author : yaosijie
 * @date : 2024/7/24
 */
public class BusinessProcessOrderSubmitOpPlugin extends AbstractOperationServicePlugIn {


    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_useworkshop");
        fieldKeys.add("nckd_wareorderworkshop");
        fieldKeys.add("nckd_businessdocument");
        fieldKeys.add("nckd_useworkshop");
    }
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] entities = this.getDataEntities();
                Arrays.asList(entities).forEach(k -> {
                    DynamicObject dynamic = k.getDataEntity();
                    String billno = dynamic.getString("billno");
                    //拿到分录数据
                    DynamicObjectCollection dynamicObjects = dynamic.getDynamicObjectCollection("nckd_bussinessentries");
                    for (DynamicObject dynamicObject : dynamicObjects){
                        //对应业务单据
                        String businessdocument = dynamicObject.getString("nckd_businessdocument");
                        //正库存领用车间
                        String useworkshop = dynamicObject.getString("nckd_useworkshop");
                        //入库单对应车间
                        String wareorderworkshop = dynamicObject.getString("nckd_wareorderworkshop");
                        if (("3".equals(businessdocument) || "4".equals(businessdocument)) && StringUtil.isEmpty(useworkshop)){
                            this.addErrorMessage(k, String.format("单据编号：(%s),【对应业务单据】为生产领料单或领料出库单时，正库存领用车间必录",
                                    billno));
                        }else if (("1".equals(businessdocument) || "2".equals(businessdocument)) && StringUtil.isEmpty(wareorderworkshop)){
                            this.addErrorMessage(k, String.format("单据编号：(%s),【对应业务单据】为完工入库单或生产入库单时，入库单对应车间必录",
                                    billno));
                        }
                    }
                });
            }
        });
    }
}
