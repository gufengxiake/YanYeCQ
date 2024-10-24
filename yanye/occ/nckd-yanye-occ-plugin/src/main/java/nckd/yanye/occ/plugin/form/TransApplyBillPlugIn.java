package nckd.yanye.occ.plugin.form;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
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
import kd.bos.servicehelper.user.UserServiceHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/*
 * 调拨申请单表单插件
 * 表单标识：nckd_im_transapply_ext
 * author:吴国强 2024-07-22
 */
public class TransApplyBillPlugIn extends AbstractBillPlugIn implements BeforeF7SelectListener {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit inWareHouseEdit = this.getView().getControl("inwarehouse");
        if (inWareHouseEdit != null) {
            inWareHouseEdit.addBeforeF7SelectListener(this);
        }
        BasedataEdit wareHoseEdit = this.getView().getControl("warehouse");
        if (wareHoseEdit != null) {
            wareHoseEdit.addBeforeF7SelectListener(this);
        }
        BasedataEdit salerIdEdit = this.getView().getControl("nckd_ywy");
        if (salerIdEdit != null) {
            salerIdEdit.addBeforeF7SelectListener(this);
        }
        BasedataEdit operatorEdit = this.getView().getControl("nckd_operatorgroup");
        if (operatorEdit != null) {
            operatorEdit.addBeforeF7SelectListener(this);
        }
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name = evt.getProperty().getName();
        if (name.equalsIgnoreCase("inwarehouse")) {
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if (id.equals("1994937113375673344") || nameq.equalsIgnoreCase("借货申请")) {
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

        } else if (name.equalsIgnoreCase("warehouse")) {
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if (id.equals("1994937462568258560") || nameq.equalsIgnoreCase("借货归还申请")) {
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
        } else if (name.equalsIgnoreCase("nckd_operatorgroup")) {
            //销售组织
            DynamicObject salOrg = (DynamicObject) this.getModel().getValue("org", 0);
            if (salOrg == null) {
                this.getView().showErrorNotification("请先选择申请组织！");
                evt.setCancel(true);
                return;
            }
            Object orgId = salOrg.getPkValue();
            ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
            List<QFilter> qFilters = new ArrayList<>();
            qFilters.add(new QFilter("createorg.id", QCP.equals, orgId).and("operatorgrouptype", QCP.equals, "XSZ"));
            formShowParameter.getListFilterParameter().setQFilters(qFilters);

        } else if (name.equalsIgnoreCase("nckd_ywy")) {
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
                    Long userId=user.getLong("id");
                    // 构造QFilter  operatornumber业务员   operatorgrpid 业务组id
                    QFilter Filter = new QFilter("operatornumber", QCP.equals, number)
                            .and("operatorgrpid", QCP.equals, operatorGroupId);
                    //查找业务员
                    DynamicObjectCollection opreatorColl = QueryServiceHelper.query("bd_operator",
                            "id", Filter.toArray(), "");
                    if (!opreatorColl.isEmpty()) {
                        DynamicObject operatorItem = opreatorColl.get(0);
                        Long operatorId = operatorItem.getLong("id");
                        this.getModel().setItemValueByID("nckd_ywy", operatorId);
                    }

                    //查找业务员对应的销售片区
                    // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组 entryentity.operator 业务员
                    QFilter gFilter = new QFilter("createorg.id", QCP.equals, orgId)
                            .and("operatorgrouptype", QCP.equals, "XSZ")
                            .and("entryentity.operator.id",QCP.equals,userId);
                    //查找销售片区
                    DynamicObjectCollection gcollections = QueryServiceHelper.query("bd_operatorgroup",
                            "entryentity.nckd_regiongroup regiongroup", gFilter.toArray(), "");
                    if(!gcollections.isEmpty()){
                        DynamicObject regionGroupItem = gcollections.get(0);
                        long regionGroupId = (long) regionGroupItem.get("regiongroup");
                        this.getModel().setItemValueByID("nckd_regiongroup", regionGroupId);
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
                    Long userId=user.getLong("id");
                    // 构造QFilter  operatornumber业务员   operatorgrpid 业务组id
                    QFilter Filter = new QFilter("operatornumber", QCP.equals, number)
                            .and("operatorgrpid", QCP.equals, operatorGroupId);
                    //查找业务员
                    DynamicObjectCollection opreatorColl = QueryServiceHelper.query("bd_operator",
                            "id", Filter.toArray(), "");
                    if (!opreatorColl.isEmpty()) {
                        DynamicObject operatorItem = opreatorColl.get(0);
                        String operatorId = operatorItem.getString("id");
                        this.getModel().setItemValueByID("nckd_ywy", operatorId);
                    }
                    //查找业务员对应的销售片区
                    // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组 entryentity.operator 业务员
                    QFilter gFilter = new QFilter("createorg.id", QCP.equals, orgId)
                            .and("operatorgrouptype", QCP.equals, "XSZ")
                            .and("entryentity.operator.id",QCP.equals,userId);
                    //查找销售片区
                    DynamicObjectCollection gcollections = QueryServiceHelper.query("bd_operatorgroup",
                            "entryentity.nckd_regiongroup regiongroup", gFilter.toArray(), "");
                    if(!gcollections.isEmpty()){
                        DynamicObject regionGroupItem = gcollections.get(0);
                        long regionGroupId = (long) regionGroupItem.get("regiongroup");
                        this.getModel().setItemValueByID("nckd_regiongroup", regionGroupId);
                    }
                }
            }

        }
    }

    @Override
    public void itemClick(ItemClickEvent e) {
        super.itemClick(e);

        String itemKey = e.getItemKey();
        //一键还货按钮
        if (itemKey.equalsIgnoreCase("nckd_return")) {

            //单据类型
            DynamicObject billtype = (DynamicObject) this.getModel().getValue("billtype", 0);
            if (billtype == null) {
                this.getView().showErrorNotification("单据类型为空");
                return;
            }
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            if (id.equals("1994937462568258560") || nameq.equalsIgnoreCase("借货归还申请")) {
                DynamicObject ywy = (DynamicObject) this.getModel().getValue("nckd_ywy", 0);
                if (ywy == null) {
                    this.getView().showErrorNotification("请先维护业务员!");
                    return;
                }
                DynamicObject dept = (DynamicObject) this.getModel().getValue("applydept", 0);
                if (dept == null) {
                    this.getView().showErrorNotification("请先维护申请部门!");
                    return;
                }
                Object ywyId = ywy.getPkValue();
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
                this.getView().showErrorNotification("单据类型不为借货归还申请,请修改!");
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
                DynamicObject dept = (DynamicObject) this.getModel().getValue("applydept", 0);
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
                    this.getModel().setItemValueByNumber("warehouse", stockNumber, row);
                    this.getModel().setItemValueByNumber("inwarehouse", number, row);
                    this.getModel().setValue("lotnumber", lotNum, row);
                    this.getModel().setItemValueByNumber("lot", lotNum, row);

                    row++;
                }
            }

        }
    }
}
