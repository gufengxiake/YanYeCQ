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
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class MaterialrequestListPlugin extends AbstractListPlugin {

    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“产成品”时
     * 集合中的参数代表单据类型
     */
    private List<String> finishedGoodsList = Arrays.asList("1", "2", "3", "4");
    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“半成品”时
     */
    private List<String> semiFinishedList = Arrays.asList("1", "2", "3");
    /**
     * 【物料类型】为‘物资’或‘资产’+【物料属性】为‘外购’时
     */
    private List<String> outsourcingList = Arrays.asList("1", "2", "3", "5");
    /**
     * 【物料类型】为‘费用’+【物料属性】为‘外购’时
     */
    private List<String> feeOutsourcingList = Arrays.asList("3", "5");


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
            Map<Long, DynamicObject> map = new HashMap<>();
            Arrays.stream(bussProcessOrderArr).forEach(t -> {
                if (t.getBoolean("nckd_isgenerate")) {
                    this.getView().showErrorNotification("物料申请单编号：" + t.getString("billno") + "已生成物料维护单，不允许重复生成");
                    return;
                }
                t.set("nckd_isgenerate", true);
                map.put(t.getLong("id"), t);
                //物料分录
                DynamicObjectCollection dynamicObjectCollection = t.getDynamicObjectCollection("nckd_materialentries");
                objects.addAll(dynamicObjectCollection);

            });
            for (DynamicObject dynamicObject : objects) {
                /**
                 * 物料类型nckd_materialtype(1:物资、7:费用、8:资产)
                 * 物料属性nckd_materialattribute(1:自制、2：外购)
                 * 自制物料类型nckd_selfmaterialtype(1：产成品、2：半成品)
                 */
                if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                        && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                        && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                    getDynamicObject(dynamicObject, finishedGoodsList, map);
                } else if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                        && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                        && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                    getDynamicObject(dynamicObject, semiFinishedList, map);
                } else if (Arrays.asList("1", "8").contains(dynamicObject.getString("nckd_materialtype"))
                        && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                    getDynamicObject(dynamicObject, outsourcingList, map);
                } else if ("7".equals(dynamicObject.getString("nckd_materialtype"))
                        && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                    getDynamicObject(dynamicObject, feeOutsourcingList, map);
                }

                // 申请组织
                DynamicObject org = map.get(dynamicObject.getLong("nckd_fid")).getDynamicObject("org");

                // 生成物料属性信息
                if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
                        && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))
                        && "113".equals(org.getString("number"))) {
                    //【物料属性】为‘自制’+【自制物料类型】‘产成品’+【申请组织】‘江西盐业包装有限公司’
                    this.purchaseInfo(dynamicObject, org);// 采购基本信息
                } else if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
                        && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                    //【物料属性】为‘自制’+【自制物料类型】“半成品”
                    this.purchaseInfo(dynamicObject, org);// 采购基本信息
                    this.marketInfo(dynamicObject, org);// 销售基本信息
                } else if ("2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                    //【物料属性】为‘外购’
                    this.marketInfo(dynamicObject, org);// 销售基本信息
                }
            }
            SaveServiceHelper.update(bussProcessOrderArr);
            this.getView().showSuccessNotification("物料维护单生成成功");
        }
    }

    /**
     * 销售基本信息
     */
    private void marketInfo(DynamicObject dynamicObject, DynamicObject org) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialsalinfo");

        DynamicObject material = this.getMaterial(dynamicObject);

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 销售信息创建组织
        newDynamicObject.set("createorg", org);
        // 销售单位
        newDynamicObject.set("salesunit", material.getDynamicObject("baseunit"));
        // 销售信息数据状态
        newDynamicObject.set("status", "A");
        // 销售信息控制策略
        newDynamicObject.set("ctrlstrategy", this.getCtrlStrgy(org));
        // 销售信息使用状态
        newDynamicObject.set("enable", "1");
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        SaveServiceHelper.saveOperate("bd_materialsalinfo", new DynamicObject[]{newDynamicObject}, OperateOption.create());
    }

    /**
     * 采购基本信息
     */
    private void purchaseInfo(DynamicObject dynamicObject, DynamicObject org) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialpurchaseinfo");

        DynamicObject material = this.getMaterial(dynamicObject);

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 采购信息创建组织
        newDynamicObject.set("createorg", org);
        // 采购单位
        newDynamicObject.set("purchaseunit", material.getDynamicObject("baseunit"));
        // 采购信息数据状态
        newDynamicObject.set("status", "A");
        // 采购信息控制策略
        newDynamicObject.set("ctrlstrategy", this.getCtrlStrgy(org));
        // 采购信息使用状态
        newDynamicObject.set("enable", "1");
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        SaveServiceHelper.saveOperate("bd_materialpurchaseinfo", new DynamicObject[]{newDynamicObject}, OperateOption.create());
    }


    // 获取物料
    private DynamicObject getMaterial(DynamicObject dynamicObject) {
        String nckdMaterialnumber = dynamicObject.getString("nckd_materialnumber");
        DynamicObject bd_material = BusinessDataServiceHelper.loadSingle("bd_material", new QFilter[]{new QFilter("number", QCP.equals, nckdMaterialnumber)});
        return bd_material;
    }

    // 获取控制策略
    private String getCtrlStrgy(DynamicObject org) {
        String ctrlStrgy = BaseDataServiceHelper.getBdCtrlStrgy("bd_materialmftinfo", String.valueOf(org.getPkValue()));
        if (ctrlStrgy != null && ctrlStrgy.length() > 0) {
            String[] ctrlStrgys = ctrlStrgy.split(",");
            if (ctrlStrgys.length > 1) {
                String[] var3 = ctrlStrgys;
                int var4 = ctrlStrgys.length;

                for (int var5 = 0; var5 < var4; ++var5) {
                    String ctr = var3[var5];
                    if (kd.bos.dataentity.utils.StringUtils.isNotEmpty(ctr)) {
                        return ctr;
                    }
                }
            }
        }

        return ctrlStrgy;
    }

    /**
     * @param dynamicObject 物料分录
     * @param list          单据类型集合
     * @return
     */
    private void getDynamicObject(DynamicObject dynamicObject, List<String> list, Map<Long, DynamicObject> map) {
        for (String billType : list) {
            DynamicObject object = map.get(dynamicObject.getLong("nckd_fid"));
            DynamicObject materialmaintenanObject = BusinessDataServiceHelper.newDynamicObject("nckd_materialmaintenan");
            /**制单信息**/
            materialmaintenanObject.set("creator", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("createtime", new Date());
            materialmaintenanObject.set("modifier", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("modifytime", new Date());
            materialmaintenanObject.set("billstatus", "A");//单据状态
            materialmaintenanObject.set("org", object.getDynamicObject("org"));//申请组织
            materialmaintenanObject.set("nckd_materialmaintunit", "add");//单据维护类型：新增物料属性
            materialmaintenanObject.set("nckd_documenttype", billType);//单据类型：
            materialmaintenanObject.set("nckd_materialclassify", dynamicObject.getDynamicObject("nckd_materialclassify"));//物料分类
            materialmaintenanObject.set("nckd_materialname", dynamicObject.getString("nckd_materialname"));//物料名称
            materialmaintenanObject.set("nckd_specifications", dynamicObject.getString("nckd_specifications"));//规格
            materialmaintenanObject.set("nckd_model", dynamicObject.getString("nckd_model"));//型号
            materialmaintenanObject.set("nckd_baseunit", dynamicObject.getDynamicObject("nckd_baseunit"));//基本单位

            materialmaintenanObject.set("nckd_materialtype", dynamicObject.getString("nckd_materialtype"));//物料类型
            materialmaintenanObject.set("nckd_oldmaterialnumber", dynamicObject.getString("nckd_oldmaterialnumber"));//旧物料编码
            materialmaintenanObject.set("nckd_mnemoniccode", dynamicObject.getString("nckd_mnemoniccode"));//助记码
            materialmaintenanObject.set("nckd_remark", dynamicObject.getString("nckd_remark"));//描述
            materialmaintenanObject.set("nckd_materialrisk", dynamicObject.getString("nckd_materialrisk"));//物料危险性
            materialmaintenanObject.set("nckd_outsourcing", dynamicObject.getBoolean("nckd_outsourcing"));//可委外
            materialmaintenanObject.set("nckd_materialattribute", dynamicObject.getString("nckd_materialattribute"));//物料属性
            materialmaintenanObject.set("nckd_selfmaterialtype", dynamicObject.getString("nckd_selfmaterialtype"));//自制物料类型
            materialmaintenanObject.set("nckd_materialid", dynamicObject.getLong("id"));//物料申请单物料分录id
            OperationServiceHelper.executeOperate("save", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
        }
    }
}
