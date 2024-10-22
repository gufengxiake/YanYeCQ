package nckd.yanye.fi.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.entity.operate.Donothing;
import kd.bos.filter.FilterContainer;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.hr.hbp.common.util.DatePattern;
import kd.hr.hbp.common.util.DateUtils;
import nckd.base.common.utils.capp.CacheBusinessData;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 *
 * Module           :财务云-租赁管理-退养人员工资
 * Description      :退休人员工资编辑插件
 *
 * @author guozhiwei
 * @date  2024/10/22 14:01
 *  标识:nckd_gl_voucher_ext
 *
 */

public class VoucherEditPlugin  extends AbstractBillPlugIn {

    public static final Map<String,String> KEY_MAP = new HashMap<>(6);


    // "本期，今天，本周，本月"
    private static final List<String> DATE_KEYS = Arrays.asList(new String[]{"10", "865760366578131968", "13", "63"});
    // “上一期”
    private static final List<String> DATE_KEYS2 = Arrays.asList(new String[]{"865760791251411968"});
    // “下一期，下月”
    private static final List<String> DATE_KEYS3 = Arrays.asList(new String[]{"865760640340354048", "8"});

//                //  10 本周
//                // 865760366578131968 本期
//                // 865760791251411968 上一期
//                // 865760640340354048 下一期
//                // 13 今天
//                // 本月 63
//                // 24 过去3个月
//                // 8 下月

    @Override
    public void afterCreateNewData(EventObject e) {
        // 如果是新增页面。去掉 入职操作
        BillShowParameter bsp = (BillShowParameter) this.getView().getFormShowParameter();
        if (bsp.getStatus() == OperationStatus.ADDNEW) {
            String bookeddate = CacheBusinessData.get("bookeddate", this.getView().getFormShowParameter().getRootPageId());
            CacheBusinessData.remove("bookeddate", this.getView().getFormShowParameter().getRootPageId());

            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(date);
            Date date2 = DateUtils.stringToDate(dateStr, DatePattern.YYYY_MM_DD);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date2);

            // 获取上月最后一天
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置为当前月的第一天
            calendar.add(Calendar.DAY_OF_MONTH, -1); // 上个月最后一天
            Date lastDayOfLastMonth = calendar.getTime();

            // 获取下月最后一天
            calendar.setTime(date2);
            calendar.add(Calendar.MONTH, 2); // 下个月
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置为下个月的第一天
            calendar.add(Calendar.DAY_OF_MONTH, -1); // 最后一天
            Date lastDayOfNextMonth = calendar.getTime();


            if(DATE_KEYS.contains(bookeddate)){
                // 使用今天
                this.getModel().setValue("bizdate",date2);
                this.getModel().setValue("bookeddate",date2);
            }else if(DATE_KEYS2.contains(bookeddate)){
                // 上月月末最后一天
                this.getModel().setValue("bizdate",lastDayOfLastMonth);
                this.getModel().setValue("bookeddate",lastDayOfLastMonth);

            }else if(DATE_KEYS3.contains(bookeddate)){
                // 下月月末最后一天
                this.getModel().setValue("bizdate",lastDayOfNextMonth);
                this.getModel().setValue("bookeddate",lastDayOfNextMonth);
            }


        }

    }

}
