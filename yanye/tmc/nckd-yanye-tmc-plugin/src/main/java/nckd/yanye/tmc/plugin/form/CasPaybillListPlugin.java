package nckd.yanye.tmc.plugin.form;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.RefObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.*;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.BillList;
import kd.bos.list.ListShowParameter;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :财务云-出纳-付款单
 * Description      :2.在对供应商支付环节对【上级要求风险单位、失信单位、自定义黑名单单位】预警提示，预警信息如下：收款单位为问题单位，是否确认付款。
 *
 * @author : zhujintao
 * @date : 2024/8/7
 */
public class CasPaybillListPlugin extends AbstractListPlugin {
    private static String KEY_BEFORESUBMIT = "beforesubmit";
    private static String KEY_BATCHENDORSE = "batchendorse";
    private static String OPPARAM_AFTERCONFIRM = "afterconfirm";
    private static Map<String, String> unittypeMap;

    static {
        unittypeMap = new HashMap<>();
        unittypeMap.put("B", "上级要求风险单位");
        unittypeMap.put("C", "失信单位");
        unittypeMap.put("D", "自定义黑名单单位");
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate formOperate = (FormOperate) args.getSource();
        if (StringUtils.equals(KEY_BEFORESUBMIT, formOperate.getOperateKey())) {
            //获取列表选中数据
            BillList billlistap = this.getView().getControl("billlistap");
            ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
            EntityType entityType = billlistap.getEntityType();
            //获取选中行pkid
            Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
            //获取完整数据
            DynamicObject[] casPaybillArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
            ////获取收款人类型 和 供应商
            List<DynamicObject> casPaybillList = Arrays.stream(casPaybillArr).filter(e -> "bd_supplier".equals(e.getString("payeetype"))).collect(Collectors.toList());
            if (casPaybillList.size() == 1) {
                DynamicObject casPaybill = casPaybillList.get(0);
                long payee = casPaybill.getLong("payee");
                DynamicObject supplier = BusinessDataServiceHelper.loadSingle(payee, "bd_supplier");
                String unittype = supplier.getString("nckd_unittype");
                //不为正常单位
                if ("A" != unittype) {
                    RefObject<String> afterConfirm = new RefObject<>();
                    // 自定义操作参数中，没有afterconfirm参数：说明是首次执行付款操作，需要提示用户确认
                    if (!formOperate.getOption().tryGetVariableValue(OPPARAM_AFTERCONFIRM, afterConfirm)) {
                        // 显示确认消息
                        ConfirmCallBackListener confirmCallBacks = new ConfirmCallBackListener(KEY_BEFORESUBMIT, this);
                        this.getView().showConfirm("供应商为" + unittypeMap.get(unittype) + "的单位，是否继续提交?", MessageBoxOptions.YesNo, ConfirmTypes.Default, confirmCallBacks);
                        // 在没有确认之前，先取消本次操作
                        args.setCancel(true);
                    }
                }
            }
        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs args) {
        super.afterDoOperation(args);
        FormOperate formOperate = (FormOperate) args.getSource();
        //批量确认背书
        if (StringUtils.equals(KEY_BATCHENDORSE, formOperate.getOperateKey())) {
            //获取列表选中数据
            BillList billlistap = this.getView().getControl("billlistap");
            ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
            EntityType entityType = billlistap.getEntityType();
            //获取选中行pkid
            Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
            //获取完整数据
            DynamicObject[] casPaybillArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
            //获取结算号 settletnumber
            Set<String> settletnumber = Arrays.stream(casPaybillArr).map(e -> {
                DynamicObject draftbill = e.getDynamicObjectCollection("draftbill").get(0);
                DynamicObject dy = (DynamicObject) draftbill.get(1);
                return dy.getString("draftbillno");
            }).collect(Collectors.toSet());
            //cdm_drafttradebill
            //创建弹出列表界面对象，ListShowParameter 表示弹出页面为列表界面
            ListShowParameter listShowParameter = new ListShowParameter();
            //设置F7列表表单模板 F7选择列表界面：bos_listf7 普通列表界面：bos_list
            listShowParameter.setFormId("bos_list");
            //设置BillFormId为基础资料的标识
            listShowParameter.setBillFormId("cdm_drafttradebill");
            //设置弹出页面标题
            //listShowParameter.setCaption("人员同步选择界面");
            //设置弹出页面的打开方式
            listShowParameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
            List<QFilter> qFilters = listShowParameter.getListFilterParameter().getQFilters();
            qFilters.add(new QFilter("entrys.draftbill.draftbillno", QCP.in, settletnumber));
            this.getView().showForm(listShowParameter);
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        if (StringUtils.equals(KEY_BEFORESUBMIT, messageBoxClosedEvent.getCallBackId())) {
            // 提交确认
            if (messageBoxClosedEvent.getResult() == MessageBoxResult.Yes) {
                // 确认执行提交操作
                // 构建操作自定义参数，标志为确认后再次执行操作，避免重复显示交互提示
                OperateOption operateOption = OperateOption.create();
                operateOption.setVariableValue(OPPARAM_AFTERCONFIRM, "true");

                // 执行提交操作，并传入自定义操作参数
                this.getView().invokeOperation(KEY_BEFORESUBMIT, operateOption);
            }
        }

    }
}