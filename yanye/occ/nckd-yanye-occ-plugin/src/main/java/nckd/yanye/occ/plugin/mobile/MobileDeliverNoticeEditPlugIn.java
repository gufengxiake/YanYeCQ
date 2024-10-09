package nckd.yanye.occ.plugin.mobile;

import kd.bos.bill.MobileFormPosition;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormConfig;
import kd.bos.form.FormMetadataCache;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.MobileListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;

public class MobileDeliverNoticeEditPlugIn extends AbstractMobFormPlugin {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners("nckd_buttonap");
    }

    public void click(EventObject evt) {
        super.click(evt);
        Control control = (Control) evt.getSource();
        String key = control.getKey();
        //选择派车单按钮
        if (key.equalsIgnoreCase("nckd_buttonap")) {
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
            MobileListShowParameter listPara = createShowMobileF7ListForm("nckd_vehicledisp", false);//第二个参数为是否支持多选;
            //过滤列表
            ListFilterParameter listFilterParameter = new ListFilterParameter();
            QFilter listFilter = new QFilter("billno", QCP.not_in, vehicledispList);
            //客户Id
            Object customerId = customer.getPkValue();
            listFilter.and("nckd_basecustomer.id", QCP.equals, customerId);
            listFilterParameter.setFilter(listFilter);
            listPara.setListFilterParameter(listFilterParameter);
            // 设置回调
            listPara.setCloseCallBack(new CloseCallBack(this, "return"));
            this.getView().showForm(listPara);
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {

        super.closedCallBack(closedCallBackEvent);
        ListSelectedRowCollection returnData = (ListSelectedRowCollection) closedCallBackEvent.getReturnData();
        if(returnData.size()==0){
            return;
        }
        String key = closedCallBackEvent.getActionId();
        // 接收回调
        if (com.alibaba.druid.util.StringUtils.equalsIgnoreCase("return", key) && closedCallBackEvent.getReturnData() instanceof ListSelectedRowCollection) {
            ListSelectedRow row = returnData.get(0);
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
    }


    /*
    创建移动列表
     */
    private static MobileListShowParameter createShowMobileF7ListForm(String formId, boolean isMultiSelect) {
        MobileListShowParameter para = new MobileListShowParameter();
        FormConfig formConfig = FormMetadataCache.getMobListFormConfig(formId);
        para.setCaption(formConfig.getCaption().toString());
        para.setLookUp(true);
        para.setBillFormId(formId);
        ShowType showType;
        if (formConfig.getShowType() == ShowType.MainNewTabPage) {
            showType = ShowType.Floating;
        } else {
            showType = ShowType.Modal;
            para.setPosition(MobileFormPosition.Bottom);
        }
        para.getOpenStyle().setShowType(showType);
        para.setMultiSelect(isMultiSelect);

        String f7ListFormId = formConfig.getF7ListFormId();
        if (StringUtils.isNotBlank(f7ListFormId)) {
            para.setFormId(f7ListFormId);
        }

        return para;
    }
}
