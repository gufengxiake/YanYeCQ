package nckd.yanye.hr.plugin.form.kaoqin;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.fi.ap.formplugin.ApBaseEdit;
import org.apache.commons.lang3.ObjectUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;

/**
 * Module           :工时假勤云-加班管理-加班申请
 * Description      :值班申请插件
 *
 * @author guozhiwei
 * @date  2024-08-30 9：15
 *
 */
public class OtApplyBillSelfPPlugin extends ApBaseEdit {


    public void registerListener(EventObject e) {
        super.registerListener(e);
//        this.addClickListeners(new String[]{"nckd_advconbaritemap2"});
//        this.addItemClickListeners("advcontoolbarap2");
        this.addItemClickListeners("advcontoolbarap2");
        this.addClickListeners(new String[]{"btnok"});

//        this.filterMaterialVersion();
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        String key = evt.getItemKey();
        //监听新增分录按钮
        if (key.equals("nckd_advconbaritemap2")) {
//            System.out.println("1");
            // 展示动态表单
            FormShowParameter formShowParameter = new FormShowParameter();
            // 弹窗案例-动态表单 页面标识
            formShowParameter.setFormId("nckd_duty_week");
            // 自定义传参，把当前单据的文本字段传过去
            formShowParameter.setCustomParam("org", ((DynamicObject)this.getModel().getValue("org")).getPkValue());
            // 设置回调事件，回调插件为当前插件，标识为kdec_sfform
            formShowParameter.setCloseCallBack(new CloseCallBack(this,"nckd_duty_week"));
            // 设置打开类型为模态框（不设置的话指令参数缺失，没办法打开页面）
            formShowParameter.getOpenStyle().setShowType(ShowType.Modal);
            // 当前页面发送showform指令。注意也可以从其他页面发送指令，后续有文章介绍
            this.getView().showForm(formShowParameter);

        }
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control)evt.getSource();
//        switch (c.getKey().toLowerCase()) {
//            case "nckd_advconbaritemap2":
//                break;
//        }
    }
    public void closedCallBack(ClosedCallBackEvent e) {
        super.closedCallBack(e);
        String id = e.getActionId();
        Object returnData = e.getReturnData();
        switch (id) {
            case "nckd_duty_week":
                this.closeassaccountF7(returnData);
//                this.closeassaccountF7(returnData);
                break;
        }

    }

    // f7 返回参数
    private void closeassaccountF7(Object returnData) {
        if(ObjectUtils.isEmpty(returnData)){
            return;
        }
        // 获取分录数据
        IDataModel model = this.getModel();
        DynamicObjectCollection scentry = new DynamicObjectCollection();
        //this.getModel().getDataEntity().getDynamicObjectCollection("sdentry");

        // 处理返回参数
        for (DynamicObject returnDatum : (DynamicObjectCollection) returnData) {
//            DynamicObject dynamicObject = scentry.addNew();
            scentry =  model.getEntryEntity("sdentry");
            int i = 0;
            // 如果不为初始数据，则新增行
            if(scentry.size() != 1){
                // 新增行号
                i = model.insertEntryRow("sdentry", scentry.size() + 1);
            }
            addNewEntry(i,returnDatum,0);

//            System.out.println(returnDatum);
//            // 加班类型
//            dynamicObject.set("sdottype",returnDatum.getDynamicObject("nckd_sdottype"));
//            // 值班类型
//            dynamicObject.set("nckd_dutytype",returnDatum.getDynamicObject("nckd_dutytype"));
//            //加班日期
//            dynamicObject.set("otdutydate",returnDatum.getDate("nckd_otdutydate"));
//            //开始日期
//            dynamicObject.set("otstartdate",returnDatum.getDate("nckd_otstartdate"));
//            //结束日期
//            dynamicObject.set("otenddate",returnDatum.getDate("nckd_otenddate"));
//            // 补偿方式
//            dynamicObject.set("compentyped",returnDatum.get("nckd_compentyped"));
//            // 加班原因
//            dynamicObject.set("otresond",returnDatum.getString("nckd_textfield"));


            int days = returnDatum.getInt("nckd_dutydays");

            // 判断值班天数是否超过1天
            if(days>1){
                // todo 新增两条,只改变日期
//                int j = model.insertEntryRow("sdentry", scentry.size() + 1);
                // 获取初始值班日期数据
                Date nckdOtdutydate = returnDatum.getDate("nckd_otdutydate");
                Date nckdOtstartdate = returnDatum.getDate("nckd_otstartdate");
                Date nckdOtenddate = returnDatum.getDate("nckd_otenddate");
                returnDatum.set("nckd_dutydays",1);
                for (int i1 = 0; i1 <= days -2; i1++) {
                    int index = i + i1 +1;
                    returnDatum.set("nckd_otdutydate",combineDateAddDays(nckdOtdutydate, i1+1));
                    returnDatum.set("nckd_otstartdate",combineDateAddDays(nckdOtstartdate, i1+1));
                    returnDatum.set("nckd_otenddate",combineDateAddDays(nckdOtenddate, i1+1));
                    int sdentry = model.insertEntryRow("sdentry", index);
                    addNewEntry(sdentry,returnDatum,1);
                }


//                addNewEntry(i+1,dynamicObject,0);
//                DynamicObject dynamicObject2 = scentry.addNew();
//                dynamicObject2  = dynamicObject;
//                dynamicObject2.getDate("dynamicObject2")
//                dynamicObject2.set("dynamicObject2",+1);
            }


        }
        this.getView().updateView();
//        DynamicObjectCollection scentry = this.getModel().getEntryEntity("scentry");
//        DynamicObject dynamicObject = scentry.addNew();


    }

    private void addNewEntry(int row,DynamicObject returnDatum,int days) {
        IDataModel model = this.getModel();
        // 加班类型
        model.setValue("sdottype",returnDatum.getDynamicObject("nckd_sdottype"),row);
        // 值班类型
        model.setValue("nckd_dutytype",returnDatum.getDynamicObject("nckd_dutytype"),row);
        //加班日期
        model.setValue("otdutydate",returnDatum.getDate("nckd_otdutydate"),row);
        //开始日期
        model.setValue("otstartdate",returnDatum.getDate("nckd_otstartdate"),row);
        // 获取值班天数，重新计算结束日期
        int nckdDutydays = returnDatum.getInt("nckd_dutydays");
        if(nckdDutydays>1){
            Date nckdOtenddate = returnDatum.getDate("nckd_otenddate");
            returnDatum.set("nckd_otenddate",combineDateAddDays(nckdOtenddate,1-nckdDutydays));
        }
        //结束日期
        model.setValue("otenddate",returnDatum.getDate("nckd_otenddate"),row);
        // 补偿方式
        model.setValue("compentyped",returnDatum.get("nckd_compentyped"),row);
        // 加班原因
        model.setValue("otresond",returnDatum.getString("nckd_otresond"),row);

    }

    private static Date combineDateAddDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }



}
