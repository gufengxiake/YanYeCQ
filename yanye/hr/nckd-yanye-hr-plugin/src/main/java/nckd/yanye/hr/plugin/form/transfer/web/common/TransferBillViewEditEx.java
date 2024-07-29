package nckd.yanye.hr.plugin.form.transfer.web.common;

import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.form.IFormView;
import kd.bos.form.control.Label;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.hr.hbp.formplugin.web.HRCoreBaseBillEdit;
import kd.hr.hdm.common.transfer.util.ObjectUtils;
import kd.hr.hdm.common.transfer.util.TransferCommonUtil;
import kd.hr.hdm.common.transfer.util.TransferJudgementUtil;
import kd.sdk.plugin.Plugin;
import org.apache.commons.lang3.StringUtils;

import java.util.EventObject;

/**
 * 动态表单插件
 */
public class TransferBillViewEditEx extends HRCoreBaseBillEdit  {

    /**
     * 2024-07-29 Tyx
     * 处理单据查看时职级及干部类型标签赋值
     * @param e
     */
    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        IDataModel dataModel = this.getModel();
        IFormView formView = this.getView();
        DynamicObject dataEntity = dataModel.getDataEntity();
        String billStatus = dataEntity.getString("billstatus");
        OperationStatus status = formView.getFormShowParameter().getStatus();
        if (TransferJudgementUtil.VIEW.test(status)) {
            this.setTransferInfo(dataModel, formView);
        }
    }

    private void setTransferInfo(IDataModel model, IFormView view) {
        //调出扩展字段
        TransferCommonUtil.setLblBaseDataText(view, model, "nckd_lblzhijisource", "nckd_oldzhiji", Boolean.TRUE);
        TransferCommonUtil.setLblBaseDataText(view, model, "nckd_lblganbutypesource", "nckd_oldganbutype", Boolean.TRUE);
        //调入扩展字段
        this.setLabelText("nckd_lblzhijitarget","nckd_zhiji");
        this.setLabelText("nckd_lblganbutypetarget","nckd_ganbutype");
    }

    private void setLabelText(String labelName, String propertyName) {
        Label lblsource = (Label)this.getView().getControl(labelName);
        DynamicObject dy = (DynamicObject)this.getModel().getValue(propertyName);
        if (!ObjectUtils.isEmpty(dy)) {
            if (StringUtils.isNotEmpty(dy.getString("name"))) {
                lblsource.setText(dy.getString("name"));
            }
        } else {
            lblsource.setText("-");
        }

    }
}