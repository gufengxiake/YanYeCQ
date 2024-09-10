package nckd.yanye.scm.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.metadata.botp.ConvertRuleReader;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Module           :供应链云-采购管理-采购订单-采购合同
 * Description      :退合同履约保证金：收款处理生成付款申请单插件
 * 单据标识：nckd_conm_purcontract_ext
 *
 * @author : yaosijie
 * @since : 2024/9/9
 */
public class ConmPurcontractExtOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("id");
        fieldKeys.add("entry.e_sourcebillid");
        fieldKeys.add("billstatus");
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        //勾选的采购合同数据
        DynamicObject[] entities = e.getDataEntities();
        //查询收款处理单(cas_recbill)
        for (DynamicObject dynamicObject : entities){
            QFilter qFilter = new QFilter("entry.e_sourcebillid", QCP.equals,dynamicObject.getPkValue())
                    .and("billstatus",QCP.equals,"D");
            //查询收款处理
            DynamicObject[] casrecbillObjects = BusinessDataServiceHelper.load("cas_recbill", "id", qFilter.toArray());
            //调用收款处理生成付款申请单转换规则
            List<ListSelectedRow> rows = new ArrayList<>();
            for (DynamicObject object : casrecbillObjects){
                rows.add(new ListSelectedRow(object.getPkValue()));
            }
            // 创建下推参数
            PushArgs pushArgs = new PushArgs();
            //源单单据标识
            String sourceEntityNumber = "cas_recbill";
            //目标单单据标识
            String targetEntityNumber = "ap_payapply";
            pushArgs.setSourceEntityNumber(sourceEntityNumber);
            pushArgs.setTargetEntityNumber(targetEntityNumber);
            //不检查目标单新增权限
            pushArgs.setHasRight(true);
            //下推后默认保存
            pushArgs.setAutoSave(true);
            //是否生成单据转换报告
            pushArgs.setBuildConvReport(false);

            // 单据转换规则id
            ConvertRuleReader reader = new ConvertRuleReader();
            List<String> ruleIds = reader.loadRuleIds(sourceEntityNumber, targetEntityNumber, false);
            if(ruleIds.size() > 0){
                pushArgs.setRuleId(ruleIds.get(0));
            }
            //构建选中行数据包
            pushArgs.setSelectedRows(rows);

            // 执行下推操作
            ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
            if(!result.isSuccess()){
                throw new KDBizException("下推失败：" + result.getMessage());
            }
        }
    }

}
