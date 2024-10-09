package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.AfterAddRowEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.dataentity.utils.StringUtils;
/**
 * 销售订单勾选清运供应商，根据商务伙伴基础资料自动获取，同时行类别需要同步为“服务（数量）”
 * 表单插件  nckd_sm_salorder_ext
 * author:黄文波 2024-09-23
 */
public class SalOrderBillqyPlugIn extends AbstractBillPlugIn {

    //    @Override
//    public void registerListener(EventObject e) {
//        super.registerListener(e);
//        BasedataEdit inWareHouseEdit = this.getView().getControl("materialmasterid");
//
//        BasedataEdit wareHoseEdit = this.getView().getControl("warehouse");
//        wareHoseEdit.addBeforeF7SelectListener(this);
//    }
    @Override
    public void propertyChanged(PropertyChangedArgs e) {

        super.propertyChanged(e);
        String resut="";
        String propName = e.getProperty().getName();
//        //获取表头的需求部门字段
//        DynamicObject dept = (DynamicObject) this.getModel().getValue("dept");
//        Object deptId = dept.getPkValue();

        //订货客户
        if (propName.equals("customer")) {
            //Date billDate = DateTime.now().toDate();

            String ynqy= this.getModel().getValue("nckd_xsqy").toString();

            if("true".equals(ynqy)) {
                DynamicObject customer = (DynamicObject) this.getModel().getValue("customer");
                if (customer == null) return;
                //客户Id
                Object customerId = customer.getPkValue();
                customer = BusinessDataServiceHelper.loadSingle(customerId, customer.getDynamicObjectType().getName());

                String custname = customer.getString("name");


                //查询客户，找到商务伙伴ID
                DynamicObject customerDy = BusinessDataServiceHelper.loadSingle(customerId, "bd_customer");
                DynamicObject FBizPartner = customerDy.getDynamicObject("bizpartner");
                Object FBizPartnerID = FBizPartner.getPkValue();

                //根据商务伙伴ID查询供应商
                DynamicObject sup = BusinessDataServiceHelper.loadSingle("bd_supplier", new QFilter[]{new QFilter("bizpartner", QCP.equals, FBizPartnerID)});
                if (sup == null) return;
                String supid = sup.getString("id");


                this.getModel().setValue("nckd_qygys", supid);

                DynamicObjectCollection billentry = this.getModel().getEntryEntity("billentry");
                int entryRowCount = this.getModel().getEntryRowCount("billentry");
                for (int i = 0; i < entryRowCount; i++) {

                    //查询行类型为020
                    DynamicObject linetypeDy = BusinessDataServiceHelper.loadSingle("bd_linetype", new QFilter[]{new QFilter("number", QCP.equals, "020")});
                    Object linetype = linetypeDy.getPkValue();

                    getModel().setValue("linetype", linetype, i);
                }
            }

        }


        //销售清运
        if (propName.equals("nckd_xsqy")) {
            //Date billDate = DateTime.now().toDate();


           String ynqy= this.getModel().getValue("nckd_xsqy").toString();

            if("true".equals(ynqy)) {
                DynamicObject customer = (DynamicObject) this.getModel().getValue("customer");
                if (customer == null) return;
                //客户Id
                Object customerId = customer.getPkValue();
                customer = BusinessDataServiceHelper.loadSingle(customerId, customer.getDynamicObjectType().getName());

                String custname = customer.getString("name");


                //查询客户，找到商务伙伴ID
                DynamicObject customerDy = BusinessDataServiceHelper.loadSingle(customerId, "bd_customer");
                DynamicObject FBizPartner = customerDy.getDynamicObject("bizpartner");
                Object FBizPartnerID = FBizPartner.getPkValue();

                //根据商务伙伴ID查询供应商
                DynamicObject sup = BusinessDataServiceHelper.loadSingle("bd_supplier", new QFilter[]{new QFilter("bizpartner", QCP.equals, FBizPartnerID)});
                if (sup == null) return;
                String supid = sup.getString("id");


                this.getModel().setValue("nckd_qygys", supid);

                DynamicObjectCollection billentry = this.getModel().getEntryEntity("billentry");
                int entryRowCount = this.getModel().getEntryRowCount("billentry");
                for (int i = 0; i < entryRowCount; i++) {

                    //查询行类型为020
                    DynamicObject linetypeDy = BusinessDataServiceHelper.loadSingle("bd_linetype", new QFilter[]{new QFilter("number", QCP.equals, "020")});
                    Object linetype = linetypeDy.getPkValue();

                    getModel().setValue("linetype", linetype, i);
                }
            }
            else
            {
                this.getModel().setValue("nckd_qygys", null);

                DynamicObjectCollection billentry = this.getModel().getEntryEntity("billentry");
                int entryRowCount = this.getModel().getEntryRowCount("billentry");
                for (int i = 0; i < entryRowCount; i++) {

                    //查询行类型为020
                    DynamicObject linetypeDy = BusinessDataServiceHelper.loadSingle("bd_linetype", new QFilter[]{new QFilter("number", QCP.equals, "010")});
                    Object linetype = linetypeDy.getPkValue();

                    getModel().setValue("linetype", linetype, i);
                }

            }
        }

        //销售订单单据体【产品类型】字段如果为特殊包装产品，单据头存在特殊包装字段勾选
        if (propName.equals("material")) {
            //获取当前行
            int index = e.getChangeSet()[0].getRowIndex();

            DynamicObject material = (DynamicObject) this.getModel().getValue("material",index);
            if (material == null) return;
            //物料Id
            Object materialId = material.getPkValue();
            String producttype=  material.getString("nckd_regularpackaging1");//获取物料销售信息-产品类型字段值

//            DynamicObject salinfo = BusinessDataServiceHelper.loadSingle( "bd_materialsalinfo","id,nckd_regularpackaging1",new QFilter[]{new QFilter("masterid", QCP.equals,materialId)});
//            String producttype = salinfo.getString("nckd_regularpackaging1");

            if("1".equals(producttype))
            {
                this.getModel().setValue("nckd_tsbz","true");
            }

        }

    }
    public void afterAddRow(AfterAddRowEventArgs e) {
        // 添加、插入、复制新行完毕，给新行填写了默认值之后，触发此事件；
        // 插件可以在此修改新行字段默认值，或者调整界面上控件的状态
        if (StringUtils.equals("billentry", e.getEntryProp().getName())){

 
            String ynqy= this.getModel().getValue("nckd_xsqy").toString();

            if("true".equals(ynqy)) {
                int entryRowCount = this.getModel().getEntryRowCount("billentry");
                for (int i = 0; i < entryRowCount; i++) {

                    //查询行类型为020
                    DynamicObject linetypeDy = BusinessDataServiceHelper.loadSingle("bd_linetype", new QFilter[]{new QFilter("number", QCP.equals, "020")});
                    Object linetype = linetypeDy.getPkValue();

                    getModel().setValue("linetype", linetype, i);
                }
            }
        }


        // 实例：DataModelChangeListener捕获此事件，给前端单据体增加新行
    }
}
