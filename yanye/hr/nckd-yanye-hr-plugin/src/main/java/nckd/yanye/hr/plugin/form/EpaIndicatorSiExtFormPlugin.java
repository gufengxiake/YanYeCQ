package nckd.yanye.hr.plugin.form;


import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.IFormView;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.form.control.Label;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Module           :目标绩效云-个人绩效考核-指标制定
 * Description      :添加考核量化指标按钮监听弹框展示基础资料
 * nckd_epa_genareains_a_ext
 * @author : yaosijie
 * @date : 2024/9/29
 */
public class EpaIndicatorSiExtFormPlugin extends AbstractFormPlugin{

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        Label label = this.getView().getControl("nckd_blanktwolabel");
        label.addClickListener(this);
        Button button = this.getView().getControl("nckd_buttonap");
        button.addClickListener(this);

    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control)evt.getSource();
        String key = source.getKey();
        if (key.equals("nckd_blanktwolabel") || key.equals("nckd_buttonap")){
            //打开销售合同列表
            ListShowParameter parameter = ShowFormHelper.createShowListForm("nckd_examinetarget", true);
            //弹框名称
            parameter.setCaption("考核量化指标列表页面");
            //设置回调
            parameter.setCloseCallBack(new CloseCallBack(this, "examinetarget"));
            getView().showForm(parameter);
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        IFormView parentView = this.getView().getParentView();
        DynamicObject activityObject = parentView.getModel().getDataEntity();
        Long activityId = activityObject.getLong("activity.id");
        //常规指标区id
        Object id = this.getModel().getValue("id");
        ListSelectedRowCollection selectCollections = (ListSelectedRowCollection) closedCallBackEvent.getReturnData();
        if ("examinetarget".equals(closedCallBackEvent.getActionId()) && null != selectCollections){
            List<Object> list = new ArrayList();
            for (ListSelectedRow row : selectCollections) {
                // list存储id
                list.add(row.getPrimaryKeyValue());
            }
            // 构造QFilter
            QFilter qFilter = new QFilter("id", QFilter.in, list);
            //查询考核量化指
            DynamicObject[] examinetargetObjects = BusinessDataServiceHelper.load("nckd_examinetarget" ,"id,name,number,nckd_targetvalue,nckd_calculatetype,nckd_addvalue,nckd_addfraction,nckd_examineclassify,nckd_datefield" , new QFilter[]{qFilter});
            for (DynamicObject dynamicObject : Arrays.asList(examinetargetObjects)){
                DynamicObject invcountscheme = BusinessDataServiceHelper.newDynamicObject("epa_genareaind_assign");
                invcountscheme.set("indctrname",dynamicObject.getLocaleString("name"));
                invcountscheme.set("instanceid",id);
                invcountscheme.set("qualitytarget",dynamicObject.get("nckd_targetvalue"));//目标值
                invcountscheme.set("customfiled1",dynamicObject.getString("nckd_calculatetype"));//计算类型
                invcountscheme.set("customfiled2",dynamicObject.getString("nckd_addvalue"));//每增加数值
                invcountscheme.set("customfiled3",dynamicObject.getString("nckd_addfraction"));//增加分数
                invcountscheme.set("customfiled4",dynamicObject.getString("id"));//考核量化指标id
//                invcountscheme.set("nckd_number",dynamicObject.get("number"));//考核量化指标编码
                invcountscheme.set("activity",activityId);//考核活动id
                invcountscheme.set("weight",BigDecimal.ONE);//考核活动id
                OperationServiceHelper.executeOperate("save", "epa_genareaind_assign", new DynamicObject[]{invcountscheme}, OperateOption.create());
            }
            //刷新页面
            parentView.invokeOperation("refresh");
            this.getView().sendFormAction(parentView);
        }
    }


}
