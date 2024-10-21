package nckd.yanye.scm.plugin.form;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.exception.KDBizException;
import kd.bos.form.ConfirmCallBackListener;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.MessageBoxResult;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

/**
 * @author husheng
 * @date 2024-10-21 15:52
 * @description 物料维护单（nckd_materialmaintenan）
 */
public class MaterialmaintenanListPlugin extends AbstractListPlugin {
    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);

        FormOperate formOperate = (FormOperate) args.getSource();
        String operateKey = formOperate.getOperateKey();
        if ("submit".equals(operateKey)) {
            boolean flag = this.saveOrSubmitVerify();
            if (flag) {
                this.showMessage(args, operateKey);
            }
        }
    }

    private Boolean saveOrSubmitVerify() {
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        QFilter qFilter = new QFilter("id", QCP.in, selectedRows.getPrimaryKeyValues())
                .and("nckd_materialmaintunit", QCP.equals, "updateinfo");
        DynamicObjectCollection materialmaintenan = QueryServiceHelper.query("nckd_materialmaintenan", "id,nckd_altermaterialname,nckd_alterspecificat,nckd_altermodel", new QFilter[]{qFilter});

        final Boolean[] flag = {false};
        materialmaintenan.stream().forEach(m -> {
            // 校验物料名称、规格、型号，组合唯一性
            // 物料
            QFilter qFilter1 = new QFilter("name", QCP.equals, m.getString("nckd_altermaterialname"))
                    .and("modelnum", QCP.equals, m.getString("nckd_alterspecificat"))
                    .and("nckd_model", QCP.equals, m.getString("nckd_altermodel"));
            boolean exists1 = QueryServiceHelper.exists("bd_material", qFilter1.toArray());

            // 物料申请单
            QFilter qFilter2 = new QFilter("nckd_materialentries.nckd_materialname", QCP.equals, m.getString("nckd_altermaterialname"))
                    .and("nckd_materialentries.nckd_specifications", QCP.equals, m.getString("nckd_alterspecificat"))
                    .and("nckd_materialentries.nckd_model", QCP.equals, m.getString("nckd_altermodel"));
            boolean exists2 = QueryServiceHelper.exists("nckd_materialrequest", qFilter2.toArray());

            // 物料维护单
            QFilter qFilter3 = new QFilter("nckd_altermaterialname", QCP.equals, m.getString("nckd_altermaterialname"))
                    .and("nckd_alterspecificat", QCP.equals, m.getString("nckd_alterspecificat"))
                    .and("nckd_altermodel", QCP.equals, m.getString("nckd_altermodel"))
                    .and("nckd_materialmaintunit", QCP.equals, "updateinfo");
            int size1 = QueryServiceHelper.query("nckd_materialmaintenan", "id", qFilter3.toArray()).size();

            // 物料名称、规格、型号，需要组合校验唯一性
            if (exists1 || exists2 || size1 > 1) {
                throw new KDBizException("物料名称、规格、型号，组合需唯一!");
            }

            // 校验物料名称唯一性
            // 物料
            QFilter qFilter4 = new QFilter("name", QCP.equals, m.getString("nckd_altermaterialname"));
            boolean exists3 = QueryServiceHelper.exists("bd_material", qFilter4.toArray());

            // 物料申请单
            QFilter qFilter5 = new QFilter("nckd_materialentries.nckd_materialname", QCP.equals, m.getString("nckd_altermaterialname"));
            boolean exists4 = QueryServiceHelper.exists("nckd_materialrequest", qFilter5.toArray());

            // 物料维护单
            QFilter qFilter6 = new QFilter("nckd_altermaterialname", QCP.equals, m.getString("nckd_altermaterialname"))
                    .and("nckd_materialmaintunit", QCP.equals, "updateinfo");
            int size2 = QueryServiceHelper.query("nckd_materialmaintenan", "id", qFilter6.toArray()).size();

            // 物料名称、规格、型号，需要组合校验唯一性
            if (exists3 || exists4 || size2 > 1) {
                flag[0] = true;
            }
        });

        return flag[0];
    }

    private void showMessage(BeforeDoOperationEventArgs args, String operateKey) {
        // 判断是否处理过
        String isDealed = this.getView().getPageCache().get("isDealed");
        if (!"true".equals(isDealed)) {
            // 取消原来的操作
            args.setCancel(true);
            // 在用户点击确认框上的按钮后，系统会调用confirmCallBack方法
            ConfirmCallBackListener confirmCallBackListener = new ConfirmCallBackListener(operateKey, this);
            // 设置页面确认框，参数为：标题，选项框类型，回调监听
            this.getView().showConfirm("物料名称重复！", MessageBoxOptions.YesNo, confirmCallBackListener);
            // 只执行一次
            this.getView().getPageCache().put("isDealed", "true");
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);

        String callBackId = messageBoxClosedEvent.getCallBackId();
        //判断回调参数id
        if ("submit".equals(callBackId)) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                this.getView().invokeOperation(callBackId);
            } else if (MessageBoxResult.No.equals(messageBoxClosedEvent.getResult())) {
                // 点击否也清除
                this.getView().getPageCache().remove("isDealed");
            }
        }
    }
}
