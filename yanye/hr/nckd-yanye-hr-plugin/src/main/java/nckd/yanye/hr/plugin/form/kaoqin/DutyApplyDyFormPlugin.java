package nckd.yanye.hr.plugin.form.kaoqin;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.bos.form.field.BasedataEdit;
import kd.fi.cas.formplugin.common.DynamicFormPlugin;
import org.apache.commons.lang3.ObjectUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Module           :工时假勤云-加班管理-加班申请
 * Description      :值班申请动态表单·插件
 *
 * @author guozhiwei
 * @date  2024-09-2 9：15
 *
 */
public class DutyApplyDyFormPlugin extends DynamicFormPlugin {

    // 值班类型
    private final Map<String,String> dutyTypeMap = new HashMap<String, String>() {{
        put("GZRWB-01", "17:00-8:30");
        put("LXLT-01", "8:30-8:30");
        put("ZM-24", "8:30-8:30");
        put("ZMBB-01", "8:30-17:30");
        put("ZMWB-01", "17:00-8:30");
    }};


    public void registerListener(EventObject e) {
        super.registerListener(e);
//        this.addClickListeners(new String[]{"nckd_advconbaritemap2"});
//        this.addItemClickListeners("advcontoolbarap2");
        this.addClickListeners(new String[]{"btnok"});

//        this.filterMaterialVersion();
    }

    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        FormShowParameter parameter = this.getView().getFormShowParameter();
        Map<String, Object> paramMap = parameter.getCustomParams();
        IDataModel model = this.getModel();
        this.setValue("nckd_orgfield", paramMap.get("org"));

    }

    public void propertyChanged(PropertyChangedArgs e) {
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        // 获取 下标
        int iRow = changeData[0].getRowIndex();

        if (newValue != oldValue) {
            switch (key) {
                case "nckd_dutytype":
                      this.updateDutyType(this.getModel().getValue("nckd_otdutydate"), newValue,this.getModel().getValue("nckd_dutydays"),iRow);
                    break;
                case "nckd_otdutydate":
                    this.updateDutyType(newValue,this.getModel().getValue("nckd_dutytype"),this.getModel().getValue("nckd_dutydays"),iRow);
                    break;
                case "nckd_dutydays":
                    this.updateDutyType(this.getModel().getValue("nckd_otdutydate"),this.getModel().getValue("nckd_dutytype"),newValue,iRow);
                    break;
                default:
                    break;
            }

        }
    }

    private void updateDutyType(Object date,Object dutyType,Object dutyDays,int iRow) {
        if ( ObjectUtils.isEmpty(date) || ObjectUtils.isEmpty(dutyType) || ObjectUtils.isEmpty(dutyDays)){
            return;
        }
        // 值班类型
        DynamicObject dutyType1 = (DynamicObject) dutyType;
        if(ObjectUtils.isEmpty(dutyType1.getDataStorage())){
            return;
        }

        String dutyTypeStr = dutyType1.getString("number");
        // 获取值班日期
        Date dutyDate = (Date) date;
        String s = dutyTypeMap.get(dutyTypeStr);
        String[] splitStr = s.split("-");
        // 获取开始结束时间
        // 周末白班不用跨日
        int days = "ZMBB-01".equals(dutyTypeStr)?0:1;
        // 获取值班天数
//        Object nckdIntegerfield = this.getModel().getValue("nckd_dutydays", iRow);
        if(ObjectUtils.isNotEmpty(dutyDays)){
            int nckdIntegerfieldValue = (int) dutyDays;
            days = days + nckdIntegerfieldValue-1;
        }
        String startStr = splitStr[0];
        String endStr = splitStr[1];

        Date startTime = combineDateAndTime(dutyDate, startStr, 0);
        Date endTime = combineDateAndTime(dutyDate, endStr,days);
        this.setValue("nckd_otstartdate",startTime,iRow);
        this.setValue("nckd_otenddate",endTime,iRow);

    }
    // 自动计算开始时间，结束时间  值班日期 nckd_datefield，
    // 将 Date 和时间字符串组合成 Date 对象
    private static Date combineDateAndTime(Date date, String timeStr,int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        try {
            Date time = timeFormat.parse(timeStr);
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(time);
            calendar.add(Calendar.DAY_OF_MONTH, days);
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, 0);  // 设置秒为0
            return calendar.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }
    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control)evt.getSource();
//        BasedataEdit payeeacctcashf7;
        switch (c.getKey().toLowerCase()) {
            case "btnok":
                this.btnOk();

        }
    }
    private void btnOk() {
        IDataModel model = this.getModel();
        // 构建返回的数据
        DynamicObjectCollection resultMap = model.getDataEntity(true).getDynamicObjectCollection("nckd_entryentity");
//      this.getModel().getDataEntity(true).getDynamicObjectCollection("nckd_entryentity")
        this.getView().returnDataToParent(resultMap);
        this.getView().close();
    }

}
