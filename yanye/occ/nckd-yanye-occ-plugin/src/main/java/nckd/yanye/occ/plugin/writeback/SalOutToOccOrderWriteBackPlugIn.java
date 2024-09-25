package nckd.yanye.occ.plugin.writeback;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.BillEntityType;
import kd.bos.entity.botp.plugin.AbstractWriteBackPlugIn;
import kd.bos.entity.botp.plugin.args.AfterReadSourceBillEventArgs;
import kd.bos.entity.botp.plugin.args.AfterSaveSourceBillEventArgs;
import kd.bos.entity.botp.plugin.args.BeforeReadSourceBillEventArgs;
import kd.bos.entity.botp.plugin.args.PreparePropertysEventArgs;
import kd.bos.orm.util.StringUtils;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.MetadataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.ArrayList;
import java.util.List;

/*
销售出库反写插件
 */
public class SalOutToOccOrderWriteBackPlugIn extends AbstractWriteBackPlugIn {

    @Override
    public void beforeReadSourceBill(BeforeReadSourceBillEventArgs e) {
        super.beforeReadSourceBill(e);
        if ("ocbsoc_saleorder".equals(e.getSrcMainType().getName())) {
            e.getFieldKeys().add("nckd_vehicle");
            e.getFieldKeys().add("nckd_driver");
        }

    }
    @Override
    public void afterReadSourceBill(AfterReadSourceBillEventArgs e) {
        DynamicObject[] srcDataEntities= e.getSrcDataEntities();
        for(DynamicObject srcDataEntry:srcDataEntities){
           Object pkId=srcDataEntry.getPkValue();
           DynamicObject vehicle=srcDataEntry.getDynamicObject("nckd_vehicle");
           DynamicObject driver=srcDataEntry.getDynamicObject("nckd_driver");
        }
    }


    public void afterSaveSourceBill(AfterSaveSourceBillEventArgs e) {
        super.afterSaveSourceBill(e);

        BillEntityType srcSubMainType = e.getSrcSubMainType();
        String opType = this.getOpType();
        if ("ocbsoc_saleorder".equals(srcSubMainType.getName()) && (opType.equalsIgnoreCase("audit") || opType.equalsIgnoreCase("unaudit") )) {
            DynamicObject[] srcDataEntities = e.getSrcDataEntities();

            List<DynamicObject> saveValue = new ArrayList(16);
            DynamicObject[] saveValues = srcDataEntities;
            int var7 = srcDataEntities.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                DynamicObject srcDataEntity = saveValues[var8];
                srcDataEntity = BusinessDataServiceHelper.loadSingle(srcDataEntity.getPkValue(), MetadataServiceHelper.getDataEntityType("ocbsoc_saleorder"));

//                srcDataEntity.set("billstatus", billStatus);
//
//                srcDataEntity.set("signstatus", signStatus);
//                saveValue.add(srcDataEntity);
//                saveValue.addAll(this.setTotalStockBaseQty(srcDataEntity, signStatus));
            }

            //saveValues = (DynamicObject[])saveValue.toArray(new DynamicObject[saveValue.size()]);
            //SaveServiceHelper.save(saveValues);
        }

    }
}
