package nckd.yanye.occ.plugin.writeback;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityType;
import kd.bos.entity.botp.plugin.AbstractWriteBackPlugIn;
import kd.bos.entity.botp.plugin.args.AfterCommitAmountEventArgs;
import kd.bos.entity.botp.runtime.BFRowId;

/*
签收单反写销售出库插件
 */
public class SignToSalOutWriteBackPlugIn extends AbstractWriteBackPlugIn {

//    @Override
//    public void afterCommitAmount(AfterCommitAmountEventArgs e) {
//        // TODO 在此添加业务逻辑
//        DynamicObject srcActive= e.getSrcActiveRow();
//        EntityType entity= e.getSrcEntity();
//        String file= e.getSrcFieldKey();
//        DynamicObject tarActive= e.getTargetActiveRow();
//        EntityType tarentity=e.getTargetEntity();
//        BFRowId x= e.getTargetRowId();
//        if(srcActive!=null){
//            int c=0;
//        }
//
//    }
}
