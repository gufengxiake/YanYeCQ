package nckd.yanye.hr.plugin.form.renrenchailv;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.BizDataEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

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
        if ("nckd_collectionpeople".equals(fieldKey) && newValue != null){
            model.setValue("payer", newValue, rowIndex);
            DynamicObject dynamicObject = (DynamicObject) newValue;
            model.setValue("payeraccount02",dynamicObject.getString("payeraccount02"));
        }
        if ("payer".equals(fieldKey) && newValue != null){
            model.setValue("nckd_collectionpeople", newValue, rowIndex);
            DynamicObject dynamicObject = (DynamicObject) newValue;
            model.setValue("payeraccount02",dynamicObject.getString("payeraccount02"));
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
            //构造人员查询条件，bos_user.entryentity.orgstructure.longnumber 创建组织，billstatus：数据状态（C:已审核），enable：使用状态（1:可用）
            QFilter qFilter = new QFilter("entryentity.orgstructure.longnumber", QCP.like,"%"+orgObject.getString("number")+"%");
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("bos_user", "id", new QFilter[]{qFilter});
            List<Object> list = Arrays.stream(dynamicObjects).map(t->t.getPkValue()).collect(Collectors.toList());
            //构造收款信息查询条件 billstatus：数据状态（C:已审核），enable：使用状态（1:可用）
            QFilter skqFilter = new QFilter("status",QCP.equals,"C").and("enable",QCP.equals,"1")
                    .and("payer",QCP.in,list);
            beforeF7SelectEvent.getCustomQFilters().add(skqFilter);
        }

    }

    @Override
    public void createNewData(BizDataEventArgs e) {
        super.createNewData(e);

    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
    }
}
