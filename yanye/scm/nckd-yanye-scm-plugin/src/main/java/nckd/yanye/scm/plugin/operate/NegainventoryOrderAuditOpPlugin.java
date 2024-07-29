package nckd.yanye.scm.plugin.operate;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Module           :制造云-生产任务管理-负库存物料检查单
 * Description      :负库存物料检查单审核操作插件
 *
 * @author : zhujintao
 * @date : 2024/7/29
 */
public class NegainventoryOrderAuditOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
    }

    /**
     * 负库存物料检查单审核后删除盘点方案，盘点表
     *
     * @param e
     */
    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        DynamicObject[] entities = e.getDataEntities();
        //获取盘点方案编码
        Set<String> nckdInvcountschemenoSet = Arrays.stream(entities).map(k -> k.getString("nckd_invcountschemeno")).collect(Collectors.toSet());
        QFilter qFilter = new QFilter("billno", QCP.in, nckdInvcountschemenoSet);
        DynamicObject[] load = BusinessDataServiceHelper.load("im_invcountscheme", "id,billno,name", qFilter.toArray());
        //反审核
        OperationResult unAuditOperationResult = OperationServiceHelper.executeOperate("unaudit", "im_invcountscheme", load, OperateOption.create());
        if (!unAuditOperationResult.isSuccess()) {
            throw new KDBizException("选中的负库存物料检查单对应的盘点方案反审核失败,请手动反审核盘点方案并删除");
        } else {
            OperationResult deleteOperationResult = OperationServiceHelper.executeOperate("delete", "im_invcountscheme", load, OperateOption.create());
            if (!deleteOperationResult.isSuccess()) {
                throw new KDBizException("选中的负库存物料检查单对应的盘点方案删除失败,请手动删除盘点方案");
            }
        }
    }
}
