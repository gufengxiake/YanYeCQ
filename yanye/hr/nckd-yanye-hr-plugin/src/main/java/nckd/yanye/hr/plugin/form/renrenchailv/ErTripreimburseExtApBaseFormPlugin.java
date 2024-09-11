package nckd.yanye.hr.plugin.form.renrenchailv;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;

import java.util.EventObject;

/**
 * Module           :财务云-费用核算-人人差旅单据
 * Description      :差旅报销单单据插件
 * nckd_er_tripreimburse_ext
 * @author : yaosijie
 * @date : 2024/9/10
 */
public class ErTripreimburseExtApBaseFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        ChangeData changeData = e.getChangeSet()[0];
        int rowIndex = changeData.getRowIndex();
        IDataModel model = this.getModel();
        Object newValue = changeData.getNewValue();
        if ("nckd_collectionpeople".equals(fieldKey)){
            model.setValue("payer", newValue, rowIndex);
        }
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //收款信息基础资料点击前的监听
        BasedataEdit materielfieldEdit = this.getView().getControl("nckd_collectionpeople");
        materielfieldEdit.addBeforeF7SelectListener(this);
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        DynamicObject orgObject = (DynamicObject)this.getModel().getValue("company");
        if (name.equals("nckd_collectionpeople")){
            //构造收款信息查询条件，createorg.id 创建组织，billstatus：数据状态（C:已审核），enable：使用状态（1:可用）
            QFilter qFilter = new QFilter("createorg.id", QCP.equals,orgObject.getPkValue())
                    .and("status",QCP.equals,"C").and("enable",QCP.equals,"1");
            beforeF7SelectEvent.getCustomQFilters().add(qFilter);
        }

    }
}
