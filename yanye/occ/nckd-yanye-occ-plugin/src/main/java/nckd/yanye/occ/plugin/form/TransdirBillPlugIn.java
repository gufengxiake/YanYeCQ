package nckd.yanye.occ.plugin.form;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.RowDataEntity;
import kd.bos.entity.datamodel.events.AfterAddRowEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
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
import kd.bos.servicehelper.user.UserServiceHelper;

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
        BasedataEdit wareHoseEdit = this.getView().getControl("outwarehouse");
        wareHoseEdit.addBeforeF7SelectListener(this);
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name = evt.getProperty().getName();
        if (name.equalsIgnoreCase("warehouse")) {
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            if (billtype == null) {
                return;
            }
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if (id.equals("1980435041267748864") || nameq.equalsIgnoreCase("借货单")) {
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            } else {
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.not_equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            }

        } else if (name.equalsIgnoreCase("outwarehouse")) {
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            if (billtype == null) {
                return;
            }
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if (id.equals("1980435141796826112") || nameq.equalsIgnoreCase("借货归还单")) {
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            } else {
                ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
                List<QFilter> qFilters = new ArrayList<>();
                qFilters.add(new QFilter("nckd_isjh", QCP.not_equals, "1"));
                formShowParameter.getListFilterParameter().setQFilters(qFilters);
            }
        }


    }
    @Override
    public void afterCreateNewData(EventObject e) {
        DynamicObject user= UserServiceHelper.getCurrentUser("id,number,name");
        if(user!=null){
            String number=user.getString("number");
            this.getModel().setItemValueByNumber("nckd_ywy",number);
        }
    }
//    @Override
//    public void propertyChanged(PropertyChangedArgs e) {
//        String propName = e.getProperty().getName();
//        if ("billtype".equals(propName)) {
//            DynamicObject billtype = (DynamicObject) e.getChangeSet()[0].getNewValue();
//            String nameq = billtype.getString("name");
//            Object id = billtype.getPkValue();
//            DynamicObject org = (DynamicObject) this.getModel().getValue("org", 0);
//            Object orgId = org.getPkValue();
//            // 构造QFilter
//            QFilter qFilter = new QFilter("nckd_isjh", QCP.equals, "1").and("createorg.id", QCP.equals, orgId);
//            // 将选中的id对应的数据从数据库加载出来
//            DynamicObjectCollection collections = QueryServiceHelper.query("bd_warehouse",
//                    "id", qFilter.toArray(), "");
//            if(collections.isEmpty()){return;}
//            DynamicObject stock = collections.get(0);
//            String stockId = stock.getString(("id"));
//            int row = this.getModel().getEntryRowCount("billentry");
//            if (id.equals("1980435141796826112") || nameq.equalsIgnoreCase("借货归还单")) {
//
//                for (int i = 0; i < row; i++) {
//
//                    this.getModel().setItemValueByID("outwarehouse", stockId, i);
//                }
//            } else if (id.equals("1980435041267748864") || nameq.equalsIgnoreCase("借货单")) {
//                for (int i = 0; i < row; i++) {
//
//                    this.getModel().setItemValueByID("warehouse", stockId, i);
//                }
//            }
//        }
//    }

