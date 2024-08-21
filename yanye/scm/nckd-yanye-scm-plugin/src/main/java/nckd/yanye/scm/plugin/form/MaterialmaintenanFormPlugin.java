package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author husheng
 * @date 2024-08-21 9:28
 * @description 物料维护单-弹框查询
 *   nckd_materialmaintenan
 */
public class MaterialmaintenanFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        BasedataEdit purchaseorgEdit = this.getControl("nckd_purchaseorg");
        purchaseorgEdit.addBeforeF7SelectListener(this);
        BasedataEdit buyerEdit = this.getControl("nckd_buyer");
        buyerEdit.addBeforeF7SelectListener(this);
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        QFilter qFilter = new QFilter("number", QCP.equals,"1");
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bos_adminorg",new QFilter[]{qFilter});
        //创建组织默认江盐集团
        this.getModel().setValue("nckd_createorganiza",dynamicObject);
        this.getModel().setValue("nckd_initiatingdepart", RequestContext.get().getOrgId());
        super.afterCreateNewData(e);
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        List<QFilter> qFilters = new ArrayList<>();
        if ("nckd_buyer".equals(name)) {
            //  根据采购组获取采购员
            DynamicObject purchaseorg = (DynamicObject) this.getModel().getValue("nckd_purchaseorg");
            DynamicObject operatorgroup = BusinessDataServiceHelper.loadSingle(purchaseorg.getPkValue(), "bd_operatorgroup");
            List<Object> objects = operatorgroup.getDynamicObjectCollection("entryentity").stream().map(dynamicObject -> {
                return dynamicObject.getDynamicObject("operator").getPkValue();
            }).collect(Collectors.toList());
            QFilter qFilter = new QFilter("id", QCP.in, objects);
            qFilters.add(qFilter);
        } else if ("nckd_purchaseorg".equals(name)) {
            // 业务组类型是采购组
            QFilter qFilter = new QFilter("operatorgrouptype", QCP.equals, "CGZ");
            qFilters.add(qFilter);
        }
        beforeF7SelectEvent.setCustomQFilters(qFilters);
    }
}
