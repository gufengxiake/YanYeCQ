package nckd.yanye.scm.plugin.operate;

import kd.bos.bill.BillShowParameter;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityType;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.exception.KDBizException;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.metadata.botp.ConvertRuleReader;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module           :供应链云-采购管理-采购订单-采购合同
 * Description      :退合同履约保证金：收款处理生成付款申请单插件
 * 单据标识：nckd_conm_purcontract_ext
 *
 * @author : yaosijie
 * @since : 2024/9/9
 */
public class ConmPurcontractExtOpPlugin extends AbstractListPlugin {

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        if("returnbond".equals(e.getOperateKey())) {
            BillList billlistap = this.getView().getControl("billlistap");
            ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
            List<ListSelectedRow> selectedRowList = selectedRows.stream().filter(t -> "C".equals(t.getBillStatus())).collect(Collectors.toList());
            EntityType entityType = billlistap.getEntityType();
            //获取选中行审核状态数据的pkid
            List<Object> list = selectedRowList.stream().map(t -> t.getPrimaryKeyValue()).collect(Collectors.toList());
            //获取完整数据
            DynamicObject[] entities = BusinessDataServiceHelper.load(list.toArray(), entityType);
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
                }else {
                    BillShowParameter param = new BillShowParameter();
                    param.setFormId("ap_payapply");
                    param.setPkId(result.getTargetBillIds().toArray()[0]);
                    param.getOpenStyle().setShowType(ShowType.MainNewTabPage);
                    this.getView().showForm(param);
                }
        }

        }

    }
}
