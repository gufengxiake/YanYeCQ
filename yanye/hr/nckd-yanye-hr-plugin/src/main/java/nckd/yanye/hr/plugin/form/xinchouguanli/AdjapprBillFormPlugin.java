package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.Control;

import java.util.EventObject;

/**
 * 员工定调薪申请单-表单插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 *
 * @author liuxiao
 * @since 2024/08/19
 */
public class AdjapprBillFormPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        switch (e.getProperty().getName()) {
            case "":

            default:
                break;
        }
    }


    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control)evt.getSource();
        String key = source.getKey();
        System.out.println(1);
    }


}
