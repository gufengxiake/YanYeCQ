package nckd.yanye.fi.plugin.form;


import kd.bos.entity.filter.ControlFilter;
import kd.bos.entity.filter.ControlFilters;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.plugin.AbstractListPlugin;
import nckd.base.common.utils.capp.CacheBusinessData;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;


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

public class VoucherListPlugin extends AbstractListPlugin {


    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        FormOperate opreate = (FormOperate) args.getSource();
        String operateKey = opreate.getOperateKey();
//        if(operateKey.equals("new")){
//            ControlFilters controlFilters = this.getControlFilters();
//            Map<String, ControlFilter> filters = controlFilters.getFilters();
//            ControlFilter bookeddate = filters.get("bookeddate");
//            List<Object> value = bookeddate.getValue();
//            if(ObjectUtils.isNotEmpty(value)){
//                Object o = value.get(0);
//                this.getPageCache().put("bookeddate",o.toString());
//                //  10 本周
//                // 865760366578131968 本期
//                // 865760791251411968 上一期
//                // 865760640340354048 下一期
//                // 13 今天
//                // 本月 63
//                // 24 过去3个月
//                // 8 下月
//
//
//
//
//            }
//
//
//        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        String operateKey = afterDoOperationEventArgs.getOperateKey();
        if(operateKey.equals("new")){
            ControlFilters controlFilters = this.getControlFilters();
            Map<String, ControlFilter> filters = controlFilters.getFilters();
            ControlFilter bookeddate = filters.get("bookeddate");
            List<Object> value = bookeddate.getValue();
            if(ObjectUtils.isNotEmpty(value)){
                Object o = value.get(0);
                CacheBusinessData.set("bookeddate",this.getView().getFormShowParameter().getRootPageId(),o.toString());

                //  10 本周
                // 865760366578131968 本期
                // 865760791251411968 上一期
                // 865760640340354048 下一期
                // 13 今天
                // 本月 63
                // 24 过去3个月
                // 8 下月




            }


        }
    }
}
