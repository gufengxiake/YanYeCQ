package nckd.yanye.scm.plugin.form;

import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.exception.KDBizException;
import kd.bos.form.IFormView;
import kd.bos.form.control.Control;
import kd.bos.form.plugin.AbstractFormPlugin;
import nckd.yanye.scm.common.utils.ZcPlatformJsonUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;


/**
 * 招采公告作废弹窗-表单插件
 * 单据标识：nckd_zccancelorder
 *
 * @author liuxiao
 * @since 2024-08-19
 */
public class ZcCancelOrderFormPlugin extends AbstractFormPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners("btnok");
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        IFormView view = this.getView();
        // 显隐
        // 公告发布日期
        view.setVisible("-1".equals(model.getValue("nckd_closereason")), "nckd_otherreason");
    }

    @Override
    public void afterBindData(EventObject e) {
        this.getView().setVisible(false, "nckd_otherreason");
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control) evt.getSource();
        String sourceKey = source.getKey();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("closeReason", String.valueOf(this.getModel().getValue("nckd_closereason")));
        hashMap.put("otherReason", String.valueOf(this.getModel().getValue("nckd_otherreason")));
        hashMap.put("title", String.valueOf(this.getModel().getValue("nckd_title")));
        hashMap.put("content", String.valueOf(this.getModel().getValue("nckd_content")));
        // 附件
        hashMap.put("attachments", this.getModel().getValue("nckd_closeattachmentids"));

        //监听确定按钮
        if ("btnok".equals(sourceKey)) {
            this.getView().returnDataToParent(hashMap);
            if (this.getModel().getValue("nckd_closereason") == null) {
                throw new KDBizException("请选择关闭原因");
            }
            if ("-1".equals(this.getModel().getValue("nckd_closereason"))) {
                if (StringUtils.isEmpty((String) this.getModel().getValue("nckd_otherreason"))) {
                    throw new KDBizException("请填写其他原因");
                }
            }
            if (StringUtils.isEmpty((String) this.getModel().getValue("nckd_title"))) {
                throw new KDBizException("请填写公告标题");
            }
            if (StringUtils.isEmpty((String) this.getModel().getValue("nckd_content"))) {
                throw new KDBizException("请填写公告内容");
            }

            this.getView().close();
        }
    }
}
