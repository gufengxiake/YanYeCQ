package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;

/*
直接调拨单提交服务
 */
public class TransdirOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("lotnumber");//调出批号
        e.getFieldKeys().add("inlotnumber");//调入批号
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);

        DynamicObject[] entities = e.getDataEntities();
        // 逐单处理
        for (DynamicObject dataEntity : entities) {
            DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("billentry");
            if(!entryentity.isEmpty()){
                for(DynamicObject entryRow:entryentity){
                    String inlotnumber= entryRow.getString("inlotnumber");
                    if(inlotnumber.trim().equalsIgnoreCase("")){
                        String lotnumber=entryRow.getString("lotnumber");
                        entryRow.set("inlotnumber",lotnumber);
                    }
                }
            }
        }
    }
}
