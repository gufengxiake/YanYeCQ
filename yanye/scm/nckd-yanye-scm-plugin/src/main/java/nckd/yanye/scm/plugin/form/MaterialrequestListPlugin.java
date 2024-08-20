package nckd.yanye.scm.plugin.form;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class MaterialrequestListPlugin extends AbstractListPlugin {

    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“产成品”时
     * 集合中的参数代表单据类型
     */
    private List<String> finishedGoodsList= Arrays.asList("1","2","3","4");
    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“半成品”时
     */
    private List<String> semiFinishedList = Arrays.asList("1","2","3");
    /**
     * 【物料类型】为‘物资’或‘资产’+【物料属性】为‘外购’时
     */
    private List<String> outsourcingList = Arrays.asList("1","2","3","5");
    /**
     * 【物料类型】为‘费用’+【物料属性】为‘外购’时
     */
    private List<String> feeOutsourcingList = Arrays.asList("3","5");


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("nckd_material");
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        String operateKey = afterDoOperationEventArgs.getOperateKey();
        //生成物料维护单
        if (StringUtils.equals("material", operateKey)) {
            BillList billlistap = this.getView().getControl("billlistap");
            ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
            EntityType entityType = billlistap.getEntityType();
            //获取选中行pkid
            Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
            //构造DynamicObjectCollection 存储所有的物料分录
            DynamicObjectCollection objects = new DynamicObjectCollection();
            //获取完整数据（物料申请单）
            DynamicObject[] bussProcessOrderArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
            Map<Long,DynamicObject> map = new HashMap<>();
            Arrays.stream(bussProcessOrderArr).forEach(t->{
                if (t.getBoolean("nckd_isgenerate")) {
                    this.getView().showErrorNotification("物料申请单编号："+t.getString("billno") + "已生成物料维护单，不允许重复生成");
                    return;
                }
                t.set("nckd_isgenerate",true);
                map.put(t.getLong("id"),t);
                //物料分录
                DynamicObjectCollection dynamicObjectCollection = t.getDynamicObjectCollection("nckd_materialentries");
                objects.addAll(dynamicObjectCollection);

            });
            for (DynamicObject dynamicObject : objects){
                /**
                 * 物料类型nckd_materialtype(1:物资、7:费用、8:资产)
                 * 物料属性nckd_materialattribute(1:自制、2：外购)
                 * 自制物料类型nckd_selfmaterialtype(1：产成品、2：半成品)
                 */
                if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                        && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                        && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))){
                    getDynamicObject(dynamicObject,finishedGoodsList,map);
                }else if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                        && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                        && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))){
                    getDynamicObject(dynamicObject,semiFinishedList,map);
                }else if (Arrays.asList("1","8").contains(dynamicObject.getString("nckd_materialtype"))
                        && "2".equals(dynamicObject.getString("nckd_materialattribute"))){
                    getDynamicObject(dynamicObject,outsourcingList,map);
                }else if ("7".equals(dynamicObject.getString("nckd_materialtype"))
                        && "2".equals(dynamicObject.getString("nckd_materialattribute"))){
                    getDynamicObject(dynamicObject,feeOutsourcingList,map);
                }
            }
            SaveServiceHelper.update(bussProcessOrderArr);
            this.getView().showSuccessNotification("物料维护单生成成功");
        }
    }

    /**
     *
     * @param dynamicObject 物料分录
     * @param list 单据类型集合
     * @return
     */
    private void getDynamicObject(DynamicObject dynamicObject,List<String> list,Map<Long,DynamicObject> map){
        for (String billType : list){
            DynamicObject object = map.get(dynamicObject.getLong("nckd_fid"));
            DynamicObject materialmaintenanObject = BusinessDataServiceHelper.newDynamicObject("nckd_materialmaintenan");
            /**制单信息**/
            materialmaintenanObject.set("creator", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("createtime",new Date());
            materialmaintenanObject.set("modifier", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("modifytime", new Date());
            materialmaintenanObject.set("billstatus","A");//单据状态
            materialmaintenanObject.set("org",object.getDynamicObject("org"));//申请组织
            materialmaintenanObject.set("nckd_materialmaintunit","add");//单据维护类型：新增物料属性
            materialmaintenanObject.set("nckd_documenttype",billType);//单据类型：
            materialmaintenanObject.set("nckd_materialclassify",dynamicObject.getDynamicObject("nckd_materialclassify"));//物料分类
            materialmaintenanObject.set("nckd_materialname",dynamicObject.getString("nckd_materialname"));//物料名称
            materialmaintenanObject.set("nckd_specifications",dynamicObject.getString("nckd_specifications"));//规格
            materialmaintenanObject.set("nckd_model",dynamicObject.getString("nckd_model"));//型号
            materialmaintenanObject.set("nckd_baseunit",dynamicObject.getDynamicObject("nckd_baseunit"));//基本单位

            materialmaintenanObject.set("nckd_materialtype",dynamicObject.getString("nckd_materialtype"));//物料类型
            materialmaintenanObject.set("nckd_oldmaterialnumber",dynamicObject.getString("nckd_oldmaterialnumber"));//旧物料编码
            materialmaintenanObject.set("nckd_mnemoniccode",dynamicObject.getString("nckd_mnemoniccode"));//助记码
            materialmaintenanObject.set("nckd_remark",dynamicObject.getString("nckd_remark"));//描述
            materialmaintenanObject.set("nckd_materialrisk",dynamicObject.getString("nckd_materialrisk"));//物料危险性
            materialmaintenanObject.set("nckd_outsourcing",dynamicObject.getBoolean("nckd_outsourcing"));//可委外
            materialmaintenanObject.set("nckd_materialattribute",dynamicObject.getString("nckd_materialattribute"));//物料属性
            materialmaintenanObject.set("nckd_selfmaterialtype",dynamicObject.getString("nckd_selfmaterialtype"));//自制物料类型
            materialmaintenanObject.set("nckd_materialid",dynamicObject.getLong("id"));//物料申请单物料分录id
            OperationServiceHelper.executeOperate("save", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
        }
    }
}
