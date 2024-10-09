package nckd.yanye.occ.plugin.Convert;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicProperty;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.bos.entity.botp.runtime.ConvertConst;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;
import nckd.yanye.occ.plugin.operate.SalOutSaveOperationPlugIn;

import java.util.List;
import java.util.Map;

/**
 * 单据转换插件
 */
public class SalOutPushFinApConvertPlugIn extends AbstractConvertPlugIn implements Plugin {
    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);
//        ExtendedDataEntity[] extendedDataEntities = e.getTargetExtDataEntitySet().FindByEntityKey(this.getTgtMainType().toString());
//        DynamicProperty nckdDeliverchannel = e.getFldProperties().get("nckd_deliverchannel");
//        if(nckdDeliverchannel == null){
//            return;
//        }
//        for (ExtendedDataEntity extendedDataEntity : extendedDataEntities) {
//            // 取当前目标单，对应的源单行
//            List<DynamicObject> srcRows = (List<DynamicObject>)extendedDataEntity.getValue(ConvertConst.ConvExtDataKey_SourceRows); //获取源单数据行
//            // 取源单第一行上的字段值，忽略其他行
//            DynamicObject srcRow = srcRows.get(0);
//            Object srcId =  nckdDeliverchannel.getValue(srcRow);
//            Object supplierId = new SalOutSaveOperationPlugIn().getSupplierId((Long) srcId);
//            DynamicObject bd_supplier = BusinessDataServiceHelper.loadSingle("bd_supplier",
//                    new QFilter[]{new QFilter("id", QCP.equals, supplierId)});//1110082709432067072L
//            Object asstact = extendedDataEntity.getValue("asstact");
//            extendedDataEntity.setValue("asstact",bd_supplier);
//            extendedDataEntity.setValue("receivingsupplierid",bd_supplier);
//            extendedDataEntity.setValue("remark",bd_supplier);
//            Object aa = extendedDataEntity.getValue("asstact");
//        }
    }

}