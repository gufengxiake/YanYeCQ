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
//                invcountscheme.set("nckd_number",dynamicObject.get("number"));//考核量化指标编码
                invcountscheme.set("activity",activityId);//考核活动id
                //根据考核活动id查询
                DynamicObject epaActivityObject = BusinessDataServiceHelper.loadSingle(activityId,"epa_activity");
                DynamicObject executeschemeObject = BusinessDataServiceHelper.loadSingle(epaActivityObject.getDynamicObject("executescheme").getPkValue(),"epa_executescheme");//考核周期执行计划
                DynamicObject scheme = BusinessDataServiceHelper.loadSingle(epaActivityObject.getDynamicObject("scheme").getPkValue(),"epa_scheme");//考核计划
                String period = executeschemeObject.getString("timetag.name");//考核周期：2024M3
                Date startyear = scheme.getDate("startyear");//考核年度
                Long cycletype = scheme.getLong("cycletype.id");//周期类型
                Long cyclescheme = scheme.getLong("cyclescheme.id");//周期方案
                //构造考核量化指标填报查询条件 周期类型，周期方案，考核量化指标
                QFilter quantiFilter = new QFilter("nckd_cycletype.id", QCP.equals,cycletype).and("nckd_cycleprogramme.id",QCP.equals,cyclescheme)
                        .and("nckd_year",QCP.equals,startyear).and("nckd_assessment",QCP.equals,period);
                DynamicObject[] quantizationfillingObject = BusinessDataServiceHelper.load("nckd_quantizationfilling" ,"id,entryentity.nckd_completevalue,entryentity.nckd_examinetarget,entryentity.nckd_completevalue" , new QFilter[]{quantiFilter});
                //取第一条核量化指标填报数据
                DynamicObject quantiDynamicObject = quantizationfillingObject[0];
                //考核量化指标填报分录
                DynamicObjectCollection dynamicObjectCollection = quantiDynamicObject.getDynamicObjectCollection("entryentity");
                List<DynamicObject> objects = dynamicObjectCollection.stream().filter(t-> dynamicObject.getString("id").equals(t.getString("nckd_examinetarget.id"))).collect(Collectors.toList());
                //获取完成值
                BigDecimal completevalue = objects.get(0).getBigDecimal("nckd_completevalue");
//                invcountscheme.set("nckd_completevalue",completevalue);//完成值
                invcountscheme.set("qualityres",completevalue);//完成值
                OperationServiceHelper.executeOperate("save", "epa_genareaind_assign", new DynamicObject[]{invcountscheme}, OperateOption.create());
            }
            //刷新页面
            parentView.invokeOperation("refresh");
            this.getView().sendFormAction(parentView);
        }
    }


}
