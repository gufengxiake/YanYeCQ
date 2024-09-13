package nckd.yanye.occ.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.control.Control;
import kd.bos.form.plugin.AbstractFormPlugin;

import java.util.EventObject;

/**
 * Module           :全渠道云-支付配置
 * Description      :弹框输入订单号和金额，带回到父页面
 *
 * @author : zhujintao
 * @date : 2024/9/13
 */
public class RefundReturnDataToParentFromPlugin extends AbstractFormPlugin {

    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners(new String[]{"btnok"});
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control c = (Control) evt.getSource();
        switch (c.getKey().toLowerCase()) {
            case "btnok":
                this.btnOk();

        }
    }

    private void btnOk() {
        // 构建返回的数据
        DynamicObject dataEntity = this.getModel().getDataEntity();
        this.getView().returnDataToParent(dataEntity);
        this.getView().close();
    }
}
