package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class TransdirBillPlugIn extends AbstractBillPlugIn implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit inWareHouseEdit = this.getView().getControl("warehouse");
        inWareHouseEdit.addBeforeF7SelectListener(this);
        BasedataEdit wareHoseEdit=this.getView().getControl("outwarehouse");
        wareHoseEdit.addBeforeF7SelectListener(this);
    }
    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name= evt.getProperty().getName();
        if(name.equalsIgnoreCase("warehouse")){
            DynamicObject billtype= (DynamicObject) this.getModel().getValue("billtype",0);
            String nameq=billtype.getString("name");
            Object id=billtype.getPkValue();
            if(id.equals("1980435041267748864")||nameq.equalsIgnoreCase("借货单")){
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            }

        }else if(name.equalsIgnoreCase("outwarehouse")){
            DynamicObject billtype= (DynamicObject) this.getModel().getValue("billtype",0);
            String nameq=billtype.getString("name");
            Object id=billtype.getPkValue();
            if(id.equals("1980435141796826112")||nameq.equalsIgnoreCase("借货归还单")){
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            }
        }




    }
    @Override
    public void itemClick(ItemClickEvent e) {
        super.itemClick(e);

        String itemKey = e.getItemKey();
        //一键还货按钮
        if (itemKey.equalsIgnoreCase("nckd_return")) {
            DynamicObject ywy = (DynamicObject) this.getModel().getValue("nckd_ywy", 0);
            if (ywy == null) {
                this.getView().showErrorNotification("请先维护业务员!");
                return;
            }
            Object ywyId = ywy.getPkValue();
            //单据类型
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            if(billtype==null){
                this.getView().showErrorNotification("请先维护单据类型");
                return;
            }
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if(id.equals("1980435141796826112") || nameq.equalsIgnoreCase("借货归还单")){
                DynamicObject org= (DynamicObject) this.getModel().getValue("org",0);
                Object orgId=org.getPkValue();
                ListShowParameter listPara = ShowFormHelper.createShowListForm("nckd_xsyjhyebf", true);//第二个参数为是否支持多选;
                ListFilterParameter listFilterParameter = new ListFilterParameter();
                listFilterParameter.setFilter(new QFilter("nckd_qty", QCP.not_equals, 0)
                        .and("nckd_fapplyuserid.id", QCP.equals, ywyId)
                        .and("nckd_orgfield.id",QCP.equals,orgId));
                listPara.setListFilterParameter(listFilterParameter);
                // 设置回调
                listPara.setCloseCallBack(new CloseCallBack(this, "return"));
                this.getView().showForm(listPara);
            }
            else {
                this.getView().showErrorNotification("单据类型不为借货归还单,请修改!");
            }


        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {

        super.closedCallBack(closedCallBackEvent);
        // 接收回调
        if (closedCallBackEvent.getReturnData() instanceof ListSelectedRowCollection) {
            ListSelectedRowCollection selectCollections = (ListSelectedRowCollection) closedCallBackEvent.getReturnData();
            ArrayList list = new ArrayList();
            for (ListSelectedRow row : selectCollections) {
                // list存储id
                list.add(row.getPrimaryKeyValue());
            }
            // 构造QFilter
            QFilter qFilter = new QFilter("id", QFilter.in, list);

            // 将选中的id对应的数据从数据库加载出来
            DynamicObjectCollection collections = QueryServiceHelper.query("nckd_xsyjhyebf",
                    "id,nckd_fmaterialid.number number,nckd_qty", qFilter.toArray(), "");
            if(collections.size()>0){
                //清空单据体
                this.getModel().deleteEntryData("billentry");
                this.getModel().batchCreateNewEntryRow("billentry",collections.size());
                int row=0;
                for (DynamicObject object : collections) {
                    Object matId= object.get("number");
                    BigDecimal qty= object.getBigDecimal("nckd_qty");
                    this.getModel().setItemValueByNumber("material",matId.toString(),row);
                    this.getModel().setValue("qty",qty,row);
                    row++;
                }
            }

        }
    }
}
