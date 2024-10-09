package nckd.yanye.occ.plugin.form;

import com.alibaba.druid.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

import java.math.BigDecimal;
import java.util.EventObject;
import java.util.HashSet;

/*
 *发货通知单表单插件，选择派车信息单 携带出对应车辆司机
 * 表单标识：nckd_sm_delivernotice_ext
 * author:吴国强 2024-10-09
 */

public class DeliverNoticeBillPlugIn extends AbstractBillPlugIn {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //派车信息单
        TextEdit salecontractno = getControl("nckd_vehicledisp");
        if (salecontractno != null) {
            salecontractno.addClickListener(this);
        }
    }

    @Override
    public void click(EventObject evt) {
        Control control = (Control) evt.getSource();
        String key = control.getKey();
        // 点击派车信息单字段,打开派车信息单据列表界面
        if (StringUtils.equalsIgnoreCase("nckd_vehicledisp", key)) {
            //订货客户
            DynamicObject customer = (DynamicObject) this.getModel().getValue("customer");
            if (customer == null) {
                this.getView().showErrorNotification("请先选择订货客户！");
                return;
            }
            //查找其他单据已引用的派车信息单
            String billNo = this.getModel().getDataEntity().getString("billno");
            QFilter filter = new QFilter("billno", QCP.not_equals, billNo).and("nckd_vehicledisp", QCP.not_equals,"");
            DynamicObjectCollection vehicledisp = QueryServiceHelper.query("sm_delivernotice",
                    "nckd_vehicledisp", filter.toArray(), "");
            HashSet<String> vehicledispList = new HashSet<>();
            if (!vehicledisp.isEmpty()) {
                for (DynamicObject item : vehicledisp) {
                    String vehicledispNo = item.getString("nckd_vehicledisp");
                    vehicledispList.add(vehicledispNo);
                }
            }

            //打开销售合同列表
            ListShowParameter parameter = ShowFormHelper.createShowListForm("nckd_vehicledisp", false);
            //过滤列表
            ListFilterParameter listFilterParameter = new ListFilterParameter();
            QFilter listFilter = new QFilter("billno", QCP.not_in, vehicledispList);
            //客户Id
            Object customerId = customer.getPkValue();
            listFilter.and("nckd_basecustomer.id", QCP.equals, customerId);
            listFilterParameter.setFilter(listFilter);
            parameter.setListFilterParameter(listFilterParameter);
            //设置回调
            parameter.setCloseCallBack(new CloseCallBack(this, "vehicledisp"));
            getView().showForm(parameter);
        }
        super.click(evt);
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent evt) {
        String key = evt.getActionId();
        Object returnData = evt.getReturnData();
        // 将选择的销售合同数据回写至样例单据上的相应字段
        if (StringUtils.equalsIgnoreCase("vehicledisp", key) && returnData != null) {
            ListSelectedRow row = ((ListSelectedRowCollection) returnData).get(0);
            DynamicObject billObj = BusinessDataServiceHelper.loadSingle(row.getPrimaryKeyValue(), "nckd_vehicledisp");
            getModel().setValue("nckd_vehicledisp", row.getBillNo());//派车信息单编码
            String plateno = billObj.getString("nckd_plateno");//车牌
            String idcardno = billObj.getString("nckd_idcardno");//身份证号
            //根据车牌号查找车辆信息基础资料
            QFilter filter = new QFilter("name", QCP.equals, plateno).and("status", QCP.equals, "C");
            DynamicObjectCollection vehicle = QueryServiceHelper.query("nckd_vehicle",
                    "id", filter.toArray(), "");
            if (!vehicle.isEmpty()) {
                DynamicObject dataObject = vehicle.get(0);
                Object pkId = dataObject.get("id");
                getModel().setItemValueByID("nckd_vehicle", pkId, 0);
            } else {
                getModel().setValue("nckd_vehicle", null, 0);
            }
            //根据身份证获取司机信息基础资料
            QFilter qFilter = new QFilter("nckd_idcardno", QCP.equals, idcardno).and("status", QCP.equals, "C");
            DynamicObjectCollection driver = QueryServiceHelper.query("nckd_driver",
                    "id", qFilter.toArray(), "");
            if (!driver.isEmpty()) {
                DynamicObject dataObject = driver.get(0);
                Object pkId = dataObject.get("id");
                getModel().setItemValueByID("nckd_driver", pkId, 0);
            } else {
                getModel().setValue("nckd_driver", null, 0);
            }

        }
        //this.getView().invokeOperation("save");//保存单据
        super.closedCallBack(evt);
    }

}
