package nckd.yanye.scm.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.serialization.SerializationUtils;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.flex.FlexEntireData;
import kd.bos.entity.property.FlexProp;
import kd.bos.flex.FlexService;
//import org.joda.time.DateTime;
import java.util.Date;
import java.util.EventObject;
import java.util.Map;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.property.BasedataProp;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.FlexEdit;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.MainEntityType;
import kd.bos.dataentity.metadata.clr.DataEntityPropertyCollection;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.occ.ocbase.common.util.DynamicObjectUtils;

/**
 * 需求申请辅助属性写值
 * 表单插件
 * author:黄文波 2024-08-29
 */
public class RequirapplyBillPlugin extends AbstractBillPlugIn {

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

        //物料
        if (propName.equals("materialmasterid")) {
            //Date billDate = DateTime.now().toDate();
            //获取当前行
            int index = e.getChangeSet()[0].getRowIndex();
            DynamicObject item = (DynamicObject) this.getModel().getValue("materialmasterid", index);
            if (item == null) return;
            //商品Id
            Object itemId = item.getPkValue();
            item=BusinessDataServiceHelper.loadSingle(itemId,item.getDynamicObjectType().getName());
            if(item.getBoolean("isuseauxpty")) {
                DynamicObjectCollection auxptyentry = item.getDynamicObjectCollection("auxptyentry");
                for (DynamicObject auxptyent : auxptyentry) {
                    String num=  DynamicObjectUtils.getDynamicObject(auxptyent,"auxpty").getString("number");
//                     auxptyent.getDynamicObject("");
                    if("001".equals(num))
                    {
                        resut="A";
                        continue;
                    }
//                    System.out.println(num);
                }
            }
          if("A".equals(resut))
          {

              Object VAL_BASEDATA = itemId;
              // 给基础资料字段(物料)设置默认值
//            DynamicObject baseDataDefValObj = BusinessDataServiceHelper.loadSingle(VAL_BASEDATA, "Item-00002006");
//            getModel().setValue("materialmasterid", baseDataDefValObj);

              // 获取基础资料的弹性域字段
              BasedataEdit basedataEdit = getControl("materialmasterid");
              String flexFieldKey = basedataEdit.getFlexKey();
              // 获取单据上弹性域字段的type & value & property
              FlexEdit flexEdit = getControl(flexFieldKey);
              IDataModel fModel = flexEdit.getFlexModel();
              MainEntityType flexFieldEntityType = fModel.getDataEntityType();
              DynamicObject flexFieldVal = fModel.getDataEntity();
              DataEntityPropertyCollection properties = flexFieldEntityType.getProperties();
//            // 设置初始值
//            for (IDataEntityProperty prop : properties) {
//                String key = prop.getName();
//                if ("id".equals(key)) {
//                    continue;
//                }
//                if (!(prop instanceof BasedataProp)) {
//                    if (key.endsWith("_id")) {
//                        // 可不设置
//					    prop.setValue(flexFieldVal, "2027475951251632128");
//                        continue;
//                    }
//                    String subKey = key;
//                    key = subKey.split("__")[1];
//                    switch (key) {
//                        case "f000027":
//                            prop.setValue(flexFieldVal, "2027761533668307968");
//                            break;
//                        default:
//                            break;
//                    }
//                } else {
//                    prop.setValue(flexFieldVal, BusinessDataServiceHelper.loadSingle(deptId, "bos_org"));
//                }
//            }

              for (IDataEntityProperty prop : properties) {

                  prop.setValue(flexFieldVal, BusinessDataServiceHelper.loadSingle(deptId, "bos_org"));
              }
              FlexEntireData flexEntireData = new FlexEntireData();
              flexEntireData.setFlexData(flexFieldEntityType, flexFieldVal);
              long id = FlexService.saveFlexData(flexFieldEntityType, flexEntireData);
              // logger.info("保存弹性域字段值后生成的主键ID: " + id);
              flexFieldEntityType.getPrimaryKey().setValue(flexFieldVal, id);
              // new一个弹性域字段对象,并赋值
              DynamicObject flexObject = (DynamicObject) ((FlexProp) flexEdit.getProperty()).getComplexType().createInstance();
              flexObject.set("id", id);
              Map<String, Object> values = flexEntireData.getFlexValue();
              flexObject.set("value", SerializationUtils.toJsonString(values));
              getModel().setValue(flexFieldKey, flexObject);

          }


        }

    }

}
