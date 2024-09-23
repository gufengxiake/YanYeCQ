package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
//import org.joda.time.DateTime;
import java.util.*;

import kd.bos.metadata.dao.MetaCategory;
import kd.bos.metadata.dao.MetadataDao;
import kd.bos.metadata.entity.EntityMetadata;
import kd.bos.metadata.entity.businessfield.BasedataField;
import kd.bos.metadata.form.ControlAp;
import kd.bos.metadata.form.FormMetadata;
import kd.bos.metadata.form.control.EntryFieldAp;
import kd.bos.servicehelper.BusinessDataServiceHelper;

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
        //获取表头的需求部门字段
        DynamicObject dept = (DynamicObject) this.getModel().getValue("dept");
        Object deptId = dept.getPkValue();

        //订货客户
        if (propName.equals("customer")) {
            //Date billDate = DateTime.now().toDate();

            DynamicObject customer = (DynamicObject) this.getModel().getValue("customer");
            if (customer == null) return;
            //客户Id
            Object customerId = customer.getPkValue();
            customer=BusinessDataServiceHelper.loadSingle(customerId,customer.getDynamicObjectType().getName());



            String  entityKey="bd_bizpartner";
            DynamicObject load = (DynamicObject) this.getModel().getValue(entityKey);
            if(load == null)return;
            String number = (String)load.get("number");
            HashMap<String, String> map = new HashMap<>();
            //功能：根据表单标识获取表单所有控件
            //根据表单编码获取表单id
            String id = MetadataDao.getIdByNumber(number, MetaCategory.Form);
            //获取表单元数据
            FormMetadata formMeta = (FormMetadata) MetadataDao.readRuntimeMeta(id, MetaCategory.Form);
            //获取实体元数据
            EntityMetadata metadata = (EntityMetadata) MetadataDao.readRuntimeMeta(id, MetaCategory.Entity);
            // 表单元数据绑定实体元数据
            formMeta.bindEntityMetadata(metadata);
            //获取所有控件集合
            List<ControlAp<?>> items = formMeta.getItems();
            for (ControlAp<?> itemhb : items)
            {
                // 判断当前控件是否属于EntryFieldAp
                if(itemhb instanceof EntryFieldAp)
                {
                    // 判断控件是否为基础资料控件
                    if(((EntryFieldAp) itemhb).getField() instanceof BasedataField)
                    {
                        // 该步是存放基础资料多语言标识名称，以及标识
                        map.put(itemhb.getName().getLocaleValue(),itemhb.getKey());
                    }
                }
            }



        }
        if (propName.equals("material")) {
            //获取当前行
            int index = e.getChangeSet()[0].getRowIndex();

            DynamicObject material = (DynamicObject) this.getModel().getValue("material",index);
            if (material == null) return;
            //客户Id
            Object materialId = material.getPkValue();
            material=BusinessDataServiceHelper.loadSingle(materialId,material.getDynamicObjectType().getName());

            String   producttype=this.getModel().getValue("nckd_regularpackaging",index).toString();
            if("为特殊包装产品".equals(producttype))
            {

            }


        }

    }

}
