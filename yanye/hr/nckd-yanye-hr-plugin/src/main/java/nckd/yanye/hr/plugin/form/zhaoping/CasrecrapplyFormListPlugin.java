package nckd.yanye.hr.plugin.form.zhaoping;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.ConfirmCallBackListener;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.MessageBoxResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.ObjectUtils;
import kd.bos.list.BillList;

/**
 * Module           :人才供应云-招聘直通车-首页-临时招聘申请
 * Description      :临时招聘申请列表操作插件
 *
 * @author guozhiwei
 * @date  2024/9/18 16：20
 * 标识 nckd_casrecrapply
 */



public class CasrecrapplyFormListPlugin extends AbstractListPlugin {


    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate opreate = (FormOperate) args.getSource();
        switch (opreate.getOperateKey()) {
            case "submit":
                ListSelectedRowCollection selectCols = args.getListSelectedData();
                StringBuffer exceedMsg = new StringBuffer();
                Object[] primaryKeyValues  = selectCols.getPrimaryKeyValues();
                BillList billlistap = this.getView().getControl("billlistap");
                EntityType entityType = billlistap.getEntityType();
                //获取完整数据
                DynamicObject[] casPaybillArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
                for (int i = 0; i < casPaybillArr.length; i++) {
                    // 在此添加处理逻辑
                    DynamicObject dynamicObject = casPaybillArr[i];
                    int nckdSftaffcount = dynamicObject.getInt("nckd_sftaffcount");
                    // 实际人数
                    int nckdRelnum = dynamicObject.getInt("nckd_relnum");
                    // 申请人数
                    int nckdApplynum = dynamicObject.getInt("nckd_applynum");

                    if(nckdSftaffcount < nckdRelnum + nckdApplynum){
                        // 判断是否处理过
                        String isDealed = this.getView().getPageCache().get("isDealed");
                        if (!"true".equals(isDealed)) {
                            if(ObjectUtils.isEmpty(exceedMsg)){
                                exceedMsg.append("单据：");
                            }
                            exceedMsg.append(dynamicObject.getString("billno")+",");
                        }
                    }
                }
                if(ObjectUtils.isNotEmpty(exceedMsg)){
                    args.setCancel(true);
                    exceedMsg.append("请注意，申请人数超编，是否继续提报？");
                    ConfirmCallBackListener confirmCallBackListener = new ConfirmCallBackListener("isExceed", this);
                    // 设置页面确认框，参数为：标题，选项框类型，回调监听
                    this.getView().showConfirm(exceedMsg.toString(), MessageBoxOptions.YesNo, confirmCallBackListener);
                    // 只执行一次
                    this.getView().getPageCache().put("isDealed", "true");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        // 清除
        this.getView().getPageCache().remove("isDealed");
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        //判断回调参数id
        if ("isExceed".equals(messageBoxClosedEvent.getCallBackId())) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                this.getView().invokeOperation("submit");
            } else if (MessageBoxResult.No.equals(messageBoxClosedEvent.getResult())) {
                // 点击否也清除
                this.getView().getPageCache().remove("isDealed");
            }
        }
    }

}
