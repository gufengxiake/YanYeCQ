package nckd.yanye.occ.plugin.form;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.time.LocalDate;
import java.util.*;

/*
要货订单表单插件
表单标识：nckd_ocbsoc_saleorder_ext
author:wgq
date:2024/08/20
 */
public class OcbSaleOrderBillPlugIn extends AbstractBillPlugIn implements BeforeF7SelectListener {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit salerIdEdit = this.getView().getControl("nckd_salerid");
        if(salerIdEdit!=null){
            salerIdEdit.addBeforeF7SelectListener(this);
        }
        BasedataEdit operatorEdit = this.getView().getControl("nckd_operatorgroup");
        if(operatorEdit!=null){
            operatorEdit.addBeforeF7SelectListener(this);
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {

        String propName = e.getProperty().getName();
        //订货渠道
        if("orderchannelid".equalsIgnoreCase(propName)){
            DynamicObject orderchannel= (DynamicObject) e.getChangeSet()[0].getNewValue();
            DynamicObject psy=null;
            if(orderchannel!=null){
                //销售片区
                DynamicObject pq=orderchannel.getDynamicObject("nckd_regiongroup");
                if(pq!=null){
                    Object pqId=pq.getPkValue();
                    DynamicObject pqData=BusinessDataServiceHelper.loadSingle(pqId,"nckd_regiongroup");
                    //配送员
                    psy=pqData.getDynamicObject("nckd_deliveryman");
                }
            }
            this.getModel().setValue("nckd_deliveryman",psy);
        }
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name = evt.getProperty().getName();
        if (name.equalsIgnoreCase("nckd_operatorgroup")) {
            //销售组织
            DynamicObject salOrg = (DynamicObject) this.getModel().getValue("saleorgid", 0);
            if (salOrg == null) {
                this.getView().showErrorNotification("请先选择销售组织！");
                evt.setCancel(true);
                return;
            }
            Object orgId = salOrg.getPkValue();
            ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
            List<QFilter> qFilters = new ArrayList<>();
            qFilters.add(new QFilter("createorg.id", QCP.equals, orgId).and("operatorgrouptype", QCP.equals, "XSZ"));
            formShowParameter.getListFilterParameter().setQFilters(qFilters);

        } else if (name.equalsIgnoreCase("nckd_salerid")) {
            DynamicObject operatorGroup = (DynamicObject) this.getModel().getValue("nckd_operatorgroup", 0);
            if (operatorGroup == null) {
                this.getView().showErrorNotification("请先选择业务组！");
                evt.setCancel(true);
                return;
            }
            Object operatorGroupId = operatorGroup.getPkValue();
            ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
            List<QFilter> qFilters = new ArrayList<>();
            qFilters.add(new QFilter("operatorgrpid", QCP.equals, operatorGroupId));
            formShowParameter.getListFilterParameter().setQFilters(qFilters);
        }
    }


    @Override
    public void afterCreateNewData(EventObject e) {

        Long orgId = RequestContext.get().getOrgId();
        if (orgId != 0) {
            // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组
            QFilter qFilter = new QFilter("createorg.id", QCP.equals, orgId)
                    .and("operatorgrouptype", QCP.equals, "XSZ");
            //查找业务组
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_operatorgroup",
                    "id", qFilter.toArray(), "");
            if (!collections.isEmpty()) {
                DynamicObject operatorGroupItem = collections.get(0);
                long operatorGroupId = (long) operatorGroupItem.get("id");
                this.getModel().setItemValueByID("nckd_operatorgroup", operatorGroupId);
                DynamicObject user = UserServiceHelper.getCurrentUser("id,number,name");
                if (user != null && operatorGroupId != 0) {
                    String number = user.getString("number");
                    // 构造QFilter  operatornumber业务员   operatorgrpid 业务组id
                    QFilter Filter = new QFilter("operatornumber", QCP.equals, number)
                            .and("operatorgrpid", QCP.equals, operatorGroupId);
                    //查找业务员
                    DynamicObjectCollection opreatorColl = QueryServiceHelper.query("bd_operator",
                            "id", Filter.toArray(), "");
                    if (!opreatorColl.isEmpty()) {
                        DynamicObject operatorItem = opreatorColl.get(0);
                        String operatorId = operatorItem.getString("id");
                        this.getModel().setItemValueByID("nckd_salerid", operatorId);
                    }
                }
            }

        }
    }

    @Override
    public void afterCopyData(EventObject e) {
        Long orgId = RequestContext.get().getOrgId();
        if (orgId != 0) {
            // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组
            QFilter qFilter = new QFilter("createorg.id", QCP.equals, orgId)
                    .and("operatorgrouptype", QCP.equals, "XSZ");
            //查找业务组
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_operatorgroup",
                    "id", qFilter.toArray(), "");
            if (!collections.isEmpty()) {
                DynamicObject operatorGroupItem = collections.get(0);
                long operatorGroupId = (long) operatorGroupItem.get("id");
                this.getModel().setItemValueByID("nckd_operatorgroup", operatorGroupId);
                DynamicObject user = UserServiceHelper.getCurrentUser("id,number,name");
                if (user != null && operatorGroupId != 0) {
                    String number = user.getString("number");
                    // 构造QFilter  operatornumber业务员   operatorgrpid 业务组id
                    QFilter Filter = new QFilter("operatornumber", QCP.equals, number)
                            .and("operatorgrpid", QCP.equals, operatorGroupId);
                    //查找业务员
                    DynamicObjectCollection opreatorColl = QueryServiceHelper.query("bd_operator",
                            "id", Filter.toArray(), "");
                    if (!opreatorColl.isEmpty()) {
                        DynamicObject operatorItem = opreatorColl.get(0);
                        String operatorId = operatorItem.getString("id");
                        this.getModel().setItemValueByID("nckd_salerid", operatorId);
                    }
                }
            }

        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
    }

}