//    @Override
//    public void afterAddRow(AfterAddRowEventArgs e) {
//        super.afterAddRow(e);
//        if ("billentry".equals(e.getEntryProp().getName())) {
//            DynamicObject org = (DynamicObject) this.getModel().getValue("org", 0);
//            Object orgId = org.getPkValue();
//            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
//            String nameq = billtype.getString("name");
//            Object id = billtype.getPkValue();
//            // 构造QFilter
//            QFilter qFilter = new QFilter("nckd_isjh", QCP.equals, "1").and("createorg.id", QCP.equals, orgId);
//            // 将选中的id对应的数据从数据库加载出来
//            DynamicObjectCollection collections = QueryServiceHelper.query("bd_warehouse",
//                    "id", qFilter.toArray(), "");
//            DynamicObject stock = collections.get(0);
//            String stockId = stock.getString(("id"));
//            RowDataEntity[] rowdata = e.getRowDataEntities();
//            if (id.equals("1980435141796826112") || nameq.equalsIgnoreCase("借货归还单")) {
//
//                for (RowDataEntity rowDataEntity : rowdata) {
//                    int currentindex = rowDataEntity.getRowIndex();
//                    this.getModel().setItemValueByID("outwarehouse", stockId, currentindex);
//                }
//            } else if (id.equals("1980435041267748864") || nameq.equalsIgnoreCase("借货单")) {
//                for (RowDataEntity rowDataEntity : rowdata) {
//                    int currentindex = rowDataEntity.getRowIndex();
//                    this.getModel().setItemValueByID("warehouse", stockId, currentindex);
//                }
//            }
//        }
//    }

    @Override
    public void itemClick(ItemClickEvent e) {
        super.itemClick(e);

        String itemKey = e.getItemKey();
        //一键还货按钮
        if (itemKey.equalsIgnoreCase("nckd_return")) {

            //单据类型
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            if (billtype == null) {
                this.getView().showErrorNotification("请先维护单据类型");
                return;
            }
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if (id.equals("1980435141796826112") || nameq.equalsIgnoreCase("借货归还单")) {
                DynamicObject ywy = (DynamicObject) this.getModel().getValue("nckd_ywy", 0);
                if (ywy == null) {
                    this.getView().showErrorNotification("请先维护业务员!");
                    return;
                }
                Object ywyId = ywy.getPkValue();
                DynamicObject dept = (DynamicObject) this.getModel().getValue("dept", 0);
                if (dept == null) {
                    this.getView().showErrorNotification("请先维护调入部门!");
                    return;
                }
                DynamicObject org = (DynamicObject) this.getModel().getValue("org", 0);
                Object orgId = org.getPkValue();
                ListShowParameter listPara = ShowFormHelper.createShowListForm("nckd_xsyjhyebf", true);//第二个参数为是否支持多选;
                ListFilterParameter listFilterParameter = new ListFilterParameter();
                listFilterParameter.setFilter(new QFilter("nckd_qty", QCP.not_equals, 0)
                        .and("nckd_fapplyuserid.id", QCP.equals, ywyId)
                        .and("nckd_orgfield.id", QCP.equals, orgId));
                listPara.setListFilterParameter(listFilterParameter);
                // 设置回调
                listPara.setCloseCallBack(new CloseCallBack(this, "return"));
                this.getView().showForm(listPara);
            } else {
                this.getView().showErrorNotification("单据类型不为借货归还单,请修改!");
            }


        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {

        super.closedCallBack(closedCallBackEvent);
        String key = closedCallBackEvent.getActionId();
        // 接收回调
        if (StringUtils.equalsIgnoreCase("return", key) && closedCallBackEvent.getReturnData() instanceof ListSelectedRowCollection) {
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
                    "id,nckd_fmaterialid.number number,nckd_qty,nckd_fwarehouseid.number stocknumber,nckd_lotnum", qFilter.toArray(), "");
            if (collections.size() > 0) {
                //清空单据体
                this.getModel().deleteEntryData("billentry");
                this.getModel().batchCreateNewEntryRow("billentry", collections.size());
                DynamicObject dept = (DynamicObject) this.getModel().getValue("dept", 0);
                Object deptId = dept.getPkValue();
                DynamicObject org = (DynamicObject) this.getModel().getValue("org", 0);
                Object orgId = org.getPkValue();
                //从部门 仓库设置基础资料中获取对应仓库
                // 构造QFilter
                QFilter sFilter = new QFilter("createorg", QCP.equals, orgId)
                        .and("status", QCP.equals, "C")
                        .and("nckd_bm", QCP.equals, deptId);

                //查找部门对应仓库
                DynamicObjectCollection stockDycll = QueryServiceHelper.query("nckd_bmcksz",
                        "id,nckd_ck.number number", sFilter.toArray(), "modifytime");
                String number = "";
                if (!stockDycll.isEmpty()) {
                    DynamicObject stockItem = stockDycll.get(0);
                    number = stockItem.getString("number");
                }
                int row = 0;
                for (DynamicObject object : collections) {
                    Object matId = object.get("number");//物料编码
                    BigDecimal qty = object.getBigDecimal("nckd_qty");//库存数量
                    String stockNumber = object.getString("stocknumber");//仓库编码
                    String lotNum = object.getString("nckd_lotnum");//批号
                    this.getModel().setItemValueByNumber("material", matId.toString(), row);
                    this.getModel().setValue("qty", qty, row);
                    this.getModel().setItemValueByNumber("outwarehouse", stockNumber, row);//调出仓库
                    this.getModel().setItemValueByNumber("warehouse", number, row);//调入仓库
                    this.getModel().setValue("lotnumber", lotNum, row);//调出批号
                    this.getModel().setValue("inlotnumber", lotNum, row);//调入批号
                    this.getModel().setItemValueByNumber("lot", lotNum, row);//调出批号主档
                    this.getModel().setItemValueByNumber("inlot", lotNum, row);//调入批号主档
                    row++;
                }
            }

        }
    }
}
